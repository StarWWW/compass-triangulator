package compasstri.client;

import compasstri.CompassTriMod;
import net.fabricmc.loader.api.FabricLoader;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;

/**
 * Saves a waypoint in Xaero's Minimap (which Xaero's World Map shows too), in the waypoint set in use for the world
 * and dimension the player is in, the same way Xaero's own "add waypoint" screen does.
 * Only touched when Xaero's Minimap is installed, so the mod works without it.
 */
final class XaeroWaypoints {
    enum Result { ADDED, RENAMED, NO_XAERO, NOT_READY, FAILED }

    private XaeroWaypoints() {
    }

    static boolean installed() {
        FabricLoader loader = FabricLoader.getInstance();
        return loader.isModLoaded("xaerominimap") || loader.isModLoaded("xaerominimapfair");
    }

    /** A waypoint already at these coordinates is renamed instead of added again. */
    static Result add(int x, int y, int z, String name) {
        if (!installed()) return Result.NO_XAERO;
        try {
            return Impl.add(x, y, z, name);
        } catch (Throwable t) {
            CompassTriMod.LOGGER.error("Could not add a waypoint to Xaero's Minimap", t);
            return Result.FAILED;
        }
    }

    /** Kept apart so that none of Xaero's classes are loaded when it is not installed. */
    private static final class Impl {
        static Result add(int x, int y, int z, String name) throws java.io.IOException {
            MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
            if (session == null) return Result.NOT_READY;
            MinimapWorld world = session.getWorldManager().getCurrentWorld();
            if (world == null) return Result.NOT_READY;
            WaypointSet set = world.getCurrentWaypointSet();
            if (set == null) return Result.NOT_READY;
            Result result = Result.ADDED;
            Waypoint existing = null;
            for (Waypoint w : set.getWaypoints()) {
                if (w.getX() == x && w.getY() == y && w.getZ() == z) {
                    existing = w;
                    break;
                }
            }
            if (existing != null) {
                existing.setName(name);
                result = Result.RENAMED;
            } else {
                set.add(new Waypoint(x, y, z, name, "L", WaypointColor.PURPLE, WaypointPurpose.NORMAL));
            }
            session.getWorldManagerIO().saveWorld(world);
            return result;
        }
    }
}
