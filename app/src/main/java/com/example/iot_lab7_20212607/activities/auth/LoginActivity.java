package com.example.iot_lab7_20212607.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.iot_lab7_20212607.R;
import com.example.iot_lab7_20212607.activities.company.CompanyMainActivity;
import com.example.iot_lab7_20212607.activities.operational.OperationalMainActivity;
import com.example.iot_lab7_20212607.databinding.ActivityLoginBinding;
import com.example.iot_lab7_20212607.models.User;
import com.example.iot_lab7_20212607.utils.Constants;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.OAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        setupLoginButton();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkUserAndRedirect(currentUser);
        }
    }

    private void setupLoginButton() {
        binding.btnMicrosoftLogin.setOnClickListener(v -> signInWithMicrosoft());
    }

    private void signInWithMicrosoft() {
        showLoading(true);

        OAuthProvider.Builder provider = OAuthProvider.newBuilder("microsoft.com");
        // Agregamos scopes necesarios
        provider.addCustomParameter("prompt", "select_account");
        List<String> scopes = Arrays.asList("user.read", "profile", "email");
        provider.setScopes(scopes);

        mAuth.startActivityForSignInWithProvider(this, provider.build())
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = task.getResult().getUser();
                        createUserIfNotExists(user);
                    } else {
                        showLoading(false);
                        // Mostrar el error específico
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() :
                                getString(R.string.login_error);
                        Toast.makeText(LoginActivity.this,
                                "Error: " + errorMessage,
                                Toast.LENGTH_LONG).show();
                        Log.e("LoginActivity", "Error en login: ", task.getException());
                    }
                });
    }

    private void createUserIfNotExists(FirebaseUser firebaseUser) {
        mFirestore.collection("users")
                .document(firebaseUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (!document.exists()) {
                            // Crear nuevo usuario operativo por defecto
                            User newUser = new User();
                            newUser.setName(firebaseUser.getDisplayName());
                            newUser.setEmail(firebaseUser.getEmail());
                            newUser.setRole(Constants.ROLE_OPERATIONAL);
                            newUser.setBalance(Constants.INITIAL_BALANCE);

                            mFirestore.collection("users")
                                    .document(firebaseUser.getUid())
                                    .set(newUser)
                                    .addOnSuccessListener(aVoid -> {
                                        Intent intent = new Intent(LoginActivity.this, OperationalMainActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        showLoading(false);
                                        Toast.makeText(LoginActivity.this,
                                                getString(R.string.error_generic),
                                                Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            // Usuario ya existe
                            checkUserAndRedirect(firebaseUser);
                        }
                    } else {
                        showLoading(false);
                        Toast.makeText(LoginActivity.this,
                                getString(R.string.error_generic),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserAndRedirect(FirebaseUser firebaseUser) {
        mFirestore.collection("users")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        Intent intent;
                        if (Constants.ROLE_COMPANY.equals(user.getRole())) {
                            intent = new Intent(LoginActivity.this, CompanyMainActivity.class);
                        } else {
                            intent = new Intent(LoginActivity.this, OperationalMainActivity.class);
                        }
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this,
                            getString(R.string.error_generic),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnMicrosoftLogin.setEnabled(!show);
    }
}