package com.example.fubuki.short_distance_perception;

import android.text.TextUtils;

import java.util.List;

public class MyUtils {
    //string转float
    public static float convertToFloat(String number, float defaultValue) {
        if (TextUtils.isEmpty(number)) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(number);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
