package com.chocohead.sm.loader;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.metadata.ModDependency;

import com.chocohead.sm.api.DescriptivePerson;
import com.chocohead.sm.api.ProjectContact;
import com.chocohead.sm.util.ImmutableClassToInstancesMap;
import com.chocohead.sm.util.ImmutableClassToInstancesMap.Builder;

/** This is loaded on the pre-Mixin phase of Knot, <b>BE VERY CAREFUL WHAT IS LOADED</b> */
class ProtoModMetadata extends CommonModMetadata {
	private final Map<String, List<String>> listeners;

	ProtoModMetadata(String id, String name, String description, SemanticVersion version, String license, String iconPath,
				ProjectContact contact, Collection<DescriptivePerson> authors, Collection<DescriptivePerson> contributors,
				Map<String, List<String>> listeners, List<String> mixins, Collection<ModDependency> depends,
				Collection<ModDependency> recommends, Collection<ModDependency> suggests, Collection<ModDependency> conflicts,
				Collection<ModDependency> breaks, Map<String, String> custom) {
		super(id, name, description, version, license, iconPath, contact, authors, contributors, mixins, depends, recommends, suggests, conflicts, breaks, custom);

		this.listeners = listeners;
	}

	public Map<String, List<String>> getProtoListeners() {
		return listeners;
	}

	public ModMetadata convert() {
		Builder<Object> listeners = ImmutableClassToInstancesMap.builder();

		for (Entry<String, List<String>> entry : this.listeners.entrySet()) {
			Class<?> listenerType;
			try {
				listenerType = Class.forName(entry.getKey());
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("Unable to find listener with type " + entry.getKey() + " for " + getId(), e);
			}

			for (String className : entry.getValue()) {

				Class<?> listener;
				try {
					listener = Class.forName(className);
				} catch (ClassNotFoundException e) {
					throw new IllegalArgumentException("Unable to find listener " + className + " (of type " + listenerType + ") for " + getId(), e);
				}

				try {//The wildcards are a little too wild for javac to be able to realise the ? is the same as ?
					fudgeGenerics(listeners, listenerType, listener.asSubclass(listenerType).newInstance());
				} catch (ReflectiveOperationException e) {
					throw new IllegalArgumentException("Unable to create listener " + listener + " (of type " + listenerType + ") for " + getId(), e);
				}
			}
		}

		return new ModMetadata(this, listeners.immutified().build());
	}

	@SuppressWarnings("unchecked") //Actually not safe, be careful!
	private static void fudgeGenerics(Builder<Object> listeners, Class<?> type, Object instance) {
		assert type.isInstance(instance): "Care wasn't taken to end up casting " + instance + " as a " + type;
		listeners.put((Class<Object>) type, instance);
	}
}