package com.example.covidalert;

import static com.example.covidalert.Constants.BASE_URL;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VaccineEdit extends AppCompatActivity {

    private final String TAG = "VaccineEdit";
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private String advertisingId = "";

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

            SharedPreferences sharedPreferences = Objects.requireNonNull(this).getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            boolean licenseUploaded = sharedPreferences.getBoolean("dlUploaded", false);
            String dl_name = sharedPreferences.getString("dl_name", "").toString();
            String dl_dob = sharedPreferences.getString("dl_dob", "").toString();

            if(licenseUploaded && dl_name != "" && dl_dob != ""){
                String[] arrname=  dl_name.split(" ");
                if(arrname.length>0){
                    String fname = arrname[0].trim().toLowerCase();
                    String lname = arrname[arrname.length-1].trim().toLowerCase();
                    if(!updatedFirstName.equalsIgnoreCase(fname) || !updatedLastName.equalsIgnoreCase(lname) || !updatedDob.equalsIgnoreCase(dl_dob)){
                        AlertDialog.Builder builder = new AlertDialog.Builder(VaccineEdit.this);
                        builder.setMessage("Vaccine Personal information do not match with License Details. Please Re-enter details correctly");
                        builder.setTitle("Alert!");
                        builder.setPositiveButton("OK", (DialogInterface.OnClickListener) (dialog, which) -> {
                            // When the user click yes button then app will close
                            dialog.cancel();
                        });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    }
                    else {
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

                        DriverDetails driverDetails = new DriverDetails(updatedFirstName, updatedLastName, updatedDob, "", updatedFirstDoseDate, updatedFirstDoseManufacturer, updatedSecondDoseDate, updatedSecondDoseManufacturer, updatedOtherDoseDate, updatedOtherDoseManufacturer);
                        executorService.execute(postVaccine(driverDetails));
                    }
                }

            }
            else{
                AlertDialog.Builder builder = new AlertDialog.Builder(VaccineEdit.this);
                builder.setMessage("Driver License Details do not exists!");
                builder.setTitle("Alert!");
                builder.setCancelable(false);
                builder.setPositiveButton("OK", (DialogInterface.OnClickListener) (dialog, which) -> {
                    // When the user click yes button then app will close
                    dialog.cancel();
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
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

    private Runnable postVaccine(DriverDetails driverDetails) {
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

                        String url = BASE_URL + "/vaccine";

                        try {
                            JSONObject jsonBody = new JSONObject();
                            jsonBody.put("userId", finalAdvertisingId);
                            jsonBody.put("givenName", driverDetails.givenName);
                            jsonBody.put("familyName", driverDetails.familyName);
                            jsonBody.put("dateOfBirth", driverDetails.dateOfBirth);
                            jsonBody.put("firstDoseDate", driverDetails.firstDoseDate);
                            jsonBody.put("firstDoseManufacturer", driverDetails.firstDoseManufacturer);
                            jsonBody.put("secondDoseDate", driverDetails.secondDoseDate);
                            jsonBody.put("secondDoseManufacturer", driverDetails.secondDoseManufacturer);
                            jsonBody.put("otherDoseDate", driverDetails.otherDoseDate);
                            jsonBody.put("otherDoseManufacturer", driverDetails.otherDoseManufacturer);

                            String requestBody = jsonBody.toString();

                            StringRequest stringRequest = new StringRequest(Request.Method.POST, url, response -> {
                                Log.d("RESPONSE", "response => " + response);
                                runOnUiThread(VaccineEdit.this::gotoMainActivity);
                            }, error -> {
                                Log.d("ERROR", "error => " + error.toString());
                                runOnUiThread(() -> {
                                    Toast.makeText(getApplicationContext(), "Vaccine upload failed. Try again", Toast.LENGTH_SHORT).show();
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