package com.example.chessgame.logic;

import com.example.chessgame.model.Move;
import com.example.chessgame.model.Piece;
import java.util.Stack;

public class GameManager {
    private final Board board;
    private final MoveValidator validator;
    private boolean whiteTurn = true;
    private final Stack<Move> history = new Stack<>();
    private boolean gameOver = false;
    private String winner = "";

    public GameManager() {
        board = new Board();
        validator = new MoveValidator(board);
    }

    public Board getBoard() { return board; }
    public boolean isWhiteTurn() { return whiteTurn; }
    public boolean isGameOver() { return gameOver; }
    public String getWinner() { return winner; }

    // ✅ Thực hiện nước đi; trả về true nếu thành công
    public boolean tryMove(int fr, int fc, int tr, int tc) {
        if (gameOver) return false;

        Piece moved = board.getPiece(fr, fc);
        if (moved == null) return false;

        Piece capturedBefore = board.getPiece(tr, tc);
        boolean prevHasMoved = moved.hasMoved();

        // Thực hiện nước đi (MoveValidator xử lý promotion + castling + en passant)
        if (!validator.makeMove(fr, fc, tr, tc, whiteTurn)) return false;

        // ✅ Lưu lịch sử để Undo
        history.push(new Move(fr, fc, tr, tc, moved, capturedBefore, prevHasMoved));

        // ✅ Kiểm tra vua bị ăn
        if (!hasKing(true)) {
            gameOver = true;
            winner = "Đen";
        } else if (!hasKing(false)) {
            gameOver = true;
            winner = "Trắng";
        }

        // ✅ Đổi lượt nếu chưa kết thúc
        if (!gameOver) whiteTurn = !whiteTurn;

        return true;
    }

    // ✅ Kiểm tra còn vua không
    private boolean hasKing(boolean whiteKing) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p != null && p.getType() == Piece.Type.KING && p.isWhite() == whiteKing)
                    return true;
            }
        }
        return false;
    }

    // ✅ Undo 1 nước
    public boolean undo() {
        if (history.isEmpty()) return false;
        Move last = history.pop();
        Piece moved = last.movedPiece;
        Piece captured = last.capturedPiece;

        board.placePiece(last.fromRow, last.fromCol, moved);
        board.placePiece(last.toRow, last.toCol, captured);
        if (moved != null) moved.setMoved(last.movedPieceHasMovedBefore);

        whiteTurn = !whiteTurn;
        gameOver = false;
        winner = "";
        return true;
    }

    public int getTotalMoves() {
        return history.size();
    }

    // ✅ Reset ván mới
    public void reset() {
        board.reset();
        history.clear();
        whiteTurn = true;
        gameOver = false;
        winner = "";
    }
}
