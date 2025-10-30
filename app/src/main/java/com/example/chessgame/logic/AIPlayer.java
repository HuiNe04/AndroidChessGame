package com.example.chessgame.logic;

import android.util.Log;

import com.example.chessgame.model.Piece;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * AIPlayer.java
 *
 * - AI dùng GameManager làm nguồn chân lý (board + validator).
 * - Không tạo MoveValidator mới (tránh trạng thái không đồng bộ).
 * - Bọc các cuộc gọi validator bằng try/catch để tránh ném exception làm đơ UI.
 * - Hỗ trợ 3 mức độ: random (1), greedy (2), smart (3).
 */
public class AIPlayer {

    private static final String TAG = "AIPlayer";

    private final GameManager gm;   // GameManager chứa board, validator, history...
    private final Random rnd = new Random();  // Dùng để chọn ngẫu nhiên
    private final int aiLevel;      // Mức độ AI (1=dễ,2=trung bình,3=khó)

    /**
     * Constructor nhận GameManager và cấp độ AI.
     * @param gm      game manager của ván hiện tại (AI sẽ dùng gm.getBoard() và gm.getValidator())
     * @param aiLevel 1..3
     */
    public AIPlayer(GameManager gm, int aiLevel) {
        this.gm = gm;
        this.aiLevel = aiLevel;
    }

    /**
     * makeBestMove: entry point cho AI.
     * @param aiIsWhite màu của AI (true nếu AI chơi Trắng)
     * @return true nếu AI thực hiện được một nước, false nếu không tìm được nước
     */
    public boolean makeBestMove(boolean aiIsWhite) {
        switch (aiLevel) {
            case 1: return makeRandomMove(aiIsWhite);   // dễ: random
            case 2: return makeGreedyMove(aiIsWhite);   // trung bình: ưu tiên ăn quân
            case 3: return makeSmartMove(aiIsWhite);    // khó: score đơn giản
            default: return makeRandomMove(aiIsWhite);
        }
    }

    // -------------------------
    // Level 1: Random move
    // -------------------------
    public boolean makeRandomMove(boolean aiIsWhite) {
        Board board = gm.getBoard();                    // lấy board từ GameManager
        MoveValidator mv = gm.getValidator();           // lấy validator CHUNG từ GameManager
        List<int[]> moves = new ArrayList<>();          // danh sách các nước hợp lệ

        // Duyệt mọi ô để tìm quân của AI
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p == null) continue;
                if (p.isWhite() != aiIsWhite) continue; // chỉ xử lý quân của AI

