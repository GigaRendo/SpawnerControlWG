package dev.spawnercontrol.mixin;

import dev.spawnercontrol.SpawnerContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ThreadLocalRandom;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow protected abstract void dropFromLootTable(ServerLevel level, DamageSource source, boolean hitByPlayer);
    @Shadow protected abstract void dropExperience(ServerLevel level, @Nullable Entity killer);

    @Unique private static boolean sc$reentry;

    @Unique
    private static int sc$times(double m) {
        int t = (int) Math.floor(m);
        if (ThreadLocalRandom.current().nextDouble() < m - t) t++;
        return t;
    }

    @Inject(method = "dropFromLootTable", at = @At("HEAD"), cancellable = true)
    private void sc$loot(ServerLevel level, DamageSource source, boolean hitByPlayer, CallbackInfo ci) {
        if (sc$reentry) return;
        double m = SpawnerContext.mult((Entity) (Object) this, "sc_loot_");
        if (m == 1.0) return;
        ci.cancel();
        sc$reentry = true;
        try {
            for (int i = sc$times(m); i > 0; i--) dropFromLootTable(level, source, hitByPlayer);
        } finally { sc$reentry = false; }
    }

    @Inject(method = "dropExperience", at = @At("HEAD"), cancellable = true)
    private void sc$xp(ServerLevel level, @Nullable Entity killer, CallbackInfo ci) {
        if (sc$reentry) return;
        double m = SpawnerContext.mult((Entity) (Object) this, "sc_xp_");
        if (m == 1.0) return;
        ci.cancel();
        sc$reentry = true;
        try {
            for (int i = sc$times(m); i > 0; i--) dropExperience(level, killer);
        } finally { sc$reentry = false; }
    }
}
