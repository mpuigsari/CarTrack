package com.example.cartrack.Fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.cartrack.Model.carModel;
import com.example.cartrack.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

public class MapaFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private FirebaseFirestore firestore;
    private DatabaseReference realTimeDbRef;
    private LocationManager locationManager;
    private HashMap<String, Marker> carMarkers = new HashMap<>();
    private HashMap<String, Circle> carCircles = new HashMap<>();
    private HashMap<String, String> deviceToCarMap = new HashMap<>();
    private HashMap<String, Location> lastKnownLocations = new HashMap<>();
    private HashMap<String, String> carStates = new HashMap<>();
    private HashMap<String, ValueEventListener> activeListeners = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        CheckGPS();

        firestore = FirebaseFirestore.getInstance();
        realTimeDbRef = FirebaseDatabase.getInstance("https://cartrack3048-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("/Devices");

        SwitchCompat mapTypeSwitch = view.findViewById(R.id.map_type_switch);
        mapTypeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            } else {
                googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            }
        });

        return view;
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d("ActivityResultLauncher", "Permission granted");
                } else {
                    Log.d("ActivityResultLauncher", "Permission denied");
                }
            });

    private void CheckGPS() {
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            OnGPS();
        } else {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            } else {
                SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
                if (mapFragment == null) {
                    mapFragment = SupportMapFragment.newInstance();
                    getChildFragmentManager().beginTransaction()
                            .replace(R.id.map, mapFragment)
                            .commit();
                }
                mapFragment.getMapAsync(this);
            }
        }
    }

    private void OnGPS() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage("Enable GPS")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialogInterface, i) -> startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("No", (dialogInterface, i) -> dialogInterface.dismiss());
        builder.create().show();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.setMyLocationEnabled(true);
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));

        googleMap.setOnMarkerClickListener(marker -> {
            displayCarInfo(marker);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 20));
            return true;
        });

        fetchCarsAndDisplay();
    }

    private void fetchCarsAndDisplay() {
        String currentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestore.collection("CarInfo")
                .whereEqualTo("currentUserID", currentUserID)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "Error fetching cars", error);
                        return;
                    }
                    if (snapshot != null) {
                        for (DocumentChange documentChange : snapshot.getDocumentChanges()) {
                            carModel car = documentChange.getDocument().toObject(carModel.class);
                            car.setCarDocID(documentChange.getDocument().getId());
                            switch (documentChange.getType()) {
                                case ADDED:
                                case MODIFIED:
                                    addOrUpdateCarMarker(car, new LatLng(car.getCarLatitude(), car.getCarLongitude()));
                                    break;
                                case REMOVED:
                                    removeCarMarker(car.getCarDocID());
                                    break;
                            }
                        }
                    }
                });
    }

    private void fetchRTDBLocation(String carDocID, String deviceID) {
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Double latitude = snapshot.child("latitude").getValue(Double.class);
                    Double longitude = snapshot.child("longitude").getValue(Double.class);
                    Float accuracy = snapshot.child("accuracy").getValue(Float.class);
                    if (latitude != null && longitude != null) {
                        updateCarLocationInFirestore(carDocID, latitude, longitude, accuracy);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("RTDB", "Error fetching location for deviceID: " + deviceID, error.toException());
            }
        };

        realTimeDbRef.child(deviceID).addValueEventListener(listener);
        activeListeners.put(deviceID, listener);
    }

    private void updateCarLocationInFirestore(String carDocID, double latitude, double longitude, Float accuracy) {
        HashMap<String, Object> updates = new HashMap<>();
        updates.put("carLatitude", latitude);
        updates.put("carLongitude", longitude);
        if (accuracy != null) updates.put("carAccuracy", accuracy);

        firestore.collection("CarInfo").document(carDocID)
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Location updated for carDocID: " + carDocID))
                .addOnFailureListener(e -> Log.e("Firestore", "Error updating location for carDocID: " + carDocID, e));
    }

    private void addOrUpdateCarMarker(carModel car, LatLng location) {
        Marker marker = carMarkers.get(car.getCarDocID());
        if (marker != null) {
            marker.setPosition(location);
            marker.setTag(car);
            updateCarState(car);

            Circle circle = carCircles.get(car.getCarDocID());
            if (circle != null) {
                circle.setCenter(location);
                circle.setRadius(car.getCarAccuracy());
            }
        } else {
            addCarMarker(car, location);
        }
    }

    private void addCarMarker(carModel car, LatLng location) {
        if (!isAdded()) return;

        Drawable vectorDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.tractor);
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);

        Marker marker = googleMap.addMarker(new MarkerOptions()
                .position(location)
                .title(car.getCarName())
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap)));

        if (marker != null) {
            marker.setTag(car);
            carMarkers.put(car.getCarDocID(), marker);
        }

        CircleOptions circleOptions = new CircleOptions()
                .center(location)
                .radius(car.getCarAccuracy())
                .strokeWidth(3f)
                .strokeColor(Color.YELLOW)
                .fillColor(0x66FFFF00);
        carCircles.put(car.getCarDocID(), googleMap.addCircle(circleOptions));
    }

    private void removeCarMarker(String carDocID) {
        Marker marker = carMarkers.remove(carDocID);
        if (marker != null) marker.remove();
        Circle circle = carCircles.remove(carDocID);
        if (circle != null) circle.remove();
    }

    private void updateCarState(carModel car) {
        Location lastLocation = lastKnownLocations.get(car.getCarDocID());
        Location currentLocation = new Location("current");
        currentLocation.setLatitude(car.getCarLatitude());
        currentLocation.setLongitude(car.getCarLongitude());

        String state;
        if (lastLocation != null && lastLocation.distanceTo(currentLocation) > 10) {
            state = "Moving";
        } else if (lastLocation != null) {
            state = "Stationary";
        } else {
            state = "Unknown";
        }

        carStates.put(car.getCarDocID(), state);
        lastKnownLocations.put(car.getCarDocID(), currentLocation);
    }

    private void displayCarInfo(Marker marker) {
        View carInfoView = LayoutInflater.from(getContext()).inflate(R.layout.layout_carinfo, null);

        try {
            TextView carNameTextView = carInfoView.findViewById(R.id.txtcarName);
            TextView carModelTextView = carInfoView.findViewById(R.id.txtcarModel);
            TextView carPlateTextView = carInfoView.findViewById(R.id.txtcarPlate);
            TextView carLocationTextView = carInfoView.findViewById(R.id.txtcarLocation);
            TextView carStateTextView = carInfoView.findViewById(R.id.txtcarState);
            ImageView carImageView = carInfoView.findViewById(R.id.CarImage);

            carModel car = (carModel) marker.getTag();
            if (car != null) {
                carNameTextView.setText(car.getCarName());
                carModelTextView.setText(car.getCarModel());
                carPlateTextView.setText(car.getCarPlate());
                carLocationTextView.setText(car.getCarRealAddress());

                Picasso.get().load(car.getCarImage()).networkPolicy(NetworkPolicy.OFFLINE)
                        .into(carImageView, new com.squareup.picasso.Callback() {
                            @Override
                            public void onSuccess() {
                                Log.d("Picasso", "Image loaded from cache.");
                            }

                            @Override
                            public void onError(Exception e) {
                                Picasso.get().load(car.getCarImage()).into(carImageView);
                            }
                        });

                carStateTextView.setText(carStates.getOrDefault(car.getCarDocID(), "Unknown"));
            }
        } catch (Exception e) {
            Log.e("MapaFragment", "Error displaying car info", e);
        }

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomSheetDialog.setContentView(carInfoView);
        bottomSheetDialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (String deviceID : activeListeners.keySet()) {
            realTimeDbRef.child(deviceID).removeEventListener(activeListeners.get(deviceID));
        }
        activeListeners.clear();
    }
}
