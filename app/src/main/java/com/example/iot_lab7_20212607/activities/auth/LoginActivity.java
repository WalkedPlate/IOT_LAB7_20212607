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
import com.example.iot_lab7_20212607.models.BusLine;
import com.example.iot_lab7_20212607.models.User;
import com.example.iot_lab7_20212607.preferences.PreferenceManager;
import com.example.iot_lab7_20212607.utils.Constants;
import com.example.iot_lab7_20212607.utils.DialogUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.OAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private PreferenceManager preferenceManager;

    private static final String COMPANY_EMAIL = " ampueromario@hotmail.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(this);


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
        Log.d("LoginActivity", "Iniciando proceso de login con Microsoft");
        showLoading(true);

        OAuthProvider.Builder provider = OAuthProvider.newBuilder("microsoft.com");
        provider.addCustomParameter("prompt", "select_account");
        List<String> scopes = Arrays.asList("user.read", "profile", "email");
        provider.setScopes(scopes);

        Log.d("LoginActivity", "Iniciando actividad de sign in con provider");
        mAuth.startActivityForSignInWithProvider(this, provider.build())
                .addOnSuccessListener(authResult -> {
                    Log.d("LoginActivity", "Login con Microsoft exitoso");
                    FirebaseUser user = authResult.getUser();
                    createUserIfNotExists(user);
                })
                .addOnFailureListener(e -> {
                    Log.e("LoginActivity", "Error en login con Microsoft", e);
                    showLoading(false);
                    Toast.makeText(LoginActivity.this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void createUserIfNotExists(FirebaseUser firebaseUser) {
        Log.d("LoginActivity", "Verificando si el usuario existe: " + firebaseUser.getUid());
        mFirestore.collection("users")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    Log.d("LoginActivity", "Consulta a Firestore exitosa. Existe documento: " + document.exists());
                    if (!document.exists()) {
                        createNewUser(firebaseUser);
                    } else {
                        checkUserAndRedirect(firebaseUser);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("LoginActivity", "Error al verificar usuario en Firestore", e);
                    showLoading(false);
                    Toast.makeText(LoginActivity.this,
                            getString(R.string.error_generic),
                            Toast.LENGTH_SHORT).show();
                });
    }
    private void createNewUser(FirebaseUser firebaseUser) {
        Log.d("LoginActivity", "Creando nuevo usuario con email: " + firebaseUser.getEmail());

        User newUser = new User();
        newUser.setName(firebaseUser.getDisplayName());
        newUser.setEmail(firebaseUser.getEmail());

        if (COMPANY_EMAIL.equalsIgnoreCase(firebaseUser.getEmail())) {
            newUser.setRole(Constants.ROLE_COMPANY);
            newUser.setBalance(0.0);
            Log.d("LoginActivity", "Usuario detectado como empresa");

            // Primero creamos el usuario
            mFirestore.collection("users")
                    .document(firebaseUser.getUid())
                    .set(newUser)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("LoginActivity", "Usuario empresa creado exitosamente");

                        // Actualizar todos los busLines con el nuevo companyId
                        updateBusLinesCompanyId(firebaseUser.getUid(), newUser);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("LoginActivity", "Error al crear usuario empresa", e);
                        showLoading(false);
                        Toast.makeText(LoginActivity.this,
                                getString(R.string.error_generic),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Usuario operativo normal
            newUser.setRole(Constants.ROLE_OPERATIONAL);
            newUser.setBalance(Constants.INITIAL_BALANCE);
            Log.d("LoginActivity", "Usuario detectado como operativo");

            mFirestore.collection("users")
                    .document(firebaseUser.getUid())
                    .set(newUser)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("LoginActivity", "Usuario operativo creado exitosamente");
                        preferenceManager.saveUserData(newUser, firebaseUser.getUid());

                        Intent intent = new Intent(LoginActivity.this, OperationalMainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("LoginActivity", "Error al crear usuario operativo", e);
                        showLoading(false);
                        Toast.makeText(LoginActivity.this,
                                getString(R.string.error_generic),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateBusLinesCompanyId(String companyId, User companyUser) {
        DialogUtils.showLoadingDialog(this, "Configurando líneas de bus...");

        // Obtener todas las líneas de bus
        mFirestore.collection("busLines")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Usar WriteBatch para actualización en lote
                    WriteBatch batch = mFirestore.batch();
                    boolean hasUpdates = false;

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        // Actualizar cada documento con el nuevo companyId
                        DocumentReference busLineRef = mFirestore.collection("busLines")
                                .document(document.getId());
                        batch.update(busLineRef, "companyId", companyId);
                        hasUpdates = true;
                    }

                    if (hasUpdates) {
                        // Ejecutar todas las actualizaciones en una sola transacción
                        batch.commit()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("LoginActivity", "Líneas de bus actualizadas exitosamente");
                                    DialogUtils.hideLoadingDialog();
                                    finishCompanySetup(companyId, companyUser);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("LoginActivity", "Error actualizando líneas de bus", e);
                                    DialogUtils.hideLoadingDialog();
                                    // Aun así continuamos con el flujo
                                    finishCompanySetup(companyId, companyUser);
                                });
                    } else {
                        // No hay líneas de bus para actualizar
                        Log.d("LoginActivity", "No se encontraron líneas de bus para actualizar");
                        DialogUtils.hideLoadingDialog();
                        finishCompanySetup(companyId, companyUser);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("LoginActivity", "Error obteniendo líneas de bus", e);
                    DialogUtils.hideLoadingDialog();
                    // Aun así continuamos con el flujo
                    finishCompanySetup(companyId, companyUser);
                });
    }

    private void finishCompanySetup(String companyId, User companyUser) {
        // Guardar datos en SharedPreferences
        preferenceManager.saveUserData(companyUser, companyId);

        // Redirigir a la vista de empresa
        Intent intent = new Intent(LoginActivity.this, CompanyMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    private void checkUserAndRedirect(FirebaseUser firebaseUser) {
        Log.d("LoginActivity", "Verificando rol de usuario existente");
        mFirestore.collection("users")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        Log.d("LoginActivity", "Usuario encontrado con rol: " + user.getRole());
                        // Guardar datos en SharedPreferences
                        preferenceManager.saveUserData(user, firebaseUser.getUid());

                        Intent intent;
                        if (Constants.ROLE_COMPANY.equals(user.getRole())) {
                            intent = new Intent(LoginActivity.this, CompanyMainActivity.class);
                            Log.d("LoginActivity", "Redirigiendo a CompanyMainActivity");
                        } else {
                            intent = new Intent(LoginActivity.this, OperationalMainActivity.class);
                            Log.d("LoginActivity", "Redirigiendo a OperationalMainActivity");
                        }
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("LoginActivity", "Error al verificar usuario", e);
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