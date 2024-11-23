package com.example.iot_lab7_20212607.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.iot_lab7_20212607.R;
import com.example.iot_lab7_20212607.databinding.ItemBusLineCompanyBinding;
import com.example.iot_lab7_20212607.models.BusLine;
import com.example.iot_lab7_20212607.models.Transaction;
import com.example.iot_lab7_20212607.utils.Constants;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CompanyBusLineAdapter extends RecyclerView.Adapter<CompanyBusLineAdapter.ViewHolder> {
    private List<BusLine> busLines = new ArrayList<>();
    private final OnBusLineClickListener listener;
    private final FirebaseFirestore mFirestore;

    public CompanyBusLineAdapter(OnBusLineClickListener listener) {
        this.listener = listener;
        this.mFirestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bus_line_company, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BusLine busLine = busLines.get(position);
        holder.bind(busLine);
    }

    @Override
    public int getItemCount() {
        return busLines.size();
    }

    public void setBusLines(List<BusLine> busLines) {
        this.busLines = busLines;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivBus;
        private final TextView tvBusLine;
        private final TextView tvMonthlyRevenue;
        private final TextView tvSubscriptions;
        private final ImageButton btnEdit;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBus = itemView.findViewById(R.id.ivBus);
            tvBusLine = itemView.findViewById(R.id.tvBusLine);
            tvMonthlyRevenue = itemView.findViewById(R.id.tvMonthlyRevenue);
            tvSubscriptions = itemView.findViewById(R.id.tvSubscriptions);
            btnEdit = itemView.findViewById(R.id.btnEdit);
        }

        void bind(BusLine busLine) {
            Context context = itemView.getContext();

            // Configurar nombre de la línea
            tvBusLine.setText(busLine.getName());

            // Mostrar valores iniciales mientras se cargan
            tvMonthlyRevenue.setText(context.getString(R.string.revenue_format, 0.0));
            tvSubscriptions.setText(context.getString(R.string.subscription_count_format, 0));

            // Cargar imagen
            if (busLine.getImageUrls() != null && !busLine.getImageUrls().isEmpty()) {
                Glide.with(context)
                        .load(busLine.getImageUrls().get(0))
                        .placeholder(R.drawable.placeholder_bus)
                        .error(R.drawable.placeholder_bus)
                        .into(ivBus);
            } else {
                ivBus.setImageResource(R.drawable.placeholder_bus);
            }

            // Configurar botón de edición
            btnEdit.setOnClickListener(v -> listener.onBusLineClick(busLine));

            // Cargar estadísticas
            loadBusLineStats(busLine);
        }

        private void loadBusLineStats(BusLine busLine) {
            Context context = itemView.getContext();
            long startOfMonth = getStartOfMonth();

            // Cargar ingresos del mes
            mFirestore.collection("transactions")
                    .whereEqualTo("busLineId", busLine.getId())
                    .whereGreaterThanOrEqualTo("timestamp", startOfMonth)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        Log.d("CompanyAdapter", "Found " + querySnapshot.size() +
                                " transactions for bus " + busLine.getId());

                        double revenue = 0;
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Transaction transaction = doc.toObject(Transaction.class);
                            if (transaction != null) {
                                String type = transaction.getType();
                                if (Constants.TRANSACTION_ENTRY.equals(type) ||
                                        Constants.TRANSACTION_SUBSCRIPTION.equals(type)) {
                                    revenue += transaction.getAmount();
                                    Log.d("CompanyAdapter", String.format(
                                            "Added transaction: type=%s, amount=%.2f",
                                            type, transaction.getAmount()));
                                }
                            }
                        }

                        Log.d("CompanyAdapter", "Total revenue for " + busLine.getId() +
                                ": " + revenue);
                        tvMonthlyRevenue.setText(context.getString(R.string.revenue_format, revenue));
                    })
                    .addOnFailureListener(e ->
                            Log.e("CompanyAdapter", "Error loading transactions for " +
                                    busLine.getId(), e));

            // Cargar suscripciones
            mFirestore.collection("users")
                    .whereArrayContains("subscriptions", busLine.getId())
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        int count = querySnapshot.size();
                        Log.d("CompanyAdapter", "Found " + count +
                                " subscriptions for bus " + busLine.getId());
                        tvSubscriptions.setText(context.getString(
                                R.string.subscription_count_format, count));
                    })
                    .addOnFailureListener(e ->
                            Log.e("CompanyAdapter", "Error loading subscriptions for " +
                                    busLine.getId(), e));
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
    }

    public interface OnBusLineClickListener {
        void onBusLineClick(BusLine busLine);
    }
}