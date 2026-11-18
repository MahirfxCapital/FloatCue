package com.hisabtech.floatcue;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** Decorative native Canvas illustration: no external assets or network required. */
public final class StudioGraphics extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    public StudioGraphics(Context context) { super(context); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = (int) (218 * getResources().getDisplayMetrics().density + .5f);
        setMeasuredDimension(width, height);
    }
    private void fill(Canvas c, float x0,float y0,float x1,float y1,float radius,int color) {
        p.reset(); p.setAntiAlias(true); p.setColor(color);
        c.drawRoundRect(new RectF(x0,y0,x1,y1),radius,radius,p);
    }
    private void stroke(Canvas c,float x0,float y0,float x1,float y1,float r,int color,float width) {
        p.reset();p.setAntiAlias(true);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);
        c.drawRoundRect(new RectF(x0,y0,x1,y1),r,r,p);
        p.setStyle(Paint.Style.FILL);
    }
    private void line(Canvas c,float x0,float y0,float x1,float y1,int color,float width) {
        p.reset();p.setAntiAlias(true);p.setColor(color);p.setStrokeWidth(width);
        p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x0,y0,x1,y1,p);
    }
    private void label(Canvas c,String value,float x,float baseline,int sp,int color,boolean bold) {
        p.reset();p.setAntiAlias(true);p.setColor(color);p.setTextSize(sp);
        p.setTypeface(bold?android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD):
            android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL));
        c.drawText(value,x,baseline,p);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        float sx = getWidth() / 380f, sy = getHeight()/218f;
        canvas.scale(sx,sy);
        p.reset();p.setAntiAlias(true);
        p.setShader(new LinearGradient(0,0,380,218,
            new int[]{0xff28233d,0xff171b30,0xff151927},null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(new RectF(0,0,380,218),28,28,p);
        p.setShader(null);
        p.setShader(new RadialGradient(226,66,158,new int[]{0x665b3cc0,0x005b3cc0},
            new float[]{0f,1f},Shader.TileMode.CLAMP));
        canvas.drawCircle(226,66,158,p);p.setShader(null);
        p.setColor(0x668488a4);
        for(int x=20;x<380;x+=20) for(int y=18;y<218;y+=20) {
            p.setAlpha(27);canvas.drawCircle(x,y,.65f,p);
        }
        // Abstract preview of the camera.
        fill(canvas,28,26,163,202,23,0xff0c101a);
        stroke(canvas,28,26,163,202,23,0xff44475a,1.4f);
        fill(canvas,66,32,126,37,3,0xff303343);
        p.setColor(0xff3d3c59);canvas.drawCircle(96,111,45,p);
        p.setColor(0xff5e4c81);canvas.drawCircle(96,97,16,p);
        p.setColor(0xff9587b6);canvas.drawOval(new RectF(67,114,125,167),p);
        line(canvas,48,59,59,59,0xff9ca0bb,1.5f);
        line(canvas,48,59,48,70,0xff9ca0bb,1.5f);
        line(canvas,132,59,143,59,0xff9ca0bb,1.5f);
        line(canvas,143,59,143,70,0xff9ca0bb,1.5f);
        p.setColor(0xfff95b78);canvas.drawCircle(96,185,7,p);
        stroke(canvas,87,176,105,194,10,0xffed8b9b,1);
        // Floating translucent script panel foreground.
        p.setShader(new LinearGradient(138,46,344,180,0xee453276,0xee191d37,Shader.TileMode.CLAMP));
        canvas.drawRoundRect(new RectF(134,42,351,174),18,18,p);
        p.setShader(null);
        stroke(canvas,134,42,351,174,18,0xff7364a1,1.4f);
        fill(canvas,147,54,338,74,10,0x66392c68);
        label(canvas,"FLOATCUE",159,68,9,0xffe8ddff,true);
        fill(canvas,314,61,8+314,69,4,0xffb1ffce);
        label(canvas,"Speak with",155,103,16,0xfff7f4ff,true);
        label(canvas,"confidence.",155,126,16,0xfff7f4ff,true);
        line(canvas,156,138,318,138,0xff776e92,2.8f);
        line(canvas,156,147,271,147,0xff655f82,2.8f);
        fill(canvas,146,155,337,166,5,0xff56467f);
        label(canvas,"▶",155,164,8,0xfff5edff,true);
        fill(canvas,171,160,274,162,1,0xff9788ca);
        fill(canvas,171,160,219,162,1,0xffe3d4ff);
        fill(canvas,309,158,318,162,2,0xffc5b7ea);
        // Presentation sparkle.
        label(canvas,"✦",330,38,21,0xffc8b7ff,true);
        canvas.restore();
    }
}
