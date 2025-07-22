package com.securelock;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import java.util.Set;

public class LockReceiver extends BroadcastReceiver {
    private static final String TAG = "LockReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "=== LockReceiver onReceive ===");
        Log.d(TAG, "Received intent: " + intent.getAction());
        Log.d(TAG, "Intent extras: " + intent.getExtras());
        
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        SharedPreferences prefs = context.getSharedPreferences("SecureLockPrefs", Context.MODE_PRIVATE);
        
        String action = intent.getAction();
        String day = intent.getStringExtra("day");
        int hour = intent.getIntExtra("hour", 0);
        int minute = intent.getIntExtra("minute", 0);
        boolean isLock = intent.getBooleanExtra("isLock", true);
        
        Log.d(TAG, "Action: " + action + ", Day: " + day + ", Hour: " + hour + ", Minute: " + minute + ", IsLock: " + isLock);
        Log.d(TAG, "Current time: " + new java.util.Date());
        Log.d(TAG, "Device admin active: " + dpm.isAdminActive(new ComponentName(context, MyDeviceAdminReceiver.class)));
        
        if ("com.securelock.LOCK_DEVICE".equals(action)) {
            ComponentName compName = new ComponentName(context, MyDeviceAdminReceiver.class);
            Log.d(TAG, "Processing LOCK_DEVICE action...");
            
            if (dpm.isAdminActive(compName)) {
                Log.d(TAG, "Device admin active, processing lock request...");
                
                // Find the schedule that matches this day and time
                int scheduleCount = prefs.getInt("schedule_count", 0);
                Log.d(TAG, "Total schedules found: " + scheduleCount);
                
                boolean scheduleFound = false;
                int lockDurationMinutes = 5; // Default duration
                String scheduleLabel = "SecureLock";
                
                for (int i = 0; i < scheduleCount; i++) {
                    String prefix = "schedule_" + i + "_";
                    Set<String> scheduleDays = prefs.getStringSet(prefix + "days", null);
                    
                    Log.d(TAG, "Checking schedule " + i + ": " + scheduleDays);
                    
                    if (scheduleDays != null && !scheduleDays.isEmpty() && scheduleDays.contains(day)) {
                        // This schedule matches the day, get its times
                        int startHour = prefs.getInt(prefix + "start_hour", 9);
                        int startMinute = prefs.getInt(prefix + "start_minute", 0);
                        int endHour = prefs.getInt(prefix + "end_hour", 17);
                        int endMinute = prefs.getInt(prefix + "end_minute", 0);
                        String label = prefs.getString(prefix + "label", "SecureLock");
                        
                        Log.d(TAG, "Schedule " + i + " times: " + startHour + ":" + String.format("%02d", startMinute) + 
                              " to " + endHour + ":" + String.format("%02d", endMinute));
                        
                        // Check if current alarm matches this schedule's start time
                        if (hour == startHour && minute == startMinute) {
                            // Calculate duration in minutes
                            int startTotalMinutes = startHour * 60 + startMinute;
                            int endTotalMinutes = endHour * 60 + endMinute;
                            
                            if (endTotalMinutes > startTotalMinutes) {
                                lockDurationMinutes = endTotalMinutes - startTotalMinutes;
                            } else {
                                // Handle overnight schedules (end time is next day)
                                lockDurationMinutes = (24 * 60 - startTotalMinutes) + endTotalMinutes;
                            }
                            
                            scheduleLabel = label;
                            scheduleFound = true;
                            Log.d(TAG, "Found matching schedule! Duration: " + lockDurationMinutes + " minutes, Label: " + scheduleLabel);
                            break;
                        }
                    }
                }
                
                if (scheduleFound || day != null) {
                    // Start lock service with calculated duration
                    Intent lockIntent = new Intent(context, LockService.class);
                    lockIntent.putExtra("duration", lockDurationMinutes);
                    lockIntent.putExtra("schedule_label", scheduleLabel);
                    
                    Log.d(TAG, "Starting LockService with duration: " + lockDurationMinutes + " minutes");
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(lockIntent);
                    } else {
                        context.startService(lockIntent);
                    }
                    
                    // Show toast notification
                    Toast.makeText(context, "🔒 " + scheduleLabel + " activated for " + lockDurationMinutes + " minutes", 
                                 Toast.LENGTH_LONG).show();
                } else {
                    Log.w(TAG, "No matching schedule found for day: " + day + ", time: " + hour + ":" + minute);
                }
            } else {
                Log.e(TAG, "Device admin not active!");
                Toast.makeText(context, "Device admin not active! Please enable in settings.", Toast.LENGTH_LONG).show();
            }
        } else if ("com.securelock.UNLOCK_DEVICE".equals(action)) {
            Log.d(TAG, "Unlock time reached");
            Toast.makeText(context, "Unlock time reached!", Toast.LENGTH_SHORT).show();
        }
    }
} 