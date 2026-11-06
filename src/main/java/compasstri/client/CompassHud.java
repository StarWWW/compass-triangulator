package compasstri.client;

import compasstri.CompassTriConfig;
import compasstri.CompassTriMod;
import compasstri.HeldCompass;
import compasstri.Readings;
import compasstri.Triangulator;
import compasstri.Triangulator.Estimate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A small panel while a compass is held (or readings exist): where the compass points, the readings so far, and
 * where they meet.
 */
public final class CompassHud {
    private static final int PAD = 4;
    private static final int LINE = 10;
    private static final int TITLE_COLOR = 0xFFE8C66A;

    private CompassHud() {
    }

    public static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        CompassTriConfig config = CompassTriConfig.get();
        // HUD elements are part of the game HUD, so F1 hides this panel too
        if (!config.hud || player == null) return;
        List<Component> lines = lines(player, config);
        if (lines.isEmpty()) return;

        Font font = mc.font;
        Component title = Component.translatable("compasstri.hud.title");
        int width = font.width(title);
        for (Component line : lines) width = Math.max(width, font.width(line));
        width += PAD * 2;
        int height = PAD * 2 + LINE * (lines.size() + 1);
        int x = switch (config.hudPosition) {
            case TOP_LEFT, MIDDLE_LEFT -> 4;
            case TOP_RIGHT, MIDDLE_RIGHT -> g.guiWidth() - width - 4;
        };
        int y = switch (config.hudPosition) {
            case TOP_LEFT, TOP_RIGHT -> 4;
            case MIDDLE_LEFT, MIDDLE_RIGHT -> (g.guiHeight() - height) / 2;
        };
        g.fill(x, y, x + width, y + height, 0x90000000);
        g.text(font, title, x + PAD, y + PAD, TITLE_COLOR);
        for (int i = 0; i < lines.size(); i++) {
            g.text(font, lines.get(i), x + PAD, y + PAD + LINE * (i + 1), 0xFFFFFFFF);
        }
    }

    static List<Component> lines(LocalPlayer player, CompassTriConfig config) {
        HeldCompass held = HeldCompass.find(player);
        boolean compassInHand = held != null || HeldCompass.holdsSomeCompass(player);
        List<Readings.Entry> entries = Readings.entries();
        List<Component> lines = new ArrayList<>();
        if (!compassInHand && entries.isEmpty()) return lines;

        boolean fromData = config.source == CompassTriConfig.Source.AUTO && held != null && held.usable();
        if (fromData) {
            double yaw = held.yaw(player);
            lines.add(Component.translatable("compasstri.hud.direction", Texts.degrees(yaw),
                Texts.turn(player.getYRot(), yaw)));
            lines.add(Component.translatable("compasstri.hud.data", held.target().pos().getX(), held.target().pos().getZ())
                .withStyle(ChatFormatting.GRAY));
            if (Readings.targetMoves()) {
                lines.add(Component.translatable("compasstri.hud.moving").withStyle(ChatFormatting.GOLD));
            }
        } else if (compassInHand) {
            if (held != null && !held.usable()) {
                lines.add(Component.translatable("compasstri.hud.spinning").withStyle(ChatFormatting.RED));
            }
            lines.add(Component.translatable("compasstri.hud.facing", Texts.degrees(Triangulator.angleBetween(0, player.getYRot()))));
            lines.add(CompassTriMod.RECORD_KEY.isUnbound()
                ? Component.translatable("compasstri.hud.hint_command").withStyle(ChatFormatting.GRAY)
                : Component.translatable("compasstri.hud.hint_key", CompassTriMod.RECORD_KEY.getTranslatedKeyMessage())
                    .withStyle(ChatFormatting.GRAY));
        }

        lines.add(Component.translatable("compasstri.hud.readings", entries.size()));
        Estimate e = Readings.estimate();
        if (e != null) {
            double distance = Math.hypot(e.x() - player.getX(), e.z() - player.getZ());
            lines.add(Component.translatable("compasstri.hud.estimate", Texts.block(e.x()), Texts.block(e.z()),
                Texts.blocks(Math.max(e.radius(), 1))).withStyle(ChatFormatting.GREEN));
            lines.add(Component.translatable("compasstri.hud.away", Texts.blocks(distance)).withStyle(ChatFormatting.GREEN));
            if (e.behind()) {
                lines.add(Component.translatable("compasstri.hud.inconsistent").withStyle(ChatFormatting.YELLOW));
            } else if (fromData) {
                double off = Math.hypot(e.x() - (held.target().pos().getX() + 0.5), e.z() - (held.target().pos().getZ() + 0.5));
                lines.add(off <= Math.max(2, e.radius())
                    ? Component.translatable("compasstri.hud.matches").withStyle(ChatFormatting.DARK_GREEN)
                    : Component.translatable("compasstri.hud.differs", Texts.blocks(off)).withStyle(ChatFormatting.GOLD));
            }
        } else if (!entries.isEmpty()) {
            lines.add(Component.translatable("compasstri.hud.need_more").withStyle(ChatFormatting.YELLOW));
        }
        return lines;
    }
}
