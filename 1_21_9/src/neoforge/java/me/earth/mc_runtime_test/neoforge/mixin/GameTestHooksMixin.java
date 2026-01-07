package me.earth.mc_runtime_test.neoforge.mixin;

import net.neoforged.neoforge.gametest.GameTestHooks;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameTestHooks.class)
public abstract class GameTestHooksMixin {
    @Inject(method = "isGametestEnabled", at = @At("HEAD"))
    private static void gametestAlwaysEnabled(CallbackInfoReturnable cir) {
        cir.setReturnValue(true);
    }
}
