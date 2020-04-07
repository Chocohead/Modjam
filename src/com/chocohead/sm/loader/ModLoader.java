package com.chocohead.sm.loader;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import net.devtech.rrp.entrypoint.RRPPreGenEntrypoint;

import com.chocohead.sm.api.SaltsModMetadata;

public class ModLoader implements PreLaunchEntrypoint, RRPPreGenEntrypoint {
	private enum TitusThreadFactory implements ThreadFactory {
		INSTANCE;

		@Override
		public Thread newThread(Runnable task) {
			Thread thread = new Thread(group, task, "Salts-Mill-Worker: " + number.getAndIncrement());

			thread.setDaemon(true);
			thread.setPriority(Thread.NORM_PRIORITY);

			return thread;
		}

		private final ThreadGroup group = Thread.currentThread().getThreadGroup();
		private static final AtomicInteger number = new AtomicInteger(1);
	}
	public static final Logger LOGGER = LogManager.getLogger();
	public static final ListeningExecutorService THREAD_POOL = MoreExecutors.listeningDecorator(new ThreadPoolExecutor(0, Math.max(Runtime.getRuntime().availableProcessors() * 2, 4),
																								30L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), TitusThreadFactory.INSTANCE));

	@Override
	public void register() {
		try {
			PreLoader.resourceLoader.join(); //Block until we're done loading in all the cassettes
		} catch (InterruptedException e) {
			throw new RuntimeException("Unexpected interruption", e);
		}

		PreLoader.resourceLoader = null; //All done, no more need for this
	}

	@Override
	public void onPreLaunch() {
		PreLoader.resourceLoader.clearClassLoading();
	}

	public static List<ModMetadata> getMods() {
		return Collections.unmodifiableList(PreLoader.MODS);
	}

	public static List<Entry<SaltsModMetadata, File>> getExtraResourcePacks() {
		return Collections.unmodifiableList(PreLoader.EXTRA_RESOURCE_PACKS);
	}
}