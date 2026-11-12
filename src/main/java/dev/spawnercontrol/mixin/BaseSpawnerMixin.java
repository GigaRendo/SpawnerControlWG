package dev.spawnercontrol.mixin;

import dev.spawnercontrol.Rules;
import dev.spawnercontrol.SpawnerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BaseSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseSpawner.class)
public abstract class BaseSpawnerMixin {
    @Shadow private int minSpawnDelay;
    @Shadow private int maxSpawnDelay;
    @Shadow private int spawnCount;
    @Shadow private int maxNearbyEntities;
    @Shadow private int requiredPlayerRange;
    @Shadow private int spawnRange;

    @Unique private boolean sc$applied;
    @Unique private int sc$minD, sc$maxD, sc$count, sc$maxN, sc$req, sc$sr;

    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private void sc$head(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        SpawnerContext.clear();
        if (Rules.isIdle()) { ci.cancel(); return; }

        String mob = SpawnerContext.mobId((BaseSpawner) (Object) this);
        Rules.Result res = Rules.decide(level.dimension().toString(), pos, mob);
        if (!res.enabled()) { ci.cancel(); return; }

        Rules.Region r = res.region();
        if (r == null) return; // enabled by global whitelist / default: vanilla behaviour

        sc$applied = true;
        sc$minD = minSpawnDelay; sc$maxD = maxSpawnDelay; sc$count = spawnCount;
        sc$maxN = maxNearbyEntities; sc$req = requiredPlayerRange; sc$sr = spawnRange;

        requiredPlayerRange = r.always ? 1_000_000 : (r.range != null ? r.range : Rules.data.defaultRange);
        if (r.count != null) spawnCount = r.count;
        if (r.minDelay != null) minSpawnDelay = r.minDelay;
        if (r.maxDelay != null) maxSpawnDelay = Math.max(r.maxDelay, minSpawnDelay);
        if (r.maxNearby != null) maxNearbyEntities = r.maxNearby;
        if (r.spawnRange != null) spawnRange = r.spawnRange;

        SpawnerContext.region = r;
        SpawnerContext.always = r.always;
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private void sc$tail(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        if (sc$applied) {
            minSpawnDelay = sc$minD; maxSpawnDelay = sc$maxD; spawnCount = sc$count;
            maxNearbyEntities = sc$maxN; requiredPlayerRange = sc$req; spawnRange = sc$sr;
            sc$applied = false;
        }
        SpawnerContext.clear();
    }

    /** "Works with no players nearby": skip the vanilla player check. */
    @Inject(method = "isNearPlayer", at = @At("HEAD"), cancellable = true, require = 0)
    private void sc$near(CallbackInfoReturnable<Boolean> cir) {
        if (SpawnerContext.always) cir.setReturnValue(true);
    }
}
