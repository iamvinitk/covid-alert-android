package com.example.covidalert;

import static com.example.covidalert.Constants.BASE_URL;

import androidx.appcompat.app.AppCompatActivity;

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
import com.example.covidalert.model.DriverDetails;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class VaccineEdit extends AppCompatActivity {

    private final String TAG = "VaccineEdit";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vaccine_edit);

        Intent intent = getIntent();
        String firstName = intent.getStringExtra("vaccine_first_name");
        String lastName = intent.getStringExtra("vaccine_last_name");
        String dob = intent.getStringExtra("vaccine_dob");
        String firstDoseDate = intent.getStringExtra("vaccine_first_dose_date");
        String secondDoseDate = intent.getStringExtra("vaccine_second_dose_date");
        String otherDoseDate = intent.getStringExtra("vaccine_other_dose_date");

        String firstDoseManufacturer = intent.getStringExtra("vaccine_first_dose_manufacturer");
        String secondDoseManufacturer = intent.getStringExtra("vaccine_second_dose_manufacturer");
        String otherDoseManufacturer = intent.getStringExtra("vaccine_other_dose_manufacturer");

        EditText firstNameEditText = findViewById(R.id.vaccine_edit_given_name);
        EditText lastNameEditText = findViewById(R.id.vaccine_edit_family_name);
        EditText dobEditText = findViewById(R.id.vaccine_edit_dob);
        EditText firstDoseDateEditText = findViewById(R.id.vaccine_edit_first_dose_date);
        EditText secondDoseDateEditText = findViewById(R.id.vaccine_edit_second_dose_date);
        EditText otherDoseDateEditText = findViewById(R.id.vaccine_edit_other_dose_date);
        EditText firstDoseManufacturerEditText = findViewById(R.id.vaccine_edit_first_dose_manufacturer);
        EditText secondDoseManufacturerEditText = findViewById(R.id.vaccine_edit_second_dose_manufacturer);
        EditText otherDoseManufacturerEditText = findViewById(R.id.vaccine_edit_other_dose_manufacturer);

        Button submitButton = findViewById(R.id.vaccine_edit_submit_btn);

        firstNameEditText.setText(firstName);
        lastNameEditText.setText(lastName);
        dobEditText.setText(dob);
        firstDoseDateEditText.setText(firstDoseDate);
        secondDoseDateEditText.setText(secondDoseDate);
        otherDoseDateEditText.setText(otherDoseDate);
        firstDoseManufacturerEditText.setText(firstDoseManufacturer);
        secondDoseManufacturerEditText.setText(secondDoseManufacturer);
        otherDoseManufacturerEditText.setText(otherDoseManufacturer);

        submitButton.setOnClickListener(view -> {
            String updatedFirstName = firstNameEditText.getText().toString();
            String updatedLastName = lastNameEditText.getText().toString();
            String updatedDob = dobEditText.getText().toString();
            String updatedFirstDoseDate = firstDoseDateEditText.getText().toString();
            String updatedSecondDoseDate = secondDoseDateEditText.getText().toString();
            String updatedOtherDoseDate = otherDoseDateEditText.getText().toString();
            String updatedFirstDoseManufacturer = firstDoseManufacturerEditText.getText().toString();
            String updatedSecondDoseManufacturer = secondDoseManufacturerEditText.getText().toString();
            String updatedOtherDoseManufacturer = otherDoseManufacturerEditText.getText().toString();

            SharedPreferences.Editor editor = getSharedPreferences("MyPrefs", MODE_PRIVATE).edit();
            editor.putBoolean("vaccineUploaded", true);
            editor.putString("vaccine_first_name", updatedFirstName);
            editor.putString("vaccine_last_name", updatedLastName);
            editor.putString("vaccine_dob", updatedDob);
            editor.putString("vaccine_first_dose_date", updatedFirstDoseDate);
            editor.putString("vaccine_first_dose_manufacture", updatedFirstDoseManufacturer);
            editor.putString("vaccine_second_dose_date", updatedSecondDoseDate);
            editor.putString("vaccine_second_dose_manufacture", updatedSecondDoseManufacturer);
            editor.putString("vaccine_other_dose_date", updatedOtherDoseDate);
            editor.putString("vaccine_other_dose_manufacture", updatedOtherDoseManufacturer);
            editor.apply();

            RequestQueue requestQueue = Volley.newRequestQueue(this);

            String url = BASE_URL + "/vaccine";
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
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("userId", advertisingId);
                jsonBody.put("givenName", updatedFirstName);
                jsonBody.put("familyName", updatedLastName);
                jsonBody.put("dateOfBirth", updatedDob);
                jsonBody.put("firstDoseDate", updatedFirstDoseDate);
                jsonBody.put("firstDoseManufacturer", updatedFirstDoseManufacturer);
                jsonBody.put("secondDoseDate", updatedSecondDoseDate);
                jsonBody.put("secondDoseManufacturer", updatedSecondDoseManufacturer);
                jsonBody.put("otherDoseDate", updatedOtherDoseDate);
                jsonBody.put("otherDoseManufacturer", updatedOtherDoseManufacturer);

                String requestBody = jsonBody.toString();

                StringRequest stringRequest = new StringRequest(Request.Method.POST, url, response -> {
                    Log.d("RESPONSE", "response => " + response);
                    runOnUiThread(this::gotoMainActivity);
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
        Toast.makeText(this, "Vaccine information saved", Toast.LENGTH_SHORT).show();
        finish();
//        Intent intent = new Intent(this, MainActivity.class);
//        startActivity(intent);
    }
}