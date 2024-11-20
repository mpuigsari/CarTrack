package com.example.cartrack.Model;
import android.os.Parcel;
import android.os.Parcelable;

public class carModel implements Parcelable{
    private String CarName, CarModel, CarPlate, CarLongitude, CarLatitude, CarImage, CarDocID, CurrentUserID, CarRealAddress;

    public carModel(){}

    public carModel(String carName, String carModel, String carPlate, String carLongitude, String carLatitude, String carImage, String carDocID, String currentUserID, String carRealAddress) {
        this.CarName = carName;
        this.CarModel = carModel;
        this.CarPlate = carPlate;
        this.CarLongitude = carLongitude;
        this.CarLatitude = carLatitude;
        this.CarImage = carImage;
        this.CarDocID = carDocID;
        this.CurrentUserID = currentUserID;
        this.CarRealAddress = carRealAddress;

    }
    // Constructor for Parcel
    protected carModel(Parcel in) {
        CarName = in.readString();
        CarModel = in.readString();
        CarPlate = in.readString();
        CarRealAddress = in.readString();
        CarLongitude = in.readString();
        CarLatitude = in.readString();
        CarImage = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(CarName);
        dest.writeString(CarModel);
        dest.writeString(CarPlate);
        dest.writeString(CarRealAddress);
        dest.writeString(CarLongitude);
        dest.writeString(CarLatitude);
        dest.writeString(CarImage);
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

    public String getCarLongitude() {
        return this.CarLongitude;
    }

    public void setCarLongitude(String carLongitude) {
        this.CarLongitude = carLongitude;
    }

    public String getCarLatitude() {
        return this.CarLatitude;
    }

    public void setCarLatitude(String carLatitude) {
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
}
