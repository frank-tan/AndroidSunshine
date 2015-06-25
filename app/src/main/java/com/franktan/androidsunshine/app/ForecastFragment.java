package com.franktan.androidsunshine.app;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;


/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    ArrayAdapter<String> adapter;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        new FetchWeatherAsyncTask(this).execute("94043");
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_refresh){
            new FetchWeatherAsyncTask(this).execute("94043");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void setServerData(String[] result){

        adapter = new ArrayAdapter<String>(getActivity(),
                    R.layout.list_item_forcast, R.id.list_item_forecast_textview, result);

        ListView forecastListView = (ListView) getView().findViewById(R.id.listview_forecast);
        forecastListView.setAdapter(adapter);

    }
}
