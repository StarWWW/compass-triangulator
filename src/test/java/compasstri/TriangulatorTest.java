package compasstri;

import compasstri.Triangulator.Estimate;
import compasstri.Triangulator.Reading;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriangulatorTest {
    private static final double TX = 4321.5;
    private static final double TZ = -987.5;

    private static Reading toward(double x, double z, double noise, double sigma) {
        return new Reading(x, z, Triangulator.yawTowards(x, z, TX, TZ) + noise, sigma);
    }

    @Test
    void yawFollowsTheGame() {
        // 0 is south (+Z), 90 west (-X), 180 north, -90 east
        assertEquals(0, Triangulator.yawTowards(0, 0, 0, 10), 1e-9);
        assertEquals(90, Triangulator.yawTowards(0, 0, -10, 0), 1e-9);
        assertEquals(180, Math.abs(Triangulator.yawTowards(0, 0, 0, -10)), 1e-9);
        assertEquals(-90, Triangulator.yawTowards(0, 0, 10, 0), 1e-9);
        assertEquals(20, Triangulator.angleBetween(170, -170), 1e-9);
        assertEquals(-20, Triangulator.angleBetween(-170, 170), 1e-9);
    }

    @Test
    void twoExactReadingsMeetAtTheTarget() {
        Estimate e = Triangulator.solve(List.of(toward(0, 0, 0, 0.01), toward(500, 300, 0, 0.01)));
        assertNotNull(e);
        assertEquals(TX, e.x(), 0.01);
        assertEquals(TZ, e.z(), 0.01);
        assertTrue(e.rms() < 0.01);
        assertFalse(e.behind());
    }

    @Test
    void oneDegreeReadingsFromAFewPlaces() {
        // the target is ~4400 blocks away; readings 1 degree off at random, walking a square of 400 blocks
        Random random = new Random(42);
        List<Reading> readings = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            double x = (i % 4) * 130 - 200;
            double z = (i / 4) * 400 - 200;
            readings.add(toward(x, z, random.nextGaussian(), 1.0));
        }
        Estimate e = Triangulator.solve(readings);
        assertNotNull(e);
        double miss = Math.hypot(e.x() - TX, e.z() - TZ);
        assertTrue(miss <= e.radius(), "missed by " + miss + ", radius " + e.radius());
        // rough readings close together and far from the target: a wide but honest radius
        assertTrue(e.radius() > 50, "radius " + e.radius());
    }

    @Test
    void closerAndWiderReadingsAreMoreSure() {
        List<Reading> far = List.of(toward(0, 0, 0, 1), toward(100, 0, 0, 1));
        List<Reading> wide = List.of(toward(0, 0, 0, 1), toward(TX + 1000, TZ - 3000, 0, 1));
        assertTrue(Triangulator.solve(wide).radius() < Triangulator.solve(far).radius());
    }

    @Test
    void parallelOrTooFewReadingsGiveNothing() {
        assertNull(Triangulator.solve(List.of()));
        assertNull(Triangulator.solve(List.of(toward(0, 0, 0, 1))));
        // walking straight towards the target: every reading points the same way
        assertNull(Triangulator.solve(List.of(new Reading(0, 0, 45, 1), new Reading(-10, 10, 45, 1))));
        // exactly opposite directions are parallel lines too
        assertNull(Triangulator.solve(List.of(new Reading(0, 0, 45, 1), new Reading(50, 0, -135, 1))));
    }

    @Test
    void readingsThatPointAwayAreFlagged() {
        // two readings that cross behind one of them: they cannot both be pointing at the same thing
        Estimate e = Triangulator.solve(List.of(new Reading(0, 0, 0, 0.5), new Reading(100, 100, 45, 0.5),
            new Reading(0, 50, 180, 0.5)));
        assertNotNull(e);
        assertTrue(e.behind());
    }

    @Test
    void aMovingDecoyTargetStillGivesTheRealOne() {
        // a server that only wants to show the direction could send a fake point 1000 blocks along it, moved as the
        // player moves; each reading still points at the real target
        List<Reading> readings = new ArrayList<>();
        for (double[] p : new double[][]{{0, 0}, {300, -50}, {-200, 400}}) {
            double yaw = Triangulator.yawTowards(p[0], p[1], TX, TZ);
            double fakeX = p[0] - Math.sin(Math.toRadians(yaw)) * 1000;
            double fakeZ = p[1] + Math.cos(Math.toRadians(yaw)) * 1000;
            readings.add(new Reading(p[0], p[1], Triangulator.yawTowards(p[0], p[1], fakeX, fakeZ), 0.01));
        }
        Estimate e = Triangulator.solve(readings);
        assertEquals(TX, e.x(), 0.5);
        assertEquals(TZ, e.z(), 0.5);
    }
}
