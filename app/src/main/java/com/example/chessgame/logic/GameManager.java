package com.example.chessgame.logic;

import com.example.chessgame.model.Piece;
import com.example.chessgame.model.Piece.Type;

/**
 * ✅ GameManager.java (Final Fixed Version)
 *
 * Quản lý toàn bộ trạng thái ván cờ:
 *  - Lưu trữ bàn cờ, trạng thái lượt, lịch sử nước đi
 *  - Kiểm tra thắng / thua / hòa, bao gồm cả chiếu bí (checkmate) và bí hòa (stalemate)
 *  - Cung cấp API cho UI (ChessBoardView, ChessActivity)
 */
public class GameManager {
    // ----- Biến nội bộ -----
    private final Board board;                // Đối tượng Board lưu ma trận quân cờ
    private final MoveValidator validator;    // Kiểm tra hợp lệ nước đi
    private boolean whiteTurn = true;         // true = Trắng đi, false = Đen đi

    private final java.util.Stack<HistoryEntry> history = new java.util.Stack<>();
    private boolean gameOver = false;         // true nếu ván đã kết thúc
    private String winner = "";               // "Trắng" | "Đen" | "Hòa" | ""

    // ----- Constructor -----
    public GameManager() {
        board = new Board();
        validator = new MoveValidator(board);
    }

    // Getter để lớp khác dùng
    public Board getBoard() { return board; }
    public boolean isWhiteTurn() { return whiteTurn; }
    public boolean isGameOver() { return gameOver; }
    public String getWinner() { return winner; }
    public MoveValidator getValidator() { return validator; }

    // ===============================================================
    // ⏪ Cấu trúc lưu lại thông tin 1 nước đi để UNDO
    // ===============================================================
    private static class HistoryEntry {
        public Board.MoveBackup mainBackup;
        public boolean isEnPassant = false;
        public Piece enPassantCapturedPiece = null;
        public int enPassantCapturedRow = -1, enPassantCapturedCol = -1;

        public boolean isCastling = false;
        public Board.MoveBackup rookBackup = null;

        public boolean isPromotion = false;
        public Piece promotedPawnOriginal = null;

        public boolean previousGameOver = false;
        public String previousWinner = "";
        public boolean previousWhiteTurn = true;
    }

