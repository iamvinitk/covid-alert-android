package com.example.covidalert;

import static com.example.covidalert.Constants.BASE_URL;
import static java.security.AccessController.getContext;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.covidalert.model.ContactHistoryNotification;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class NotificationListActivity extends AppCompatActivity {
    private ArrayList<ContactHistoryNotification> notifications;
    private NotificationAdapter mAdapter;
    private RecyclerView recyclerView;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_list);

        // Set the title of the activity
        setTitle("Notifications");

        notifications = new ArrayList<>();
        notifications.add(new ContactHistoryNotification("1", "A12345", "2023-04-16", "DeviceID1", "Partially Vaccinated"));
        notifications.add(new ContactHistoryNotification("2", "B67890", "2023-04-15", "DeviceID2", "Fully Vaccinated"));

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recycler_view_notifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        textView = findViewById(R.id.text_view_no_notifications);

        // Initialize adapter and set it to RecyclerView
        mAdapter = new NotificationAdapter(notifications);
        recyclerView.setAdapter(mAdapter);

        SharedPreferences sharedPreferences = Objects.requireNonNull(this).getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        String licenceNumber = sharedPreferences.getString("dl_number", null);

        String url = BASE_URL + "/contactHistory/notifications/" + licenceNumber;
        System.out.println("URL: " + url);
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, response -> {
            Log.d("RESPONSE", "response => " + response);
            runOnUiThread(() -> {
                Gson gson = new Gson();
                ContactHistoryNotification[] newNotifications = gson.fromJson(response, ContactHistoryNotification[].class);

                notifications.clear();
                // add the new notifications
                notifications.addAll(Arrays.asList(newNotifications));
                // notify the adapter
                if (notifications.size() == 0) {
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

        if (notifications.size() == 0) {
            textView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            textView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    public class NotificationViewHolder extends RecyclerView.ViewHolder {
        private TextView contactDate;
        private TextView contactTimeElapsed;
        private TextView contactVaccineStatus;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            contactDate = itemView.findViewById(R.id.notification_date);
            contactTimeElapsed = itemView.findViewById(R.id.notification_time_elapsed);
            contactVaccineStatus = itemView.findViewById(R.id.notification_status_value);
        }

        public void bind(ContactHistoryNotification notification) {
            contactDate.setText(notification.getContactDate());
            contactTimeElapsed.setText(notification.getTimeElapsed());
            contactVaccineStatus.setText(notification.getContactVaccineStatus());
        }
    }

    public class NotificationAdapter extends RecyclerView.Adapter<NotificationViewHolder> {
        private List<ContactHistoryNotification> notifications;

        public NotificationAdapter(List<ContactHistoryNotification> notifications) {
            this.notifications = notifications;
        }

        @NonNull
        @Override
        public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_item, parent, false);
            return new NotificationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
            holder.bind(notifications.get(position));
        }

        @Override
        public int getItemCount() {
            return notifications.size();
        }
    }
}

