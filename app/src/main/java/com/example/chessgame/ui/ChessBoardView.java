package com.example.chessgame.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Color;

import com.example.chessgame.R;
import com.example.chessgame.logic.GameManager;
import com.example.chessgame.logic.MoveValidator;
import com.example.chessgame.model.Piece;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ChessBoardView (updated)
 *
 * - Dùng chung MoveValidator từ GameManager (không tạo validator cục bộ)
 * - Bảo vệ các cuộc gọi kiểm tra nước hợp lệ bằng try/catch để tránh crash/đơ
 * - Ghi log (Log.d/w/e) để dễ debug khi có vấn đề về move validation
 *
 * Lưu ý: để hoạt động đúng, GameManager phải expose hàm getValidator() trả MoveValidator.
 */
public class ChessBoardView extends View {

    private static final String TAG = "ChessBoardView";

    // Callback để ChessActivity cập nhật UI khi có nước đi xong
    public interface OnMoveListener {
        void onMoveCompleted();
    }
    private OnMoveListener moveListener;
    public void setOnMoveListener(OnMoveListener listener) {
        this.moveListener = listener;
    }

    // Drawing / state fields
    private Paint paint = new Paint();
    private int cellSize;

    // Core logic holders
    private GameManager gameManager;
    private MoveValidator validator; // --> sẽ lấy từ gameManager (chung 1 instance)

    // Selected square + valid moves for highlighting
    private int selectedR = -1, selectedC = -1;
    private List<int[]> validMoves = new ArrayList<>();

    // Sound & visual capture effect
    private SoundPool soundPool;
    private int soundSelect, soundMove, soundCapture;
    private float captureScale = 1.0f;
    private boolean capturing = false;
    private int captureRow = -1, captureCol = -1;

    // Images
    private Bitmap lightSquare, darkSquare;
    private Map<String, Bitmap> pieceImages = new HashMap<>();

    // ---------------- constructor ----------------
    public ChessBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    // ---------------- init ----------------
    private void init() {
        paint.setAntiAlias(true);

        // Tạo gameManager ở đây (UI giữ 1 instance GameManager)
        gameManager = new GameManager();

        // Lấy MoveValidator từ GameManager để đảm bảo trạng thái validator đồng bộ
        // (không tạo MoveValidator riêng, tránh mismatch en-passant, v.v.)
        validator = gameManager.getValidator();

        initSounds();
        loadImages();
    }

    // ---------------- sounds & images ----------------
    private void initSounds() {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(attrs)
                .build();

        // load sound resources (nếu có)
        soundSelect = soundPool.load(getContext(), R.raw.select, 1);
        soundMove = soundPool.load(getContext(), R.raw.move, 1);
        soundCapture = soundPool.load(getContext(), R.raw.capture, 1);
    }

    private void loadImages() {
        // tải image ô và quân từ resources
        lightSquare = BitmapFactory.decodeResource(getResources(), R.drawable.chess_light);
        darkSquare = BitmapFactory.decodeResource(getResources(), R.drawable.chess_dark);

        pieceImages.put("WHITE_KING", BitmapFactory.decodeResource(getResources(), R.drawable.w_king));
        pieceImages.put("WHITE_QUEEN", BitmapFactory.decodeResource(getResources(), R.drawable.w_queen));
        pieceImages.put("WHITE_ROOK", BitmapFactory.decodeResource(getResources(), R.drawable.w_rook));
        pieceImages.put("WHITE_BISHOP", BitmapFactory.decodeResource(getResources(), R.drawable.w_bishop));
        pieceImages.put("WHITE_KNIGHT", BitmapFactory.decodeResource(getResources(), R.drawable.w_knight));
        pieceImages.put("WHITE_PAWN", BitmapFactory.decodeResource(getResources(), R.drawable.w_pawn));

        pieceImages.put("BLACK_KING", BitmapFactory.decodeResource(getResources(), R.drawable.b_king));
        pieceImages.put("BLACK_QUEEN", BitmapFactory.decodeResource(getResources(), R.drawable.b_queen));
        pieceImages.put("BLACK_ROOK", BitmapFactory.decodeResource(getResources(), R.drawable.b_rook));
        pieceImages.put("BLACK_BISHOP", BitmapFactory.decodeResource(getResources(), R.drawable.b_bishop));
        pieceImages.put("BLACK_KNIGHT", BitmapFactory.decodeResource(getResources(), R.drawable.b_knight));
        pieceImages.put("BLACK_PAWN", BitmapFactory.decodeResource(getResources(), R.drawable.b_pawn));
    }

