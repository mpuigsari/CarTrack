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
import android.widget.LinearLayout;
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
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

public class MapaFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private FirebaseFirestore firestore;
    private LocationManager locationManager;
    private HashMap<String, Location> lastKnownLocations = new HashMap<>();
    private HashMap<String, Marker> carMarkers = new HashMap<>();
    private HashMap<String, Circle> carCircles = new HashMap<>();
    private HashMap<String, String> carStates = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        CheckGPS();

        firestore = FirebaseFirestore.getInstance();

        // Initialize switch for toggling map type
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
            Log.d("CheckGPS", "GPS is disabled");
            OnGPS();
            CheckGPS();
        } else {
            Log.d("CheckGPS", "GPS is enabled");
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                CheckGPS();
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
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage("Enable GPS").setCancelable(false)
                .setPositiveButton("Yes", (dialogInterface, i) -> startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("No", (dialogInterface, i) -> dialogInterface.dismiss());
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
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

        // Set up Firestore listener for real-time updates
        firestore.collection("CarInfo").whereEqualTo("currentUserID", FirebaseAuth.getInstance().getCurrentUser().getUid()).addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e("Firestore", "Error fetching car data", e);
                return;
            }

            if (snapshots == null || snapshots.isEmpty()) {
                Log.w("Firestore", "No car data found");
                return;
            }

            for (DocumentChange documentChange : snapshots.getDocumentChanges()) {
                carModel car = documentChange.getDocument().toObject(carModel.class);
                LatLng location = new LatLng(car.getCarLatitude(), car.getCarLongitude());

                switch (documentChange.getType()) {
                    case ADDED:
                        addCarMarker(car, location);
                        break;
                    case MODIFIED:
                        updateCarMarker(car, location);
                        break;
                    case REMOVED:
                        removeCarMarker(car);
                        break;
                }
            }
        });
    }

    private void addCarMarker(carModel car, LatLng location) {
        if (!isAdded()) {
            Log.w("MapaFragment", "Fragment is not attached to context. Cannot add car marker.");
            return;
        }
        updateCarState(car);

        if (carMarkers.containsKey(car.getCarDocID())) {
            updateCarMarker(car, location);
            return;
        }

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

        Location currentLocation = new Location("current");
        currentLocation.setLatitude(car.getCarLatitude());
        currentLocation.setLongitude(car.getCarLongitude());
        lastKnownLocations.put(car.getCarDocID(), currentLocation);

        CircleOptions circleOptions = new CircleOptions()
                .center(location)
                .radius(car.getCarAccuracy())
                .strokeWidth(3f)
                .strokeColor(Color.YELLOW)
                .fillColor(0x66FFFF00);
        Circle circle = googleMap.addCircle(circleOptions);
        carCircles.put(car.getCarDocID(), circle);
    }

    private void updateCarMarker(carModel car, LatLng newLocation) {
        updateCarState(car);

        Marker marker = carMarkers.get(car.getCarDocID());
        if (marker != null) {
            marker.setPosition(newLocation);
            marker.setTag(car);
            Log.d("Firestore", "Updated car location: " + car.getCarDocID());

            // Update circle accuracy radius
            Circle circle = carCircles.get(car.getCarDocID());
            if (circle != null) {
                circle.setCenter(newLocation);
                circle.setRadius(car.getCarAccuracy());
            }
        } else {
            addCarMarker(car, newLocation);
        }

        Location currentLocation = new Location("current");
        currentLocation.setLatitude(car.getCarLatitude());
        currentLocation.setLongitude(car.getCarLongitude());
        lastKnownLocations.put(car.getCarDocID(), currentLocation);
    }

    private void removeCarMarker(carModel car) {
        Marker marker = carMarkers.remove(car.getCarDocID());
        if (marker != null) {
            marker.remove();
        }

        Circle circle = carCircles.remove(car.getCarDocID());
        if (circle != null) {
            circle.remove();
        }

        lastKnownLocations.remove(car.getCarDocID());
        carStates.remove(car.getCarDocID());
    }

    private void updateCarState(carModel car) {
        Location lastLocation = lastKnownLocations.get(car.getCarDocID());
        String state;
        if (lastLocation != null) {
            Location currentLocation = new Location("current");
            currentLocation.setLatitude(car.getCarLatitude());
            currentLocation.setLongitude(car.getCarLongitude());
            float distance = lastLocation.distanceTo(currentLocation);
            if (distance > 10) {
                state = "Moving";
            } else {
                state = "Stationary";
            }
        } else {
            state = "Unknown";
        }
        carStates.put(car.getCarDocID(), state);
    }

    private String getCarState(String carDocID) {
        return carStates.getOrDefault(carDocID, "Unknown");
    }

    private void displayCarInfo(Marker marker) {
        View carInfoView = LayoutInflater.from(getContext()).inflate(R.layout.layout_carinfo, null);
        TextView carNameTextView = carInfoView.findViewById(R.id.txtcarName);
        TextView carModelTextView = carInfoView.findViewById(R.id.txtcarModel);
        TextView carPlateTextView = carInfoView.findViewById(R.id.txtcarPlate);
        TextView carLocationTextView = carInfoView.findViewById(R.id.txtcarLocation);
        TextView carStateTextView = carInfoView.findViewById(R.id.txtcarState);
        ImageView carImageView = carInfoView.findViewById(R.id.CarImage);
        ImageView buttonEditCar = carInfoView.findViewById(R.id.buttonEditCar);
        buttonEditCar.setVisibility(View.GONE);
        ImageView buttonDeleteCar = carInfoView.findViewById(R.id.buttonDeleteCar);
        buttonDeleteCar.setVisibility(View.GONE);
        LinearLayout buttonDevice = carInfoView.findViewById(R.id.buttonDevice);
        buttonDevice.setVisibility(View.GONE);

        carModel car = (carModel) marker.getTag();
        if (car != null) {
            carNameTextView.setText(car.getCarName());
            carModelTextView.setText(car.getCarModel());
            carPlateTextView.setText(car.getCarPlate());
            carLocationTextView.setText(car.getCarRealAddress());
            Picasso.get().load(car.getCarImage()).networkPolicy(NetworkPolicy.OFFLINE).into(carImageView, new com.squareup.picasso.Callback(){
            @Override
            public void onSuccess() {
                Log.d("Picasso", "Image loaded from cache.");}
            @Override
            public void onError(Exception e) {
                Log.w("Picasso", "Cache miss, loading from network.");
                // If not in cache, load from network
                Picasso.get().load(car.getCarImage()).into(carImageView);}
        });

            carStateTextView.setText(getCarState(car.getCarDocID())); // Use the updated car state
        }

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        bottomSheetDialog.setContentView(carInfoView);
        bottomSheetDialog.show();
    }
}
