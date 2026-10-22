package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * The moonlit landscape from the user's reference: teal night sky, glowing moon with a thin
 * horizontal flare, cloud wisps, dark hills with a winding path, and tree silhouettes on the left.
 *
 * Everything is drawn with Canvas primitives at the view's real resolution, so it stays sharp
 * on 1080p and 4K panels alike — no image assets, no scaling blur.
 */
public class LandscapeView extends View {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sky = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint streak = new Paint(Paint.ANTI_ALIAS_FLAG);

    public LandscapeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;

        // ---- sky: deep navy melting into bright teal near the horizon ----
        sky.setShader(new LinearGradient(0, 0, w * 0.18f, h,
                new int[]{0xFF123049, 0xFF1B7079, 0xFF2DB599}, new float[]{0f, 0.52f, 0.84f},
                Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, sky);

        // The reference photo keeps the left side moody: a soft dark falloff from the left edge.
        sky.setShader(new LinearGradient(0, 0, w * 0.48f, 0,
                0x52123049, 0x00123049, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, sky);

        // ---- moon ----
        float mx = w * 0.70f, my = h * 0.36f, mr = h * 0.112f;
        glow.setShader(new RadialGradient(mx, my, mr * 3.4f,
                new int[]{0x8CFBF1BC, 0x30FAEEB2, 0x00000000}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, glow);
        fill.setColor(0xFFFCF4CD);
        c.drawCircle(mx, my, mr, fill);

        // Thin flare passing the moon's upper third, and a wide soft one under it.
        streak.setColor(0x1AFFFFFF);
        c.drawRect(0, my - h * 0.038f, w, my - h * 0.030f, streak);
        streak.setColor(0x0EFFFFFF);
        c.drawRect(0, my + h * 0.016f, w, my + h * 0.026f, streak);

        // ---- cloud wisps: thin darker streaks left and right ----
        fill.setColor(0x380E4B5A);
        cloud(c, w * 0.05f, w * 0.48f, h * 0.470f, h * 0.016f);
        cloud(c, w * 0.08f, w * 0.36f, h * 0.555f, h * 0.013f);
        cloud(c, w * 0.58f, w * 0.98f, h * 0.515f, h * 0.014f);
        fill.setColor(0x260E4B5A);
        cloud(c, w * 0.20f, w * 0.54f, h * 0.620f, h * 0.011f);

        // ---- hills ----
        fill.setColor(0xFF2AA38F);   // far hills, lighter teal
        c.drawRect(0, h * 0.760f, w, h, fill);
        fill.setColor(0xFF1E6E78);   // mid hills
        hill(c, w, h, h * 0.800f, w * 0.32f, h * 0.745f, w * 0.68f, h * 0.800f);
        fill.setColor(0xFF14405C);   // foreground ground
        hill(c, w, h, h * 0.860f, w * 0.45f, h * 0.830f, w * 0.90f, h * 0.895f);

        // ---- winding path on the foreground ground ----
        fill.setColor(0xFF2F7089);
        Path trail = new Path();
        trail.moveTo(w * 0.505f, h);
        trail.quadTo(w * 0.460f, h * 0.930f, w * 0.520f, h * 0.885f);
        trail.quadTo(w * 0.560f, h * 0.858f, w * 0.545f, h * 0.845f);
        trail.lineTo(w * 0.575f, h * 0.848f);
        trail.quadTo(w * 0.575f, h * 0.872f, w * 0.545f, h * 0.905f);
        trail.quadTo(w * 0.540f, h * 0.960f, w * 0.585f, h);
        trail.close();
        c.drawPath(trail, fill);

        // ---- tree silhouettes, left edge ----
        fill.setColor(0xFF0D2F45);
        tree(c, w, h, w * 0.055f, w * 0.028f, w * 0.095f, true);   // thick trunk
        tree(c, w, h, w * 0.140f, w * 0.016f, w * 0.165f, false);  // thin trunk
    }

    /** A soft cloud: one flattened ellipse. */
    private void cloud(Canvas c, float x0, float x1, float y, float thick) {
        c.drawOval(x0, y - thick, x1, y + thick, fill);
    }

    /** A hill: rises from (0, baseY) over the crest and settles back down by the far edge. */
    private void hill(Canvas c, float w, float h, float baseY, float cx, float topY, float endX, float endY) {
        Path p = new Path();
        p.moveTo(0, baseY);
        p.quadTo(cx, topY, endX, endY);
        p.lineTo(w, h);
        p.lineTo(0, h);
        p.close();
        c.drawPath(p, fill);
    }

    /** A leaning bare trunk: a tapered quad from the bottom edge to past the top, with branch stubs. */
    private void tree(Canvas c, float w, float h, float baseX, float baseHalf, float topX, boolean big) {
        Path t = new Path();
        t.moveTo(baseX - baseHalf, h);
        t.quadTo((baseX + topX) / 2 - baseHalf * 0.55f, h * 0.5f, topX - baseHalf * 0.35f, -h * 0.06f);
        t.lineTo(topX + baseHalf * 0.35f, -h * 0.06f);
        t.quadTo((baseX + topX) / 2 + baseHalf * 0.55f, h * 0.5f, baseX + baseHalf, h);
        t.close();
        c.drawPath(t, fill);

        // Branch stubs: short tapered spikes off the trunk, pointing up and away.
        float lean = topX - baseX;
        branch(c, w, h, baseX + lean * 0.42f, h * (big ? 0.34f : 0.58f),
                w * (big ? 0.055f : 0.038f), h * (big ? 0.16f : 0.10f));
        branch(c, w, h, baseX + lean * 0.18f, h * (big ? 0.52f : 0.72f),
                -w * (big ? 0.042f : 0.030f), h * (big ? 0.12f : 0.085f));
    }

    /** One branch: a thin triangle from a trunk point out to a tip. */
    private void branch(Canvas c, float w, float h, float ox, float oy, float dx, float dy) {
        Path b = new Path();
        b.moveTo(ox - w * 0.004f, oy);
        b.lineTo(ox + w * 0.006f, oy);
        b.lineTo(ox + dx, oy - dy);
        b.close();
        c.drawPath(b, fill);
    }
}
