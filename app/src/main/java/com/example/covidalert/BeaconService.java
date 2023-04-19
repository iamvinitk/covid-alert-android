package com.example.covidalert;

import static com.example.covidalert.Constants.BASE_URL;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;


import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collection;


public class BeaconService extends Service implements BeaconConsumer {
    private static final String CHANNEL_ID = "BeaconServiceChannel";
    private BeaconManager beaconManager;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private String advertisingId = "";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
//
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Bluetooth Service")
                .setContentText("Scanning for users...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(2, notification);
        Runnable advertisingIdRunnable = getAdvertisingId();
        executorService.execute(advertisingIdRunnable);

        // Receiver Code
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.ALTBEACON_LAYOUT));
        beaconManager.bind(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    beacons.forEach(b -> {
                        Log.i("BeaconID", b.getId1().toString());
                        String contactDeviceId = b.getId1().toString();
                        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

                        String licenceNumber = sharedPreferences.getString("dl_number", null);
                        System.out.println("licenceNumber: " + licenceNumber);
                        if (licenceNumber != null) {
                            System.out.println("advertisingId: " + advertisingId);
                            System.out.println("contactDeviceId: " + contactDeviceId);
                            System.out.println("licenceNumber: " + licenceNumber);

                            String url = BASE_URL + "/contactHistory";
                            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

                            try {


                                JSONObject jsonBody = new JSONObject();
                                LocalDateTime currentDate = LocalDateTime.now();
                                jsonBody.put("userId", advertisingId);
                                jsonBody.put("licenseNumber", licenceNumber);
                                jsonBody.put("contactDate", currentDate.toString());
                                jsonBody.put("contactDeviceId", contactDeviceId);

                                String requestBody = jsonBody.toString();
                                final int[] statusCode = {0};
                                StringRequest stringRequest = new StringRequest(Request.Method.POST, url, response -> {
                                    if (statusCode[0] == 201) {
                                        // Response Code
                                        Log.d("RESPONSE CONTACT HISTORY Galaxy", "response => " + response);
                                        Gson gson = new Gson();
                                        Type type = new TypeToken<Map<String, Object>>() {
                                        }.getType();
                                        Map<String, Object> contactInfo = gson.fromJson(response, type);

                                        if(!contactInfo.get("contactVaccineStatus").equals("FULLY VACCINATED")) {
                                            String message = String.format("Contacted with %s person on %s", contactInfo.get("contactVaccineStatus"), contactInfo.get("contactDate").toString().substring(0, 10));
                                            CharSequence name = "Covid Contact Alert";
                                            String description = message;
                                            int importance = NotificationManager.IMPORTANCE_DEFAULT;
                                            NotificationChannel channel = new NotificationChannel("CovidAlertChannel", name, importance);
                                            channel.setDescription(description);
                                            NotificationManager notificationManager = getSystemService(NotificationManager.class);
                                            notificationManager.createNotificationChannel(channel);

//                                        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
//                                                .setSmallIcon(android.R.drawable.ic_dialog_info)
//                                                .setContentTitle("Covid Contact Alert")
//                                                .setContentText(message)
//                                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                                                .setAutoCancel(true);

//                                        notificationManager.notify(2, builder.build());

//                                        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intentNotification, PendingIntent.FLAG_IMMUTABLE);

                                            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "CHANNEL_ID")
                                                    .setSmallIcon(R.drawable.ic_launcher_background)
                                                    .setContentTitle("Covid Contact Alert")
                                                    .setContentText(message)
                                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                                    // Set the intent that will fire when the user taps the notification
//                                                .setContentIntent(pendingIntent)
                                                    .setAutoCancel(true);

                                            notificationManager.notify(1, builder.build());
                                        }
                                    }
                                }, error -> Log.d("ERROR", "")) {
                                    @Override
                                    public String getBodyContentType() {
                                        return "application/json; charset=utf-8";
                                    }

                                    @Override
                                    public byte[] getBody() {
                                        return requestBody.getBytes(StandardCharsets.UTF_8);
                                    }

                                    @Override
                                    public Response<String> parseNetworkResponse(NetworkResponse networkResponse) {
                                        statusCode[0] = networkResponse.statusCode;
                                        return super.parseNetworkResponse(networkResponse);
                                    }
                                };

                                requestQueue.add(stringRequest);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Beacon Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    private Runnable getAdvertisingId() {
        return new Runnable() {
            @Override
            public void run() {
                AdvertisingIdClient.Info adInfo = null;
                try {
                    adInfo = AdvertisingIdClient.getAdvertisingIdInfo(getApplicationContext());
                } catch (Exception e) {
                    Log.e("TAG", "Error getting Advertising ID: " + e.getMessage());
                }

                if (adInfo != null) {
                    advertisingId = adInfo.getId();
                    String adTrackingEnabled = String.valueOf(adInfo.isLimitAdTrackingEnabled());
                    System.out.println("adTrackingEnabled: " + adTrackingEnabled);
                    System.out.println("advertisingId: " + advertisingId);

                } else {
                    advertisingId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                }

                final String finalAdvertisingId = advertisingId;
                mainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Update the UI with the advertisingId here
                        System.out.println("Advertising ID: " + finalAdvertisingId);
                        // Sender Code
                        Beacon beacon = new Beacon.Builder()
                                .setId1(finalAdvertisingId)
                                .setId2("1")
                                .setId3("2")
                                .setManufacturer(0x0118) // Radius Networks. Change this for other beacon layouts
                                .setTxPower(-59)
                                .setDataFields(Arrays.asList(new Long[]{0l})) // Remove this for beacon layouts without d: fields
                                .build();

                        BeaconParser beaconParser = new BeaconParser()
                                .setBeaconLayout(BeaconParser.ALTBEACON_LAYOUT);
                        BeaconTransmitter beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser);

                        beaconTransmitter.startAdvertising(beacon, new AdvertiseCallback() {

                            @Override
                            public void onStartFailure(int errorCode) {
                                Log.e("TAG", "Advertisement start failed with code: " + errorCode);
                            }

                            @Override
                            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
//                                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "CHANNEL_ID")
//                                        .setSmallIcon(R.drawable.ic_launcher_background)
//                                        .setContentTitle("Covid Contact Alert")
//                                        .setContentText("message")
//                                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                                        // Set the intent that will fire when the user taps the notification
////                                                .setContentIntent(pendingIntent)
//                                        .setAutoCancel(true);
//                                NotificationChannel channel = new NotificationChannel("CovidAlertChannel", "name", NotificationManager.IMPORTANCE_DEFAULT);
//                                channel.setDescription("description");
//
//                                NotificationManager notificationManager = getSystemService(NotificationManager.class);
//                                notificationManager.createNotificationChannel(channel);
//                                notificationManager.notify(1, builder.build());
                                Log.i("TAG", "Advertisement start succeeded.");
                            }
                        });
                    }
                });
            }
        };
    }
}
