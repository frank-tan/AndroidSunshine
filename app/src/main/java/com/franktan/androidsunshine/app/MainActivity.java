package com.franktan.androidsunshine.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    public static final String LOG_TAG = "androidsunshine";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    }

}
