package com.daniele22.mlkitdemo;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;


public class SettingsActivity extends AppCompatActivity {

    public static final String KEY_LANDMARKS = "landmark_face";

    public static final String KEY_BBOX_FACE = "bbox_face";
    public static final String KEY_BBOX_EYE = "bbox_eye";
    public static final String KEY_BBOX_EYEBROW = "bbox_eyebrow";
    public static final String KEY_BBOX_NOSE = "bbox_nose";
    public static final String KEY_BBOX_LIP = "bbox_lip";

    public static final String KEY_CONTOUR_FACE = "contour_face";
    public static final String KEY_CONTOUR_EYE = "contour_eye";
    public static final String KEY_CONTOUR_EYEBROW = "contour_eyebrow";
    public static final String KEY_CONTOUR_NOSE = "contour_nose";
    public static final String KEY_CONTOUR_LIP = "contour_lip";

    public static final String KEY_ORIENTATION_AXIS = "draw_axis";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

}
