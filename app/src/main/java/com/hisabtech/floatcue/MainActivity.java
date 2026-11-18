package com.hisabtech.floatcue;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Offline-first native script editor for FloatCue. */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(15, 17, 27);
    private static final int SURFACE = Color.rgb(28, 31, 46);
    private static final int WHITE = Color.rgb(248, 248, 255);
    private static final int MUTED = Color.rgb(174, 180, 205);
    private static final int PURPLE = Color.rgb(144, 105, 252);
    private static final int DOC_REQUEST = 123;

    private LinearLayout root, scriptList;
    private EditText titleInput, scriptInput;
    private TextView permissionLabel, speedLabel, fontLabel, opacityLabel;
    private ScriptStore.Script selected;

    @Override public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        selected = ScriptStore.active(this);
        buildUi();
        handleIncoming(getIntent());
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncoming(intent);
    }
    @Override protected void onResume() {
        super.onResume();
        updatePermissionLabel();
    }
    private void updatePermissionLabel() {
        if (permissionLabel != null) permissionLabel.setText(
            Settings.canDrawOverlays(this) ? "● Overlay access enabled" : "○ Overlay access required");
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private LinearLayout column() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }
    private LinearLayout row() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.HORIZONTAL);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }
    private void pad(View v, int l, int t, int r, int b) {
        v.setPadding(dp(l), dp(t), dp(r), dp(b));
    }
    private void margin(LinearLayout parent, View child, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(top); parent.addView(child, p);
    }
    private Button button(String label, boolean primary, View.OnClickListener action) {
        Button b = new Button(this);
        b.setAllCaps(false); b.setText(label); b.setTextSize(14);
        b.setTextColor(WHITE);
        b.setBackground(shape(primary ? PURPLE : SURFACE, 16));
        b.setMinHeight(dp(48)); b.setOnClickListener(action);
        return b;
    }
    private void putRowButton(LinearLayout r, Button b, float weight) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(50), weight);
        p.rightMargin = dp(6); r.addView(b, p);
    }
    private void buildUi() {
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setBackgroundColor(BG);
        root = column(); pad(root, 20, 24, 20, 36);
        scroller.addView(root);
        setContentView(scroller);

        TextView eyebrow = text("FLOATCUE  /  CREATOR STUDIO", 11, PURPLE, true);
        root.addView(eyebrow);
        TextView heading = text("Speak naturally.\nRead beautifully.", 30, WHITE, true);
        margin(root, heading, 12);
        TextView intro = text("Your floating teleprompter stays with you over compatible apps, cameras and live-streaming screens.", 14, MUTED, false);
        margin(root, intro, 10);

        permissionLabel = text("", 13, MUTED, false);
        margin(root, permissionLabel, 20);
        Button permission = button("Allow floating window", false, v -> {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        });
        margin(root, permission, 10);

        TextView scriptsLabel = text("SCRIPTS", 12, MUTED, true);
        margin(root, scriptsLabel, 24);
        scriptList = column();
        margin(root, scriptList, 8);

        titleInput = new EditText(this);
        titleInput.setSingleLine(true);
        titleInput.setTextColor(WHITE); titleInput.setHintTextColor(MUTED);
        titleInput.setTextSize(17); titleInput.setHint("Script title");
        titleInput.setBackground(shape(SURFACE, 14));
        pad(titleInput, 16, 12, 16, 12);
        margin(root, titleInput, 16);

        scriptInput = new EditText(this);
        scriptInput.setGravity(Gravity.TOP | Gravity.START);
        scriptInput.setMinLines(8);
        scriptInput.setTextSize(18);
        scriptInput.setTextColor(WHITE); scriptInput.setHintTextColor(MUTED);
        scriptInput.setHint("Paste or write your speech here…");
        scriptInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                                  InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        scriptInput.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        scriptInput.setBackground(shape(SURFACE, 18));
        pad(scriptInput, 18, 16, 18, 18);
        margin(root, scriptInput, 10);

        LinearLayout row1 = row();
        putRowButton(row1, button("Paste", false, v -> paste()), 1);
        putRowButton(row1, button("Import TXT", false, v -> importText()), 1);
        putRowButton(row1, button("Save", true, v -> save()), 1);
        margin(root, row1, 12);

        LinearLayout row2 = row();
        putRowButton(row2, button("+ New", false, v -> {
            save(); selected = ScriptStore.create(this); bindScript(); renderList();
        }), 1);
        putRowButton(row2, button("Delete", false, v -> {
            ScriptStore.delete(this, selected.id);
            selected = ScriptStore.active(this); bindScript(); renderList();
            toast("Script deleted");
        }), 1);
        margin(root, row2, 6);

        TextView appearance = text("READING PREFERENCES", 12, MUTED, true);
        margin(root, appearance, 24);
        speedLabel = text("", 14, WHITE, true);
        margin(root, speedLabel, 12);
        addSeek("speed", 20, 250, 90, speedLabel, "Scroll speed", " px/sec");
        fontLabel = text("", 14, WHITE, true);
        margin(root, fontLabel, 10);
        addSeek("font", 18, 64, 34, fontLabel, "Font size", " sp");
        opacityLabel = text("", 14, WHITE, true);
        margin(root, opacityLabel, 10);
        addSeek("opacity", 20, 95, 76, opacityLabel, "Panel opacity", "%");

        Button start = button("▶ Start floating teleprompter", true, v -> startOverlay());
        margin(root, start, 22);
        Button stop = button("Stop floating window", false, v ->
            stopService(new Intent(this, OverlayService.class)));
        margin(root, stop, 8);
        TextView tip = text("Tip: Start FloatCue, then open TikTok, Facebook or your phone camera. Drag the top bar to move it. The overlay is not guaranteed on every protected screen.", 13, MUTED, false);
        margin(root, tip, 18);

        bindScript(); renderList(); updatePermissionLabel();
    }
    private void addSeek(String key, int min, int max, int def, TextView label, String name, String suffix) {
        int saved = ScriptStore.prefs(this).getInt(key, def);
        label.setText(name + ": " + saved + suffix);
        SeekBar seek = new SeekBar(this);
        seek.setMax(max - min); seek.setProgress(Math.max(0, Math.min(max-min, saved-min)));
        seek.setProgressTintList(android.content.res.ColorStateList.valueOf(PURPLE));
        seek.setThumbTintList(android.content.res.ColorStateList.valueOf(PURPLE));
        margin(root, seek, 4);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar s) { }
            public void onStopTrackingTouch(SeekBar s) { }
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                int value = progress + min;
                label.setText(name + ": " + value + suffix);
                if (fromUser) {
                    ScriptStore.prefs(MainActivity.this).edit().putInt(key, value).apply();
                    refreshOverlay();
                }
            }
        });
    }
    private void renderList() {
        scriptList.removeAllViews();
        for (ScriptStore.Script script : ScriptStore.all(this)) {
            final boolean isSelected = script.id.equals(selected.id);
            TextView item = text((isSelected ? "● " : "○ ") + script.title, 15,
                isSelected ? PURPLE : WHITE, isSelected);
            item.setBackground(shape(SURFACE, 12)); pad(item, 14, 10, 14, 10);
            margin(scriptList, item, 4);
            item.setOnClickListener(v -> {
                save(); selected = script; ScriptStore.select(this, script.id);
                bindScript(); renderList();
            });
        }
    }
    private void bindScript() {
        titleInput.setText(selected.title);
        scriptInput.setText(selected.body);
    }
    private void save() {
        selected.title = titleInput.getText().toString().trim();
        if (selected.title.isEmpty()) selected.title = "Untitled script";
        selected.body = scriptInput.getText().toString();
        ScriptStore.save(this, selected);
        renderList(); refreshOverlay();
        toast("Saved locally");
    }
    private void paste() {
        ClipboardManager m = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (m != null && m.hasPrimaryClip()) {
            ClipData clip = m.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                CharSequence text = clip.getItemAt(0).coerceToText(this);
                int cursor = Math.max(0, scriptInput.getSelectionStart());
                scriptInput.getText().insert(cursor, text);
            }
        }
    }
    private void importText() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/plain");
        startActivityForResult(i, DOC_REQUEST);
    }
    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == DOC_REQUEST && res == RESULT_OK && data != null && data.getData() != null) {
            try (InputStream stream = getContentResolver().openInputStream(data.getData());
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096]; int n;
                while ((n = stream.read(buf)) != -1 && out.size() < 1024 * 1024) out.write(buf, 0, n);
                scriptInput.setText(out.toString(StandardCharsets.UTF_8.name()));
            } catch (Exception e) { toast("Could not import TXT file"); }
        }
    }
    private void handleIncoming(Intent intent) {
        if (intent == null) return;
        CharSequence incoming = null;
        if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType()))
            incoming = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        if (Intent.ACTION_PROCESS_TEXT.equals(intent.getAction()))
            incoming = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        if (incoming != null) {
            selected = ScriptStore.create(this);
            selected.title = "Imported script";
            selected.body = incoming.toString();
            ScriptStore.save(this, selected);
            bindScript(); renderList();
            intent.setAction(null);
            toast("Script imported from another app");
        }
    }
    private void refreshOverlay() {
        Intent i = new Intent(this, OverlayService.class);
        i.setAction(OverlayService.ACTION_REFRESH);
        if (OverlayService.isRunning) startService(i);
    }
    private void startOverlay() {
        save();
        if (!Settings.canDrawOverlays(this)) {
            toast("Please allow Draw over other apps, then return and tap Start");
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())));
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{ Manifest.permission.POST_NOTIFICATIONS }, 42);
        }
        Intent i = new Intent(this, OverlayService.class);
        i.setAction(OverlayService.ACTION_SHOW);
        try { startForegroundService(i); toast("FloatCue is floating — switch to your camera!"); }
        catch (Exception e) { toast("Unable to start overlay: " + e.getClass().getSimpleName()); }
    }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
