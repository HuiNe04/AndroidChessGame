package com.example.chessgame;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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
 * --------------------------------------------
 * Màn hình chính của trò chơi cờ vua.
 * - Hiển thị bàn cờ (ChessBoardView)
 * - Quản lý AIPlayer (nếu đấu máy)
 * - Cập nhật trạng thái / lưu lịch sử / restart ván
 *
 * 🧠 Lưu ý:
 *  - Sử dụng Handler + Runnable thay vì while(true) để tránh đơ / leak.
 *  - Khi ván kết thúc (checkmate / hòa) → AI dừng hoàn toàn.
 */
public class ChessActivity extends AppCompatActivity {

    private ChessBoardView chessBoard;        // View hiển thị bàn cờ và nhận tương tác
    private DatabaseHelper db;                // Quản lý SQLite để lưu lịch sử
    private AIPlayer aiPlayer;                // Trí tuệ nhân tạo (nếu đấu với máy)
    private boolean aiEnabled = false;        // Cờ bật chế độ đấu với máy
    private TextView txtStatus;               // TextView hiển thị lượt đi
    private int aiLevel = 1;                  // Mức độ AI (1–3)
    private final Handler handler = new Handler(); // Handler điều phối tác vụ trên UI thread
    private static final String TAG = "ChessActivity"; // Tag debug log

    /**
     * Runnable AI — được Handler gọi định kỳ.
     * Tự động kiểm tra:
     *   - Nếu tới lượt AI và game chưa kết thúc → AI đi nước.
     *   - Nếu chưa tới lượt → kiểm tra lại sau một khoảng ngắn.
     *   - Nếu ván đã kết thúc → dừng hẳn scheduling.
     */
    private final Runnable aiRunnable = new Runnable() {
        @Override
        public void run() {
            // Nếu AI được bật và chưa game over
            if (aiEnabled && !chessBoard.getGameManager().isGameOver()) {

                // 🔸 Kiểm tra có phải lượt của AI không (AI là quân Đen)
                if (!chessBoard.getGameManager().isWhiteTurn()) {
                    boolean moved = aiPlayer.makeBestMove(false); // false = quân Đen

                    if (!moved) {
                        Log.w(TAG, "⚠️ AI không tìm được nước đi (có thể bị chiếu bí hoặc hòa)");
                    } else {
                        // AI đi xong → cập nhật giao diện
                        chessBoard.invalidate();
                        updateStatus();
                    }

                    // Nếu game chưa kết thúc sau nước AI → lên lịch gọi lại (AI “suy nghĩ” 1.2s)
                    if (!chessBoard.getGameManager().isGameOver()) {
                        handler.postDelayed(this, 1200);
                    } else {
                        Log.d(TAG, "✅ Game kết thúc sau nước đi AI → dừng scheduling");
                    }
                } else {
                    // Nếu chưa tới lượt AI → chờ 0.5s rồi kiểm tra lại
                    handler.postDelayed(this, 500);
                }

            } else {
                // Nếu game đã kết thúc hoặc AI tắt → dừng hoàn toàn
                Log.d(TAG, "❌ Dừng AI thread: gameOver=" + chessBoard.getGameManager().isGameOver());
                handler.removeCallbacks(this);
            }
        }
    };

    // ===========================================================
    // 1️⃣ onCreate() — khởi tạo giao diện và logic ban đầu
    // ===========================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chess);

        // Ánh xạ view XML
        chessBoard = findViewById(R.id.chessBoard);
        txtStatus = findViewById(R.id.txtStatus);
        db = new DatabaseHelper(this);

        // Khi người chơi đi nước → cập nhật trạng thái lượt và kiểm tra thắng/thua
        chessBoard.setOnMoveListener(this::updateStatus);

        // ---------------- Toolbar menu ----------------
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            // ⏪ Hoàn tác
            if (id == R.id.mnuUndo) {
                boolean undone = chessBoard.undoMove();
                if (!undone)
                    Toast.makeText(this, "❌ Không thể hoàn tác!", Toast.LENGTH_SHORT).show();
                updateStatus();
                return true;
            }

            // 🔄 Restart ván
            else if (id == R.id.mnuRestart) {
                handler.removeCallbacks(aiRunnable); // dừng AI hiện tại nếu có
                chessBoard.resetGame();
                updateStatus();
                Toast.makeText(this, "🔁 Đã khởi động lại ván cờ", Toast.LENGTH_SHORT).show();

                // Nếu đấu máy → khởi động lại AI
                if (aiEnabled) handler.postDelayed(aiRunnable, 800);
                return true;
            }

