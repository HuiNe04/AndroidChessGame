package com.example.chessgame.logic;

import com.example.chessgame.model.Piece;
import com.example.chessgame.model.Piece.Type;

/**
 * MoveValidator.java
 *
 * - Kiểm tra tính hợp lệ của nước đi (isValidMove)
 * - Thực hiện nước đi (makeMove) — commit (game thật)
 * - Cung cấp các helper: isKingInCheck, isCheckmate
 *
 * Lưu ý: isValidMoveInternal(...) có hai chế độ:
 *  - commit == false: chỉ kiểm tra hợp lệ, và KHÔNG thay đổi trạng thái (dùng mô phỏng để kiểm tra xem nước có khiến vua bị chiếu không)
 *  - commit == true: thực sự thực hiện nước đi trên board (dùng cho GameManager khi người chơi hoặc AI thực hiện nước)
 */
public class MoveValidator {
    private final Board board;

    /**
     * En passant square: lưu ô có thể bị ăn en-passant (row,col) hoặc null
     * Đây là trạng thái của bàn cờ, lưu tại MoveValidator để thuận tiện xử lý
     */
    private int[] enPassantSquare = null;

    public MoveValidator(Board board) {
        this.board = board;
    }

    public int[] getEnPassantSquare() {
        return enPassantSquare;
    }

    // ------------------------------
    // Public API
    // ------------------------------

    /**
     * Kiểm tra nước đi hợp lệ (không commit)
     */
    public boolean isValidMove(int fromR, int fromC, int toR, int toC, boolean isWhiteTurn) {
        return isValidMoveInternal(fromR, fromC, toR, toC, isWhiteTurn, false);
    }

    /**
     * Thực hiện nước đi thật (commit vào board)
     * Trả về true nếu thành công, false nếu không hợp lệ
     */
    public boolean makeMove(int fromR, int fromC, int toR, int toC, boolean isWhiteTurn) {
        return isValidMoveInternal(fromR, fromC, toR, toC, isWhiteTurn, true);
    }

