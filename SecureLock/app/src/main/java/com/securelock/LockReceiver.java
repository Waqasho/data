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
        Log.d(TAG, "Received intent: " + intent.getAction());
        
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        SharedPreferences prefs = context.getSharedPreferences("SecureLockPrefs", Context.MODE_PRIVATE);
        
        String action = intent.getAction();
        String day = intent.getStringExtra("day");
        int hour = intent.getIntExtra("hour", 0);
        int minute = intent.getIntExtra("minute", 0);
        int duration = intent.getIntExtra("duration", 0);
        
        Log.d(TAG, "Action: " + action + ", Day: " + day + ", Hour: " + hour + ", Minute: " + minute);
        Log.d(TAG, "Current time: " + System.currentTimeMillis());
        Log.d(TAG, "Device admin active: " + dpm.isAdminActive(new ComponentName(context, MyDeviceAdminReceiver.class)));
        
        if ("com.securelock.LOCK_DEVICE".equals(action)) {
            ComponentName compName = new ComponentName(context, MyDeviceAdminReceiver.class);
            Log.d(TAG, "Checking device admin status...");
            if (dpm.isAdminActive(compName)) {
                Log.d(TAG, "Device admin active, starting lock service...");
                
                // Find the schedule that matches this day and current time
                int scheduleCount = prefs.getInt("schedule_count", 0);
                Log.d(TAG, "Total schedules found: " + scheduleCount);
                
                // Try to find matching schedule first
                boolean scheduleFound = false;
                for (int i = 0; i < scheduleCount; i++) {
                    String prefix = "schedule_" + i + "_";
                    Set<String> scheduleDays = prefs.getStringSet(prefix + "days", null);
                    
                    if (scheduleDays != null && scheduleDays.contains(day)) {
                        // This schedule matches the day, get its times
                        int startHour = prefs.getInt(prefix + "start_hour", 9);
                        int startMinute = prefs.getInt(prefix + "start_minute", 0);
                        int endHour = prefs.getInt(prefix + "end_hour", 17);
                        int endMinute = prefs.getInt(prefix + "end_minute", 0);
                        String label = prefs.getString(prefix + "label", "Scheduled Lock");
                        
                        Log.d(TAG, "Found matching schedule " + i + ": " + label);
                        Log.d(TAG, "Schedule times: " + startHour + ":" + startMinute + " to " + endHour + ":" + endMinute);
                        
                        // Calculate duration in minutes
                        int startTotalMinutes = startHour * 60 + startMinute;
                        int endTotalMinutes = endHour * 60 + endMinute;
                        int durationMinutes = endTotalMinutes - startTotalMinutes;
                        
                        if (durationMinutes <= 0) {
                            durationMinutes += 24 * 60; // Add 24 hours if end time is next day
                        }
                        
                        Log.d(TAG, "Found matching schedule: " + label + " for " + durationMinutes + " minutes");
                        
                        // Start the lock service
                        Intent serviceIntent = new Intent(context, LockService.class);
                        serviceIntent.putExtra("duration", durationMinutes);
                        serviceIntent.putExtra("schedule_label", label);
                        
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent);
                        } else {
                            context.startService(serviceIntent);
                        }
                        
                        Log.d(TAG, "Lock service started for " + durationMinutes + " minutes!");
                        Toast.makeText(context, "Schedule '" + label + "' activated! Device locked for " + durationMinutes + " minutes!", Toast.LENGTH_SHORT).show();
                        scheduleFound = true;
                        break;
                    }
                }
                
                // If no specific schedule found, use default lock
                if (!scheduleFound) {
                    Log.d(TAG, "No matching schedule found, using default lock");
                    
                    // Use default 8-hour lock (9 AM to 5 PM)
                    int durationMinutes = 8 * 60; // 8 hours
                    String label = "Default Lock";
                    
                    // Start the lock service
                    Intent serviceIntent = new Intent(context, LockService.class);
                    serviceIntent.putExtra("duration", durationMinutes);
                    serviceIntent.putExtra("schedule_label", label);
                    
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent);
                    } else {
                        context.startService(serviceIntent);
                    }
                    
                    Log.d(TAG, "Default lock service started for " + durationMinutes + " minutes!");
                    Toast.makeText(context, "Default lock activated! Device locked for " + durationMinutes + " minutes!", Toast.LENGTH_SHORT).show();
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