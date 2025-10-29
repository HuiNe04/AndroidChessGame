package com.example.chessgame;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;


import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnTwoPlayer = findViewById(R.id.btnTwoPlayer);
        Button btnAI = findViewById(R.id.btnAI);
        Button btnHistory = findViewById(R.id.btnHistory);

        // Chế độ 2 người chơi
        btnTwoPlayer.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, ChessActivity.class);
            i.putExtra("mode", "2player");
            startActivity(i);
        });

        // 🤖 Chế độ đấu với máy (AI)
        btnAI.setOnClickListener(v -> {
            // Tạo danh sách cấp độ hiển thị trong hộp thoại
            String[] levels = {"Dễ 😄", "Trung bình 🙂", "Khó 😤"};

            // Tạo hộp thoại chọn cấp độ
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Chọn cấp độ AI") // tiêu đề hộp thoại
                    .setItems(levels, (dialog, which) -> {
                        // which: 0 = Dễ, 1 = Trung bình, 2 = Khó
                        int aiLevel = which + 1; // quy đổi sang 1, 2, 3

                        // Tạo Intent để mở ChessActivity
                        Intent i = new Intent(MainActivity.this, ChessActivity.class);
                        i.putExtra("mode", "ai");       // chế độ chơi với máy
                        i.putExtra("AI_LEVEL", aiLevel); // truyền cấp độ AI
                        startActivity(i); // mở màn chơi cờ
                    })
                    .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss()) // nút Hủy
                    .show(); // hiển thị hộp thoại
        });

        // Chế độ đấu máy
        //btnAI.setOnClickListener(v -> {
            //Intent i = new Intent(MainActivity.this, ChessActivity.class);
            //i.putExtra("mode", "ai");
            //startActivity(i);
        //});

        // Xem lịch sử
        btnHistory.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(i);
        });
    }
}
