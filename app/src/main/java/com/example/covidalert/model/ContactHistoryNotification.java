package com.example.covidalert.model;

//  {
//    "userId": "17e1609f-91d2-477a-8b8a-2e05899a53c9",
//    "licenseNumber": "Y6950111",
//    "contactDate": "2023-04-13T00:00:00.000Z",
//    "contactDeviceId": "5e946fc8-d788-4cc7-87e0-e25e676b6fc0",
//    "contactVaccineStatus": "PARTIALLY VACCINATED"
//  }

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ContactHistoryNotification {
    private String userId;
    private String licenseNumber;
    private String contactDate;
    private String contactDeviceId;
    private String contactVaccineStatus;

    public ContactHistoryNotification(String userId, String licenseNumber, String contactDate, String contactDeviceId, String contactVaccineStatus) {
        this.userId = userId;
        this.licenseNumber = licenseNumber;
        this.contactDate = contactDate;
        this.contactDeviceId = contactDeviceId;
        this.contactVaccineStatus = contactVaccineStatus;
    }

    public String getUserId() {
        return userId;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public String getContactDate() {
        // Return date in format: 2021-04-16
        return contactDate.substring(0, 10);
    }

    public String getContactDeviceId() {
        return contactDeviceId;
    }

    public String getContactVaccineStatus() {
        return contactVaccineStatus;
    }

    public String getTimeElapsed() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        try {
            Date inputDate = sdf.parse(this.contactDate);
            Date currentDate = new Date();

            long diffInMillis = Math.abs(currentDate.getTime() - inputDate.getTime());
            long diffInSec = TimeUnit.MILLISECONDS.toSeconds(diffInMillis);
            long diffInMin = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
            long diffInHours = TimeUnit.MILLISECONDS.toHours(diffInMillis);
            long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);
            long diffInWeeks = diffInDays / 7;

            if (diffInWeeks > 0) {
                return diffInWeeks + " weeks ago";
            } else if (diffInDays > 0) {
                return diffInDays + " days ago";
            } else if (diffInHours > 0) {
                return diffInHours + " hours ago";
            } else if (diffInMin > 0) {
                return diffInMin + " minutes ago";
            } else {
                return diffInSec + " seconds ago";
            }

        } catch (ParseException e) {
            e.printStackTrace();
            return "Invalid date format";
        }
    }

    public String getStatus() {
        if(this.contactVaccineStatus.equals("PARTIALLY VACCINATED"))
            return "Partially Vaccinated";
        else if(this.contactVaccineStatus.equals("NOT VACCINATED"))
            return "Not Vaccinated";
        else
            return "Unknown";
    }

    public void setContactDate(String contactDate) {
        this.contactDate = contactDate;
    }

    public String getContactTime() {
        // Return time in format: 2021-04-16T00:00:00.000Z
        return contactDate.substring(11, 19);
    }
}
