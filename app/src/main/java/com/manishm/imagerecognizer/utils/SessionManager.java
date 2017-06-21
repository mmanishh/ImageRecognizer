package com.manishm.imagerecognizer.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;




import java.util.HashMap;

public class SessionManager {
    // Shared Preferences
    SharedPreferences pref;

    // Editor for Shared preferences
    Editor editor;

    // Context
    Context _context;

    // Shared pref mode
    int PRIVATE_MODE = 0;

    // Sharedpref file name
    public static final String PREF_NAME = "ImageRecognizer";

    // All Shared Preferences Keys
    public static final String TOGGLE_VALUE = "value";




    // Constructor
    public SessionManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME,PRIVATE_MODE);
        editor = pref.edit();

    }

    /**
     * Create login session
     */
    public void storeValue(boolean value) {
        // Storing login value as TRUE
        editor.putBoolean(TOGGLE_VALUE, value);
        // commit changes
        editor.commit();
    }


    public boolean getValue(String key)
    {
        return pref.getBoolean(key,true);
    }


}
