package com.example.cartrack.Fragments;
import static androidx.navigation.fragment.FragmentKt.findNavController;

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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.TotpSecret;

public class ResetPassFragment extends Fragment {

    private EditText edEmail;
    private AppCompatButton butRes;
    private ProgressBar progressBar;
    private TextView LoginText;
    FirebaseAuth firebaseAuth;

    public ResetPassFragment(){}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_respass, container, false);

        edEmail = view.findViewById(R.id.EditTextEmailPass);
        butRes = view.findViewById(R.id.ButtonResetPass);
        progressBar = view.findViewById(R.id.progressBarPass);
        LoginText = view.findViewById(R.id.textPasstoSignIn);
        firebaseAuth = FirebaseAuth.getInstance();
        butRes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Método SignUp
                ResetPassword();
            }
        });
        LoginText.setOnClickListener(v -> navigateToFragment(new SignInFragment()));





        return view;
}

    private void ResetPassword() {
        String email = edEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)){
            edEmail.setError("Email cannot be empty");
        }else{
            progressBar.setVisibility(View.VISIBLE);
            firebaseAuth.sendPasswordResetEmail(email).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Reset password link sent", Toast.LENGTH_SHORT).show();
                    new Handler().postDelayed(() -> {
                        navigateToFragment(new SignInFragment());
                    }, 1000);

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);

                }
            });

        }
    }
    private void navigateToFragment(Fragment fragment) {
        if (fragment != null) {
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();

            // Set custom animations
            transaction.setCustomAnimations(
                    R.anim.slide_in_left,  // Enter animation
                    R.anim.slide_out_right,   // Exit animation
                    R.anim.slide_in_left,    // Pop enter animation
                    R.anim.slide_out_right    // Pop exit animation
            );

            transaction.replace(R.id.fragment_container, fragment)
                    .commit();
        }
    }
}
