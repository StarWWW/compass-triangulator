import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

/**
 * Generates icon.png in Minecraft's own item style: a compass whose needle points at a lodestone block behind it.
 * 32 x 32 pixels, each written as 8 x 8.
 * Run from the project folder with a JDK that has AWT: java tools/MakeIcon.java
 */
public class MakeIcon {
    public static void main(String[] args) throws Exception {
        lodestone(16, 0, 16);
        compass(0, 7, 25);
        write("src/main/resources/assets/compasstri/icon.png");
    }

    static final int[] STONE = {0x8C8C8C, 0x808080, 0x979797, 0x777777, 0x868686};

    /** Chiselled stone with a band of iron round its sides and an iron plate on top. */
    static void lodestone(int x, int y, int w) {
        int[] side = texture(11, STONE);
        int[] top = texture(12, STONE);
        for (int j = 0; j < 16; j++) {
            for (int i = 0; i < 16; i++) {
                int k = j * 16 + i;
                boolean edge = i == 0 || j == 0 || i == 15 || j == 15;
                if (edge) {
                    side[k] = shade(side[k], 0.75);
                    top[k] = shade(top[k], 0.75);
                }
                if (j >= 5 && j <= 10 && !edge) {
                    side[k] = j == 5 ? 0xA9AFB8 : j == 10 ? 0x3E4248 : (i + j) % 3 == 0 ? 0x4C5057 : 0x5A5F66;
                }
                double ring = Math.max(Math.abs(i - 7.5), Math.abs(j - 7.5));
                if (ring < 5) top[k] = ring >= 4 ? 0x3E4248 : ring >= 3 ? 0xA9AFB8 : ring < 1.5 ? 0x2E3136 : 0x5A5F66;
            }
        }
        block(x, y, w, top, side, side);
    }

