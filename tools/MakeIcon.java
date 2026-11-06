import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Generates icon.png: a lodestone compass at the bottom, two dashed sight lines from two places on the map that
 * cross at a red target mark (triangulation). Drawn at 64x64 and scaled to 128x128 so it stays crisp in Mod Menu.
 * Run from the project folder: java tools/MakeIcon.java
 */
public class MakeIcon {
    static final int N = 64;
    static final int[] PX = new int[N * N];

    static final int TARGET_X = 44, TARGET_Y = 15;

    public static void main(String[] args) throws Exception {
        badge(0xFF2A3346, 0xFF141822);
        grid();
        // two places a reading was taken, and their lines towards the target
        sight(12, 40, 0xFF8FD3FF);
        sight(52, 50, 0xFF8FD3FF);
        target();
        compass(20, 46, 13);
        write(new File("src/main/resources/assets/compasstri/icon.png"), 2);
    }

    static void set(int x, int y, int argb) {
        if (x < 0 || y < 0 || x >= N || y >= N) return;
        if (PX[y * N + x] == 0) return; // outside the badge
        PX[y * N + x] = argb;
    }

    /** Rounded square with a vertical gradient. */
    static void badge(int top, int bottom) {
        int r = 10;
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                int cx = Math.max(r, Math.min(N - 1 - r, x));
                int cy = Math.max(r, Math.min(N - 1 - r, y));
                if ((x - cx) * (x - cx) + (y - cy) * (y - cy) > r * r) continue;
                PX[y * N + x] = mix(top, bottom, y / (double) (N - 1));
            }
        }
        // a light edge
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                if (PX[y * N + x] == 0) continue;
                boolean edge = x == 0 || y == 0 || x == N - 1 || y == N - 1
                    || PX[y * N + x - 1] == 0 || PX[y * N + x + 1] == 0 || PX[(y - 1) * N + x] == 0 || PX[(y + 1) * N + x] == 0;
                if (edge) PX[y * N + x] = 0xFF4A5670;
            }
        }
    }

    /** Faint map grid, like chunk lines. */
    static void grid() {
        for (int y = 2; y < N - 2; y++) {
            for (int x = 2; x < N - 2; x++) {
                if ((x % 8 == 0 || y % 8 == 0) && PX[y * N + x] != 0) set(x, y, mix(PX[y * N + x], 0xFF3A4560, 0.6));
            }
        }
    }

    /** A dashed line from (x0, y0) to the target, with a dot where the reading was taken. */
    static void sight(int x0, int y0, int color) {
        int dx = Math.abs(TARGET_X - x0), dy = -Math.abs(TARGET_Y - y0);
        int sx = x0 < TARGET_X ? 1 : -1, sy = y0 < TARGET_Y ? 1 : -1;
        int err = dx + dy, x = x0, y = y0, step = 0;
        while (true) {
            if ((step++ / 2) % 2 == 0) set(x, y, color);
            if (x == TARGET_X && y == TARGET_Y) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x += sx; }
            if (e2 <= dx) { err += dx; y += sy; }
        }
        for (int oy = -1; oy <= 1; oy++) {
            for (int ox = -1; ox <= 1; ox++) set(x0 + ox, y0 + oy, ox == 0 || oy == 0 ? 0xFFFFFFFF : 0xFF8FD3FF);
        }
    }

    /** Red ring with a cross: the estimated place. */
    static void target() {
        for (int y = -6; y <= 6; y++) {
            for (int x = -6; x <= 6; x++) {
                double d = Math.sqrt(x * x + y * y);
                if (d >= 4.2 && d <= 5.6) set(TARGET_X + x, TARGET_Y + y, 0xFFFF4D4D);
                else if (d < 4.2 && (x == 0 || y == 0) && d > 1.2) set(TARGET_X + x, TARGET_Y + y, 0xFFFFB3B3);
            }
        }
        set(TARGET_X, TARGET_Y, 0xFFFFFFFF);
    }

    /** A compass: brass rim, dark face, red and white needle pointing up and right (at the target). */
    static void compass(int cx, int cy, int r) {
        for (int y = -r - 1; y <= r + 1; y++) {
            for (int x = -r - 1; x <= r + 1; x++) {
                double d = Math.sqrt(x * x + y * y);
                if (d > r + 0.5) continue;
                int color;
                if (d > r - 2.2) color = mix(0xFFE0B456, 0xFF8A6320, (y + r) / (2.0 * r)); // rim
                else color = mix(0xFF3B3F4A, 0xFF23262E, (y + r) / (2.0 * r)); // face
                set(cx + x, cy + y, color);
            }
        }
        // tick marks
        for (int[] t : new int[][]{{0, -1}, {1, 0}, {0, 1}, {-1, 0}}) {
            set(cx + t[0] * (r - 3), cy + t[1] * (r - 3), 0xFF9AA0AE);
        }
        // needle towards the target
        double ang = Math.atan2(TARGET_Y - cy, TARGET_X - cx);
        double ux = Math.cos(ang), uy = Math.sin(ang);
        for (double s = -(r - 4); s <= r - 4; s += 0.25) {
            int px = (int) Math.round(cx + ux * s), py = (int) Math.round(cy + uy * s);
            int color = s > 0 ? 0xFFFF5A4A : 0xFFE6E6E6;
            set(px, py, color);
            // a little wider near the middle
            if (Math.abs(s) < (r - 4) * 0.55) {
                set((int) Math.round(px - uy), (int) Math.round(py + ux), s > 0 ? 0xFFC9372B : 0xFFB8B8B8);
            }
        }
        set(cx, cy, 0xFFE0B456);
    }

    static int mix(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return 0xFF000000 | (int) (ar + (br - ar) * t) << 16 | (int) (ag + (bg - ag) * t) << 8 | (int) (ab + (bb - ab) * t);
    }

    static void write(File out, int factor) throws Exception {
        BufferedImage img = new BufferedImage(N * factor, N * factor, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < N * factor; y++) {
            for (int x = 0; x < N * factor; x++) img.setRGB(x, y, PX[(y / factor) * N + x / factor]);
        }
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
        System.out.println("wrote " + out);
    }
}
