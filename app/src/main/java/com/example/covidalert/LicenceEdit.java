package com.example.covidalert;

import static com.example.covidalert.Constants.BASE_URL;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.google.android.gms.ads.identifier.AdvertisingIdClient;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class LicenceEdit extends AppCompatActivity {

    private static final String TAG = "LicenceEdit";

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

            RequestQueue requestQueue = Volley.newRequestQueue(this);

            String url = BASE_URL + "/licence";
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

            try {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                String macAddress = advertisingId;
                if (bluetoothAdapter != null) {
                    macAddress = bluetoothAdapter.getAddress();
                }

                JSONObject jsonBody = new JSONObject();

                jsonBody.put("userId", advertisingId);
                jsonBody.put("licenseNumber", updatedDlNumber);
                jsonBody.put("deviceId", macAddress);
                jsonBody.put("fullName", updatedName);
                jsonBody.put("dateOfBirth", updatedDob);

                String requestBody = jsonBody.toString();

                StringRequest stringRequest = new StringRequest(Request.Method.POST, url, response -> {
                    Log.d("RESPONSE", "response => " + response);
                    runOnUiThread(() -> {
                        finish();
                        Toast.makeText(this, "Licence uploaded successfully", Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(this, ChooseDocument.class);
                        i.putExtra("documentType", "vaccine_certificate");
                        startActivity(i);
                    });
                }, error -> Log.d("ERROR", "error => " + error.toString())) {
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
        });

    }

    private void gotoMainActivity() {
        // Show Toast
        Toast.makeText(this, "Licence information saved", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}