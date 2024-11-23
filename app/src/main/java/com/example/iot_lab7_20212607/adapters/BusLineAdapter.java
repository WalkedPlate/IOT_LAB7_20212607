package com.example.iot_lab7_20212607.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.iot_lab7_20212607.R;
import com.example.iot_lab7_20212607.databinding.ItemBusLineBinding;
import com.example.iot_lab7_20212607.models.BusLine;

import java.util.ArrayList;
import java.util.List;

public class BusLineAdapter extends RecyclerView.Adapter<BusLineAdapter.BusLineViewHolder> {

    private List<BusLine> busLines;
    private final OnBusLineClickListener listener;

    public interface OnBusLineClickListener {
        void onBusLineClick(BusLine busLine);
    }

    public BusLineAdapter(OnBusLineClickListener listener) {
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
        ItemBusLineBinding binding = ItemBusLineBinding.inflate(
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
        private final ItemBusLineBinding binding;

        public BusLineViewHolder(ItemBusLineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(BusLine busLine) {
            binding.tvBusLine.setText(busLine.getName());
            binding.tvPrice.setText(itemView.getContext().getString(
                    R.string.price_format, busLine.getUnitPrice()));

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

            binding.getRoot().setOnClickListener(v -> listener.onBusLineClick(busLine));
        }
    }
}