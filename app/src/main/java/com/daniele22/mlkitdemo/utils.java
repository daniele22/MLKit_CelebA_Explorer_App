package com.daniele22.mlkitdemo;

import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class utils {

    /**
     * Check if all permission are granted
     *
     * @return bool
     */
    public static boolean allPermissionsGranted(String[] required_permissions, Context context) {
        for (String permission : required_permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }



}
