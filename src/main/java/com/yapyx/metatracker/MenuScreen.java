package com.yapyx.metatracker;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Hauptmenue (Rechts-Shift). */
public class MenuScreen extends Screen {

    public MenuScreen() {
        super(Text.literal("MetaTracker"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 40;

        addDrawableChild(ButtonWidget.builder(Text.literal("Meta Tracker"),
                b -> this.client.setScreen(new MetaScreen(this)))
                .dimensions(cx - 75, y, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Resource Packs"),
                b -> this.client.setScreen(new PackScreen(this)))
                .dimensions(cx - 75, y + 26, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Schliessen"),
                b -> this.close())
                .dimensions(cx - 75, y + 60, 150, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        Ui.center(context, this.textRenderer, "yapyX Client", this.width / 2, this.height / 2 - 70, 0xFFFFFFFF);
    }
}
