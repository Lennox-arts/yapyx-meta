package com.yapyx.metatracker;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Pack-Name + Item waehlen, dann in den Editor. */
public class PackScreen extends Screen {

    private static String nameText = "yapyX-Pack";
    private static String itemText = "diamond_sword";

    private final Screen parent;
    private String status = "";

    public PackScreen(Screen parent) {
        super(Text.literal("Resource Packs"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        TextFieldWidget nameField = new TextFieldWidget(this.textRenderer, cx - 100, 50, 200, 20, Text.literal("Name"));
        nameField.setMaxLength(32);
        nameField.setText(nameText);
        nameField.setChangedListener(s -> nameText = s);
        addDrawableChild(nameField);

        TextFieldWidget itemField = new TextFieldWidget(this.textRenderer, cx - 100, 90, 200, 20, Text.literal("Item"));
        itemField.setMaxLength(64);
        itemField.setText(itemText);
        itemField.setChangedListener(s -> itemText = s);
        addDrawableChild(itemField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Editor oeffnen"), b -> openEditor())
                .dimensions(cx - 100, 120, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Packs neu laden"), b -> this.client.reloadResources())
                .dimensions(cx - 100, 146, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Zurueck"), b -> this.close())
                .dimensions(cx - 50, this.height - 28, 100, 20).build());
    }

    private void openEditor() {
        String name = nameText.trim().replaceAll("[^A-Za-z0-9_-]", "_");
        if (name.isEmpty()) {
            status = "Bitte einen Pack-Namen eingeben.";
            return;
        }
        Identifier id = Identifier.tryParse(itemText.trim().toLowerCase());
        if (id == null || !Registries.ITEM.containsId(id)) {
            status = "Dieses Item gibt es nicht.";
            return;
        }
        this.client.setScreen(new EditorScreen(this, name, id));
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);
        int cx = this.width / 2;
        Ui.center(c, this.textRenderer, "Resource Packs", cx, 10, 0xFFFFFFFF);
        Ui.text(c, this.textRenderer, "Pack-Name", cx - 100, 39, 0xFFAAAAAA);
        Ui.text(c, this.textRenderer, "Item (z.B. diamond_sword)", cx - 100, 79, 0xFFAAAAAA);
        if (!status.isEmpty()) Ui.center(c, this.textRenderer, status, cx, 176, 0xFFFF5555);
        Ui.center(c, this.textRenderer, "Pack einmal unter Optionen > Ressourcenpakete aktivieren,", cx, 196, 0xFFAAAAAA);
        Ui.center(c, this.textRenderer, "danach reicht \"Packs neu laden\".", cx, 208, 0xFFAAAAAA);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
