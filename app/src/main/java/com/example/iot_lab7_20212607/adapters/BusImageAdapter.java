package com.example.iot_lab7_20212607.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.iot_lab7_20212607.R;
import com.example.iot_lab7_20212607.databinding.ItemBusImageBinding;

import java.util.ArrayList;
import java.util.List;

public class BusImageAdapter extends RecyclerView.Adapter<BusImageAdapter.ImageViewHolder> {

    private List<String> images;
    private final OnImageClickListener listener;

    public interface OnImageClickListener {
        void onImageClick(String imageUrl);
    }

    public BusImageAdapter(OnImageClickListener listener) {
        this.images = new ArrayList<>();
        this.listener = listener;
    }

    public void setImages(List<String> images) {
        this.images = images;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBusImageBinding binding = ItemBusImageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ImageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        holder.bind(images.get(position));
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    class ImageViewHolder extends RecyclerView.ViewHolder {
        private final ItemBusImageBinding binding;

        public ImageViewHolder(ItemBusImageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(String imageUrl) {
            Glide.with(itemView.getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_bus)
                    .into(binding.ivBusImage);

            binding.btnDelete.setOnClickListener(v -> listener.onImageClick(imageUrl));
        }
    }
}