package compasstri;

import java.util.List;

/**
 * Where do the compass directions taken at different places meet?
 * <p>
 * Each reading is a ray on the map: from where the player stood, in the direction the compass pointed. The estimate
 * is the point with the smallest (weighted) sum of squared distances to all the rays. A direction known to within
 * {@code sigma} degrees misses by about {@code distance * tan(sigma)} blocks, so far readings and rough ones count
 * less, and the same numbers give how sure the estimate is.
 * <p>
 * Directions use Minecraft's yaw, as shown on the F3 screen: 0 is south (+Z), 90 west (-X), 180 north, -90 east.
 */
public final class Triangulator {
    /**
     * Readings whose directions all lie within this many degrees of each other are taken as parallel. Kept small:
     * exact directions from the compass data cross well even a fraction of a degree apart, and rough ones that are
     * close together simply get a large radius.
     */
    static final double MIN_SPREAD_DEG = 0.01;
    private static final int ROUNDS = 4;

    private Triangulator() {
    }

    /**
     * @param x        where the reading was taken
     * @param z        where the reading was taken
     * @param yaw      the direction the compass pointed, in Minecraft yaw degrees
     * @param sigma    how far off that direction may be, in degrees (about one standard deviation)
     */
    public record Reading(double x, double z, double yaw, double sigma) {
        double dx() {
            return -Math.sin(Math.toRadians(yaw));
        }

        double dz() {
            return Math.cos(Math.toRadians(yaw));
        }

        /** Distance from the point to this reading's line (sideways, not along it). */
        double offset(double px, double pz) {
            return Math.abs((px - x) * dz() - (pz - z) * dx());
        }

        /** How far ahead of the reading the point is (negative: behind it). */
        double ahead(double px, double pz) {
            return (px - x) * dx() + (pz - z) * dz();
        }
    }

    /**
     * @param x        the estimated position
     * @param z        the estimated position
     * @param radius   about 95% sure the target is within this many blocks
     * @param rms      how far the readings' lines pass from the estimate on average (0 for perfect readings)
     * @param count    readings used
     * @param behind   some reading points away from the estimate: the readings do not belong together (different
     *                 targets, or the target moved)
     */
    public record Estimate(double x, double z, double radius, double rms, int count, boolean behind) {
    }

    /** Yaw from one point towards another, the same way the game turns the compass needle. */
    public static double yawTowards(double fromX, double fromZ, double toX, double toZ) {
        return Math.toDegrees(Math.atan2(-(toX - fromX), toZ - fromZ));
    }

    /** The difference between two directions, -180 (exclusive) to 180 degrees. */
    public static double angleBetween(double fromYaw, double toYaw) {
        double d = (toYaw - fromYaw) % 360;
        if (d <= -180) d += 360;
        if (d > 180) d -= 360;
        return d;
    }

    /** True when the readings point in directions different enough to cross. */
    public static boolean spreadEnough(List<Reading> readings) {
        for (int i = 0; i < readings.size(); i++) {
            for (int j = i + 1; j < readings.size(); j++) {
                if (Math.abs(angleBetween(readings.get(i).yaw(), readings.get(j).yaw())) >= MIN_SPREAD_DEG
                    && Math.abs(Math.abs(angleBetween(readings.get(i).yaw(), readings.get(j).yaw())) - 180) >= MIN_SPREAD_DEG) {
                    return true;
                }
            }
        }
        return false;
    }

    /** @return the estimate, or null with fewer than two readings or when they are (nearly) parallel */
    public static Estimate solve(List<Reading> readings) {
        if (readings.size() < 2 || !spreadEnough(readings)) return null;
        double[] distance = new double[readings.size()];
        java.util.Arrays.fill(distance, 1.0);
        double x = 0;
        double z = 0;
        double a11 = 0;
        double a12 = 0;
        double a22 = 0;
        // weights depend on the distance to the estimate, which depends on the weights: a few rounds settle it
        for (int round = 0; round < ROUNDS; round++) {
            a11 = 0;
            a12 = 0;
            a22 = 0;
            double b1 = 0;
            double b2 = 0;
            for (int i = 0; i < readings.size(); i++) {
                Reading r = readings.get(i);
                double miss = Math.max(distance[i], 1.0) * Math.tan(Math.toRadians(Math.max(r.sigma(), 1e-4)));
                double w = 1.0 / (miss * miss);
                double dx = r.dx();
                double dz = r.dz();
                // (I - d d^T): only the sideways part of the gap to the line counts
                double m11 = 1 - dx * dx;
                double m12 = -dx * dz;
                double m22 = 1 - dz * dz;
                a11 += w * m11;
                a12 += w * m12;
                a22 += w * m22;
                b1 += w * (m11 * r.x() + m12 * r.z());
                b2 += w * (m12 * r.x() + m22 * r.z());
            }
            double det = a11 * a22 - a12 * a12;
            if (!(det > 0) || det < 1e-12 * (a11 + a22) * (a11 + a22)) return null;
            x = (a22 * b1 - a12 * b2) / det;
            z = (a11 * b2 - a12 * b1) / det;
            for (int i = 0; i < readings.size(); i++) {
                Reading r = readings.get(i);
                distance[i] = Math.hypot(x - r.x(), z - r.z());
            }
        }
        // the inverse of the weight matrix is the estimate's covariance; its larger axis gives the radius
        double det = a11 * a22 - a12 * a12;
        double c11 = a22 / det;
        double c22 = a11 / det;
        double c12 = -a12 / det;
        double mean = (c11 + c22) / 2;
        double spread = Math.sqrt(((c11 - c22) / 2) * ((c11 - c22) / 2) + c12 * c12);
        double radius = 2 * Math.sqrt(Math.max(mean + spread, 0));

        double sum = 0;
        boolean behind = false;
        for (Reading r : readings) {
            double off = r.offset(x, z);
            sum += off * off;
            if (r.ahead(x, z) < -Math.max(radius, 1.0)) behind = true;
        }
        return new Estimate(x, z, radius, Math.sqrt(sum / readings.size()), readings.size(), behind);
    }
}
