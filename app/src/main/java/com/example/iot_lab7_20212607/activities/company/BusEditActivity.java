package com.example.iot_lab7_20212607.activities.company;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.iot_lab7_20212607.R;
import com.example.iot_lab7_20212607.adapters.BusImageAdapter;
import com.example.iot_lab7_20212607.databinding.ActivityBusEditBinding;
import com.example.iot_lab7_20212607.models.BusLine;
import com.example.iot_lab7_20212607.preferences.PreferenceManager;
import com.example.iot_lab7_20212607.utils.DialogUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BusEditActivity extends AppCompatActivity {

    private ActivityBusEditBinding binding;
    private FirebaseFirestore mFirestore;
    private FirebaseStorage mStorage;
    private PreferenceManager preferenceManager;
    private String busLineId;
    private BusLine currentBusLine;
    private BusImageAdapter imageAdapter;
    private List<Uri> newImages;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int STORAGE_PERMISSION_REQUEST = 2;
    private Bitmap qrBitmap;
    private static final int QR_SIZE = 256;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBusEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mFirestore = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance();
        preferenceManager = new PreferenceManager(this);
        newImages = new ArrayList<>();

        busLineId = getIntent().getStringExtra("busLineId");

        binding.toolbar.setTitle(busLineId != null ?
                R.string.title_edit_bus : R.string.title_add_bus);

        setupViews();

        if (busLineId != null) {
            loadBusLineData();
        } else {
            currentBusLine = new BusLine();
            currentBusLine.setCompanyId(preferenceManager.getUserId());
        }
    }

    private void setupViews() {
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Configurar RecyclerView para imágenes
        binding.rvImages.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new BusImageAdapter(
                imageUrl -> showDeleteImageConfirmation(imageUrl)
        );
        binding.rvImages.setAdapter(imageAdapter);

        // Configurar botón de agregar imagen
        binding.btnAddImage.setOnClickListener(v -> openImagePicker());

        // Configurar FAB de guardar
        binding.fabSave.setOnClickListener(v -> validateAndSave());

        // Configurar botones de QR
        binding.btnGenerateQr.setOnClickListener(v -> generateQR());
        binding.btnShareQr.setOnClickListener(v -> shareQR());

        // Si estamos en editar, generamos QR automáticamente
        if (busLineId != null) {
            binding.btnGenerateQr.setText(R.string.view_qr);
        }
    }

    private void generateQR() {
        try {
            String qrContent = "BUS_ID:" + (busLineId != null ? busLineId : "NEW");

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            binding.ivQrCode.setImageBitmap(bitmap);
            binding.ivQrCode.setVisibility(View.VISIBLE);
            binding.btnShareQr.setVisibility(View.VISIBLE);

        } catch (WriterException e) {
            Toast.makeText(this, getString(R.string.error_generating_qr),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void shareQR() {
        if (binding.ivQrCode.getDrawable() == null) return;

        try {
            // Crear un nuevo bitmap cada vez que compartimos xd
            Bitmap qrToShare = Bitmap.createBitmap(QR_SIZE, QR_SIZE, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(qrToShare);
            binding.ivQrCode.getDrawable().setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            binding.ivQrCode.getDrawable().draw(canvas);

            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File imageFile = new File(cachePath, "bus_qr.png");

            try (FileOutputStream stream = new FileOutputStream(imageFile)) {
                qrToShare.compress(Bitmap.CompressFormat.PNG, 80, stream);
                stream.flush();
            }

            // Liberar memoria
            qrToShare.recycle();

            Uri contentUri = FileProvider.getUriForFile(this,
                    "com.example.iot_lab7_20212607.fileprovider", imageFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_qr_title)));

        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.error_sharing_qr),
                    Toast.LENGTH_SHORT).show();
        }
    }


    private void loadBusLineData() {
        DialogUtils.showLoadingDialog(this, getString(R.string.loading_generic));

        mFirestore.collection("busLines")
                .document(busLineId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    DialogUtils.hideLoadingDialog();
                    currentBusLine = documentSnapshot.toObject(BusLine.class);
                    if (currentBusLine != null) {
                        currentBusLine.setId(documentSnapshot.getId());
                        updateUI();
                    }
                })
                .addOnFailureListener(e -> {
                    DialogUtils.hideLoadingDialog();
                    Toast.makeText(this, getString(R.string.error_generic),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void updateUI() {
        binding.etBusLine.setText(currentBusLine.getName());
        binding.etUnitPrice.setText(String.valueOf(currentBusLine.getUnitPrice()));
        binding.etSubscriptionPrice.setText(String.valueOf(currentBusLine.getSubscriptionPrice()));

        if (currentBusLine.getImageUrls() != null) {
            imageAdapter.setImages(currentBusLine.getImageUrls());
        }
    }

    private void openImagePicker() {
        // Verificar permisos primero
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                checkPermissions();
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                checkPermissions();
                return;
            }
        }

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,
                getString(R.string.select_images)), PICK_IMAGE_REQUEST);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            if (data.getClipData() != null) {
                // Múltiples imágenes seleccionadas
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    newImages.add(imageUri);
                }
            } else if (data.getData() != null) {
                // Una sola imagen seleccionada
                Uri imageUri = data.getData();
                newImages.add(imageUri);
            }
            updateImagePreview();
        }
    }

    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Verificar permisos según versión de Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    STORAGE_PERMISSION_REQUEST);
        }
    }


    private void updateImagePreview() {
        List<String> currentImages = currentBusLine.getImageUrls() != null ?
                new ArrayList<>(currentBusLine.getImageUrls()) : new ArrayList<>();

        // Agregar URIs temporales de nuevas imágenes
        for (Uri uri : newImages) {
            currentImages.add(uri.toString());
        }

        imageAdapter.setImages(currentImages);
    }

    private void validateAndSave() {
        String name = binding.etBusLine.getText().toString().trim();
        String unitPriceStr = binding.etUnitPrice.getText().toString().trim();
        String subscriptionPriceStr = binding.etSubscriptionPrice.getText().toString().trim();

        if (name.isEmpty()) {
            binding.tilBusLine.setError(getString(R.string.error_required_field));
            return;
        }
        if (unitPriceStr.isEmpty()) {
            binding.tilUnitPrice.setError(getString(R.string.error_required_field));
            return;
        }
        if (subscriptionPriceStr.isEmpty()) {
            binding.tilSubscriptionPrice.setError(getString(R.string.error_required_field));
            return;
        }

        double unitPrice, subscriptionPrice;
        try {
            unitPrice = Double.parseDouble(unitPriceStr);
            subscriptionPrice = Double.parseDouble(subscriptionPriceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, getString(R.string.error_invalid_price),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (unitPrice <= 0 || subscriptionPrice <= 0) {
            Toast.makeText(this, getString(R.string.error_invalid_price),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        saveBusLine(name, unitPrice, subscriptionPrice);
    }

    private void saveBusLine(String name, double unitPrice, double subscriptionPrice) {
        DialogUtils.showLoadingDialog(this, getString(R.string.saving));

        // Actualizar datos básicos
        currentBusLine.setName(name);
        currentBusLine.setUnitPrice(unitPrice);
        currentBusLine.setSubscriptionPrice(subscriptionPrice);

        // Si hay nuevas imágenes, subirlas primero
        if (!newImages.isEmpty()) {
            uploadImages(new ArrayList<>(newImages), new ArrayList<>(),
                    success -> {
                        if (success) {
                            saveToFirestore();
                        } else {
                            DialogUtils.hideLoadingDialog();
                            Toast.makeText(this, getString(R.string.error_upload_images),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            saveToFirestore();
        }
    }

    private void uploadImages(List<Uri> remainingImages, List<String> uploadedUrls,
                              OnImagesUploadedListener listener) {
        if (remainingImages.isEmpty()) {
            // Todas las imágenes se subieron
            if (currentBusLine.getImageUrls() == null) {
                currentBusLine.setImageUrls(new ArrayList<>());
            }
            currentBusLine.getImageUrls().addAll(uploadedUrls);
            listener.onComplete(true);
            return;
        }

        Uri imageUri = remainingImages.remove(0);
        String imageName = "bus_" + System.currentTimeMillis() + "_" + uploadedUrls.size();
        StorageReference imageRef = mStorage.getReference()
                .child("bus_images")
                .child(preferenceManager.getUserId())
                .child(imageName);

        imageRef.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return imageRef.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    uploadedUrls.add(uri.toString());
                    uploadImages(remainingImages, uploadedUrls, listener);
                })
                .addOnFailureListener(e -> listener.onComplete(false));
    }

    private void saveToFirestore() {
        if (busLineId == null) {
            // Nueva línea de bus
            mFirestore.collection("busLines")
                    .add(currentBusLine)
                    .addOnSuccessListener(documentReference -> {
                        DialogUtils.hideLoadingDialog();
                        Toast.makeText(this, getString(R.string.success_save),
                                Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        DialogUtils.hideLoadingDialog();
                        Toast.makeText(this, getString(R.string.error_save),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Actualizar línea existente
            mFirestore.collection("busLines")
                    .document(busLineId)
                    .set(currentBusLine)
                    .addOnSuccessListener(aVoid -> {
                        DialogUtils.hideLoadingDialog();
                        Toast.makeText(this, getString(R.string.success_save),
                                Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        DialogUtils.hideLoadingDialog();
                        Toast.makeText(this, getString(R.string.error_save),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void showDeleteImageConfirmation(String imageUrl) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_image)
                .setMessage(R.string.delete_image_confirmation)
                .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                    if (currentBusLine.getImageUrls() != null) {
                        currentBusLine.getImageUrls().remove(imageUrl);
                        imageAdapter.setImages(currentBusLine.getImageUrls());
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    interface OnImagesUploadedListener {
        void onComplete(boolean success);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, getString(R.string.error_storage_permission),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
        if (busLineId != null && binding.ivQrCode.getDrawable() == null) {
            generateQR();
        }
    }



}