package com.example.securefolder.ui.modules;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.securefolder.R;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PhotosAdapter extends RecyclerView.Adapter<PhotosAdapter.ViewHolder> {

    private final List<File> files;
    private final OnPhotoActionListener listener;

    // Selection State
    private boolean isSelectionMode = false;
    private final Set<File> selectedFiles = new HashSet<>();

    public interface OnPhotoActionListener {
        void onPhotoClick(File file);
        void onSelectionModeChanged(boolean active, int count);
    }

    public PhotosAdapter(List<File> files, OnPhotoActionListener listener) {
        this.files = files;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = files.get(position);
        holder.tvName.setText("Secure Img " + (position + 1));

        // Handle Visual Selection
        if (selectedFiles.contains(file)) {
            holder.overlay.setVisibility(View.VISIBLE);
        } else {
            holder.overlay.setVisibility(View.GONE);
        }

        // Click Listener
        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(file);
            } else {
                listener.onPhotoClick(file);
            }
        });

        // Long Click Listener
        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                isSelectionMode = true;
                toggleSelection(file);
                return true;
            }
            return false;
        });
    }

    private void toggleSelection(File file) {
        if (selectedFiles.contains(file)) {
            selectedFiles.remove(file);
        } else {
            selectedFiles.add(file);
        }

        // Check if we should exit selection mode
        if (selectedFiles.isEmpty()) {
            isSelectionMode = false;
        }

        notifyDataSetChanged();
        listener.onSelectionModeChanged(isSelectionMode, selectedFiles.size());
    }

    public void clearSelection() {
        isSelectionMode = false;
        selectedFiles.clear();
        notifyDataSetChanged();
        listener.onSelectionModeChanged(false, 0);
    }

    public List<File> getSelectedFiles() {
        return new ArrayList<>(selectedFiles);
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        FrameLayout overlay;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvFileName);
            overlay = itemView.findViewById(R.id.overlaySelection);
        }
    }
}