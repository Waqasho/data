package com.securelock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.Calendar;
import java.util.Set;
import android.content.ComponentName;
import android.os.Build;

public class ScheduleManager {
    private static final String TAG = "ScheduleManager";
    private static final String PREFS_NAME = "SecureLockPrefs";
    private static final String SCHEDULE_SET_TIME = "schedule_set_time";
    private static final long RESTRICT_DAYS = 15 * 24 * 60 * 60 * 1000L;

    public static boolean canEditSchedule(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastSet = prefs.getLong(SCHEDULE_SET_TIME, 0);
        long now = System.currentTimeMillis();
        return (now - lastSet) >= RESTRICT_DAYS;
    }

    public static void setLockSchedule(Context context, int startHour, int startMinute, 
                                     int endHour, int endMinute, Set<String> selectedDays) {
        Log.d(TAG, "Setting lock schedule: " + startHour + ":" + startMinute + " to " + endHour + ":" + endMinute);
        Log.d(TAG, "Selected days: " + selectedDays);
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        // Cancel existing alarms first
        cancelExistingAlarms(context);
        
        // Set new alarms for each selected day
        for (String day : selectedDays) {
            int dayOfWeek = getDayOfWeek(day);
            Log.d(TAG, "Setting alarms for day: " + day + " (dayOfWeek: " + dayOfWeek + ")");
            
            // Set lock alarm for this day
            setAlarmForDay(context, alarmManager, day, startHour, startMinute, true);
            
            // Set unlock alarm for this day
            setAlarmForDay(context, alarmManager, day, endHour, endMinute, false);
        }

        // Save schedule timestamp
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(SCHEDULE_SET_TIME, System.currentTimeMillis());
        editor.apply();
        
        Log.d(TAG, "Schedule saved successfully.");
    }
    
