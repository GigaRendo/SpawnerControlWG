package dev.spawnercontrol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class Rules {
    public static final class Region {
        public String id;
        public String dim;
        public int x, y, z, radius;
        public Integer range;          // null = default range
        public boolean always;         // work with no players nearby
        public Integer count, minDelay, maxDelay, maxNearby, spawnRange; // null = spawner's own value
        public double loot = 1.0, xp = 1.0;
        public Set<String> mobs = new LinkedHashSet<>(); // empty = any mob

        boolean contains(String d, BlockPos p) {
            if (!dim.equals(d)) return false;
            long dx = p.getX() - x, dy = p.getY() - y, dz = p.getZ() - z;
            return dx * dx + dy * dy + dz * dz <= (long) radius * radius;
        }
    }

    public static final class Data {
        public boolean defaultEnabled = false;
        public int defaultRange = 16;
        public Set<String> globalMobs = new LinkedHashSet<>();
        public List<Region> regions = new ArrayList<>();
        public int nextId = 1;
    }

    public record Result(boolean enabled, Region region) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Data data = new Data();

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("spawnercontrol.json");
    }

    public static void load() {
        try {
            Path f = file();
            if (Files.exists(f)) {
                Data d = GSON.fromJson(Files.readString(f), Data.class);
                if (d != null) data = d;
            } else save();
        } catch (Exception e) {
            System.err.println("[SpawnerControl] Failed to load config: " + e);
        }
    }

    public static void save() {
        try {
            Files.writeString(file(), GSON.toJson(data));
        } catch (Exception e) {
            System.err.println("[SpawnerControl] Failed to save config: " + e);
        }
    }

    public static String norm(String s) {
        s = s.toLowerCase(Locale.ROOT).trim();
        return s.contains(":") ? s : "minecraft:" + s;
    }

    public static boolean isIdle() {
        return !data.defaultEnabled && data.globalMobs.isEmpty() && data.regions.isEmpty();
    }

    public static Result decide(String dim, BlockPos pos, String mob) {
        Region best = null;
        for (Region r : data.regions) {
            if (!r.contains(dim, pos)) continue;
            if (!r.mobs.isEmpty() && !r.mobs.contains(mob)) continue;
            if (best == null || r.radius < best.radius) best = r; // smallest region wins
        }
        boolean enabled = data.defaultEnabled || best != null || data.globalMobs.contains(mob);
        return new Result(enabled, best);
    }

    public static Region byId(String id) {
        for (Region r : data.regions) if (r.id.equalsIgnoreCase(id)) return r;
        return null;
    }

    public static String dimName(String key) {
        int i = key.lastIndexOf("/ ");
        String s = i >= 0 ? key.substring(i + 2) : key;
        return s.endsWith("]") ? s.substring(0, s.length() - 1) : s;
    }
}
