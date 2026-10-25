package com.yapyx.metatracker;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.*;

/** Zeigt die Metas (haeufigste Items) pro Server. Mit < > zwischen Servern wechseln. */
public class MetaScreen extends Screen {

    private final Screen parent;
    private final List<String> servers;
    private int index = 0;

    public MetaScreen(Screen parent) {
        super(Text.literal("Meta Tracker"));
        this.parent = parent;
        this.servers = new ArrayList<>(MetaTracker.data.keySet());
        Collections.sort(this.servers);
        int cur = this.servers.indexOf(MetaTracker.lastServer);
        if (cur >= 0) this.index = cur;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
            if (!servers.isEmpty()) index = (index - 1 + servers.size()) % servers.size();
        }).dimensions(cx - 140, 28, 20, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            if (!servers.isEmpty()) index = (index + 1) % servers.size();
        }).dimensions(cx + 120, 28, 20, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Zurueck"), b -> this.close())
                .dimensions(cx - 50, this.height - 28, 100, 20).build());
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);
        int cx = this.width / 2;

        Ui.center(c, this.textRenderer, "Meta Tracker", cx, 10, 0xFFFFFFFF);

        if (servers.isEmpty()) {
            Ui.center(c, this.textRenderer, "Noch keine Daten.", cx, 70, 0xFFAAAAAA);
            Ui.center(c, this.textRenderer, "Joine einem Server, andere Spieler werden automatisch erfasst.",
                    cx, 84, 0xFFAAAAAA);
            return;
        }

        String server = servers.get(index);
        Ui.center(c, this.textRenderer, server + "  (" + (index + 1) + "/" + servers.size() + ")", cx, 34, 0xFFFFFF55);

        Map<String, Integer> items = MetaTracker.data.get(server);
        if (items == null) return;

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(items.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        int rows = Math.min(10, Math.max(1, (this.height - 110) / 14));
        int max = sorted.isEmpty() ? 1 : sorted.get(0).getValue();
        int y = 62;
        for (int i = 0; i < Math.min(rows, sorted.size()); i++) {
            var e = sorted.get(i);
            Ui.text(c, this.textRenderer, e.getKey().replace('_', ' '), cx - 140, y, 0xFFFFFFFF);
            int barW = Math.max(2, 90 * e.getValue() / max);
            c.fill(cx + 10, y, cx + 10 + barW, y + 9, 0xFF4488FF);
            Ui.text(c, this.textRenderer, String.valueOf(e.getValue()), cx + 16 + barW, y, 0xFFFFFFFF);
            y += 14;
        }
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
