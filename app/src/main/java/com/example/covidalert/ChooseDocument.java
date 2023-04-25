package com.example.covidalert;

import static com.example.covidalert.Constants.BASE_URL;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChooseDocument extends AppCompatActivity {
    private String mCurrentPhotoPath;
    private static final String TAG = "ChooseDocument";
    private ImageView previewImage;
    private String docUploadType;
    private Boolean isImageCaptured = false;
    private Bitmap mImageBitmap;
    private ProgressBar progressBar;
    private Button btn;
    private String advertisingId = "";
    private String fname = "";
    private String lname = "";
    private String dateOfBirth = "";
    private Boolean alertFlag = true;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_document);

        Intent intent = getIntent();
        String docType = intent.getStringExtra("documentType");
        System.out.println("Document Type: " + docType);
        previewImage = findViewById(R.id.choose_doc_img_preview);
        if (docType.equals("vaccine_certificate")) {
            docUploadType = "vaccine";
            previewImage.setImageResource(R.drawable.vaccine_placeholder);
        } else {
            docUploadType = "dl";
        }
        btn = findViewById(R.id.choose_doc_btn_upload);
        btn.setText(String.format("Upload %s", docType.replace("_", " ").toUpperCase(Locale.ROOT)));

        // Progress Bar
        progressBar = findViewById(R.id.choose_doc_progress_bar);

        //check app level permission is granted for Camera
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            //grant the permission
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);

        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        }

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (checkSelfPermission(Manifest.permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_MEDIA_LOCATION}, 101);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (checkSelfPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE}, 101);
            }
        }

        File photoFile = createImageFile();

        btn.setOnClickListener(v -> {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                    if (photoFile != null && !isImageCaptured) {
                        Uri uri = FileProvider.getUriForFile(ChooseDocument.this, BuildConfig.APPLICATION_ID + ".provider", photoFile);
                        System.out.println("photoURI: " + uri);
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                        someActivityResultLauncher.launch(cameraIntent);
                    }

//                    if (mImageBitmap != null && isImageCaptured) {
//                        progressBar.setVisibility(ProgressBar.VISIBLE);
//                        uploadImage(mImageBitmap);
//                    }
                }
        );
    }

    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        try {
                            isImageCaptured = true;
                            mImageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.parse(mCurrentPhotoPath));
                            previewImage.setImageBitmap(mImageBitmap);
                            btn.setText("Uploading...");
                            progressBar.setVisibility(ProgressBar.VISIBLE);
                            uploadImage(mImageBitmap);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });

    private File createImageFile() {
        File image = null;
        System.out.println("===================createImageFile======================");
        try {
            // Create an image file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES);
            image = File.createTempFile(
                    imageFileName,  // prefix
                    ".jpg",         // suffix
                    storageDir      // directory
            );

            // Save a file: path for use with ACTION_VIEW intents
            mCurrentPhotoPath = "file:" + image.getAbsolutePath();

        } catch (IOException ex) {
            Log.i(TAG, ex.getMessage());
        }
        System.out.println("image: " + image);
        return image;
    }

    private void uploadImage(Bitmap photo) {
        try {
            RequestQueue requestQueue = Volley.newRequestQueue(this);
            String url = BASE_URL + "/vision";

            String base64Str = convertImageToBase64(photo);
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("image", "data:image/jpeg;base64," + base64Str);
            jsonBody.put("type", docUploadType);

            String requestBody = jsonBody.toString();

            StringRequest stringRequest = new StringRequest(Request.Method.POST, url, response -> {
                DriverDetails driverDetails = new Gson().fromJson(response, new TypeToken<DriverDetails>() {
                }.getType());

                if (Objects.equals(docUploadType, "dl")) {
                    fname = driverDetails.givenName;
                    lname = driverDetails.familyName;
                    dateOfBirth = driverDetails.dateOfBirth;
                    String name = driverDetails.givenName + " " + driverDetails.familyName;
                    String dob = driverDetails.dateOfBirth;

                    LocalDate expdt = LocalDate.parse(driverDetails.expirationDate);
                    if (expdt.isBefore(LocalDate.now())) {
                        System.out.println("ExpDT:" + expdt);
                        AlertDialog.Builder builder = new AlertDialog.Builder(ChooseDocument.this);
                        builder.setMessage("Scanned licence is not valid. Please scan a valid licence.");
                        builder.setTitle("Alert!");
                        builder.setPositiveButton("OK", (DialogInterface.OnClickListener) (dialog, which) -> {
                            // When the user click yes button then app will close
                            btn.setText("Upload Licence");
                            isImageCaptured = false;
                            progressBar.setVisibility(ProgressBar.GONE);
                            dialog.cancel();
                        });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    } else {
                        String data = "Name: " + name + "\n" + "Date of Birth: " + dob;
                        runOnUiThread(() -> showAlert(data, driverDetails));
                    }
                }

                if (Objects.equals(docUploadType, "vaccine")) {
                    String name = driverDetails.givenName + " " + driverDetails.familyName;
                    String dob = driverDetails.dateOfBirth;
                    String firstDoseDate = driverDetails.firstDoseDate;
                    String firstDoseManufacture = driverDetails.firstDoseManufacturer;
                    String secondDoseDate = driverDetails.secondDoseDate;
                    String secondDoseManufacture = driverDetails.secondDoseManufacturer;
                    String otherDoseDate = driverDetails.otherDoseDate;
                    String otherDoseManufacture = driverDetails.otherDoseManufacturer;
                    String data = "Name: " + name + "\n" + "Date of Birth: " + dob + "\n" +
                            "First Dose Manufacturer: " + firstDoseManufacture + "\n" + "First Dose Date: " + firstDoseDate + "\n" +
                            "Second Dose Manufacturer: " + secondDoseManufacture + "\n" + "Second Dose Date: " + secondDoseDate + "\n" +
                            "Other Dose Manufacturer: " + otherDoseManufacture + "\n" + "Other Dose Date: " + otherDoseDate;
                    runOnUiThread(() -> showAlert(data, driverDetails));
                }

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
    }

    private String convertImageToBase64(Bitmap photo) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
    }

    private void showAlert(String message, DriverDetails driverDetails) {
        progressBar.setVisibility(ProgressBar.GONE);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setTitle("Are the details correct?");
        builder.setCancelable(false);
        builder.setPositiveButton("Yes", (dialog, which) -> {

            saveAndUploadDate(docUploadType, driverDetails, dialog);
            if (alertFlag) {
                if (docUploadType.equals("vaccine")) {
                    setResult(Activity.RESULT_OK);
                    finish();
                } else {
                    setResult(Activity.RESULT_OK);
                    finish();
                    Intent i = new Intent(this, ChooseDocument.class);
                    i.putExtra("documentType", "vaccine_certificate");
                    startActivity(i);
                }
            }
        });

        builder.setNegativeButton("No", (dialog, which) -> {
            if (docUploadType.equals("dl")) {
                setResult(Activity.RESULT_OK);
                finish();
                Intent intent = new Intent(this, LicenceEdit.class);
                intent.putExtra("dl_first_name", driverDetails.givenName);
                intent.putExtra("dl_last_name", driverDetails.familyName);
                intent.putExtra("dl_dob", driverDetails.dateOfBirth);
                intent.putExtra("dl_number", driverDetails.licenseNumber);
                startActivity(intent);
            } else if (docUploadType.equals("vaccine")) {
                setResult(Activity.RESULT_OK);
                finish();
                Intent intent = new Intent(this, VaccineEdit.class);
                intent.putExtra("vaccine_first_name", driverDetails.givenName);
                intent.putExtra("vaccine_last_name", driverDetails.familyName);
                intent.putExtra("vaccine_dob", driverDetails.dateOfBirth);
                intent.putExtra("vaccine_first_dose_date", driverDetails.firstDoseDate);
                intent.putExtra("vaccine_first_dose_manufacturer", driverDetails.firstDoseManufacturer);
                intent.putExtra("vaccine_second_dose_date", driverDetails.secondDoseDate);
                intent.putExtra("vaccine_second_dose_manufacturer", driverDetails.secondDoseManufacturer);
                intent.putExtra("vaccine_other_dose_date", driverDetails.otherDoseDate);
                intent.putExtra("vaccine_other_dose_manufacturer", driverDetails.otherDoseManufacturer);
                startActivity(intent);
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void saveAndUploadDate(String docUploadTypeTemp, DriverDetails driverDetails, DialogInterface maindialog) {
        if (docUploadTypeTemp.equals("dl")) {
            SharedPreferences.Editor editor = getSharedPreferences("MyPrefs", MODE_PRIVATE).edit();
            editor.putBoolean("dlUploaded", true);
            editor.putString("dl_name", driverDetails.givenName + " " + driverDetails.familyName);
            editor.putString("dl_dob", driverDetails.dateOfBirth);
            editor.putString("dl_number", driverDetails.licenseNumber);

            // Vaccine
            editor.putBoolean("vaccineUploaded", false);
            editor.putString("vaccine_first_name", "");
            editor.putString("vaccine_last_name", "");
            editor.putString("vaccine_dob", "");
            editor.putString("vaccine_first_dose_date", "");
            editor.putString("vaccine_first_dose_manufacture", "");
            editor.putString("vaccine_second_dose_date", "");
            editor.putString("vaccine_second_dose_manufacture", "");
            editor.putString("vaccine_other_dose_date", "");
            editor.putString("vaccine_other_dose_manufacture", "");

            editor.apply();
            Runnable advertisingIdRunnable = postLicence(driverDetails);
            executorService.execute(advertisingIdRunnable);

        } else if (docUploadTypeTemp.equals("vaccine")) {
            SharedPreferences sharedPreferences = Objects.requireNonNull(this).getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            boolean licenseUploaded = sharedPreferences.getBoolean("dlUploaded", false);
            String dl_name = sharedPreferences.getString("dl_name", "");
            String dl_dob = sharedPreferences.getString("dl_dob", "");

            if (licenseUploaded && !dl_name.equals("") && !dl_dob.equals("")) {
                String[] arrname = dl_name.split(" ");
                if (arrname.length > 0) {
                    String fname = arrname[0].trim().toLowerCase();
                    String lname = arrname[arrname.length - 1].trim().toLowerCase();
                    if (!driverDetails.givenName.equalsIgnoreCase(fname) || !driverDetails.familyName.equalsIgnoreCase(lname) || !driverDetails.dateOfBirth.equalsIgnoreCase(dl_dob)) {
                        maindialog.cancel();
                        alertFlag = false;
                        AlertDialog.Builder builder = new AlertDialog.Builder(ChooseDocument.this);
                        builder.setMessage("Vaccine Personal information do not match with License Details. Please Re-scan and upload vaccine card..");
                        builder.setTitle("Alert!");
                        builder.setPositiveButton("OK", (DialogInterface.OnClickListener) (dialog, which) -> {
                            btn.setText("Upload Vaccine");
                            isImageCaptured = false;
                            progressBar.setVisibility(ProgressBar.GONE);
                            // When the user click yes button then app will close
                            dialog.cancel();
                        });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    } else {
                        SharedPreferences.Editor editor = getSharedPreferences("MyPrefs", MODE_PRIVATE).edit();
                        editor.putBoolean("vaccineUploaded", true);
                        editor.putString("vaccine_first_name", driverDetails.givenName);
                        editor.putString("vaccine_last_name", driverDetails.familyName);
                        editor.putString("vaccine_dob", driverDetails.dateOfBirth);
                        editor.putString("vaccine_first_dose_date", driverDetails.firstDoseDate);
                        editor.putString("vaccine_first_dose_manufacture", driverDetails.firstDoseManufacturer);
                        editor.putString("vaccine_second_dose_date", driverDetails.secondDoseDate);
                        editor.putString("vaccine_second_dose_manufacture", driverDetails.secondDoseManufacturer);
                        editor.putString("vaccine_other_dose_date", driverDetails.otherDoseDate);
                        editor.putString("vaccine_other_dose_manufacture", driverDetails.otherDoseManufacturer);
                        editor.apply();

                        Runnable advertisingIdRunnable = postVaccine(driverDetails);
                        executorService.execute(advertisingIdRunnable);
                    }
                }
            }

        }
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
                        JSONObject jsonBody = new JSONObject();

                        try {
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
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        String requestBody = jsonBody.toString();

                        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, response -> {
                            Log.d("RESPONSE", "response => " + response);
                            runOnUiThread(() -> {
                                Toast.makeText(getApplicationContext(), "Vaccine uploaded successfully", Toast.LENGTH_SHORT).show();
                            });
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

                    }
                });
            }
        };
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