package com.example.securefolder.ui.modules;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.securefolder.R;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PasswordsAdapter extends RecyclerView.Adapter<PasswordsAdapter.ViewHolder> {

    private final List<PasswordsActivity.PassItem> data;
    private final OnPassActionListener listener;
    private boolean isSelectionMode = false;
    private final Set<Integer> selectedIds = new HashSet<>();

    public interface OnPassActionListener {
        void onPassClick(PasswordsActivity.PassItem item);
        void onCopyUser(String user);
        void onCopyPass(String pass);
        void onSelectionChanged(boolean active, int count);
    }

    public PasswordsAdapter(List<PasswordsActivity.PassItem> data, OnPassActionListener listener) {
        this.data = data;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_password, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PasswordsActivity.PassItem item = data.get(position);
        holder.tvAppName.setText(item.appName);
        holder.tvUsername.setText(item.username);

        // Icon Letter
        if (!item.appName.isEmpty()) {
            holder.tvLetter.setText(item.appName.substring(0, 1).toUpperCase());
        }

        // Selection Visuals
        if (selectedIds.contains(item.id)) {
            holder.overlay.setVisibility(View.VISIBLE);
        } else {
            holder.overlay.setVisibility(View.GONE);
        }

        // Click Logic
        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) toggleSelection(item.id);
            else listener.onPassClick(item);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                isSelectionMode = true;
                toggleSelection(item.id);
                return true;
            }
            return false;
        });

        // Action Buttons
        holder.btnCopyUser.setOnClickListener(v -> listener.onCopyUser(item.username));
        holder.btnCopyPass.setOnClickListener(v -> listener.onCopyPass(item.password));
    }

    private void toggleSelection(int id) {
        if (selectedIds.contains(id)) selectedIds.remove(id);
        else selectedIds.add(id);

        if (selectedIds.isEmpty()) isSelectionMode = false;

        notifyDataSetChanged();
        listener.onSelectionChanged(isSelectionMode, selectedIds.size());
    }

    public void clearSelection() {
        isSelectionMode = false;
        selectedIds.clear();
        notifyDataSetChanged();
        listener.onSelectionChanged(false, 0);
    }

    public List<Integer> getSelectedIds() {
        return new ArrayList<>(selectedIds);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAppName, tvUsername, tvLetter;
        ImageButton btnCopyUser, btnCopyPass;
        FrameLayout overlay;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAppName = itemView.findViewById(R.id.tvAppName);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvLetter = itemView.findViewById(R.id.tvIconLetter);
            btnCopyUser = itemView.findViewById(R.id.btnCopyUser);
            btnCopyPass = itemView.findViewById(R.id.btnCopyPass);
            overlay = itemView.findViewById(R.id.overlaySelection);
        }
    }
}