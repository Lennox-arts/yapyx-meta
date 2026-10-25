package com.yapyx.metatracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * MetaTracker
 *  - Zaehlt, welche Items (Waffe + Ruestung) andere Spieler pro Server tragen.
 *  - /metatracker  -> GUI mit allen Metas (pro Server blaettern)
 *  - /resourcegui  -> Texturepack-Editor
 *  - Rechts-Shift  -> Client-Menue
 *  - Combo-Anzeige in der Aktionsleiste
 */
public class MetaTracker implements ClientModInitializer {

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("metatracker.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Server -> (Item -> Anzahl Spieler)
    static Map<String, Map<String, Integer>> data = new HashMap<>();
    static String lastServer = "";

    private static KeyBinding openKey;
    private final Set<UUID> seen = new HashSet<>();
    private int combo = 0;
    private int tick = 0;

    @Override
    public void onInitializeClient() {
        load();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("metatracker", "main"));
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.metatracker.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category));

        // Combo hochzaehlen, wenn ich einen Spieler treffe
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && entity instanceof PlayerEntity) combo++;
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("metatracker").executes(ctx -> {
                openLater(new MetaScreen(null));
                return 1;
            }));
            dispatcher.register(ClientCommandManager.literal("resourcegui").executes(ctx -> {
                openLater(new PackScreen(null));
                return 1;
            }));
        });
    }

    /** Screen erst im naechsten Tick oeffnen, sonst schliesst der Chat ihn wieder. */
    private static void openLater(net.minecraft.client.gui.screen.Screen screen) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.send(() -> mc.setScreen(screen));
    }

    private void onTick(MinecraftClient mc) {
        while (openKey.wasPressed()) {
            if (mc.currentScreen == null && mc.player != null) mc.setScreen(new MenuScreen());
        }

        if (mc.player == null || mc.world == null) return;
        ServerInfo info = mc.getCurrentServerEntry();
        if (info == null) return; // nur auf Multiplayer-Servern

        String server = info.address;
        if (!server.equals(lastServer)) {
            lastServer = server;
            seen.clear();
            combo = 0;
        }

        // Combo zuruecksetzen, wenn ich getroffen werde
        if (mc.player.hurtTime > 0) combo = 0;
        if (combo > 0) mc.player.sendMessage(Text.literal("Combo: " + combo), true);

        // Nur jede Sekunde nach neuen Spielern schauen
        if (++tick % 20 != 0) return;

        boolean changed = false;
        for (var p : mc.world.getPlayers()) {
            if (p == mc.player || !seen.add(p.getUuid())) continue;
            count(server, p.getMainHandStack());
            count(server, p.getEquippedStack(EquipmentSlot.HEAD));
            count(server, p.getEquippedStack(EquipmentSlot.CHEST));
            count(server, p.getEquippedStack(EquipmentSlot.LEGS));
            count(server, p.getEquippedStack(EquipmentSlot.FEET));
            changed = true;
        }
        if (changed) save();
    }

    private void count(String server, ItemStack stack) {
        if (stack.isEmpty()) return;
        String id = Registries.ITEM.getId(stack.getItem()).getPath();
        data.computeIfAbsent(server, k -> new HashMap<>()).merge(id, 1, Integer::sum);
    }

    private static void load() {
        try {
            if (Files.exists(FILE)) {
                var type = new TypeToken<Map<String, Map<String, Integer>>>() {}.getType();
                Map<String, Map<String, Integer>> loaded = GSON.fromJson(Files.readString(FILE), type);
                if (loaded != null) data = loaded;
            }
        } catch (Exception ignored) {}
    }

    static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(data));
        } catch (Exception ignored) {}
    }
}
