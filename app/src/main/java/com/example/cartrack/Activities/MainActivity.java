package com.example.cartrack.Activities;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.Fragment;

import com.example.cartrack.Fragments.GarageFragment;
import com.example.cartrack.Fragments.MapaFragment;
import com.example.cartrack.Fragments.ProfileFragment;
import com.example.cartrack.R;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private boolean keep = true;
    private final int DELAY = 1250;
    private FirebaseAuth firebaseAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Splash Screen setup
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(() -> keep);
        new Handler().postDelayed(() -> keep = false, DELAY);

        setContentView(R.layout.activity_main);
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        // Initialize Firebase Auth
        FirebaseApp.initializeApp(getApplicationContext());
        firebaseAuth = FirebaseAuth.getInstance();

        // Check if user is logged in
        if (firebaseAuth.getCurrentUser() == null) {
            // User is not logged in, redirect to SignInActivity
            startActivity(new Intent(MainActivity.this, SignInActivity.class));
            finish(); // Close MainActivity
            return; // Exit onCreate
        }

        // Setup Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);
        loadFragment(new GarageFragment());

    }

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener = item -> {
        // By using switch we can easily get
        // the selected fragment
        // by using there id.
        Fragment selectedFragment = null;
        int itemId = item.getItemId();
        if (itemId == R.id.navigation_map) {
            selectedFragment = new MapaFragment();
        } else if (itemId == R.id.navigation_garage) {
            selectedFragment = new GarageFragment();
        } else if (itemId == R.id.navigation_profile) {
            selectedFragment = new ProfileFragment();
        }
        // It will help to replace the
        // one fragment to other.
        return loadFragment(selectedFragment);
    };

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true; // Fragment transaction successful
        }
        return false; // Fragment transaction failed
    }
}