    // ------------------------------
    // Core: kiểm tra/commit nước đi
    // ------------------------------
    private boolean isValidMoveInternal(int fromR, int fromC, int toR, int toC, boolean isWhiteTurn, boolean commit) {
        // Lấy quân ở ô nguồn
        Piece p = board.getPiece(fromR, fromC);
        if (p == null) return false;                                   // ô nguồn trống => invalid
        if (p.isWhite() != isWhiteTurn) return false;                   // không phải lượt của quân này
        if (fromR == toR && fromC == toC) return false;                 // không di chuyển
        if (toR < 0 || toR > 7 || toC < 0 || toC > 7) return false;     // ra ngoài bàn

        // Không ăn quân cùng màu
        Piece dest = board.getPiece(toR, toC);
        if (dest != null && dest.isWhite() == p.isWhite()) return false;

        // Kiểm tra quy tắc di chuyển của từng loại quân
        boolean valid;
        switch (p.getType()) {
            case PAWN:   valid = validPawn(p, fromR, fromC, toR, toC); break;
            case KNIGHT: valid = validKnight(fromR, fromC, toR, toC); break;
            case ROOK:   valid = validRook(fromR, fromC, toR, toC); break;
            case BISHOP: valid = validBishop(fromR, fromC, toR, toC); break;
            case QUEEN:  valid = validQueen(fromR, fromC, toR, toC); break;
            case KING:   valid = validKing(p, fromR, fromC, toR, toC); break;
            default:     valid = false;
        }

        if (!valid) return false;

        // --------------------------
        // Kiểm tra "king safety" — không cho nước đi khiến chính vua bị chiếu
        // --------------------------
        // Để kiểm tra điều này, ta mô phỏng nước đi (trên board) rồi kiểm tra isKingInCheck
        // Sử dụng Board.makeMove()/undoMove() khi có thể để mô phỏng an toàn.
        boolean makesKingInCheck;
        Board.MoveBackup backup = null; // dùng cho undo nếu ta gọi board.makeMove
        boolean simulatedEnPassant = false;
        Piece enPassantCapturedPiece = null;
        int enPassantCapturedRow = -1, enPassantCapturedCol = -1;

        // Nếu commit == false -> mô phỏng để kiểm tra tính an toàn, sau đó UNDO
        if (!commit) {
            // Xác định xem đây có phải là nước en passant không (đi ngang 1, lên/xuống 1, ô đích trống, enPassantSquare trùng)
            boolean isEnPassant = false;
            Piece destPiece = board.getPiece(toR, toC);
            if (p.getType() == Type.PAWN && Math.abs(toC - fromC) == 1 && Math.abs(toR - fromR) == 1 && destPiece == null) {
                if (enPassantSquare != null && enPassantSquare[0] == toR && enPassantSquare[1] == toC) {
                    isEnPassant = true;
                }
            }

            if (isEnPassant) {
                // Mô phỏng en passant: remove pawn bị ăn (ở hàng khác), di chuyển quân
                int captureRow = p.isWhite() ? toR + 1 : toR - 1;
                enPassantCapturedRow = captureRow;
                enPassantCapturedCol = toC;
                enPassantCapturedPiece = board.getPiece(captureRow, toC);

                // Thực hiện mô phỏng: đặt ô bắt (captureRow,toC) = null, dịch quân tới toR,toC, dọn from
                Piece moving = board.getPiece(fromR, fromC);
                boolean origMoved = moving.hasMoved();

                // apply simulation
                board.placePiece(captureRow, toC, null);
                board.placePiece(toR, toC, moving);
                board.placePiece(fromR, fromC, null);
                moving.setPosition(toR, toC);
                moving.setMoved(true);

                // Kiểm tra vua có bị chiếu không
                makesKingInCheck = isKingInCheck(p.isWhite());

                // Undo mô phỏng
                board.placePiece(fromR, fromC, moving);
                board.placePiece(toR, toC, null);
                board.placePiece(captureRow, toC, enPassantCapturedPiece);
                moving.setPosition(fromR, fromC);
                moving.setMoved(origMoved);

            } else {
                // Thông thường: dùng makeMove/undoMove để mô phỏng an toàn (Board.MoveBackup)
                backup = board.makeMove(fromR, fromC, toR, toC); // thực hiện tạm thời
                makesKingInCheck = isKingInCheck(p.isWhite());
                board.undoMove(backup); // hoàn tác mô phỏng
            }

            // Nếu mô phỏng cho thấy vua bị chiếu → invalid
            if (makesKingInCheck) return false;
            return true; // hợp lệ và an toàn
        }

        // --------------------------
        // Nếu commit == true: thực hiện nước đi thật trên board
        // --------------------------
        // Handle en passant when committing (special-case capture)
        if (p.getType() == Type.PAWN && Math.abs(toC - fromC) == 1 && Math.abs(toR - fromR) == 1 && dest == null) {
            // en passant capture: quân bị ăn nằm ở captureRow
            if (enPassantSquare != null && enPassantSquare[0] == toR && enPassantSquare[1] == toC) {
                int captureRow = p.isWhite() ? toR + 1 : toR - 1;
                // remove the captured pawn
                board.placePiece(captureRow, toC, null);
            }
        }

        // Cập nhật En Passant: nếu pawn đi 2 ô thì set enPassantSquare, ngược lại clear
        if (p.getType() == Type.PAWN && Math.abs(toR - fromR) == 2) {
            enPassantSquare = new int[]{ (fromR + toR) / 2, toC };
        } else {
            enPassantSquare = null;
        }

        // Thực hiện di chuyển chính thức (dùng movePiece có sẵn)
        board.movePiece(fromR, fromC, toR, toC);

        // Thực hiện phong cấp nếu cần (promotion to QUEEN)
        if (p.getType() == Type.PAWN) {
            if ((p.isWhite() && toR == 0) || (!p.isWhite() && toR == 7)) {
                board.placePiece(toR, toC, new Piece(Type.QUEEN, p.isWhite(), toR, toC));
            }
        }

        // Nhập thành (khi vua di chuyển 2 cột) — di chuyển luôn rook tương ứng
        if (p.getType() == Type.KING && Math.abs(toC - fromC) == 2) {
            boolean kingSide = toC > fromC;
            int rookFrom = kingSide ? 7 : 0;
            int rookTo = kingSide ? toC - 1 : toC + 1;
            board.movePiece(fromR, rookFrom, fromR, rookTo);
        }

        return true;
    }

    // ------------------------------
    // Piece-specific validators (giữ logic như trước, có mở rộng)
    // ------------------------------

