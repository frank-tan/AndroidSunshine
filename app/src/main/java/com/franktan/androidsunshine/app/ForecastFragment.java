package com.franktan.androidsunshine.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.franktan.androidsunshine.app.data.WeatherContract;

public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private SharedPreferences sharedPreferences;
    private ForecastAdapter mForecastAdapter;
    private ListView mForecastListView;
    private static final String LOG_TAG = "androidsunshine";
    private static final int FORECAST_LOADER_ID = 0;
    private ActivityCallback mParentActivity;
    private int mPosition = 0;
    private static String SELECTED_KEY = "POSITION";

    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // force any activity using this fragment to implement ActivityCallback
        try {
            mParentActivity = (ActivityCallback) activity;
        } catch (ClassCastException  e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ActivityCallback");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER_ID, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String locationSetting = Utility.getPreferredLocation(getActivity());
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting,
                System.currentTimeMillis()
        );
        return new CursorLoader(
                getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mForecastAdapter.swapCursor(cursor);
        if(mPosition != ListView.INVALID_POSITION) {
            mForecastListView.setSelection(mPosition);
            mForecastListView.setItemChecked(mPosition, true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mForecastAdapter.swapCursor(null);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);

        View view = inflater.inflate(R.layout.fragment_main, container, false);
        mForecastListView = (ListView) view.findViewById(R.id.listview_forecast);
        mForecastListView.setAdapter(mForecastAdapter);

        mForecastListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView adapterView, View view, int position, long l) {
                mPosition = position;

                String locationSetting = Utility.getPreferredLocation(getActivity());
                // move to detail weather activity
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(mPosition);
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                if (cursor == null) {
                    return;
                }

                if (mParentActivity.isTwoPaneDevice()) {
                    // notify activity to update detail weather pane
                    Uri selectItemUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationSetting, cursor.getLong(COL_WEATHER_DATE));
                    mParentActivity.onForecastItemSelected(selectItemUri);
                } else {
                    Intent intent = new Intent(getActivity(), WeatherDetailActivity.class)
                            .setData(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                                    locationSetting, cursor.getLong(COL_WEATHER_DATE)
                            ));
                    startActivity(intent);
                }


            }
        });

        // restore the active list item position to mPosition variable
        if(savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            int savedPosition = savedInstanceState.getInt(SELECTED_KEY);
            if(savedPosition >= 0) {
                mPosition = savedPosition;
            }
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save the active list item position for future restore
        if(mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    /**
     * Call the FeatchWeatherTask async task to get the latest weather forecast information
     */
    public void updateWeather () {
        FetchWeatherTask weatherTask = new FetchWeatherTask(getActivity());
        String location = Utility.getPreferredLocation(getActivity());
        weatherTask.execute(location);
    }

    public void onLocationChanged() {
        updateWeather();
        getLoaderManager().restartLoader(FORECAST_LOADER_ID, null, this);
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface ActivityCallback {

        public void onForecastItemSelected(Uri newUri);
        public boolean isTwoPaneDevice();
    }
}
