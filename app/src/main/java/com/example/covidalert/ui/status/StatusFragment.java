package com.example.covidalert.ui.status;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.covidalert.ChooseDocument;
import com.example.covidalert.R;
import com.example.covidalert.databinding.FragmentStatusBinding;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;

public class StatusFragment extends Fragment {
    private Button uploadButton;
    private TextView vaccine1, vaccine2, vaccineOther, product1, product2, productOther, date1, date2, dateOther, vaccinationStatus;

    private CardView statusVaccinationCard;
    private String docType = "license";

    @Override
    public void onResume() {
        super.onResume();
        updateData(getContext());
    }

    private FragmentStatusBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        StatusViewModel statusViewModel =
                new ViewModelProvider(this).get(StatusViewModel.class);
        Intent intent = getActivity().getIntent();

        if (intent != null) {
            String intentDocType = intent.getStringExtra("documentType");
            if (intentDocType != null) {
                docType = intentDocType;
            }
        }

        binding = FragmentStatusBinding.inflate(inflater, container, false);
        uploadButton = binding.getRoot().findViewById(R.id.status_upload_btn);
//        vaccineButton = binding.getRoot().findViewById(R.id.status_upload_vaccination_btn);
        vaccine1 = binding.getRoot().findViewById(R.id.vaccine1);
        vaccine2 = binding.getRoot().findViewById(R.id.vaccine2);
        vaccineOther = binding.getRoot().findViewById(R.id.vaccineOther);

        product1 = binding.getRoot().findViewById(R.id.product1);
        product2 = binding.getRoot().findViewById(R.id.product2);
        productOther = binding.getRoot().findViewById(R.id.productOther);

        date1 = binding.getRoot().findViewById(R.id.date1);
        date2 = binding.getRoot().findViewById(R.id.date2);
        dateOther = binding.getRoot().findViewById(R.id.dateOther);

        vaccinationStatus = binding.getRoot().findViewById(R.id.status_vaccination_card_text);

        statusVaccinationCard = binding.getRoot().findViewById(R.id.status_vaccination_card);
        updateData(getContext());
        return binding.getRoot();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void updateData(Context context) {
        // Get Shared Preferences
        // If the user has already uploaded a document, hide the button
        // If the user has already uploaded a document, show the document details

        SharedPreferences sharedPreferences = Objects.requireNonNull(getActivity()).getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        boolean licenseUploaded = sharedPreferences.getBoolean("dlUploaded", false);
        boolean vaccineUploaded = sharedPreferences.getBoolean("vaccineUploaded", false);

        String vaccine1Product = sharedPreferences.getString("vaccine_first_dose_manufacture", "");
        String vaccine2Product = sharedPreferences.getString("vaccine_second_dose_manufacture", "");
        String vaccine1Date = sharedPreferences.getString("vaccine_first_dose_date", "");
        String vaccine2Date = sharedPreferences.getString("vaccine_second_dose_date", "");
        String vaccineOtherDate = sharedPreferences.getString("vaccine_other_dose_date", "");
        String vaccineOtherProduct = sharedPreferences.getString("vaccine_other_dose_manufacture", "");

        if (vaccine1Date.length() > 0 && vaccine1Product.length() > 0) {
            vaccine1.setText("1st Dose");
            product1.setText(vaccine1Product);
            date1.setText(vaccine1Date);
            statusVaccinationCard.setVisibility(View.VISIBLE);
        } else {
            vaccine1.setVisibility(View.GONE);
            product1.setVisibility(View.GONE);
            date1.setVisibility(View.GONE);
        }

        if (vaccine2Date.length() > 0 && vaccine2Product.length() > 0) {
            vaccine2.setText("2nd Dose");
            product2.setText(vaccine2Product);
            date2.setText(vaccine2Date);
            statusVaccinationCard.setVisibility(View.VISIBLE);
        } else {
            vaccine2.setVisibility(View.GONE);
            product2.setVisibility(View.GONE);
            date2.setVisibility(View.GONE);
        }

        if (vaccineOtherDate.length() > 0 && vaccineOtherProduct.length() > 0) {
            vaccineOther.setText("Other Dose");
            productOther.setText(vaccineOtherProduct);
            dateOther.setText(vaccineOtherDate);
            statusVaccinationCard.setVisibility(View.VISIBLE);
        } else {
            vaccineOther.setVisibility(View.GONE);
            productOther.setVisibility(View.GONE);
            dateOther.setVisibility(View.GONE);
        }

        if (licenseUploaded) {
            statusVaccinationCard.setVisibility(View.VISIBLE);
        }

        if (vaccineUploaded && licenseUploaded) {
            uploadButton.setText("Documents Uploaded");
        } else if (licenseUploaded) {
            uploadButton.setText("Upload Vaccine");
        } else {
            uploadButton.setText("Upload License");
        }


        String finalDocType = docType;
        uploadButton.setOnClickListener(view -> {
            Intent i = new Intent(context, ChooseDocument.class);
            i.putExtra("documentType", finalDocType);
            activityResultLauncher.launch(i);
        });

        // Update the Vaccination Status based on the vaccination status.
        // If only one dose is taken, show the vaccination status as "Partially Vaccinated" and the count of days from the first dose.
        // If both the doses are taken, show the vaccination status as "Fully Vaccinated" and the count of days from the second dose.
        // If no doses are taken, show the vaccination status as "Not Vaccinated"
        // The date is in the format "yyyy/MM/dd"

        String vaccinationStatusText = "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        if (vaccine1Date.length() > 0 && vaccine2Date.length() > 0 && vaccineOtherDate.length() > 0) {
            try {
                LocalDate givenDate = LocalDate.parse(vaccineOtherDate, formatter);
                LocalDate currentDate = LocalDate.now();
                long days = ChronoUnit.DAYS.between(givenDate, currentDate);
                vaccinationStatusText = "Fully Vaccinated since " + days + " days";

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } else if (vaccine1Date.length() > 0 && vaccine2Date.length() > 0) {
            try {
                LocalDate givenDate = LocalDate.parse(vaccine2Date, formatter);
                LocalDate currentDate = LocalDate.now();
                long days = ChronoUnit.DAYS.between(givenDate, currentDate);
                vaccinationStatusText = "Fully Vaccinated since " + days + " days";
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (vaccine1Date.length() > 0) {
            try {
                LocalDate givenDate = LocalDate.parse(vaccine1Date, formatter);
                LocalDate currentDate = LocalDate.now();
                long days = ChronoUnit.DAYS.between(givenDate, currentDate);
                vaccinationStatusText = "Partially Vaccinated since " + days + " days";
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            vaccinationStatusText = "Not Vaccinated";
        }

        vaccinationStatus.setText(vaccinationStatusText);
    }

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    updateData(getContext());
                }
            }
    );
}