    // Pawn move rules (bao gồm capture & en-passant possibility; not commit)
    private boolean validPawn(Piece p, int fr, int fc, int tr, int tc) {
        int dir = p.isWhite() ? -1 : 1;
        Piece dest = board.getPiece(tr, tc);

        // moving one step forward (empty)
        if (tc == fc && tr == fr + dir && dest == null) return true;

        // two steps from starting rank
        if (tc == fc && ((p.isWhite() && fr == 6) || (!p.isWhite() && fr == 1))
                && tr == fr + 2 * dir && dest == null && board.getPiece(fr + dir, fc) == null)
            return true;

        // captures (including en passant possibility)
        if (Math.abs(tc - fc) == 1 && tr == fr + dir) {
            // normal capture
            if (dest != null && dest.isWhite() != p.isWhite()) return true;
            // en-passant: dest empty but enPassantSquare set to this square
            if (enPassantSquare != null && enPassantSquare[0] == tr && enPassantSquare[1] == tc) return true;
        }

        return false;
    }

    // Knight rules
    private boolean validKnight(int fr, int fc, int tr, int tc) {
        int dr = Math.abs(tr - fr), dc = Math.abs(tc - fc);
        return (dr == 2 && dc == 1) || (dr == 1 && dc == 2);
    }

    // Rook rules (no jumping)
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

    // Bishop rules (diagonal)
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

    // Queen = rook + bishop
    private boolean validQueen(int fr, int fc, int tr, int tc) {
        return validRook(fr, fc, tr, tc) || validBishop(fr, fc, tr, tc);
    }

    // King rules: one-step or castling (basic checks)
    private boolean validKing(Piece p, int fr, int fc, int tr, int tc) {
        int dr = Math.abs(tr - fr), dc = Math.abs(tc - fc);

        // 1) Thông thường: Vua di chuyển 1 ô theo bất kỳ hướng nào
        if (dr <= 1 && dc <= 1) {
            // Không cho vua đi tới ô kề cạnh vua đối phương (hai vua không được sát nhau)
            for (int r = tr - 1; r <= tr + 1; r++) {
                for (int c = tc - 1; c <= tc + 1; c++) {
                    if (r < 0 || r > 7 || c < 0 || c > 7) continue;
                    Piece other = board.getPiece(r, c);
                    if (other != null && other.getType() == Type.KING && other.isWhite() != p.isWhite()) {
                        return false; // ô đích kề vua địch -> invalid
                    }
                }
            }
            return true;
        }

        // 2) Castling: vua chưa đi, di chuyển 2 ô ngang, rook chưa đi, đường trống
        if (!p.hasMoved() && dr == 0 && dc == 2) {
            boolean kingSide = tc > fc;
            int rookCol = kingSide ? 7 : 0;
            Piece rook = board.getPiece(fr, rookCol);
            if (rook != null && rook.getType() == Type.ROOK && !rook.hasMoved()) {
                int step = kingSide ? 1 : -1;
                // Kiểm tra ô giữa không có quân
                for (int c = fc + step; c != rookCol; c += step) {
                    if (board.getPiece(fr, c) != null) return false;
                }
                // **Kiểm tra ô vua đi qua không bị chiếu** (bao gồm ô bắt đầu, ô trung gian, ô đích)
                // ô hiện tại (fc), ô trung gian (fc + step), ô đích (fc + 2*step)
                if (isSquareAttacked(fr, fc, !p.isWhite())) return false; // bắt đầu không được đang bị chiếu
                if (isSquareAttacked(fr, fc + step, !p.isWhite())) return false; // không được đi qua ô bị chiếu
                if (isSquareAttacked(fr, fc + 2 * step, !p.isWhite())) return false; // ô đích không được bị chiếu
                return true;
            }
        }
        return false;
    }

    // ------------------------------
    // Check detection helpers
    // ------------------------------

