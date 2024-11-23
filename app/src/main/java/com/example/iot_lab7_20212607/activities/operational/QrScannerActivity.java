package com.example.iot_lab7_20212607.activities.operational;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
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
import com.google.zxing.BarcodeFormat;

import java.util.Collections;

public class QrScannerActivity extends AppCompatActivity {
    private ActivityQrScannerBinding binding;
    private CodeScanner codeScanner;
    private FirebaseFirestore mFirestore;
    private PreferenceManager preferenceManager;

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private boolean isFlashEnabled = false;

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

        // Configuraciones básicas
        codeScanner.setCamera(CodeScanner.CAMERA_BACK);
        codeScanner.setFormats(Collections.singletonList(BarcodeFormat.QR_CODE));
        codeScanner.setAutoFocusEnabled(true);
        codeScanner.setFlashEnabled(false);

        setupFlash();


        // Callback para decodificación
        codeScanner.setDecodeCallback(result -> runOnUiThread(() -> {
            Log.d("QrScannerActivity", "QR content: " + result.getText());
            handleQRResult(result.getText());
        }));

        // Callback de error
        codeScanner.setErrorCallback(error -> runOnUiThread(() -> {
            Log.e("QrScannerActivity", "Scanner error: " + error.getMessage());
            Toast.makeText(this, getString(R.string.error_camera), Toast.LENGTH_SHORT).show();
        }));

