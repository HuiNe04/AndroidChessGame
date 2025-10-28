package com.example.chessgame.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chessgame.R;
import com.example.chessgame.db.DatabaseHelper;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private final List<DatabaseHelper.GameRecord> games;

    public HistoryAdapter(List<DatabaseHelper.GameRecord> games) {
        this.games = games;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DatabaseHelper.GameRecord g = games.get(position);
        String mode = g.mode.equals("ai") ? "Đấu Máy" : "2 Người";
        holder.txtGameTitle.setText("♟️ Ván #" + g.id + " - " + mode);
        holder.txtWinner.setText("Người thắng: " + g.winner);
        holder.txtMoves.setText("Số nước đi: " + g.totalMoves);
        holder.txtDate.setText("Ngày: " + g.datePlayed);
    }

    @Override
    public int getItemCount() {
        return games.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtGameTitle, txtWinner, txtMoves, txtDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtGameTitle = itemView.findViewById(R.id.txtGameTitle);
            txtWinner = itemView.findViewById(R.id.txtWinner);
            txtMoves = itemView.findViewById(R.id.txtMoves);
            txtDate = itemView.findViewById(R.id.txtDate);
        }
    }
}
