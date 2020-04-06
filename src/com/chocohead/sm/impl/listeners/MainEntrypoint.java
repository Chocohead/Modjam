package com.chocohead.sm.impl.listeners;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;

import com.chocohead.sm.api.listeners.AsyncListener.Synchroniser;
import com.chocohead.sm.api.listeners.GameStartupListener;
import com.chocohead.sm.api.listeners.GameStartupListener.GameStartupAsyncListener;
import com.chocohead.sm.loader.ModLoader;

public class MainEntrypoint implements ClientModInitializer, DedicatedServerModInitializer {
	private static final GameStartupListener ASYNC_COMPLETE = () -> {};

	private static <T> Stream<T> getListeners(Class<T> type) {
		return ModLoader.getMods().stream().flatMap(mod -> mod.getListeners(type).stream());
	}

	@Override
	public void onInitializeClient() {
		ModLoader.LOGGER.info("Loading Salts Mill [client side]");
		initialiseListeners();
	}

	@Override
	public void onInitializeServer() {
		ModLoader.LOGGER.info("Loading Salts Mill [server side]");
		initialiseListeners();
	}

	private void initialiseListeners() {
		BlockingQueue<GameStartupListener> listeners = getListeners(GameStartupListener.class).collect(Collectors.toCollection(LinkedBlockingQueue::new));

		Futures.addCallback(ModLoader.THREAD_POOL.submit(() -> {
			return getListeners(GameStartupAsyncListener.class).map(listener -> {
				Synchroniser syncer = task -> listeners.add(task::run);

				return ModLoader.THREAD_POOL.submit(() -> listener.onGameStart(syncer));
			}).collect(Collectors.toList());
		}), new FutureCallback<List<ListenableFuture<?>>>() {
			@Override
			public void onSuccess(List<ListenableFuture<?>> futures) {
				if (futures.isEmpty()) {
					listeners.add(ASYNC_COMPLETE);
				} else {
					Futures.whenAllSucceed(futures).call(() -> listeners.add(ASYNC_COMPLETE), ModLoader.THREAD_POOL);
				}
			}

			@Override
			public void onFailure(Throwable t) {
				//Failed to even schedule them all, not ideal
				throw new RuntimeException("Error preparing initialisers", t);
			}
		});

		boolean clearToEnd = false;
		GameStartupListener listener;
		do {
			try {
				listener = listeners.take();
			} catch (InterruptedException e) {
				throw new RuntimeException("Unexpected interruption", e);
			}

			listener.onGameStart();
		} while (!(clearToEnd |= listener == ASYNC_COMPLETE) && !listeners.isEmpty());
	}
}