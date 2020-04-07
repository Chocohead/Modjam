package com.chocohead.sm.loader;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.metadata.ModDependency;

import com.chocohead.sm.api.DescriptivePerson;
import com.chocohead.sm.api.ProjectContact;
import com.chocohead.sm.util.ClassToInstancesMap;

public class ModMetadata extends CommonModMetadata {
	private final Map<Class<? extends Object>, List<? extends Object>> realListeners = new IdentityHashMap<>();
	private final Map<String, List<String>> protoListeners;

	ModMetadata(String id, String name, String description, SemanticVersion version, String license, String iconPath,
				ProjectContact contact, Collection<DescriptivePerson> authors, Collection<DescriptivePerson> contributors,
				ClassToInstancesMap<Object> listeners, List<String> mixins, Collection<ModDependency> depends,
				Collection<ModDependency> recommends, Collection<ModDependency> suggests, Collection<ModDependency> conflicts,
				Collection<ModDependency> breaks, Map<String, String> custom) {
		super(id, name, description, version, license, iconPath, contact, authors, contributors, mixins, depends, recommends, suggests, conflicts, breaks, custom);

		protoListeners = Collections.emptyMap();
		for (Entry<Class<? extends Object>, List<? extends Object>> entry : listeners.entrySet()) {
			realListeners.put(entry.getKey(), entry.getValue());
		}
	}

	ModMetadata(ProtoModMetadata proto, Map<String, List<String>> listeners) {
		super(proto);

		protoListeners = new HashMap<>(listeners); //We need it mutable
	}

	@SuppressWarnings("unchecked") //Much lying needs to be done to the compiler
	public <T> List<? extends T> getListeners(Class<T> type) {
		List<?> listeners = realListeners.get(type);
		if (listeners != null) return (List<? extends T>) listeners;

		if (!protoListeners.containsKey(type.getName())) return Collections.emptyList();
		assert protoListeners.get(type.getName()) != null; //This would be bad

		ModLoader.LOGGER.info("Building listeners for " + type);
		listeners = protoListeners.remove(type.getName()).stream().map(className -> {
			Class<?> listener;
			try {
				listener = Class.forName(className);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("Unable to find listener " + className + " (of type " + type + ") for " + getId(), e);
			}

			try {
				return listener.asSubclass(type).newInstance(); //Attempt to assert the real class is right
			} catch (ReflectiveOperationException e) {
				throw new IllegalArgumentException("Unable to create listener " + listener + " (of type " + type + ") for " + getId(), e);
			}
		}).collect(Collectors.toList());
		realListeners.put(type, listeners);

		return (List<? extends T>) listeners;
	}
}