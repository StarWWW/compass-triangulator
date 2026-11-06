package compasstri.client;

import compasstri.Readings;
import compasstri.Triangulator;
import compasstri.Triangulator.Estimate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

/** Numbers and messages shared by the panel, the chat messages and the settings screen. */
public final class Texts {
    private Texts() {
    }

    /** One decimal, always with a dot: "123.4". */
    public static String degrees(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /** A block coordinate: "-1234". */
    public static String block(double value) {
        return Long.toString((long) Math.floor(value));
    }

    public static String blocks(double value) {
        return Long.toString(Math.round(value));
    }

    /** "12.3° right", "4.0° left" or "straight ahead", from where the player looks to the given direction. */
    public static Component turn(double facingYaw, double targetYaw) {
        double d = Triangulator.angleBetween(facingYaw, targetYaw);
        if (Math.abs(d) < 0.5) return Component.translatable("compasstri.hud.ahead");
        // yaw grows to the right (clockwise, seen from above)
        return Component.translatable(d > 0 ? "compasstri.hud.right" : "compasstri.hud.left", degrees(Math.abs(d)));
    }

    /** "Estimate: 4321, -988 ±12 blocks (5 readings) [Copy]" */
    public static Component estimate(Estimate e) {
        MutableComponent text = estimateText(e);
        String coordinates = block(e.x()) + " " + block(e.z());
        text.append(" ").append(Component.translatable("compasstri.msg.copy").withStyle(style -> style
            .withColor(ChatFormatting.AQUA)
            .withClickEvent(new ClickEvent.CopyToClipboard(coordinates))
            .withHoverEvent(new HoverEvent.ShowText(Component.translatable("compasstri.msg.copy.tooltip")))));
        if (e.behind()) {
            text.append(Component.literal("\n").append(Component.translatable("compasstri.hud.inconsistent"))
                .withStyle(ChatFormatting.YELLOW));
        }
        return text;
    }

    /** "Estimate: 4321, -988 ±12 blocks (5 readings)" */
    public static MutableComponent estimateText(Estimate e) {
        return Component.translatable("compasstri.msg.estimate", block(e.x()), block(e.z()),
            blocks(Math.max(e.radius(), 1)), e.count()).withStyle(ChatFormatting.GREEN);
    }

    /** "Reading 3: 123.4° from 100, -200 (compass data)" */
    public static Component recorded(Readings.Entry entry, int number) {
        var r = entry.reading();
        Component source = entry.kind() == Readings.Kind.DATA
            ? Component.translatable("compasstri.msg.source_data")
            : Component.translatable("compasstri.msg.source_view", degrees(r.sigma()));
        return Component.translatable("compasstri.msg.recorded", number, degrees(r.yaw()), block(r.x()), block(r.z()),
            source).withStyle(ChatFormatting.GRAY);
    }

    /** The estimate (with a copy button for chat), or why there is none yet. */
    public static Component status() {
        return status(true);
    }

    public static Component status(boolean copyButton) {
        Estimate e = Readings.estimate();
        if (e != null) return copyButton ? estimate(e) : estimateText(e);
        int n = Readings.entries().size();
        return n == 0
            ? Component.translatable("compasstri.msg.no_readings").withStyle(ChatFormatting.GRAY)
            : Component.translatable("compasstri.hud.need_more").withStyle(ChatFormatting.YELLOW);
    }
}
