package com.example.iot_lab7_20212607.activities.operational;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.iot_lab7_20212607.R;
import com.example.iot_lab7_20212607.databinding.ActivityBusDetailBinding;
import com.example.iot_lab7_20212607.models.BusLine;
import com.example.iot_lab7_20212607.models.Transaction;
import com.example.iot_lab7_20212607.models.User;
import com.example.iot_lab7_20212607.preferences.PreferenceManager;
import com.example.iot_lab7_20212607.utils.Constants;
import com.example.iot_lab7_20212607.utils.DialogUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.imaginativeworld.whynotimagecarousel.model.CarouselItem;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BusDetailActivity extends AppCompatActivity {
    private ActivityBusDetailBinding binding;
    private FirebaseFirestore mFirestore;
    private PreferenceManager preferenceManager;
    private String busLineId;
    private BusLine busLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBusDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mFirestore = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(this);
        busLineId = getIntent().getStringExtra("busLineId");

        if (busLineId == null) {
            Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupViews();
        loadBusLineData();
    }

    private void setupViews() {
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
        binding.btnSubscribe.setOnClickListener(v -> showSubscriptionConfirmation());

        generateQRCode();
    }

    private void loadBusLineData() {
        DialogUtils.showLoadingDialog(this, getString(R.string.loading_generic));

        mFirestore.collection("busLines")
                .document(busLineId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    DialogUtils.hideLoadingDialog();
                    busLine = documentSnapshot.toObject(BusLine.class);
                    if (busLine != null) {
                        busLine.setId(documentSnapshot.getId());
                        updateUI();
                    } else {
                        Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    DialogUtils.hideLoadingDialog();
                    Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void updateUI() {
        binding.tvUnitPrice.setText(getString(R.string.price_format, busLine.getUnitPrice()));
        binding.tvSubscriptionPrice.setText(getString(R.string.price_format, busLine.getSubscriptionPrice()));

        // Configurar el carrusel de imágenes
        if (busLine.getImageUrls() != null && !busLine.getImageUrls().isEmpty()) {
            List<CarouselItem> carouselItems = new ArrayList<>();
            for (String imageUrl : busLine.getImageUrls()) {
                carouselItems.add(new CarouselItem(imageUrl));
            }
            binding.carousel.setData(carouselItems);
        }

        // Verificar si ya está suscrito
        String userId = preferenceManager.getUserId();
        mFirestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null && user.getSubscriptions() != null
                            && user.getSubscriptions().contains(busLineId)) {
                        binding.btnSubscribe.setEnabled(false);
                        binding.btnSubscribe.setText(R.string.already_subscribed);
                    }
                });
    }

    private void showSubscriptionConfirmation() {
        View view = getLayoutInflater().inflate(R.layout.dialog_confirm_subscription, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .create();

        TextView tvPrice = view.findViewById(R.id.tvPrice);
        Button btnConfirm = view.findViewById(R.id.btnConfirm);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        tvPrice.setText(getString(R.string.price_format, busLine.getSubscriptionPrice()));

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            processSubscription();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void processSubscription() {
        DialogUtils.showLoadingDialog(this, getString(R.string.processing_subscription));

        String userId = preferenceManager.getUserId();
        double currentBalance = preferenceManager.getUserBalance();

        if (currentBalance < busLine.getSubscriptionPrice()) {
            DialogUtils.hideLoadingDialog();
            Toast.makeText(this, getString(R.string.insufficient_balance), Toast.LENGTH_SHORT).show();
            return;
        }

        // Actualizar saldo y agregar suscripción
        double newBalance = currentBalance - busLine.getSubscriptionPrice();

        DocumentReference userRef = mFirestore.collection("users").document(userId);

        mFirestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(userRef);
            User user = snapshot.toObject(User.class);
            if (user != null) {
                user.setBalance(newBalance);
                if (user.getSubscriptions() == null) {
                    user.setSubscriptions(new ArrayList<>());
                }
                user.getSubscriptions().add(busLineId);
                transaction.set(userRef, user);
            }
            return null;
        }).addOnSuccessListener(aVoid -> {
            DialogUtils.hideLoadingDialog();
            preferenceManager.updateBalance(newBalance);
            Toast.makeText(this, getString(R.string.subscription_success), Toast.LENGTH_SHORT).show();
            binding.btnSubscribe.setEnabled(false);
            binding.btnSubscribe.setText(R.string.already_subscribed);

            // Registrar la transacción
            Transaction transaction = new Transaction();
            transaction.setUserId(userId);
            transaction.setBusLineId(busLineId);
            transaction.setType(Constants.TRANSACTION_SUBSCRIPTION);
            transaction.setAmount(busLine.getSubscriptionPrice());
            transaction.setTimestamp(System.currentTimeMillis());

            mFirestore.collection("transactions")
                    .add(transaction);
        }).addOnFailureListener(e -> {
            DialogUtils.hideLoadingDialog();
            Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show();
        });
    }

    private void generateQRCode() {
        try {
            // Crear el contenido del QR (formato: BUS_ID:busLineId)
            String qrContent = "BUS_ID:" + busLineId;

            // Configurar los parámetros del QR
            int width = 512;
            int height = 512;
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 1);

            // Crear el escritor de QR
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, width, height, hints);

            // Convertir la matriz en un bitmap
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixels[y * width + x] = bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

            // Mostrar el QR en el ImageView
            binding.ivQrCode.setImageBitmap(bitmap);
            binding.cardQr.setVisibility(View.VISIBLE);

            // Opcional: Agregar funcionalidad para compartir el QR
            binding.btnShareQr.setOnClickListener(v -> shareQRCode(bitmap));

        } catch (Exception e) {
            Log.e("BusDetailActivity", "Error generando QR", e);
            Toast.makeText(this, getString(R.string.error_generating_qr), Toast.LENGTH_SHORT).show();
            binding.cardQr.setVisibility(View.GONE);
        }
    }

    private void shareQRCode(Bitmap qrBitmap) {
        try {
            // Guardar el bitmap temporalmente
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File imageFile = new File(cachePath, "shared_qr.png");

            FileOutputStream stream = new FileOutputStream(imageFile);
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            // Crear el URI del archivo
            Uri contentUri = FileProvider.getUriForFile(this,
                    "com.example.iot_lab7_20212607.fileprovider", imageFile);

            // Crear y lanzar el intent para compartir
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_qr_title)));

        } catch (Exception e) {
            Log.e("BusDetailActivity", "Error compartiendo QR", e);
            Toast.makeText(this, getString(R.string.error_sharing_qr), Toast.LENGTH_SHORT).show();
        }
    }
}