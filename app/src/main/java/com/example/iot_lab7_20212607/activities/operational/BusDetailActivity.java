package com.example.iot_lab7_20212607.activities.operational;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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

import org.imaginativeworld.whynotimagecarousel.model.CarouselItem;

import java.util.ArrayList;
import java.util.List;

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
}