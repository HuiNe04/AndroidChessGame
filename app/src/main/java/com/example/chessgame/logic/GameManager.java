package com.example.chessgame.logic;

import com.example.chessgame.model.Piece;
import com.example.chessgame.model.Piece.Type;

/**
 * GameManager.java
 *
 * 💡 Nhiệm vụ:
 *  - Quản lý toàn bộ trạng thái ván cờ (board, lượt đi, lịch sử, thắng/thua/hòa)
 *  - Giao tiếp giữa lớp hiển thị (ChessBoardView / ChessActivity) và logic xử lý (MoveValidator)
 *  - Cung cấp hàm tryMove(), undo(), reset() để điều khiển gameplay
 */
public class GameManager {
    private final Board board;                 // Lưu trạng thái bàn cờ (ma trận Piece[8][8])
    private final MoveValidator validator;    // Dùng để kiểm tra nước đi hợp lệ
    private boolean whiteTurn = true;         // true = lượt Trắng, false = lượt Đen

    // Stack lưu toàn bộ lịch sử nước đi — hỗ trợ Undo chính xác
    private final java.util.Stack<HistoryEntry> history = new java.util.Stack<>();

    private boolean gameOver = false;         // true nếu ván đã kết thúc
    private String winner = "";               // "Trắng", "Đen", hoặc "Hòa"

    // ------------------------------
    // Constructor
    // ------------------------------
    public GameManager() {
        board = new Board();                  // Tạo bàn cờ mới (gọi setupBoard())
        validator = new MoveValidator(board); // Tạo MoveValidator gắn với board này
    }

    // ------------------------------
    // Getter cơ bản
    // ------------------------------
    public Board getBoard() { return board; }
    public boolean isWhiteTurn() { return whiteTurn; }
    public boolean isGameOver() { return gameOver; }
    public String getWinner() { return winner; }

    // ------------------------------
    // Lớp HistoryEntry: ghi nhớ toàn bộ thông tin của 1 nước đi
    // ------------------------------
    private static class HistoryEntry {
        public Board.MoveBackup mainBackup; // Backup của di chuyển chính (from→to)
        public boolean isEnPassant = false; // Có phải en-passant không?
        public Piece enPassantCapturedPiece = null;
        public int enPassantCapturedRow = -1, enPassantCapturedCol = -1;

        public boolean isCastling = false;  // Có phải nhập thành không?
        public Board.MoveBackup rookBackup = null; // Backup cho rook di chuyển

        public boolean isPromotion = false; // Có phong cấp không?
        public Piece promotedPawnOriginal = null;  // Lưu pawn gốc trước khi đổi thành queen

        // Lưu trạng thái game trước nước đi này (để undo chính xác)
        public boolean previousGameOver = false;
        public String previousWinner = "";
        public boolean previousWhiteTurn;
    }

