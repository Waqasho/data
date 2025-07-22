package com.securelock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class LockService extends Service {
    private static final String TAG = "LockService";
    private static final String CHANNEL_ID = "SecureLockChannel";
    private static final String PERSISTENT_CHANNEL_ID = "SecureLockPersistent";
    private static final int NOTIFICATION_ID = 1001;
    private static final int PERSISTENT_NOTIFICATION_ID = 1002;
    
    private DevicePolicyManager dpm;
    private ComponentName compName;
    private PowerManager powerManager;
    private Timer lockTimer;
    private long lockEndTime;
    private Handler handler;
    private boolean isLockActive = false;
    private boolean isImmediateLock = false;
    private String scheduleLabel = "SecureLock";
    private static LockService instance;
    
    // Broadcast receiver for screen unlock events
    private BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (isLockActive && (Intent.ACTION_USER_PRESENT.equals(action) || Intent.ACTION_SCREEN_ON.equals(action))) {
                // User unlocked the screen or turned on screen, re-lock immediately
                Log.d(TAG, "Screen event detected: " + action + ", re-locking device");
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isLockActive) {
                            lockDevice();
                        }
                    }
                }, 1000); // Wait 1 second before re-locking
            }
        }
    };
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        compName = new ComponentName(this, MyDeviceAdminReceiver.class);
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        handler = new Handler(Looper.getMainLooper());
        
        // Register screen unlock receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenReceiver, filter);
        
        createNotificationChannels();
        showPersistentNotification();
    }
    
    public static LockService getInstance() {
        return instance;
    }
    
    public static void stopCurrentLock(Context context) {
        if (instance != null) {
            instance.stopLock();
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int durationMinutes = intent.getIntExtra("duration", 5);
            scheduleLabel = intent.getStringExtra("schedule_label");
            isImmediateLock = intent.getBooleanExtra("is_immediate", false);
            
            if (scheduleLabel == null) {
                scheduleLabel = "SecureLock";
            }
            
            // Stop any existing lock first
            if (isLockActive) {
                stopCurrentLockOnly();
            }
            
            startLock(durationMinutes);
        }
        return START_STICKY;
    }
    
    private void startLock(int durationMinutes) {
        isLockActive = true;
        lockEndTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L);
        
        Log.d(TAG, "Starting lock for " + durationMinutes + " minutes. End time: " + lockEndTime);
        
        // Show schedule start notification
        showScheduleStartNotification(durationMinutes);
        
        // Lock device immediately
        lockDevice();
        
        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification(durationMinutes));
        
        // Start timer to update notification and check lock status
        lockTimer = new Timer();
        lockTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateNotification();
                checkLockStatus();
                
                // Periodically ensure device is locked (every 30 seconds)
                if (isLockActive && (System.currentTimeMillis() % 30000) < 1000) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (isLockActive) {
                                lockDevice();
                            }
                        }
                    });
                }
            }
        }, 0, 1000); // Update every second
    }
    
    private void lockDevice() {
        if (dpm.isAdminActive(compName)) {
            try {
                dpm.lockNow();
                Log.d(TAG, "Device locked successfully at " + System.currentTimeMillis());
                
                // Also show a toast to confirm lock
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(LockService.this, "🔒 Device Locked!", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error locking device: " + e.getMessage());
                
                // Show error toast
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(LockService.this, "❌ Error locking device: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        } else {
            Log.e(TAG, "Device admin not active");
            
            // Show error toast
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(LockService.this, "⚠️ Device admin not active! Please enable in settings.", Toast.LENGTH_LONG).show();
                }
            });
            
            stopSelf();
        }
    }
    
    private void updateNotification() {
        long remainingTime = lockEndTime - System.currentTimeMillis();
        if (remainingTime <= 0) {
            // Lock time expired
            stopLock();
            return;
        }
        
        int remainingMinutes = (int) (remainingTime / (60 * 1000));
        int remainingSeconds = (int) ((remainingTime % (60 * 1000)) / 1000);
        
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        notificationManager.notify(NOTIFICATION_ID, 
            createNotification(remainingMinutes, remainingSeconds));
    }
    
    private void checkLockStatus() {
        // Check if lock time has expired
        if (System.currentTimeMillis() >= lockEndTime) {
            Log.d(TAG, "Lock time expired, stopping lock service");
            stopLock();
        }
    }
    
    private void stopCurrentLockOnly() {
        isLockActive = false;
        
        if (lockTimer != null) {
            lockTimer.cancel();
            lockTimer = null;
        }
        
        // Remove active lock notification
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }
    
    private void stopLock() {
        stopCurrentLockOnly();
        
        // Show completion notification
        showCompletionNotification();
        
        // Turn screen on automatically when timer ends
        turnScreenOn();
        
        stopForeground(true);
        
        // Don't stop service completely, keep persistent notification
        showPersistentNotification();
    }
    
    private void turnScreenOn() {
        try {
            // Turn screen on when lock expires
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "SecureLock:ScreenOn"
            );
            wakeLock.acquire(3000); // Keep screen on for 3 seconds
            
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(LockService.this, "✅ Lock Timer Complete! Screen Turned On", Toast.LENGTH_LONG).show();
                }
            });
            
            Log.d(TAG, "Screen turned on automatically after lock expiry");
        } catch (Exception e) {
            Log.e(TAG, "Error turning screen on: " + e.getMessage());
        }
    }
    
    private Notification createNotification(int minutes) {
        return createNotification(minutes, 0);
    }
    
    private Notification createNotification(int minutes, int seconds) {
        String timeText = String.format("%02d:%02d", minutes, seconds);
        String title = "🔒 " + scheduleLabel + " Active";
        String content = "Device locked - " + timeText + " remaining";
        
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE);
        
        // Add stop action
        Intent stopIntent = new Intent(this, LockService.class);
        stopIntent.setAction("STOP_LOCK");
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE);
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
            .addAction(android.R.drawable.ic_media_pause, "Stop Lock", stopPendingIntent)
            .build();
    }
    
    private void showPersistentNotification() {
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        Intent notificationIntent = new Intent(this, ScheduleListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE);
        
        Notification persistentNotification = new NotificationCompat.Builder(this, PERSISTENT_CHANNEL_ID)
            .setContentTitle("🛡️ SecureLock Running")
            .setContentText("App is running in background - Ready to lock when scheduled")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build();
        
        notificationManager.notify(PERSISTENT_NOTIFICATION_ID, persistentNotification);
    }
    
    private void showScheduleStartNotification(int durationMinutes) {
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        String lockType = isImmediateLock ? "Immediate Lock" : "Scheduled Lock";
        
        Notification startNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔒 " + lockType + " Started")
            .setContentText(scheduleLabel + " - Device locked for " + durationMinutes + " minutes")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build();
        
        notificationManager.notify(NOTIFICATION_ID + 2, startNotification);
    }
    
    private void showCompletionNotification() {
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        String lockType = isImmediateLock ? "Immediate Lock" : "Scheduled Lock";
        
        Notification completionNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("✅ " + lockType + " Complete")
            .setContentText(scheduleLabel + " - Lock time expired. Screen turned on automatically!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build();
        
        notificationManager.notify(NOTIFICATION_ID + 1, completionNotification);
    }
    
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Active lock channel
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "SecureLock Active",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for active lock sessions");
            
            // Persistent background channel
            NotificationChannel persistentChannel = new NotificationChannel(
                PERSISTENT_CHANNEL_ID,
                "SecureLock Background",
                NotificationManager.IMPORTANCE_LOW
            );
            persistentChannel.setDescription("Background service notification");
            
            NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
            notificationManager.createNotificationChannel(persistentChannel);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        if (screenReceiver != null) {
            unregisterReceiver(screenReceiver);
        }
        if (lockTimer != null) {
            lockTimer.cancel();
        }
        
        // Remove persistent notification when service is destroyed
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(PERSISTENT_NOTIFICATION_ID);
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    // Method to manually stop the lock service
    public static void stopLockService(Context context) {
        if (instance != null) {
            instance.stopLock();
        }
    }
} 