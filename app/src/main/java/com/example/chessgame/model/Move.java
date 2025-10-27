package com.example.chessgame.model;

public class Move {
    public int fromRow, fromCol, toRow, toCol;
    public Piece movedPiece;      // tham chiếu tới Piece (lưu để undo)
    public Piece capturedPiece;   // tham chiếu (nếu có)
    public boolean movedPieceHasMovedBefore;

    public Move(int fr, int fc, int tr, int tc, Piece moved, Piece captured, boolean prevHasMoved) {
        this.fromRow = fr; this.fromCol = fc; this.toRow = tr; this.toCol = tc;
        this.movedPiece = moved;
        this.capturedPiece = captured;
        this.movedPieceHasMovedBefore = prevHasMoved;
    }
}
