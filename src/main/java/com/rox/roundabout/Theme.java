package com.rox.roundabout;

import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontEdging;
import io.github.humbleui.skija.FontHinting;
import io.github.humbleui.skija.FontMgr;
import io.github.humbleui.skija.FontStyle;
import io.github.humbleui.skija.Typeface;

/** Colour tokens and fonts. Colours are ARGB. */
public final class Theme {
    public final int background;
    public final int panel;
    public final int panelRaised;
    public final int border;
    public final int text;
    public final int textDim;
    public final int textFaint;
    public final int accent;
    public final int good;
    public final int warn;
    public final int hot;

    public final Font ui;
    public final Font uiBold;
    public final Font uiSmall;
    public final Font mono;
    public final Font monoLarge;

    private Theme(int background, int panel, int panelRaised, int border, int text, int textDim, int textFaint,
                  int accent, int good, int warn, int hot) {
        this.background = background;
        this.panel = panel;
        this.panelRaised = panelRaised;
        this.border = border;
        this.text = text;
        this.textDim = textDim;
        this.textFaint = textFaint;
        this.accent = accent;
        this.good = good;
        this.warn = warn;
        this.hot = hot;

        FontMgr fonts = FontMgr.getDefault();
        Typeface sans = fonts.matchFamiliesStyle(
                new String[]{"Inter", "Adwaita Sans", "Noto Sans", "Cantarell", "sans-serif"}, FontStyle.NORMAL);
        Typeface sansBold = fonts.matchFamiliesStyle(
                new String[]{"Inter", "Adwaita Sans", "Noto Sans", "Cantarell", "sans-serif"}, FontStyle.BOLD);
        Typeface monoFace = fonts.matchFamiliesStyle(
                new String[]{"JetBrains Mono", "Adwaita Mono", "Noto Sans Mono", "monospace"}, FontStyle.NORMAL);
        ui = font(sans, 13);
        uiBold = font(sansBold, 13);
        uiSmall = font(sans, 11);
        mono = font(monoFace, 13);
        monoLarge = font(monoFace, 18);
    }

    private static Font font(Typeface face, float size) {
        return new Font(face, size)
                .setSubpixel(true)
                .setEdging(FontEdging.SUBPIXEL_ANTI_ALIAS)
                .setHinting(FontHinting.SLIGHT);
    }

    public static Theme dark() {
        return new Theme(
                0xFF0D0F13, // background
                0xFF14171D, // panel
                0xFF1B1F27, // panelRaised
                0xFF262B35, // border
                0xFFD8DEE9, // text
                0xFF7D8696, // textDim
                0xFF3B424F, // textFaint
                0xFF5AA9FF, // accent
                0xFF6FDC9C, // good
                0xFFFFB454, // warn
                0xFFFF6A4D  // hot
        );
    }

    /** Linear blend between two ARGB colours. */
    public static int mix(int from, int to, float t) {
        t = Math.clamp(t, 0f, 1f);
        int a = lerp((from >>> 24) & 0xFF, (to >>> 24) & 0xFF, t);
        int r = lerp((from >>> 16) & 0xFF, (to >>> 16) & 0xFF, t);
        int g = lerp((from >>> 8) & 0xFF, (to >>> 8) & 0xFF, t);
        int b = lerp(from & 0xFF, to & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int withAlpha(int colour, float alpha) {
        return ((int) (Math.clamp(alpha, 0f, 1f) * 255) << 24) | (colour & 0xFFFFFF);
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    /** Heat ramp: 0 = cold (transparent), 1 = just written. */
    public int heat(float heat) {
        if (heat < 0.5f) {
            return mix(withAlpha(warn, 0f), withAlpha(warn, 0.32f), heat * 2);
        }
        return mix(withAlpha(warn, 0.32f), withAlpha(hot, 0.62f), (heat - 0.5f) * 2);
    }
}
