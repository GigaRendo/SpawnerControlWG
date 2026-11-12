package dev.spawnercontrol;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseSpawner;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Server-thread state describing the spawner currently ticking. */
public final class SpawnerContext {
    public static Rules.Region region;
    public static boolean always;

    public static void clear() { region = null; always = false; }

    public static void tag(Entity e) {
        Rules.Region r = region;
        if (r == null) return;
        if (r.loot != 1.0) e.addTag("sc_loot_" + r.loot);
        if (r.xp != 1.0) e.addTag("sc_xp_" + r.xp);
    }

    public static double mult(Entity e, String prefix) {
        for (String t : e.getTags()) {
            if (t.startsWith(prefix)) {
                try { return Double.parseDouble(t.substring(prefix.length())); } catch (NumberFormatException ignored) {}
            }
        }
        return 1.0;
    }

    private static Field DATA_FIELD;
    private static Method ENTITY_METHOD;

    /** Reads the spawner's current mob id (reflection keeps this independent of record/accessor names). */
    public static String mobId(BaseSpawner spawner) {
        try {
            if (DATA_FIELD == null) {
                DATA_FIELD = BaseSpawner.class.getDeclaredField("nextSpawnData");
                DATA_FIELD.setAccessible(true);
            }
            Object sd = DATA_FIELD.get(spawner);
            if (sd == null) return "";
            if (ENTITY_METHOD == null) {
                try { ENTITY_METHOD = sd.getClass().getMethod("entityToSpawn"); }
                catch (NoSuchMethodException e) { ENTITY_METHOD = sd.getClass().getMethod("getEntityToSpawn"); }
            }
            Object tag = ENTITY_METHOD.invoke(sd);
            if (tag instanceof net.minecraft.nbt.CompoundTag ct) return Rules.norm(ct.getStringOr("id", ""));
        } catch (Exception ignored) {}
        return "";
    }
}
