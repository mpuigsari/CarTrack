package com.example.cartrack.Fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.cartrack.Model.carModel;
import com.example.cartrack.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

public class MapaFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private List<carModel> carList;
    private FirebaseFirestore firestore;
    private LocationManager locationManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.map, mapFragment)
                    .commit();
        }
        mapFragment.getMapAsync(this);

        CheckLocationPermission();
        firestore = FirebaseFirestore.getInstance();

        return view;
    }
    private void CheckLocationPermission() {
        //Check for location permission
        locationManager= (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Log.d("CheckLocationPermission", "GPS is disabled");
            OnGPS();
            CheckLocationPermission();
        }
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Log.d("CheckLocationPermission", "GPS is enabled");
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
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.setMyLocationEnabled(true);
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.styles)); //JSON for map style


        googleMap.setOnMarkerClickListener(marker -> {
            // Handle marker click events
            Toast.makeText(getContext(), "Marker clicked: " + marker.getTitle(), Toast.LENGTH_SHORT).show();
            return false; // Return true if the event is consumed
        });

        firestore.collection("CarInfo").addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e("Firestore", "Error fetching car data", e);
                return;
            }
            googleMap.clear();
            if (snapshots == null || snapshots.isEmpty()) {
                Log.w("Firestore", "No car data found");
                return;
            }

            for (QueryDocumentSnapshot doc : snapshots) {
                try {
                     double latitude = Double.parseDouble(doc.getString("carLatitude"));
                     double longitude = Double.parseDouble(doc.getString("carLongitude"));
                    String carName = doc.getString("carName");
                    LatLng location = new LatLng(latitude, longitude);
                    googleMap.addMarker(new MarkerOptions()
                            .position(location)
                            .title(carName)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));  // Marker to customize
                } catch (NumberFormatException | NullPointerException error) {
                    Log.e("GetCarInfo", "Error parsing latitude/longitude: " + error.getMessage());
                }

            }
        });
    }
}
