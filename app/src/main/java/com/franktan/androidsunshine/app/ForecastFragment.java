package com.franktan.androidsunshine.app;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment implements FetchWeatherAsyncTask.AcceptWeatherData {
    private SharedPreferences sharedPreferences;
    private ArrayAdapter<String> forecastAdapter;
    private ListView forecastListView;
    private static final String LOG_TAG = "androidsunshine";

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        forecastListView = (ListView) view.findViewById(R.id.listview_forecast);
        forecastListView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String text = forecastAdapter.getItem(position);

                        Intent goToDetailPage = new Intent(getActivity(), WeatherDetailActivity.class);
                        goToDetailPage.putExtra("detailWeatherText", text);
                        startActivity(goToDetailPage);
                    }
                }
        );

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
        //inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id){
            case R.id.action_settings:
                Intent i = new Intent(getActivity(), SettingsActivity.class);
                startActivity(i);
                return true;
            case R.id.action_get_location:
                String postCode = sharedPreferences.getString(
                        getString(R.string.pref_location_key),
                        getString(R.string.pref_location_default));
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                intent.setData(Uri.parse("geo:0,0?q=" + postCode));
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                }
                return true;
            case R.id.action_refresh:
                updateWeather();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void setServerData(String[] weatherDataArray){
        ArrayList<String> resultArrayList = new ArrayList<String>(Arrays.asList(weatherDataArray));
        if(forecastAdapter == null) {
            forecastAdapter = new ArrayAdapter<String>(getActivity(),
                    R.layout.list_item_forcast, R.id.list_item_forecast_textview, resultArrayList);

            forecastListView.setAdapter(forecastAdapter);
        } else {
            forecastAdapter.clear();
            forecastAdapter.addAll(resultArrayList);
        }
    }
    private void updateWeather () {
        String postCode = sharedPreferences.getString(
                getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));
        new FetchWeatherAsyncTask(this,getActivity().getBaseContext()).execute(postCode);
    }

}
