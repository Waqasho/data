package com.securelock;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {
    // Roman Urdu: Device admin activate/deactivate hone par messages
    @Override
    public void onEnabled(Context context, Intent intent) {
        Toast.makeText(context, "Device Admin activate ho gaya!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        Toast.makeText(context, "Device Admin deactivate ho gaya!", Toast.LENGTH_SHORT).show();
    }
} 