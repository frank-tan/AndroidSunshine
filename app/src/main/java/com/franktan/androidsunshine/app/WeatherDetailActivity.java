package com.franktan.androidsunshine.app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


public class WeatherDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_detail);

        if(savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.weather_detail_container, new WeatherDetailFragment())
                    .commit();
        }
    }

}
