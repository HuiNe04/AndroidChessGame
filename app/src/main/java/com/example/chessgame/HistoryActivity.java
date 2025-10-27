package com.example.chessgame;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chessgame.db.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    private DatabaseHelper db;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        db = new DatabaseHelper(this);
        listView = findViewById(R.id.listHistory);

        List<DatabaseHelper.GameRecord> records = db.getAllGames();
        List<String> display = new ArrayList<>();

        for (DatabaseHelper.GameRecord g : records) {
            String mode = g.mode.equals("ai") ? "Đấu Máy" : "2 Người";
            String line = "Ván #" + g.id + " - " + mode +
                    "\nNgười thắng: " + g.winner +
                    "\nSố nước đi: " + g.totalMoves +
                    "\nNgày: " + g.datePlayed;
            display.add(line);
        }

        if (display.isEmpty()) {
            display.add("Chưa có lịch sử ván chơi nào.");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, display);
        listView.setAdapter(adapter);
    }
}
