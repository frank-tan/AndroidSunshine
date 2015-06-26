package com.franktan.androidsunshine.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class WeatherDetailFragment extends Fragment {

    public WeatherDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather_detail, container, false);

        Intent intent = getActivity().getIntent();
        TextView detailWeatherText = (TextView) view.findViewById(R.id.detailWeatherText);
        detailWeatherText.setText(intent.getExtras().getString("detailWeatherText"));
        // Inflate the layout for this fragment
        return view;
    }

}
