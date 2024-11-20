package com.example.cartrack.Fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputLayout;
import androidx.fragment.app.Fragment;
import com.example.cartrack.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class AccountFragment extends Fragment {
    private EditText edName, edEmail, edPassword;
    private ProgressBar progressBar;
    private TextInputLayout edPasswordInputLayout;
    private Button btnEdit;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private String CurrentUserID;
    private String loaded_name, loaded_email, loaded_password;

    public AccountFragment(){}

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);
        edName = view.findViewById(R.id.EditTextNameAccount);
        edEmail = view.findViewById(R.id.EditTextEmailAccount);
        edPassword = view.findViewById(R.id.passwordEditText);
        edPasswordInputLayout = view.findViewById(R.id.passwordInputLayout);
        edPasswordInputLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        progressBar = view.findViewById(R.id.progressBarPass);

        btnEdit = view.findViewById(R.id.ButtonAccount);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        CurrentUserID = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();

        Preview();


        return view;
    }

    private void LoadUserData() {
        firestore.collection("Users").document(CurrentUserID).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                loaded_name = Objects.requireNonNull(task.getResult()).getString("userName");
                loaded_email = Objects.requireNonNull(task.getResult()).getString("userEmail");
                loaded_password = Objects.requireNonNull(task.getResult()).getString("userPassword");

                edName.setText(loaded_name);
                edEmail.setText(loaded_email);
                edPassword.setText(loaded_password);
            }else{
                Log.e("LoadUserData", "Error loading user data: " + task.getException());
                edName.setText(R.string.google_error);
                edEmail.setText(R.string.google_error);
                edPassword.setText(R.string.google_error);
            }
        });
        }


    private void UpdateUserData(){
        Loading();
        String name, email, password;
        name = edName.getText().toString().trim().isEmpty() ? loaded_name : edName.getText().toString().trim();
        email = edEmail.getText().toString().trim().isEmpty() ? loaded_email : edEmail.getText().toString().trim();
        password = edPassword.getText().toString().trim().isEmpty() ? loaded_password : edPassword.getText().toString().trim();

        firestore.collection("Users").document(CurrentUserID).update("userName", name, "userEmail", email, "userPassword", password)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Information successfully updated!");
                    Log.d("Firestore", "Name: " + name);
                    Log.d("Firestore", "Email: " + email);
                    Log.d("Firestore", "Password: " + password);
                    Toast.makeText(getContext(), "Information successfully updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error updating information", e);
                    Log.e("Firestore", "Name: " + name);
                    Log.e("Firestore", "Email: " + email);
                    Log.e("Firestore", "Password: " + password);
                    Toast.makeText(getContext(), "Error updating information", Toast.LENGTH_SHORT).show();
                });
        Preview();

    }

    private void Preview(){
        progressBar.setVisibility(ViewGroup.GONE);
        edName.setEnabled(false);
        edEmail.setEnabled(false);
        edPassword.setEnabled(false);
        btnEdit.setText(R.string.edit);
        btnEdit.setOnClickListener(view -> Editing());
        btnEdit.setAlpha(1f);
        LoadUserData();

    }
    private void Editing(){
        progressBar.setVisibility(ViewGroup.GONE);
        edName.setEnabled(true);
        edEmail.setEnabled(true);
        edPassword.setEnabled(true);
        btnEdit.setText(R.string.save);
        btnEdit.setOnClickListener(view -> UpdateUserData());
        btnEdit.setAlpha(1f);
    }

    private void Loading(){
        progressBar.setVisibility(ViewGroup.VISIBLE);
        edEmail.setEnabled(false);
        edName.setEnabled(false);
        edPassword.setEnabled(false);
        btnEdit.setEnabled(false);
        btnEdit.setAlpha(0.5f);
    }
}