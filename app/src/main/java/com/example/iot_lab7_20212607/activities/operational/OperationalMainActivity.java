package com.example.iot_lab7_20212607.activities.operational;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.iot_lab7_20212607.R;
import com.example.iot_lab7_20212607.activities.auth.LoginActivity;
import com.example.iot_lab7_20212607.adapters.BusLineAdapter;
import com.example.iot_lab7_20212607.databinding.ActivityOperationalMainBinding;
import com.example.iot_lab7_20212607.models.BusLine;
import com.example.iot_lab7_20212607.preferences.PreferenceManager;
import com.example.iot_lab7_20212607.utils.DialogUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class OperationalMainActivity extends AppCompatActivity {
    private ActivityOperationalMainBinding binding;
    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private PreferenceManager preferenceManager;
    private BusLineAdapter busLineAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOperationalMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        preferenceManager = new PreferenceManager(this);

        setupViews();
        setupUser();
        loadBusLines();
    }

    private void setupViews() {
        // Configurar RecyclerView
        binding.rvBusLines.setLayoutManager(new LinearLayoutManager(this));
        busLineAdapter = new BusLineAdapter(bus -> {
            Intent intent = new Intent(this, BusDetailActivity.class);
            intent.putExtra("busLineId", bus.getId());
            startActivity(intent);
        });
        binding.rvBusLines.setAdapter(busLineAdapter);

        // Configurar BottomNavigationView
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_scan) {
                startActivity(new Intent(this, QrScannerActivity.class));
                return true;
            }
            return false;
        });

        // Configurar botón de logout
        binding.btnLogout.setOnClickListener(v -> logout());
    }

    private void setupUser() {
        String userName = preferenceManager.getUserName();
        double balance = preferenceManager.getUserBalance();

        binding.tvWelcome.setText(getString(R.string.welcome_message, userName));
        binding.tvBalance.setText(getString(R.string.label_balance, balance));
    }

    private void loadBusLines() {
        DialogUtils.showLoadingDialog(this, getString(R.string.loading_generic));

        mFirestore.collection("busLines")
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
                })
                .addOnFailureListener(e -> {
                    DialogUtils.hideLoadingDialog();
                    Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show();
                });
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
}