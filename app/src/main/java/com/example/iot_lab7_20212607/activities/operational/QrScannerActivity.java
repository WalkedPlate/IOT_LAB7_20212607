package com.example.iot_lab7_20212607.activities.operational;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.budiyev.android.codescanner.CodeScanner;
import com.example.iot_lab7_20212607.R;
import com.example.iot_lab7_20212607.databinding.ActivityQrScannerBinding;
import com.example.iot_lab7_20212607.models.BusLine;
import com.example.iot_lab7_20212607.models.Transaction;
import com.example.iot_lab7_20212607.models.User;
import com.example.iot_lab7_20212607.preferences.PreferenceManager;
import com.example.iot_lab7_20212607.utils.Constants;
import com.example.iot_lab7_20212607.utils.DialogUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class QrScannerActivity extends AppCompatActivity {
    private ActivityQrScannerBinding binding;
    private CodeScanner codeScanner;
    private FirebaseFirestore mFirestore;
    private PreferenceManager preferenceManager;

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private String lastScannedBusId = null;
    private long lastEntryTimestamp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQrScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mFirestore = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(this);

        setupQRScanner();
        requestCameraPermission();
    }

    private void setupQRScanner() {
        codeScanner = new CodeScanner(this, binding.scannerView);
        codeScanner.setDecodeCallback(result -> runOnUiThread(() -> handleQRResult(result.getText())));
        codeScanner.setErrorCallback(error -> runOnUiThread(() ->
                Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show()));

        binding.scannerView.setOnClickListener(v -> codeScanner.startPreview());
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        } else {
            codeScanner.startPreview();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                codeScanner.startPreview();
            } else {
                Toast.makeText(this, getString(R.string.error_no_camera), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void handleQRResult(String result) {
        try {
            // El QR debe contener el ID del bus en formato: "BUS_ID:busLineId"
            if (!result.startsWith("BUS_ID:")) {
                Toast.makeText(this, getString(R.string.error_invalid_qr),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String busLineId = result.substring(7);
            String userId = preferenceManager.getUserId();

            // Verificar si es entrada o salida
            mFirestore.collection("transactions")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("busLineId", busLineId)
                    .whereEqualTo("type", Constants.TRANSACTION_ENTRY)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots.isEmpty()) {
                            // No hay transacción de entrada previa, procesar entrada
                            processEntry(busLineId);
                        } else {
                            // Existe transacción de entrada, procesar salida
                            DocumentSnapshot lastEntry = queryDocumentSnapshots.getDocuments().get(0);
                            Transaction entryTransaction = lastEntry.toObject(Transaction.class);
                            if (entryTransaction != null) {
                                processExit(busLineId, entryTransaction.getTimestamp());
                            }
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this,
                            getString(R.string.error_generic), Toast.LENGTH_SHORT).show());

        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_invalid_qr), Toast.LENGTH_SHORT).show();
        }
    }

    private void processEntry(String busLineId) {
        DialogUtils.showLoadingDialog(this, getString(R.string.processing_entry));

        String userId = preferenceManager.getUserId();
        double currentBalance = preferenceManager.getUserBalance();

        // Verificar si el usuario tiene suscripción
        mFirestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        boolean hasSubscription = user.getSubscriptions() != null &&
                                user.getSubscriptions().contains(busLineId);

                        // Obtener información del bus
                        mFirestore.collection("busLines")
                                .document(busLineId)
                                .get()
                                .addOnSuccessListener(busDocument -> {
                                    BusLine busLine = busDocument.toObject(BusLine.class);
                                    if (busLine != null) {
                                        if (!hasSubscription) {
                                            if (currentBalance < busLine.getUnitPrice()) {
                                                DialogUtils.hideLoadingDialog();
                                                Toast.makeText(this,
                                                        getString(R.string.insufficient_balance),
                                                        Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                            // Actualizar saldo
                                            double newBalance = currentBalance - busLine.getUnitPrice();
                                            updateUserBalance(userId, newBalance);
                                            preferenceManager.updateBalance(newBalance);
                                        }

                                        // Registrar transacción
                                        Transaction transaction = new Transaction();
                                        transaction.setUserId(userId);
                                        transaction.setBusLineId(busLineId);
                                        transaction.setType(Constants.TRANSACTION_ENTRY);
                                        transaction.setAmount(hasSubscription ? 0 : busLine.getUnitPrice());
                                        transaction.setTimestamp(System.currentTimeMillis());

                                        mFirestore.collection("transactions")
                                                .add(transaction)
                                                .addOnSuccessListener(documentReference -> {
                                                    DialogUtils.hideLoadingDialog();
                                                    showSuccessDialog(true, transaction.getAmount(),
                                                            preferenceManager.getUserBalance());
                                                });
                                    }
                                });
                    }
                });
    }

    private void processExit(String busLineId, long entryTimestamp) {
        DialogUtils.showLoadingDialog(this, getString(R.string.processing_exit));

        long tripDuration = (System.currentTimeMillis() - entryTimestamp) / (1000 * 60); // en minutos
        double cashbackPercentage = tripDuration < Constants.CASHBACK_THRESHOLD_MINUTES ?
                Constants.CASHBACK_HIGH_PERCENTAGE : Constants.CASHBACK_LOW_PERCENTAGE;

        String userId = preferenceManager.getUserId();

        // Obtener la última transacción de entrada para calcular el cashback
        mFirestore.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("busLineId", busLineId)
                .whereEqualTo("type", Constants.TRANSACTION_ENTRY)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Transaction entryTransaction = queryDocumentSnapshots.getDocuments()
                                .get(0).toObject(Transaction.class);
                        if (entryTransaction != null) {
                            double cashbackAmount = entryTransaction.getAmount() * cashbackPercentage;
                            if (cashbackAmount > 0) {
                                // Actualizar saldo con cashback
                                double currentBalance = preferenceManager.getUserBalance();
                                double newBalance = currentBalance + cashbackAmount;
                                updateUserBalance(userId, newBalance);
                                preferenceManager.updateBalance(newBalance);
                            }

                            // Registrar transacción de salida
                            Transaction exitTransaction = new Transaction();
                            exitTransaction.setUserId(userId);
                            exitTransaction.setBusLineId(busLineId);
                            exitTransaction.setType(Constants.TRANSACTION_EXIT);
                            exitTransaction.setAmount(0);
                            exitTransaction.setCashback(cashbackAmount);
                            exitTransaction.setTimestamp(System.currentTimeMillis());

                            mFirestore.collection("transactions")
                                    .add(exitTransaction)
                                    .addOnSuccessListener(documentReference -> {
                                        DialogUtils.hideLoadingDialog();
                                        showSuccessDialog(false, cashbackAmount,
                                                preferenceManager.getUserBalance());
                                    });
                        }
                    }
                });
    }

    private void updateUserBalance(String userId, double newBalance) {
        mFirestore.collection("users")
                .document(userId)
                .update("balance", newBalance);
    }

    private void showSuccessDialog(boolean isEntry, double amount, double newBalance) {
        View view = getLayoutInflater().inflate(R.layout.dialog_qr_success, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        TextView tvMessage = view.findViewById(R.id.tvMessage);
        TextView tvAmount = view.findViewById(R.id.tvAmount);
        TextView tvBalance = view.findViewById(R.id.tvBalance);
        Button btnOk = view.findViewById(R.id.btnOk);

        tvMessage.setText(isEntry ? R.string.success_entry_title : R.string.success_exit_title);
        if (amount > 0) {
            tvAmount.setVisibility(View.VISIBLE);
            String amountText = isEntry ?
                    getString(R.string.amount_charged, amount) :
                    getString(R.string.amount_refunded, amount);
            tvAmount.setText(amountText);
            tvAmount.setTextColor(ContextCompat.getColor(this,
                    isEntry ? R.color.error : R.color.success));
        } else {
            tvAmount.setVisibility(View.GONE);
        }
        tvBalance.setText(getString(R.string.current_balance, newBalance));

        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (codeScanner != null) {
            codeScanner.startPreview();
        }
    }

    @Override
    protected void onPause() {
        if (codeScanner != null) {
            codeScanner.releaseResources();
        }
        super.onPause();
    }
}