package com.example.securefolder.ui.modules;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.securefolder.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder> {

    private final List<NotesActivity.NoteItem> notes;
    private final OnNoteActionListener listener;
    private boolean isSelectionMode = false;
    private final Set<Integer> selectedIds = new HashSet<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());

    public interface OnNoteActionListener {
        void onNoteClick(NotesActivity.NoteItem note);
        void onSelectionChanged(boolean active, int count);
    }

    public NotesAdapter(List<NotesActivity.NoteItem> notes, OnNoteActionListener listener) {
        this.notes = notes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotesActivity.NoteItem note = notes.get(position);
        holder.tvTitle.setText(note.title);
        holder.tvContent.setText(note.content);
        holder.tvDate.setText(sdf.format(new Date(note.timestamp)));

        // Selection
        if (selectedIds.contains(note.id)) {
            holder.overlay.setVisibility(View.VISIBLE);
        } else {
            holder.overlay.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) toggleSelection(note.id);
            else listener.onNoteClick(note);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                isSelectionMode = true;
                toggleSelection(note.id);
                return true;
            }
            return false;
        });
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
        return notes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvDate;
        FrameLayout overlay;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvDate = itemView.findViewById(R.id.tvDate);
            overlay = itemView.findViewById(R.id.overlaySelection);
        }
    }
}