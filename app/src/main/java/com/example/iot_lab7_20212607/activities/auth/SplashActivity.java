package com.example.iot_lab7_20212607.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.iot_lab7_20212607.R;
import com.example.iot_lab7_20212607.activities.company.CompanyMainActivity;
import com.example.iot_lab7_20212607.activities.operational.OperationalMainActivity;
import com.example.iot_lab7_20212607.databinding.ActivitySplashBinding;
import com.example.iot_lab7_20212607.models.User;
import com.example.iot_lab7_20212607.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {
    private ActivitySplashBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        // Esperar 2 segundos y verificar autenticación
        new Handler(Looper.getMainLooper()).postDelayed(this::checkAuth, 2000);

    }

    private void checkAuth() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Usuario ya está autenticado
            checkUserRoleAndRedirect(currentUser);
        } else {
            // Usuario no está autenticado, ir al login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void checkUserRoleAndRedirect(FirebaseUser firebaseUser) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        Intent intent;
                        if (Constants.ROLE_COMPANY.equals(user.getRole())) {
                            intent = new Intent(this, CompanyMainActivity.class);
                        } else {
                            intent = new Intent(this, OperationalMainActivity.class);
                        }
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    // En caso de error, enviar al login
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                });
    }
}