package com.example.iot_lab7_20212607.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.iot_lab7_20212607.models.User;

public class PreferenceManager {
    private static final String PREF_NAME = "BusTicketsPrefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_BALANCE = "userBalance";

    private final SharedPreferences preferences;

    public PreferenceManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveUserData(User user, String userId) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_ROLE, user.getRole());
        editor.putString(KEY_USER_NAME, user.getName());
        editor.putString(KEY_USER_EMAIL, user.getEmail());
        editor.putFloat(KEY_USER_BALANCE, (float) user.getBalance());
        editor.apply();
    }

    public String getUserId() {
        return preferences.getString(KEY_USER_ID, "");
    }

    public String getUserRole() {
        return preferences.getString(KEY_USER_ROLE, "");
    }

    public String getUserName() {
        return preferences.getString(KEY_USER_NAME, "");
    }

    public double getUserBalance() {
        return preferences.getFloat(KEY_USER_BALANCE, 0f);
    }

    public void updateBalance(double newBalance) {
        preferences.edit().putFloat(KEY_USER_BALANCE, (float) newBalance).apply();
    }

    public void clearUserData() {
        preferences.edit().clear().apply();
    }
}