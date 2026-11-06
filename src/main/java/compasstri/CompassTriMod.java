package compasstri;

import com.mojang.blaze3d.platform.InputConstants;
import compasstri.client.CompassHud;
import compasstri.client.CompassTriCommands;
import compasstri.client.CompassTriScreen;
import compasstri.client.ScreenCompass;
import compasstri.client.Texts;
import compasstri.client.Waypoints;
import compasstri.compat.Compat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompassTriMod implements ClientModInitializer {
    public static final String MOD_ID = "compasstri";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final KeyMapping.Category KEY_CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "keybindings"));
    // InputConstants.UNKNOWN is the game's "no key": -1 up to 26.2, 0 from 26.3 (SDL input). A hard-coded -1
    // counts as a real key in 26.3 and crashes the game when it reads the keyboard state.
    /** Unbound by default so they never clash with another mod; set them in Controls. */
    public static final KeyMapping RECORD_KEY = new KeyMapping("key.compasstri.record", InputConstants.UNKNOWN.getValue(), KEY_CATEGORY);
    public static final KeyMapping UNDO_KEY = new KeyMapping("key.compasstri.undo", InputConstants.UNKNOWN.getValue(), KEY_CATEGORY);
    public static final KeyMapping CLEAR_KEY = new KeyMapping("key.compasstri.clear", InputConstants.UNKNOWN.getValue(), KEY_CATEGORY);
    public static final KeyMapping WAYPOINT_KEY = new KeyMapping("key.compasstri.waypoint", InputConstants.UNKNOWN.getValue(), KEY_CATEGORY);
    public static final KeyMapping SETTINGS_KEY = new KeyMapping("key.compasstri.settings", InputConstants.UNKNOWN.getValue(), KEY_CATEGORY);

    @Override
    public void onInitializeClient() {
        CompassTriConfig.get();
        KeyMappingHelper.registerKeyMapping(RECORD_KEY);
        KeyMappingHelper.registerKeyMapping(UNDO_KEY);
        KeyMappingHelper.registerKeyMapping(CLEAR_KEY);
        KeyMappingHelper.registerKeyMapping(WAYPOINT_KEY);
        KeyMappingHelper.registerKeyMapping(SETTINGS_KEY);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Readings.tick(client);
            while (RECORD_KEY.consumeClick()) record(client);
            while (UNDO_KEY.consumeClick()) {
                if (client.player != null && Readings.undo()) {
                    client.player.sendOverlayMessage(Component.translatable("compasstri.msg.undone", Readings.entries().size()));
                }
            }
            while (CLEAR_KEY.consumeClick()) {
                Readings.clear();
                if (client.player != null) client.player.sendOverlayMessage(Component.translatable("compasstri.msg.cleared"));
            }
            while (WAYPOINT_KEY.consumeClick()) {
                if (client.player != null) Waypoints.addInGame(client.player);
            }
            while (SETTINGS_KEY.consumeClick()) {
                Compat.setScreen(client, new CompassTriScreen(Compat.screen(client)));
            }
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "panel"), CompassHud::extract);
        ScreenCompass.register();
        ClientCommandRegistrationCallback.EVENT.register(CompassTriCommands::register);
    }

    private static void record(Minecraft client) {
        if (client.player == null) return;
        Readings.Entry entry = Readings.record(client.player);
        client.player.sendSystemMessage(Texts.recorded(entry, Readings.entries().size()));
        client.player.sendSystemMessage(Texts.status());
    }
}
