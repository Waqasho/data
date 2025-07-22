package com.securelock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;

public class LockDurationActivity extends AppCompatActivity {
    
    private TextView durationText, titleText, descriptionText;
    private SeekBar durationSeekBar;
    private Button startLockButton;
    private Button duration15min, duration30min, duration1h, duration2h, duration3h;
    private ImageButton backButton;
    private int selectedDuration = 30; // Default 30 minutes
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_duration);
        
        initializeViews();
        setupClickListeners();
        updateDurationDisplay();
    }
    
    private void initializeViews() {
        titleText = findViewById(R.id.titleText);
        descriptionText = findViewById(R.id.descriptionText);
        durationText = findViewById(R.id.durationText);
        durationSeekBar = findViewById(R.id.durationSeekBar);
        startLockButton = findViewById(R.id.startLockButton);
        backButton = findViewById(R.id.backButton);
        
        // Preset buttons
        duration15min = findViewById(R.id.duration15m);
        duration30min = findViewById(R.id.duration30m);
        duration1h = findViewById(R.id.duration1h);
        duration2h = findViewById(R.id.duration2h);
        duration3h = findViewById(R.id.duration3h);
        
        // Set seekbar range (1-180 minutes)
        if (durationSeekBar != null) {
            durationSeekBar.setMax(179); // 0-179 represents 1-180
            durationSeekBar.setProgress(selectedDuration - 1);
        }
    }
    
    private void setupClickListeners() {
        // Preset duration buttons
        if (duration15min != null) duration15min.setOnClickListener(this::onDuration15minClick);
        if (duration30min != null) duration30min.setOnClickListener(this::onDuration30minClick);
        if (duration1h != null) duration1h.setOnClickListener(this::onDuration1hClick);
        if (duration2h != null) duration2h.setOnClickListener(this::onDuration2hClick);
        if (duration3h != null) duration3h.setOnClickListener(this::onDuration3hClick);
        
        // SeekBar listener with null checks
        if (durationSeekBar != null) {
            durationSeekBar.setOnSeekBarChangeListener(new SeekBarChangeListener());
        }
        
        // Start lock button with null check
        if (startLockButton != null) {
            startLockButton.setOnClickListener(this::onStartLockClick);
        }
        
        // Back button with null check
        if (backButton != null) {
            backButton.setOnClickListener(this::onBackClick);
        }
    }
    
    // Separate click methods to avoid anonymous inner class issues
    private void onDuration15minClick(View v) {
        setDuration(15);
    }
    
    private void onDuration30minClick(View v) {
        setDuration(30);
    }
    
    private void onDuration1hClick(View v) {
        setDuration(60);
    }
    
    private void onDuration2hClick(View v) {
        setDuration(120);
    }
    
    private void onDuration3hClick(View v) {
        setDuration(180);
    }
    
    private void onStartLockClick(View v) {
        startImmediateLock();
    }
    
    private void onBackClick(View v) {
        finish();
    }
    
    // Separate SeekBar listener class
    private class SeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser && seekBar != null) {
                selectedDuration = progress + 1; // Convert 0-179 to 1-180
                updateDurationDisplay();
                updatePresetButtons();
            }
        }
        
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}
        
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    }
    
    private void setDuration(int minutes) {
        selectedDuration = minutes;
        if (durationSeekBar != null) {
            durationSeekBar.setProgress(minutes - 1); // Convert to 0-179 range
        }
        updateDurationDisplay();
        updatePresetButtons();
    }
    
    private void updateDurationDisplay() {
        if (durationText == null) return;
        
        String displayText;
        if (selectedDuration < 60) {
            displayText = selectedDuration + " Minutes";
        } else {
            int hours = selectedDuration / 60;
            int minutes = selectedDuration % 60;
            if (minutes == 0) {
                displayText = hours + " Hour" + (hours > 1 ? "s" : "");
            } else {
                displayText = hours + "h " + minutes + "m";
            }
        }
        durationText.setText("⏱️ Duration: " + displayText);
    }
    
    private void updatePresetButtons() {
        // Reset all button states
        resetButtonState(duration15min);
        resetButtonState(duration30min);
        resetButtonState(duration1h);
        resetButtonState(duration2h);
        resetButtonState(duration3h);
        
        // Highlight selected duration
        Button selectedButton = null;
        switch (selectedDuration) {
            case 15: selectedButton = duration15min; break;
            case 30: selectedButton = duration30min; break;
            case 60: selectedButton = duration1h; break;
            case 120: selectedButton = duration2h; break;
            case 180: selectedButton = duration3h; break;
        }
        
        if (selectedButton != null) {
            selectedButton.setBackgroundResource(R.color.blue_accent);
        }
    }
    
    private void resetButtonState(Button button) {
        if (button != null) {
            button.setBackgroundResource(R.color.dark_button);
        }
    }
    
    private void startImmediateLock() {
        // Check device admin first
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(this, MyDeviceAdminReceiver.class);
        
        if (dpm == null || !dpm.isAdminActive(adminComponent)) {
            Toast.makeText(this, "⚠️ Device admin not enabled! Please enable it first.", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Start the lock service
        Intent serviceIntent = new Intent(this, LockService.class);
        serviceIntent.putExtra("duration_minutes", selectedDuration);
        serviceIntent.putExtra("is_immediate", true);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        Toast.makeText(this, "🔒 Device will be locked for " + getFormattedDuration(), Toast.LENGTH_LONG).show();
        finish();
    }
    
    private String getFormattedDuration() {
        if (selectedDuration < 60) {
            return selectedDuration + " minutes";
        } else {
            int hours = selectedDuration / 60;
            int minutes = selectedDuration % 60;
            if (minutes == 0) {
                return hours + " hour" + (hours > 1 ? "s" : "");
            } else {
                return hours + "h " + minutes + "m";
            }
        }
    }
} 