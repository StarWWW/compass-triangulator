package compasstri.client;

import compasstri.CompassTriMod;
import compasstri.HeldCompass;
import compasstri.Readings;
import compasstri.Triangulator.Estimate;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Lodestone compasses shown in a screen: a server's auction house, a chest, the inventory. Their tooltip gets where
 * the compass points (its stored target, and the direction and distance from where the player stands), and while the
 * mouse is over one, the "record a reading" key takes a reading from it. So the compass never has to be held: open
 * the screen at a few places far apart (e.g. after random teleports), and record each time.
 * <p>
 * Besides chest-like screens this works in any screen that shows a compass, such as a server's confirmation dialog
 * (DonutSMP: right-click a listing, "Are you sure you want to buy this?"), which is where some servers name the
 * seller. There the keys are only taken while the mouse is over a compass, so they still reach the screen otherwise
 * (in the Controls screen, for one). The chat is left out.
 */
public final class ScreenCompass {
    /** A tooltip drawn this recently means the mouse is still over that compass. */
    private static final long HOVER_MS = 250;
    private static final long RECORDED_MS = 2500;

    private static @Nullable ItemStack hovered;
    private static long hoveredAt;
    private static long recordedAt;

    private ScreenCompass() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> tooltip(stack, lines));
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof ChatScreen) return;
            // only our keys are taken; every other key still goes to the screen
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> !keyPressed(s, event));
        });
    }

    private static void tooltip(ItemStack stack, List<Component> lines) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        HeldCompass compass = HeldCompass.of(stack, player);
        if (compass == null) return;
        Screen screen = screen(mc);
        boolean inScreen = screen != null && !(screen instanceof ChatScreen);
        if (inScreen) {
            hovered = stack;
            hoveredAt = System.currentTimeMillis();
        }
        lines.addAll(lines(compass, player, inScreen));
    }

    static List<Component> lines(HeldCompass compass, LocalPlayer player, boolean inScreen) {
        List<Component> out = new java.util.ArrayList<>();
        if (compass.target() == null) {
            out.add(Component.translatable("compasstri.tooltip.no_target").withStyle(ChatFormatting.RED));
            return out;
        }
        BlockPos pos = compass.target().pos();
        out.add(Component.translatable("compasstri.tooltip.target", pos.getX(), pos.getY(), pos.getZ())
            .withStyle(ChatFormatting.GRAY));
        if (!compass.usable()) {
            out.add(Component.translatable("compasstri.tooltip.other_dimension").withStyle(ChatFormatting.RED));
            return out;
        }
        out.add(Component.translatable("compasstri.tooltip.direction", Texts.degrees(compass.yaw(player)),
            Texts.blocks(compass.distance(player))).withStyle(ChatFormatting.GRAY));
        if (!inScreen) return out;
        if (Waypoints.xaeroInstalled()) {
            String name = Waypoints.listing(compass.stack()).waypointName();
            out.add(CompassTriMod.WAYPOINT_KEY.isUnbound()
                ? Component.translatable("compasstri.tooltip.waypoint_unbound", name).withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable("compasstri.tooltip.waypoint", CompassTriMod.WAYPOINT_KEY.getTranslatedKeyMessage(),
                    name).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (System.currentTimeMillis() - recordedAt < RECORDED_MS && !Readings.entries().isEmpty()) {
            out.add(Component.translatable("compasstri.tooltip.recorded", Readings.entries().size())
                .withStyle(ChatFormatting.GREEN));
        } else {
            out.add(CompassTriMod.RECORD_KEY.isUnbound()
                ? Component.translatable("compasstri.tooltip.hint_unbound").withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable("compasstri.tooltip.hint_key", CompassTriMod.RECORD_KEY.getTranslatedKeyMessage())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        Estimate e = Readings.estimate();
        if (e != null) {
            out.add(Component.translatable("compasstri.hud.estimate", Texts.block(e.x()), Texts.block(e.z()),
                Texts.blocks(Math.max(e.radius(), 1))).withStyle(ChatFormatting.GREEN));
        } else if (!Readings.entries().isEmpty()) {
            out.add(Component.translatable("compasstri.tooltip.need_more", Readings.entries().size())
                .withStyle(ChatFormatting.YELLOW));
        }
        return out;
    }

    /** @return true when the key was one of ours and is used up */
    private static boolean keyPressed(Screen screen, KeyEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return false;
        // typing into a text box in the screen
        if (screen.getFocused() instanceof EditBox) return false;
        boolean overCompass = hovered != null && System.currentTimeMillis() - hoveredAt <= HOVER_MS;
        if (!(screen instanceof AbstractContainerScreen<?>) && !overCompass) return false;
        if (CompassTriMod.UNDO_KEY.matches(event)) {
            if (Readings.undo()) player.sendSystemMessage(Component.translatable("compasstri.msg.undone", Readings.entries().size()));
            return true;
        }
        boolean waypoint = CompassTriMod.WAYPOINT_KEY.matches(event);
        if (!waypoint && !CompassTriMod.RECORD_KEY.matches(event)) return false;
        ItemStack stack = hovered;
        if (stack == null || System.currentTimeMillis() - hoveredAt > HOVER_MS) {
            player.sendSystemMessage(Component.translatable("compasstri.msg.hover_first").withStyle(ChatFormatting.YELLOW));
            return true;
        }
        HeldCompass compass = HeldCompass.of(stack, player);
        if (compass == null || !compass.usable()) {
            player.sendSystemMessage(Component.translatable("compasstri.msg.no_target_here").withStyle(ChatFormatting.YELLOW));
            return true;
        }
        if (waypoint) {
            Waypoints.addAtCompass(player, compass);
            return true;
        }
        Readings.Entry entry = Readings.recordFrom(player, compass);
        Waypoints.remember(compass);
        recordedAt = System.currentTimeMillis();
        player.sendSystemMessage(Texts.recorded(entry, Readings.entries().size()));
        player.sendSystemMessage(Texts.status());
        return true;
    }

    private static @Nullable Screen screen(Minecraft mc) {
        return compasstri.compat.Compat.screen(mc);
    }
}
