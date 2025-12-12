package com.example.securefolder.ui.modules;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.securefolder.R;
import com.example.securefolder.utils.DatabaseHelper;
import java.io.File;
import java.util.List;

public class DocumentsAdapter extends RecyclerView.Adapter<DocumentsAdapter.ViewHolder> {

    private final List<File> files;
    private final DatabaseHelper dbHelper;
    private final OnDocumentClickListener listener;

    public interface OnDocumentClickListener {
        void onDocumentClick(File file);
    }

    public DocumentsAdapter(List<File> files, DatabaseHelper dbHelper, OnDocumentClickListener listener) {
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

        // Try to get original name to show extension
        String origPath = dbHelper.getOriginalPath(file.getName());
        String displayName = "Document " + (position + 1);
        if (origPath != null) {
            File origFile = new File(origPath);
            displayName = origFile.getName();
        }

        holder.tvName.setText(displayName);
        holder.icon.setImageResource(R.drawable.ic_document);
        holder.itemView.setOnClickListener(v -> listener.onDocumentClick(file));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView icon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvFileName);
            icon = itemView.findViewById(R.id.ivIcon);
        }
    }
}