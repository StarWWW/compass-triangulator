package compasstri.client;

import compasstri.CompassTriConfig;
import compasstri.HeldCompass;
import compasstri.Listing;
import compasstri.Readings;
import compasstri.Triangulator.Estimate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Turns a compass's target, or the estimate, into a waypoint in Xaero's Minimap named "seller Lodestone price".
 */
public final class Waypoints {
    /** The listing of the compass readings were last taken from, to name a waypoint at the estimate. */
    private static Listing lastListing = Listing.NONE;
    /** The height of the last target seen in compass data; the estimate itself has none. */
    private static @Nullable Integer lastY;

    private Waypoints() {
    }

    /** The seller and price in the text under the item, as far as they can be read. */
    public static Listing listing(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        List<String> lines = lore == null ? List.of() : lore.lines().stream().map(Component::getString).toList();
        CompassTriConfig config = CompassTriConfig.get();
        return Listing.parse(lines, config.sellerPattern.isEmpty() ? null : config.sellerPattern,
            config.pricePattern.isEmpty() ? null : config.pricePattern);
    }

    /** A reading was taken from this compass: name a later waypoint at the estimate after its listing. */
    static void remember(HeldCompass compass) {
        lastListing = listing(compass.stack());
        if (compass.target() != null) lastY = compass.target().pos().getY();
    }

    /** At the target stored in a compass (one in a screen, or in the hand). */
    static void addAtCompass(Player player, HeldCompass compass) {
        if (compass.target() == null || !compass.usable()) {
            player.sendSystemMessage(Component.translatable("compasstri.msg.no_target_here").withStyle(ChatFormatting.YELLOW));
            return;
        }
        BlockPos pos = compass.target().pos();
        add(player, pos.getX(), pos.getY(), pos.getZ(), listing(compass.stack()).waypointName());
    }

    /** In the game: at the estimate if there is one, otherwise at the target of the compass in the hand. */
    public static void addInGame(Player player) {
        Estimate e = Readings.estimate();
        if (e != null) {
            int y = lastY != null ? lastY : (int) Math.floor(player.getY());
            add(player, (int) Math.floor(e.x()), y, (int) Math.floor(e.z()), lastListing.waypointName());
            return;
        }
        HeldCompass held = HeldCompass.find(player);
        if (held != null && held.usable()) {
            addAtCompass(player, held);
            return;
        }
        player.sendSystemMessage(Component.translatable("compasstri.msg.waypoint_nothing").withStyle(ChatFormatting.YELLOW));
    }

    private static void add(Player player, int x, int y, int z, String name) {
        Component message = switch (XaeroWaypoints.add(x, y, z, name)) {
            case ADDED -> Component.translatable("compasstri.msg.waypoint_added", name, x, y, z).withStyle(ChatFormatting.GREEN);
            case RENAMED -> Component.translatable("compasstri.msg.waypoint_renamed", x, y, z, name).withStyle(ChatFormatting.GREEN);
            case NO_XAERO -> Component.translatable("compasstri.msg.waypoint_no_xaero").withStyle(ChatFormatting.YELLOW);
            case NOT_READY -> Component.translatable("compasstri.msg.waypoint_not_ready").withStyle(ChatFormatting.YELLOW);
            case FAILED -> Component.translatable("compasstri.msg.waypoint_failed").withStyle(ChatFormatting.RED);
        };
        player.sendSystemMessage(message);
    }

    static boolean xaeroInstalled() {
        return XaeroWaypoints.installed();
    }
}
