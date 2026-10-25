package com.yapyx.metatracker;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** 16x16 Pixel-Editor fuer eine Item-Textur. Speichert direkt in resourcepacks/<Pack>/. */
public class EditorScreen extends Screen {

    private static final int[] PALETTE = {
            0xFF000000, 0xFFFFFFFF, 0xFF9D9D97, 0xFF474F52,
            0xFFB02E26, 0xFFF9801D, 0xFFFED83D, 0xFF80C71F,
            0xFF5E7C16, 0xFF169C9C, 0xFF3AB3DA, 0xFF3C44AA,
            0xFF8932B8, 0xFFC74EBD, 0xFF835432, 0xFFF38BAA
    };

    private final Screen parent;
    private final String packName;
    private final Identifier itemId;
    private final int[] px = new int[256]; // ARGB, 0 = durchsichtig

    private int selected = PALETTE[0];
    private String hex = "#FF0000";
    private String status = "";
    private int cell, gx, gy, palX, palY;

    public EditorScreen(Screen parent, String packName, Identifier itemId) {
        super(Text.literal("Editor"));
        this.parent = parent;
        this.packName = packName;
        this.itemId = itemId;
        loadExisting();
    }

    private Path packDir() {
        return FabricLoader.getInstance().getGameDir().resolve("resourcepacks").resolve(packName);
    }

    private Path textureFile() {
        return packDir().resolve("assets").resolve(itemId.getNamespace())
                .resolve("textures").resolve("item").resolve(itemId.getPath() + ".png");
    }

    private void loadExisting() {
        try {
            Path f = textureFile();
            if (Files.exists(f)) {
                BufferedImage img = ImageIO.read(f.toFile());
                if (img != null && img.getWidth() == 16 && img.getHeight() == 16) {
                    for (int i = 0; i < 256; i++) px[i] = img.getRGB(i % 16, i / 16);
                }
            }
        } catch (Exception ignored) {}
    }

    private void save() {
        try {
            Path f = textureFile();
            Files.createDirectories(f.getParent());
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int i = 0; i < 256; i++) img.setRGB(i % 16, i / 16, px[i]);
            ImageIO.write(img, "png", f.toFile());

            Path meta = packDir().resolve("pack.mcmeta");
            if (!Files.exists(meta)) {
                Files.writeString(meta, "{\"pack\":{\"pack_format\":69,\"min_format\":69,"
                        + "\"max_format\":999,\"description\":\"" + packName + " (MetaTracker)\"}}");
            }
            status = "Gespeichert!";
        } catch (Exception e) {
            status = "Fehler beim Speichern.";
        }
    }

    private void applyHex() {
        try {
            String h = hex.trim().replace("#", "");
            if (h.length() == 6) {
                selected = 0xFF000000 | Integer.parseInt(h, 16);
            } else {
                status = "Hex braucht 6 Zeichen (z.B. #FF0000).";
            }
        } catch (NumberFormatException e) {
            status = "Ungueltige Farbe.";
        }
    }

    @Override
    protected void init() {
        cell = Math.max(5, Math.min(12, (this.height - 70) / 16));
        gx = (this.width - (16 * cell + 20 + 110)) / 2;
        gy = 28;
        palX = gx + 16 * cell + 20;
        palY = 28;

        // 256 Pixel-Buttons
        for (int i = 0; i < 256; i++) {
            final int idx = i;
            addDrawableChild(ButtonWidget.builder(Text.empty(), b -> px[idx] = selected)
                    .dimensions(gx + (i % 16) * cell, gy + (i / 16) * cell, cell, cell).build());
        }

        // Palette
        for (int k = 0; k < 16; k++) {
            final int color = PALETTE[k];
            addDrawableChild(ButtonWidget.builder(Text.empty(), b -> selected = color)
                    .dimensions(palX + (k % 4) * 18, palY + (k / 4) * 18, 16, 16).build());
        }

        // Eigene Farbe
        TextFieldWidget hexField = new TextFieldWidget(this.textRenderer, palX, palY + 86, 70, 16, Text.literal("Hex"));
        hexField.setMaxLength(7);
        hexField.setText(hex);
        hexField.setChangedListener(s -> hex = s);
        addDrawableChild(hexField);
        addDrawableChild(ButtonWidget.builder(Text.literal("OK"), b -> applyHex())
                .dimensions(palX + 74, palY + 86, 30, 16).build());

        // Aktionen
        int y = palY + 108;
        addDrawableChild(ButtonWidget.builder(Text.literal("Radierer"), b -> selected = 0)
                .dimensions(palX, y, 104, 16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Alles fuellen"), b -> Arrays.fill(px, selected))
                .dimensions(palX, y + 17, 104, 16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Leeren"), b -> Arrays.fill(px, 0))
                .dimensions(palX, y + 34, 104, 16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Speichern"), b -> save())
                .dimensions(palX, y + 51, 104, 16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Neu laden"), b -> {
            save();
            this.client.reloadResources();
        }).dimensions(palX, y + 68, 104, 16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Zurueck"), b -> this.close())
                .dimensions(palX, y + 85, 104, 16).build());
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);

        Ui.center(c, this.textRenderer, itemId.getPath() + "  |  Pack: " + packName, this.width / 2, 10, 0xFFFFFFFF);

        // Pixel (ueber die Buttons gemalt; die Buttons nehmen weiterhin die Klicks an)
        for (int i = 0; i < 256; i++) {
            int x = i % 16, y = i / 16;
            int sx = gx + x * cell, sy = gy + y * cell;
            int bg = ((x + y) & 1) == 0 ? 0xFF606060 : 0xFF808080;
            int col = (px[i] >>> 24) == 0 ? bg : (px[i] | 0xFF000000);
            c.fill(sx, sy, sx + cell - 1, sy + cell - 1, col);
        }

        // Palette
        for (int k = 0; k < 16; k++) {
            int x = palX + (k % 4) * 18, y = palY + (k / 4) * 18;
            if (PALETTE[k] == selected) c.fill(x - 1, y - 1, x + 17, y + 17, 0xFFFFFFFF);
            c.fill(x, y, x + 16, y + 16, PALETTE[k]);
        }

        Ui.text(c, this.textRenderer, "Eigene Farbe", palX, palY + 74, 0xFFAAAAAA);
        c.fill(palX + 80, palY + 74, palX + 100, palY + 84, selected == 0 ? 0xFF606060 : selected);

        if (!status.isEmpty()) Ui.center(c, this.textRenderer, status, this.width / 2, this.height - 14, 0xFFFFFF55);
    }

    @Override
    public void removed() {
        save(); // automatisch speichern beim Verlassen
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
