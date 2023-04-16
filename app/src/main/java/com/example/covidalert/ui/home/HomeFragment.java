package com.example.covidalert.ui.home;

import static com.example.covidalert.Constants.BASE_URL;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.covidalert.NotificationAdapter;
import com.example.covidalert.NotificationListActivity;
import com.example.covidalert.R;
import com.example.covidalert.databinding.FragmentHomeBinding;
import com.example.covidalert.model.ContactHistoryNotification;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private BarChart barChart;

    private FloatingActionButton filterButton;
    private Calendar startDate;
    private Calendar endDate;
    private String startDateString;
    private String endDateString;

    private String reportDateString;
    private Button applyButton;
    private Calendar reportCalendar;

    private ArrayList<ContactHistoryNotification> reportList;
    private NotificationAdapter mAdapter;
    private RecyclerView recyclerView;
    private TextView textView;

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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_icon) {
            startActivity(new Intent(getActivity(), NotificationListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        barChart = root.findViewById(R.id.barChart);

        applyButton = root.findViewById(R.id.applyButton);
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showReportDatePickerDialog();
            }
        });

        filterButton = root.findViewById(R.id.filterButton);
        filterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showStartDatePickerDialog();
            }
        });

        getData(null, null);

        // Report
        reportList = new ArrayList<>();
        textView = root.findViewById(R.id.home_no_notifications);
        recyclerView = root.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        // Initialize adapter and set it to RecyclerView
        mAdapter = new NotificationAdapter(reportList, 1);
        recyclerView.setAdapter(mAdapter);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),1);
        recyclerView.addItemDecoration(dividerItemDecoration);
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

    private void showStartDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        startDate = Calendar.getInstance();
                        startDate.set(year, month, dayOfMonth);
                        startDateString = year + "-" + (month + 1) + "-" + dayOfMonth;
                        showEndDatePickerDialog();
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.setTitle("Select Start Date");
        datePickerDialog.show();
    }

    private void showEndDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        endDate = Calendar.getInstance();
                        endDate.set(year, month, dayOfMonth);
                        endDateString = year + "-" + (month + 1) + "-" + dayOfMonth;
//                        updateBarChartData();
                        System.out.println("Start Date: " + startDateString);
                        System.out.println("End Date: " + endDateString);
                        getData(startDateString, endDateString);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.setTitle("Select End Date");

        datePickerDialog.show();
    }

    private void getData(String startDate, String endDate) {
        if(startDate == null && endDate == null) {
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
        } else {
            SharedPreferences sharedPreferences = Objects.requireNonNull(getActivity()).getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

            String licenceNumber = sharedPreferences.getString("dl_number", null);

            String url = BASE_URL + "/contactHistory/" + licenceNumber + "?startDate=" + startDate + "&endDate=" + endDate;
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
        }
    }

    private void showReportDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        reportCalendar = Calendar.getInstance();
                        reportCalendar.set(year, month, dayOfMonth);
                        reportDateString = year + "-" + (month + 1) + "-" + dayOfMonth;
                        // Update the recyclerview
                        System.out.println("Report Date: " + reportDateString);
                        updateReports();
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.setTitle("Select Report Date");
        datePickerDialog.show();
    }

    private void updateReports(){
        SharedPreferences sharedPreferences = Objects.requireNonNull(getActivity()).getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        String licenceNumber = sharedPreferences.getString("dl_number", null);

        String url = BASE_URL + "/contactHistory/notifications/" + licenceNumber + "?date=" + reportDateString;
        System.out.println("URL: " + url);
        RequestQueue requestQueue = Volley.newRequestQueue(getActivity());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, response -> {
            Log.d("RESPONSE", "response => " + response);
            Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                Gson gson = new Gson();
                ContactHistoryNotification[] newNotifications = gson.fromJson(response, ContactHistoryNotification[].class);

                reportList.clear();
                // add the new notifications
                reportList.addAll(Arrays.asList(newNotifications));
                // notify the adapter
                if (reportList.size() == 0) {
                    textView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    textView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
                mAdapter.notifyDataSetChanged();
            });
        }, error -> Log.d("ERROR", "error => " + error.toString())) {

        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 48, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(stringRequest);

        if (reportList.size() == 0) {
            textView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            textView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}