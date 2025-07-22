package com.securelock;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScheduleListActivity extends AppCompatActivity {
    
    private LinearLayout scheduleContainer;
    private Button addNewScheduleButton, immediateLockButton;
    private ImageButton backButton;
    private ScrollView scrollView;
    private SharedPreferences prefs;
    
    private static final String PREFS_NAME = "SecureLockPrefs";
    private static final String SCHEDULE_COUNT = "schedule_count";
    private static final String SCHEDULE_PREFIX = "schedule_";
    private static final long RESTRICT_DAYS = 15 * 24 * 60 * 60 * 1000L;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_list);
        
        initializeViews();
        setupClickListeners();
        loadSchedules();
    }
    
    private void initializeViews() {
        scheduleContainer = findViewById(R.id.scheduleContainer);
        addNewScheduleButton = findViewById(R.id.addNewScheduleButton);
        immediateLockButton = findViewById(R.id.immediateLockButton);
        backButton = findViewById(R.id.backButton);
        scrollView = findViewById(R.id.scrollView);
        
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Start persistent service
        startPersistentService();
    }
    
    private void setupClickListeners() {
        addNewScheduleButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });
        
        immediateLockButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, LockDurationActivity.class);
            startActivity(intent);
        });
        
        backButton.setOnClickListener(v -> finish());
    }
    
    private void startPersistentService() {
        // Start the service to show persistent notification
        Intent serviceIntent = new Intent(this, LockService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
    
    private void loadSchedules() {
        scheduleContainer.removeAllViews();
        
        int scheduleCount = prefs.getInt(SCHEDULE_COUNT, 0);
        
        if (scheduleCount == 0) {
            // Show modern empty state
            LinearLayout emptyLayout = new LinearLayout(this);
            emptyLayout.setOrientation(LinearLayout.VERTICAL);
            emptyLayout.setGravity(android.view.Gravity.CENTER);
            emptyLayout.setPadding(40, 100, 40, 100);
            
            TextView emptyIcon = new TextView(this);
            emptyIcon.setText("📅");
            emptyIcon.setTextSize(48);
            emptyIcon.setGravity(android.view.Gravity.CENTER);
            emptyLayout.addView(emptyIcon);
            
            TextView emptyTitle = new TextView(this);
            emptyTitle.setText("No Schedules Yet");
            emptyTitle.setTextSize(20);
            emptyTitle.setTextColor(getResources().getColor(android.R.color.white));
            emptyTitle.setGravity(android.view.Gravity.CENTER);
            emptyTitle.setPadding(0, 16, 0, 8);
            emptyLayout.addView(emptyTitle);
            
            TextView emptyText = new TextView(this);
            emptyText.setText("Create your first schedule to automatically lock your device at specific times");
            emptyText.setTextSize(14);
            emptyText.setTextColor(getResources().getColor(R.color.light_gray));
            emptyText.setGravity(android.view.Gravity.CENTER);
            emptyText.setPadding(0, 0, 0, 24);
            emptyLayout.addView(emptyText);
            
            scheduleContainer.addView(emptyLayout);
        } else {
            // Load all schedules
            for (int i = 0; i < scheduleCount; i++) {
                ScheduleItem schedule = loadSchedule(i);
                if (schedule != null) {
                    addScheduleView(schedule, i);
                }
            }
        }
    }
    
    private ScheduleItem loadSchedule(int index) {
        String prefix = SCHEDULE_PREFIX + index + "_";
        
        String label = prefs.getString(prefix + "label", "");
        if (label.isEmpty()) return null;
        
        Set<String> days = prefs.getStringSet(prefix + "days", new HashSet<>());
        int startHour = prefs.getInt(prefix + "start_hour", 9);
        int startMinute = prefs.getInt(prefix + "start_minute", 0);
        int endHour = prefs.getInt(prefix + "end_hour", 17);
        int endMinute = prefs.getInt(prefix + "end_minute", 0);
        
        return new ScheduleItem(label, days, startHour, startMinute, endHour, endMinute);
    }
    
    private void addScheduleView(ScheduleItem schedule, int index) {
        // Create schedule card
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setBackgroundResource(R.drawable.schedule_card_background);
        cardLayout.setPadding(16, 16, 16, 16);
        cardLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        
        // Margin for spacing
        LinearLayout.LayoutParams marginParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        marginParams.setMargins(16, 8, 16, 8);
        cardLayout.setLayoutParams(marginParams);
        
        // Label
        TextView labelText = new TextView(this);
        labelText.setText(schedule.label);
        labelText.setTextSize(18);
        labelText.setTextColor(getResources().getColor(android.R.color.black));
        labelText.setTypeface(null, android.graphics.Typeface.BOLD);
        cardLayout.addView(labelText);
        
        // Time range
        TextView timeText = new TextView(this);
        String startTime = String.format("%02d:%02d", schedule.startHour, schedule.startMinute);
        String endTime = String.format("%02d:%02d", schedule.endHour, schedule.endMinute);
        timeText.setText("Time: " + startTime + " - " + endTime);
        timeText.setTextSize(14);
        timeText.setTextColor(getResources().getColor(android.R.color.darker_gray));
        timeText.setPadding(0, 8, 0, 8);
        cardLayout.addView(timeText);
        
        // Days
        TextView daysText = new TextView(this);
        daysText.setText("Days: " + getDaysText(schedule.days));
        daysText.setTextSize(14);
        daysText.setTextColor(getResources().getColor(android.R.color.darker_gray));
        daysText.setPadding(0, 0, 0, 16);
        cardLayout.addView(daysText);
        
        // Buttons row
        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        
        // Edit button
        Button editButton = new Button(this);
        editButton.setText("Edit");
        editButton.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        ));
        editButton.setOnClickListener(v -> {
            long lastSet = prefs.getLong("schedule_set_time", 0);
            long now = System.currentTimeMillis();
            if (now - lastSet < RESTRICT_DAYS) {
                Toast.makeText(this, "Schedule 15 din tak edit nahi ho sakta!", Toast.LENGTH_LONG).show();
            } else {
                editSchedule(index);
            }
        });
        buttonRow.addView(editButton);
        
        // Delete button
        Button deleteButton = new Button(this);
        deleteButton.setText("Delete");
        deleteButton.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        ));
        deleteButton.setOnClickListener(v -> {
            long lastSet = prefs.getLong("schedule_set_time", 0);
            long now = System.currentTimeMillis();
            if (now - lastSet < RESTRICT_DAYS) {
                Toast.makeText(this, "Schedule 15 din tak delete nahi ho sakta!", Toast.LENGTH_LONG).show();
            } else {
                deleteSchedule(index);
            }
        });
        buttonRow.addView(deleteButton);
        
        cardLayout.addView(buttonRow);
        scheduleContainer.addView(cardLayout);
    }
    
    private String getDaysText(Set<String> days) {
        StringBuilder sb = new StringBuilder();
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        String[] dayCodes = {"S", "M", "T", "W", "R", "F", "A"};
        
        for (int i = 0; i < dayCodes.length; i++) {
            if (days.contains(dayCodes[i])) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(dayNames[i]);
            }
        }
        
        return sb.toString();
    }
    
    private void editSchedule(int index) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("edit_index", index);
        startActivity(intent);
    }
    
    private void deleteSchedule(int index) {
        // Delete schedule data
        String prefix = SCHEDULE_PREFIX + index + "_";
        SharedPreferences.Editor editor = prefs.edit();
        
        editor.remove(prefix + "label");
        editor.remove(prefix + "days");
        editor.remove(prefix + "start_hour");
        editor.remove(prefix + "start_minute");
        editor.remove(prefix + "end_hour");
        editor.remove(prefix + "end_minute");
        
        // Shift remaining schedules
        int scheduleCount = prefs.getInt(SCHEDULE_COUNT, 0);
        for (int i = index; i < scheduleCount - 1; i++) {
            String oldPrefix = SCHEDULE_PREFIX + (i + 1) + "_";
            String newPrefix = SCHEDULE_PREFIX + i + "_";
            
            String label = prefs.getString(oldPrefix + "label", "");
            Set<String> days = prefs.getStringSet(oldPrefix + "days", new HashSet<>());
            int startHour = prefs.getInt(oldPrefix + "start_hour", 9);
            int startMinute = prefs.getInt(oldPrefix + "start_minute", 0);
            int endHour = prefs.getInt(oldPrefix + "end_hour", 17);
            int endMinute = prefs.getInt(oldPrefix + "end_minute", 0);
            
            editor.putString(newPrefix + "label", label);
            editor.putStringSet(newPrefix + "days", days);
            editor.putInt(newPrefix + "start_hour", startHour);
            editor.putInt(newPrefix + "start_minute", startMinute);
            editor.putInt(newPrefix + "end_hour", endHour);
            editor.putInt(newPrefix + "end_minute", endMinute);
            
            editor.remove(oldPrefix + "label");
            editor.remove(oldPrefix + "days");
            editor.remove(oldPrefix + "start_hour");
            editor.remove(oldPrefix + "start_minute");
            editor.remove(oldPrefix + "end_hour");
            editor.remove(oldPrefix + "end_minute");
        }
        
        editor.putInt(SCHEDULE_COUNT, scheduleCount - 1);
        editor.apply();
        
        Toast.makeText(this, "Schedule delete ho gaya!", Toast.LENGTH_SHORT).show();
        loadSchedules(); // Refresh the list
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadSchedules(); // Refresh when returning from MainActivity
    }
    
    // Helper class to store schedule data
    private static class ScheduleItem {
        String label;
        Set<String> days;
        int startHour, startMinute, endHour, endMinute;
        
        ScheduleItem(String label, Set<String> days, int startHour, int startMinute, int endHour, int endMinute) {
            this.label = label;
            this.days = days;
            this.startHour = startHour;
            this.startMinute = startMinute;
            this.endHour = endHour;
            this.endMinute = endMinute;
        }
    }
} 