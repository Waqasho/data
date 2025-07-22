package com.securelock;

import android.app.TimePickerDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    // UI Elements
    private TextView startTimeText, startTimeAMPM, endTimeText, endTimeAMPM;
    private LinearLayout startTimeContainer, endTimeContainer;
    private Button daySunday, dayMonday, dayTuesday, dayWednesday, dayThursday, dayFriday, daySaturday;
    private EditText labelEditText;
    private Button saveButton, immediateLockButton, testLockButton;
    private ImageButton backButton, viewSchedulesButton;
    
    // Data
    private SharedPreferences prefs;
    private Set<String> selectedDays;
    private int startHour = 9, startMinute = 0;
    private int endHour = 17, endMinute = 0;
    private boolean startIsAM = true, endIsAM = false;
    
    private static final String PREFS_NAME = "SecureLockPrefs";
    private static final String SCHEDULE_SET_TIME = "schedule_set_time";
    private static final String SELECTED_DAYS = "selected_days";
    private static final long RESTRICT_DAYS = 15 * 24 * 60 * 60 * 1000L; // 15 din

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupClickListeners();
        loadSavedData();
    }

    private void initializeViews() {
        // Time elements
        startTimeText = findViewById(R.id.startTimeText);
        startTimeAMPM = findViewById(R.id.startTimeAMPM);
        endTimeText = findViewById(R.id.endTimeText);
        endTimeAMPM = findViewById(R.id.endTimeAMPM);
        startTimeContainer = findViewById(R.id.startTimeContainer);
        endTimeContainer = findViewById(R.id.endTimeContainer);
        
        // Day buttons
        daySunday = findViewById(R.id.daySunday);
        dayMonday = findViewById(R.id.dayMonday);
        dayTuesday = findViewById(R.id.dayTuesday);
        dayWednesday = findViewById(R.id.dayWednesday);
        dayThursday = findViewById(R.id.dayThursday);
        dayFriday = findViewById(R.id.dayFriday);
        daySaturday = findViewById(R.id.daySaturday);
        
        // Other elements
        labelEditText = findViewById(R.id.labelEditText);
        saveButton = findViewById(R.id.saveButton);
        immediateLockButton = findViewById(R.id.immediateLockButton);
        testLockButton = findViewById(R.id.testLockButton);
        backButton = findViewById(R.id.backButton);
        viewSchedulesButton = findViewById(R.id.viewSchedulesButton);
        
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        selectedDays = new HashSet<>();
    }

    private void setupClickListeners() {
        // Time pickers
        startTimeContainer.setOnClickListener(v -> showTimePicker(true));
        endTimeContainer.setOnClickListener(v -> showTimePicker(false));
        
        // Day selection
        daySunday.setOnClickListener(v -> toggleDay("S", daySunday));
        dayMonday.setOnClickListener(v -> toggleDay("M", dayMonday));
        dayTuesday.setOnClickListener(v -> toggleDay("T", dayTuesday));
        dayWednesday.setOnClickListener(v -> toggleDay("W", dayWednesday));
        dayThursday.setOnClickListener(v -> toggleDay("R", dayThursday));
        dayFriday.setOnClickListener(v -> toggleDay("F", dayFriday));
        daySaturday.setOnClickListener(v -> toggleDay("A", daySaturday));
        
        // Save button - simplified to avoid conflicts
        saveButton.setOnClickListener(v -> saveSchedule());
        
        // Immediate lock button
        immediateLockButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, LockDurationActivity.class);
            startActivity(intent);
        });
        
        // Back button
        backButton.setOnClickListener(v -> finish());
        
        // View schedules button
        viewSchedulesButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScheduleListActivity.class);
            startActivity(intent);
        });
        
        // Test lock button (long press on save button)
        saveButton.setOnLongClickListener(v -> {
            ScheduleManager.setTestAlarm(this);
            Toast.makeText(this, "1-minute test alarm set!", Toast.LENGTH_SHORT).show();
            return true;
        });
        
        // Test lock button
        testLockButton.setOnClickListener(v -> {
            ScheduleManager.testLockNow(this);
            Toast.makeText(this, "Test lock triggered!", Toast.LENGTH_SHORT).show();
        });
        
        // Long press on test button for immediate test alarm
        testLockButton.setOnLongClickListener(v -> {
            ScheduleManager.setImmediateTestAlarm(this);
            Toast.makeText(this, "30-second test alarm set!", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    private void showTimePicker(boolean isStartTime) {
        TimePickerDialog.OnTimeSetListener timeSetListener = (view, hourOfDay, minute) -> {
            if (isStartTime) {
                startHour = hourOfDay;
                startMinute = minute;
                updateTimeDisplay(true);
            } else {
                endHour = hourOfDay;
                endMinute = minute;
                updateTimeDisplay(false);
            }
        };

        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            timeSetListener,
            isStartTime ? startHour : endHour,
            isStartTime ? startMinute : endMinute,
            false // 24-hour format
        );
        timePickerDialog.show();
    }

    private void updateTimeDisplay(boolean isStartTime) {
        if (isStartTime) {
            int displayHour = startHour;
            startIsAM = startHour < 12;
            if (startHour == 0) displayHour = 12;
            else if (startHour > 12) displayHour = startHour - 12;
            startTimeText.setText(String.format("%02d:%02d", displayHour, startMinute));
            startTimeAMPM.setText(startIsAM ? "AM" : "PM");
        } else {
            int displayHour = endHour;
            endIsAM = endHour < 12;
            if (endHour == 0) displayHour = 12;
            else if (endHour > 12) displayHour = endHour - 12;
            endTimeText.setText(String.format("%02d:%02d", displayHour, endMinute));
            endTimeAMPM.setText(endIsAM ? "AM" : "PM");
        }
    }

    private void toggleDay(String day, Button button) {
        if (selectedDays.contains(day)) {
            selectedDays.remove(day);
            button.setBackgroundResource(R.drawable.day_button_background);
        } else {
            selectedDays.add(day);
            button.setBackgroundResource(R.drawable.day_button_selected);
        }
    }

    private void loadSavedData() {
        // Check if editing existing schedule
        int editIndex = getIntent().getIntExtra("edit_index", -1);
        
        if (editIndex >= 0) {
            // Load existing schedule data
            String prefix = "schedule_" + editIndex + "_";
            
            // Load selected days
            Set<String> savedDays = prefs.getStringSet(prefix + "days", null);
            if (savedDays != null) {
                selectedDays = new HashSet<>(savedDays);
                updateDayButtons();
            }
            
            // Load times
            startHour = prefs.getInt(prefix + "start_hour", 9);
            startMinute = prefs.getInt(prefix + "start_minute", 0);
            endHour = prefs.getInt(prefix + "end_hour", 17);
            endMinute = prefs.getInt(prefix + "end_minute", 0);
            updateTimeDisplay(true);
            updateTimeDisplay(false);
            
            // Load label
            String savedLabel = prefs.getString(prefix + "label", "");
            labelEditText.setText(savedLabel);
        } else {
            // Load default data for new schedule
            Set<String> savedDays = prefs.getStringSet(SELECTED_DAYS, null);
            if (savedDays != null) {
                selectedDays = new HashSet<>(savedDays);
                updateDayButtons();
            }
            
            startHour = prefs.getInt("start_hour", 9);
            startMinute = prefs.getInt("start_minute", 0);
            endHour = prefs.getInt("end_hour", 17);
            endMinute = prefs.getInt("end_minute", 0);
            updateTimeDisplay(true);
            updateTimeDisplay(false);
            
            String savedLabel = prefs.getString("label", "");
            labelEditText.setText(savedLabel);
        }
    }

    private void updateDayButtons() {
        daySunday.setBackgroundResource(selectedDays.contains("S") ? R.drawable.day_button_selected : R.drawable.day_button_background);
        dayMonday.setBackgroundResource(selectedDays.contains("M") ? R.drawable.day_button_selected : R.drawable.day_button_background);
        dayTuesday.setBackgroundResource(selectedDays.contains("T") ? R.drawable.day_button_selected : R.drawable.day_button_background);
        dayWednesday.setBackgroundResource(selectedDays.contains("W") ? R.drawable.day_button_selected : R.drawable.day_button_background);
        dayThursday.setBackgroundResource(selectedDays.contains("R") ? R.drawable.day_button_selected : R.drawable.day_button_background);
        dayFriday.setBackgroundResource(selectedDays.contains("F") ? R.drawable.day_button_selected : R.drawable.day_button_background);
        daySaturday.setBackgroundResource(selectedDays.contains("A") ? R.drawable.day_button_selected : R.drawable.day_button_background);
    }

    private void saveSchedule() {
        // Debug: Show selectedDays before validation
        Toast.makeText(this, "selectedDays: " + selectedDays, Toast.LENGTH_LONG).show();
        android.util.Log.d("DEBUG", "selectedDays: " + selectedDays);
        // Check if device admin is active
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName compName = new ComponentName(this, MyDeviceAdminReceiver.class);
        
        if (!dpm.isAdminActive(compName)) {
            Toast.makeText(this, "Pehle Device Admin activate karein! Settings > Security > Device Administrators mein jaayein.", Toast.LENGTH_LONG).show();
            // Open device admin settings
            Intent intent = new Intent("android.settings.DEVICE_ADMIN_SETTINGS");
            startActivity(intent);
            return;
        }

        // Validate inputs
        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "Kam se kam ek din select karein!", Toast.LENGTH_LONG).show();
            return;
        }

        String label = labelEditText.getText().toString().trim();
        if (label.isEmpty()) {
            Toast.makeText(this, "Label zaroori hai!", Toast.LENGTH_LONG).show();
            return;
        }

        // Get edit index if editing existing schedule
        int editIndex = getIntent().getIntExtra("edit_index", -1);
        
        // Save schedule
        SharedPreferences.Editor editor = prefs.edit();
        
        if (editIndex >= 0) {
            // Editing existing schedule
            String prefix = "schedule_" + editIndex + "_";
            editor.putString(prefix + "label", label);
            editor.putStringSet(prefix + "days", new HashSet<>(selectedDays));
            editor.putInt(prefix + "start_hour", startHour);
            editor.putInt(prefix + "start_minute", startMinute);
            editor.putInt(prefix + "end_hour", endHour);
            editor.putInt(prefix + "end_minute", endMinute);
        } else {
            // Adding new schedule
            int scheduleCount = prefs.getInt("schedule_count", 0);
            String prefix = "schedule_" + scheduleCount + "_";
            editor.putString(prefix + "label", label);
            editor.putStringSet(prefix + "days", new HashSet<>(selectedDays));
            editor.putInt(prefix + "start_hour", startHour);
            editor.putInt(prefix + "start_minute", startMinute);
            editor.putInt(prefix + "end_hour", endHour);
            editor.putInt(prefix + "end_minute", endMinute);
            editor.putInt("schedule_count", scheduleCount + 1);
        }
        
        editor.apply();

        // Set up alarms for selected days
        ScheduleManager.setLockSchedule(this, startHour, startMinute, endHour, endMinute, selectedDays);

        Toast.makeText(this, "Schedule save ho gaya! Lock screen " + startHour + ":" + String.format("%02d", startMinute) + " pe lagega.", Toast.LENGTH_LONG).show();
        finish();
    }
} 