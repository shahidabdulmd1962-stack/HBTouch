package com.hb.touch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView debug;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(48, 96, 48, 48);

        TextView tv = new TextView(this);
        tv.setTextSize(16);
        tv.setText("HB Touch\n\nTurn on \"HB Touch\" in Accessibility.\n"
                + "Then double tap an empty spot on the Home screen to turn the display off.\n");

        Button btn = new Button(this);
        btn.setText("Open Accessibility Settings");
        btn.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        TextView dbgTitle = new TextView(this);
        dbgTitle.setTextSize(14);
        dbgTitle.setPadding(0, 48, 0, 8);
        dbgTitle.setText("Last tap check (debug):");

        debug = new TextView(this);
        debug.setTextSize(14);
        debug.setTextIsSelectable(true);

        Button refresh = new Button(this);
        refresh.setText("Refresh");
        refresh.setOnClickListener(v -> debug.setText(DoubleTapService.lastReason));

        root.addView(tv);
        root.addView(btn);
        root.addView(dbgTitle);
        root.addView(debug);
        root.addView(refresh);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        debug.setText(DoubleTapService.lastReason);
    }
}
