package com.chocohead.sm.loader;

import net.fabricmc.loader.api.SemanticVersion;

import com.chocohead.sm.util.ImmutableClassToInstancesMap;

class ModBuilder extends CommonModBuilder {
	private final ImmutableClassToInstancesMap.Builder<Object> listeners = ImmutableClassToInstancesMap.builder();

	public ModBuilder(String modID, SemanticVersion version) {
		super(modID, version);
	}

	@SuppressWarnings("unchecked") //Actually not safe, be careful!
	ModBuilder withFudgedGenerics(Class<?> type, Object instance) {
		assert type.isInstance(instance): "Care wasn't taken to end up casting " + instance + " as a " + type;
		return withListener((Class<Object>) type, instance);
	}

	public <T> ModBuilder withListener(Class<T> type, T instance) {
		listeners.put(type, instance);
		return this;
	}

	@Override
	public ModMetadata build() {
		return new ModMetadata(id, name, description, version, license, iconPath, contact, authors.build(), contributors.build(), listeners.immutified().build(),
				mixins.build(), depends.build(), recommends.build(), suggests.build(), conflicts.build(), breaks.build(), custom.build());
	}
}