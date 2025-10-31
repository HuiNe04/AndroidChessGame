package com.example.chessgame.logic;

import com.example.chessgame.model.Piece;
import com.example.chessgame.model.Piece.Type;

/**
 * Board.java
 * -----------
 * Lưu trữ trạng thái bàn cờ (8x8 Piece[][]), cung cấp các API:
 *  - setupBoard(): khởi tạo vị trí chuẩn
 *  - getPiece(...), placePiece(...), movePiece(...) : thao tác cơ bản
 *  - makeMove(...) và undoMove(...) : cho AI mô phỏng nước đi rồi hoàn tác (efficient)
 *  - copy(): tạo bản sao của bàn cờ (deep copy của Piece) — tùy chọn dùng cho AI
 *
 * Move flow recommendation:
 *  - backup = board.makeMove(fromR,fromC,toR,toC);
 *  - ... (tính toán trên board)
 *  - board.undoMove(backup);
 */
public class Board {
    // Mảng 8x8 lưu các Piece (null = ô trống)
    private Piece[][] board = new Piece[8][8];

    public Board() {
        setupBoard();
    }

    // -------------------------
    // Khởi tạo bàn cờ tiêu chuẩn
    // -------------------------
    public void setupBoard() {
        // Tạo mảng mới (xóa mọi quân trước đó)
        board = new Piece[8][8];

        // --- Black major pieces (hàng 0) ---
        board[0][0] = new Piece(Type.ROOK, false, 0, 0);
        board[0][1] = new Piece(Type.KNIGHT, false, 0, 1);
        board[0][2] = new Piece(Type.BISHOP, false, 0, 2);
        board[0][3] = new Piece(Type.QUEEN, false, 0, 3);
        board[0][4] = new Piece(Type.KING, false, 0, 4);
        board[0][5] = new Piece(Type.BISHOP, false, 0, 5);
        board[0][6] = new Piece(Type.KNIGHT, false, 0, 6);
        board[0][7] = new Piece(Type.ROOK, false, 0, 7);

        // --- Black pawns (hàng 1) ---
        for (int i = 0; i < 8; i++) board[1][i] = new Piece(Type.PAWN, false, 1, i);

        // --- White pawns (hàng 6) ---
        for (int i = 0; i < 8; i++) board[6][i] = new Piece(Type.PAWN, true, 6, i);

        // --- White major pieces (hàng 7) ---
        board[7][0] = new Piece(Type.ROOK, true, 7, 0);
        board[7][1] = new Piece(Type.KNIGHT, true, 7, 1);
        board[7][2] = new Piece(Type.BISHOP, true, 7, 2);
        board[7][3] = new Piece(Type.QUEEN, true, 7, 3);
        board[7][4] = new Piece(Type.KING, true, 7, 4);
        board[7][5] = new Piece(Type.BISHOP, true, 7, 5);
        board[7][6] = new Piece(Type.KNIGHT, true, 7, 6);
        board[7][7] = new Piece(Type.ROOK, true, 7, 7);
    }

    // -------------------------
    // Truy xuất / thao tác cơ bản
    // -------------------------
    /**
     * Lấy Piece tại ô (r,c). Trả về null nếu ra ngoài hoặc ô trống.
     */
    public Piece getPiece(int r, int c) {
        if (r < 0 || r > 7 || c < 0 || c > 7) return null;
        return board[r][c];
    }

    /**
     * Thực hiện di chuyển cơ bản (không kiểm tra hợp lệ ở đây).
     * Trả về Piece bị ăn (nếu có), hoặc null nếu không có.
     *
     * Lưu ý: phương thức này giống makeMove nhưng trả Piece thay vì MoveBackup.
     * Bạn có thể dùng makeMove(...) nếu muốn undo sau đó.
     */
    public Piece movePiece(int fromR, int fromC, int toR, int toC) {
        Piece p = getPiece(fromR, fromC);
        if (p == null) return null;

        Piece captured = getPiece(toR, toC);   // lưu quân bị ăn (nếu có)
        board[toR][toC] = p;                   // đặt quân tại ô đích
        board[fromR][fromC] = null;            // dọn ô cũ

        // cập nhật vị trí trong Piece và cờ hasMoved
        p.setPosition(toR, toC);
        p.setMoved(true);

        return captured;
    }

    /**
     * Đặt quân cờ vào ô (r,c). Dùng cho undo, castling, en-passant, v.v.
     */
    public void placePiece(int r, int c, Piece p) {
        board[r][c] = p;
        if (p != null) p.setPosition(r, c);
    }

