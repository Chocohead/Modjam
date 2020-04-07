package com.chocohead.ctr.mixins;

import net.minecraft.item.ItemGroup;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemGroup.class)
public interface ItemGroupAccess {
	@Accessor
	void setIndex(int index);
}