package com.chocohead.ctr.mixins;

import net.minecraft.item.ItemGroup;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemGroup.class)
public interface ItemGroupMixin {
	@Accessor
	void setIndex(int index);

	@Mixin(targets = "net/minecraft/item/ItemGroup$3")
	abstract class NotSoSpecialNowMixin extends ItemGroup {
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