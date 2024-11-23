package com.example.iot_lab7_20212607.activities.company;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.iot_lab7_20212607.R;
import com.example.iot_lab7_20212607.activities.auth.LoginActivity;
import com.example.iot_lab7_20212607.adapters.CompanyBusLineAdapter;
import com.example.iot_lab7_20212607.databinding.ActivityCompanyMainBinding;
import com.example.iot_lab7_20212607.models.BusLine;
import com.example.iot_lab7_20212607.models.Transaction;
import com.example.iot_lab7_20212607.preferences.PreferenceManager;
import com.example.iot_lab7_20212607.utils.DialogUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CompanyMainActivity extends AppCompatActivity {
    private ActivityCompanyMainBinding binding;
    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private PreferenceManager preferenceManager;
    private CompanyBusLineAdapter busLineAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCompanyMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        preferenceManager = new PreferenceManager(this);

        setupViews();
        loadCompanyData();
        loadBusLines();
    }

    private void setupViews() {
        // Configurar RecyclerView
        binding.rvBusLines.setLayoutManager(new LinearLayoutManager(this));
        busLineAdapter = new CompanyBusLineAdapter(
                busLine -> {
                    Intent intent = new Intent(this, BusEditActivity.class);
                    intent.putExtra("busLineId", busLine.getId());
                    startActivity(intent);
                }
        );
        binding.rvBusLines.setAdapter(busLineAdapter);

        // Configurar FAB para agregar nueva línea
        binding.fabAddBus.setOnClickListener(v -> {
            Intent intent = new Intent(this, BusEditActivity.class);
            startActivity(intent);
        });

        // Configurar botón de logout
        binding.btnLogout.setOnClickListener(v -> logout());
    }

    private void loadCompanyData() {
        String userName = preferenceManager.getUserName();
        binding.tvWelcome.setText(getString(R.string.welcome_message, userName));

        // Cargar ingresos mensuales
        String userId = preferenceManager.getUserId();
        long startOfMonth = getStartOfMonth();

        mFirestore.collection("transactions")
                .whereEqualTo("companyId", userId)
                .whereGreaterThanOrEqualTo("timestamp", startOfMonth)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalRevenue = 0;
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Transaction transaction = document.toObject(Transaction.class);
                        if (transaction != null) {
                            totalRevenue += transaction.getAmount();
                        }
                    }
                    binding.tvMonthlyRevenue.setText(getString(R.string.price_format, totalRevenue));
                });
    }

    private void loadBusLines() {
        String userId = preferenceManager.getUserId();

        DialogUtils.showLoadingDialog(this, getString(R.string.loading_generic));

        mFirestore.collection("busLines")
                .whereEqualTo("companyId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<BusLine> busLines = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        BusLine busLine = document.toObject(BusLine.class);
                        if (busLine != null) {
                            busLine.setId(document.getId());
                            busLines.add(busLine);
                        }
                    }
                    busLineAdapter.setBusLines(busLines);
                    DialogUtils.hideLoadingDialog();

                    // Mostrar mensaje si no hay líneas
                    if (busLines.isEmpty()) {
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                    } else {
                        binding.tvEmptyState.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    DialogUtils.hideLoadingDialog();
                    Toast.makeText(this, getString(R.string.error_generic),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private long getStartOfMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private void logout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirmation)
                .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                    mAuth.signOut();
                    preferenceManager.clearUserData();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBusLines(); // Recargar al volver de edición
    }
}