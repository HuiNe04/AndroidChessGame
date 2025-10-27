package com.example.chessgame.logic;

import com.example.chessgame.model.Move;
import com.example.chessgame.model.Piece;
import java.util.Stack;

public class GameManager {
    private Board board;
    private MoveValidator validator;
    private boolean whiteTurn = true;
    private Stack<Move> history = new Stack<>();

    public GameManager() {
        board = new Board();
        validator = new MoveValidator(board);
    }

    public Board getBoard() { return board; }
    public boolean isWhiteTurn() { return whiteTurn; }

    // Thử thực hiện nước đi; trả về true nếu thành công
    public boolean tryMove(int fr, int fc, int tr, int tc) {
        if (!validator.isValidMove(fr, fc, tr, tc, whiteTurn)) return false;

        Piece moved = board.getPiece(fr, fc);
        Piece captured = board.getPiece(tr, tc);
        boolean prevHasMoved = moved.hasMoved();

        // Thực hiện tạm thời
        board.movePiece(fr, fc, tr, tc);

        // Kiểm tra không self-check (không được đưa vua mình vào trạng thái bị chiếu)
        if (isKingInCheck(!whiteTurn)) {
            // rollback
            board.placePiece(fr, fc, moved);
            board.placePiece(tr, tc, captured);
            moved.setMoved(prevHasMoved);
            return false;
        }

        // Lưu history để undo
        Move mv = new Move(fr, fc, tr, tc, moved, captured, prevHasMoved);
        history.push(mv);

        // đổi lượt
        whiteTurn = !whiteTurn;
        return true;
    }

    // Undo 1 nước
    public boolean undo() {
        if (history.isEmpty()) return false;
        Move last = history.pop();
        Piece moved = last.movedPiece;
        Piece captured = last.capturedPiece;

        board.placePiece(last.fromRow, last.fromCol, moved);
        board.placePiece(last.toRow, last.toCol, captured);
        if (moved != null) moved.setMoved(last.movedPieceHasMovedBefore);

        whiteTurn = !whiteTurn;
        return true;
    }

    // Kiểm tra vua của color (true=white) có bị chiếu không
    public boolean isKingInCheck(boolean whiteColor) {
        int kingR = -1, kingC = -1;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p != null && p.getType() == Piece.Type.KING && p.isWhite() == whiteColor) {
                    kingR = r; kingC = c;
                }
            }
        }
        if (kingR == -1) return true; // không thấy vua => coi như bị check

        // duyệt tất cả quân đối phương, nếu có nước tới vua => check
        MoveValidator mv = new MoveValidator(board);
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p != null && p.isWhite() != whiteColor) {
                    if (mv.isValidMove(r, c, kingR, kingC, !whiteColor)) return true;
                }
            }
        }
        return false;
    }

    public int getTotalMoves() {
        return history.size();
    }
}