                // Duyệt mọi ô đích có thể tới
                for (int tr = 0; tr < 8; tr++) {
                    for (int tc = 0; tc < 8; tc++) {
                        try {
                            // Bọc gọi validator để an toàn: validator có thể mô phỏng/undo và ném exception hiếm
                            if (mv != null && mv.isValidMove(r, c, tr, tc, aiIsWhite)) {
                                moves.add(new int[]{r, c, tr, tc});
                            }
                        } catch (Exception ex) {
                            // Log lỗi nhưng bỏ qua nước đó (không để crash/đơ)
                            Log.e(TAG, "Validator exception in makeRandomMove for " + r + "," + c + " -> " + tr + "," + tc, ex);
                        }
                    }
                }
            }
        }

        if (moves.isEmpty()) {
            Log.d(TAG, "makeRandomMove: no valid moves found for AI (aiIsWhite=" + aiIsWhite + ")");
            return false;
        }

        // Chọn ngẫu nhiên 1 nước và thực hiện qua GameManager (gm.tryMove sẽ commit và lưu history)
        int[] sel = moves.get(rnd.nextInt(moves.size()));
        boolean res = gm.tryMove(sel[0], sel[1], sel[2], sel[3]);
        Log.d(TAG, "makeRandomMove: executed move " + sel[0] + "," + sel[1] + " -> " + sel[2] + "," + sel[3] + " result=" + res);
        return res;
    }

    // -------------------------
    // Level 2: Greedy (ưu tiên ăn)
    // -------------------------
    private boolean makeGreedyMove(boolean aiIsWhite) {
        Board board = gm.getBoard();
        MoveValidator mv = gm.getValidator();
        List<int[]> bestMoves = new ArrayList<>();
        int bestValue = Integer.MIN_VALUE; // lưu giá trị lớn nhất tìm được

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p == null || p.isWhite() != aiIsWhite) continue;

                for (int tr = 0; tr < 8; tr++) {
                    for (int tc = 0; tc < 8; tc++) {
                        try {
                            if (mv != null && mv.isValidMove(r, c, tr, tc, aiIsWhite)) {
                                Piece target = board.getPiece(tr, tc);
                                int value = 0;
                                if (target != null && target.isWhite() != aiIsWhite) {
                                    value = getPieceValue(target); // điểm theo loại quân bị ăn
                                }

                                if (value > bestValue) {
                                    bestValue = value;
                                    bestMoves.clear();
                                    bestMoves.add(new int[]{r, c, tr, tc});
                                } else if (value == bestValue) {
                                    bestMoves.add(new int[]{r, c, tr, tc});
                                }
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "Validator exception in makeGreedyMove for move " + r + "," + c + "->" + tr + "," + tc, ex);
                        }
                    }
                }
            }
        }

        if (bestMoves.isEmpty()) {
            Log.d(TAG, "makeGreedyMove: no capture moves, fallback to random");
            return makeRandomMove(aiIsWhite);
        }

        int[] sel = bestMoves.get(rnd.nextInt(bestMoves.size()));
        boolean res = gm.tryMove(sel[0], sel[1], sel[2], sel[3]);
        Log.d(TAG, "makeGreedyMove: executed move " + sel[0] + "," + sel[1] + " -> " + sel[2] + "," + sel[3] + " result=" + res + " bestValue=" + bestValue);
        return res;
    }

    // -------------------------
    // Level 3: Smart (simple scoring)
    // -------------------------
    private boolean makeSmartMove(boolean aiIsWhite) {
        Board board = gm.getBoard();
        MoveValidator mv = gm.getValidator();

        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = null;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p == null || p.isWhite() != aiIsWhite) continue;

                for (int tr = 0; tr < 8; tr++) {
                    for (int tc = 0; tc < 8; tc++) {
                        try {
                            if (mv != null && mv.isValidMove(r, c, tr, tc, aiIsWhite)) {
                                Piece captured = board.getPiece(tr, tc);
                                int score = 0;

                                // ăn quân được -> cộng điểm
                                if (captured != null && captured.isWhite() != aiIsWhite) {
                                    score += getPieceValue(captured) * 10; // nhân hệ số để ưu tiên ăn
                                }

                                // khuyến khích trung tâm: khoảng cách Manhattan tới ô (3,3)/(4,4)
                                int centerDist = Math.abs(tr - 3) + Math.abs(tc - 3);
                                score -= centerDist * 2;

                                // khuyến khích không bỏ vào ô bị ăn ngay (rất cơ bản)
                                // (tạm thời không mô phỏng sâu để tránh tốn thời gian)
                                if (score > bestScore) {
                                    bestScore = score;
                                    bestMove = new int[]{r, c, tr, tc};
                                }
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "Validator exception in makeSmartMove for move " + r + "," + c + "->" + tr + "," + tc, ex);
                        }
                    }
                }
            }
        }

        if (bestMove == null) {
            Log.d(TAG, "makeSmartMove: no scored move found, fallback to random");
            return makeRandomMove(aiIsWhite);
        }

        boolean res = gm.tryMove(bestMove[0], bestMove[1], bestMove[2], bestMove[3]);
        Log.d(TAG, "makeSmartMove: executed bestMove " + bestMove[0] + "," + bestMove[1] + " -> " + bestMove[2] + "," + bestMove[3] + " score=" + bestScore + " result=" + res);
        return res;
    }

    /**
     * Trả về điểm cơ bản cho từng loại quân.
     * Giá trị là số nguyên, được sử dụng để so sánh nước ăn.
     */
    private int getPieceValue(Piece p) {
        if (p == null) return 0;
        switch (p.getType()) {
            case PAWN:   return 100;
            case KNIGHT: return 300;
            case BISHOP: return 300;
            case ROOK:   return 500;
            case QUEEN:  return 900;
            case KING:   return 10000;
            default:     return 0;
        }
    }
}
