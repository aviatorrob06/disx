package xyz.ar06.disx.client_only;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public record DisxAudioLocation(BlockPos blockPos, ResourceLocation resourceLocation) {
}
