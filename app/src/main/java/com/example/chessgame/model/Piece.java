package com.example.chessgame.model;

public class Piece {
    public enum Type { KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN }

    private Type type;
    private boolean isWhite;
    private int row, col;
    private boolean hasMoved; // dùng cho castling / pawn first move

    public Piece(Type type, boolean isWhite, int row, int col) {
        this.type = type;
        this.isWhite = isWhite;
        this.row = row;
        this.col = col;
        this.hasMoved = false;
    }

    public Type getType() { return type; }
    public boolean isWhite() { return isWhite; }
    public int getRow() { return row; }
    public int getCol() { return col; }
    public void setPosition(int row, int col) { this.row = row; this.col = col; }
    public boolean hasMoved() { return hasMoved; }
    public void setMoved(boolean moved) { this.hasMoved = moved; }

    // sao chép nhẹ (không clone đầy đủ) - nếu cần deep clone implement sau
    public Piece copy() {
        Piece p = new Piece(this.type, this.isWhite, this.row, this.col);
        p.setMoved(this.hasMoved);
        return p;
    }
}
