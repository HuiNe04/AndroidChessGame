package com.example.chessgame;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

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

        // Chế độ đấu máy
        btnAI.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, ChessActivity.class);
            i.putExtra("mode", "ai");
            startActivity(i);
        });

        // Xem lịch sử
        btnHistory.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(i);
        });
    }
}