    // ===============================================================
    // ♟️ tryMove(): Thực hiện nước đi nếu hợp lệ
    // ===============================================================
    public boolean tryMove(int fr, int fc, int tr, int tc) {
        // 1️⃣ Nếu ván đã kết thúc -> không cho đi
        if (gameOver) return false;

        // 2️⃣ Lấy quân nguồn; nếu trống -> invalid
        Piece moved = board.getPiece(fr, fc);
        if (moved == null) return false;

        // 3️⃣ Kiểm tra hợp lệ nước đi (theo luật + an toàn vua)
        if (!validator.isValidMove(fr, fc, tr, tc, whiteTurn)) return false;

        // 4️⃣ Lưu snapshot để UNDO sau này
        HistoryEntry he = new HistoryEntry();
        he.previousGameOver = gameOver;
        he.previousWinner = winner;
        he.previousWhiteTurn = whiteTurn;

        // 5️⃣ Kiểm tra En Passant
        boolean isEnPassant = false;
        Piece enPassantCaptured = null;
        int enPassantRow = -1, enPassantCol = -1;
        Piece directCaptured = board.getPiece(tr, tc);

        if (moved.getType() == Type.PAWN && Math.abs(tc - fc) == 1 && Math.abs(tr - fr) == 1 && directCaptured == null) {
            int[] eps = validator.getEnPassantSquare();
            if (eps != null && eps[0] == tr && eps[1] == tc) {
                isEnPassant = true;
                enPassantRow = moved.isWhite() ? tr + 1 : tr - 1;
                enPassantCol = tc;
                enPassantCaptured = board.getPiece(enPassantRow, enPassantCol);
                board.placePiece(enPassantRow, enPassantCol, null); // xóa quân bị ăn tạm
            }
        }

        // 6️⃣ Thực hiện nước đi thật sự
        Board.MoveBackup mainBackup = board.makeMove(fr, fc, tr, tc);
        he.mainBackup = mainBackup;

        // Nếu là En Passant -> lưu lại
        if (isEnPassant) {
            he.isEnPassant = true;
            he.enPassantCapturedPiece = enPassantCaptured;
            he.enPassantCapturedRow = enPassantRow;
            he.enPassantCapturedCol = enPassantCol;
        }

        // 7️⃣ Kiểm tra Promotion (phong hậu)
        if (mainBackup.movedPiece != null && mainBackup.movedPiece.getType() == Type.PAWN) {
            int toRow = mainBackup.toR;
            if ((mainBackup.movedPiece.isWhite() && toRow == 0) || (!mainBackup.movedPiece.isWhite() && toRow == 7)) {
                he.isPromotion = true;
                he.promotedPawnOriginal = mainBackup.movedPiece;
                board.placePiece(toRow, mainBackup.toC,
                        new Piece(Type.QUEEN, mainBackup.movedPiece.isWhite(), toRow, mainBackup.toC));
            }
        }

        // 8️⃣ Nhập thành (Castling)
        if (mainBackup.movedPiece != null &&
                mainBackup.movedPiece.getType() == Type.KING &&
                Math.abs(mainBackup.toC - mainBackup.fromC) == 2) {

            boolean kingSide = mainBackup.toC > mainBackup.fromC;
            int rookFrom = kingSide ? 7 : 0;
            int rookTo = kingSide ? mainBackup.toC - 1 : mainBackup.toC + 1;
            Board.MoveBackup rookBackup = board.makeMove(mainBackup.fromR, rookFrom, mainBackup.fromR, rookTo);
            he.isCastling = true;
            he.rookBackup = rookBackup;
        }

        // 9️⃣ Lưu lại vào stack lịch sử
        history.push(he);

        // ===============================================================
        // ⚖️ Cập nhật trạng thái ván đấu (win / lose / draw / checkmate)
        // ===============================================================

        // ❗1. Nếu mất vua trắng => Đen thắng
        if (!hasKing(true)) {
            gameOver = true;
            winner = "Đen";
        }
        // ❗2. Nếu mất vua đen => Trắng thắng
        else if (!hasKing(false)) {
            gameOver = true;
            winner = "Trắng";
        }
        // ❗3. Nếu chỉ còn hai vua => Hòa
        else if (onlyKingsLeft()) {
            gameOver = true;
            winner = "Hòa";
        }
        // ❗4. Nếu chiếu hết (checkmate)
        else if (validator.isCheckmate(!whiteTurn)) {
            gameOver = true;
            winner = whiteTurn ? "Trắng" : "Đen"; // người vừa đi là người thắng
        }
        // ❗5. Nếu không bị chiếu nhưng không còn nước đi hợp lệ => hòa (stalemate)
        else if (!validator.isKingInCheck(!whiteTurn)) {
            boolean hasLegalMove = false;

            // Kiểm tra toàn bộ bàn xem bên kia còn nước hợp lệ không
            for (int r = 0; r < 8 && !hasLegalMove; r++) {
                for (int c = 0; c < 8 && !hasLegalMove; c++) {
                    Piece p = board.getPiece(r, c);
                    if (p != null && p.isWhite() == !whiteTurn) {
                        for (int tr2 = 0; tr2 < 8 && !hasLegalMove; tr2++) {
                            for (int tc2 = 0; tc2 < 8 && !hasLegalMove; tc2++) {
                                if (validator.isValidMove(r, c, tr2, tc2, !whiteTurn)) {
                                    hasLegalMove = true;
                                }
                            }
                        }
                    }
                }
            }

            if (!hasLegalMove) {
                gameOver = true;
                winner = "Hòa";
            }
        }

        // 🔁 10️⃣ Nếu game chưa kết thúc -> đổi lượt
        if (!gameOver) whiteTurn = !whiteTurn;

        return true; // ✅ Move hợp lệ, đã thực hiện xong
    }

    // ===============================================================
    // ⏪ Undo (hoàn tác nước đi)
    // ===============================================================
    public boolean undo() {
        if (history.isEmpty()) return false;
        HistoryEntry he = history.pop();

        if (he.isCastling && he.rookBackup != null)
            board.undoMove(he.rookBackup);

        if (he.mainBackup != null)
            board.undoMove(he.mainBackup);

        if (he.isEnPassant && he.enPassantCapturedPiece != null)
            board.placePiece(he.enPassantCapturedRow, he.enPassantCapturedCol, he.enPassantCapturedPiece);

        if (he.isPromotion && he.promotedPawnOriginal != null) {
            int toR = he.mainBackup.toR, toC = he.mainBackup.toC;
            board.placePiece(toR, toC, he.promotedPawnOriginal);
            he.promotedPawnOriginal.setPosition(toR, toC);
        }

        gameOver = he.previousGameOver;
        winner = he.previousWinner;
        whiteTurn = he.previousWhiteTurn;
        return true;
    }

    // ===============================================================
    // 🔍 Kiểm tra trạng thái bàn cờ
    // ===============================================================
    private boolean hasKing(boolean whiteKing) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p != null && p.getType() == Type.KING && p.isWhite() == whiteKing)
                    return true;
            }
        }
        return false;
    }

    private boolean onlyKingsLeft() {
        int pieceCount = 0, kingCount = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p != null) {
                    pieceCount++;
                    if (p.getType() == Type.KING) kingCount++;
                }
            }
        }
        return (pieceCount == 2 && kingCount == 2);
    }

    public int getTotalMoves() {
        return history.size();
    }

    // ===============================================================
    // 🔁 Reset bàn cờ về trạng thái ban đầu
    // ===============================================================
    public void reset() {
        board.reset();
        history.clear();
        whiteTurn = true;
        gameOver = false;
        winner = "";
    }
}
