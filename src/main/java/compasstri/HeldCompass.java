package compasstri;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import org.jspecify.annotations.Nullable;

/**
 * A lodestone compass in the player's hand (or shown in a screen, such as a server's auction house), and what the game
 * knows about where it points.
 * <p>
 * The game turns a lodestone compass's needle towards the target stored in the item ({@code lodestone_tracker}),
 * so whenever the needle points somewhere, that target is known here too. It is used as it is for the direction,
 * and shown, because a server could also send a stand-in point that only lies in the right direction.
 *
 * @param target  the stored target, or null when there is none (the needle spins)
 * @param usable  the target is in this dimension, so the needle points at it
 */
public record HeldCompass(ItemStack stack, @Nullable GlobalPos target, boolean usable) {
    public static @Nullable HeldCompass find(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            HeldCompass compass = of(player.getItemInHand(hand), player);
            if (compass != null) return compass;
        }
        return null;
    }

    /** Any item with lodestone data, as seen from where the player is; null for other items. */
    public static @Nullable HeldCompass of(ItemStack stack, Player player) {
        LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        if (tracker == null) return null;
        GlobalPos target = tracker.target().orElse(null);
        // the needle only points at a target in the same dimension, wherever the compass is shown
        boolean usable = target != null && target.dimension().equals(player.level().dimension());
        return new HeldCompass(stack, target, usable);
    }

    /** Any kind of compass in a hand, also ones without lodestone data (for readings taken by looking). */
    public static boolean holdsSomeCompass(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty()) continue;
            if (stack.has(DataComponents.LODESTONE_TRACKER)) return true;
            if (BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().contains("compass")) return true;
            var model = stack.get(DataComponents.ITEM_MODEL);
            if (model != null && model.getPath().contains("compass")) return true;
        }
        return false;
    }

    /** The direction from the player to the target block's centre, as the needle shows it (Minecraft yaw). */
    public double yaw(Player player) {
        return Triangulator.yawTowards(player.getX(), player.getZ(), target.pos().getX() + 0.5, target.pos().getZ() + 0.5);
    }

    public double distance(Player player) {
        return Math.hypot(target.pos().getX() + 0.5 - player.getX(), target.pos().getZ() + 0.5 - player.getZ());
    }
}
