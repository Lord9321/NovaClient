package nova.modules;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import nova.core.*;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public final class ClientModules {
    private ClientModules() {}

    private static int hsb(float h, float s, float v) {
        h = (h - (float) Math.floor(h)) * 6f;
        int i = (int) h;
        float f = h - i, p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f)), r, g, b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return 0xFF000000 | (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }

    /** Accent colour + window colour of the GUI and HUD. */
    public static class Theme extends Mod {
        static Theme INSTANCE;
        Setting.Mode color = mode("Accent", "Orange", "Orange", "Purple", "Blue", "Red", "Green", "Pink", "Cyan", "Gold", "Rainbow", "Custom");
        Setting.Num cr = num("Custom red", 255, 0, 255, 1), cg = num("Custom green", 123, 0, 255, 1), cb = num("Custom blue", 61, 0, 255, 1);
        Setting.Mode panelMode = mode("Panel", "Nova", "Nova", "Midnight", "Amoled", "Slate", "Forest", "Wine", "Custom");
        Setting.Num pr = num("Panel red", 17, 0, 90, 1), pg = num("Panel green", 12, 0, 90, 1), pb = num("Panel blue", 14, 0, 90, 1);

        public Theme() { super("Theme", Category.CLIENT, "Accent colour and window colour of the GUI and HUD."); INSTANCE = this; }

        public static int accent() {
            if (INSTANCE == null) return 0xFFFF7B3D;
            return switch (INSTANCE.color.value) {
                case "Blue" -> 0xFF4D8AFF;
                case "Red" -> 0xFFFF4D5A;
                case "Green" -> 0xFF3DDC84;
                case "Pink" -> 0xFFFF4DC4;
                case "Purple" -> 0xFF8A4DFF;
                case "Cyan" -> 0xFF35D0E8;
                case "Gold" -> 0xFFFFC53D;
                case "Rainbow" -> hsb((System.currentTimeMillis() % 6000L) / 6000f, 0.62f, 1f);
                case "Custom" -> 0xFF000000 | (INSTANCE.cr.getInt() << 16) | (INSTANCE.cg.getInt() << 8) | INSTANCE.cb.getInt();
                default -> 0xFFFF7B3D;
            };
        }

        /** Base colour of the window (dark colours work best, every other shade in the menu is derived from it). */
        public static int panel() {
            if (INSTANCE == null) return 0xFF110C0E;
            return switch (INSTANCE.panelMode.value) {
                case "Midnight" -> 0xFF0B0E17;
                case "Amoled" -> 0xFF000000;
                case "Slate" -> 0xFF14171B;
                case "Forest" -> 0xFF0B130E;
                case "Wine" -> 0xFF160A10;
                case "Custom" -> 0xFF000000 | (INSTANCE.pr.getInt() << 16) | (INSTANCE.pg.getInt() << 8) | INSTANCE.pb.getInt();
                default -> 0xFF110C0E;
            };
        }
    }

    /** Size, shape and glow of the click GUI. */
    public static class Layout extends Mod {
        static Layout INSTANCE;
        Setting.Num width = num("Width", 600, 400, 800, 10), height = num("Height", 360, 260, 480, 10);
        Setting.Mode columns = mode("Columns", "Auto", "Auto", "1", "2", "3");
        Setting.Num radius = num("Corner radius", 9, 0, 14, 1), glow = num("Glow (%)", 100, 0, 200, 10), opacity = num("Window opacity (%)", 92, 40, 100, 1);
        Setting.Bool sheen = bool("Gradient and header line", true);

        public Layout() { super("Layout", Category.CLIENT, "Size, corners, glow and opacity of this menu."); INSTANCE = this; }

        public static int width() { return INSTANCE == null ? 600 : INSTANCE.width.getInt(); }
        public static int height() { return INSTANCE == null ? 360 : INSTANCE.height.getInt(); }
        public static int radius() { return INSTANCE == null ? 9 : INSTANCE.radius.getInt(); }
        public static float glow() { return INSTANCE == null ? 1f : (float) INSTANCE.glow.get() / 100f; }
        public static int opacity() { return INSTANCE == null ? 92 : INSTANCE.opacity.getInt(); }
        public static boolean sheen() { return INSTANCE == null || INSTANCE.sheen.get(); }

        /** Number of card columns for a content width. */
        public static int cols(int cw) {
            if (INSTANCE == null || INSTANCE.columns.is("Auto")) return cw >= 340 ? 2 : 1;
            int n = Integer.parseInt(INSTANCE.columns.value);
            return Math.max(1, Math.min(n, (cw + 6) / 116));
        }
    }

    /**
     * Your own picture behind the menu. Put .png / .jpg files in config/nova/images, then pick one here.
     * The picture is drawn as a grid of coloured cells ("Pixel size") with plain fills, so it does not depend on texture APIs
     * that change between Minecraft versions.
     */
    public static class Background extends Mod {
        static Background INSTANCE;
        Setting.Mode image = mode("Image", "None", "None");
        Setting.Action refresh = action("Refresh image list", Background::refreshAction);
        Setting.Mode area = mode("Draw on", "Window", "Window", "Whole screen");
        Setting.Mode fit = mode("Fit", "Cover", "Cover", "Stretch");
        Setting.Num opacity = num("Image opacity (%)", 60, 5, 100, 5), dim = num("Image dim (%)", 30, 0, 90, 5);
        Setting.Num pixel = num("Pixel size", 3, 2, 10, 1), cardOp = num("Card opacity (%)", 70, 20, 100, 5);

        private static final class Cache { String key = ""; int n; int[] x, y, w, h, c; }
        private static BufferedImage src;
        private static String srcKey = "";
        private static Cache cache;

        public Background() { super("Background", Category.CLIENT, "Your own picture behind the menu (config/nova/images)."); INSTANCE = this; }

        private static Path dir() { return FabricLoader.getInstance().getConfigDir().resolve("nova").resolve("images"); }

        /** Reads the image folder into the "Image" list (also runs every time the menu opens). */
        public static void scan() {
            if (INSTANCE == null) return;
            List<String> names = new ArrayList<>();
            try { Files.createDirectories(dir()); } catch (Exception ignored) {}
            try (Stream<Path> st = Files.list(dir())) {
                st.forEach(f -> {
                    String n = f.getFileName().toString(), l = n.toLowerCase();
                    if (l.endsWith(".png") || l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".gif") || l.endsWith(".bmp")) names.add(n);
                });
            } catch (Exception ignored) {}
            Collections.sort(names);
            names.add(0, "None");
            INSTANCE.image.setModes(names.toArray(new String[0]));
        }

        private static void refreshAction() {
            scan();
            U.chat("Put .png / .jpg files in " + dir() + " - " + (INSTANCE.image.modes.length - 1) + " found.");
        }

        private static boolean active() { return INSTANCE != null && INSTANCE.enabled && !INSTANCE.image.is("None"); }

        /** How see-through the cards are (1 when there is no picture). */
        public static float cardAlpha() { return active() ? (float) INSTANCE.cardOp.get() / 100f : 1f; }

        public static void drawScreen(DrawContext ctx, int sw, int sh, float alpha) {
            if (active() && INSTANCE.area.is("Whole screen")) paint(ctx, 0, 0, sw, sh, 0, alpha);
        }

        public static void drawWindow(DrawContext ctx, int x, int y, int w, int h, int rad, float alpha) {
            if (active() && INSTANCE.area.is("Window")) paint(ctx, x, y, w, h, rad, alpha);
        }

        private static void paint(DrawContext ctx, int ox, int oy, int w, int h, int rad, float alpha) {
            Background b = INSTANCE;
            Path p = dir().resolve(b.image.value);
            if (!Files.exists(p)) return;
            String fk = p + "|" + p.toFile().lastModified();
            if (!fk.equals(srcKey)) {
                srcKey = fk; src = null; cache = null;
                try {
                    src = readScaled(p.toFile());
                    if (src == null) throw new java.io.IOException("unsupported image format");
                } catch (Throwable t) { U.chat("Could not load image: " + t.getMessage()); }
            }
            if (src == null) return;
            int cell = b.pixel.getInt();
            if (b.area.is("Whole screen")) cell = Math.max(cell, 5);
            while ((w / cell + 1) * (h / cell + 1) > 30000) cell++;
            String key = fk + "|" + w + "x" + h + "|" + cell + "|" + b.fit.value + "|" + rad;
            if (cache == null || !key.equals(cache.key)) cache = build(key, w, h, cell, rad, b.fit.is("Cover"));
            int a = Math.round(255f * (float) b.opacity.get() / 100f * alpha);
            if (a > 3) {
                Cache c = cache;
                for (int i = 0; i < c.n; i++)
                    ctx.fill(ox + c.x[i], oy + c.y[i], ox + c.x[i] + c.w[i], oy + c.y[i] + c.h[i], (a << 24) | (c.c[i] & 0xFFFFFF));
            }
            float dm = (float) b.dim.get() / 100f;
            if (dm > 0.01f) Gfx.rrect(ctx, ox, oy, w, h, rad, Gfx.a(0xFF000000, dm * alpha));
        }

        /** Reads the picture, at reduced size for huge files (keeps memory low). */
        private static BufferedImage readScaled(java.io.File f) throws java.io.IOException {
            try (ImageInputStream in = ImageIO.createImageInputStream(f)) {
                if (in == null) return null;
                Iterator<ImageReader> it = ImageIO.getImageReaders(in);
                if (!it.hasNext()) return null;
                ImageReader rd = it.next();
                try {
                    rd.setInput(in, true, true);
                    int sub = Math.max(1, Math.max(rd.getWidth(0), rd.getHeight(0)) / 1600);
                    ImageReadParam prm = rd.getDefaultReadParam();
                    prm.setSourceSubsampling(sub, sub, 0, 0);
                    return rd.read(0, prm);
                } finally { rd.dispose(); }
            }
        }

        private static boolean close(int a, int b) {
            return Math.abs(((a >> 16) & 255) - ((b >> 16) & 255)) <= 10
                && Math.abs(((a >> 8) & 255) - ((b >> 8) & 255)) <= 10
                && Math.abs((a & 255) - (b & 255)) <= 10;
        }

        /** Average colour of every cell, rounded corners cut out, equal neighbours merged into one wide rectangle. */
        private static Cache build(String key, int w, int h, int cell, int rad, boolean cover) {
            int cols = (w + cell - 1) / cell, rows = (h + cell - 1) / cell;
            double iw = src.getWidth(), ih = src.getHeight(), sx, sy, offX = 0, offY = 0;
            if (cover) {
                double sc = Math.max(w / iw, h / ih);
                sx = sy = sc;
                offX = (iw * sc - w) / 2; offY = (ih * sc - h) / 2;
            } else { sx = w / iw; sy = h / ih; }
            int[] col = new int[cols * rows];
            for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
                int dx0 = c * cell, dy0 = r * cell, dx1 = Math.min(w, dx0 + cell), dy1 = Math.min(h, dy0 + cell);
                if (rad > 0) {
                    double cxm = (dx0 + dx1) / 2.0, cym = (dy0 + dy1) / 2.0;
                    double ax = Math.max(rad, Math.min(w - rad, cxm)), ay = Math.max(rad, Math.min(h - rad, cym));
                    double ddx = cxm - ax, ddy = cym - ay;
                    if (ddx * ddx + ddy * ddy > (double) rad * rad) continue;   // outside the rounded corner
                }
                int sr = 0, sg = 0, sb = 0;
                for (int j = 0; j < 3; j++) for (int i = 0; i < 3; i++) {
                    double fx = dx0 + (dx1 - dx0) * (i + 0.5) / 3.0, fy = dy0 + (dy1 - dy0) * (j + 0.5) / 3.0;
                    int u = (int) Math.max(0, Math.min(iw - 1, (fx + offX) / sx)), v = (int) Math.max(0, Math.min(ih - 1, (fy + offY) / sy));
                    int argb = src.getRGB(u, v);
                    sr += (argb >> 16) & 255; sg += (argb >> 8) & 255; sb += argb & 255;
                }
                col[r * cols + c] = 0xFF000000 | ((sr / 9) << 16) | ((sg / 9) << 8) | (sb / 9);
            }
            Cache ca = new Cache();
            int max = cols * rows;
            ca.key = key; ca.x = new int[max]; ca.y = new int[max]; ca.w = new int[max]; ca.h = new int[max]; ca.c = new int[max];
            for (int r = 0; r < rows; r++) {
                int c = 0;
                while (c < cols) {
                    int v = col[r * cols + c];
                    if (v == 0) { c++; continue; }
                    int e = c + 1;
                    while (e < cols && col[r * cols + e] != 0 && close(v, col[r * cols + e])) e++;
                    int x = c * cell, y = r * cell;
                    ca.x[ca.n] = x; ca.y[ca.n] = y; ca.w[ca.n] = Math.min(w, e * cell) - x; ca.h[ca.n] = Math.min(h, y + cell) - y; ca.c[ca.n] = v;
                    ca.n++;
                    c = e;
                }
            }
            return ca;
        }
    }

    /** Sounds and animation of the click GUI. */
    public static class Gui extends Mod {
        static Gui INSTANCE;
        Setting.Bool sounds = bool("Click sounds", true);

        public Gui() { super("GUI", Category.CLIENT, "Sounds of this menu."); INSTANCE = this; }

        public static boolean sounds() { return INSTANCE == null || INSTANCE.sounds.get(); }
    }

    /** Small popups in the bottom right when a module is switched on or off. */
    public static class Notify extends Mod {
        static Notify INSTANCE;
        record N(String text, boolean on, long t0) {}
        static final List<N> LIST = new ArrayList<>();
        Setting.Num seconds = num("Duration (s)", 2.5, 1, 6, 0.5);

        public Notify() { super("Notifications", Category.CLIENT, "Popups when a module is toggled."); INSTANCE = this; }

        public static void push(String name, boolean on) {
            if (INSTANCE == null || !INSTANCE.enabled) return;
            LIST.add(0, new N(name, on, System.currentTimeMillis()));
            while (LIST.size() > 6) LIST.remove(LIST.size() - 1);
        }

        @Override public void onHud(DrawContext ctx) {
            int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
            long now = System.currentTimeMillis();
            long life = (long) (seconds.get() * 1000);
            LIST.removeIf(n -> now - n.t0() > life);
            int acc = Theme.accent();
            int pan = Gfx.mix(Theme.panel(), 0xFFFFFFFF, 0.05f);
            for (int i = 0; i < LIST.size(); i++) {
                N n = LIST.get(i);
                long age = now - n.t0();
                float in = Gfx.ease(age / 260f);
                float out = 1f - Gfx.ease((age - (life - 300)) / 300f);
                String state = n.on() ? "enabled" : "disabled";
                int w = U.width(n.text()) + U.width(state) + 34, h = 20;
                int x = sw - w - 8 + Math.round((1 - in) * (w + 12)), y = sh - 44 - i * 24;
                float al = Math.min(in, out);
                Gfx.shadow(ctx, x, y, w, h, 5, al);
                Gfx.rrect(ctx, x, y, w, h, 5, Gfx.a((0xF0 << 24) | (pan & 0xFFFFFF), al));
                Gfx.rrect(ctx, x, y + 4, 2, h - 8, 1, Gfx.a(acc, al));
                int dot = n.on() ? 0xFF4DE08A : 0xFFFF5A5A;
                Gfx.rrect(ctx, x + 7, y + 7, 6, 6, 3, Gfx.a(dot, al));
                U.text(ctx, n.text(), x + 19, y + 6, Gfx.a(0xFFFFFFFF, al));
                U.text(ctx, state, x + 23 + U.width(n.text()), y + 6, Gfx.a(n.on() ? acc : 0xFF8E8380, al));
            }
        }
    }
}
