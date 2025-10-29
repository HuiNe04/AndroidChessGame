package com.example.chessgame.logic;

import com.example.chessgame.model.Piece;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Lớp AIPlayer chịu trách nhiệm điều khiển máy tính trong chế độ "Đấu với máy".
 * Hỗ trợ 3 cấp độ:
 *   1️⃣ Dễ      → đi ngẫu nhiên
 *   2️⃣ Trung bình → ưu tiên ăn quân
 *   3️⃣ Khó     → chọn nước ăn có lợi nhất
 */
public class AIPlayer {

    private GameManager gm;   // Quản lý toàn bộ ván cờ (bàn, lượt, luật,...)
    private Random rnd = new Random();  // Để chọn ngẫu nhiên khi cần
    private int aiLevel;      // Mức độ AI (1 = Dễ, 2 = TB, 3 = Khó)

    /**
     * Constructor nhận GameManager và cấp độ AI.
     * @param gm       Đối tượng GameManager của ván hiện tại
     * @param aiLevel  Cấp độ AI (1–3)
     */
    public AIPlayer(GameManager gm, int aiLevel) {
        this.gm = gm;
        this.aiLevel = aiLevel;
    }

    /**
     * 🔹 Chọn nước đi tốt nhất dựa vào cấp độ AI
     * @param aiIsWhite màu của AI (true = quân Trắng, false = quân Đen)
     * @return true nếu máy thực hiện được nước đi, false nếu không
     */
    public boolean makeBestMove(boolean aiIsWhite) {
        switch (aiLevel) {
            case 1:
                return makeRandomMove(aiIsWhite);   // dễ → random
            case 2:
                return makeGreedyMove(aiIsWhite);   // trung bình → ăn quân nếu có thể
            case 3:
                return makeSmartMove(aiIsWhite);    // khó → chọn nước ăn có lợi nhất
            default:
                return makeRandomMove(aiIsWhite);
        }
    }

    /**
     * 🔹 Cấp độ 1 (Dễ) — AI chọn một nước đi hợp lệ ngẫu nhiên
     */
    public boolean makeRandomMove(boolean aiIsWhite) {
        Board board = gm.getBoard();
        List<int[]> moves = new ArrayList<>();
        MoveValidator mv = new MoveValidator(board);

        // Duyệt toàn bộ bàn cờ để tìm nước hợp lệ
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p != null && p.isWhite() == aiIsWhite) {
                    // Tìm mọi ô mà quân này có thể di chuyển tới
                    for (int tr = 0; tr < 8; tr++) {
                        for (int tc = 0; tc < 8; tc++) {
                            if (mv.isValidMove(r, c, tr, tc, aiIsWhite)) {
                                moves.add(new int[]{r, c, tr, tc});
                            }
                        }
                    }
                }
            }
        }

        // Nếu không có nước hợp lệ → không đi được
        if (moves.isEmpty()) return false;

        // Chọn ngẫu nhiên một nước trong danh sách
        int[] sel = moves.get(rnd.nextInt(moves.size()));
        return gm.tryMove(sel[0], sel[1], sel[2], sel[3]);
    }

    /**
     * 🔹 Cấp độ 2 (Trung bình) — Ưu tiên ăn quân giá trị cao nhất
     */
    private boolean makeGreedyMove(boolean aiIsWhite) {
        Board board = gm.getBoard();
        MoveValidator mv = new MoveValidator(board);
        List<int[]> bestMoves = new ArrayList<>();
        int bestValue = -9999;

        // Duyệt toàn bộ bàn cờ
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p != null && p.isWhite() == aiIsWhite) {
                    // Duyệt các ô đích có thể di chuyển tới
                    for (int tr = 0; tr < 8; tr++) {
                        for (int tc = 0; tc < 8; tc++) {
                            if (mv.isValidMove(r, c, tr, tc, aiIsWhite)) {
                                Piece target = board.getPiece(tr, tc);
                                int value = 0;

                                // Nếu nước đi này ăn được quân đối thủ → cộng điểm theo giá trị quân bị ăn
                                if (target != null && target.isWhite() != aiIsWhite) {
                                    value = getPieceValue(target);
                                }

                                // Lưu nước ăn có giá trị cao nhất
                                if (value > bestValue) {
                                    bestValue = value;
                                    bestMoves.clear();
                                    bestMoves.add(new int[]{r, c, tr, tc});
                                } else if (value == bestValue) {
                                    // Nếu có nhiều nước tương đương → lưu lại để random sau
                                    bestMoves.add(new int[]{r, c, tr, tc});
                                }
                            }
                        }
                    }
                }
            }
        }

        // Nếu không có nước ăn → fallback sang random
        if (bestMoves.isEmpty()) {
            return makeRandomMove(aiIsWhite);
        }

        // Chọn ngẫu nhiên 1 nước trong các nước tốt nhất
        int[] sel = bestMoves.get(rnd.nextInt(bestMoves.size()));
        return gm.tryMove(sel[0], sel[1], sel[2], sel[3]);
    }

    /**
     * 🔹 Cấp độ 3 (Khó) — Tính điểm lợi/hại: ăn quân mạnh, tránh mất lợi thế
     * (phiên bản cơ bản, chưa phải minimax)
     */
    private boolean makeSmartMove(boolean aiIsWhite) {
        Board board = gm.getBoard();
        MoveValidator mv = new MoveValidator(board);

        int bestScore = Integer.MIN_VALUE; // điểm cao nhất tìm được
        int[] bestMove = null;             // nước đi tương ứng

        // Duyệt toàn bộ quân cờ của AI
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p != null && p.isWhite() == aiIsWhite) {

                    for (int tr = 0; tr < 8; tr++) {
                        for (int tc = 0; tc < 8; tc++) {
                            if (mv.isValidMove(r, c, tr, tc, aiIsWhite)) {

                                Piece captured = board.getPiece(tr, tc);
                                int score = 0;

                                // ✅ Nếu ăn được quân → cộng điểm bằng giá trị quân bị ăn
                                if (captured != null && captured.isWhite() != aiIsWhite) {
                                    score += getPieceValue(captured);
                                }

                                // ⚖️ Thêm logic đơn giản: khuyến khích di chuyển về trung tâm
                                int distFromCenter = Math.abs(tr - 3) + Math.abs(tc - 3);
                                score -= distFromCenter * 2; // càng xa trung tâm → điểm giảm

                                // ✅ Nếu điểm cao hơn → chọn nước này
                                if (score > bestScore) {
                                    bestScore = score;
                                    bestMove = new int[]{r, c, tr, tc};
                                }
                            }
                        }
                    }
                }
            }
        }

        // Nếu không có nước "tốt" → fallback sang random
        if (bestMove == null) {
            return makeRandomMove(aiIsWhite);
        }

        // Thực hiện nước đi được chọn
        return gm.tryMove(bestMove[0], bestMove[1], bestMove[2], bestMove[3]);
    }

    /**
     * 🔹 Trả về giá trị của từng loại quân (điểm cơ bản)
     * Dùng enum Piece.Type thay vì String
     */
    private int getPieceValue(Piece p) {
        switch (p.getType()) {
            case PAWN:
                return 100;
            case KNIGHT:
            case BISHOP:
                return 300;
            case ROOK:
                return 500;
            case QUEEN:
                return 900;
            case KING:
                return 10000;
            default:
                return 0;
        }
    }
}
