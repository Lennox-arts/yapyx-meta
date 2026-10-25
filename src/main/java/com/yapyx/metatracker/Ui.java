package com.yapyx.metatracker;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Kleine Hilfe zum Zeichnen von Text. Farben immer mit 0xFF vorne (ARGB). */
final class Ui {
    private Ui() {}

    static void text(DrawContext c, TextRenderer tr, String s, int x, int y, int color) {
        c.drawTextWithShadow(tr, Text.literal(s), x, y, color);
    }

    static void center(DrawContext c, TextRenderer tr, String s, int cx, int y, int color) {
        c.drawTextWithShadow(tr, Text.literal(s), cx - tr.getWidth(s) / 2, y, color);
    }
}
