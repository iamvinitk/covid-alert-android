package com.example.covidalert;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.covidalert.model.ContactHistoryNotification;

import java.util.List;

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

