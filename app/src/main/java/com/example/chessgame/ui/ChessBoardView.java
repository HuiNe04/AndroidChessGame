package com.example.chessgame.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.chessgame.R;
import com.example.chessgame.logic.GameManager;
import com.example.chessgame.logic.MoveValidator;
import com.example.chessgame.model.Piece;

import java.util.ArrayList;
import java.util.List;

public class ChessBoardView extends View {
    private Paint paint = new Paint();
    private int cellSize;
    private GameManager gameManager;
    private MoveValidator validator;
    private int selectedR = -1, selectedC = -1;
    private List<int[]> validMoves = new ArrayList<>();

    private SoundPool soundPool;
    private int soundSelect, soundMove, soundCapture;
    private float captureScale = 1.0f;
    private boolean capturing = false;
    private int captureRow = -1, captureCol = -1;

    public ChessBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setAntiAlias(true);
        gameManager = new GameManager();
        validator = new MoveValidator(gameManager.getBoard());
        initSounds();
    }

    private void initSounds() {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(attrs)
                .build();

        soundSelect = soundPool.load(getContext(), R.raw.select, 1);
        soundMove = soundPool.load(getContext(), R.raw.move, 1);
        soundCapture = soundPool.load(getContext(), R.raw.capture, 1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        cellSize = width / 8;
        drawBoard(canvas);
        drawHighlights(canvas);
        drawPieces(canvas);
        if (selectedR != -1 && selectedC != -1) drawSelection(canvas, selectedR, selectedC);
        if (capturing) drawCaptureEffect(canvas);
    }

    private void drawBoard(Canvas canvas) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                boolean light = (r + c) % 2 == 0;
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(light ? Color.parseColor("#EEE8D5") : Color.parseColor("#6B4F3A"));
                canvas.drawRect(c * cellSize, r * cellSize, (c + 1) * cellSize, (r + 1) * cellSize, paint);
            }
        }
    }

    private void drawHighlights(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(80, 0, 200, 0));
        for (int[] move : validMoves) {
            int r = move[0], c = move[1];
            canvas.drawRect(c * cellSize, r * cellSize, (c + 1) * cellSize, (r + 1) * cellSize, paint);
        }
    }

    private void drawPieces(Canvas canvas) {
        paint.setTextSize(cellSize * 0.6f);
        paint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fm = paint.getFontMetrics();
        float textOffset = (fm.descent + fm.ascent) / 2;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = gameManager.getBoard().getPiece(r, c);
                if (p != null) {
                    paint.setColor(p.isWhite() ? Color.WHITE : Color.BLACK);
                    String s = getSymbol(p.getType());
                    float x = c * cellSize + cellSize / 2f;
                    float y = r * cellSize + cellSize / 2f - textOffset;
                    canvas.drawText(s, x, y, paint);
                }
            }
        }
    }

    private void drawSelection(Canvas canvas, int r, int c) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.YELLOW);
        paint.setStrokeWidth(6);
        canvas.drawRect(c * cellSize, r * cellSize, (c + 1) * cellSize, (r + 1) * cellSize, paint);
    }

    private void drawCaptureEffect(Canvas canvas) {
        if (captureRow == -1 || captureCol == -1) return;
        float cx = captureCol * cellSize + cellSize / 2f;
        float cy = captureRow * cellSize + cellSize / 2f;
        paint.setColor(Color.argb(150, 255, 0, 0));
        canvas.drawCircle(cx, cy, cellSize * 0.4f * captureScale, paint);
    }

    private String getSymbol(Piece.Type type) {
        switch (type) {
            case KING: return "♔";
            case QUEEN: return "♕";
            case ROOK: return "♖";
            case BISHOP: return "♗";
            case KNIGHT: return "♘";
            case PAWN: return "♙";
            default: return "?";
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return false;
        if (cellSize == 0) return false;

        int col = (int) (event.getX() / cellSize);
        int row = (int) (event.getY() / cellSize);
        if (row < 0 || row > 7 || col < 0 || col > 7) return false;

        Piece selectedPiece = gameManager.getBoard().getPiece(row, col);

        if (selectedR == -1) {
            if (selectedPiece != null && selectedPiece.isWhite() == gameManager.isWhiteTurn()) {
                selectedR = row;
                selectedC = col;
                validMoves = getValidMovesForPiece(selectedPiece, row, col);
                soundPool.play(soundSelect, 1, 1, 0, 0, 1);
            }
        } else {
            Piece target = gameManager.getBoard().getPiece(row, col);
            boolean isCapture = target != null && target.isWhite() != gameManager.isWhiteTurn();

            boolean moved = gameManager.tryMove(selectedR, selectedC, row, col);
            selectedR = -1;
            selectedC = -1;
            validMoves.clear();

            if (moved) {
                soundPool.play(isCapture ? soundCapture : soundMove, 1, 1, 0, 0, 1);
                if (isCapture) triggerCaptureAnimation(row, col);
            } else {
                if (selectedPiece != null && selectedPiece.isWhite() == gameManager.isWhiteTurn()) {
                    selectedR = row;
                    selectedC = col;
                    validMoves = getValidMovesForPiece(selectedPiece, row, col);
                }
            }
        }
        invalidate();
        return true;
    }

    private List<int[]> getValidMovesForPiece(Piece p, int r, int c) {
        List<int[]> moves = new ArrayList<>();
        for (int tr = 0; tr < 8; tr++) {
            for (int tc = 0; tc < 8; tc++) {
                if (validator.isValidMove(r, c, tr, tc, gameManager.isWhiteTurn())) {
                    moves.add(new int[]{tr, tc});
                }
            }
        }
        return moves;
    }

    private void triggerCaptureAnimation(int row, int col) {
        captureRow = row;
        captureCol = col;
        capturing = true;
        ObjectAnimator anim = ObjectAnimator.ofFloat(this, "captureScale", 1.0f, 0.0f);
        anim.setDuration(400);
        anim.start();
        anim.addUpdateListener(animation -> invalidate());
        postDelayed(() -> capturing = false, 450);
    }

    public void setCaptureScale(float value) {
        this.captureScale = value;
        invalidate();
    }

    public void resetGame() {
        gameManager = new GameManager();
        validator = new MoveValidator(gameManager.getBoard());
        selectedR = selectedC = -1;
        validMoves.clear();
        invalidate();
    }

    public boolean undoMove() {
        boolean ok = gameManager.undo();
        invalidate();
        return ok;
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
