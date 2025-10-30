package com.example.chessgame;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chessgame.db.DatabaseHelper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * HistoryActivity.java
 * --------------------
 * Màn hình hiển thị danh sách các ván cờ đã lưu trong SQLite.
 */
public class HistoryActivity extends AppCompatActivity {
    private ListView lvHistory;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history); // layout có ListView id=lvHistory

        lvHistory = findViewById(R.id.lvHistory);
        db = new DatabaseHelper(this);

        // 🔹 Lấy toàn bộ lịch sử ván đấu
        var list = db.getAllGames();

        // 🔹 Định dạng dữ liệu hiển thị dạng text
        var items = new java.util.ArrayList<String>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        for (var g : list) {
            String date = "";
            try {
                date = sdf.format(new Date(Long.parseLong(g.datePlayed)));
            } catch (Exception ignored) {}
            String s = "🕹 " + g.mode + "\n🏆 " + g.winner +
                    "\n♟ Số nước: " + g.totalMoves +
                    "\n📅 Ngày: " + date;
            items.add(s);
        }

        // 🔹 Đưa vào ListView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        lvHistory.setAdapter(adapter);
    }
}
