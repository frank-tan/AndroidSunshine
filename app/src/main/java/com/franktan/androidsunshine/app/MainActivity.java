package com.franktan.androidsunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;


public class MainActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    public static final String LOG_TAG = "androidsunshine";
    private final String FORECASTFRAGMENT_TAG = "FORECASTFRAGMENT_TAG";
    private String mLocation;
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        // if R.id.weather_detail_container exist, it means it is on a screen larger than 600dp on
        // the smaller edge
        if(findViewById(R.id.weather_detail_container) != null) {
            mTwoPane = true;
            // if it is not saved. Otherwise let Android system handle it
            if(savedInstanceState == null){
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container,new WeatherDetailFragment())
                        .commit();
            }
        } else {
            // smaller screen
            mTwoPane = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        String location = Utility.getPreferredLocation( this );
//        if (location != null && !location.equals(mLocation)) {
//            ForecastFragment ff = (ForecastFragment)getSupportFragmentManager().findFragmentByTag(FORECASTFRAGMENT_TAG);
//            if(ff != null) {
//                ff.onLocationChanged();
//            }
//        }
//        mLocation = location;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);
        menuInflater.inflate(R.menu.forecastfragment, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id){
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;
            case R.id.action_get_location:
                String postCode = sharedPreferences.getString(
                        getString(R.string.pref_location_key),
                        getString(R.string.pref_location_default));
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                intent.setData(Uri.parse("geo:0,0?q=" + postCode));
                if (intent.resolveActivity(this.getPackageManager()) != null) {
                    startActivity(intent);
                }
                return true;
            case R.id.action_refresh:
                ForecastFragment ff = (ForecastFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);
                ff.updateWeather();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
