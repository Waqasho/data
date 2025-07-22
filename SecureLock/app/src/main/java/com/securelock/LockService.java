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
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class LockService extends Service {
    private static final String TAG = "LockService";
    private static final int NOTIFICATION_ID = 1001;
    private static final int PERSISTENT_NOTIFICATION_ID = 1002;
    private static final String CHANNEL_ID = "SecureLockChannel";
    private static final String PERSISTENT_CHANNEL_ID = "SecureLockPersistentChannel";
    
    private static boolean isLockActive = false;
    private static long lockEndTime = 0;
    
    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;
    private Handler handler;
    private Timer lockTimer;
    private Timer notificationTimer;
    private PowerManager.WakeLock wakeLock;
    private BroadcastReceiver screenReceiver;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, MyDeviceAdminReceiver.class);
        handler = new Handler(Looper.getMainLooper());
        
        createNotificationChannels();
        startPersistentNotification();
        registerScreenReceiver();
        
        Log.d(TAG, "LockService created");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            boolean shouldStopCurrentLock = intent.getBooleanExtra("stop_current_lock", false);
            
            if (shouldStopCurrentLock) {
                Log.d(TAG, "Stopping current lock as requested");
                stopCurrentLockOnly();
                return START_STICKY;
            }
            
            int durationMinutes = intent.getIntExtra("duration_minutes", 30);
            boolean isImmediate = intent.getBooleanExtra("is_immediate", false);
            String scheduleLabel = intent.getStringExtra("schedule_label");
            
            Log.d(TAG, "Starting lock service - Duration: " + durationMinutes + " minutes, Immediate: " + isImmediate + ", Label: " + scheduleLabel);
            
            if (isImmediate) {
                startImmediateLock(durationMinutes);
            } else {
                // Handle scheduled lock
                startScheduledLock(durationMinutes, scheduleLabel != null ? scheduleLabel : "Scheduled Lock");
            }
        }
        
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        if (screenReceiver != null) {
            try {
                unregisterReceiver(screenReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering screen receiver: " + e.getMessage());
            }
        }
        
        if (lockTimer != null) {
            lockTimer.cancel();
        }
        
        if (notificationTimer != null) {
            notificationTimer.cancel();
        }
        
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        
        Log.d(TAG, "LockService destroyed");
    }
    
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            
            // Lock notification channel
            NotificationChannel lockChannel = new NotificationChannel(
                CHANNEL_ID,
                "SecureLock Active",
                NotificationManager.IMPORTANCE_HIGH
            );
            lockChannel.setDescription("Shows when device lock is active");
            lockChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(lockChannel);
            
            // Persistent notification channel
            NotificationChannel persistentChannel = new NotificationChannel(
                PERSISTENT_CHANNEL_ID,
                "SecureLock Running",
                NotificationManager.IMPORTANCE_LOW
            );
            persistentChannel.setDescription("Shows that SecureLock is running in background");
            persistentChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(persistentChannel);
        }
    }
    
    private void startPersistentNotification() {
        Intent intent = new Intent(this, ScheduleListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );
        
        Notification notification = new NotificationCompat.Builder(this, PERSISTENT_CHANNEL_ID)
            .setContentTitle("🔒 SecureLock Running")
            .setContentText("Tap to open app")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
        
        startForeground(PERSISTENT_NOTIFICATION_ID, notification);
    }
    
    private void registerScreenReceiver() {
        screenReceiver = new ScreenBroadcastReceiver(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenReceiver, filter);
    }
    
    // Static BroadcastReceiver class to avoid null pointer issues
    private static class ScreenBroadcastReceiver extends BroadcastReceiver {
        private final WeakReference<LockService> serviceRef;
        
        public ScreenBroadcastReceiver(LockService service) {
            this.serviceRef = new WeakReference<>(service);
        }
        
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isLockActive && intent != null) {
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_ON.equals(action) || Intent.ACTION_USER_PRESENT.equals(action)) {
                    LockService service = serviceRef.get();
                    if (service != null) {
                        service.lockDeviceNow();
                    }
                }
            }
        }
    }
    
    private void startImmediateLock(int durationMinutes) {
        // Stop any existing lock first
        if (isLockActive) {
            stopCurrentLockOnly();
        }
        
        isLockActive = true;
        lockEndTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L);
        
        Log.d(TAG, "Starting immediate lock for " + durationMinutes + " minutes");
        
        // Lock device immediately
        lockDeviceNow();
        
        // Start notification updates
        startNotificationUpdates();
        
        // Schedule unlock
        scheduleUnlock();
        
        // Acquire wake lock to keep screen control
        acquireWakeLock();
    }
    
    private void startScheduledLock(int durationMinutes, String label) {
        // Stop any existing lock first
        if (isLockActive) {
            stopCurrentLockOnly();
        }
        
        isLockActive = true;
        lockEndTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L);
        
        Log.d(TAG, "Starting scheduled lock '" + label + "' for " + durationMinutes + " minutes");
        
        // Lock device immediately for scheduled lock too
        lockDeviceNow();
        
        // Start notification updates
        startNotificationUpdates();
        
        // Schedule unlock
        scheduleUnlock();
        
        // Acquire wake lock to keep screen control
        acquireWakeLock();
        
        // Show scheduled lock started toast
        if (handler != null) {
            handler.post(new ToastRunnable(this, "📅 " + label + " started! Device locked for " + durationMinutes + " minutes."));
        }
    }
    
    private void lockDeviceNow() {
        try {
            if (devicePolicyManager != null && devicePolicyManager.isAdminActive(adminComponent)) {
                devicePolicyManager.lockNow();
                Log.d(TAG, "Device locked successfully");
            } else {
                Log.e(TAG, "Device admin not active");
                showErrorToast("Device admin not active! Please enable in settings.");
                stopSelf();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error locking device: " + e.getMessage());
            showErrorToast("Error locking device: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }
    
    private void showErrorToast(final String message) {
        if (handler != null) {
            handler.post(new ToastRunnable(this, message));
        }
    }
    
    // Static Runnable class for Toast
    private static class ToastRunnable implements Runnable {
        private final WeakReference<Context> contextRef;
        private final String message;
        
        public ToastRunnable(Context context, String message) {
            this.contextRef = new WeakReference<>(context);
            this.message = message;
        }
        
        @Override
        public void run() {
            Context context = contextRef.get();
            if (context != null) {
                Toast.makeText(context, "⚠️ " + message, Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void startNotificationUpdates() {
        if (notificationTimer != null) {
            notificationTimer.cancel();
        }
        
        notificationTimer = new Timer();
        notificationTimer.scheduleAtFixedRate(new NotificationUpdateTask(this), 0, 1000);
    }
    
    // Static TimerTask class
    private static class NotificationUpdateTask extends TimerTask {
        private final WeakReference<LockService> serviceRef;
        
        public NotificationUpdateTask(LockService service) {
            this.serviceRef = new WeakReference<>(service);
        }
        
        @Override
        public void run() {
            LockService service = serviceRef.get();
            if (service != null) {
                service.updateNotification();
            }
        }
    }
    
    private void scheduleUnlock() {
        if (lockTimer != null) {
            lockTimer.cancel();
        }
        
        long delay = lockEndTime - System.currentTimeMillis();
        if (delay > 0) {
            lockTimer = new Timer();
            lockTimer.schedule(new UnlockTask(this), delay);
            Log.d(TAG, "Unlock scheduled in " + (delay / 1000) + " seconds");
        } else {
            stopLock();
        }
    }
    
    // Static TimerTask class for unlock
    private static class UnlockTask extends TimerTask {
        private final WeakReference<LockService> serviceRef;
        
        public UnlockTask(LockService service) {
            this.serviceRef = new WeakReference<>(service);
        }
        
        @Override
        public void run() {
            LockService service = serviceRef.get();
            if (service != null) {
                service.stopLock();
            }
        }
    }
    
    private void acquireWakeLock() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "SecureLock:UnlockWakeLock"
                );
                // Don't acquire here, only when unlocking
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating wake lock: " + e.getMessage());
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
        
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, 
                createNotification(remainingMinutes, remainingSeconds));
        }
    }
    
    private Notification createNotification(int minutes, int seconds) {
        String timeText = String.format("%02d:%02d", minutes, seconds);
        
        Intent intent = new Intent(this, ScheduleListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔒 Device Locked")
            .setContentText("Unlocks in: " + timeText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build();
    }
    
    private void stopLock() {
        Log.d(TAG, "Stopping lock service");
        
        isLockActive = false;
        
        // Cancel timers
        if (lockTimer != null) {
            lockTimer.cancel();
            lockTimer = null;
        }
        
        if (notificationTimer != null) {
            notificationTimer.cancel();
            notificationTimer = null;
        }
        
        // Remove lock notification
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
        
        // Turn on screen when unlocking
        turnOnScreen();
        
        // Show unlock toast
        if (handler != null) {
            handler.post(new ToastRunnable(this, "✅ Device unlocked! Lock period completed."));
        }
        
        Log.d(TAG, "Device unlocked successfully");
    }
    
    private void turnOnScreen() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                // Create a new wake lock for screen on
                PowerManager.WakeLock screenWakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "SecureLock:ScreenOnWakeLock"
                );
                
                screenWakeLock.acquire(5000); // Keep screen on for 5 seconds
                Log.d(TAG, "Screen turned on with wake lock");
                
                if (handler != null) {
                    handler.postDelayed(new WakeLockReleaseRunnable(screenWakeLock), 5000);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error turning on screen: " + e.getMessage());
        }
    }
    
    // Static Runnable for wake lock release
    private static class WakeLockReleaseRunnable implements Runnable {
        private final WeakReference<PowerManager.WakeLock> wakeLockRef;
        
        public WakeLockReleaseRunnable(PowerManager.WakeLock wakeLock) {
            this.wakeLockRef = new WeakReference<>(wakeLock);
        }
        
        @Override
        public void run() {
            PowerManager.WakeLock wakeLock = wakeLockRef.get();
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }
    
    private void stopCurrentLockOnly() {
        isLockActive = false;
        
        if (lockTimer != null) {
            lockTimer.cancel();
            lockTimer = null;
        }
        
        if (notificationTimer != null) {
            notificationTimer.cancel();
            notificationTimer = null;
        }
        
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }
    
    public static boolean isLockCurrentlyActive() {
        return isLockActive;
    }
    
    public static long getRemainingLockTime() {
        if (!isLockActive) return 0;
        return Math.max(0, lockEndTime - System.currentTimeMillis());
    }
} 