package com.example.securefolder.ui.modules;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.securefolder.R;
import java.io.File;
import java.util.List;

public class VideosAdapter extends RecyclerView.Adapter<VideosAdapter.ViewHolder> {

    private final List<File> files;
    private final OnVideoClickListener listener;

    public interface OnVideoClickListener {
        void onVideoClick(File file);
    }

    public VideosAdapter(List<File> files, OnVideoClickListener listener) {
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
        holder.tvName.setText("Video " + (position + 1));
        holder.icon.setImageResource(R.drawable.ic_video); // Use the video icon we already have
        holder.itemView.setOnClickListener(v -> listener.onVideoClick(file));
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