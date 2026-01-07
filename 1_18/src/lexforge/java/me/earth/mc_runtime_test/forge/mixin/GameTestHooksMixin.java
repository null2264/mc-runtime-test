package me.earth.mc_runtime_test.neoforge.mixin;

import net.minecraftforge.gametest.ForgeGameTestHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ForgeGameTestHooks.class)
public abstract class GameTestHooksMixin {
    @Inject(method = "isGametestEnabled", at = @At("HEAD"))
    private static void gametestAlwaysEnabled(CallbackInfoReturnable cir) {
        cir.setReturnValue(true);
    }
}
