package com.example.chessgame.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "chess.db";
    private static final int DB_VER = 2; // ✅ tăng version để reset DB cũ
    public static final String TABLE_GAME = "GameHistory";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VER);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_GAME + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "mode TEXT," +
                "winner TEXT," +
                "total_moves INTEGER," +
                "date_played TEXT)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GAME);
        onCreate(db);
    }

    // ✅ Ghi lại lịch sử ván cờ
    public long insertGame(String mode, String winner, int totalMoves) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("mode", mode);
        cv.put("winner", winner);
        cv.put("total_moves", totalMoves);

        // Lưu ngày giờ dễ đọc
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        cv.put("date_played", sdf.format(new Date()));

        long id = db.insert(TABLE_GAME, null, cv);
        db.close();
        return id;
    }

    // ✅ Lấy toàn bộ lịch sử
    public List<GameRecord> getAllGames() {
        List<GameRecord> res = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, mode, winner, total_moves, date_played FROM " + TABLE_GAME + " ORDER BY id DESC", null);

        if (c.moveToFirst()) {
            do {
                GameRecord gr = new GameRecord(
                        c.getInt(0),
                        c.getString(1),
                        c.getString(2),
                        c.getInt(3),
                        c.getString(4)
                );
                res.add(gr);
            } while (c.moveToNext());
        }

        c.close();
        db.close();
        return res;
    }

    // ✅ Lớp lưu dữ liệu 1 ván
    public static class GameRecord {
        public int id;
        public String mode;
        public String winner;
        public int totalMoves;
        public String datePlayed;

        public GameRecord(int id, String mode, String winner, int totalMoves, String datePlayed) {
            this.id = id;
            this.mode = mode;
            this.winner = winner;
            this.totalMoves = totalMoves;
            this.datePlayed = datePlayed;
        }
    }
}
