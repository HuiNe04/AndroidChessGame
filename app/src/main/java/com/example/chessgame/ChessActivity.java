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

/**
 * ChessActivity.java
 * ------------------
 * Màn hình chính của ván cờ:
 * - Hiển thị bàn cờ
 * - Quản lý lượt chơi
 * - Gọi AIPlayer nếu chơi với máy
 * - Hiển thị trạng thái (Trắng / Đen)
 */
public class ChessActivity extends AppCompatActivity {

    // 🔹 Thành phần giao diện và logic
    private ChessBoardView chessBoard;   // View hiển thị bàn cờ
    private DatabaseHelper db;           // Lưu lịch sử ván cờ
    private AIPlayer aiPlayer;           // Đối tượng AI (máy)
    private boolean aiEnabled = false;   // Cờ bật chế độ AI
    private TextView txtStatus;          // Text hiển thị trạng thái lượt
    private int aiLevel = 1;             // Mức độ AI (1=Dễ, 2=TB, 3=Khó)
    private final Handler handler = new Handler(); // Dùng để delay máy suy nghĩ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chess);

        // 🔹 Ánh xạ View trong layout XML
        chessBoard = findViewById(R.id.chessBoard);
        txtStatus = findViewById(R.id.txtStatus);
        db = new DatabaseHelper(this);

        // Khi người chơi di chuyển → cập nhật trạng thái lượt
        chessBoard.setOnMoveListener(this::updateStatus);

        // 🔹 Toolbar và các chức năng trong menu (Undo, Restart, History)
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.mnuUndo) {
                // ⏪ Hoàn tác nước đi trước
                boolean undone = chessBoard.undoMove();
                if (!undone)
                    Toast.makeText(this, "Không thể hoàn tác!", Toast.LENGTH_SHORT).show();
                updateStatus();
                return true;

            } else if (id == R.id.mnuRestart) {
                // 🔁 Khởi động lại ván cờ mới
                chessBoard.resetGame();
                updateStatus();
                Toast.makeText(this, "🔁 Đã khởi động lại ván cờ", Toast.LENGTH_SHORT).show();
                return true;

            } else if (id == R.id.mnuHistory) {
                // 🕓 Mở lịch sử các ván đã chơi
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            }

            return false;
        });

        // 🔹 Xác định chế độ chơi (AI / 2 người)
        String mode = getIntent().getStringExtra("mode");
        aiEnabled = "ai".equals(mode);

        // Nếu chế độ AI → nhận thêm cấp độ AI từ Intent
        if (aiEnabled) {
            aiLevel = getIntent().getIntExtra("AI_LEVEL", 1); // mặc định là 1 (Dễ)
            aiPlayer = new AIPlayer(chessBoard.getGameManager(), aiLevel); // truyền cấp độ vào AI
            Toast.makeText(this, "🤖 Đấu với máy (Cấp độ " + aiLevel + ")", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "👥 Chế độ 2 người chơi", Toast.LENGTH_SHORT).show();
        }

        // 🔹 Cập nhật trạng thái mỗi khi bàn cờ vẽ lại (xoay màn hình, load lại)
        chessBoard.addOnLayoutChangeListener((v, l, t, r, b, oldl, oldt, oldr, oldb) -> updateStatus());

        // 🔹 Nếu bật AI → cho máy tự động đi khi đến lượt
        if (aiEnabled) {
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(1200); // máy "suy nghĩ" 1.2s cho tự nhiên
                    } catch (InterruptedException ignored) {}

                    runOnUiThread(() -> {
                        // Khi đến lượt máy (Đen), chưa kết thúc ván
                        if (!chessBoard.getGameManager().isWhiteTurn()
                                && !chessBoard.getGameManager().isGameOver()) {

                            // Gọi AIPlayer để chọn nước đi theo cấp độ
                            aiPlayer.makeBestMove(false);

                            // Vẽ lại bàn cờ sau khi máy đi
                            chessBoard.invalidate();
                            updateStatus();
                        }
                    });
                }
            }).start();
        }

        // Cập nhật trạng thái ban đầu (Trắng đi trước)
        updateStatus();
    }

    /**
     * 🔹 Cập nhật trạng thái hiển thị (lượt chơi hoặc kết thúc)
     */
    private void updateStatus() {
        var gm = chessBoard.getGameManager();

        // ✅ Nếu ván đã kết thúc (vua bị ăn)
        if (gm.isGameOver()) {
            String winner = gm.getWinner();
            showGameOverDialog("🏁 Vua bị ăn!\n" + winner + " thắng trận!");
            return;
        }

        // ✅ Nếu chưa hết cờ → hiển thị lượt hiện tại
        boolean whiteTurn = gm.isWhiteTurn();
        txtStatus.setText("Lượt: " + (whiteTurn ? "Trắng" : "Đen"));
    }

    /**
     * 🔹 Hiển thị hộp thoại khi ván cờ kết thúc
     */
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
