package com.example.covidalert.ui.home;

import static com.example.covidalert.Constants.BASE_URL;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.covidalert.R;
import com.example.covidalert.databinding.FragmentHomeBinding;
import com.example.covidalert.model.ContactHistory;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private BarChart barChart;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        barChart = root.findViewById(R.id.barChart);
        SharedPreferences sharedPreferences = Objects.requireNonNull(getActivity()).getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        String licenceNumber = sharedPreferences.getString("dl_number", null);

        String url = BASE_URL + "/contactHistory/" + licenceNumber;
        System.out.println("URL: " + url);
        RequestQueue requestQueue = Volley.newRequestQueue(getActivity());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, response -> {
            Log.d("RESPONSE", "response => " + response);
            getActivity().runOnUiThread(() -> {
                updateGraph(response);
            });
        }, error -> Log.d("ERROR", "error => " + error.toString())) {

        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 48, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(stringRequest);


        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private String getDate(int daysAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, daysAgo);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d");
        return dateFormat.format(calendar.getTime());
    }

    private void updateGraph(String response) {
        ArrayList<BarEntry> partiallyVaccinated = new ArrayList<>();
        ArrayList<BarEntry> notVaccinated = new ArrayList<>();
        ArrayList<String> dates = new ArrayList<>();

        Gson gson = new Gson();

        try {
            // Parse the JSON string into a Map
            Type type = new com.google.gson.reflect.TypeToken<Map<String, Map<String, Integer>>>() {
            }.getType();
            Map<String, Map<String, Integer>> data = gson.fromJson(response, type);

            // Loop through the map and print the values
            int i = 0;
            for (Map.Entry<String, Map<String, Integer>> entry : data.entrySet()) {
                String date = entry.getKey();
                Map<String, Integer> vaccineStatus = entry.getValue();
                System.out.println("Date: " + date);
                for (Map.Entry<String, Integer> status : vaccineStatus.entrySet()) {
                    if (status.getKey().equals("PARTIALLY VACCINATED"))
                        partiallyVaccinated.add(new BarEntry(i, status.getValue()));
                    else if (status.getKey().equals("NOT VACCINATED"))
                        notVaccinated.add(new BarEntry(i, status.getValue()));
                    System.out.println("Vaccine Status: " + status.getKey() + ", Count: " + status.getValue());
                    dates.add(date);
                    i++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set up the bar chart data sets
        BarDataSet maleDataSet = new BarDataSet(notVaccinated, "Not Vaccinated");
        maleDataSet.setColor(Color.BLUE);
        maleDataSet.setValueTextColor(Color.BLACK);
        BarDataSet femaleDataSet = new BarDataSet(partiallyVaccinated, "Partially Vaccinated");
        femaleDataSet.setColor(Color.RED);
        femaleDataSet.setValueTextColor(Color.BLACK);

        // Set up the bar chart data
        BarData barData = new BarData(maleDataSet, femaleDataSet);
        barData.setValueTextSize(12f);
        barData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) {
                    return "";
                } else {
                    return String.valueOf((int) value);
                }
            }
        });

        // Set up the bar chart description
        Description description = new Description();
        description.setText("Bar Chart Example");
        description.setTextSize(16f);
        barChart.setDescription(description);

        // Customize the bar chart appearance
        barChart.setDrawValueAboveBar(true);
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setPinchZoom(false);
        barChart.setDrawBarShadow(false);

        // Set up the x-axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dates));

        // Set up the y-axis
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        leftAxis.setDrawGridLines(false);
        leftAxis.setDrawAxisLine(false);
        barChart.getAxisRight().setEnabled(false);

        // Set the bar width and group spacing
        float groupSpace = 0.3f;
        float barSpace = 0.35f;
        float barWidth = 0.25f;
        barData.setBarWidth(barWidth);

        // Add the data to the chart and refresh it
        barChart.setData(barData);
        barChart.groupBars(0f, groupSpace, barSpace);
        barChart.invalidate();
    }
}