    /**
     * Kiểm tra xem ô (r,c) có đang bị tấn công bởi màu byWhite hay không.
     * Dùng cho kiểm tra castling (không được đi qua ô bị chiếu) và kiểm tra tổng quát.
     */
    private boolean isSquareAttacked(int r, int c, boolean byWhite) {
        // out-of-range -> not attacked (defensive)
        if (r < 0 || r > 7 || c < 0 || c > 7) return false;

        // Duyệt tất cả quân của bên byWhite, kiểm tra xem có tấn công ô (r,c)
        for (int rr = 0; rr < 8; rr++) {
            for (int cc = 0; cc < 8; cc++) {
                Piece p = board.getPiece(rr, cc);
                if (p == null || p.isWhite() != byWhite) continue;

                Type t = p.getType();

                // Pawn tấn công chéo (lưu ý hướng)
                if (t == Type.PAWN) {
                    int dir = p.isWhite() ? -1 : 1;
                    int attackR = rr + dir;
                    if (attackR >= 0 && attackR < 8) {
                        if (cc - 1 >= 0 && attackR == r && cc - 1 == c) return true;
                        if (cc + 1 < 8 && attackR == r && cc + 1 == c) return true;
                    }
                    continue;
                }

                // Knight
                if (t == Type.KNIGHT) {
                    if (validKnight(rr, cc, r, c)) return true;
                    continue;
                }

                // Rook / Queen (rook-like)
                if (t == Type.ROOK || t == Type.QUEEN) {
                    if (validRook(rr, cc, r, c)) return true;
                }

                // Bishop / Queen (bishop-like)
                if (t == Type.BISHOP || t == Type.QUEEN) {
                    if (validBishop(rr, cc, r, c)) return true;
                }

                // King (adjacency)
                if (t == Type.KING) {
                    int dr = Math.abs(r - rr), dc = Math.abs(c - cc);
                    if (dr <= 1 && dc <= 1) return true;
                }
            }
        }
        return false;
    }

    /**
     * Kiểm tra xem vua của màu whiteColor có đang bị chiếu không.
     * Thuật toán:
     *  - Tìm vị trí vua
     *  - Duyệt tất cả quân đối phương, kiểm tra xem có quân nào có thể tấn công vị trí vua không
     *
     * Lưu ý: sử dụng các validator type-specific (validKnight, validRook, ...) để kiểm tra tấn công,
     * nhưng phải xử lý pawn capture riêng vì pawn di chuyển khác với cách tấn công.
     */
    public boolean isKingInCheck(boolean whiteColor) {
        // 1. Tìm vua
        int kingR = -1, kingC = -1;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p != null && p.getType() == Type.KING && p.isWhite() == whiteColor) {
                    kingR = r; kingC = c;
                    break;
                }
            }
            if (kingR != -1) break;
        }
        if (kingR == -1) {
            // Không tìm thấy vua (có thể đã bị ăn) -> coi là bị chiếu (hoặc game over)
            return true;
        }

        // 2. Dùng helper isSquareAttacked để kiểm tra nhanh
        return isSquareAttacked(kingR, kingC, !whiteColor);
    }

    /**
     * Kiểm tra xem bên whiteToMove có bị chiếu hết hay không.
     * Thuật toán:
     *  - Nếu vua không bị chiếu -> false (chưa phải checkmate)
     *  - Nếu vua bị chiếu -> thử mọi nước hợp lệ cho bên đó; nếu tồn tại nước nào khiến vua không còn bị chiếu => not checkmate
     *  - Nếu không có nước nào => checkmate
     */
    public boolean isCheckmate(boolean whiteToMove) {
        // Nếu vua chưa bị chiếu -> không phải checkmate
        if (!isKingInCheck(whiteToMove)) return false;

        // Thử mọi nước đi hợp lệ của bên này
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p == null || p.isWhite() != whiteToMove) continue;

                // Duyệt các ô đích
                for (int tr = 0; tr < 8; tr++) {
                    for (int tc = 0; tc < 8; tc++) {
                        // Nếu không hợp lệ theo quy tắc piece-specific -> skip early
                        if (!isValidMove(r, c, tr, tc, whiteToMove)) continue;

                        // Mô phỏng nước đi bằng Board.makeMove/undoMove (an toàn)
                        Board.MoveBackup backup = board.makeMove(r, c, tr, tc);
                        boolean kingStillInCheck = isKingInCheck(whiteToMove);
                        board.undoMove(backup);

                        if (!kingStillInCheck) {
                            // Tồn tại nước thoát chiếu -> không phải checkmate
                            return false;
                        }
                    }
                }
            }
        }

        // Không tìm được nước thoát => checkmate
        return true;
    }
}
