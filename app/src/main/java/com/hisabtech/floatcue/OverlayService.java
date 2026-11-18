package com.hisabtech.floatcue;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.content.res.ColorStateList;
import android.widget.ProgressBar;
import java.util.Locale;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** A movable teleprompter overlay. No accessibility service, screen capture, camera, or microphone. */
public final class OverlayService extends Service {
    public static final String ACTION_SHOW = "com.hisabtech.floatcue.SHOW";
    public static final String ACTION_REFRESH = "com.hisabtech.floatcue.REFRESH";
    public static final String ACTION_STOP = "com.hisabtech.floatcue.STOP";
    private static final String CHANNEL = "floatcue_overlay";
    private static final int NOTIFICATION_ID = 2001;
    private static final int PURPLE = 0xff9a76ff;
    private static final int WHITE = 0xfff7f5ff;
    private static final int MUTED = 0xffa5abc2;
    public static volatile boolean isRunning = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windows;
    private WindowManager.LayoutParams params;
    private FrameLayout root;
    private LinearLayout panel;
    private TextView bubble, scriptText, status, metrics, countdownText;
    private ProgressBar progress;
    private View focusLine;
    private FrameLayout readingArea;
    private boolean countingDown = false, startedOnce = false;
    private Runnable countdownTick;
    private long lastMetricsUpdate = 0;
    private String displayedScriptId = "";
    private ScrollView scriptScroll;
    private Button playButton;
    private boolean playing = false;
    private boolean collapsed = false;
    private boolean mirrored = false;
    private int panelWidth, panelHeight;
    private int fontSp = 34;
    private float speed = 90;
    private double scrollPosition = 0;
    private long lastFrame = 0;
    private float density() { return getResources().getDisplayMetrics().density; }

