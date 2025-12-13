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
    
    private TextView durationText;
    private SeekBar durationSeekBar;
    private Button duration5m, duration10m, duration15m, duration30m;
    private Button startLockButton;
    private ImageButton backButton;
    
    private int selectedDuration = 5; // Default 5 minutes
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_duration);
        
        initializeViews();
        setupClickListeners();
        updateDurationDisplay();
    }
    
    private void initializeViews() {
        durationText = findViewById(R.id.durationText);
        durationSeekBar = findViewById(R.id.durationSeekBar);
        duration5m = findViewById(R.id.duration5m);
        duration10m = findViewById(R.id.duration10m);
        duration15m = findViewById(R.id.duration15m);
        duration30m = findViewById(R.id.duration30m);
        startLockButton = findViewById(R.id.startLockButton);
        backButton = findViewById(R.id.backButton);
    }
    
    private void setupClickListeners() {
        // Duration preset buttons
        duration5m.setOnClickListener(v -> setDuration(5));
        duration10m.setOnClickListener(v -> setDuration(10));
        duration15m.setOnClickListener(v -> setDuration(15));
        duration30m.setOnClickListener(v -> setDuration(30));
        
        // SeekBar listener
        durationSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    selectedDuration = progress;
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
        
        // Test alarm button (long press on start lock button)
        startLockButton.setOnLongClickListener(v -> {
            ScheduleManager.setTestAlarm(this);
            Toast.makeText(this, "Test alarm set for next minute!", Toast.LENGTH_SHORT).show();
            return true;
        });
        
        // Direct lock test (double tap on start lock button)
        startLockButton.setOnClickListener(new View.OnClickListener() {
            private long lastClickTime = 0;
            @Override
            public void onClick(View v) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastClickTime < 500) { // Double tap within 500ms
                    // Direct lock test
                    DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                    ComponentName compName = new ComponentName(LockDurationActivity.this, MyDeviceAdminReceiver.class);
                    
                    if (dpm.isAdminActive(compName)) {
                        try {
                            dpm.lockNow();
                            Toast.makeText(LockDurationActivity.this, "Direct lock activated!", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(LockDurationActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(LockDurationActivity.this, "Device admin not active!", Toast.LENGTH_LONG).show();
                    }
                    lastClickTime = 0; // Reset
                } else {
                    lastClickTime = currentTime;
                }
            }
        });
    }
    
    private void setDuration(int minutes) {
        selectedDuration = minutes;
        durationSeekBar.setProgress(minutes);
        updateDurationDisplay();
        updatePresetButtons();
    }
    
    private void updateDurationDisplay() {
        durationText.setText("Minutes: " + selectedDuration);
    }
    
    private void updatePresetButtons() {
        // Reset all buttons
        duration5m.setBackgroundResource(R.drawable.duration_button_background);
        duration10m.setBackgroundResource(R.drawable.duration_button_background);
        duration15m.setBackgroundResource(R.drawable.duration_button_background);
        duration30m.setBackgroundResource(R.drawable.duration_button_background);
        
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
        }
    }
    
    private void startImmediateLock() {
        // Check device admin
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName compName = new ComponentName(this, MyDeviceAdminReceiver.class);
        
        if (!dpm.isAdminActive(compName)) {
            Toast.makeText(this, "Pehle Device Admin activate karein!", Toast.LENGTH_LONG).show();
            Intent intent = new Intent("android.settings.DEVICE_ADMIN_SETTINGS");
            startActivity(intent);
            return;
        }
        
        // Start the lock service
        Intent serviceIntent = new Intent(this, LockService.class);
        serviceIntent.putExtra("duration", selectedDuration);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        Toast.makeText(this, "Device locked for " + selectedDuration + " minutes!", Toast.LENGTH_SHORT).show();
        finish();
    }
} 