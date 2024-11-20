package com.example.cartrack.Fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.cartrack.Activities.SignInActivity;
import com.example.cartrack.R;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileFragment extends Fragment {
    private CardView accountCard, notificationsCard, settingsCard, logoutCard;

    public ProfileFragment() {
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        accountCard = view.findViewById(R.id.accountCard);
        notificationsCard = view.findViewById(R.id.notificationsCard);
        settingsCard = view.findViewById(R.id.settingsCard);
        logoutCard = view.findViewById(R.id.logoutCard);

        accountCard.setOnClickListener(view1 -> navigateToFragment(new AccountFragment()));
        notificationsCard.setOnClickListener(view1 -> navigateToFragment(new NotificationsFragment()));
        settingsCard.setOnClickListener(view1 -> navigateToFragment(new SettingsFragment()));
        logoutCard.setOnClickListener(view1 -> LogOut());



        return view;
}

    private void LogOut() {
        // Implement logout logic here
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Log Out");
        builder.setMessage("Are you sure you want to log out?");

        // Confirm button
        builder.setPositiveButton("Yes", (dialog, which) -> {
            FirebaseAuth.getInstance().signOut(); // Sign out from Firebase
            navigateToLoginScreen(); // Navigate to the login screen or close the app
            Log.d("Logout", "User logged out successfully.");
        });

        // Cancel button
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    // Navigate to the login screen or close the app
    private void navigateToLoginScreen() {
        Intent intent = new Intent(requireContext(), SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear the back stack
        startActivity(intent);
    }

    private void navigateToFragment(Fragment targetFragment) {
        Log.d("GarageFragment", "Navigating to " + targetFragment.getClass().getSimpleName());
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, targetFragment)
                .addToBackStack(null)
                .commit();
    }

}
