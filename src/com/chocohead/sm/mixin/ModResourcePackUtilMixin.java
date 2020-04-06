package com.chocohead.sm.mixin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.fabric.impl.resource.loader.ModResourcePackUtil;

import com.chocohead.sm.api.SaltsModMetadata;
import com.chocohead.sm.loader.DevModResourcePack;
import com.chocohead.sm.loader.ModLoader;

@Pseudo
@Mixin(value = ModResourcePackUtil.class, remap = false)
public class ModResourcePackUtilMixin {//Little naughty, but should only be relevant for dev anyway
	@Inject(method = "appendModResourcePacks", at = @At("HEAD"))
	private static void appendExtraPacks(List<ResourcePack> packList, ResourceType type, CallbackInfo callback) {
		for (Entry<SaltsModMetadata, File> entry : ModLoader.getExtraResourcePacks()) {
			try (DevModResourcePack resourcePack = new DevModResourcePack(entry.getValue(), entry.getKey())) {
				//Not actually any effect to closing it, but it makes Eclipse happy so why not
				packList.add(resourcePack);
			} catch (IOException e) {
				throw new AssertionError("Managed to throw in an empty close method?", e);
			}
		}
	}
}