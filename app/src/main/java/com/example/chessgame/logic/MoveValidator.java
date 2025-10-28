package com.example.chessgame.logic;

import com.example.chessgame.model.Piece;
import com.example.chessgame.model.Piece.Type;

public class MoveValidator {
    private final Board board;
    private int[] enPassantSquare = null;

    public MoveValidator(Board board) {
        this.board = board;
    }

    public int[] getEnPassantSquare() {
        return enPassantSquare;
    }

    // ✅ Kiểm tra nước đi hợp lệ (không thực hiện)
    public boolean isValidMove(int fromR, int fromC, int toR, int toC, boolean isWhiteTurn) {
        return isValidMoveInternal(fromR, fromC, toR, toC, isWhiteTurn, false);
    }

    // ✅ Thực hiện nước đi thật (GameManager dùng)
    public boolean makeMove(int fromR, int fromC, int toR, int toC, boolean isWhiteTurn) {
        return isValidMoveInternal(fromR, fromC, toR, toC, isWhiteTurn, true);
    }

    private boolean isValidMoveInternal(int fromR, int fromC, int toR, int toC, boolean isWhiteTurn, boolean commit) {
        Piece p = board.getPiece(fromR, fromC);
        if (p == null) return false;
        if (p.isWhite() != isWhiteTurn) return false;
        if (fromR == toR && fromC == toC) return false;
        if (toR < 0 || toR > 7 || toC < 0 || toC > 7) return false;

        Piece dest = board.getPiece(toR, toC);
        if (dest != null && dest.isWhite() == p.isWhite()) return false;

        boolean valid = false;
        switch (p.getType()) {
            case PAWN:   valid = validPawn(p, fromR, fromC, toR, toC); break;
            case KNIGHT: valid = validKnight(fromR, fromC, toR, toC); break;
            case ROOK:   valid = validRook(fromR, fromC, toR, toC); break;
            case BISHOP: valid = validBishop(fromR, fromC, toR, toC); break;
            case QUEEN:  valid = validQueen(fromR, fromC, toR, toC); break;
            case KING:   valid = validKing(p, fromR, fromC, toR, toC); break;
        }

        if (!valid) return false;

        // ⚠️ Không kiểm tra vua bị chiếu — tự do di chuyển

        if (!commit) return true;

        // En Passant
        if (p.getType() == Type.PAWN && Math.abs(toR - fromR) == 1 && Math.abs(toC - fromC) == 1 && dest == null) {
            int captureRow = p.isWhite() ? toR + 1 : toR - 1;
            board.placePiece(captureRow, toC, null);
        }

        // Cập nhật En Passant
        if (p.getType() == Type.PAWN && Math.abs(toR - fromR) == 2) {
            enPassantSquare = new int[]{(fromR + toR) / 2, toC};
        } else {
            enPassantSquare = null;
        }

        // Di chuyển quân
        board.movePiece(fromR, fromC, toR, toC);

        // Phong cấp
        if (p.getType() == Type.PAWN) {
            if ((p.isWhite() && toR == 0) || (!p.isWhite() && toR == 7)) {
                board.placePiece(toR, toC, new Piece(Type.QUEEN, p.isWhite(), toR, toC));
            }
        }

        // Nhập thành (di chuyển xe)
        if (p.getType() == Type.KING && Math.abs(toC - fromC) == 2) {
            boolean kingSide = toC > fromC;
            int rookFrom = kingSide ? 7 : 0;
            int rookTo = kingSide ? toC - 1 : toC + 1;
            board.movePiece(fromR, rookFrom, fromR, rookTo);
        }

        return true;
    }

    // ♙ PAWN
    private boolean validPawn(Piece p, int fr, int fc, int tr, int tc) {
        int dir = p.isWhite() ? -1 : 1;
        Piece dest = board.getPiece(tr, tc);

        if (tc == fc && tr == fr + dir && dest == null) return true;
        if (tc == fc && ((p.isWhite() && fr == 6) || (!p.isWhite() && fr == 1))
                && tr == fr + 2 * dir && dest == null && board.getPiece(fr + dir, fc) == null)
            return true;

        if (Math.abs(tc - fc) == 1 && tr == fr + dir) {
            if (dest != null && dest.isWhite() != p.isWhite()) return true;
            if (enPassantSquare != null && enPassantSquare[0] == tr && enPassantSquare[1] == tc) return true;
        }

        return false;
    }

    // ♞ KNIGHT
    private boolean validKnight(int fr, int fc, int tr, int tc) {
        int dr = Math.abs(tr - fr), dc = Math.abs(tc - fc);
        return (dr == 2 && dc == 1) || (dr == 1 && dc == 2);
    }

    // ♜ ROOK
    private boolean validRook(int fr, int fc, int tr, int tc) {
        if (fr != tr && fc != tc) return false;
        int dr = Integer.signum(tr - fr), dc = Integer.signum(tc - fc);
        int r = fr + dr, c = fc + dc;
        while (r != tr || c != tc) {
            if (board.getPiece(r, c) != null) return false;
            r += dr; c += dc;
        }
        return true;
    }

    // ♝ BISHOP
    private boolean validBishop(int fr, int fc, int tr, int tc) {
        if (Math.abs(tr - fr) != Math.abs(tc - fc)) return false;
        int dr = Integer.signum(tr - fr), dc = Integer.signum(tc - fc);
        int r = fr + dr, c = fc + dc;
        while (r != tr || c != tc) {
            if (board.getPiece(r, c) != null) return false;
            r += dr; c += dc;
        }
        return true;
    }

    // ♛ QUEEN
    private boolean validQueen(int fr, int fc, int tr, int tc) {
        return validRook(fr, fc, tr, tc) || validBishop(fr, fc, tr, tc);
    }

    // ♚ KING
    private boolean validKing(Piece p, int fr, int fc, int tr, int tc) {
        int dr = Math.abs(tr - fr), dc = Math.abs(tc - fc);
        if (dr <= 1 && dc <= 1) return true;

        // Nhập thành
        if (!p.hasMoved() && dr == 0 && dc == 2) {
            boolean kingSide = tc > fc;
            int rookCol = kingSide ? 7 : 0;
            Piece rook = board.getPiece(fr, rookCol);
            if (rook != null && rook.getType() == Type.ROOK && !rook.hasMoved()) {
                int step = kingSide ? 1 : -1;
                for (int c = fc + step; c != rookCol; c += step) {
                    if (board.getPiece(fr, c) != null) return false;
                }
                return true;
            }
        }
        return false;
    }


    public boolean isKingInCheck(boolean whiteColor) { return false; }
    public boolean isCheckmate(boolean whiteToMove) { return false; }
}
