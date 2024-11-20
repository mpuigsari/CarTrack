package com.example.cartrack.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.Navigation;

import com.example.cartrack.Activities.MainActivity;
import com.example.cartrack.R;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Objects;


public class SignInFragment extends Fragment {
    private static final String WEB_CLIENT_ID = "204160790685-eonpalpok1s9sjbctuvqllhd9q90qq5s.apps.googleusercontent.com";
    private static final  int REQUEST_CODE = 100;
    
    private EditText edEmail, edPassword;
    private AppCompatButton SignInbutton;
    private ProgressBar progressBar;
    private TextView SignUpText, ResPass;

    private SignInButton SigninGoogle;
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient signInClient;




    public SignInFragment(){}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){

        View view = inflater.inflate(R.layout.fragment_sign_in, container, false);

        edEmail=view.findViewById(R.id.EditTextEmailSignIn);
        edPassword=view.findViewById(R.id.EditTextPassSignIn);
        SignInbutton = view.findViewById(R.id.ButtonSignIn);
        progressBar = view.findViewById(R.id.progressBarSignIn);
        progressBar.setVisibility(ViewGroup.GONE);
        SignUpText = view.findViewById(R.id.textSignIntoSignUp);

        SigninGoogle = view.findViewById(R.id.GoogleSignIn);
        ResPass = view.findViewById(R.id.ForgetPassword);
        
        firebaseAuth= FirebaseAuth.getInstance();
        CreateRequest();
        

        SignInbutton.setOnClickListener(view1 -> firebaseSignInUserEmail());
        SignUpText.setOnClickListener(v -> navigateToFragment(new SignUpFragment()));

        // Navigate to Reset Password
        ResPass.setOnClickListener(v -> navigateToFragment(new ResetPassFragment()));
        SigninGoogle.setOnClickListener(view14 -> sign_in_google());


        return view;
}



    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if(user != null){
            logged_in();}
    }

    private void firebaseSignInUserEmail() {

        String email = edEmail.getText().toString().trim(),
                password = edPassword.getText().toString().trim();
        progressBar.setVisibility(ViewGroup.VISIBLE);
        if(TextUtils.isEmpty(email) || TextUtils.isEmpty(password)){
            Toast.makeText(getContext(), "Please enter email and password", Toast.LENGTH_LONG).show();
            progressBar.setVisibility(ViewGroup.GONE);
        }else {
            firebaseAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    progressBar.setVisibility(ViewGroup.GONE);
                    logged_in();
                }

            }).addOnFailureListener(e -> {
                progressBar.setVisibility(ViewGroup.GONE);
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

            });
        }
    }

    private void logged_in(){
        progressBar.setVisibility(ViewGroup.GONE);
        Toast.makeText(getContext(), "Login Successful", Toast.LENGTH_LONG).show();
        //Go TO Homepage
        Intent intent = new Intent(getActivity(), MainActivity.class);
        startActivity(intent);
        getActivity().finish();

    }


    private void CreateRequest() {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(WEB_CLIENT_ID)
                .requestEmail()
                .build();
        signInClient = GoogleSignIn.getClient(requireContext(), signInOptions);
    }
    private void sign_in_google() {
        progressBar.setVisibility(ViewGroup.VISIBLE);
        Intent intent = signInClient.getSignInIntent();
        startActivityForResult(intent,REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_CODE){
            Task<GoogleSignInAccount> task= GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account =task.getResult(ApiException.class);
                firebaseSignInUserGoogle(account);
            } catch (ApiException e) {
                progressBar.setVisibility(ViewGroup.GONE);
                e.printStackTrace();
                Toast.makeText(getContext(), ""+task.getResult(), Toast.LENGTH_SHORT).show();


            }
        }
    }

    private void firebaseSignInUserGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    logged_in();

                }

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressBar.setVisibility(ViewGroup.GONE);
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
    }
    private void navigateToFragment(Fragment fragment) {
        if (fragment != null) {
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            Fragment currentFragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.fragment_container);

            if (currentFragment != null && currentFragment.getClass().equals(fragment.getClass())) {
                // If it's the same fragment, don't add to back stack
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit();
            } else {
                // If it's a different fragment, add to back stack
                getActivity().getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(
                                R.anim.slide_in_right,  // Enter animation
                                R.anim.slide_out_left,   // Exit animation
                                R.anim.slide_in_left,    // Pop enter animation (disabled)
                                R.anim.slide_out_right   // Pop exit animation (disabled)
                        )
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null) // Allow navigating back
                        .commit();
            }
    }}
}

