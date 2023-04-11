package com.example.covidalert;

import static com.example.covidalert.Constants.BASE_URL;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

public class BluetoothScanService extends Service {
    private static final String TAG = "BluetoothScanService";
    private static final int NOTIFICATION_ID = 1;
    public static final String BROADCAST_ACTION = "com.example.covidalert.UPDATE_UI";

    private static final String CHANNEL_ID = "BluetoothScanServiceChannel";
    private static final String CHANNEL_ID_ALERT = "CovidAlertChannel";
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler = new Handler();
    private Runnable mPeriodicScanRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground();
//        else
//            startForeground(NOTIFICATION_ID, getNotification());
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(BROADCAST_ACTION));

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startPeriodicScan();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            mBluetoothAdapter.cancelDiscovery();
            mHandler.removeCallbacks(mPeriodicScanRunnable);
            unregisterReceiver(mReceiver);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                try {
                    String deviceInfo = device.getName() + " - " + device.getAddress();
                    Log.d(TAG, "Device found: " + deviceInfo);
                    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

                    String licenceNumber = sharedPreferences.getString("dl_number", null);

                    if (licenceNumber != null) {
                        try {

                            AdvertisingIdClient.Info adInfo = null;
                            try {
                                adInfo = AdvertisingIdClient.getAdvertisingIdInfo(getApplicationContext());
                            } catch (Exception e) {
                                Log.e(TAG, "Error getting Advertising ID: " + e.getMessage());
                            }

                            String advertisingId = null;
                            if (adInfo != null) {
                                advertisingId = adInfo.getId();
                            } else {
                                advertisingId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                            }

                            String url = BASE_URL + "/contactHistory";
                            RequestQueue requestQueue = Volley.newRequestQueue(Objects.requireNonNull(context));

                            JSONObject jsonBody = new JSONObject();
                            LocalDate currentDate = LocalDate.now();
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                            String formattedDate = currentDate.format(formatter);
                            jsonBody.put("userId", advertisingId);
                            jsonBody.put("licenseNumber", licenceNumber);
                            jsonBody.put("contactDate", formattedDate);
                            jsonBody.put("contactDeviceId", device.getAddress());

                            String requestBody = jsonBody.toString();

                            StringRequest stringRequest = new StringRequest(Request.Method.POST, url, response -> {
                                Log.d("RESPONSE CONTACT HISTORY Galaxy", "response => " + response);
                                Gson gson = new Gson();
                                Type type = new TypeToken<Map<String, Object>>() {}.getType();
                                Map<String, Object> contactInfo = gson.fromJson(response, type);

                                Intent intent1 = new Intent(BROADCAST_ACTION);
                                String message = String.format("Contacted with %s person on %s", contactInfo.get("contactVaccineStatus"), contactInfo.get("contactDate").toString().substring(0, 10));
                                intent1.putExtra("message", message);
//                                getApplicationContext().sendBroadcast(intent1);
                                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent1);
                            }, error -> Log.d("ERROR", "")) {
                                @Override
                                public String getBodyContentType() {
                                    return "application/json; charset=utf-8";
                                }

                                @Override
                                public byte[] getBody() {
                                    return requestBody.getBytes(StandardCharsets.UTF_8);
                                }
                            };

//                            stringRequest.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 48, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

                            requestQueue.add(stringRequest);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Bluetooth Scan Service";
            String description = "Scanning for nearby Bluetooth devices…";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification getNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Bluetooth Scan Service")
                .setContentText("Scanning for nearby Bluetooth devices…")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setTicker("Bluetooth Scan Service")
                .build();
    }

    private void startPeriodicScan() {
        mPeriodicScanRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    mBluetoothAdapter.startDiscovery();
                    createNotificationChannel();
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
                mHandler.postDelayed(this, 60000); // Run every 1 minute (60000 milliseconds)
            }
        };
        mHandler.post(mPeriodicScanRunnable);
    }

    private void startMyOwnForeground() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            String NOTIFICATION_CHANNEL_ID = "com.example.covidalert";
            String channelName = "My Background Service";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setContentTitle("App is running in background")
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();
            startForeground(2, notification);
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("Broadcast received");
            String message = intent.getStringExtra("message");
            CharSequence name = "Covid Contact Alert";
            String description = message;
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_ALERT, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Covid Contact Alert")
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            notificationManager.notify(1, builder.build());
        }
    };
}


