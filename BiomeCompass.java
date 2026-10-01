package com.turbominer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fleche vers le biome neigeux le plus proche + liste des biomes detectes (chunks charges) a choisir au clavier.
 * Le client ne connait que les biomes des chunks charges : la portee est celle de ta distance d'affichage.
 */
public final class BiomeCompass {
    public static final class Entry {
        public final Identifier id;
        public final BlockPos pos;
        public final boolean snowy;
        public final String name;
        final double dist;

        Entry(Identifier id, BlockPos pos, double dist, boolean snowy, String name) {
            this.id = id;
            this.pos = pos;
            this.dist = dist;
            this.snowy = snowy;
            this.name = name;
        }
    }

    private static List<Entry> entries = List.of();
    private static Identifier selected = null; // null = biome neigeux le plus proche (auto)
    private static int ticks = 0;

    private BiomeCompass() {}

    public static void reset() {
        entries = List.of();
        selected = null;
        ticks = 0;
    }

    public static void toggle() {
        ModConfig.biomeHud = !ModConfig.biomeHud;
        ModConfig.save();
        ticks = 0;
        say("Boussole biomes : " + (ModConfig.biomeHud ? "ON" : "OFF"));
    }

    public static boolean isSnowy(Identifier id) {
        String p = id.getPath();
        return p.contains("snow") || p.contains("frozen") || p.contains("glacier") || p.contains("jagged")
                || p.equals("grove") || p.startsWith("ice") || p.contains("_ice");
    }

    // ---------- selection ----------

    public static Entry target() {
        List<Entry> l = entries;
        if (selected != null) {
            for (Entry e : l) if (e.id.equals(selected)) return e;
            return null;
        }
        for (Entry e : l) if (e.snowy) return e;
        return null;
    }

    public static void cycle(int dir) {
        List<Entry> l = entries;
        if (l.isEmpty()) {
            say("Aucun biome detecte pour l'instant");
            return;
        }
        Entry t = target();
        int i = t == null ? (dir > 0 ? -1 : 0) : l.indexOf(t);
        i = Math.floorMod(i + dir, l.size());
        selected = l.get(i).id;
        say("Biome vise : " + l.get(i).name);
    }

    public static void selectAutoSnow() {
        selected = null;
        say("Biome vise : neige (auto)");
    }

