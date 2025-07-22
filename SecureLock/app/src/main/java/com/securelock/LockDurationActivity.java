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
    private Button duration5m, duration10m, duration15m, duration30m, duration1h, duration2h, duration3h;
    private Button startLockButton;
    private ImageButton backButton;
    
    private int selectedDuration = 30; // Default 30 minutes
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_duration);
        
        initializeViews();
        setupClickListeners();
        updateDurationDisplay();
        setupSeekBar();
    }
    
    private void initializeViews() {
        titleText = findViewById(R.id.titleText);
        descriptionText = findViewById(R.id.descriptionText);
        durationText = findViewById(R.id.durationText);
        durationSeekBar = findViewById(R.id.durationSeekBar);
        duration5m = findViewById(R.id.duration5m);
        duration10m = findViewById(R.id.duration10m);
        duration15m = findViewById(R.id.duration15m);
        duration30m = findViewById(R.id.duration30m);
        duration1h = findViewById(R.id.duration1h);
        duration2h = findViewById(R.id.duration2h);
        duration3h = findViewById(R.id.duration3h);
        startLockButton = findViewById(R.id.startLockButton);
        backButton = findViewById(R.id.backButton);
        
        // Set modern titles
        if (titleText != null) {
            titleText.setText("🔒 Immediate Lock");
        }
        if (descriptionText != null) {
            descriptionText.setText("Select duration and lock your device instantly");
        }
    }
    
    private void setupSeekBar() {
        // Set SeekBar range: 1 minute to 180 minutes (3 hours)
        durationSeekBar.setMax(179); // 0-179 = 1-180 minutes
        durationSeekBar.setProgress(selectedDuration - 1); // Set to 30 minutes (index 29)
    }
    
    private void setupClickListeners() {
        // Duration preset buttons
        duration5m.setOnClickListener(v -> setDuration(5));
        duration10m.setOnClickListener(v -> setDuration(10));
        duration15m.setOnClickListener(v -> setDuration(15));
        duration30m.setOnClickListener(v -> setDuration(30));
        duration1h.setOnClickListener(v -> setDuration(60));
        duration2h.setOnClickListener(v -> setDuration(120));
        duration3h.setOnClickListener(v -> setDuration(180));
        
        // SeekBar listener
        durationSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    selectedDuration = progress + 1; // Convert 0-179 to 1-180
                    updateDurationDisplay();
                    updatePresetButtons();
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Start lock button
        startLockButton.setOnClickListener(v -> startImmediateLock());
        
        // Back button
        backButton.setOnClickListener(v -> finish());
    }
    
    private void setDuration(int minutes) {
        selectedDuration = minutes;
        durationSeekBar.setProgress(minutes - 1); // Convert to 0-179 range
        updateDurationDisplay();
        updatePresetButtons();
    }
    
    private void updateDurationDisplay() {
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
        // Reset all buttons
        duration5m.setBackgroundResource(R.drawable.duration_button_background);
        duration10m.setBackgroundResource(R.drawable.duration_button_background);
        duration15m.setBackgroundResource(R.drawable.duration_button_background);
        duration30m.setBackgroundResource(R.drawable.duration_button_background);
        duration1h.setBackgroundResource(R.drawable.duration_button_background);
        duration2h.setBackgroundResource(R.drawable.duration_button_background);
        duration3h.setBackgroundResource(R.drawable.duration_button_background);
        
        // Highlight selected button
        switch (selectedDuration) {
            case 5:
                duration5m.setBackgroundResource(R.drawable.duration_button_selected);
                break;
            case 10:
                duration10m.setBackgroundResource(R.drawable.duration_button_selected);
                break;
            case 15:
                duration15m.setBackgroundResource(R.drawable.duration_button_selected);
                break;
            case 30:
                duration30m.setBackgroundResource(R.drawable.duration_button_selected);
                break;
            case 60:
                duration1h.setBackgroundResource(R.drawable.duration_button_selected);
                break;
            case 120:
                duration2h.setBackgroundResource(R.drawable.duration_button_selected);
                break;
            case 180:
                duration3h.setBackgroundResource(R.drawable.duration_button_selected);
                break;
        }
    }
    
    private void startImmediateLock() {
        // Check device admin
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName compName = new ComponentName(this, MyDeviceAdminReceiver.class);
        
        if (!dpm.isAdminActive(compName)) {
            Toast.makeText(this, "⚠️ Pehle Device Admin activate karein! Settings mein jaayein.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent("android.settings.DEVICE_ADMIN_SETTINGS");
            startActivity(intent);
            return;
        }
        
        // Create immediate lock intent
        Intent lockIntent = new Intent("com.securelock.LOCK_DEVICE");
        lockIntent.setComponent(new ComponentName(this, LockReceiver.class));
        lockIntent.putExtra("duration", selectedDuration);
        lockIntent.putExtra("label", "Immediate Lock (" + getDurationString() + ")");
        lockIntent.putExtra("is_immediate", true);
        
        // Send broadcast to start immediate lock
        sendBroadcast(lockIntent);
        
        String durationStr = getDurationString();
        Toast.makeText(this, "🔒 Immediate lock started for " + durationStr + "!", Toast.LENGTH_LONG).show();
        finish();
    }
    
    private String getDurationString() {
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