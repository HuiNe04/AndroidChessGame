package com.example.chessgame.logic;

import com.example.chessgame.model.Piece;
import com.example.chessgame.model.Piece.Type;

public class Board {
    private Piece[][] board = new Piece[8][8];

    public Board() {
        setupBoard();
    }

    // ✅ Khởi tạo lại bàn cờ chuẩn
    public void setupBoard() {
        // Xóa toàn bộ quân cũ
        board = new Piece[8][8];

        // --- Black pieces ---
        board[0][0] = new Piece(Type.ROOK, false, 0, 0);
        board[0][1] = new Piece(Type.KNIGHT, false, 0, 1);
        board[0][2] = new Piece(Type.BISHOP, false, 0, 2);
        board[0][3] = new Piece(Type.QUEEN, false, 0, 3);
        board[0][4] = new Piece(Type.KING, false, 0, 4);
        board[0][5] = new Piece(Type.BISHOP, false, 0, 5);
        board[0][6] = new Piece(Type.KNIGHT, false, 0, 6);
        board[0][7] = new Piece(Type.ROOK, false, 0, 7);

        // --- Black pawns ---
        for (int i = 0; i < 8; i++)
            board[1][i] = new Piece(Type.PAWN, false, 1, i);

        // --- White pawns ---
        for (int i = 0; i < 8; i++)
            board[6][i] = new Piece(Type.PAWN, true, 6, i);

        // --- White pieces ---
        board[7][0] = new Piece(Type.ROOK, true, 7, 0);
        board[7][1] = new Piece(Type.KNIGHT, true, 7, 1);
        board[7][2] = new Piece(Type.BISHOP, true, 7, 2);
        board[7][3] = new Piece(Type.QUEEN, true, 7, 3);
        board[7][4] = new Piece(Type.KING, true, 7, 4);
        board[7][5] = new Piece(Type.BISHOP, true, 7, 5);
        board[7][6] = new Piece(Type.KNIGHT, true, 7, 6);
        board[7][7] = new Piece(Type.ROOK, true, 7, 7);
    }

    public Piece getPiece(int r, int c) {
        if (r < 0 || r > 7 || c < 0 || c > 7) return null;
        return board[r][c];
    }

    // ✅ Thực hiện di chuyển (không kiểm tra hợp lệ ở đây)
    public Piece movePiece(int fromR, int fromC, int toR, int toC) {
        Piece p = getPiece(fromR, fromC);
        if (p == null) return null;

        Piece captured = getPiece(toR, toC);
        board[toR][toC] = p;
        board[fromR][fromC] = null;

        p.setPosition(toR, toC);
        p.setMoved(true);

        return captured;
    }

    // ✅ Đặt quân cờ (cho undo, castling, en passant, v.v.)
    public void placePiece(int r, int c, Piece p) {
        board[r][c] = p;
        if (p != null) p.setPosition(r, c);
    }

    // ✅ Trả về mảng bàn cờ
    public Piece[][] getBoardArray() {
        return board;
    }

    // ✅ Reset bàn cờ về trạng thái ban đầu
    public void reset() {
        setupBoard();
    }
}
