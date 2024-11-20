package com.example.cartrack.Fragments;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.Navigation;

import com.example.cartrack.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.cartrack.Model.userModel;
import com.google.firebase.firestore.SetOptions;
public class SignUpFragment  extends Fragment {
    private EditText edName, edEmail, edPassword;
    private AppCompatButton SignUpButton;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    String Userid;
    private ProgressBar progressBar;
    private TextView SignInText;


    public SignUpFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_up, container, false);

        edName = view.findViewById(R.id.EditTextNameSignUp);
        edEmail = view.findViewById(R.id.EditTextEmailPass);
        edPassword = view.findViewById(R.id.EditTextPassSignUp);
        SignUpButton = view.findViewById(R.id.ButtonResetPass);
        progressBar = view.findViewById(R.id.progressBarPass);
        progressBar.setVisibility(ViewGroup.GONE);
        SignInText = view.findViewById(R.id.textSignUptoSignIn);


        //Crear instancia firebase y firestore
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        //Pulsar SignUp
        SignUpButton.setOnClickListener(view1 -> {
            //Método SignUp
            SignUpUser();
        });
        SignInText.setOnClickListener(v -> navigateToFragment(new SignInFragment()));


        return view;

    }

    private void SignUpUser() {
        String name = edName.getText().toString().trim();
        String email = edEmail.getText().toString().trim();
        String password = edPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_LONG).show();
            return;
        }

        progressBar.setVisibility(ViewGroup.VISIBLE);
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // User created successfully
                        Userid = firebaseAuth.getCurrentUser().getUid();
                        DocumentReference UserInfo = firestore.collection("Users").document(Userid);
                        userModel model = new userModel(name, email, password, Userid);

                        UserInfo.set(model, SetOptions.merge()).addOnSuccessListener(unused -> {
                            progressBar.setVisibility(ViewGroup.GONE);
                            Toast.makeText(getContext(), "User registered successfully", Toast.LENGTH_LONG).show();
                            // Optionally navigate after a delay
                            new Handler().postDelayed(() -> {
                                navigateToFragment(new SignInFragment());
                            }, 2000);
                        }).addOnFailureListener(e -> {
                            progressBar.setVisibility(ViewGroup.GONE);
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    } else {
                        // Handle user creation failure
                        progressBar.setVisibility(ViewGroup.GONE);
                        Toast.makeText(getContext(), task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                }).addOnFailureListener(e -> {
                    progressBar.setVisibility(ViewGroup.GONE);
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
    private void navigateToFragment(Fragment fragment) {
        if (fragment != null) {
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();

            // Set custom animations
            transaction.setCustomAnimations(
                    R.anim.slide_in_left,  // Enter animation
                    R.anim.slide_out_right,   // Exit animation
                    R.anim.slide_in_right,    // Pop enter animation
                    0    // Pop exit animation
            );

            transaction.replace(R.id.fragment_container, fragment)
                    .commit();
        }
    }
}



