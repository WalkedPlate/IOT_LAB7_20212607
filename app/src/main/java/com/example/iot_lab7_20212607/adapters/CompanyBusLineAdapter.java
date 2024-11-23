package com.example.iot_lab7_20212607.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.iot_lab7_20212607.R;
import com.example.iot_lab7_20212607.databinding.ItemBusLineCompanyBinding;
import com.example.iot_lab7_20212607.models.BusLine;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class CompanyBusLineAdapter extends RecyclerView.Adapter<CompanyBusLineAdapter.BusLineViewHolder> {

    private List<BusLine> busLines;
    private final OnBusLineClickListener listener;

    public interface OnBusLineClickListener {
        void onBusLineClick(BusLine busLine);
    }

    public CompanyBusLineAdapter(OnBusLineClickListener listener) {
        this.busLines = new ArrayList<>();
        this.listener = listener;
    }

    public void setBusLines(List<BusLine> busLines) {
        this.busLines = busLines;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BusLineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBusLineCompanyBinding binding = ItemBusLineCompanyBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BusLineViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BusLineViewHolder holder, int position) {
        holder.bind(busLines.get(position));
    }

    @Override
    public int getItemCount() {
        return busLines.size();
    }

    class BusLineViewHolder extends RecyclerView.ViewHolder {
        private final ItemBusLineCompanyBinding binding;

        public BusLineViewHolder(ItemBusLineCompanyBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(BusLine busLine) {
            binding.tvBusLine.setText(busLine.getName());
            binding.tvMonthlyRevenue.setText(itemView.getContext()
                    .getString(R.string.monthly_revenue_line, busLine.getUnitPrice()));

            // Cargar la primera imagen si existe
            if (busLine.getImageUrls() != null && !busLine.getImageUrls().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(busLine.getImageUrls().get(0))
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_bus)
                        .into(binding.ivBus);
            } else {
                binding.ivBus.setImageResource(R.drawable.placeholder_bus);
            }

            // Cargar conteo de suscripciones
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereArrayContains("subscriptions", busLine.getId())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots ->
                            binding.tvSubscriptions.setText(itemView.getContext()
                                    .getString(R.string.subscriptions_count,
                                            queryDocumentSnapshots.size())));

            binding.btnEdit.setOnClickListener(v -> listener.onBusLineClick(busLine));
        }
    }
}
