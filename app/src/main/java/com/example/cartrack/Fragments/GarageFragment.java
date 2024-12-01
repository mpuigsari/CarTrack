package com.example.cartrack.Fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cartrack.Model.carModel;
import com.example.cartrack.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.cartrack.Adapter.carInfoAdapter;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class GarageFragment extends Fragment {
    FloatingActionButton buttonAddCar;
    private FirebaseFirestore firestore;
    private carInfoAdapter carInfoAdapter;
    private ArrayList<carModel> carInfoList;
    private String address, deviceID;
    private Double longitude, latitude;
    private RecyclerView recyclerViewCarInfo;
    private SearchView searchView;

    private Handler handler = new Handler();
    private Runnable checkMovementRunnable;
    private HashMap<String, Location> lastKnownLocations;

    public GarageFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_garage, container, false);
        buttonAddCar = view.findViewById(R.id.buttonAddCar);
        buttonAddCar.setOnClickListener(view1 -> navigateToAddCarFragment());
        recyclerViewCarInfo = view.findViewById(R.id.recyclerViewCarInfo);

        firestore = FirebaseFirestore.getInstance();
        carInfoList = new ArrayList<>();
        lastKnownLocations = new HashMap<>();
        recyclerViewCarInfo.setHasFixedSize(true);

        searchView = view.findViewById(R.id.searchViewBar);
        int hintColor = ContextCompat.getColor(requireContext(), R.color.navy);
        int textColor = ContextCompat.getColor(requireContext(), android.R.color.black);

        // Set hint color
        int id = searchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
        TextView searchText = searchView.findViewById(id);
        searchText.setHintTextColor(hintColor);
        searchText.setTextColor(textColor);

        Log.d("GarageFragment", "Fetching car information...");
        GetCarInfo();

        // Periodically check for movement every 5 seconds
        Log.d("GarageFragment", "Starting periodic movement check.");
        startMovementCheck();

        return view;
    }

    private void GetCarInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            Log.d("GetCarInfo", "Authenticated user ID: " + userId);

            // Query Firestore for cars where currentUserID matches the authenticated user's UID
            firestore.collection("CarInfo")
                    .whereEqualTo("currentUserID", userId)  // Filter for cars belonging to the authenticated user
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        Log.d("GetCarInfo", "Received car data from Firestore.");

                        if (!queryDocumentSnapshots.isEmpty()) {
                            ArrayList<DocumentSnapshot> list = (ArrayList<DocumentSnapshot>) queryDocumentSnapshots.getDocuments();
                            for (DocumentSnapshot d : list) {
                                // Debug: Log car document ID
                                String carDocID = d.getId();
                                Log.d("GetCarInfo", "Processing car document ID: " + carDocID);

                                carModel car = d.toObject(carModel.class);
                                if (car != null) {
                                    // Debug: Log car details
                                    Log.d("GetCarInfo", "Car info: " + car.toString());

                                    longitude = car.getCarLongitude();
                                    latitude = car.getCarLatitude();

                                    if (longitude != null && latitude != null) {
                                        try {
                                            double lat = latitude;
                                            double lon = longitude;
                                            address = getAddressfromLatLong(requireContext(), lat, lon);
                                            car.setCarRealAddress(address);

                                            // Store car location
                                            Location currentLocation = new Location("current");
                                            currentLocation.setLatitude(lat);
                                            currentLocation.setLongitude(lon);
                                            lastKnownLocations.put(car.getCarDocID(), currentLocation);

                                            // Add car to the list
                                            carInfoList.add(car);
                                        } catch (NumberFormatException e) {
                                            Log.e("GetCarInfo", "Error parsing latitude/longitude: " + e.getMessage());
                                        }
                                    } else {
                                        Log.w("GetCarInfo", "Missing latitude or longitude for car: " + carDocID);
                                    }
                                } else {
                                    Log.w("GetCarInfo", "Car object is null for document: " + carDocID);
                                }
                            }

                            // Debug: Log the size of the carInfoList
                            Log.d("GetCarInfo", "Setting up adapter with " + carInfoList.size() + " cars.");
                            carInfoAdapter = new carInfoAdapter(carInfoList, requireContext(), this::editCar, this::deleteCar, this::deviceCar);
                            recyclerViewCarInfo.setAdapter(carInfoAdapter);
                            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
                            recyclerViewCarInfo.setLayoutManager(linearLayoutManager);
                            carInfoAdapter.notifyDataSetChanged();
                        } else {
                            Log.w("GetCarInfo", "No car data found for the current user in Firestore.");
                        }
                    })
                    .addOnFailureListener(e -> Log.e("GetCarInfo", "Error getting car info: " + e.getMessage()));
        } else {
            Log.e("GetCarInfo", "User is not authenticated.");
        }
    }


    private String getAddressfromLatLong(Context context, double latitude, double longitude) {
        String address = "Address not available";
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                address = addresses.get(0).getAddressLine(0);
            }
        } catch (Exception e) {
            Log.e("getAddressfromLatLong", "Error getting address: " + e.getMessage());
        }
        return address;
    }

    private void startMovementCheck() {
        checkMovementRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d("GarageFragment", "Checking car movement...");
                // Check car movement
                for (carModel car : carInfoList) {
                    checkCarMovement(car);
                }
                // Schedule the next check in 5 seconds
                handler.postDelayed(this, 5000);
            }
        };
        handler.postDelayed(checkMovementRunnable, 0); // Start the check immediately
    }

    private void checkCarMovement(carModel car) {
        String carDocID = car.getCarDocID();
        Location lastLocation = lastKnownLocations.get(carDocID);
        if (lastLocation != null) {
            double currentLat = car.getCarLatitude();
            double currentLon = car.getCarLongitude();
            Location currentLocation = new Location("current");
            currentLocation.setLatitude(currentLat);
            currentLocation.setLongitude(currentLon);

            // Calculate the distance between last and current locations
            float distance = lastLocation.distanceTo(currentLocation); // in meters
            Log.d("checkCarMovement", "Car " + carDocID + " moved " + distance + " meters.");
            if (distance > 10) { // If the car moved more than 10 meters

                updateCarStateInAdapter(car, "Moving");
            } else {
                updateCarStateInAdapter(car, "Stationary");
            }

            // Update the last known location
            lastKnownLocations.put(carDocID, currentLocation);
        } else {
            Log.w("checkCarMovement", "Last location for car " + car.getCarDocID() + " not found.");
        }
    }

    private void updateCarStateInAdapter(carModel car, String state) {
        // Find the position of the car in the list
        int position = carInfoList.indexOf(car);
        if (position != -1) {
            Log.d("updateCarState", "Updating car state for car " + car.getCarDocID() + " to " + state);
            carInfoAdapter.notifyItemChanged(position); // Notify adapter that data has changed
            com.example.cartrack.Adapter.carInfoAdapter.ViewHolder holder = (com.example.cartrack.Adapter.carInfoAdapter.ViewHolder) recyclerViewCarInfo.findViewHolderForAdapterPosition(position);
            if (holder != null) {
                if(!Objects.equals(holder.getCarState(), state)) {
                    holder.setCarState(state);
                }else{
                    Log.d("updateCarState", "Car " + car.getCarDocID() + " already in state " + state);
                }
            }
        } else {
            Log.w("updateCarState", "Car " + car.getCarDocID() + " not found in list.");
        }
    }
    private void deviceCar(carModel car) {
        // Inside the click listener, create and show the dialog
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.pop_up_device, null);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        dialogBuilder.setView(dialogView);

        // Initialize input and button from the dialog layout
        EditText deviceIdInput = dialogView.findViewById(R.id.deviceIdInput);
        Button submitDeviceButton = dialogView.findViewById(R.id.submitDeviceButton);
        TextView popupTitle = dialogView.findViewById(R.id.popupTitle);
        ImageView closeIcon = dialogView.findViewById(R.id.closeIcon);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        ImageView deleteIcon = dialogView.findViewById(R.id.deleteDevice);
        deleteIcon.setVisibility(View.INVISIBLE);

        checkDeviceLinked(car.getCarDocID(), popupTitle);
        if (deviceID != null && !deviceID.isEmpty()) {
        deleteIcon.setOnClickListener(v -> showDeleteConfirmationDialog(deviceID));
        deleteIcon.setVisibility(View.VISIBLE);}
        // Create the AlertDialog
        AlertDialog dialog = dialogBuilder.create();

        // Set up the submit button to handle user input
        submitDeviceButton.setOnClickListener(submitView -> {
            String deviceId = deviceIdInput.getText().toString().trim();
            deviceIdInput.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
            if (!deviceId.isEmpty()) {
                popupTitle.setText(String.format("Device ID: %s", deviceId));
                // Call method to handle device registration and pass deviceIdInput to update it
                Log.d("GarageFragment", "Submitting device ID: " + deviceId);
                linkDevice(deviceId, car.getCarDocID(), deviceIdInput);
            } else {
                // Show error if device ID is empty
                deviceIdInput.setError("Device ID cannot be empty");
                Log.w("GarageFragment", "Device ID is empty");
            }
            deviceIdInput.setEnabled(true);
            progressBar.setVisibility(View.GONE);
        });
        closeIcon.setOnClickListener(v -> dialog.dismiss());

        // Show the dialog
        dialog.show();
    }
    private void checkDeviceLinked(String carDocId, TextView txtDevice) {
        // Query the Devices collection to check for any document where carDocId matches the given ID
        firestore.collection("Devices")
                .whereEqualTo("carDocID", carDocId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            DocumentSnapshot deviceDoc = querySnapshot.getDocuments().get(0);
                            deviceID = deviceDoc.getString("deviceID");
                            txtDevice.setText(String.format("Device Linked: %s", deviceID));
                            // If the query returns results, the device is already linked
                            Log.d("GarageFragment", "Device is already linked to this car.");
                        } else {
                            // If no document is found, the car is not linked to a device
                            Log.d("GarageFragment", "This car is not linked to any device.");
                        }
                    } else {
                        // Handle the error in case the query fails
                        Log.e("GarageFragment", "Error checking device link: " + task.getException().getMessage());                    }
                });
    }
    private void linkDevice(String deviceId, String targetCarDocId, EditText deviceIdInput) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Query the Devices collection for the given deviceID
        firestore.collection("Devices")
                .whereEqualTo("deviceID", deviceId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();

                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            // There should be only one document with this deviceID
                            DocumentSnapshot deviceDoc = querySnapshot.getDocuments().get(0);
                            String existingCarDocId = deviceDoc.getString("carDocID");

                            // Check if the device has no carDocId or if it matches the desired carDocId
                            if (existingCarDocId == null || existingCarDocId.isEmpty()) {
                                // No carDocId set, so link the device to the target car
                                deviceDoc.getReference().update("carDocID", targetCarDocId)
                                        .addOnSuccessListener(aVoid -> {
                                            deviceIdInput.setHint(" - Device successfully linked!"); // Update EditText with success message
                                        })
                                        .addOnFailureListener(e -> {
                                            deviceIdInput.setHint(String.format(" - Error linking device: %s", e.getMessage())); // Show error in EditText
                                        });
                            } else if (!existingCarDocId.equals(targetCarDocId)) {
                                // The device is already linked to a different car
                                deviceIdInput.setHint(R.string.error_device_different_car); // Error message in EditText
                            } else {
                                // The device is already correctly linked to the desired car
                                deviceIdInput.setHint(R.string.device_is_already_linked_to_this_car); // Show success message in EditText
                            }
                        } else {
                            // Device ID not found
                            deviceIdInput.setHint(R.string.device_not_found); // Show device not found message
                        }
                    } else {
                        // Firestore query failed
                        deviceIdInput.setHint(String.format(" - Error fetching device: %s", task.getException().getMessage())); // Error message in EditText
                    }
                });
        deviceIdInput.setText("");
    }
    private void showDeleteConfirmationDialog(final String deviceId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Delete Device");
        builder.setMessage("Are you sure you want to delete this device?");

        // Yes button
        builder.setPositiveButton("Yes", (dialog, which) -> {
            deleteDevice(deviceId);  // Call the function to delete the device
        });

        // No button
        builder.setNegativeButton("No", (dialog, which) -> {
            dialog.dismiss();  // Close the dialog if the user selects "No"
        });

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteDevice(String deviceId) {
        // Log the starting of the method with the provided deviceId
        Log.d("DeleteDevice", "Attempting to delete device with ID: " + deviceId);

        // Query the "Devices" collection to find the device by its device ID
        firestore.collection("Devices")
                .whereEqualTo("deviceID", deviceId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        // Log the result of the query
                        Log.d("DeleteDevice", "Query successful, found " + querySnapshot.size() + " matching device(s)");

                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            // Assuming the device is uniquely identified by the deviceID
                            DocumentSnapshot deviceDoc = querySnapshot.getDocuments().get(0);
                            String docId = deviceDoc.getId(); // Get the document ID of the device
                            // Log the document ID for debugging
                            Log.d("DeleteDevice", "Device found. Document ID: " + docId);

                            // Delete the link between device and car from Firestore
                            firestore.collection("Devices").document(docId).update("carDocID","")
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("DeleteDevice", "Device successfully deleted.");
                                        Toast.makeText(getContext(), "Device deleted successfully", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("DeleteDevice", "Error deleting device: " + e.getMessage());
                                        Toast.makeText(getContext(), "Error deleting device: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Log.d("DeleteDevice", "No device found with the given ID.");
                            Toast.makeText(getContext(), "Device not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Log the error if the Firestore query fails
                        Log.e("DeleteDevice", "Error fetching device: " + task.getException().getMessage());
                        Toast.makeText(getContext(), "Error fetching device: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void editCar(com.example.cartrack.Model.carModel car) {
        Log.d("GarageFragment", "Navigating to edit car for car: " + car.getCarDocID());
        AddCarInfoFragment addCarFragment = new AddCarInfoFragment();
        Bundle args = new Bundle();
        args.putParcelable("carModel", car); // Use putParcelable
        addCarFragment.setArguments(args);

        // Navigate to AddCarInfoFragment
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, addCarFragment)
                .addToBackStack(null)
                .commit();
    }

    private void deleteCar(com.example.cartrack.Model.carModel car, int position) {
        // Show confirmation dialog before deletion
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Car")
                .setMessage("Are you sure you want to delete this car?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Log.d("DeleteCar", "Deleting car: " + car.getCarDocID());
                    firestore.collection("CarInfo").document(car.getCarDocID())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                carInfoList.remove(position);
                                carInfoAdapter.notifyItemRemoved(position);
                                Log.d("DeleteCar", "Car " + car.getCarDocID() + " deleted successfully.");
                            })
                            .addOnFailureListener(e -> Log.e("DeleteCar", "Error deleting car: " + e.getMessage()));
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // Dismiss the dialog if the user cancels
                    dialog.dismiss();
                })
                .show();
    }

    @Override
    public void onStop() {
        super.onStop();
        handler.removeCallbacks(checkMovementRunnable); // Stop periodic checks when fragment is no longer visible
        Log.d("GarageFragment", "Stopped periodic movement check.");
    }

    private void navigateToAddCarFragment() {
        Log.d("GarageFragment", "Navigating to AddCarInfoFragment.");
        AddCarInfoFragment addCarFragment = new AddCarInfoFragment();
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, addCarFragment)
                .addToBackStack(null)
                .commit();
    }
}