    // ------------------------------
    // THỰC HIỆN 1 NƯỚC ĐI (tryMove)
    // ------------------------------
    public boolean tryMove(int fr, int fc, int tr, int tc) {
        if (gameOver) return false; // Ngăn không cho đi tiếp nếu ván đã kết thúc

        Piece moved = board.getPiece(fr, fc);
        if (moved == null) return false; // Không có quân nào ở vị trí xuất phát

        // ✅ Bước 1: Kiểm tra hợp lệ (không commit)
        if (!validator.isValidMove(fr, fc, tr, tc, whiteTurn)) return false;

        // ✅ Tạo 1 bản ghi lịch sử để lưu toàn bộ thông tin Undo
        HistoryEntry he = new HistoryEntry();
        he.previousGameOver = gameOver;
        he.previousWinner = winner;
        he.previousWhiteTurn = whiteTurn;

        // Lưu lại quân bị ăn trực tiếp (nếu có)
        Piece directCaptured = board.getPiece(tr, tc);

        // ---------------- En Passant ----------------
        boolean isEnPassant = false;
        Piece enPassantCaptured = null;
        int enPassantRow = -1, enPassantCol = -1;

        if (moved.getType() == Type.PAWN && Math.abs(tc - fc) == 1 && Math.abs(tr - fr) == 1 && directCaptured == null) {
            int[] eps = validator.getEnPassantSquare();
            if (eps != null && eps[0] == tr && eps[1] == tc) {
                isEnPassant = true;
                enPassantRow = moved.isWhite() ? tr + 1 : tr - 1;
                enPassantCol = tc;
                enPassantCaptured = board.getPiece(enPassantRow, enPassantCol);
                // Xóa quân bị ăn (pawn đối phương) ở ô en-passant
                board.placePiece(enPassantRow, enPassantCol, null);
            }
        }

        // ---------------- Thực hiện di chuyển chính ----------------
        Board.MoveBackup mainBackup = board.makeMove(fr, fc, tr, tc);
        he.mainBackup = mainBackup;

        // Nếu là en-passant → lưu dữ liệu vào HistoryEntry để Undo được
        if (isEnPassant) {
            he.isEnPassant = true;
            he.enPassantCapturedPiece = enPassantCaptured;
            he.enPassantCapturedRow = enPassantRow;
            he.enPassantCapturedCol = enPassantCol;
        }

        // ---------------- Promotion (phong cấp) ----------------
        if (mainBackup.movedPiece != null && mainBackup.movedPiece.getType() == Type.PAWN) {
            int toRow = mainBackup.toR;
            if ((mainBackup.movedPiece.isWhite() && toRow == 0) || (!mainBackup.movedPiece.isWhite() && toRow == 7)) {
                he.isPromotion = true;
                he.promotedPawnOriginal = mainBackup.movedPiece;
                // Tự động đổi thành Queen
                board.placePiece(toRow, mainBackup.toC,
                        new Piece(Type.QUEEN, mainBackup.movedPiece.isWhite(), toRow, mainBackup.toC));
            }
        }

        // ---------------- Castling (nhập thành) ----------------
        if (mainBackup.movedPiece != null && mainBackup.movedPiece.getType() == Type.KING &&
                Math.abs(mainBackup.toC - mainBackup.fromC) == 2) {

            boolean kingSide = mainBackup.toC > mainBackup.fromC;
            int rookFrom = kingSide ? 7 : 0;
            int rookTo = kingSide ? mainBackup.toC - 1 : mainBackup.toC + 1;

            // Di chuyển rook và lưu backup để Undo được
            Board.MoveBackup rookBackup = board.makeMove(mainBackup.fromR, rookFrom, mainBackup.fromR, rookTo);
            he.isCastling = true;
            he.rookBackup = rookBackup;
        }

        // ✅ Lưu HistoryEntry vào stack
        history.push(he);

        // ---------------- Kiểm tra trạng thái kết thúc ----------------
        if (!hasKing(true)) {
            gameOver = true;
            winner = "Đen thắng";
        } else if (!hasKing(false)) {
            gameOver = true;
            winner = "Trắng thắng";
        }
        // ✅ Thêm điều kiện hòa khi chỉ còn 2 vua
        else if (onlyKingsLeft()) {
            gameOver = true;
            winner = "Hòa (Chỉ còn hai vua)";
        }

        // ---------------- Chuyển lượt ----------------
        if (!gameOver) whiteTurn = !whiteTurn;

        return true;
    }

    // ------------------------------
    // UNDO 1 NƯỚC ĐI
    // ------------------------------
    public boolean undo() {
        if (history.isEmpty()) return false;
        HistoryEntry he = history.pop();

        // 1️⃣ Undo nhập thành (rook trước)
        if (he.isCastling && he.rookBackup != null) {
            board.undoMove(he.rookBackup);
        }

        // 2️⃣ Undo nước chính (trả lại quân bị ăn, di chuyển ngược)
        if (he.mainBackup != null) {
            board.undoMove(he.mainBackup);
        }

        // 3️⃣ Nếu là en-passant → phục hồi quân bị ăn (pawn đối phương)
        if (he.isEnPassant && he.enPassantCapturedPiece != null) {
            board.placePiece(he.enPassantCapturedRow, he.enPassantCapturedCol, he.enPassantCapturedPiece);
        }

        // 4️⃣ Nếu có promotion → đổi lại từ Queen về pawn gốc
        if (he.isPromotion && he.promotedPawnOriginal != null) {
            int toR = he.mainBackup.toR;
            int toC = he.mainBackup.toC;
            board.placePiece(toR, toC, he.promotedPawnOriginal);
            he.promotedPawnOriginal.setPosition(toR, toC);
        }

        // 5️⃣ Khôi phục trạng thái trước đó (lượt, kết thúc, người thắng)
        this.gameOver = he.previousGameOver;
        this.winner = he.previousWinner;
        this.whiteTurn = he.previousWhiteTurn;

        return true;
    }

    // ------------------------------
    // Kiểm tra tồn tại vua theo màu
    // ------------------------------
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

    // ------------------------------
    // Kiểm tra hòa khi chỉ còn hai vua
    // ------------------------------
    private boolean onlyKingsLeft() {
        int pieceCount = 0;
        int kingCount = 0;
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

    // ------------------------------
    // Lấy tổng số nước đã đi (phục vụ thống kê)
    // ------------------------------
    public int getTotalMoves() {
        return history.size();
    }

    // ------------------------------
    // Reset toàn bộ ván mới
    // ------------------------------
    public void reset() {
        board.reset();        // Đặt lại toàn bộ bàn cờ
        history.clear();      // Xóa lịch sử
        whiteTurn = true;     // Trắng đi trước
        gameOver = false;
        winner = "";
    }
}