    /**
     * A compass of {@code size} pixels: a metal ring lit from the top left, a dark face with four marks, and a needle
     * red towards the top right and grey the other way.
     */
    static void compass(int x0, int y0, int size) {
        layer();
        double c = size / 2.0, r = c - 0.2, face = r - 2.7;
        double ux = Math.sqrt(0.5), uy = -Math.sqrt(0.5);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = x + 0.5 - c, dy = y + 0.5 - c, d = Math.hypot(dx, dy);
                if (d > r) continue;
                double lit = -(dx + dy) / Math.max(d, 0.001) / Math.sqrt(2);
                int col;
                if (d > face) {
                    col = lit > 0.6 ? 0xF0F0F0 : lit > 0.15 ? 0xC8C8C8 : lit > -0.4 ? 0x9A9A9A : 0x6C6C6C;
                    if (d > r - 0.9) col = shade(col, 0.78);
                } else {
                    col = d > face - 1 && lit < -0.3 ? 0x4C4C4C : 0x3A3A3A;
                    if (d > face - 1 && lit > 0.4) col = 0x2C2C2C;
                }
                // the needle: a thin diamond, red half towards the top right
                double u = dx * ux + dy * uy, w = Math.abs(-dx * uy + dy * ux);
                double len = face - 0.6;
                if (u >= 0 && u <= len && w <= 1.5 * (1 - u / len) + 0.35) col = w < 0.5 ? 0xFF6650 : 0xB5261C;
                if (u < 0 && -u <= len * 0.8 && w <= 1.5 * (1 + u / (len * 0.8)) + 0.35) col = w < 0.5 ? 0xEAEAEA : 0x9C9C9C;
                set(x0 + x, y0 + y, col);
            }
        }
        // the marks at north, east, south and west, and the pivot
        int m = (int) Math.round(face - 1.2);
        int ci = (int) Math.floor(c);
        for (int[] p : new int[][] {{0, -m}, {m, 0}, {0, m}, {-m, 0}}) set(x0 + ci + p[0], y0 + ci + p[1], 0x8A8A8A);
        set(x0 + ci, y0 + ci, 0x5E5E5E);
        flatten(true);
    }

    // ---------------------------------------------------------------- pixel kit (the same in every mod's icon tool)

    /** 32 x 32 pixels: items at the size Minecraft draws them (16 x 16), each pixel written as 8 x 8 (256 x 256). */
    static final int N = 32, SCALE = 8;
    static final int[] PX = new int[N * N];
    /** Where drawing goes: the picture, or a layer that gets an outline before it is put on the picture. */
    static int[] target = PX;

    static void set(int x, int y, int rgb) {
        if (x < 0 || y < 0 || x >= N || y >= N) return;
        target[y * N + x] = 0xFF000000 | rgb;
    }

    static int shade(int rgb, double f) {
        int r = (int) Math.min(255, Math.round(((rgb >> 16) & 255) * f));
        int g = (int) Math.min(255, Math.round(((rgb >> 8) & 255) * f));
        int b = (int) Math.min(255, Math.round((rgb & 255) * f));
        return (r << 16) | (g << 8) | b;
    }

    static int mix(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** Starts a layer: an item drawn on its own, so it can get its dark outline. */
    static void layer() {
        target = new int[N * N];
    }

    /**
     * Puts the layer on the picture. With {@code outline}, every empty pixel next to it first gets a dark shade of
     * its neighbour, the way Minecraft's item sprites are outlined, so the item stands out from what is behind it.
     */
    static void flatten(boolean outline) {
        int[] layer = target;
        target = PX;
        if (outline) {
            for (int y = 0; y < N; y++) {
                for (int x = 0; x < N; x++) {
                    if (layer[y * N + x] != 0) continue;
                    int n = neighbour(layer, x, y);
                    if (n != 0) PX[y * N + x] = 0xFF000000 | shade(n, 0.32);
                }
            }
        }
        for (int i = 0; i < PX.length; i++) if (layer[i] != 0) PX[i] = layer[i];
    }

    static int neighbour(int[] layer, int x, int y) {
        int[][] d = {{0, -1}, {-1, 0}, {1, 0}, {0, 1}};
        for (int[] o : d) {
            int nx = x + o[0], ny = y + o[1];
            if (nx >= 0 && ny >= 0 && nx < N && ny < N && layer[ny * N + nx] != 0) return layer[ny * N + nx];
        }
        return 0;
    }

    /** Pixel rows, one character per pixel: '.' is empty, any other is looked up in {@code keys}/{@code colors}. */
    static void sprite(int x0, int y0, String[] rows, String keys, int... colors) {
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                int k = keys.indexOf(rows[y].charAt(x));
                if (k >= 0) set(x0 + x, y0 + y, colors[k]);
            }
        }
    }

    /** A 16 x 16 texture, every pixel one of the colours at random: the grain of Minecraft's textures. */
    static int[] texture(long seed, int... colors) {
        Random rnd = new Random(seed);
        int[] t = new int[256];
        for (int i = 0; i < 256; i++) t[i] = colors[rnd.nextInt(colors.length)];
        return t;
    }

    /** A 16 x 16 texture from 8 x 8 pixel rows, each pixel doubled (the way mob skins are drawn). */
    static int[] skin(String[] rows, String keys, int... colors) {
        int[] t = new int[256];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) t[y * 16 + x] = colors[keys.indexOf(rows[y / 2].charAt(x / 2))];
        }
        return t;
    }

    /**
     * A block the way Minecraft shows one in the inventory: seen from above at an angle, the top fully lit, the left
     * side a little darker and the right side darker still. (x0, y0) is the top left of a w x w square.
     */
    static void block(int x0, int y0, int w, int[] top, int[] left, int[] right) {
        double hw = w / 2.0, q = w / 4.0;
        for (int py = 0; py < w; py++) {
            for (int px = 0; px < w; px++) {
                double x = px + 0.5, y = py + 0.5;
                double a = (y / q + (x - hw) / hw) / 2, b = (y / q - (x - hw) / hw) / 2;
                if (in(a) && in(b)) {
                    set(x0 + px, y0 + py, sample(top, a, b));
                    continue;
                }
                a = x / hw;
                b = (y - q - a * q) / hw;
                if (in(a) && in(b)) {
                    set(x0 + px, y0 + py, shade(sample(left, a, b), 0.8));
                    continue;
                }
                a = (x - hw) / hw;
                b = (y - hw + a * q) / hw;
                if (in(a) && in(b)) set(x0 + px, y0 + py, shade(sample(right, a, b), 0.6));
            }
        }
    }

    static boolean in(double v) {
        return v >= 0 && v < 1;
    }

    static int sample(int[] tex, double u, double v) {
        return tex[Math.min(15, (int) (v * 16)) * 16 + Math.min(15, (int) (u * 16))];
    }

    /** Writes the picture with every pixel as SCALE x SCALE, so it stays sharp at any size. */
    static void write(String path) throws Exception {
        BufferedImage img = new BufferedImage(N * SCALE, N * SCALE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < N * SCALE; y++) {
            for (int x = 0; x < N * SCALE; x++) img.setRGB(x, y, PX[(y / SCALE) * N + x / SCALE]);
        }
        File out = new File(path);
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
        System.out.println("Wrote " + out);
    }
}