    /**
     * Trả về mảng bàn cờ (tham chiếu nội bộ).
     * Thận trọng: chỉnh sửa mảng trả về sẽ ảnh hưởng board gốc.
     */
    public Piece[][] getBoardArray() {
        return board;
    }

    /**
     * Reset bàn cờ về trạng thái ban đầu.
     */
    public void reset() {
        setupBoard();
    }

    // -------------------------
    // API hỗ trợ AI: make/undo & copy
    // -------------------------

    /**
     * MoveBackup lưu thông tin cần thiết để undo một nước đi.
     * - movedPiece: tham chiếu tới Piece được di chuyển
     * - fromR, fromC, toR, toC: tọa độ
     * - capturedPiece: Piece bị ăn (có thể null)
     * - originalHasMoved: trạng thái hasMoved ban đầu của movedPiece trước khi di chuyển
     *
     * Lưu ý: MoveBackup dùng để hoàn tác chính xác trạng thái bàn.
     */
    public static class MoveBackup {
        public Piece movedPiece;
        public int fromR, fromC, toR, toC;
        public Piece capturedPiece;
        public boolean originalHasMoved;

        public MoveBackup(Piece movedPiece, int fromR, int fromC, int toR, int toC,
                          Piece capturedPiece, boolean originalHasMoved) {
            this.movedPiece = movedPiece;
            this.fromR = fromR;
            this.fromC = fromC;
            this.toR = toR;
            this.toC = toC;
            this.capturedPiece = capturedPiece;
            this.originalHasMoved = originalHasMoved;
        }
    }

    /**
     * Thực hiện nước đi và trả về MoveBackup để có thể undo.
     * - Phục vụ AI: AI gọi makeMove(...), tính toán trên board, rồi gọi undoMove(backup).
     *
     * Lưu ý: phương thức này **không** kiểm tra hợp lệ (đảm bảo isValidMove trước khi gọi).
     */
    public MoveBackup makeMove(int fromR, int fromC, int toR, int toC) {
        Piece moving = getPiece(fromR, fromC);
        if (moving == null) return null;

        // Lưu quân bị ăn (nếu có) để restore sau này
        Piece captured = getPiece(toR, toC);

        // Lưu trạng thái hasMoved trước khi di chuyển (dùng cho undo)
        boolean originalHasMoved = moving.hasMoved();

        // Thực hiện di chuyển trong mảng
        board[toR][toC] = moving;
        board[fromR][fromC] = null;

        // Cập nhật vị trí và đánh dấu đã di chuyển
        moving.setPosition(toR, toC);
        moving.setMoved(true);

        // Trả về backup để undo
        return new MoveBackup(moving, fromR, fromC, toR, toC, captured, originalHasMoved);
    }

    /**
     * Hoàn tác một nước đi dựa trên MoveBackup
     * - Đặt movedPiece về ô từ (fromR, fromC)
     * - Khôi phục ô toR,toC bằng capturedPiece (hoặc null nếu không có)
     * - Phục hồi trạng thái hasMoved ban đầu của movedPiece
     */
    public void undoMove(MoveBackup backup) {
        if (backup == null || backup.movedPiece == null) return;

        // Đặt movedPiece trở về vị trí cũ
        board[backup.fromR][backup.fromC] = backup.movedPiece;

        // Khôi phục ô đích = quân bị ăn (có thể null)
        board[backup.toR][backup.toC] = backup.capturedPiece;

        // Cập nhật vị trí trong đối tượng Piece
        backup.movedPiece.setPosition(backup.fromR, backup.fromC);

        // Khôi phục flag hasMoved ban đầu (rất quan trọng cho castling / pawn)
        backup.movedPiece.setMoved(backup.originalHasMoved);
    }

    /**
     * Tạo bản sao độc lập của Board (deep copy về Piece).
     * - Mỗi Piece được copy() bằng phương thức copy() trong Piece.java
     * - Tùy chọn cho AI nếu bạn muốn mô phỏng trên bản copy thay vì make/undo.
     */
    public Board copy() {
        Board nb = new Board();
        // Tạo mảng sạch rồi sao chép từng ô
        nb.board = new Piece[8][8];
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = this.board[r][c];
                if (p != null) {
                    // Sử dụng copy() đã có trong Piece để tránh tham chiếu chung
                    nb.board[r][c] = p.copy();
                } else {
                    nb.board[r][c] = null;
                }
            }
        }
        return nb;
    }
}
