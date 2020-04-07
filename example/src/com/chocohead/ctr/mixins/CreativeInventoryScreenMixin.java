package com.chocohead.ctr.mixins;

import org.objectweb.asm.Opcodes;

import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.item.ItemGroup;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.fabric.mixin.item.group.client.MixinCreativePlayerInventoryGui;

import com.chocohead.ctr.Fun;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin {
	@Shadow
	private static int selectedTab;

	@Dynamic(mixin = MixinCreativePlayerInventoryGui.class)
	private static int fabric_currentPage = 0;

	//@Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/AbstractInventoryScreen;init()V", remap = true), remap = false)
	@Inject(method = "init",
			at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/ingame/CreativeInventoryScreen;selectedTab:I", opcode = Opcodes.GETSTATIC, remap = true),
			remap = false)
	private void init(CallbackInfo callback) {
		ItemGroup[] tabs = Fun.shuffle(ItemGroup.GROUPS);

		for (int i = 0; i < tabs.length; i++) {
			((ItemGroupAccess) tabs[i]).setIndex(i);
			ItemGroup.GROUPS[i] = tabs[i];
		}

		selectedTab = ItemGroup.BUILDING_BLOCKS.getIndex();
		fabric_currentPage = selectedTab / 12;
	}
}