    // ---------------- drawing ----------------
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
                boolean isLight = (r + c) % 2 == 0;
                Bitmap square = isLight ? lightSquare : darkSquare;
                if (square != null) {
                    Rect dst = new Rect(c * cellSize, r * cellSize, (c + 1) * cellSize, (r + 1) * cellSize);
                    canvas.drawBitmap(square, null, dst, paint);
                }
            }
        }
    }

    private void drawHighlights(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x5000C800); // translucent green
        for (int[] move : validMoves) {
            int r = move[0], c = move[1];
            canvas.drawRect(c * cellSize, r * cellSize, (c + 1) * cellSize, (r + 1) * cellSize, paint);
        }
    }

    private void drawPieces(Canvas canvas) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = gameManager.getBoard().getPiece(r, c);
                if (p != null) {
                    String key = (p.isWhite() ? "WHITE_" : "BLACK_") + p.getType().name();
                    Bitmap img = pieceImages.get(key);
                    if (img != null) {
                        Rect dst = new Rect(c * cellSize, r * cellSize, (c + 1) * cellSize, (r + 1) * cellSize);
                        paint.setAlpha(255);
                        canvas.drawBitmap(img, null, dst, paint);
                    }
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
        paint.setColor(Color.argb(180, 255, 0, 0));
        canvas.drawCircle(cx, cy, cellSize * 0.4f * captureScale, paint);
    }

    // ---------------- touch handling ----------------
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Chỉ xử lý khi nhấn xuống
        if (event.getAction() != MotionEvent.ACTION_DOWN) return false;
        if (cellSize == 0) return false;

        // Không cho phép thao tác nếu ván đã kết thúc
        if (gameManager.isGameOver()) return false;

        int col = (int) (event.getX() / cellSize);
        int row = (int) (event.getY() / cellSize);
        if (row < 0 || row > 7 || col < 0 || col > 7) return false;

        // Lấy quân tại ô nhấn
        Piece selectedPiece = gameManager.getBoard().getPiece(row, col);

        // Nếu chưa có ô được chọn trước đó -> chọn quân (nếu của bên đang đi)
        if (selectedR == -1) {
            if (selectedPiece != null && selectedPiece.isWhite() == gameManager.isWhiteTurn()) {
                selectedR = row;
                selectedC = col;

                // Lấy danh sách valid moves bằng validator từ GameManager; bọc try/catch để an toàn
                try {
                    validMoves = getValidMovesForPiece(selectedPiece, row, col);
                } catch (Exception ex) {
                    // Log nếu có lỗi bất thường trong validator -> tránh crash app
                    Log.e(TAG, "Error while computing valid moves", ex);
                    validMoves = new ArrayList<>();
                }

                // Play select sound
                try { soundPool.play(soundSelect, 1, 1, 0, 0, 1); } catch (Exception ignored) {}
            }
        } else {
            // Có ô được chọn → cố gắng di chuyển tới ô nhấn
            Piece target = gameManager.getBoard().getPiece(row, col);
            boolean isCapture = target != null && target.isWhite() != gameManager.isWhiteTurn();

            // Thực hiện move thông qua GameManager (tryMove sẽ kiểm tra tính hợp lệ)
            boolean moved;
            try {
                moved = gameManager.tryMove(selectedR, selectedC, row, col);
            } catch (Exception ex) {
                // Bảo vệ: nếu có lỗi bất ngờ trong tryMove -> log và coi như không di chuyển
                Log.e(TAG, "Exception while trying move", ex);
                moved = false;
            }

            // Reset selection & highlights
            selectedR = -1;
            selectedC = -1;
            validMoves.clear();

            if (moved) {
                // Play sound / trigger animation / redraw
                try { soundPool.play(isCapture ? soundCapture : soundMove, 1, 1, 0, 0, 1); } catch (Exception ignored) {}
                if (isCapture) triggerCaptureAnimation(row, col);
                invalidate();

                // Báo Activity để cập nhật trạng thái (ví dụ kiểm tra gameOver và lưu lịch sử)
                if (moveListener != null) moveListener.onMoveCompleted();
            }
        }

        // Luôn gọi invalidate để redraw UI (dù có di chuyển hay không)
        invalidate();
        return true;
    }

    // ---------------- compute valid moves ----------------
    private List<int[]> getValidMovesForPiece(Piece p, int r, int c) {
        List<int[]> moves = new ArrayList<>();

        // Lấy validator *mỗi lần* từ gameManager (phòng trường hợp validator instance trong GM thay đổi)
        // (this avoids stale validator reference)
        MoveValidator mv = gameManager.getValidator();
        if (mv == null) {
            Log.w(TAG, "MoveValidator is null in GameManager!");
            return moves;
        }

        // Duyệt toàn bộ ô đích để tìm nước hợp lệ
        for (int tr = 0; tr < 8; tr++) {
            for (int tc = 0; tc < 8; tc++) {
                try {
                    // Bọc gọi isValidMove bằng try/catch để tránh các lỗi mô phỏng hiếm
                    if (mv.isValidMove(r, c, tr, tc, gameManager.isWhiteTurn())) {
                        moves.add(new int[]{tr, tc});
                    }
                } catch (Exception ex) {
                    // Nếu validator ném exception (hiếm), log & bỏ qua nước đó
                    Log.e(TAG, "Validator exception for move " + r + "," + c + " -> " + tr + "," + tc, ex);
                }
            }
        }
        return moves;
    }

    // ---------------- capture animation ----------------
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

    // ---------------- external controls ----------------
    public void resetGame() {
        // Reset game + re-acquire validator from GameManager to remain in sync
        gameManager.reset();
        validator = gameManager.getValidator();
        selectedR = selectedC = -1;
        validMoves.clear();
        invalidate();
    }

    public boolean undoMove() {
        boolean ok = gameManager.undo();
        // After undo, validator state inside GameManager may have changed, re-acquire for safety
        validator = gameManager.getValidator();
        invalidate();
        return ok;
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
