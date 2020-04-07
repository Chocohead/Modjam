package com.chocohead.sm.impl.listeners;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;

import com.chocohead.sm.api.listeners.AsyncListener.Synchroniser;
import com.chocohead.sm.api.listeners.GameStartupListener;
import com.chocohead.sm.api.listeners.GameStartupListener.GameStartupAsyncListener;
import com.chocohead.sm.loader.ModLoader;
import com.chocohead.sm.util.ListenerUtils;

public class MainEntrypoint implements ClientModInitializer, DedicatedServerModInitializer {
	/** Wraps any {@link Exception}s thrown whilst running asynchronous listeners
	 *  @since 0.4
	 */
	private static class ExecutionAccident extends RuntimeException {
		private static final long serialVersionUID = -1053497723326621446L;

		public ExecutionAccident(String message, Throwable cause) {
	        super(message, cause);
	    }
	}
	private static final GameStartupListener ASYNC_COMPLETE = () -> {};

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
		BlockingQueue<GameStartupListener> listeners;
		try {
			listeners = ListenerUtils.getListeners(GameStartupListener.class).collect(Collectors.toCollection(LinkedBlockingQueue::new));
		} catch (Throwable t) {
			assert !(t instanceof ExecutionAccident);
			throw new ExecutionAccident("Error preparing sync game startup listeners", t);
		}

		Futures.addCallback(ModLoader.THREAD_POOL.submit(() -> {
			return ListenerUtils.getListeners(GameStartupAsyncListener.class).map(listener -> {
				Synchroniser syncer = task -> listeners.add(task::run);

				return ModLoader.THREAD_POOL.submit(() -> {
					try {
						listener.onGameStart(syncer);
					} catch (Throwable t) {
						throw new ExecutionAccident("Error running async game startup listener " + ListenerUtils.findBlame(GameStartupAsyncListener.class, listener), t);
					}
				});
			}).collect(Collectors.toList());
		}), new FutureCallback<List<ListenableFuture<?>>>() {
			@Override
			public void onSuccess(List<ListenableFuture<?>> futures) {
				if (futures.isEmpty()) {
					listeners.add(ASYNC_COMPLETE); //Short cut
				} else {
					ListenableFuture<?> all = futures.size() > 1 ? Futures.allAsList(futures) : Iterables.getOnlyElement(futures);
					Futures.addCallback(all, new FutureCallback<Object>() {
						@Override
						public void onSuccess(Object result) {
							listeners.add(ASYNC_COMPLETE);
						}

						@Override
						public void onFailure(Throwable t) {
							listeners.add(() -> {//Failed whilst running a listener, bubble it back up
								if (t instanceof ExecutionAccident) {
									throw (ExecutionAccident) t;
								} else {
									throw new ExecutionAccident("Error running async game startup listeners", t);
								}
							});
						}
					}, ModLoader.THREAD_POOL);
				}
			}

			@Override
			public void onFailure(Throwable t) {
				listeners.add(() -> {//Failed to even schedule them all, not ideal
					assert !(t instanceof ExecutionAccident);
					throw new ExecutionAccident("Error preparing async game startup listeners", t);
				});
			}
		}, ModLoader.THREAD_POOL);

		boolean clearToEnd = false;
		GameStartupListener listener;
		do {
			try {
				listener = listeners.take();
			} catch (InterruptedException e) {
				throw new RuntimeException("Unexpected interruption waiting for game startup listeners", e);
			}

			try {
				listener.onGameStart();
			} catch (ExecutionAccident e) {
				throw e;
			} catch (Throwable t) {//If we're here it's not an async listener problem
				throw new ExecutionAccident("Error running sync game startup listener " + ListenerUtils.findBlame(GameStartupListener.class, listener), t);
			}
		} while (!(clearToEnd |= listener == ASYNC_COMPLETE) && !listeners.isEmpty());
	}
}