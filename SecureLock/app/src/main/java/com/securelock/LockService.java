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
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class LockService extends Service {
    private static final String TAG = "LockService";
    private static final String CHANNEL_ID = "SecureLockChannel";
    private static final int NOTIFICATION_ID = 1001;
    
    private DevicePolicyManager dpm;
    private ComponentName compName;
    private Timer lockTimer;
    private long lockEndTime;
    private Handler handler;
    private boolean isLockActive = false;
    private String scheduleLabel = "SecureLock";
    
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
        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        compName = new ComponentName(this, MyDeviceAdminReceiver.class);
        handler = new Handler(Looper.getMainLooper());
        
        // Register screen unlock receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenReceiver, filter);
        
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int durationMinutes = intent.getIntExtra("duration", 5);
            scheduleLabel = intent.getStringExtra("schedule_label");
            if (scheduleLabel == null) {
                scheduleLabel = "SecureLock";
            }
            startLock(durationMinutes);
        }
        return START_STICKY;
    }
    
    private void startLock(int durationMinutes) {
        isLockActive = true;
        lockEndTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L);
        
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
                        Toast.makeText(LockService.this, "Device locked!", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error locking device: " + e.getMessage());
                
                // Show error toast
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(LockService.this, "Error locking device: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        } else {
            Log.e(TAG, "Device admin not active");
            
            // Show error toast
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(LockService.this, "Device admin not active! Please enable in settings.", Toast.LENGTH_LONG).show();
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
        
        // Additional check to ensure device is still locked
        if (isLockActive) {
            // This is a backup mechanism to ensure device stays locked
            // The main locking is handled by the screen unlock receiver
        }
    }
    
    private void stopLock() {
        isLockActive = false;
        
        if (lockTimer != null) {
            lockTimer.cancel();
            lockTimer = null;
        }
        
        // Remove notification
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
        
        // Show completion notification
        showCompletionNotification();
        
        stopForeground(true);
        stopSelf();
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
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
            .build();
    }
    
    private void showScheduleStartNotification(int durationMinutes) {
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        Notification startNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔒 " + scheduleLabel + " Started")
            .setContentText("Device locked for " + durationMinutes + " minutes")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build();
        
        notificationManager.notify(NOTIFICATION_ID + 2, startNotification);
    }
    
    private void showCompletionNotification() {
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        Notification completionNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("✅ " + scheduleLabel + " Complete")
            .setContentText("Device lock time has expired - you can now use your device")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build();
        
        notificationManager.notify(NOTIFICATION_ID + 1, completionNotification);
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "SecureLock Notifications",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for SecureLock service");
            
            NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (screenReceiver != null) {
            unregisterReceiver(screenReceiver);
        }
        if (lockTimer != null) {
            lockTimer.cancel();
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    // Method to manually stop the lock service
    public static void stopLockService(Context context) {
        Intent stopIntent = new Intent(context, LockService.class);
        context.stopService(stopIntent);
    }
} 