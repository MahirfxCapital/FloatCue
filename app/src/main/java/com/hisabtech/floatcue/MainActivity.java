package com.hisabtech.floatcue;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * FloatCue Signature Studio.
 * A real native, offline-first Android UI: no browser wrapper and no remote assets.
 */
public final class MainActivity extends Activity {
    private static final int BG = 0xff0d1019;
    private static final int SURFACE = 0xff191d2b;
    private static final int SURFACE_HIGH = 0xff222537;
    private static final int BORDER = 0xff37394c;
    private static final int WHITE = 0xfff7f5ff;
    private static final int MUTED = 0xffa5abc2;
    private static final int PURPLE = 0xff9a76ff;
    private static final int MINT = 0xff91f0c4;
    private static final int DOC_REQUEST = 123;
    private static final int EXPORT_TXT = 124;
    private static final int EXPORT_BACKUP = 125;
    private static final int RESTORE_BACKUP = 126;

    private LinearLayout screen, content, navBar;
    private TextView permissionText, stylePreview;
    private View previewGuide;
    private EditText titleInput, scriptInput;
    private ScriptStore.Script selected;
    private int tab = 0; // Studio, Library, Style
    private boolean editorOpen = false;
    private final Handler autosaveHandler = new Handler(Looper.getMainLooper());
    private final Runnable autosaveTask = () -> saveEditor(false);
    private boolean dirty = false;