        binding.scannerView.setOnClickListener(v -> codeScanner.startPreview());
    }

    private void updateBalanceUI() {
        // Asumiendo que tienes un TextView para mostrar el saldo en la vista
        TextView tvBalance = findViewById(R.id.tvBalance);
        if (tvBalance != null) {
            double currentBalance = preferenceManager.getUserBalance();
            tvBalance.setText(getString(R.string.balance_format, currentBalance));
        }
    }


    private void setupFlash() {
        // Verificar si el dispositivo tiene flash
        if (getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            binding.btnFlash.setVisibility(View.VISIBLE);

            // Establecer el estado inicial
            updateFlashButton(false);

            binding.btnFlash.setOnClickListener(v -> {
                try {
                    isFlashEnabled = !isFlashEnabled;
                    codeScanner.setFlashEnabled(isFlashEnabled);
                    updateFlashButton(isFlashEnabled);

                    // Mostrar feedback al usuario
                    Toast.makeText(this,
                            getString(isFlashEnabled ? R.string.flash_on : R.string.flash_off),
                            Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    Log.e("QrScannerActivity", "Error toggling flash", e);
                    Toast.makeText(this, getString(R.string.error_flash_toggle),
                            Toast.LENGTH_SHORT).show();
                    // Revertir el estado en caso de error
                    isFlashEnabled = !isFlashEnabled;
                    updateFlashButton(isFlashEnabled);
                }
            });
        } else {
            // Si no hay flash disponible, ocultar el botón
            binding.btnFlash.setVisibility(View.GONE);
        }
    }

    private void updateFlashButton(boolean isEnabled) {
        binding.btnFlash.setImageResource(isEnabled ?
                R.drawable.ic_flash_on : R.drawable.ic_flash_off);
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
        Log.d("QrScannerActivity", "Processing QR: " + result);

        if (result == null || !result.startsWith("BUS_ID:")) {
            Toast.makeText(this, getString(R.string.error_invalid_qr), Toast.LENGTH_SHORT).show();
            codeScanner.startPreview();
            return;
        }

        String busLineId = result.substring(7);
        String userId = preferenceManager.getUserId();

        // Primero verificar que el bus existe
        mFirestore.collection("busLines")
                .document(busLineId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, getString(R.string.error_invalid_bus),
                                Toast.LENGTH_SHORT).show();
                        codeScanner.startPreview();
                        return;
                    }

                    // Verificar si hay una transacción de entrada sin salida correspondiente
                    mFirestore.collection("transactions")
                            .whereEqualTo("userId", userId)
                            .whereEqualTo("busLineId", busLineId)
                            .whereEqualTo("type", Constants.TRANSACTION_ENTRY)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (querySnapshot.isEmpty()) {
                                    // No hay entrada previa, procesar como nueva entrada
                                    processEntry(busLineId);
                                    return;
                                }

                                // Obtener la última entrada
                                Transaction lastEntry = querySnapshot.getDocuments()
                                        .get(0)
                                        .toObject(Transaction.class);

                                if (lastEntry == null) {
                                    processEntry(busLineId);
                                    return;
                                }

                                // Verificar si ya existe una salida para esta entrada
                                mFirestore.collection("transactions")
                                        .whereEqualTo("userId", userId)
                                        .whereEqualTo("busLineId", busLineId)
                                        .whereEqualTo("type", Constants.TRANSACTION_EXIT)
                                        .whereGreaterThan("timestamp", lastEntry.getTimestamp())
                                        .get()
                                        .addOnSuccessListener(exitSnapshots -> {
                                            if (exitSnapshots.isEmpty()) {
                                                // No hay salida registrada, procesar salida
                                                processExit(busLineId, lastEntry.getTimestamp());
                                            } else {
                                                // Ya existe una salida, procesar como nueva entrada
                                                processEntry(busLineId);
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("QrScannerActivity", "Error verificando salidas", e);
                                            Toast.makeText(this,
                                                    getString(R.string.error_check_transactions),
                                                    Toast.LENGTH_SHORT).show();
                                            codeScanner.startPreview();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e("QrScannerActivity", "Error verificando entradas", e);
                                Toast.makeText(this,
                                        getString(R.string.error_check_transactions),
                                        Toast.LENGTH_SHORT).show();
                                codeScanner.startPreview();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("QrScannerActivity", "Error verificando bus", e);
                    Toast.makeText(this, getString(R.string.error_check_bus),
                            Toast.LENGTH_SHORT).show();
                    codeScanner.startPreview();
                });
    }


    private void processEntry(String busLineId) {
        DialogUtils.showLoadingDialog(this, getString(R.string.processing_entry));

        // Timeout
        Handler timeoutHandler = new Handler();
        Runnable timeoutRunnable = () -> {
            if (!isFinishing()) {
                DialogUtils.hideLoadingDialog();
                Toast.makeText(this, getString(R.string.error_timeout), Toast.LENGTH_SHORT).show();
                codeScanner.startPreview();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 100000);

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
                                                timeoutHandler.removeCallbacks(timeoutRunnable);
                                                DialogUtils.hideLoadingDialog();

                                                // Mostrar diálogo informativo de saldo insuficiente
                                                showInsufficientBalanceDialog(
                                                        currentBalance,
                                                        busLine.getUnitPrice(),
                                                        busLine.getName()
                                                );
                                                return;
                                            }
                                            // Actualizar saldo
                                            double newBalance = currentBalance - busLine.getUnitPrice();
                                            // Actualizar en Firebase
                                            updateUserBalance(userId, newBalance);
                                            // Actualizar en SharedPreferences
                                            preferenceManager.updateBalance(newBalance);
                                        }

                                        // Registrar transacción
                                        Transaction transaction = new Transaction();
                                        transaction.setUserId(userId);
                                        transaction.setBusLineId(busLineId);
                                        transaction.setType(Constants.TRANSACTION_ENTRY);
                                        transaction.setAmount(hasSubscription ? 0 : busLine.getUnitPrice());
                                        transaction.setTimestamp(System.currentTimeMillis());
                                        transaction.setStatus(Constants.TRANSACTION_STATUS_COMPLETED); // Nuevo campo

                                        mFirestore.collection("transactions")
                                                .add(transaction)
                                                .addOnSuccessListener(documentReference -> {
                                                    timeoutHandler.removeCallbacks(timeoutRunnable);
                                                    DialogUtils.hideLoadingDialog();
                                                    showSuccessDialog(true, transaction.getAmount(),
                                                            preferenceManager.getUserBalance(),
                                                            busLine.getName());
                                                })
                                                .addOnFailureListener(e -> {
                                                    timeoutHandler.removeCallbacks(timeoutRunnable);
                                                    DialogUtils.hideLoadingDialog();
                                                    Toast.makeText(this, getString(R.string.error_saving_transaction),
                                                            Toast.LENGTH_SHORT).show();
                                                    codeScanner.startPreview();
                                                });
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    timeoutHandler.removeCallbacks(timeoutRunnable);
                                    DialogUtils.hideLoadingDialog();
                                    Toast.makeText(this, getString(R.string.error_check_bus),
                                            Toast.LENGTH_SHORT).show();
                                    codeScanner.startPreview();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    DialogUtils.hideLoadingDialog();
                    Toast.makeText(this, getString(R.string.error_checking_user),
                            Toast.LENGTH_SHORT).show();
                    codeScanner.startPreview();
                });
    }

    private void processExit(String busLineId, long entryTimestamp) {
        Log.d("QrScannerActivity", "Iniciando processExit - busLineId: " + busLineId);
        DialogUtils.showLoadingDialog(this, getString(R.string.processing_exit));

        // Timeout handler
        Handler timeoutHandler = new Handler();
        Runnable timeoutRunnable = () -> {
            if (!isFinishing()) {
                DialogUtils.hideLoadingDialog();
                Toast.makeText(this, getString(R.string.error_timeout), Toast.LENGTH_SHORT).show();
                codeScanner.startPreview();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 100000);

        String userId = preferenceManager.getUserId();

        // Primero obtenemos la información del bus
        mFirestore.collection("busLines")
                .document(busLineId)
                .get()
                .addOnSuccessListener(busLineDoc -> {
                    if (!busLineDoc.exists()) {
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        DialogUtils.hideLoadingDialog();
                        Toast.makeText(this, getString(R.string.error_invalid_bus), Toast.LENGTH_SHORT).show();
                        codeScanner.startPreview();
                        return;
                    }

                    BusLine busLine = busLineDoc.toObject(BusLine.class);
                    if (busLine == null) {
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        DialogUtils.hideLoadingDialog();
                        Toast.makeText(this, getString(R.string.error_invalid_bus), Toast.LENGTH_SHORT).show();
                        codeScanner.startPreview();
                        return;
                    }

                    // Buscamos la última transacción de entrada
                    mFirestore.collection("transactions")
                            .whereEqualTo("userId", userId)
                            .whereEqualTo("busLineId", busLineId)
                            .whereEqualTo("type", Constants.TRANSACTION_ENTRY)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(entrySnapshots -> {
                                if (entrySnapshots.isEmpty()) {
                                    timeoutHandler.removeCallbacks(timeoutRunnable);
                                    DialogUtils.hideLoadingDialog();
                                    Toast.makeText(this, getString(R.string.error_no_entry_found),
                                            Toast.LENGTH_SHORT).show();
                                    codeScanner.startPreview();
                                    return;
                                }

                                Transaction entryTransaction = entrySnapshots.getDocuments()
                                        .get(0)
                                        .toObject(Transaction.class);

                                if (entryTransaction == null) {
                                    timeoutHandler.removeCallbacks(timeoutRunnable);
                                    DialogUtils.hideLoadingDialog();
                                    Toast.makeText(this, getString(R.string.error_invalid_entry),
                                            Toast.LENGTH_SHORT).show();
                                    codeScanner.startPreview();
                                    return;
                                }

                                // Calcular cashback
                                long currentTime = System.currentTimeMillis();
                                long tripDuration = (currentTime - entryTransaction.getTimestamp()) / (1000 * 60);

                                double cashbackPercentage = tripDuration < Constants.CASHBACK_THRESHOLD_MINUTES ?
                                        Constants.CASHBACK_HIGH_PERCENTAGE :
                                        Constants.CASHBACK_LOW_PERCENTAGE;

                                double entryAmount = entryTransaction.getAmount();
                                double cashbackAmount = entryAmount * cashbackPercentage;

                                // Crear y guardar transacción de salida
                                Transaction exitTransaction = new Transaction();
                                exitTransaction.setUserId(userId);
                                exitTransaction.setBusLineId(busLineId);
                                exitTransaction.setType(Constants.TRANSACTION_EXIT);
                                exitTransaction.setAmount(0);
                                exitTransaction.setCashback(cashbackAmount);
                                exitTransaction.setTimestamp(currentTime);

                                mFirestore.collection("transactions")
                                        .add(exitTransaction)
                                        .addOnSuccessListener(documentReference -> {
                                            // Actualizar saldo si hay cashback
                                            if (cashbackAmount > 0) {
                                                double currentBalance = preferenceManager.getUserBalance();
                                                double newBalance = currentBalance + cashbackAmount;

                                                // Actualizar en Firebase
                                                updateUserBalance(userId, newBalance);
                                                // Actualizar en SharedPreferences
                                                preferenceManager.updateBalance(newBalance);
                                            }

                                            timeoutHandler.removeCallbacks(timeoutRunnable);
                                            DialogUtils.hideLoadingDialog();
                                            // Usar el nombre del bus
                                            showSuccessDialog(false, cashbackAmount,
                                                    preferenceManager.getUserBalance(),
                                                    busLine.getName());
                                        })
                                        .addOnFailureListener(e -> {
                                            timeoutHandler.removeCallbacks(timeoutRunnable);
                                            DialogUtils.hideLoadingDialog();
                                            Toast.makeText(this, getString(R.string.error_saving_transaction),
                                                    Toast.LENGTH_SHORT).show();
                                            codeScanner.startPreview();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                timeoutHandler.removeCallbacks(timeoutRunnable);
                                DialogUtils.hideLoadingDialog();
                                Toast.makeText(this, getString(R.string.error_checking_entry),
                                        Toast.LENGTH_SHORT).show();
                                codeScanner.startPreview();
                            });
                })
                .addOnFailureListener(e -> {
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    DialogUtils.hideLoadingDialog();
                    Toast.makeText(this, getString(R.string.error_check_bus),
                            Toast.LENGTH_SHORT).show();
                    codeScanner.startPreview();
                });
    }

    private void showInsufficientBalanceDialog(double currentBalance, double requiredAmount, String busName) {
        View view = getLayoutInflater().inflate(R.layout.dialog_insufficient_balance, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        TextView tvMessage = view.findViewById(R.id.tvMessage);
        TextView tvCurrentBalance = view.findViewById(R.id.tvCurrentBalance);
        TextView tvRequiredAmount = view.findViewById(R.id.tvRequiredAmount);
        TextView tvMissingAmount = view.findViewById(R.id.tvMissingAmount);
        Button btnOk = view.findViewById(R.id.btnOk);

        tvMessage.setText(getString(R.string.insufficient_balance_message, busName));
        tvCurrentBalance.setText(getString(R.string.current_balance_format, currentBalance));
        tvRequiredAmount.setText(getString(R.string.required_amount_format, requiredAmount));
        tvMissingAmount.setText(getString(R.string.missing_amount_format, requiredAmount - currentBalance));

        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            codeScanner.startPreview();
        });

        dialog.show();
    }

    private void showNoValidEntryDialog(String busLineId) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.error_no_entry_title)
                .setMessage(getString(R.string.error_no_entry_message))
                .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
                    dialog.dismiss();
                    codeScanner.startPreview();
                })
                .setCancelable(false)
                .show();
    }

    private void updateUserBalance(String userId, double newBalance) {
        mFirestore.collection("users")
                .document(userId)
                .update("balance", newBalance)
                .addOnSuccessListener(aVoid -> {
                    // Actualizar SharedPreferences
                    preferenceManager.updateBalance(newBalance);
                    // Actualizar UI
                    updateBalanceUI();
                })
                .addOnFailureListener(e ->
                        Log.e("QrScannerActivity", "Error actualizando saldo", e));
    }

    private void showSuccessDialog(boolean isEntry, double amount, double newBalance, String companyName) {
        DialogUtils.hideLoadingDialog();
        View view = getLayoutInflater().inflate(R.layout.dialog_qr_success, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        TextView tvMessage = view.findViewById(R.id.tvMessage);
        TextView tvCompanyName = view.findViewById(R.id.tvCompanyName);
        TextView tvAmount = view.findViewById(R.id.tvAmount);
        TextView tvBalance = view.findViewById(R.id.tvBalance);
        Button btnOk = view.findViewById(R.id.btnOk);

        tvMessage.setText(isEntry ? R.string.success_entry_title : R.string.success_exit_title);
        tvCompanyName.setText(companyName);

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
            if (!isEntry) {
                finish();
            } else {
                // Si es una entrada, reiniciar el scanner
                codeScanner.startPreview();
            }
        });

        // Actualizar UI después de mostrar el diálogo
        updateBalanceUI();

        dialog.show();
    }


    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (codeScanner != null &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED) {
                codeScanner.startPreview();
                if (isFlashEnabled) {
                    codeScanner.setFlashEnabled(true);
                    updateFlashButton(true);
                }
            }
        } catch (Exception e) {
            Log.e("QrScannerActivity", "Error iniciando el preview", e);
        }
    }

    @Override
    protected void onPause() {
        if (codeScanner != null) {
            try {
                if (isFlashEnabled) {
                    codeScanner.setFlashEnabled(false);
                    isFlashEnabled = false;
                    updateFlashButton(false);
                }
                codeScanner.releaseResources();
            } catch (Exception e) {
                Log.e("QrScannerActivity", "Error liberando recursos", e);
            }
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        try {
            if (codeScanner != null) {
                codeScanner.releaseResources();
            }
        } catch (Exception e) {
            Log.e("QrScannerActivity", "Error destruyendo el escáner", e);
        }
        super.onDestroy();
    }

}