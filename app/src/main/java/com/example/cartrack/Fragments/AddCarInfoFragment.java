package com.example.cartrack.Fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Telephony;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.cartrack.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.example.cartrack.Model.carModel;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;


public class AddCarInfoFragment extends Fragment {

    private LinearLayout SelectPhoto;
    private ImageView imageViewCarPhoto;
    private EditText editTextCarName, editTextCarModel, editTextCarPlate;
    private Button UploadButton;
    private Uri ImageUri;
    private Bitmap bitmap;
    private FirebaseStorage storage;
    private FirebaseFirestore firestore;
    private StorageReference storageReference;
    private FirebaseAuth firebaseAuth;
    private String PhotoUrl, CurrentUserID, DocID, caraddress = "";
    private ProgressBar progressBar;
    private SwitchMaterial switchlocation;
    private LocationManager locationManager;
    private TextView txtcarLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private carModel loaded_car;
    private boolean no_image = false;
    private Double carLongitude, carLatitude;
    private Float carAccuracy;



    public AddCarInfoFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LayoutInflater layoutInflater = (LayoutInflater) requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.fragment_addcar_info, container, false);
        SelectPhoto = view.findViewById(R.id.selectCarPhoto);
        SelectPhoto.setOnClickListener(view1 -> PickImageFromGallery());
        imageViewCarPhoto = view.findViewById(R.id.imageViewCarPhoto);

        editTextCarName = view.findViewById(R.id.editTextCarName);
        editTextCarModel = view.findViewById(R.id.editTextCarModel);
        editTextCarPlate = view.findViewById(R.id.editTextCarPlate);
        txtcarLocation = view.findViewById(R.id.txtcarLocation);

        switchlocation = view.findViewById(R.id.switch_location);
        switchlocation.setOnCheckedChangeListener((compoundButton, b) -> {
            if(compoundButton.isChecked()){
                CheckLocationPermission();
            }
            if(!compoundButton.isChecked()){
                txtcarLocation.setText(R.string.add_current_location);
                carLongitude = null;
                carLatitude = null;
                carAccuracy = null;
            }
        });

        UploadButton = view.findViewById(R.id.buttonUploadCarInfo);
        UploadButton.setOnClickListener(view1 -> UploadImage());
        progressBar = view.findViewById(R.id.progressBarPass);
        progressBar.setVisibility(ViewGroup.GONE);

        firestore = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance("gs://cartrack3048.firebasestorage.app");
        storageReference = storage.getReference();
        firebaseAuth = FirebaseAuth.getInstance();
        CurrentUserID = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();

        if (getArguments() != null) {
            loaded_car = getArguments().getParcelable("carModel");
            if(loaded_car!=null) {
                UploadButton.setText(R.string.update);
                editTextCarName.setHint(loaded_car.getCarName());
                editTextCarModel.setHint(loaded_car.getCarModel());
                editTextCarPlate.setHint(loaded_car.getCarPlate());
                txtcarLocation.setText(loaded_car.getCarRealAddress());
                UploadButton.setOnClickListener(view1 -> UpdateImage());
                loadCarImage(loaded_car.getCarImage(), imageViewCarPhoto);
            }
        }


        return view;
        }



    private void CheckLocationPermission() {
        //Check for location permission
        locationManager= (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Log.d("CheckLocationPermission", "GPS is disabled");
            OnGPS();
        }
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Log.d("CheckLocationPermission", "GPS is enabled");
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        Log.d("getCurrentLocation", "Getting current location");
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("getCurrentLocation", "Permission not granted, requesting...");
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext());
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                    .setWaitForAccurateLocation(false)
                    .build();
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        Log.e("getCurrentLocation", "Location is null");
                        txtcarLocation.setText(R.string.location_not_available);
                        return;}
                    for (Location location : locationResult.getLocations()) {
                        if (location != null) {
                            Log.d("getCurrentLocation", "Location obtained: " + location);
                            carLatitude = location.getLatitude();
                            carLongitude = location.getLongitude();
                            carAccuracy = location.getAccuracy();
                            caraddress = getAddressfromLatLong(requireContext(), carLatitude, carLongitude);
                            txtcarLocation.setText(caraddress);
                        }}
                    fusedLocationProviderClient.removeLocationUpdates(locationCallback);  // Stop updates after first location is retrieved
                }};
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private String getAddressfromLatLong(Context context, double latitude, double longitude) {
        String address = "Address not available";
        try{
            Geocoder geocoder=new Geocoder(context, Locale.getDefault());
            List<Address> addresses=geocoder.getFromLocation(latitude,longitude,1);
            if(addresses != null && !addresses.isEmpty()){
                address= addresses.get(0).getAddressLine(0);
            }
        }catch (Exception e){
            Log.e("getAddressfromLatLong", "Error getting address: " + e.getMessage());
        }

        return address;
    }

    private void OnGPS() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage("Enable GPS").setCancelable(false)
                .setPositiveButton("Yes", (dialogInterface, i) -> startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("No", (dialogInterface, i) -> dialogInterface.dismiss());
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    private void PickImageFromGallery() {
        CheckStoragePermission();
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        launcher.launch(intent);
    }

    private void CheckStoragePermission() {
        //Check for storage permissions
        if(ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }
    ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        getActivity();
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent data = result.getData();
            if (data != null && data.getData() != null) {
                ImageUri = data.getData();
                Log.d("Launcher", "Image Uri: " + ImageUri.toString());
                no_image = true;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), ImageUri);
                } catch (IOException e) {
                    Log.e("Error", Objects.requireNonNull(e.getMessage()));
                    Log.e("Launcher", "Excepcion bitmap");
                }
            }
            if (ImageUri != null){
                try {
                    imageViewCarPhoto.setImageBitmap(bitmap);
                    Log.d("Launcher", "Image Uri: " + ImageUri.toString());
                    no_image = true;
                }catch (Exception e){
                    Log.e("Error", Objects.requireNonNull(e.getMessage()));
                    Toast.makeText(getContext(), "Error loading image", Toast.LENGTH_LONG).show();
                }

            }
        }
    });

    private void UploadImage() {
        Loading();
        if (ImageUri != null) {
            String dir = "images/"+ firebaseAuth.getCurrentUser().getUid()+"/";
            String fileName = ImageUri.getLastPathSegment();
            Log.d("UploadImage", "Image filename: " + fileName);

            StorageReference myRef = storageReference.child(dir+fileName+UUID.randomUUID().toString()+"."+getFileExtension(ImageUri));
            Log.d("UploadImage", "Uploading to: " + myRef.getPath());
            Log.d("UploadImage", "Image Uri: " + ImageUri.toString());
            Log.d("UploadImage", "Reference: " + myRef);
            Log.d("UploadImage", "Storage: " + storageReference.toString());

            UploadTask uploadTask = myRef.putFile(ImageUri);
            uploadTask.addOnProgressListener(taskSnapshot -> {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        Log.d("UploadImage", "Upload is " + progress + "% done");
                    })
                    .addOnSuccessListener(taskSnapshot -> {
                        Log.d("UploadImage", "Upload successful.");
                        myRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            Log.d("UploadImage", "Download URL obtained: " + uri.toString());
                            PhotoUrl = uri.toString();
                            if(loaded_car!=null){
                                UpdateCarInfo();
                            }else {
                                UploadCarInfo();
                            }
                        }).addOnFailureListener(e -> {
                            Log.e("Error", "Error getting download URL: " + e.getMessage());
                            Done();
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Error", "Error uploading file: " + e.getMessage());
                        Done();
                    });
        } else {
            Log.e("UploadImage", "ImageUri is null.");
            Done();
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = requireContext().getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }


    private void UploadCarInfo(){
        String carName,carPlate,str_carModel;
        carName = editTextCarName.getText().toString().trim();
        str_carModel = editTextCarModel.getText().toString().trim();
        carPlate = editTextCarPlate.getText().toString().trim();

        //Later the location will be added
        if(!carName.isEmpty() && !str_carModel.isEmpty() && !carPlate.isEmpty()){
            Log.d("UploadCarInfo", "Creando modelo de coche");
            Log.d("UploadCarInfo", "Usuario"+CurrentUserID);
            DocumentReference myRef = firestore.collection("CarInfo").document();
            carModel carModel = new carModel(carName, str_carModel, carPlate, carLongitude, carLatitude, PhotoUrl, "", CurrentUserID,caraddress, carAccuracy);
            myRef.set(carModel, SetOptions.merge()).addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    DocID = myRef.getId();
                    carModel.setCarDocID(DocID);

                    myRef.set(carModel, SetOptions.merge()).addOnCompleteListener(task1 -> {
                        Toast.makeText(getContext(), "Upload Successful", Toast.LENGTH_LONG).show();
                        Done();
                        navigateToGarageFragment();

                    }).addOnFailureListener(e -> {
                        Log.e("Error", Objects.requireNonNull(e.getMessage()));
                        Log.e("Launcher", "Excepcion upload car info");
                        Done();
                    });
                }

            }).addOnFailureListener(e -> {
                Log.e("Error", Objects.requireNonNull(e.getMessage()));
                Log.e("Launcher", "Excepcion merge info");
                Done();
            });
        }else{
            editTextCarName.setError("Car name is required");
            editTextCarModel.setError("Car model is required");
            editTextCarPlate.setError("Car plate is required");
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_LONG).show();
            Done();

        }

    }
    public void loadCarImage(String imagePath, final ImageView imageView) {
        Uri uri = Uri.parse(imagePath);

        Picasso.get()
                .load(uri)
                .into(imageView, new Callback() {
                    @Override
                    public void onSuccess() {
                        // Success: Image has been loaded into ImageView
                        Log.d("Picasso", "Image loaded successfully");
                        ImageUri = uri;
                        PhotoUrl = ImageUri.toString();

                    }
                    @Override
                    public void onError(Exception e) {
                        // Error: Image loading failed
                        Log.e("Picasso", "Error loading image: " + e.getMessage());
                        no_image = true;
                    }
                });
    }
    private void UpdateCarInfo(){
        String carName,carPlate,str_carModel;

        carName = editTextCarName.getText().toString().trim().isEmpty() ? loaded_car.getCarName() : editTextCarName.getText().toString().trim();
        str_carModel = editTextCarModel.getText().toString().trim().isEmpty() ? loaded_car.getCarModel() : editTextCarModel.getText().toString().trim();
        carPlate = editTextCarPlate.getText().toString().trim().isEmpty() ? loaded_car.getCarPlate() : editTextCarPlate.getText().toString().trim();
        carLongitude = carLongitude == null ? loaded_car.getCarLongitude() : carLongitude;
        carLatitude = carLatitude == null ? loaded_car.getCarLatitude() : carLatitude;
        carAccuracy = carAccuracy == null ? loaded_car.getCarAccuracy() : carAccuracy;
        DocID = loaded_car.getCarDocID();

        Log.d("UpdateCarInfo", "Creando modelo de coche");
        Log.d("UpdateCarInfo", "Usuario"+CurrentUserID);
        DocumentReference myRef = firestore.collection("CarInfo").document(DocID);
        carModel carModel = new carModel(carName, str_carModel, carPlate, carLongitude, carLatitude, PhotoUrl, DocID, CurrentUserID,caraddress, carAccuracy);
        myRef.set(carModel, SetOptions.merge()).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                Toast.makeText(getContext(), "Update Successful", Toast.LENGTH_LONG).show();
                Done();
                navigateToGarageFragment();
            }
        }).addOnFailureListener(e -> {
            Log.e("Error", Objects.requireNonNull(e.getMessage()));
            Log.e("Launcher", "Excepcion merge info");
            Done();
        });
        }
        private void UpdateImage(){
            Loading();
            if(no_image){
                Log.d("UpdateImage", "Not loaded Image");
                UploadImage();
                return;}
            String fileName = ImageUri.getLastPathSegment();
            Log.d("UpdateImage", "Image filename: " + fileName);
            Log.d("UpdateImage", "Image url: " + PhotoUrl);
            UpdateCarInfo();
        }

    private void Loading(){
        progressBar.setVisibility(ViewGroup.VISIBLE);
        editTextCarPlate.setEnabled(false);
        editTextCarModel.setEnabled(false);
        editTextCarName.setEnabled(false);
        UploadButton.setEnabled(false);
        UploadButton.setAlpha(0.5f);
    }
    private void Done() {
        progressBar.setVisibility(ViewGroup.GONE);
        editTextCarPlate.setEnabled(true);
        editTextCarModel.setEnabled(true);
        editTextCarName.setEnabled(true);
        UploadButton.setEnabled(true);
        UploadButton.setAlpha(1f);
    }
    private void navigateToGarageFragment() {
        GarageFragment garageFragment = new GarageFragment();
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, garageFragment)
                .addToBackStack(null)
                .commit();
    }
}
