package com.example.chessgame;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chessgame.db.DatabaseHelper;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity {
    private ListView lvHistory;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // 🔹 Thanh tiêu đề + nút quay lại
        MaterialToolbar toolbar = findViewById(R.id.toolbarHistory);
        toolbar.setNavigationOnClickListener(v -> finish());

        lvHistory = findViewById(R.id.lvHistory);
        db = new DatabaseHelper(this);

        var list = db.getAllGames();
        android.util.Log.d("HISTORY_DEBUG", "Số bản ghi: " + list.size());

        ArrayList<String> items = new ArrayList<>();
        for (var g : list) {
            String s = "🕹️  " + g.mode +
                    "\n🏆  " + g.winner +
                    "\n♟️  Số nước: " + g.totalMoves +
                    "\n📅  " + g.datePlayed +
                    "\n────────────────────────────";
            items.add(s);
        }

        // ✨ Adapter có ripple sáng
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = view.findViewById(android.R.id.text1);
                tv.setTextColor(Color.WHITE);
                tv.setTextSize(18);
                tv.setLineSpacing(6f, 1f);
                tv.setPadding(32, 24, 32, 24);

                // 💡 Ripple trắng sáng dễ nhìn
                int rippleColor = Color.parseColor("#33FFFFFF");
                ColorDrawable mask = new ColorDrawable(Color.WHITE);
                RippleDrawable ripple = new RippleDrawable(
                        new android.content.res.ColorStateList(
                                new int[][]{new int[]{}},
                                new int[]{rippleColor}
                        ),
                        new ColorDrawable(Color.TRANSPARENT),
                        mask
                );
                view.setBackground(ripple);

                return view;
            }
        };

        lvHistory.setAdapter(adapter);

        // 🎨 Nền và padding chung
        lvHistory.setDividerHeight(12);
        lvHistory.setPadding(24, 24, 24, 24);
        lvHistory.setCacheColorHint(Color.TRANSPARENT);
        lvHistory.setBackgroundColor(Color.parseColor("#121212"));
    }
}