    private final Runnable frame = new Runnable() {
        @Override public void run() {
            if (!playing || root == null || collapsed || scriptScroll == null) return;
            long now = SystemClock.uptimeMillis();
            double seconds = Math.min(.08, Math.max(0, now - lastFrame) / 1000.0);
            lastFrame = now;
            scrollPosition += speed * density() * seconds;
            int max = Math.max(0, scriptText.getHeight() - scriptScroll.getHeight());
            if (max > 0 && scrollPosition >= max) {
                scrollPosition = max; setPlaying(false);
            }
            scriptScroll.scrollTo(0, (int) scrollPosition);
            if (now - lastMetricsUpdate > 450) {
                updateProgress();
                persistProgress();
                lastMetricsUpdate = now;
            }
            if (playing) handler.postDelayed(this, 16);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        windows = (WindowManager) getSystemService(WINDOW_SERVICE);
        isRunning = true;
        createNotificationChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_SHOW : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopSelf(); return START_NOT_STICKY;
        }
        if (ACTION_REFRESH.equals(action)) {
            if (root != null) refreshFromSettings();
            return START_STICKY;
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf(); return START_NOT_STICKY;
        }
        startForeground(NOTIFICATION_ID, buildNotification());
        if (root == null) createOverlay();
        else refreshFromSettings();
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "Floating teleprompter",
                NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("FloatCue's persistent reading overlay");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Intent stop = new Intent(this, OverlayService.class).setAction(ACTION_STOP);
        PendingIntent stopAction = PendingIntent.getService(this, 1, stop,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("FloatCue reader active")
            .setContentText("Tap to edit or use Stop to close the overlay")
            .setContentIntent(pending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop reader", stopAction)
            .setOngoing(true)
            .build();
    }
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private Button control(String symbol, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(symbol); b.setTextSize(12); b.setAllCaps(false); b.setContentDescription(symbol);
        b.setTextColor(WHITE); b.setBackground(rounded(0xff39334f, 12));
        b.setMinWidth(0); b.setMinimumWidth(0);
        b.setMinHeight(0); b.setMinimumHeight(0);
        b.setPadding(0, 0, 0, 0);
        b.setOnClickListener(listener);
        return b;
    }
    private void addControl(LinearLayout row, Button b, int width) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(width), dp(42));
        p.leftMargin = dp(4);
        row.addView(b, p);
    }
    private void createOverlay() {
        panelWidth = ScriptStore.prefs(this).getInt("panel_width", dp(335));
        panelHeight = ScriptStore.prefs(this).getInt("panel_height", dp(310));
        params = new WindowManager.LayoutParams(
            panelWidth, panelHeight, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = ScriptStore.prefs(this).getInt("x", dp(15));
        params.y = ScriptStore.prefs(this).getInt("y", dp(120));

        root = new FrameLayout(this);
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setElevation(dp(10));
        root.addView(panel, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(9), dp(7), dp(9), dp(5));
        TextView grip = text("⋮⋮  ✦ FloatCue", 14, WHITE, true); grip.setContentDescription("Drag floating reader");
        grip.setOnTouchListener(dragListener(false));
        top.addView(grip, new LinearLayout.LayoutParams(0, dp(40), 1));
        addControl(top, control("A−", v -> changeFont(-2)), 40);
        addControl(top, control("A+", v -> changeFont(2)), 40);
        addControl(top, control("⌄", v -> collapse()), 40);
        addControl(top, control("×", v -> stopSelf()), 40);
        panel.addView(top, new LinearLayout.LayoutParams(-1, dp(50)));

        readingArea = new FrameLayout(this);
        scriptScroll = new ScrollView(this);
        scriptScroll.setFillViewport(false);
        scriptScroll.setVerticalScrollBarEnabled(false);
        scriptText = text("", 34, WHITE, false);
        scriptText.setGravity(Gravity.TOP | Gravity.START);
        scriptText.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        scriptText.setLineSpacing(dp(10), 1.14f);
        scriptText.setPadding(dp(20), dp(13), dp(20), dp(27));
        scriptScroll.addView(scriptText, new ScrollView.LayoutParams(-1, -2));
        scriptScroll.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN && playing) setPlaying(false);
            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                scrollPosition = scriptScroll.getScrollY();
                updateProgress(); persistProgress();
            }
            return false; // let ScrollView handle the actual gesture
        });
        readingArea.addView(scriptScroll, new FrameLayout.LayoutParams(-1, -1));
        focusLine = new View(this);
        focusLine.setBackground(rounded(0xffb9a2ff, 3));
        focusLine.setAlpha(.78f);
        FrameLayout.LayoutParams focusParams = new FrameLayout.LayoutParams(-1, dp(2), Gravity.TOP);
        focusParams.leftMargin = dp(18); focusParams.rightMargin = dp(18);
        readingArea.addView(focusLine, focusParams);
        readingArea.addOnLayoutChangeListener((v,left,top,right,bottom,oldLeft,oldTop,oldRight,oldBottom) -> {
            FrameLayout.LayoutParams fp = (FrameLayout.LayoutParams) focusLine.getLayoutParams();
            int desired = Math.max(dp(32), (bottom - top) / 3);
            if (fp.topMargin != desired) { fp.topMargin = desired; focusLine.setLayoutParams(fp); }
        });
        countdownText = text("3", 56, WHITE, true);
        countdownText.setGravity(Gravity.CENTER);
        countdownText.setBackground(rounded(0xee4b3480, 26));
        countdownText.setElevation(dp(8));
        countdownText.setVisibility(View.GONE);
        FrameLayout.LayoutParams countParams = new FrameLayout.LayoutParams(dp(100), dp(100), Gravity.CENTER);
        readingArea.addView(countdownText, countParams);
        panel.addView(readingArea, new LinearLayout.LayoutParams(-1, 0, 1));

        metrics = text("0%   •   Ready to read", 11, MUTED, true);
        metrics.setGravity(Gravity.CENTER_VERTICAL);
        metrics.setPadding(dp(17), 0, dp(12), 0);
        panel.addView(metrics, new LinearLayout.LayoutParams(-1, dp(19)));
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(1000);
        progress.setProgressTintList(ColorStateList.valueOf(PURPLE));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(0xff49405e));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(3));
        progressParams.leftMargin = dp(17); progressParams.rightMargin = dp(17);
        panel.addView(progress, progressParams);

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(dp(5), dp(4), dp(5), dp(4));
        playButton = control("▶  Play", v -> togglePlay()); playButton.setBackground(rounded(PURPLE, 13)); playButton.setTextColor(0xff160d25);
        addControl(footer, playButton, 76);
        addControl(footer, control("↺", v -> restart()), 44);
        addControl(footer, control("−", v -> changeSpeed(-20)), 44);
        addControl(footer, control("+", v -> changeSpeed(20)), 44);
        addControl(footer, control("⇋", v -> {
            mirrored = !mirrored;
            scriptText.setScaleX(mirrored ? -1 : 1);
        }), 44);
        panel.addView(footer, new LinearLayout.LayoutParams(-1, dp(50)));

        status = text("90 px/s    •    RESIZE ↘", 10, MUTED, true); status.setContentDescription("Drag to resize floating reader");
        status.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        status.setPadding(dp(6), 0, dp(14), 0);
        status.setOnTouchListener(resizeListener());
        panel.addView(status, new LinearLayout.LayoutParams(-1, dp(26)));

        bubble = text("✦", 23, WHITE, true); bubble.setContentDescription("Expand FloatCue reader");
        bubble.setGravity(Gravity.CENTER);
        bubble.setBackground(rounded(PURPLE, 29));
        bubble.setVisibility(View.GONE);
        bubble.setOnTouchListener(dragListener(true));
        root.addView(bubble, new FrameLayout.LayoutParams(-1, -1));

        refreshFromSettings();
        try {
            windows.addView(root, params);
            clampToScreen();
        } catch (Exception ex) {
            root = null; stopSelf();
        }
    }
    private void refreshFromSettings() {
        if (root == null) return;
        speed = ScriptStore.prefs(this).getInt("speed", 90);
        fontSp = ScriptStore.prefs(this).getInt("font", 34);
        scriptText.setTextSize(fontSp);
        ScriptStore.Script active = ScriptStore.active(this);
        String script = active.body;
        final boolean newScript = !active.id.equals(displayedScriptId);
        if (newScript) {
            cancelCountdown();
            setPlaying(false);
            displayedScriptId = active.id;
            startedOnce = false;
        }
        scriptText.setText((script == null || script.trim().isEmpty()) ?
            "Add a script in FloatCue to start reading." : script + "\n\n\n\n\n");
        int opacity = ScriptStore.prefs(this).getInt("opacity", 76);
        int alpha = Math.min(255, Math.max(60, (int)(255 * opacity / 100f)));
        int theme = ScriptStore.prefs(this).getInt("reader_theme", 0);
        int background = theme == 2 ? Color.argb(alpha, 246, 244, 236)
            : theme == 1 ? Color.argb(alpha, 44, 32, 71)
            : Color.argb(alpha, 22, 20, 39);
        panel.setBackground(rounded(background, 22));
        scriptText.setTextColor(theme == 2 ? 0xff181822 : WHITE);
        scriptText.setTypeface(Typeface.create(theme == 2 ? "serif" : "sans-serif", Typeface.NORMAL));
        focusLine.setVisibility(ScriptStore.prefs(this).getBoolean("focus_line", true)
            ? View.VISIBLE : View.GONE);
        int direction = ScriptStore.prefs(this).getInt("text_direction", 0);
        scriptText.setTextDirection(direction == 1 ? View.TEXT_DIRECTION_LTR
            : direction == 2 ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_FIRST_STRONG);
        scriptText.setGravity(Gravity.TOP | (direction == 2 ? Gravity.RIGHT : Gravity.START));
        status.setText((int)speed + " px/s    •    RESIZE ↘");
        scriptScroll.post(() -> {
            int max = Math.max(0, scriptText.getHeight() - scriptScroll.getHeight());
            if (newScript) {
                float ratio = ScriptStore.prefs(this).getFloat("progress_" + displayedScriptId, 0f);
                scrollPosition = Math.max(0, Math.min(max, ratio * max));
            } else scrollPosition = Math.max(0, Math.min(max, scrollPosition));
            scriptScroll.scrollTo(0, (int)scrollPosition);
            updateProgress();
        });
    }
    private void changeFont(int amount) {
        fontSp = Math.max(18, Math.min(64, fontSp + amount));
        ScriptStore.prefs(this).edit().putInt("font", fontSp).apply();
        scriptText.setTextSize(fontSp);
    }
    private void changeSpeed(int amount) {
        speed = Math.max(20, Math.min(250, speed + amount));
        ScriptStore.prefs(this).edit().putInt("speed", (int)speed).apply();
        if (status != null) status.setText((int)speed + " px/s    •    RESIZE ↘");
    }
    private void updateProgress() {
        if (scriptScroll == null || scriptText == null || metrics == null) return;
        int max = Math.max(0, scriptText.getHeight() - scriptScroll.getHeight());
        int percent = max == 0 ? 100 : (int) Math.min(100, Math.max(0, 100d * scrollPosition / max));
        if (progress != null) progress.setProgress(percent * 10);
        double remaining = max == 0 ? 0 : Math.max(0, max - scrollPosition) /
            Math.max(1d, speed * density());
        int seconds = (int) Math.ceil(remaining);
        String duration = String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60);
        metrics.setText(percent + "%  •  " + duration + " left"
            + (playing ? "   •   READING" : "   •   PAUSED"));
    }
    private void persistProgress() {
        if (scriptText == null || scriptScroll == null || displayedScriptId.isEmpty()) return;
        int max = Math.max(0, scriptText.getHeight() - scriptScroll.getHeight());
        float ratio = max == 0 ? 0 : Math.max(0f, Math.min(1f, (float)(scrollPosition / max)));
        ScriptStore.prefs(this).edit().putFloat("progress_" + displayedScriptId, ratio).apply();
    }
    private void cancelCountdown() {
        countingDown = false;
        if (countdownTick != null) handler.removeCallbacks(countdownTick);
        if (countdownText != null) countdownText.setVisibility(View.GONE);
    }
    private void countdown() {
        cancelCountdown();
        setPlaying(false);
        countingDown = true;
        countdownText.setText("3");
        countdownText.setVisibility(View.VISIBLE);
        countdownTick = new Runnable() {
            int count = 3;
            @Override public void run() {
                if (!countingDown || root == null || collapsed) return;
                count--;
                if (count > 0) {
                    countdownText.setText(Integer.toString(count));
                    handler.postDelayed(this, 1000);
                } else {
                    countingDown = false;
                    countdownText.setVisibility(View.GONE);
                    startedOnce = true;
                    setPlaying(true);
                }
            }
        };
        handler.postDelayed(countdownTick, 1000);
    }
    private void togglePlay() {
        if (countingDown) { cancelCountdown(); return; }
        if (playing) { setPlaying(false); persistProgress(); return; }
        if (!startedOnce && ScriptStore.prefs(this).getBoolean("countdown", true)) {
            countdown();
        } else {
            startedOnce = true;
            setPlaying(true);
        }
    }
    private void restart() {
        cancelCountdown();
        setPlaying(false);
        scrollPosition = 0;
        if (scriptScroll != null) scriptScroll.scrollTo(0, 0);
        persistProgress();
        startedOnce = false;
        updateProgress();
        togglePlay();
    }
    private void setPlaying(boolean value) {
        playing = value;
        handler.removeCallbacks(frame);
        if (playButton != null) playButton.setText(value ? "Ⅱ  Pause" : "▶  Play");
        if (value && !collapsed) {
            lastFrame = SystemClock.uptimeMillis();
            handler.post(frame);
        }
        updateProgress();
    }
    private void collapse() {
        cancelCountdown();
        persistProgress();
        setPlaying(false);
        collapsed = true;
        panel.setVisibility(View.GONE);
        bubble.setVisibility(View.VISIBLE);
        params.width = dp(58);
        params.height = dp(58);
        root.setElevation(dp(12));
        safeUpdate();
    }
    private void expand() {
        collapsed = false;
        bubble.setVisibility(View.GONE);
        panel.setVisibility(View.VISIBLE);
        params.width = panelWidth; params.height = panelHeight;
        clampToScreen();
        updateProgress();
    }

    private View.OnTouchListener dragListener(boolean isBubble) {
        return new View.OnTouchListener() {
            float downX, downY;
            int startX, startY;
            boolean moved;
            @Override public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX(); downY = event.getRawY();
                        startX = params.x; startY = params.y; moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = Math.round(event.getRawX() - downX);
                        int dy = Math.round(event.getRawY() - downY);
                        if (Math.abs(dx) > dp(5) || Math.abs(dy) > dp(5)) moved = true;
                        if (moved) {
                            params.x = startX + dx; params.y = startY + dy;
                            clampToScreen();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (isBubble && !moved) expand();
                        ScriptStore.prefs(OverlayService.this).edit()
                            .putInt("x", params.x).putInt("y", params.y).apply();
                        return true;
                    default: return true;
                }
            }
        };
    }
    private View.OnTouchListener resizeListener() {
        return new View.OnTouchListener() {
            float firstX, firstY;
            int initialW, initialH;
            @Override public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        firstX = event.getRawX(); firstY = event.getRawY();
                        initialW = params.width; initialH = params.height;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.width = Math.max(dp(255), initialW + Math.round(event.getRawX() - firstX));
                        params.height = Math.max(dp(175), initialH + Math.round(event.getRawY() - firstY));
                        clampSize(); safeUpdate(); return true;
                    case MotionEvent.ACTION_UP:
                        panelWidth = params.width; panelHeight = params.height;
                        ScriptStore.prefs(OverlayService.this).edit()
                            .putInt("panel_width", panelWidth)
                            .putInt("panel_height", panelHeight).apply();
                        return true;
                    default: return true;
                }
            }
        };
    }
    private void clampSize() {
        android.util.DisplayMetrics m = getResources().getDisplayMetrics();
        params.width = Math.min(params.width, m.widthPixels);
        params.height = Math.min(params.height, m.heightPixels);
    }
    private void clampToScreen() {
        if (params == null) return;
        clampSize();
        android.util.DisplayMetrics m = getResources().getDisplayMetrics();
        params.x = Math.max(0, Math.min(params.x, Math.max(0, m.widthPixels - params.width)));
        params.y = Math.max(0, Math.min(params.y, Math.max(0, m.heightPixels - params.height - dp(24))));
        safeUpdate();
    }
    private void safeUpdate() {
        if (windows == null || root == null || root.getWindowToken() == null) return;
        try { windows.updateViewLayout(root, params); } catch (Exception ignored) { }
    }
    @Override public void onConfigurationChanged(Configuration cfg) {
        super.onConfigurationChanged(cfg);
        if (root != null) clampToScreen();
    }
    @Override public void onDestroy() {
        cancelCountdown();
        persistProgress();
        setPlaying(false);
        handler.removeCallbacksAndMessages(null);
        if (windows != null && root != null) {
            try { windows.removeView(root); } catch (Exception ignored) { }
        }
        root = null;
        isRunning = false;
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
