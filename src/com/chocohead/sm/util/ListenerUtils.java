package com.chocohead.sm.util;

import java.util.stream.Stream;

import com.chocohead.sm.loader.ModLoader;
import com.chocohead.sm.loader.ModMetadata;

public final class ListenerUtils {
	private ListenerUtils() {
	}

	public static <T> Stream<T> getListeners(Class<T> type) {
		return ModLoader.getMods().stream().flatMap(mod -> mod.getListeners(type).stream());
	}

	private static String toSafeString(Object thing) {
		try {
			return thing.toString();
		} catch (Throwable t) {
			ModLoader.LOGGER.error("Threw trying to print instance of " + thing.getClass(), t);
			return thing.getClass().getName();
		}
	}

	public static <T> ModMetadata getOwner(Class<T> listenerType, T listener) {
		for (ModMetadata mod : ModLoader.getMods()) {
			for (T candidate : mod.getListeners(listenerType)) {
				if (candidate == listener) {
					return mod;
				}
			}
		}

		throw new IllegalArgumentException("Could not find owning mod for " + toSafeString(listener) + " (of type " + listenerType + ')');
	}

	public static <T> String findBlame(Class<T> listenerType, T listener) {
		try {
			return '"' + toSafeString(listener) + "\" from " + getOwner(listenerType, listener).getFriendlyName();
		} catch (IllegalArgumentException e) {
			return '"' + toSafeString(listener) + "\" of unknown origins";
		}
	}
}