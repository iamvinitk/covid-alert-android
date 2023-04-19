package com.example.covidalert.model;

public class DriverDetails {
    public String givenName;
    public String familyName;
    public String dateOfBirth;

    public String expirationDate;
    public String licenseNumber;
    public String firstDoseDate;
    public String firstDoseManufacturer;
    public String secondDoseDate;
    public String secondDoseManufacturer;
    public String otherDoseDate;
    public String otherDoseManufacturer;

    public DriverDetails(String givenName, String familyName, String dateOfBirth, String licenseNumber, String firstDoseDate, String firstDoseManufacturer, String secondDoseDate, String secondDoseManufacturer, String otherDoseDate, String otherDoseManufacturer) {
        this.givenName = givenName;
        this.familyName = familyName;
        this.dateOfBirth = dateOfBirth;
        this.licenseNumber = licenseNumber;
        this.firstDoseDate = firstDoseDate;
        this.firstDoseManufacturer = firstDoseManufacturer;
        this.secondDoseDate = secondDoseDate;
        this.secondDoseManufacturer = secondDoseManufacturer;
        this.otherDoseDate = otherDoseDate;
        this.otherDoseManufacturer = otherDoseManufacturer;
    }
}
