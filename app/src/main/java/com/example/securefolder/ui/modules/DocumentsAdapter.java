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
import com.example.securefolder.utils.DatabaseHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DocumentsAdapter extends RecyclerView.Adapter<DocumentsAdapter.ViewHolder> {

    private final List<File> files;
    private final DatabaseHelper dbHelper;
    private final OnDocumentActionListener listener;

    private boolean isSelectionMode = false;
    private final Set<File> selectedFiles = new HashSet<>();

    public interface OnDocumentActionListener {
        void onDocumentClick(File file);
        void onSelectionChanged(boolean active, int count);
    }

    public DocumentsAdapter(List<File> files, DatabaseHelper dbHelper, OnDocumentActionListener listener) {
        this.files = files;
        this.dbHelper = dbHelper;
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

        // Resolve Real Name from DB using the UUID filename
        String displayName = dbHelper.getDisplayName(file.getName());
        if (displayName == null) displayName = "Unknown Document";

        holder.tvName.setText(displayName);
        holder.icon.setImageResource(R.drawable.ic_document);

        // Selection Visuals
        if (selectedFiles.contains(file)) {
            holder.overlay.setVisibility(View.VISIBLE);
        } else {
            holder.overlay.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) toggleSelection(file);
            else listener.onDocumentClick(file);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                isSelectionMode = true;
                toggleSelection(file);
                listener.onSelectionChanged(true, selectedFiles.size());
                return true;
            }
            return false;
        });
    }

    private void toggleSelection(File file) {
        if (selectedFiles.contains(file)) selectedFiles.remove(file);
        else selectedFiles.add(file);

        if (selectedFiles.isEmpty()) isSelectionMode = false;

        notifyDataSetChanged();
        listener.onSelectionChanged(isSelectionMode, selectedFiles.size());
    }

    public void clearSelection() {
        isSelectionMode = false;
        selectedFiles.clear();
        notifyDataSetChanged();
        listener.onSelectionChanged(false, 0);
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
        ImageView icon;
        FrameLayout overlay;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvFileName);
            icon = itemView.findViewById(R.id.ivIcon);
            overlay = itemView.findViewById(R.id.overlaySelection);
        }
    }
}