            // 📖 Lịch sử ván đấu
            else if (id == R.id.mnuHistory) {
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            }

            return false;
        });

        // ---------------------------------------------------
        // Xác định chế độ (người vs người hoặc người vs máy)
        // ---------------------------------------------------
        String mode = getIntent().getStringExtra("mode");
        aiEnabled = "ai".equals(mode);

        if (aiEnabled) {
            // Lấy cấp độ AI
            aiLevel = getIntent().getIntExtra("AI_LEVEL", 1);
            aiPlayer = new AIPlayer(chessBoard.getGameManager(), aiLevel);
            Toast.makeText(this, "🤖 Đấu với máy (Cấp độ " + aiLevel + ")", Toast.LENGTH_SHORT).show();

            // Khởi chạy runnable sau 0.7s (AI kiểm tra điều kiện tự động)
            handler.postDelayed(aiRunnable, 700);
        } else {
            Toast.makeText(this, "👥 Chế độ 2 người chơi", Toast.LENGTH_SHORT).show();
        }

        // Khi layout thay đổi (xoay màn hình / resize) → cập nhật trạng thái hiển thị
        chessBoard.addOnLayoutChangeListener((v, l, t, r, b, oldl, oldt, oldr, oldb) -> updateStatus());

        // Cập nhật trạng thái lượt lần đầu
        updateStatus();
    }

    // ===========================================================
    // 2️⃣ updateStatus() — hiển thị lượt / kiểm tra kết thúc
    // ===========================================================
    private void updateStatus() {
        var gm = chessBoard.getGameManager();

        // Nếu ván kết thúc → hiện dialog và dừng AI
        if (gm.isGameOver()) {
            String winner = gm.getWinner();
            Log.i(TAG, "🏁 GameOver: Winner=" + winner + ", totalMoves=" + gm.getTotalMoves());
            showGameOverDialog("🏁 Ván kết thúc!\n" + winner);
            return;
        }

        // Nếu chưa kết thúc → hiển thị lượt hiện tại
        txtStatus.setText("Lượt: " + (gm.isWhiteTurn() ? "Trắng" : "Đen"));
    }

    // ===========================================================
    // 3️⃣ showGameOverDialog() — hiển thị dialog thắng/thua và lưu lịch sử
    // ===========================================================
    private void showGameOverDialog(String message) {
        // Dừng AI ngay lập tức (tránh loop thêm)
        handler.removeCallbacks(aiRunnable);

        var gm = chessBoard.getGameManager();
        String winner = gm.getWinner();
        int totalMoves = gm.getTotalMoves();
        String mode = aiEnabled ? "Đấu máy (Level " + aiLevel + ")" : "2 người";

        // 🔹 Lưu lịch sử ván đấu vào SQLite
        long insertedId = -1;
        try {
            insertedId = db.insertGame(mode, winner, totalMoves);
            Log.d(TAG, "✅ Lưu lịch sử ván đấu: id=" + insertedId);
        } catch (Exception ex) {
            Log.e(TAG, "❌ Lỗi khi lưu lịch sử", ex);
        }

        if (insertedId > 0)
            Toast.makeText(this, "📖 Đã lưu lịch sử ván đấu!", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, "⚠️ Không thể lưu lịch sử!", Toast.LENGTH_SHORT).show();

        // 🔸 Hiển thị dialog kết thúc
        new AlertDialog.Builder(this)
                .setTitle("Kết thúc ván cờ")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Chơi lại", (d, w) -> {
                    chessBoard.resetGame();
                    updateStatus();

                    // Nếu AI bật → bật lại runnable mới
                    if (aiEnabled) handler.postDelayed(aiRunnable, 700);
                })
                .setNegativeButton("Thoát", (d, w) -> finish())
                .show();
    }

    // ===========================================================
    // 4️⃣ onDestroy() — giải phóng tài nguyên, dừng Handler
    // ===========================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Xóa toàn bộ callback của Handler để dừng AI và tránh leak
        handler.removeCallbacksAndMessages(null);
        Log.d(TAG, "🧹 onDestroy() → Dừng tất cả AI callback");
    }
}
