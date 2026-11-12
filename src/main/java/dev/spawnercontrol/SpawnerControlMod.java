package dev.spawnercontrol;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class SpawnerControlMod implements ModInitializer {
    @Override
    public void onInitialize() {
        Rules.load();
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> SpawnerCommands.register(dispatcher));
        // Mobs that appear while a region-controlled spawner is ticking get loot/xp tags.
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> SpawnerContext.tag(entity));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> Rules.save());
    }
}
