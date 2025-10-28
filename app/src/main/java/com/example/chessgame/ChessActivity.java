package com.example.chessgame;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chessgame.db.DatabaseHelper;
import com.example.chessgame.logic.AIPlayer;
import com.example.chessgame.ui.ChessBoardView;
import com.google.android.material.appbar.MaterialToolbar;

public class ChessActivity extends AppCompatActivity {
    private ChessBoardView chessBoard;
    private DatabaseHelper db;
    private AIPlayer aiPlayer;
    private boolean aiEnabled = false;
    private TextView txtStatus;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chess);

        // 🔹 Ánh xạ View
        chessBoard = findViewById(R.id.chessBoard);
        txtStatus = findViewById(R.id.txtStatus);
        db = new DatabaseHelper(this);

        chessBoard.setOnMoveListener(this::updateStatus);

        // 🔹 Toolbar + menu Material3
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.mnuUndo) {
                boolean undone = chessBoard.undoMove();
                if (!undone)
                    Toast.makeText(this, "Không thể hoàn tác!", Toast.LENGTH_SHORT).show();
                updateStatus();
                return true;
            } else if (id == R.id.mnuRestart) {
                chessBoard.resetGame();
                updateStatus();
                Toast.makeText(this, "🔁 Đã khởi động lại ván cờ", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.mnuHistory) {
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            }
            return false;
        });

        // 🔹 Xác định chế độ chơi (AI / 2 người)
        String mode = getIntent().getStringExtra("mode");
        aiEnabled = "ai".equals(mode);

        if (aiEnabled) {
            aiPlayer = new AIPlayer(chessBoard.getGameManager());
            Toast.makeText(this, "🤖 Chế độ đấu với máy", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "👥 Chế độ 2 người chơi", Toast.LENGTH_SHORT).show();
        }

        // 🔹 Theo dõi trạng thái sau mỗi lần vẽ lại bàn cờ
        chessBoard.addOnLayoutChangeListener((v, l, t, r, b, oldl, oldt, oldr, oldb) -> updateStatus());

        // 🔹 Nếu AI bật → cho máy đi tự động
        if (aiEnabled) {
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(1200);
                    } catch (InterruptedException ignored) {}
                    runOnUiThread(() -> {
                        if (!chessBoard.getGameManager().isWhiteTurn()
                                && !chessBoard.getGameManager().isGameOver()) {
                            aiPlayer.makeRandomMove(false);
                            chessBoard.invalidate();
                            updateStatus();
                        }
                    });
                }
            }).start();
        }

        updateStatus();
    }

    // 🔹 Cập nhật trạng thái ván đấu
    private void updateStatus() {
        var gm = chessBoard.getGameManager();

        // ✅ Nếu ván đã kết thúc (vua bị ăn)
        if (gm.isGameOver()) {
            String winner = gm.getWinner();
            showGameOverDialog("🏁 Vua bị ăn!\n" + winner + " thắng trận!");
            return;
        }

        // ✅ Nếu chưa hết cờ, hiển thị lượt chơi
        boolean whiteTurn = gm.isWhiteTurn();
        txtStatus.setText("Lượt: " + (whiteTurn ? "Trắng" : "Đen"));
    }

    // 🔹 Hộp thoại kết thúc ván
    private void showGameOverDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Kết thúc ván cờ")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Chơi lại", (d, w) -> {
                    chessBoard.resetGame();
                    updateStatus();
                })
                .setNegativeButton("Thoát", (d, w) -> finish())
                .show();
    }
}
