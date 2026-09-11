package com.phuong.screenoff;

import android.app.Activity;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String PREFS = "screenoff_settings";
    private static final String KEY_DOUBLE_TAP_HOME = "double_tap_home";

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        lp.setMargins(0, dp(8), 0, dp(8));
        b.setLayoutParams(lp);
        return b;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(40), dp(24), dp(24));

        TextView title = new TextView(this);
        title.setText(R.string.title);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView info = new TextView(this);
        info.setText(R.string.instructions);
        info.setTextSize(16);
        info.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        infoLp.setMargins(0, dp(16), 0, dp(20));
        root.addView(info, infoLp);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        Switch doubleTapSwitch = new Switch(this);
        doubleTapSwitch.setText(R.string.double_tap_home);
        doubleTapSwitch.setTextSize(16);
        doubleTapSwitch.setChecked(prefs.getBoolean(KEY_DOUBLE_TAP_HOME, true));
        doubleTapSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(KEY_DOUBLE_TAP_HOME, isChecked).apply());
        LinearLayout.LayoutParams switchLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        switchLp.setMargins(0, dp(4), 0, dp(16));
        root.addView(doubleTapSwitch, switchLp);

        Button accessibilityButton = makeButton(getString(R.string.enable_accessibility));
        accessibilityButton.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(accessibilityButton);

        Button lockButton = makeButton(getString(R.string.lock_now));
        lockButton.setOnClickListener(v -> {
            if (!ScreenOffAccessibilityService.lockScreenNow()) {
                Toast.makeText(this, R.string.enable_accessibility_first, Toast.LENGTH_LONG).show();
            }
        });
        root.addView(lockButton);

        Button addTileButton = makeButton(getString(R.string.add_tile));
        addTileButton.setOnClickListener(v -> addQuickSettingsTile());
        root.addView(addTileButton);
        setContentView(root);
    }

    private void addQuickSettingsTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            StatusBarManager manager = getSystemService(StatusBarManager.class);
            ComponentName component = new ComponentName(this, ScreenOffTileService.class);
            Icon icon = Icon.createWithResource(this, R.drawable.ic_screen_off);
            manager.requestAddTileService(component, getString(R.string.tile_label), icon, getMainExecutor(), result -> {
                String msg;
                if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) msg = getString(R.string.tile_added);
                else if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) msg = getString(R.string.tile_already_added);
                else msg = getString(R.string.tile_add_manual);
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            });
        } else {
            Toast.makeText(this, R.string.tile_add_manual, Toast.LENGTH_LONG).show();
        }
    }
}
