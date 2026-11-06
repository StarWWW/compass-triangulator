package compasstri;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/** Settings, stored at config/compasstri.properties. */
public final class CompassTriConfig {
    public static final int DISTANCE_MIN = 8;
    public static final int DISTANCE_MAX = 256;
    public static final double ANGLE_MIN = 0.2;
    public static final double ANGLE_MAX = 5.0;

    /** Where the direction of a reading comes from. */
    public enum Source {
        /** The compass's own target when the game knows it, otherwise where the player looks. */
        AUTO,
        /** Always where the player looks (for compasses drawn by a server's resource pack). */
        VIEW;

        public final String key = name().toLowerCase(Locale.ROOT);
    }

    /** Where the panel is drawn. */
    public enum HudPosition {
        TOP_LEFT, TOP_RIGHT, MIDDLE_LEFT, MIDDLE_RIGHT;

        public final String key = name().toLowerCase(Locale.ROOT);
    }

    public boolean hud = true;
    /** Take readings by themselves while a compass with a known target is held. */
    public boolean autoRecord = true;
    /** Blocks to walk between two automatic readings. */
    public int minDistance = 32;
    /** How exactly a reading taken by looking is lined up, in degrees. */
    public double angleError = 1.0;
    public Source source = Source.AUTO;
    public HudPosition hudPosition = HudPosition.MIDDLE_RIGHT;
    /**
     * Regular expressions whose first group is the seller and the price in an auction house's item text, for servers
     * whose wording the loose matching gets wrong. Only in the file; empty means the loose matching.
     */
    public String sellerPattern = "";
    public String pricePattern = "";

    private static CompassTriConfig instance;

    public static CompassTriConfig get() {
        if (instance == null) {
            instance = new CompassTriConfig();
            instance.load();
        }
        return instance;
    }

    public void resetToDefaults() {
        CompassTriConfig d = new CompassTriConfig();
        hud = d.hud;
        autoRecord = d.autoRecord;
        minDistance = d.minDistance;
        angleError = d.angleError;
        source = d.source;
        hudPosition = d.hudPosition;
        sellerPattern = d.sellerPattern;
        pricePattern = d.pricePattern;
    }

    /** Only for tests: config folder to use instead of the game's. */
    static Path testConfigDir;

    private static Path file() {
        Path dir = testConfigDir != null ? testConfigDir : FabricLoader.getInstance().getConfigDir();
        return dir.resolve("compasstri.properties");
    }

    public void load() {
        Path path = file();
        if (!Files.exists(path)) return;
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            p.load(reader);
        } catch (IOException e) {
            CompassTriMod.LOGGER.warn("Could not read {}", path, e);
            return;
        }
        hud = Boolean.parseBoolean(p.getProperty("hud", "true").trim());
        autoRecord = Boolean.parseBoolean(p.getProperty("autoRecord", "true").trim());
        try {
            minDistance = Math.clamp(Integer.parseInt(p.getProperty("minDistance", "32").trim()), DISTANCE_MIN, DISTANCE_MAX);
        } catch (NumberFormatException ignored) {
        }
        try {
            angleError = Math.clamp(Double.parseDouble(p.getProperty("angleError", "1.0").trim()), ANGLE_MIN, ANGLE_MAX);
        } catch (NumberFormatException ignored) {
        }
        for (Source s : Source.values()) {
            if (s.key.equals(p.getProperty("source", "").trim())) source = s;
        }
        for (HudPosition h : HudPosition.values()) {
            if (h.key.equals(p.getProperty("hudPosition", "").trim())) hudPosition = h;
        }
        sellerPattern = p.getProperty("sellerPattern", "").trim();
        pricePattern = p.getProperty("pricePattern", "").trim();
    }

    public void save() {
        Properties p = new Properties();
        p.setProperty("hud", Boolean.toString(hud));
        p.setProperty("autoRecord", Boolean.toString(autoRecord));
        p.setProperty("minDistance", Integer.toString(minDistance));
        p.setProperty("angleError", String.format(Locale.ROOT, "%.1f", angleError));
        p.setProperty("source", source.key);
        p.setProperty("hudPosition", hudPosition.key);
        p.setProperty("sellerPattern", sellerPattern);
        p.setProperty("pricePattern", pricePattern);
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                p.store(writer, "Compass Triangulator");
            }
        } catch (IOException e) {
            CompassTriMod.LOGGER.warn("Could not write {}", path, e);
        }
    }
}