    private static void say(String s) {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p != null) p.sendMessage(Text.literal(s), true);
    }

    // ---------- scan ----------

    public static void tick(MinecraftClient c) {
        if (!ModConfig.biomeHud || c.world == null || c.player == null) return;
        if (ticks++ % 40 != 0) return;
        scan(c, c.world, c.player);
    }

    private static void scan(MinecraftClient c, ClientWorld w, ClientPlayerEntity p) {
        int r = Math.min(32, Math.max(2, c.options.getClampedViewDistance()));
        int pcx = p.getBlockX() >> 4, pcz = p.getBlockZ() >> 4;
        double px = p.getX(), pz = p.getZ();
        Map<Identifier, BlockPos> pos = new HashMap<>();
        Map<Identifier, Double> dist = new HashMap<>();

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                int cx = pcx + dx, cz = pcz + dz;
                if (w.getChunkManager().getWorldChunk(cx, cz) == null) continue;
                int x = cx * 16 + 8, z = cz * 16 + 8;
                consider(w, new BlockPos(x, surfaceY(w, x, z), z), px, pz, pos, dist);
            }
        }
        consider(w, p.getBlockPos(), px, pz, pos, dist);

        List<Entry> out = new ArrayList<>();
        for (Map.Entry<Identifier, BlockPos> e : pos.entrySet()) {
            Identifier id = e.getKey();
            out.add(new Entry(id, e.getValue(), dist.get(id), isSnowy(id), nameOf(id)));
        }
        out.sort(Comparator.comparingDouble(en -> en.dist));
        entries = out;
    }

    private static int surfaceY(ClientWorld w, int x, int z) {
        int y;
        try {
            y = w.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1;
        } catch (Exception e) {
            y = 64;
        }
        return MathHelper.clamp(y, w.getBottomY(), w.getTopY() - 1);
    }

    private static void consider(ClientWorld w, BlockPos bp, double px, double pz,
                                 Map<Identifier, BlockPos> pos, Map<Identifier, Double> dist) {
        RegistryEntry<Biome> e = w.getBiome(bp);
        Optional<RegistryKey<Biome>> key = e.getKey();
        if (key.isEmpty()) return;
        Identifier id = key.get().getValue();
        double d = Math.hypot(bp.getX() + 0.5 - px, bp.getZ() + 0.5 - pz);
        Double old = dist.get(id);
        if (old == null || d < old) {
            dist.put(id, d);
            pos.put(id, bp);
        }
    }

    private static String nameOf(Identifier id) {
        String s = Text.translatable(Util.createTranslationKey("biome", id)).getString();
        if (s.startsWith("biome.")) {
            String path = id.getPath().replace('_', ' ');
            s = path.isEmpty() ? s : Character.toUpperCase(path.charAt(0)) + path.substring(1);
        }
        return s;
    }

    // ---------- HUD ----------

    public static void render(DrawContext ctx, MinecraftClient c) {
        if (!ModConfig.biomeHud || c.player == null) return;
        ClientPlayerEntity p = c.player;
        TextRenderer tr = c.textRenderer;
        int w = ctx.getScaledWindowWidth();
        List<Entry> l = entries;
        Entry t = target();

        int cx = w / 2, cy = 44;
        if (t != null) {
            double dx = t.pos.getX() + 0.5 - p.getX(), dz = t.pos.getZ() + 0.5 - p.getZ();
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > 6) {
                float yawT = (float) (MathHelper.atan2(dz, dx) * 180.0 / Math.PI) - 90f;
                float ang = MathHelper.wrapDegrees(yawT - p.getYaw());
                arrow(ctx, cx, cy, ang, t.snowy ? 0xFF66E0FF : 0xFF66FF66);
            }
            String s = t.name + " - " + (d <= 6 ? "vous y etes" : (int) d + " m");
            ctx.drawCenteredTextWithShadow(tr, s, cx, cy + 14, 0xFFFFFF);
        } else {
            String s = selected == null ? "Aucun biome neigeux charge" : "Biome hors de portee (non charge)";
            ctx.drawCenteredTextWithShadow(tr, s, cx, cy, 0xFFAAAA);
        }

        int y = 4;
        String title = "Biomes (+/- choisir, 3 = neige)";
        ctx.drawTextWithShadow(tr, title, w - 4 - tr.getWidth(title), y, 0x66E0FF);
        y += 10;
        int max = 9;
        int sel = t == null ? 0 : Math.max(0, l.indexOf(t));
        int start = Math.max(0, Math.min(sel - max / 2, l.size() - max));
        for (int i = start; i < Math.min(l.size(), start + max); i++) {
            Entry e = l.get(i);
            double d = Math.hypot(e.pos.getX() + 0.5 - p.getX(), e.pos.getZ() + 0.5 - p.getZ());
            String line = (e == t ? "> " : "  ") + e.name + " " + (int) d + "m";
            int col = e == t ? 0xFFFF55 : (e.snowy ? 0x66E0FF : 0xFFFFFF);
            ctx.drawTextWithShadow(tr, line, w - 4 - tr.getWidth(line), y, col);
            y += 10;
        }
        if (l.size() > max) {
            String more = (sel + 1) + "/" + l.size();
            ctx.drawTextWithShadow(tr, more, w - 4 - tr.getWidth(more), y, 0xAAAAAA);
        }
    }

    private static void arrow(DrawContext ctx, int cx, int cy, float angleDeg, int color) {
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(cx, cy, 0);
        m.scale(1.6f, 1.6f, 1f);
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angleDeg));
        for (int i = 0; i < 7; i++) {
            ctx.fill(-i, -14 + i, i + 1, -13 + i, color);
        }
        ctx.fill(-1, -7, 2, 6, color);
        m.pop();
    }
}
