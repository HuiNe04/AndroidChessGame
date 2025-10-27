package com.example.chessgame.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "chess.db";
    private static final int DB_VER = 1;
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

    public long insertGame(String mode, String winner, int totalMoves) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("mode", mode);
        cv.put("winner", winner);
        cv.put("total_moves", totalMoves);
        cv.put("date_played", String.valueOf(System.currentTimeMillis()));
        long id = db.insert(TABLE_GAME, null, cv);
        db.close();
        return id;
    }

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
