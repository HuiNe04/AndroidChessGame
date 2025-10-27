package com.example.chessgame.logic;

import com.example.chessgame.model.Piece;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AIPlayer {
    private GameManager gm;
    private Random rnd = new Random();

    public AIPlayer(GameManager gm) {
        this.gm = gm;
    }

    // aiIsWhite = màu của AI
    public boolean makeRandomMove(boolean aiIsWhite) {
        Board board = gm.getBoard();
        List<int[]> moves = new ArrayList<>();
        MoveValidator mv = new MoveValidator(board);

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p != null && p.isWhite() == aiIsWhite) {
                    for (int tr = 0; tr < 8; tr++) {
                        for (int tc = 0; tc < 8; tc++) {
                            if (mv.isValidMove(r, c, tr, tc, aiIsWhite)) {
                                moves.add(new int[]{r, c, tr, tc});
                            }
                        }
                    }
                }
            }
        }
        if (moves.isEmpty()) return false;
        int[] sel = moves.get(rnd.nextInt(moves.size()));
        return gm.tryMove(sel[0], sel[1], sel[2], sel[3]);
    }
}
