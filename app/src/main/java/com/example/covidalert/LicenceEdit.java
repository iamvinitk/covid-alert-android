package com.example.covidalert;

import static com.example.covidalert.Constants.BASE_URL;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.covidalert.model.DriverDetails;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LicenceEdit extends AppCompatActivity {

    private static final String TAG = "LicenceEdit";

    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private String advertisingId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licence_edit);

        Intent intent = getIntent();
        String firstName = intent.getStringExtra("dl_first_name");
        String lastName = intent.getStringExtra("dl_last_name");
        String dob = intent.getStringExtra("dl_dob");
        String dlNumber = intent.getStringExtra("dl_number");

        EditText nameEditText = findViewById(R.id.licence_edit_licence_name);
        EditText dobEditText = findViewById(R.id.licence_edit_licence_dob);
        EditText dlNumberEditText = findViewById(R.id.licence_edit_licence_number);

        nameEditText.setText(String.format("%s %s", firstName, lastName));
        dobEditText.setText(dob);
        dlNumberEditText.setText(dlNumber);

        Button submitButton = findViewById(R.id.licence_edit_submit_btn);

        submitButton.setOnClickListener(view -> {
            String updatedName = nameEditText.getText().toString();
            String updatedDob = dobEditText.getText().toString();
            String updatedDlNumber = dlNumberEditText.getText().toString();

            SharedPreferences.Editor editor = getSharedPreferences("MyPrefs", MODE_PRIVATE).edit();
            editor.putBoolean("dlUploaded", true);
            editor.putString("dl_name", updatedName);
            editor.putString("dl_dob", updatedDob);
            editor.putString("dl_number", updatedDlNumber);
            editor.apply();

            DriverDetails driverDetails = new DriverDetails(firstName, lastName, dob, dlNumber, "", "", "", "", "", "");
            executorService.execute(postLicence(driverDetails));
        });
    }

    private void gotoMainActivity() {
        // Show Toast
        Toast.makeText(this, "Licence information saved", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private Runnable postLicence(DriverDetails driverDetails) {
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
                        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

                        String url = BASE_URL + "/licence";

                        try {
                            JSONObject jsonBody = new JSONObject();

                            jsonBody.put("userId", finalAdvertisingId);
                            jsonBody.put("licenseNumber", driverDetails.licenseNumber);
                            jsonBody.put("deviceId", finalAdvertisingId);
                            jsonBody.put("fullName", driverDetails.givenName + " " + driverDetails.familyName);
                            jsonBody.put("dateOfBirth", driverDetails.dateOfBirth);

                            String requestBody = jsonBody.toString();

                            StringRequest stringRequest = new StringRequest(Request.Method.POST, url, response -> {
                                Log.d("RESPONSE", "response => " + response);
                                runOnUiThread(() -> {
                                    Toast.makeText(getApplicationContext(), "Licence uploaded successfully", Toast.LENGTH_SHORT).show();
                                });
                            }, error -> {
                                Log.d("ERROR", "error => " + error.toString());
                                runOnUiThread(() -> {
                                    Toast.makeText(getApplicationContext(), "Error Uploading Licence. Try Again", Toast.LENGTH_SHORT).show();
                                });
                            }) {
                                @Override
                                public String getBodyContentType() {
                                    return "application/json; charset=utf-8";
                                }

                                @Override
                                public byte[] getBody() {
                                    return requestBody.getBytes(StandardCharsets.UTF_8);
                                }
                            };

                            stringRequest.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 48, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

                            requestQueue.add(stringRequest);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        };
    }

}