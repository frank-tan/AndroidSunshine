package com.franktan.androidsunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.franktan.androidsunshine.app.data.WeatherContract;


public class WeatherDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private SharedPreferences sharedPreferences;
    private ShareActionProvider mShareActionProvider;
    private static final int FORECAST_LOADER_ID = 0;

    TextView mDayView, mHighView, mLowView, mForecastView, mHumidityView, mWindView, mPressureView;
    ImageView mIconView;

    private static final String[] FORECAST_COLUMNS = {
        WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
        WeatherContract.WeatherEntry.COLUMN_DATE,
        WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
        WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
        WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
        WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
        WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
        WeatherContract.LocationEntry.COLUMN_COORD_LAT,
        WeatherContract.LocationEntry.COLUMN_COORD_LONG,
        WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
        WeatherContract.WeatherEntry.COLUMN_PRESSURE,
        WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
        WeatherContract.WeatherEntry.COLUMN_DEGREES
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
    static final int COLUMN_HUMIDITY = 9;
    static final int COLUMN_PRESSURE = 10;
    static final int COLUMN_WIND_SPEED = 11;
    static final int COLUMN_WIND_DEGREE = 12;

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

        mDayView = (TextView)view.findViewById(R.id.list_item_date_textview);
        mHighView = (TextView)view.findViewById(R.id.list_item_high_textview);
        mLowView = (TextView)view.findViewById(R.id.list_item_low_textview);
        mIconView = (ImageView)view.findViewById(R.id.list_item_icon);
        mForecastView = (TextView)view.findViewById(R.id.list_item_forecast_textview);
        mHumidityView = (TextView)view.findViewById(R.id.humidity_textview);
        mWindView = (TextView)view.findViewById(R.id.wind_textview);
        mPressureView = (TextView)view.findViewById(R.id.pressure_textview);

        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_weather_detail, menu);
        MenuItem item = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

        setShareIntent();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER_ID, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Intent intent = getActivity().getIntent();
        if(intent == null || intent.getData() == null) {
            return null;
        } else {
            return new CursorLoader(
                    getActivity(),
                    intent.getData(),
                    FORECAST_COLUMNS,
                    null,
                    null,
                    null
            );
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null && !cursor.moveToFirst()){
            return;
        }
        int weatherIcon = Utility.getArtResourceForWeatherCondition(cursor.getInt(COL_WEATHER_CONDITION_ID));
        String dateString = Utility.getFriendlyDayString(getActivity(), cursor.getLong(COL_WEATHER_DATE));
        String description = cursor.getString(COL_WEATHER_DESC);
        boolean isMetric = Utility.isMetric(getActivity());
        String high = Utility.formatTemperature(getActivity(), cursor.getDouble(COL_WEATHER_MAX_TEMP), isMetric);
        String low = Utility.formatTemperature(getActivity(), cursor.getDouble(COL_WEATHER_MIN_TEMP), isMetric);
        float humidity = cursor.getFloat(COLUMN_HUMIDITY);
        float pressure = cursor.getFloat(COLUMN_PRESSURE);
        float wind = cursor.getFloat(COLUMN_WIND_SPEED);
        float windDir = cursor.getFloat(COLUMN_WIND_DEGREE);

        mDayView.setText(dateString);
        mHighView.setText(high);
        mLowView.setText(low);
        mForecastView.setText(description);
        mHumidityView.setText(getActivity().getString(R.string.format_humidity, humidity));
        mWindView.setText(Utility.getFormattedWind(getActivity(), wind, windDir));
        mPressureView.setText(getActivity().getString(R.string.format_pressure, pressure));
        mIconView.setImageResource(weatherIcon);

        if(mShareActionProvider != null) {
            setShareIntent();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private void setShareIntent () {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        //shareIntent.putExtra(Intent.EXTRA_TEXT, mDetailWeatherText);
        shareIntent.setType("text/plain");
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }
}
