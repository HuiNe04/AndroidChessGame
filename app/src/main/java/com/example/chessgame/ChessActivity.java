package com.example.chessgame;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chessgame.db.DatabaseHelper;
import com.example.chessgame.logic.AIPlayer;
import com.example.chessgame.logic.MoveValidator;
import com.example.chessgame.ui.ChessBoardView;

public class ChessActivity extends AppCompatActivity {
    private ChessBoardView chessBoard;
    private DatabaseHelper db;
    private AIPlayer aiPlayer;
    private boolean aiEnabled = false;
    private TextView txtStatus;
    private Handler handler = new Handler();
    private MoveValidator validator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chess);

        chessBoard = findViewById(R.id.chessBoard);
        Button btnUndo = findViewById(R.id.btnUndo);
        Button btnReset = findViewById(R.id.btnReset);
        Button btnSave = findViewById(R.id.btnSave);
        txtStatus = findViewById(R.id.txtStatus);
        db = new DatabaseHelper(this);

        String mode = getIntent().getStringExtra("mode");
        aiEnabled = "ai".equals(mode);
        validator = new MoveValidator(chessBoard.getGameManager().getBoard());

        if (aiEnabled) {
            aiPlayer = new AIPlayer(chessBoard.getGameManager());
            Toast.makeText(this, "🤖 Chế độ đấu với máy", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "👥 Chế độ 2 người chơi", Toast.LENGTH_SHORT).show();
        }

        btnUndo.setOnClickListener(v -> {
            boolean ok = chessBoard.undoMove();
            if (!ok)
                Toast.makeText(this, "Không thể Undo", Toast.LENGTH_SHORT).show();
            updateStatus();
        });

        btnReset.setOnClickListener(v -> {
            chessBoard.resetGame();
            updateStatus();
        });

        btnSave.setOnClickListener(v -> {
            int moves = chessBoard.getGameManager().getTotalMoves();
            long id = db.insertGame(mode, "Unknown", moves);
            if (id > 0)
                Toast.makeText(this, "💾 Đã lưu vào lịch sử", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "Lưu thất bại", Toast.LENGTH_SHORT).show();
        });

        // Cập nhật trạng thái sau mỗi lần vẽ lại
        chessBoard.addOnLayoutChangeListener((v, l, t, r, b, oldl, oldt, oldr, oldb) -> updateStatus());

        // Nếu chế độ AI, để máy tự đi
        if (aiEnabled) {
            new Thread(() -> {
                while (true) {
                    try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                    runOnUiThread(() -> {
                        if (!chessBoard.getGameManager().isWhiteTurn()) {
                            aiPlayer.makeRandomMove(false);
                            chessBoard.invalidate();
                            updateStatus();
                        }
                    });
                }
            }).start();
        }
    }

    private void updateStatus() {
        boolean whiteTurn = chessBoard.getGameManager().isWhiteTurn();
        boolean isCheck = validator.isKingInCheck(whiteTurn);
        boolean isCheckmate = validator.isCheckmate(whiteTurn);

        if (isCheckmate) {
            String winner = whiteTurn ? "Đen" : "Trắng";
            showGameOverDialog("♛ Hết cờ! " + winner + " thắng!");
        } else if (isCheck) {
            txtStatus.setText("⚠️ Chiếu tướng! Lượt: " + (whiteTurn ? "Trắng" : "Đen"));
        } else {
            txtStatus.setText("Lượt: " + (whiteTurn ? "Trắng" : "Đen"));
        }
    }

    private void showGameOverDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Kết thúc ván cờ")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Chơi lại", (d, w) -> chessBoard.resetGame())
                .setNegativeButton("Thoát", (d, w) -> finish())
                .show();
    }
}
