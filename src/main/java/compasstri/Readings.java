package compasstri;

import compasstri.Triangulator.Estimate;
import compasstri.Triangulator.Reading;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The readings taken so far in this dimension, and where they meet. Kept until cleared or the dimension changes.
 * <p>
 * A compass whose target never changes is read whenever the player has walked far enough. If the target changes while
 * readings are being taken, the server is sending a stand-in point that follows the player; it is only right once it
 * has been sent for where the player stands, so then a reading waits until the player has stood still for a moment
 * and the target has stopped changing.
 */
public final class Readings {
    static final int MAX = 64;
    private static final int AUTO_EVERY_TICKS = 5;
    /** With a moving target: stand still this long... */
    static final int STILL_TICKS = 30;
    /** ...and the target unchanged this long, before a reading is taken. */
    static final int STABLE_TICKS = 10;

    public enum Kind {
        /** From the compass's stored target. */
        DATA,
        /** From where the player was looking. */
        VIEW
    }

    /** @param compassTarget the target the compass held when the reading was taken (for DATA readings) */
    public record Entry(Reading reading, Kind kind, @Nullable BlockPos compassTarget) {
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static @Nullable ResourceKey<Level> dimension;
    private static @Nullable Estimate estimate;
    private static int ticks;
    private static @Nullable BlockPos lastTarget;
    private static boolean targetMoves;
    private static int targetChangedAt;
    private static double stillX;
    private static double stillZ;
    private static int stillSince;

    private Readings() {
    }

    public static List<Entry> entries() {
        return List.copyOf(ENTRIES);
    }

    public static @Nullable Estimate estimate() {
        return estimate;
    }

    /** The compass's target changed while readings were taken: a stand-in point that follows the player. */
    public static boolean targetMoves() {
        return targetMoves;
    }

    public static void clear() {
        ENTRIES.clear();
        estimate = null;
        targetMoves = false;
    }

    /**
     * How exact a direction from the compass data is: the target is a whole block, so its centre can be up to
     * about 0.71 blocks from the real point. Far away that is a tiny angle.
     */
    static double dataSigma(double distanceToTarget) {
        return Math.toDegrees(Math.atan(0.71 / Math.max(distanceToTarget, 1)));
    }

    private static Entry dataEntry(Player player, HeldCompass held) {
        return new Entry(new Reading(player.getX(), player.getZ(), held.yaw(player), dataSigma(held.distance(player))),
            Kind.DATA, held.target().pos());
    }

    /** @return false when there was nothing to remove */
    public static boolean undo() {
        if (ENTRIES.isEmpty()) return false;
        ENTRIES.removeLast();
        update();
        return true;
    }

    /**
     * A reading where the player stands: from the compass's target if the game knows it (and the settings allow),
     * otherwise in the direction the player looks, which is right when the needle points straight up.
     */
    public static Entry record(Player player) {
        CompassTriConfig config = CompassTriConfig.get();
        HeldCompass held = HeldCompass.find(player);
        Entry entry;
        if (config.source == CompassTriConfig.Source.AUTO && held != null && held.usable()) {
            entry = dataEntry(player, held);
        } else {
            entry = new Entry(new Reading(player.getX(), player.getZ(), player.getYRot(), config.angleError), Kind.VIEW,
                null);
        }
        add(player, entry);
        return entry;
    }

    /**
     * A reading where the player stands, from this compass's data: for a compass the player is not holding, shown in
     * a screen (an auction house, a chest). Only for a compass whose target is in this dimension.
     */
    public static Entry recordFrom(Player player, HeldCompass compass) {
        Entry entry = dataEntry(player, compass);
        add(player, entry);
        return entry;
    }

    /** Once per client tick: forgets readings of another dimension, and takes automatic ones. */
    public static void tick(Minecraft mc) {
        Player player = mc.player;
        if (player == null || mc.level == null) {
            if (!ENTRIES.isEmpty()) clear();
            dimension = null;
            return;
        }
        if (dimension != null && !dimension.equals(mc.level.dimension())) {
            clear();
            dimension = null;
        }
        ticks++;
        if (Math.hypot(player.getX() - stillX, player.getZ() - stillZ) > 0.3) {
            stillX = player.getX();
            stillZ = player.getZ();
            stillSince = ticks;
        }
        HeldCompass held = HeldCompass.find(player);
        if (held != null && held.usable()) {
            BlockPos target = held.target().pos();
            if (lastTarget != null && !target.equals(lastTarget)) {
                targetChangedAt = ticks;
                if (!ENTRIES.isEmpty()) targetMoves = true;
            }
            lastTarget = target;
        }
        if (ticks % AUTO_EVERY_TICKS != 0) return;
        CompassTriConfig config = CompassTriConfig.get();
        if (!config.autoRecord || config.source != CompassTriConfig.Source.AUTO) return;
        if (held == null || !held.usable()) return;
        if (!farFromAll(player.getX(), player.getZ(), config.minDistance)) return;
        // a stand-in target sent for an earlier position would point the wrong way
        if (targetMoves && (ticks - stillSince < STILL_TICKS || ticks - targetChangedAt < STABLE_TICKS)) return;
        add(player, dataEntry(player, held));
    }

    static boolean farFromAll(double x, double z, double distance) {
        for (Entry e : ENTRIES) {
            if (Math.hypot(e.reading().x() - x, e.reading().z() - z) < distance) return false;
        }
        return true;
    }

    private static void add(Player player, Entry entry) {
        dimension = player.level().dimension();
        ENTRIES.add(entry);
        while (ENTRIES.size() > MAX) ENTRIES.removeFirst();
        update();
    }

    private static void update() {
        estimate = Triangulator.solve(ENTRIES.stream().map(Entry::reading).toList());
    }
}
