package com.franktan.androidsunshine.app.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.Time;
import android.util.Log;

import com.franktan.androidsunshine.app.Constants;
import com.franktan.androidsunshine.app.MainActivity;
import com.franktan.androidsunshine.app.R;
import com.franktan.androidsunshine.app.Utility;
import com.franktan.androidsunshine.app.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Vector;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    // Global variables
    // Define a variable to contain a content resolver instance
    ContentResolver mContentResolver;
    Context mContext;

    public static final long SYNC_INTERVAL =
            (long)3 * 60 * 60;
    public static final long
            SYNC_FLEXTIME = SYNC_INTERVAL/3L;
    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int WEATHER_NOTIFICATION_ID = 3004;

    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[] {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
    };
    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final int INDEX_SHORT_DESC = 3;

    /**
     * Set up the sync adapter
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContext = context;
        mContentResolver = context.getContentResolver();
    }

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    public SyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContext = context;
        mContentResolver = context.getContentResolver();

    }

    /*
         * Specify the code you want to run in the sync adapter. The entire
         * sync adapter runs in a background thread, so you don't have to set
         * up your own background processing.
         */
    @Override
    public void onPerformSync(
            Account account,
            Bundle extras,
            String authority,
            ContentProviderClient provider,
            SyncResult syncResult) {

        Log.i(Constants.LOG_TAG, "onPerformSync: actual sync happening...");

        String locationQuery = Utility.getPreferredLocation(mContext);

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

        String format = "json";
        String units = "metric";
        int numDays = 14;

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            final String FORECAST_BASE_URL =
                    "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, locationQuery)
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                    .build();

            URL url = new URL(builtUri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return;
            }
            forecastJsonStr = buffer.toString();
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(Constants.LOG_TAG, "Error closing stream", e);
                }
            }
        }

        try {
            saveWeatherData(forecastJsonStr, locationQuery);
            SyncAdapter.notifyWeather(mContext);
        } catch (JSONException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

    }

    /**
     * @param forecastJsonStr
     * @param locationSetting
     * @return
     * @throws JSONException
     */
    private String[] saveWeatherData(String forecastJsonStr,
                             String locationSetting)
            throws JSONException {

        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.

        // These are the names of the JSON objects that need to be extracted.

        // Location information
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";

        // Location coordinate
        final String OWM_LATITUDE = "lat";
        final String OWM_LONGITUDE = "lon";

        // Weather information.  Each day's forecast info is an element of the "list" array.
        final String OWM_LIST = "list";

        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        // All temperatures are children of the "temp" object.
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        final String OWM_WEATHER = "weather";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";

        try {
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
            String cityName = cityJson.getString(OWM_CITY_NAME);

            JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
            double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
            double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);

            long locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

            // Insert the new weather information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherArray.length());

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            for(int i = 0; i < weatherArray.length(); i++) {
                // These are the values that will be collected.
                long dateTime;
                double pressure;
                int humidity;
                double windSpeed;
                double windDirection;

                double high;
                double low;

                String description;
                int weatherId;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);

                pressure = dayForecast.getDouble(OWM_PRESSURE);
                humidity = dayForecast.getInt(OWM_HUMIDITY);
                windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
                windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

                // Description is in a child array called "weather", which is 1 element long.
                // That element also contains a weather code.
                JSONObject weatherObject =
                        dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                weatherId = weatherObject.getInt(OWM_WEATHER_ID);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                high = temperatureObject.getDouble(OWM_MAX);
                low = temperatureObject.getDouble(OWM_MIN);
                Log.i(Constants.LOG_TAG, String.valueOf(high));

                ContentValues weatherValues = new ContentValues();

                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateTime);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);

                cVVector.add(weatherValues);
            }
            ContentValues[] records;
            // add to database
            if ( cVVector.size() > 0 ) {
                records = new ContentValues[cVVector.size()];
                cVVector.toArray(records);
                mContext.getContentResolver().bulkInsert(
                        WeatherContract.WeatherEntry.CONTENT_URI,
                        records
                );
                deleteOldWeatherData();
            }

            return null;

        } catch (JSONException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return null;
    }

    private void deleteOldWeatherData (){
        Uri uri = WeatherContract.WeatherEntry.CONTENT_URI;

        Calendar date = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        date.add(Calendar.DATE, -1);
        Log.i(Constants.LOG_TAG,date.toString());

        mContext.getContentResolver().delete(
                uri,
                WeatherContract.WeatherEntry.COLUMN_DATE + " <= ?",
                new String[]{Long.toString(date.getTime().getTime())}
        );
    }

    private long addLocation(String locationSetting, String cityName, double lat, double lon) {
        // First, check if the location with this city name exists in the db
        Cursor cursor = mContext.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[]{locationSetting},
                null);
        Long locationUri;
        // If it exists, return the current ID
        if(cursor.moveToFirst()){
            int idColumnIndex = cursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            locationUri = cursor.getLong(idColumnIndex);
        } else {
            ContentValues contentValues = new ContentValues();
            contentValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME,cityName);
            contentValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT,lat);
            contentValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);
            contentValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            Uri uri = mContext.getContentResolver().insert(
                    WeatherContract.LocationEntry.CONTENT_URI,
                    contentValues
            );
            locationUri =  ContentUris.parseId(uri);
        }
        cursor.close();
        return locationUri;
    }

    /**
     * Create a new dummy account for the sync adapter
     *
     * @param context The application context
     */
    private static Account configSyncAccount(Context context) {
        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(
                        Context.ACCOUNT_SERVICE);

        // If the password doesn't exist, the account doesn't exist, so add the account
        //if ( null == accountManager.getPassword(newAccount) ) {
        if (accountManager.getAccountsByType(context.getString(R.string.sync_account_type)).length == 0) {
            Log.i(Constants.LOG_TAG,"account does not exist. Add new account ");
            /*
             * Add the account and account type, no password or user data
             * If successful, return the Account object, otherwise report an error.
             */
            if (!accountManager.addAccountExplicitly(newAccount, null, null)) {
                /*
                 * If you don't set android:syncable="true" in
                 * in your <provider> element in the manifest,
                 * then call context.setIsSyncable(account, AUTHORITY, 1)
                 * here.
                 */
                Log.i(Constants.LOG_TAG,"failed to add new account");
                return null;
            }
            //account added successfully, set periodical sync
            Log.i(Constants.LOG_TAG, "add new account successful");
            addPeriodicSync(context, newAccount);
            syncWeatherDataNow(context);
        }
        // if account already exists or added successfully, return the account
        return newAccount;
    }

    /**
     * Respond to a button click by calling requestSync(). This is an
     * asynchronous operation.
     *
     * This method is attached to the refresh button in the layout
     * XML file
     *
     */
    public static void syncWeatherDataNow(Context context) {

        // Pass the settings flags by inserting them in a bundle
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        /*
         * Request the sync for the default account, authority, and
         * manual sync settings
         */
        Log.i(Constants.LOG_TAG,"Request sync");
        ContentResolver.requestSync(configSyncAccount(context), context.getString(R.string.content_authority), settingsBundle);
        Log.i(Constants.LOG_TAG, "sync requested");

    }

    private static void addPeriodicSync(Context context, Account account){
        Log.i(Constants.LOG_TAG, "addPeriodicSync");
        String authority = context.getString(R.string.content_authority);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(SYNC_INTERVAL, SYNC_FLEXTIME).
                    setSyncAdapter(account, authority).
                    setExtras(Bundle.EMPTY).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(
                    account,
                    authority,
                    Bundle.EMPTY,
                    SYNC_INTERVAL);
        }
        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(account, context.getString(R.string.content_authority), true);
    }

    /**
     * Check if sync account exists. If not, create one and schedule periodic sync.
     *
     * @param context
     */
    public static void initialize(Context context) {
        configSyncAccount(context);
    }

    public static void notifyWeather (Context context) {
        //check if we have sent notification today.
        //checking the last update and notify if it' the first of the day
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String lastNotificationKey = context.getString(R.string.pref_last_notification);
        long lastSync = prefs.getLong(lastNotificationKey, 0);
        boolean allowNotif = prefs.getBoolean(context.getString(R.string.pref_notif_key),false);

        if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS && allowNotif) {
            Cursor cursor = context.getContentResolver().query(
                    WeatherContract.WeatherEntry.buildWeatherLocationWithDate(Utility.getPreferredLocation(context), System.currentTimeMillis()),
                    NOTIFY_WEATHER_PROJECTION,
                    null,
                    null,
                    null);
            if(cursor.moveToFirst()) {
                String condition = cursor.getString(INDEX_SHORT_DESC);
                int weatherId = cursor.getInt(INDEX_WEATHER_ID);
                double high = cursor.getDouble(INDEX_MAX_TEMP);
                String highTemp = Utility.formatTemperature(context, high, Utility.isMetric(context));
                double low = cursor.getDouble(INDEX_MIN_TEMP);
                String lowTemp = Utility.formatTemperature(context, low, Utility.isMetric(context));

                String notificationText = context.getString(R.string.format_notification, condition, highTemp, lowTemp);
                int icon = Utility.getIconResourceForWeatherCondition(weatherId);

                sendNotification(context, "Today's Weather", notificationText, icon);

                //refreshing last sync
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong(lastNotificationKey, System.currentTimeMillis());
                editor.commit();
            }
            cursor.close();
        }
    }

    public static void sendNotification(Context context, String title, String text, int icon) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(icon)
                        .setContentTitle(title)
                        .setContentText(text);
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, MainActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setAutoCancel(true);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(WEATHER_NOTIFICATION_ID, mBuilder.build());


    }
}