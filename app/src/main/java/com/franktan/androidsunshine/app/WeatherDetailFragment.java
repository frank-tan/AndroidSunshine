package com.franktan.androidsunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.ShareActionProvider;
import android.widget.TextView;


public class WeatherDetailFragment extends Fragment {
    private SharedPreferences sharedPreferences;
    private ShareActionProvider mShareActionProvider;
    String detailWeatherText;

    public WeatherDetailFragment() {
        // Required empty public constructor
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
        View view = inflater.inflate(R.layout.fragment_weather_detail, container, false);

        Intent intent = getActivity().getIntent();
        detailWeatherText = intent.getExtras().getString("detailWeatherText");
        TextView detailWeatherTextView = (TextView) view.findViewById(R.id.detailWeatherText);
        detailWeatherTextView.setText(detailWeatherText);
        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.i(MainActivity.LOG_TAG,"before inflate");
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_weather_detail, menu);
        Log.i(MainActivity.LOG_TAG,"after inflate");
        MenuItem item = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        Log.i(MainActivity.LOG_TAG,"after getting ShareActionProvider");

        setShareIntent();
    }

    private void setShareIntent () {
        Log.i(MainActivity.LOG_TAG,"in setShareIntent");
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.putExtra(Intent.EXTRA_TEXT, detailWeatherText);
        shareIntent.setType("text/plain");
        Log.i(MainActivity.LOG_TAG, "checking mShareActionProvider ");
        if (mShareActionProvider != null) {
            Log.i(MainActivity.LOG_TAG,"to setShareIntent");
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }
}