    @Override public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        selected = ScriptStore.active(this);
        buildStudio();
        handleIncoming(getIntent());
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncoming(intent);
    }
    @Override protected void onResume() {
        super.onResume();
        if (!editorOpen) updatePermission();
    }
    @Override protected void onPause() {
        if (editorOpen) saveEditor(false);
        super.onPause();
    }
    @Override public void onBackPressed() {
        if (editorOpen) {
            saveEditor(false);
            editorOpen = false;
            buildStudio();
        } else if (tab != 0) {
            tab = 0;
            renderTab();
            renderNav();
        } else super.onBackPressed();
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius));
        return d;
    }
    private GradientDrawable outlined(int color, int radius, int edge) {
        GradientDrawable d = rounded(color, radius);
        d.setStroke(dp(1), edge);
        return d;
    }
    private GradientDrawable gradient(int color1, int color2, int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{color1,color2});
        d.setCornerRadius(dp(radius));
        return d;
    }
    private Drawable touchBackground(Drawable background) {
        return new RippleDrawable(ColorStateList.valueOf(0x44ffffff), background, null);
    }
    private LinearLayout vertical() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL); return v;
    }
    private LinearLayout horizontal() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.HORIZONTAL);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }
    private TextView text(String s,int size,int color,boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setIncludeFontPadding(false);
        t.setFontFeatureSettings("kern");
        t.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        return t;
    }
    private void pad(View v,int l,int t,int r,int b) {
        v.setPadding(dp(l),dp(t),dp(r),dp(b));
    }
    private void add(LinearLayout parent,View child,int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.topMargin=dp(top);
        parent.addView(child,lp);
    }
    private void addWeighted(LinearLayout parent,View child,int weight) {
        parent.addView(child,new LinearLayout.LayoutParams(0,-2,weight));
    }
    private TextView caption(String s) {
        TextView t = text(s,11,MUTED,true);
        t.setLetterSpacing(.14f);
        return t;
    }
    private TextView pill(String s,int foreground,int background) {
        TextView t=text(s,11,foreground,true);
        t.setBackground(rounded(background,30));
        pad(t,12,8,12,8);
        t.setGravity(Gravity.CENTER);
        return t;
    }
    private TextView button(String s,boolean primary,View.OnClickListener onClick) {
        TextView b=text(s,15,primary?0xff110e1b:WHITE,true);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(dp(54));
        b.setBackground(touchBackground(primary
            ? gradient(0xffc4adff,0xff9170ff,17)
            : outlined(SURFACE_HIGH,17,BORDER)));
        pad(b,14,14,14,14);
        b.setOnClickListener(onClick);
        b.setClickable(true); b.setFocusable(true);
        b.setContentDescription(s);
        return b;
    }
    private TextView tinyAction(String s,View.OnClickListener onClick) {
        TextView v = text(s,13,PURPLE,true);
        pad(v,8,12,8,12);
        v.setOnClickListener(onClick);
        v.setClickable(true);
        return v;
    }
    private void sectionHeading(LinearLayout parent,String label,String action,View.OnClickListener click) {
        LinearLayout line=horizontal();
        addWeighted(line,caption(label),1);
        if(action!=null) line.addView(tinyAction(action,click));
        add(parent,line,24);
    }
    private void buildStudio() {
        editorOpen=false;
        titleInput=null; scriptInput=null;
        screen=vertical();screen.setBackgroundColor(BG);
        setContentView(screen);
        LinearLayout header=horizontal();
        pad(header,20,17,20,13);
        TextView symbol=text("✦",23,0xff211630,true);
        symbol.setGravity(Gravity.CENTER);
        symbol.setBackground(gradient(0xffcbb7ff,0xff9973f5,14));
        header.addView(symbol,new LinearLayout.LayoutParams(dp(44),dp(44)));
        LinearLayout name=vertical(); pad(name,12,0,0,0);
        TextView wordmark=text("FloatCue",21,WHITE,true);
        TextView line=text("CREATOR STUDIO",9,MUTED,true);line.setLetterSpacing(.21f);
        name.addView(wordmark);add(name,line,4);
        header.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        TextView plus=text("+",25,WHITE,false);plus.setGravity(Gravity.CENTER);
        plus.setBackground(touchBackground(outlined(SURFACE,15,BORDER)));
        plus.setContentDescription("Create new script");
        header.addView(plus,new LinearLayout.LayoutParams(dp(45),dp(45)));
        plus.setOnClickListener(v->newScript());
        screen.addView(header,new LinearLayout.LayoutParams(-1,-2));

        ScrollView scroller=new ScrollView(this);
        scroller.setFillViewport(true);scroller.setClipToPadding(false);
        scroller.setVerticalScrollBarEnabled(false);
        content=vertical();
        pad(content,20,8,20,32);
        scroller.addView(content);
        screen.addView(scroller,new LinearLayout.LayoutParams(-1,0,1));

        navBar=horizontal();
        navBar.setBackground(outlined(0xff111521,22,0xff282b3c));
        pad(navBar,8,8,8,8);
        LinearLayout.LayoutParams navMargin=new LinearLayout.LayoutParams(-1,dp(64));
        navMargin.setMargins(dp(16),0,dp(16),dp(12));
        screen.addView(navBar,navMargin);
        renderNav();
        renderTab();
    }
    private void renderNav() {
        if(navBar==null)return;
        navBar.removeAllViews();
        String[] labels={"◈   Studio","▤   Scripts","◐   Style"};
        for(int i=0;i<labels.length;i++) {
            final int index=i;
            TextView tabButton=text(labels[i],12,i==tab?WHITE:MUTED,true);
            tabButton.setGravity(Gravity.CENTER);
            tabButton.setBackground(touchBackground(rounded(i==tab?SURFACE_HIGH:0xff111521,16)));
            tabButton.setOnClickListener(v->{ tab=index; renderTab(); renderNav(); });
            tabButton.setContentDescription(labels[i]);
            navBar.addView(tabButton,new LinearLayout.LayoutParams(0,-1,1));
        }
    }
    private void renderTab() {
        if(content==null)return;
        content.removeAllViews(); permissionText=null;stylePreview=null;previewGuide=null;
        if(tab==0)renderHome();
        else if(tab==1)renderLibrary();
        else renderStyle();
        if(android.animation.ValueAnimator.areAnimatorsEnabled()){
            content.setAlpha(0.0f);content.setTranslationY(dp(8));
            content.animate().alpha(1f).translationY(0f).setDuration(230).start();
        }
    }

    private void renderHome() {
        TextView eyebrow=caption("YOUR WORDS  /  YOUR SCREEN  /  YOUR FLOW");
        eyebrow.setTextColor(PURPLE);
        add(content,eyebrow,13);
        TextView heading=text("Every word.\nRight on cue.",35,WHITE,true);
        heading.setLineSpacing(dp(1),1f);
        add(content,heading,13);
        TextView description=text("A beautiful way to speak on camera without losing your place.",15,MUTED,false);
        description.setLineSpacing(dp(3),1f);
        add(content,description,12);
        StudioGraphics illustration=new StudioGraphics(this);
        LinearLayout.LayoutParams artLp=new LinearLayout.LayoutParams(-1,dp(218));
        artLp.topMargin=dp(21);
        illustration.setContentDescription("Illustration of a floating script over a smartphone camera");
        content.addView(illustration,artLp);
        TextView launch=button("▶    Launch floating reader",true,v->startOverlay());
        add(content,launch,18);
        LinearLayout permission=horizontal();
        permission.setBackground(rounded(0xff161b28,14));pad(permission,13,13,12,13);
        permissionText=text("",12,MUTED,false);
        permission.addView(permissionText,new LinearLayout.LayoutParams(0,-2,1));
        permission.addView(tinyAction("Settings  ›",v->requestOverlayPermission()));
        add(content,permission,12);
        updatePermission();
        LinearLayout quick=horizontal();
        TextView create=button("＋  New script",false,v->newScript());
        LinearLayout.LayoutParams q1=new LinearLayout.LayoutParams(0,dp(50),1);
        q1.rightMargin=dp(7);
        quick.addView(create,q1);
        TextView stop=button("■  Stop overlay",false,v->{
            stopService(new Intent(this,OverlayService.class));toast("Floating window closed");
            renderTab();
        });
        quick.addView(stop,new LinearLayout.LayoutParams(0,dp(50),1));
        add(content,quick,10);

        sectionHeading(content,"UP NEXT","All scripts  ›",v->{tab=1;renderTab();renderNav();});
        LinearLayout current=vertical();pad(current,17,18,17,18);
        current.setBackground(outlined(SURFACE,21,BORDER));
        LinearLayout top=horizontal();
        addWeighted(top,pill("●  SELECTED SCRIPT",MINT,0xff163d35),1);
        top.addView(tinyAction("Edit  ↗",v->openEditor()));
        current.addView(top);
        TextView title=text(selected.title,20,WHITE,true);
        add(current,title,16);
        String preview=selected.body.trim();
        if(preview.isEmpty()) preview="Write something meaningful and it will appear here.";
        if(preview.length()>155)preview=preview.substring(0,155)+"…";
        TextView excerpt=text(preview,14,MUTED,false);
        excerpt.setMaxLines(3);excerpt.setLineSpacing(dp(3),1f);
        add(current,excerpt,8);
        View divider=new View(this);divider.setBackgroundColor(0xff37394c);
        LinearLayout.LayoutParams dividerLp=new LinearLayout.LayoutParams(-1,dp(1));
        dividerLp.topMargin=dp(17);current.addView(divider,dividerLp);
        LinearLayout metadata=horizontal();
        TextView count=text(wordCount(selected.body)+" words  •  Saved on this device",12,MUTED,false);
        metadata.addView(count,new LinearLayout.LayoutParams(0,-2,1));
        add(current,metadata,15);
        add(content,current,7);

        sectionHeading(content,"CREATOR FLOW",null,null);
        LinearLayout steps=horizontal();
        String[] titles={"01  WRITE","02  FLOAT","03  RECORD"};
        for(int i=0;i<3;i++){
            LinearLayout step=vertical();
            step.setBackground(rounded(i==1?0xff28223d:SURFACE,15));
            pad(step,10,14,10,14);
            TextView t=text(titles[i],10,i==1?0xffc6b0ff:MUTED,true);
            t.setGravity(Gravity.CENTER);step.addView(t);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);
            if(i<2)lp.rightMargin=dp(7);
            steps.addView(step,lp);
        }
        add(content,steps,7);
    }

    private void renderLibrary() {
        add(content,caption("THE LIBRARY"),18);
        add(content,text("Your words,\nbeautifully organized.",31,WHITE,true),12);
        add(content,text("Private scripts saved on your phone. Tap any script to edit.",14,MUTED,false),12);
        add(content,button("+   Create new script",true,v->newScript()),20);
        LinearLayout backupActions=horizontal();
        TextView backup=button("↓  Back up scripts",false,v->exportBackup());
        TextView restore=button("↑  Restore backup",false,v->importBackup());
        LinearLayout.LayoutParams back1=new LinearLayout.LayoutParams(0,dp(50),1);
        back1.rightMargin=dp(8);backupActions.addView(backup,back1);
        backupActions.addView(restore,new LinearLayout.LayoutParams(0,dp(50),1));
        add(content,backupActions,9);
        List<ScriptStore.Script> scripts=ScriptStore.all(this);
        sectionHeading(content,"ALL SCRIPTS  ·  "+scripts.size(),null,null);
        for(ScriptStore.Script s:scripts){
            boolean active=s.id.equals(selected.id);
            LinearLayout card=vertical();
            pad(card,16,16,16,15);
            card.setBackground(touchBackground(outlined(active?0xff242139:SURFACE,18,
                active?0xff745f9b:BORDER)));
            LinearLayout first=horizontal();
            TextView index=text("▤",22,active?PURPLE:MUTED,false);
            index.setGravity(Gravity.CENTER);
            first.addView(index,new LinearLayout.LayoutParams(dp(36),dp(40)));
            LinearLayout words=vertical();
            pad(words,7,0,0,0);
            TextView title=text(s.title,17,WHITE,true);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            words.addView(title);
            String excerpt=s.body.trim().replace("\n"," ");
            if(excerpt.isEmpty())excerpt="Ready for a fresh idea";
            if(excerpt.length()>70)excerpt=excerpt.substring(0,70)+"…";
            TextView snippet=text(excerpt,12,MUTED,false);
            snippet.setSingleLine(true);snippet.setEllipsize(android.text.TextUtils.TruncateAt.END);
            add(words,snippet,6);
            first.addView(words,new LinearLayout.LayoutParams(0,-2,1));
            TextView arrow=text("↗",21,active?PURPLE:WHITE,false);pad(arrow,9,0,0,0);
            first.addView(arrow);
            card.addView(first);
            LinearLayout bottom=horizontal();
            TextView count=text(wordCount(s.body)+" WORDS  ·  "+
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(s.updatedAt)),10,MUTED,true);
            count.setLetterSpacing(.03f);
            bottom.addView(count,new LinearLayout.LayoutParams(0,-2,1));
            if(active)bottom.addView(pill("ACTIVE",PURPLE,0xff392e50));
            add(card,bottom,18);
            card.setOnClickListener(v->{
                selected=s;
                ScriptStore.select(this,s.id);
                openEditor();
            });
            add(content,card,9);
        }
        TextView reassurance=text("●  Offline by design. Your scripts remain on this device unless you export them.",12,MUTED,false);
        add(content,reassurance,21);
    }

    private void renderStyle() {
        add(content,caption("READING EXPERIENCE"),18);
        add(content,text("Make it feel\nlike you.",33,WHITE,true),12);
        TextView intro=text("Dial in the perfect size, pace and visibility before your next take.",14,MUTED,false);
        add(content,intro,12);

        LinearLayout previewCard=vertical();
        previewCard.setBackground(outlined(SURFACE,21,BORDER));
        pad(previewCard,16,16,16,16);
        previewCard.addView(caption("LIVE PANEL PREVIEW"));
        stylePreview=text("You're ready for your next great take.\n\nKeep your eyes on the camera.",28,WHITE,false);
        stylePreview.setGravity(Gravity.TOP|Gravity.START);
        stylePreview.setLineSpacing(dp(7),1.09f);
        pad(stylePreview,17,22,17,20);
        add(previewCard,stylePreview,14);
        previewGuide = new View(this);
        previewGuide.setBackground(rounded(PURPLE,3));
        LinearLayout.LayoutParams eye = new LinearLayout.LayoutParams(-1,dp(3));
        eye.topMargin = dp(3);
        previewCard.addView(previewGuide,eye);
        add(content,previewCard,22);
        updateStylePreview();

        sectionHeading(content,"CINEMA THEMES",null,null);
        LinearLayout themes=horizontal();
        String[] names={"◼  Midnight","◈  Studio","□  Paper"};
        int[] swatches={0xff28243e,0xff65478e,0xfff3eee4};
        int chosen=ScriptStore.prefs(this).getInt("reader_theme",0);
        for(int index=0;index<3;index++){
            final int item=index;
            TextView choice=text(names[index],12,index==2?0xff232132:WHITE,true);
            choice.setGravity(Gravity.CENTER);
            choice.setBackground(outlined(swatches[index],16,
                index==chosen?PURPLE:BORDER));
            choice.setMinHeight(dp(52));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(52),1);
            if(index<2)cp.rightMargin=dp(7);
            themes.addView(choice,cp);
            choice.setOnClickListener(v->{
                ScriptStore.prefs(this).edit().putInt("reader_theme",item).apply();
                refreshOverlay();renderTab();
            });
        }
        add(content,themes,10);

        sectionHeading(content,"READING TOOLS",null,null);
        toggleSetting("focus_line","EYE-LEVEL FOCUS GUIDE",
            "Keep a gentle line near the camera for better eye contact.",true);
        toggleSetting("countdown","3 · 2 · 1 COUNTDOWN",
            "Get ready before your first line begins scrolling.",true);

        sectionHeading(content,"TEXT DIRECTION",null,null);
        LinearLayout directions=horizontal();
        String[] options={"Auto","Left to right","Right to left"};
        int selectedDirection=ScriptStore.prefs(this).getInt("text_direction",0);
        for(int i=0;i<options.length;i++){
            final int index=i;
            TextView control=text(options[i],12,i==selectedDirection?WHITE:MUTED,true);
            control.setGravity(Gravity.CENTER);
            control.setBackground(outlined(i==selectedDirection?0xff3c3156:SURFACE,13,
                i==selectedDirection?PURPLE:BORDER));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(47),1);
            if(i<2)cp.rightMargin=dp(7);
            directions.addView(control,cp);
            control.setOnClickListener(v->{
                ScriptStore.prefs(this).edit().putInt("text_direction",index).apply();
                refreshOverlay();renderTab();
            });
        }
        add(content,directions,9);

        sectionHeading(content,"TUNING CONTROLS",null,null);
        settingCard("speed","SCROLL SPEED","How quickly your words move",20,250,90," px/s");
        settingCard("font","FONT SIZE","Readable from near or far",18,72,34," sp");
        settingCard("focus_position","FOCUS LINE POSITION","Where the reading guide sits in the window",18,65,33,"%");
        settingCard("opacity","WINDOW OPACITY","Control how much is visible behind the script",20,95,76,"%");
        add(content,button("▶   Test floating reader",true,v->startOverlay()),22);
        TextView hint=text("Changes apply to an active reader. Camera-app compatibility depends on Android and each app's overlay restrictions.",12,MUTED,false);
        add(content,hint,14);
    }

    private void toggleSetting(String key,String heading,String detail,boolean defaultValue) {
        LinearLayout card = horizontal();
        card.setBackground(outlined(SURFACE,17,BORDER));
        pad(card,14,15,14,15);
        LinearLayout labels=vertical();
        TextView title=text(heading,12,WHITE,true);title.setLetterSpacing(.03f);
        labels.addView(title);
        TextView description=text(detail,12,MUTED,false);
        description.setMaxLines(2);
        add(labels,description,6);
        card.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        Switch toggle=new Switch(this);
        toggle.setButtonTintList(null);
        toggle.setChecked(ScriptStore.prefs(this).getBoolean(key,defaultValue));
        toggle.setThumbTintList(ColorStateList.valueOf(0xffc9b6ff));
        toggle.setTrackTintList(ColorStateList.valueOf(PURPLE));
        toggle.setContentDescription(heading);
        toggle.setOnCheckedChangeListener((button,checked)->{
            ScriptStore.prefs(this).edit().putBoolean(key,checked).apply();
            updateStylePreview();refreshOverlay();
        });
        card.addView(toggle,new LinearLayout.LayoutParams(dp(56),dp(50)));
        add(content,card,9);
    }
    private void settingCard(String key,String title,String detail,int min,int max,int def,String unit) {
        LinearLayout card=vertical();
        card.setBackground(outlined(SURFACE,18,BORDER));
        pad(card,16,17,16,13);
        LinearLayout top=horizontal();
        TextView name=text(title,12,WHITE,true);name.setLetterSpacing(.06f);
        top.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        TextView value=text("",13,PURPLE,true);
        top.addView(value);
        card.addView(top);
        add(card,text(detail,12,MUTED,false),7);
        SeekBar seek=new SeekBar(this);
        seek.setProgressTintList(ColorStateList.valueOf(PURPLE));
        seek.setThumbTintList(ColorStateList.valueOf(0xffc8b4ff));
        seek.setProgressBackgroundTintList(ColorStateList.valueOf(BORDER));
        int original=ScriptStore.prefs(this).getInt(key,def);
        seek.setMax(max-min);
        seek.setProgress(Math.max(0,Math.min(max-min,original-min)));
        value.setText(original+unit);
        add(card,seek,12);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar s){}
            public void onStopTrackingTouch(SeekBar s){}
            public void onProgressChanged(SeekBar s,int progress,boolean fromUser){
                int actual=min+progress;
                value.setText(actual+unit);
                if(fromUser){
                    ScriptStore.prefs(MainActivity.this).edit().putInt(key,actual).apply();
                    updateStylePreview();
                    refreshOverlay();
                }
            }
        });
        add(content,card,10);
    }
    private void updateStylePreview(){
        if(stylePreview==null)return;
        int size=ScriptStore.prefs(this).getInt("font",34);
        int opacity=ScriptStore.prefs(this).getInt("opacity",76);
        int theme=ScriptStore.prefs(this).getInt("reader_theme",0);
        int direction=ScriptStore.prefs(this).getInt("text_direction",0);
        stylePreview.setTextSize(Math.max(16,Math.min(33,size)));
        stylePreview.setTextColor(theme==2?0xff1e2030:WHITE);
        stylePreview.setTypeface(theme==2?Typeface.SERIF:Typeface.DEFAULT,Typeface.NORMAL);
        stylePreview.setTextDirection(direction==1?View.TEXT_DIRECTION_LTR:
            direction==2?View.TEXT_DIRECTION_RTL:View.TEXT_DIRECTION_FIRST_STRONG);
        stylePreview.setGravity(Gravity.TOP | (direction==2?Gravity.RIGHT:Gravity.START));
        int color=theme==2?Color.argb((int)(opacity*2.55f),248,243,234):
            theme==1?Color.argb((int)(opacity*2.55f),64,43,99):
            Color.argb((int)(opacity*2.55f),43,37,72);
        stylePreview.setBackground(rounded(color,16));
        if(previewGuide!=null)previewGuide.setVisibility(
            ScriptStore.prefs(this).getBoolean("focus_line",true)?View.VISIBLE:View.GONE);
    }
    private void updatePermission() {
        if(permissionText!=null)permissionText.setText(
            Settings.canDrawOverlays(this)?"●  Overlay access is enabled":"○  Allow display over other apps");
        if(permissionText!=null)permissionText.setTextColor(
            Settings.canDrawOverlays(this)?MINT:MUTED);
    }
    private void requestOverlayPermission() {
        try{
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:"+getPackageName())));
        }catch(Exception e){
            toast("Open Android Settings and allow display over other apps.");
        }
    }

    private void newScript() {
        selected=ScriptStore.create(this);
        openEditor();
    }
    private void openEditor() {
        editorOpen=true;
        screen=vertical();screen.setBackgroundColor(BG);
        setContentView(screen);
        LinearLayout toolbar=horizontal();
        pad(toolbar,18,15,18,14);
        TextView back=text("‹",31,WHITE,false);
        back.setGravity(Gravity.CENTER);
        back.setBackground(touchBackground(rounded(SURFACE,13)));
        back.setContentDescription("Back to studio");
        back.setOnClickListener(v->{ saveEditor(false);editorOpen=false;buildStudio(); });
        toolbar.addView(back,new LinearLayout.LayoutParams(dp(43),dp(43)));
        LinearLayout heading=vertical();pad(heading,14,0,0,0);
        heading.addView(text("Script editor",20,WHITE,true));
        add(heading,text("PRIVATE  ·  SAVED ON DEVICE",10,MUTED,true),5);
        toolbar.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        TextView save=tinyAction("Save  ✓",v->saveEditor(true));
        toolbar.addView(save);
        screen.addView(toolbar);

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout editor=vertical();
        pad(editor,20,12,20,34);
        scroll.addView(editor);
        screen.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        add(editor,caption("SCRIPT TITLE"),16);
        titleInput=new EditText(this);
        titleInput.setSingleLine(true);titleInput.setSelectAllOnFocus(false);
        titleInput.setTextSize(24);titleInput.setTextColor(WHITE);titleInput.setHintTextColor(MUTED);
        titleInput.setBackground(outlined(SURFACE,17,BORDER));
        titleInput.setHint("Give your script a title");
        titleInput.setText(selected.title);
        pad(titleInput,17,18,17,18);
        add(editor,titleInput,9);

        LinearLayout metadata=horizontal();
        TextView wordCountLabel=text(wordCount(selected.body)+" words",12,MUTED,true);
        metadata.addView(wordCountLabel,new LinearLayout.LayoutParams(0,-2,1));
        metadata.addView(pill("AUTO LOCAL",MINT,0xff173a31));
        add(editor,metadata,19);
        add(editor,caption("YOUR WORDS"),17);

        scriptInput=new EditText(this);
        scriptInput.setGravity(Gravity.TOP|Gravity.START);
        scriptInput.setInputType(InputType.TYPE_CLASS_TEXT |
            InputType.TYPE_TEXT_FLAG_MULTI_LINE |
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        scriptInput.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        scriptInput.setTextColor(WHITE);scriptInput.setHintTextColor(MUTED);
        scriptInput.setTextSize(19);scriptInput.setLineSpacing(dp(7),1.1f);
        scriptInput.setMinLines(12);
        scriptInput.setHint("The next great story starts here…");
        scriptInput.setBackground(outlined(SURFACE,18,BORDER));
        pad(scriptInput,18,20,18,20);
        scriptInput.setText(selected.body);
        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int after){}
            public void onTextChanged(CharSequence s,int st,int before,int count){
                if (scriptInput != null)
                    wordCountLabel.setText(wordCount(scriptInput.getText().toString())+" words");
                scheduleAutosave();
            }
            public void afterTextChanged(android.text.Editable e){}
        };
        scriptInput.addTextChangedListener(watcher);
        titleInput.addTextChangedListener(watcher);
        add(editor,scriptInput,10);

        LinearLayout tools=horizontal();
        TextView paste=button("▤   Paste",false,v->paste());
        TextView load=button("↓   Import TXT",false,v->importText());
        LinearLayout.LayoutParams half=new LinearLayout.LayoutParams(0,dp(50),1);
        half.rightMargin=dp(6);tools.addView(paste,half);
        tools.addView(load,new LinearLayout.LayoutParams(0,dp(50),1));
        add(editor,tools,13);
        add(editor,button("↗  Export this script as TXT",false,v->exportScript()),9);

        TextView danger=tinyAction("Delete this script",v->{
            new AlertDialog.Builder(this)
                .setTitle("Delete script?")
                .setMessage("This removes the script from this device.")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Delete",(dialog,which)->{
                    ScriptStore.delete(this,selected.id);
                    selected=ScriptStore.active(this);
                    editorOpen=false;
                    tab=1;buildStudio();
                }).show();
        });
        danger.setTextColor(0xffff9ba9);
        add(editor,danger,16);

        LinearLayout bottom=horizontal();
        bottom.setBackgroundColor(BG);pad(bottom,20,8,20,16);
        TextView finish=button("✓   Save",false,v->saveEditor(true));
        TextView launch=button("▶   Go live with script",true,v->{saveEditor(false);startOverlay();});
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(53),.7f);
        p.rightMargin=dp(8);bottom.addView(finish,p);
        bottom.addView(launch,new LinearLayout.LayoutParams(0,dp(53),1.3f));
        screen.addView(bottom);
    }

    private void scheduleAutosave() {
        dirty = true;
        autosaveHandler.removeCallbacks(autosaveTask);
        if (editorOpen) autosaveHandler.postDelayed(autosaveTask, 850);
    }
    private void saveEditor(boolean notify) {
        autosaveHandler.removeCallbacks(autosaveTask);
        if(!editorOpen || titleInput==null || scriptInput==null)return;
        if (!dirty && !notify) return;
        selected.title=titleInput.getText().toString().trim();
        if(selected.title.isEmpty())selected.title="Untitled script";
        selected.body=scriptInput.getText().toString();
        ScriptStore.save(this,selected);
        dirty = false;
        refreshOverlay();
        if(notify)toast("Saved on your device");
    }
    private static int wordCount(String s) {
        if(s==null || s.trim().isEmpty())return 0;
        return s.trim().split("\\s+").length;
    }
    private void paste(){
        if(scriptInput==null)return;
        ClipboardManager manager=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if(manager==null || !manager.hasPrimaryClip()){toast("Clipboard is empty");return;}
        ClipData clip=manager.getPrimaryClip();
        if(clip==null || clip.getItemCount()==0)return;
        CharSequence value=clip.getItemAt(0).coerceToText(this);
        if(value==null)return;
        int cursor=Math.max(0,scriptInput.getSelectionStart());
        scriptInput.getText().insert(cursor,value);
    }
    private void importText() {
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        startActivityForResult(intent,DOC_REQUEST);
    }
    private void exportScript() {
        if (editorOpen) saveEditor(false);
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        String fileName = selected.title.replaceAll("[^a-zA-Z0-9_-]+", "_");
        if (fileName.isEmpty()) fileName = "FloatCue_Script";
        intent.putExtra(Intent.EXTRA_TITLE, fileName + ".txt");
        startActivityForResult(intent, EXPORT_TXT);
    }
    private void exportBackup() {
        if (editorOpen) saveEditor(false);
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "FloatCue-Library-Backup.json");
        startActivityForResult(intent, EXPORT_BACKUP);
    }
    private void importBackup() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json","text/plain"});
        startActivityForResult(intent, RESTORE_BACKUP);
    }
    private String readFile(Uri uri,int limit) throws Exception {
        try(InputStream in=getContentResolver().openInputStream(uri);
            ByteArrayOutputStream out=new ByteArrayOutputStream()){
            if(in==null)throw new IllegalArgumentException("Unable to open document");
            byte[] chunk=new byte[8192];int size;
            while((size=in.read(chunk))!=-1){
                if(out.size()+size>limit)throw new IllegalArgumentException("Document too large");
                out.write(chunk,0,size);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
    private void writeFile(Uri uri,String value) throws Exception {
        try(OutputStream out=getContentResolver().openOutputStream(uri,"w")){
            if(out==null)throw new IllegalArgumentException("Unable to write document");
            out.write(value.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK || data==null || data.getData()==null)return;
        final Uri uri=data.getData();
        if(request==EXPORT_TXT || request==EXPORT_BACKUP){
            try {
                String payload = request==EXPORT_TXT ? selected.body : ScriptStore.exportJson(this);
                writeFile(uri,payload);
                toast(request==EXPORT_TXT ? "Script exported to TXT" : "Library backup created");
            } catch(Exception exception){toast("Export failed. Please choose another folder.");}
            return;
        }
        if(request==DOC_REQUEST){
            if(!editorOpen)openEditor();
            try{
                scriptInput.setText(readFile(uri,1024*1024));
                scheduleAutosave();
                toast("Text imported and will be saved");
            }catch(Exception exception){toast("Import failed. Use a TXT file under 1 MB.");}
            return;
        }
        if(request==RESTORE_BACKUP){
            try {
                final String backup=readFile(uri,2*1024*1024);
                // Explicit user consent; keep a previous local snapshot on restore.
                new AlertDialog.Builder(this)
                    .setTitle("Restore FloatCue library?")
                    .setMessage("This replaces your current scripts. Export a backup first if you want to keep them.")
                    .setNegativeButton("Cancel",null)
                    .setPositiveButton("Restore",(dialog,which)->{
                        try{
                            int count=ScriptStore.restoreJson(this,backup);
                            selected=ScriptStore.active(this);
                            dirty=false;
                            editorOpen=false;
                            tab=1;
                            buildStudio();
                            refreshOverlay();
                            toast(count+" scripts restored");
                        }catch(Exception invalid){
                            toast("Restore failed: invalid FloatCue backup");
                        }
                    }).show();
            }catch(Exception exception){toast("Could not read backup. Limit is 2 MB.");}
        }
    }
    private void handleIncoming(Intent intent){
        if(intent==null)return;
        CharSequence incoming=null;
        if(Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType()))
            incoming=intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        if(Intent.ACTION_PROCESS_TEXT.equals(intent.getAction()))
            incoming=intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        if(incoming!=null){
            if(editorOpen)saveEditor(false);
            selected=ScriptStore.create(this);
            selected.title="Imported script";
            selected.body=incoming.toString();
            ScriptStore.save(this,selected);
            openEditor();
            intent.setAction(null);
            toast("Imported from another app");
        }
    }
    private void refreshOverlay(){
        if(!OverlayService.isRunning)return;
        Intent intent=new Intent(this,OverlayService.class);
        intent.setAction(OverlayService.ACTION_REFRESH);
        startService(intent);
    }
    private void startOverlay(){
        if(editorOpen)saveEditor(false);
        if(!Settings.canDrawOverlays(this)){
            toast("Allow FloatCue to display over other apps.");
            requestOverlayPermission();return;
        }
        if(Build.VERSION.SDK_INT>=33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},42);
        }
        Intent i=new Intent(this,OverlayService.class);
        i.setAction(OverlayService.ACTION_SHOW);
        try{
            startForegroundService(i);
            toast("FloatCue ready — open your camera!");
        }catch(Exception ex){
            toast("Could not start overlay: "+ex.getClass().getSimpleName());
        }
    }
    private void toast(String text){Toast.makeText(this,text,Toast.LENGTH_SHORT).show();}
}
