package com.chocohead.ctr;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.item.ItemGroup;

import net.fabricmc.fabric.impl.item.group.FabricCreativeGuiComponents;
import net.fabricmc.loader.api.FabricLoader;

import com.chocohead.sm.api.listeners.GameStartupListener.GameStartupAsyncListener;

public class Fun implements GameStartupAsyncListener {
	public static final int SEARCH = ItemGroup.SEARCH.getIndex();
	public static final int INVENTORY = ItemGroup.INVENTORY.getIndex();

	@Override
	public void onGameStart(Synchroniser syncer) {
		Logger logger = LogManager.getLogger();

		if (FabricLoader.getInstance().isModLoaded("fabric-item-groups-v0")) {
			logger.info("Preparing fun");

			try {
				FabricCreativeGuiComponents.COMMON_GROUPS.remove(ItemGroup.HOTBAR); //Don't need this
			} catch (Throwable t) {
				logger.error("Accidents were had during fun", t);
			}
		}

		logger.info("Fun prepared");
	}

	public static <T> T[] shuffle(T[] array) {
        List<T> list = Arrays.asList(array);

        Collections.shuffle(list);
        Collections.swap(list, list.indexOf(ItemGroup.SEARCH), SEARCH);
        Collections.swap(list, list.indexOf(ItemGroup.INVENTORY), INVENTORY);

        return list.toArray(array);
    }
}