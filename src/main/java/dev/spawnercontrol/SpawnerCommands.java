package dev.spawnercontrol;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class SpawnerCommands {
    private static final SuggestionProvider<CommandSourceStack> REGIONS = (c, b) -> {
        List<String> ids = new ArrayList<>();
        for (Rules.Region r : Rules.data.regions) ids.add(r.id);
        return SharedSuggestionProvider.suggest(ids, b);
    };
    private static final SuggestionProvider<CommandSourceStack> MOBS = (c, b) -> {
        List<String> l = new ArrayList<>();
        for (Object k : BuiltInRegistries.ENTITY_TYPE.keySet()) l.add(k.toString().replace("minecraft:", ""));
        return SharedSuggestionProvider.suggest(l, b);
    };

    private static void msg(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("spawnercontrol")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));

        // /sc <x y z> <radius> [range]   and   /sc add <x y z> <radius> [range]
        root.then(creation());
        root.then(Commands.literal("add").then(creation()));

        root.then(Commands.literal("remove").then(Commands.argument("id", StringArgumentType.word()).suggests(REGIONS)
                .executes(c -> {
                    Rules.Region r = Rules.byId(StringArgumentType.getString(c, "id"));
                    if (r == null) return fail(c);
                    Rules.data.regions.remove(r); Rules.save();
                    msg(c, "Removed region " + r.id); return 1;
                })));

        root.then(Commands.literal("list").executes(c -> {
            if (Rules.data.regions.isEmpty()) msg(c, "No regions. Default: " + (Rules.data.defaultEnabled ? "spawners ON" : "spawners OFF"));
            for (Rules.Region r : Rules.data.regions)
                msg(c, r.id + ": " + Rules.dimName(r.dim) + " " + r.x + " " + r.y + " " + r.z + " r=" + r.radius
                        + " range=" + (r.always ? "always" : r.range == null ? "default" : r.range));
            return 1;
        }));

        root.then(Commands.literal("info").then(Commands.argument("id", StringArgumentType.word()).suggests(REGIONS)
                .executes(c -> {
                    Rules.Region r = Rules.byId(StringArgumentType.getString(c, "id"));
                    if (r == null) return fail(c);
                    msg(c, r.id + " @ " + Rules.dimName(r.dim) + " " + r.x + " " + r.y + " " + r.z + " radius " + r.radius);
                    msg(c, "range=" + (r.range == null ? "default(" + Rules.data.defaultRange + ")" : r.range) + " always=" + r.always
                            + " count=" + r.count + " minDelay=" + r.minDelay + " maxDelay=" + r.maxDelay
                            + " maxNearby=" + r.maxNearby + " spawnRange=" + r.spawnRange);
                    msg(c, "loot x" + r.loot + ", xp x" + r.xp + ", mobs=" + (r.mobs.isEmpty() ? "any" : r.mobs));
                    return 1;
                })));

        // /sc edit <id> <property> ...
        RequiredArgumentBuilder<CommandSourceStack, String> id = Commands.argument("id", StringArgumentType.word()).suggests(REGIONS);
        id.then(intProp("radius", 0, 512, false, (r, v) -> r.radius = v));
        id.then(intProp("range", 0, 1_000_000, true, (r, v) -> r.range = v));
        id.then(intProp("count", 1, 64, true, (r, v) -> r.count = v));
        id.then(intProp("mindelay", 1, 1_000_000, true, (r, v) -> r.minDelay = v));
        id.then(intProp("maxdelay", 1, 1_000_000, true, (r, v) -> r.maxDelay = v));
        id.then(intProp("maxnearby", 1, 1000, true, (r, v) -> r.maxNearby = v));
        id.then(intProp("spawnrange", 1, 32, true, (r, v) -> r.spawnRange = v));
        id.then(Commands.literal("always").then(Commands.argument("value", BoolArgumentType.bool())
                .executes(c -> edit(c, r -> r.always = BoolArgumentType.getBool(c, "value")))));
        id.then(Commands.literal("noai").then(Commands.argument("value", BoolArgumentType.bool())
        .executes(c -> edit(c, r -> r.noai = BoolArgumentType.getBool(c, "value")))));
        id.then(Commands.literal("loot").then(Commands.argument("value", DoubleArgumentType.doubleArg(0, 100))
                .executes(c -> edit(c, r -> r.loot = DoubleArgumentType.getDouble(c, "value")))));
        id.then(Commands.literal("xp").then(Commands.argument("value", DoubleArgumentType.doubleArg(0, 100))
                .executes(c -> edit(c, r -> r.xp = DoubleArgumentType.getDouble(c, "value")))));
        id.then(Commands.literal("mob")
                .then(Commands.literal("add").then(Commands.argument("mob", StringArgumentType.greedyString()).suggests(MOBS)
                        .executes(c -> edit(c, r -> r.mobs.add(Rules.norm(StringArgumentType.getString(c, "mob")))))))
                .then(Commands.literal("remove").then(Commands.argument("mob", StringArgumentType.greedyString()).suggests(MOBS)
                        .executes(c -> edit(c, r -> r.mobs.remove(Rules.norm(StringArgumentType.getString(c, "mob")))))))
                .then(Commands.literal("clear").executes(c -> edit(c, r -> r.mobs.clear()))));
        root.then(Commands.literal("edit").then(id));

        // Global whitelist: /sc mob allow|deny|list|clear
        root.then(Commands.literal("mob")
                .then(Commands.literal("allow").then(Commands.argument("mob", StringArgumentType.greedyString()).suggests(MOBS)
                        .executes(c -> {
                            String m = Rules.norm(StringArgumentType.getString(c, "mob"));
                            Rules.data.globalMobs.add(m); Rules.save();
                            msg(c, "Spawners of " + m + " now work everywhere"); return 1;
                        })))
                .then(Commands.literal("deny").then(Commands.argument("mob", StringArgumentType.greedyString()).suggests(MOBS)
                        .executes(c -> {
                            String m = Rules.norm(StringArgumentType.getString(c, "mob"));
                            Rules.data.globalMobs.remove(m); Rules.save();
                            msg(c, "Removed " + m + " from the global whitelist"); return 1;
                        })))
                .then(Commands.literal("clear").executes(c -> { Rules.data.globalMobs.clear(); Rules.save(); msg(c, "Global whitelist cleared"); return 1; }))
                .then(Commands.literal("list").executes(c -> { msg(c, "Global whitelist: " + Rules.data.globalMobs); return 1; })));

        root.then(Commands.literal("default").then(Commands.argument("enabled", BoolArgumentType.bool()).executes(c -> {
            Rules.data.defaultEnabled = BoolArgumentType.getBool(c, "enabled"); Rules.save();
            msg(c, "All spawners are now " + (Rules.data.defaultEnabled ? "ENABLED" : "DISABLED") + " by default"); return 1;
        })));
        root.then(Commands.literal("defaultrange").then(Commands.argument("blocks", IntegerArgumentType.integer(0, 1_000_000)).executes(c -> {
            Rules.data.defaultRange = IntegerArgumentType.getInteger(c, "blocks"); Rules.save();
            msg(c, "Default player range = " + Rules.data.defaultRange); return 1;
        })));
        root.then(Commands.literal("reload").executes(c -> { Rules.load(); msg(c, "Reloaded config"); return 1; }));

        var node = d.register(root);
        d.register(Commands.literal("sc").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).redirect(node));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> creation() {
        return Commands.argument("pos", BlockPosArgument.blockPos())
                .then(Commands.argument("radius", IntegerArgumentType.integer(0, 512))
                        .executes(c -> create(c, null))
                        .then(Commands.argument("range", IntegerArgumentType.integer(0, 1_000_000))
                                .executes(c -> create(c, IntegerArgumentType.getInteger(c, "range")))));
    }

    private static int create(CommandContext<CommandSourceStack> c, Integer range) {
        BlockPos p = BlockPosArgument.getBlockPos(c, "pos");
        Rules.Region r = new Rules.Region();
        r.id = "region" + Rules.data.nextId++;
        r.dim = c.getSource().getLevel().dimension().toString();
        r.x = p.getX(); r.y = p.getY(); r.z = p.getZ();
        r.radius = IntegerArgumentType.getInteger(c, "radius");
        r.range = range;
        Rules.data.regions.add(r);
        Rules.save();
        msg(c, "Created " + r.id + ": spawners within " + r.radius + " blocks of " + r.x + " " + r.y + " " + r.z
                + " now work (player range " + (range == null ? "default " + Rules.data.defaultRange : range) + ")");
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> intProp(String name, int min, int max, boolean nullable,
                                                                     BiConsumer<Rules.Region, Integer> setter) {
        LiteralArgumentBuilder<CommandSourceStack> lit = Commands.literal(name)
                .then(Commands.argument("value", IntegerArgumentType.integer(min, max))
                        .executes(c -> edit(c, r -> setter.accept(r, IntegerArgumentType.getInteger(c, "value")))));
        if (nullable) lit.then(Commands.literal("default").executes(c -> edit(c, r -> setter.accept(r, null))));
        return lit;
    }

    private static int edit(CommandContext<CommandSourceStack> c, Consumer<Rules.Region> action) {
        Rules.Region r = Rules.byId(StringArgumentType.getString(c, "id"));
        if (r == null) return fail(c);
        action.accept(r);
        Rules.save();
        msg(c, "Updated " + r.id);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> c) {
        c.getSource().sendFailure(Component.literal("No such region"));
        return 0;
    }
}
