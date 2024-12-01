package com.example.cartrack.Model;

import android.os.Parcel;
import android.os.Parcelable;

public class carModel implements Parcelable {
    private String CarName, CarModel, CarPlate, CarImage, CarDocID, CurrentUserID, CarRealAddress;
    private double CarLongitude, CarLatitude; // Updated types for longitude and latitude
    private float CarAccuracy;

    public carModel() {}

    public carModel(String carName, String carModel, String carPlate, double carLongitude, double carLatitude, String carImage, String carDocID, String currentUserID, String carRealAddress, float carAccuracy) {
        this.CarName = carName;
        this.CarModel = carModel;
        this.CarPlate = carPlate;
        this.CarLongitude = carLongitude;
        this.CarLatitude = carLatitude;
        this.CarImage = carImage;
        this.CarDocID = carDocID;
        this.CurrentUserID = currentUserID;
        this.CarRealAddress = carRealAddress;
        this.CarAccuracy = carAccuracy;
    }

    // Constructor for Parcel
    protected carModel(Parcel in) {
        CarName = in.readString();
        CarModel = in.readString();
        CarPlate = in.readString();
        CarRealAddress = in.readString();
        CarLongitude = in.readDouble(); // Updated for double
        CarLatitude = in.readDouble();  // Updated for double
        CarImage = in.readString();
        CarDocID = in.readString();
        CurrentUserID = in.readString();
        CarAccuracy = in.readFloat(); // Read accuracy
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(CarName);
        dest.writeString(CarModel);
        dest.writeString(CarPlate);
        dest.writeString(CarRealAddress);
        dest.writeDouble(CarLongitude); // Write longitude
        dest.writeDouble(CarLatitude);  // Write latitude
        dest.writeString(CarImage);
        dest.writeString(CarDocID);
        dest.writeString(CurrentUserID);
        dest.writeDouble(CarAccuracy); // Write accuracy
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<carModel> CREATOR = new Creator<carModel>() {
        @Override
        public carModel createFromParcel(Parcel in) {
            return new carModel(in);
        }

        @Override
        public carModel[] newArray(int size) {
            return new carModel[size];
        }
    };

    public String getCarRealAddress() {
        return this.CarRealAddress;
    }

    public void setCarRealAddress(String carRealAddress) {
        this.CarRealAddress = carRealAddress;
    }

    public String getCarName() {
        return this.CarName;
    }

    public void setCarName(String carName) {
        this.CarName = carName;
    }

    public String getCarModel() {
        return this.CarModel;
    }

    public void setCarModel(String carModel) {
        this.CarModel = carModel;
    }

    public String getCarPlate() {
        return this.CarPlate;
    }

    public void setCarPlate(String carPlate) {
        this.CarPlate = carPlate;
    }

    public double getCarLongitude() {
        return this.CarLongitude;
    }

    public void setCarLongitude(double carLongitude) {
        this.CarLongitude = carLongitude;
    }

    public double getCarLatitude() {
        return this.CarLatitude;
    }

    public void setCarLatitude(double carLatitude) {
        this.CarLatitude = carLatitude;
    }

    public String getCarImage() {
        return this.CarImage;
    }

    public void setCarImage(String carImage) {
        this.CarImage = carImage;
    }

    public String getCarDocID() {
        return this.CarDocID;
    }

    public void setCarDocID(String carDocID) {
        this.CarDocID = carDocID;
    }

    public String getCurrentUserID() {
        return this.CurrentUserID;
    }

    public void setCurrentUserID(String currentUserID) {
        this.CurrentUserID = currentUserID;
    }

    public float getCarAccuracy() {
        return this.CarAccuracy;
    }

    public void setCarAccuracy(float carAccuracy) {
        this.CarAccuracy = carAccuracy;
    }
}
