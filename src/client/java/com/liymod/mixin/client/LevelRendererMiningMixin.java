package com.liymod.mixin.client;
import com.liymod.client.mining.LoliRangeMiningClient;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMiningMixin {
    @Unique private Set<Long> liymod$immediateSections = Set.of();
    @Inject(method = "compileSections", at = @At("HEAD"))
    private void liymod$takeMiningBatch(Camera camera, CallbackInfo ci) {
        liymod$immediateSections = LoliRangeMiningClient.takeImmediateSections(Minecraft.getInstance().level);
    }
    @Redirect(method = "compileSections", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;rebuildSectionAsync(Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;Lnet/minecraft/client/renderer/chunk/RenderRegionCache;)V"))
    private void liymod$compileMiningBatchTogether(SectionRenderDispatcher.RenderSection section,
            SectionRenderDispatcher dispatcher, RenderRegionCache cache) {
        var origin = section.getOrigin();
        if (liymod$immediateSections.contains(SectionPos.asLong(origin.getX() >> 4, origin.getY() >> 4, origin.getZ() >> 4)))
            section.compileSync(cache);
        else section.rebuildSectionAsync(dispatcher, cache);
    }
}
