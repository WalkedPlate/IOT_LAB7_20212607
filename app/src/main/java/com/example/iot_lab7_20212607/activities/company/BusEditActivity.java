package com.example.iot_lab7_20212607.activities.company;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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
}