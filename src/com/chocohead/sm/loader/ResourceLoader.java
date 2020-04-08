package com.chocohead.sm.loader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;

import net.devtech.rrp.api.RuntimeResourcePack;

@PreMixinClassloaded
final class ResourceLoader extends Thread {
	private final ReentrantLock lock = new ReentrantLock(true);
	private final Condition mapFilled = lock.newCondition();
	private final Condition loadingCleared = lock.newCondition();
	private final Map<String, byte[]> allNameToContents = new HashMap<>();
	private volatile Map<String, byte[]> currentNameToContents;
	private volatile boolean allPresent, classLoadClear;

	public ResourceLoader() {
		super("Salts Mill Resource Loader");

		setDaemon(true); //Don't hang the game if it dies before we finish
		start(); //Start immediately, we want that lock!
	}

	void giveFiles(Map<String, byte[]> nameToContents) {
		giveFiles(nameToContents, false);
	}

	void complete() {
		giveFiles(Collections.emptyMap(), true);
	}

	private void giveFiles(Map<String, byte[]> nameToContents, boolean complete) {
		assert !lock.isHeldByCurrentThread();
		lock.lock();

		try {
			assert !allPresent;

			assert currentNameToContents == null;
			currentNameToContents = nameToContents;

			if (complete) allPresent = true;
			mapFilled.signal();
		} finally {
			lock.unlock();
		}
	}

	/** Signal that the Mixin transformer is now running so it is safe to classload Minecraft types */
	void clearClassLoading() {
		assert !lock.isHeldByCurrentThread();
		lock.lock();

		try {
			assert !classLoadClear;
			classLoadClear = true;
			loadingCleared.signal();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void run() {
		try {
			assert !lock.isHeldByCurrentThread();
			lock.lock();

			while (!allPresent || currentNameToContents != null) {
				while (currentNameToContents == null) {
					try {
						mapFilled.await(); //Awaiting the condition yields the lock as the thread sleeps
					} catch (InterruptedException e) {
						throw new RuntimeException("Unexpected interruption", e);
					}
				}
				assert lock.isHeldByCurrentThread(); //We're back awake so the look is ours again now

				allNameToContents.putAll(currentNameToContents);
				currentNameToContents = null;
			}

			Map<String, Map<String, Map<String, byte[]>>> typeToNamespace = new HashMap<>();

			for (Entry<String, byte[]> entry : allNameToContents.entrySet()) {
				String file = entry.getKey();
				if (countMatches(file, '/') < 2) continue; //Expecting at least [assets/data]/namespace/***

				int split = file.indexOf('/');
				String type = file.substring(0, split++);

				int splitter = file.indexOf('/', split);
				String namespace = file.substring(split, splitter++);

				typeToNamespace.computeIfAbsent(type, k -> new HashMap<>()).computeIfAbsent(namespace, k -> new HashMap<>()).put(file.substring(splitter), entry.getValue());
			}

			while (!classLoadClear) {
				try {
					loadingCleared.await();
				} catch (InterruptedException e) {
					throw new RuntimeException("Unexpected interruption", e);
				}
			}

			load(typeToNamespace); //Delay the class loading of the Minecraft classes until the Mixin transformer is on
		} finally {
			lock.unlock(); //No need for this anymore
		}

		allNameToContents.clear();
	}

	private static int countMatches(String s, char c) {
		int result = 0;
		while (s.indexOf(c) != -1) {
			result += 1;
			s = s.substring(result + 1);
		}
		return result;
	}

	private static void load(Map<String, Map<String, Map<String, byte[]>>> typeToNamespace) {
		for (ResourceType type : ResourceType.values()) {
			Map<String, Map<String, byte[]>> namespaceToAssets = typeToNamespace.get(type.getDirectory());
			if (namespaceToAssets == null) continue; //None of these apparently

			assert !namespaceToAssets.isEmpty(); //Shouldn't be
			for (Entry<String, Map<String, byte[]>> entry : namespaceToAssets.entrySet()) {
				String namespace = entry.getKey();

				assert !entry.getValue().isEmpty(); //Shouldn't be either
				for (Entry<String, byte[]> asset : entry.getValue().entrySet()) {
					try {
						RuntimeResourcePack.INSTANCE.addAsyncResource(new Identifier(namespace, asset.getKey()), asset::getValue);
					} catch (InvalidIdentifierException e) {
						String typeName;
						switch (type) {
						case CLIENT_RESOURCES:
							typeName = "asset";
							break;

						case SERVER_DATA:
							typeName = "datapack entry";
							break;

						default:
							typeName = "mystical " + type; //Most mysterious
							break;
						}
						ModLoader.LOGGER.warn("Invalid " + typeName + " found in a cassette: \"" + namespace + "\":\"" + asset.getKey() + '"');
					}
				}
			}
		}
	}
}