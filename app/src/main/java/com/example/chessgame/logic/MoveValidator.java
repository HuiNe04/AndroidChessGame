package com.example.chessgame.logic;

import com.example.chessgame.model.Piece;
import com.example.chessgame.model.Piece.Type;

public class MoveValidator {
    private Board board;
    private int[] enPassantSquare = null; // ô có thể ăn en passant (row, col)

    public MoveValidator(Board board) {
        this.board = board;
    }

    public int[] getEnPassantSquare() {
        return enPassantSquare;
    }

    // ✅ Kiểm tra nước đi hợp lệ tổng quát (bao gồm castling, en passant, promotion)
    public boolean isValidMove(int fromR, int fromC, int toR, int toC, boolean isWhiteTurn) {
        Piece p = board.getPiece(fromR, fromC);
        if (p == null) return false;
        if (p.isWhite() != isWhiteTurn) return false;
        if (fromR == toR && fromC == toC) return false;
        if (toR < 0 || toR > 7 || toC < 0 || toC > 7) return false;

        Piece dest = board.getPiece(toR, toC);
        if (dest != null && dest.isWhite() == p.isWhite()) return false;

        boolean valid = false;

        switch (p.getType()) {
            case PAWN:
                valid = validPawn(p, fromR, fromC, toR, toC);
                break;
            case KNIGHT:
                valid = validKnight(fromR, fromC, toR, toC);
                break;
            case ROOK:
                valid = validRook(fromR, fromC, toR, toC);
                break;
            case BISHOP:
                valid = validBishop(fromR, fromC, toR, toC);
                break;
            case QUEEN:
                valid = validQueen(fromR, fromC, toR, toC);
                break;
            case KING:
                valid = validKing(p, fromR, fromC, toR, toC);
                break;
        }

        if (!valid) return false;

        // ✅ Phong cấp tốt (promotion)
        if (p.getType() == Type.PAWN) {
            if ((p.isWhite() && toR == 0) || (!p.isWhite() && toR == 7)) {
                // Mặc định phong thành Hậu (Queen)
                p = new Piece(Type.QUEEN, p.isWhite(), toR, toC);
                board.placePiece(toR, toC, p);
            }
        }

        // ✅ Xử lý En Passant (bắt tốt qua đường)
        if (p.getType() == Type.PAWN && Math.abs(toR - fromR) == 1 && Math.abs(toC - fromC) == 1 && dest == null) {
            // ô bị ăn en passant
            int captureRow = p.isWhite() ? toR + 1 : toR - 1;
            board.placePiece(captureRow, toC, null);
        }

        // ✅ Cập nhật ô có thể En Passant
        if (p.getType() == Type.PAWN && Math.abs(toR - fromR) == 2) {
            enPassantSquare = new int[]{(fromR + toR) / 2, toC};
        } else {
            enPassantSquare = null;
        }

        return true;
    }

    // ♙ PAWN
    private boolean validPawn(Piece p, int fr, int fc, int tr, int tc) {
        int dir = p.isWhite() ? -1 : 1;
        Piece dest = board.getPiece(tr, tc);

        // Di chuyển 1 ô
        if (tc == fc && tr == fr + dir && dest == null) return true;

        // Di chuyển 2 ô từ hàng xuất phát
        if (tc == fc && ((p.isWhite() && fr == 6) || (!p.isWhite() && fr == 1))
                && tr == fr + 2 * dir && dest == null && board.getPiece(fr + dir, fc) == null)
            return true;

        // Ăn chéo
        if (Math.abs(tc - fc) == 1 && tr == fr + dir) {
            if (dest != null && dest.isWhite() != p.isWhite()) return true;
            // En passant
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

    // ♚ KING (bao gồm castling)
    private boolean validKing(Piece p, int fr, int fc, int tr, int tc) {
        int dr = Math.abs(tr - fr), dc = Math.abs(tc - fc);
        if (dr <= 1 && dc <= 1) return true;

        // ✅ Castling (nhập thành)
        if (!p.hasMoved() && dr == 0 && (dc == 2)) {
            boolean kingSide = (tc > fc);
            int rookCol = kingSide ? 7 : 0;
            Piece rook = board.getPiece(fr, rookCol);

            if (rook != null && rook.getType() == Type.ROOK && !rook.hasMoved()) {
                int step = kingSide ? 1 : -1;
                for (int c = fc + step; c != rookCol; c += step) {
                    if (board.getPiece(fr, c) != null) return false;
                }

                // Nếu thoả mãn, di chuyển rook (thực hiện tại GameManager)
                return true;
            }
        }
        return false;
    }

    // ✅ Kiểm tra chiếu / hết cờ
    public boolean isCheckmate(boolean whiteToMove) {
        if (!isKingInCheck(whiteToMove)) return false;

        // duyệt tất cả các quân còn lại
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p != null && p.isWhite() == whiteToMove) {
                    for (int tr = 0; tr < 8; tr++) {
                        for (int tc = 0; tc < 8; tc++) {
                            if (isValidMove(r, c, tr, tc, whiteToMove)) {
                                // thử di chuyển ảo
                                Piece captured = board.getPiece(tr, tc);
                                board.movePiece(r, c, tr, tc);
                                boolean stillCheck = isKingInCheck(whiteToMove);
                                board.placePiece(r, c, p);
                                board.placePiece(tr, tc, captured);
                                if (!stillCheck) return false;
                            }
                        }
                    }
                }
            }
        }
        return true; // không có nước hợp lệ để thoát
    }

    // ✅ Kiểm tra vua có đang bị chiếu không
    public boolean isKingInCheck(boolean whiteColor) {
        int kingR = -1, kingC = -1;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p != null && p.getType() == Type.KING && p.isWhite() == whiteColor) {
                    kingR = r; kingC = c;
                }
            }
        }
        if (kingR == -1) return true; // mất vua => check

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p != null && p.isWhite() != whiteColor) {
                    if (isBasicAttack(p, r, c, kingR, kingC)) return true;
                }
            }
        }
        return false;
    }

    // kiểm tra nước tấn công cơ bản (không cần self-check)
    private boolean isBasicAttack(Piece p, int fr, int fc, int tr, int tc) {
        switch (p.getType()) {
            case PAWN:
                int dir = p.isWhite() ? -1 : 1;
                return Math.abs(fc - tc) == 1 && tr == fr + dir;
            case KNIGHT:
                int dr = Math.abs(fr - tr), dc = Math.abs(fc - tc);
                return (dr == 1 && dc == 2) || (dr == 2 && dc == 1);
            case BISHOP:
                return validBishop(fr, fc, tr, tc);
            case ROOK:
                return validRook(fr, fc, tr, tc);
            case QUEEN:
                return validQueen(fr, fc, tr, tc);
            case KING:
                return Math.abs(fr - tr) <= 1 && Math.abs(fc - tc) <= 1;
        }
        return false;
    }
}