    private static void setAlarmForDay(Context context, AlarmManager alarmManager, 
                                     String day, int hour, int minute, boolean isLock) {
        Calendar alarmTime = Calendar.getInstance();
        Calendar now = Calendar.getInstance();
        
        // Set to today first
        alarmTime.set(Calendar.HOUR_OF_DAY, hour);
        alarmTime.set(Calendar.MINUTE, minute);
        alarmTime.set(Calendar.SECOND, 0);
        alarmTime.set(Calendar.MILLISECOND, 0);
        
        // Calculate target day of week
        int targetDayOfWeek = getDayOfWeek(day);
        int currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        
        // Calculate days to add
        int daysToAdd = targetDayOfWeek - currentDayOfWeek;
        if (daysToAdd < 0) {
            daysToAdd += 7; // Next week
        } else if (daysToAdd == 0) {
            // Same day - check if time has passed
            if (alarmTime.getTimeInMillis() <= now.getTimeInMillis()) {
                daysToAdd = 7; // Next week
            }
        }
        
        alarmTime.add(Calendar.DAY_OF_YEAR, daysToAdd);
        
        String action = isLock ? "com.securelock.LOCK_DEVICE" : "com.securelock.UNLOCK_DEVICE";
        Intent intent = new Intent(action);
        intent.putExtra("day", day);
        intent.putExtra("hour", hour);
        intent.putExtra("minute", minute);
        intent.putExtra("isLock", isLock);
        // Explicitly set the receiver component
        intent.setComponent(new ComponentName(context, LockReceiver.class));
        
        int requestCode = getRequestCode(day, isLock);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Use setExactAndAllowWhileIdle for more reliable alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), pendingIntent);
        }
        
        String type = isLock ? "Lock" : "Unlock";
        Log.d(TAG, type + " alarm set for " + day + " at " + hour + ":" + String.format("%02d", minute) + 
              " (trigger time: " + alarmTime.getTime() + ")");
        Log.d(TAG, "Current time: " + now.getTime() + ", Days to add: " + daysToAdd);
    }
    
    // Test method to manually trigger lock for testing
    public static void testLockNow(Context context) {
        Log.d(TAG, "Testing lock now...");
        Intent lockIntent = new Intent("com.securelock.LOCK_DEVICE");
        lockIntent.putExtra("day", "M");
        lockIntent.putExtra("hour", 9);
        lockIntent.putExtra("minute", 0);
        // Explicitly set the receiver component
        lockIntent.setComponent(new ComponentName(context, LockReceiver.class));
        context.sendBroadcast(lockIntent);
    }
    
    // Method to set alarm for next minute for testing
    public static void setTestAlarm(Context context) {
        Log.d(TAG, "Setting test alarm for next minute...");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        Calendar testTime = Calendar.getInstance();
        testTime.add(Calendar.MINUTE, 1); // 1 minute from now
        
        Intent lockIntent = new Intent("com.securelock.LOCK_DEVICE");
        lockIntent.putExtra("day", "M");
        lockIntent.putExtra("hour", testTime.get(Calendar.HOUR_OF_DAY));
        lockIntent.putExtra("minute", testTime.get(Calendar.MINUTE));
        // Explicitly set the receiver component
        lockIntent.setComponent(new ComponentName(context, LockReceiver.class));
        
        PendingIntent lockPending = PendingIntent.getBroadcast(context, 999, lockIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, testTime.getTimeInMillis(), lockPending);
        Log.d(TAG, "Test alarm set for: " + testTime.getTime());
    }
    
    // Method to set alarm for next 30 seconds for immediate testing
    public static void setImmediateTestAlarm(Context context) {
        Log.d(TAG, "Setting immediate test alarm for next 30 seconds...");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        Calendar testTime = Calendar.getInstance();
        testTime.add(Calendar.SECOND, 30); // 30 seconds from now
        
        Intent lockIntent = new Intent("com.securelock.LOCK_DEVICE");
        lockIntent.putExtra("day", "M");
        lockIntent.putExtra("hour", testTime.get(Calendar.HOUR_OF_DAY));
        lockIntent.putExtra("minute", testTime.get(Calendar.MINUTE));
        // Explicitly set the receiver component
        lockIntent.setComponent(new ComponentName(context, LockReceiver.class));
        
        PendingIntent lockPending = PendingIntent.getBroadcast(context, 998, lockIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, testTime.getTimeInMillis(), lockPending);
        Log.d(TAG, "Immediate test alarm set for: " + testTime.getTime());
    }
    
    private static void cancelExistingAlarms(Context context) {
        Log.d(TAG, "Cancelling existing alarms");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        // Cancel all possible alarms (for all days)
        String[] days = {"S", "M", "T", "W", "R", "F", "A"};
        for (String day : days) {
            // Cancel lock alarms
            Intent lockIntent = new Intent("com.securelock.LOCK_DEVICE");
            lockIntent.setComponent(new ComponentName(context, LockReceiver.class));
            PendingIntent lockPending = PendingIntent.getBroadcast(context, 
                getRequestCode(day, true), lockIntent, 
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (lockPending != null) {
                alarmManager.cancel(lockPending);
                lockPending.cancel();
                Log.d(TAG, "Cancelled lock alarm for " + day);
            }
            
            // Cancel unlock alarms
            Intent unlockIntent = new Intent("com.securelock.UNLOCK_DEVICE");
            unlockIntent.setComponent(new ComponentName(context, LockReceiver.class));
            PendingIntent unlockPending = PendingIntent.getBroadcast(context, 
                getRequestCode(day, false), unlockIntent, 
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (unlockPending != null) {
                alarmManager.cancel(unlockPending);
                unlockPending.cancel();
                Log.d(TAG, "Cancelled unlock alarm for " + day);
            }
        }
        
        // Cancel test alarms
        Intent testIntent = new Intent("com.securelock.LOCK_DEVICE");
        testIntent.setComponent(new ComponentName(context, LockReceiver.class));
        PendingIntent testPending = PendingIntent.getBroadcast(context, 999, testIntent, 
            PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (testPending != null) {
            alarmManager.cancel(testPending);
            testPending.cancel();
            Log.d(TAG, "Cancelled test alarm");
        }
        
        PendingIntent immediateTestPending = PendingIntent.getBroadcast(context, 998, testIntent, 
            PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (immediateTestPending != null) {
            alarmManager.cancel(immediateTestPending);
            immediateTestPending.cancel();
            Log.d(TAG, "Cancelled immediate test alarm");
        }
    }
    
    private static int getDayOfWeek(String day) {
        switch (day) {
            case "S": return Calendar.SUNDAY;
            case "M": return Calendar.MONDAY;
            case "T": return Calendar.TUESDAY;
            case "W": return Calendar.WEDNESDAY;
            case "R": return Calendar.THURSDAY;
            case "F": return Calendar.FRIDAY;
            case "A": return Calendar.SATURDAY;
            default: return Calendar.MONDAY;
        }
    }
    
    private static int getRequestCode(String day, boolean isLock) {
        int dayCode = 0;
        switch (day) {
            case "S": dayCode = 1; break;
            case "M": dayCode = 2; break;
            case "T": dayCode = 3; break;
            case "W": dayCode = 4; break;
            case "R": dayCode = 5; break;
            case "F": dayCode = 6; break;
            case "A": dayCode = 7; break;
        }
        return dayCode * 10 + (isLock ? 1 : 2);
    }
} 