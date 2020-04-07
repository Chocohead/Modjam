package com.chocohead.ctr.mixins;

import net.minecraft.item.ItemGroup;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.fabricmc.fabric.impl.item.group.FabricCreativeGuiComponents;

@Mixin(value = ItemGroup.class, priority = 500)
abstract class ItemGroupMixin {
	@Shadow
	public abstract int getIndex();

	@Shadow
	public abstract boolean isTopRow();

	@Inject(method = "isTopRow", cancellable = true, at = @At("HEAD"))
	private void isTopRow(CallbackInfoReturnable<Boolean> info) {
		if (getIndex() > 11) {
			info.setReturnValue((getIndex() - 12) % (12 - FabricCreativeGuiComponents.COMMON_GROUPS.size()) < 5);
		}
	}

	@Inject(method = "getColumn", cancellable = true, at = @At("HEAD"))
	private void getColumn(CallbackInfoReturnable<Integer> info) {
		if (getIndex() > 11) {
			int column = (getIndex() - 12) % (12 - FabricCreativeGuiComponents.COMMON_GROUPS.size());
			info.setReturnValue(isTopRow() ? column : column - 5);
		}
	}

	@Mixin(targets = "net/minecraft/item/ItemGroup$3")
	static abstract class NotSoSpecialNowMixin extends ItemGroup {
		public NotSoSpecialNowMixin() {
			super(0, null);

			throw new AssertionError("Constructed a Mixin?");
		}

		/**
		 * @reason The toolbar creative tab thinks it is above the others in being special
		 *
		 * @author Chocohead
		 *
		 * @see AnnotatedMixinElementHandlerOverwrite#registerOverwrite Where Mixin complains about this
		 */
		@Override
		@Overwrite
		public boolean isSpecial() {
			return super.isSpecial();
		}
	}
}