package com.chocohead.sm.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.loader.api.SemanticVersion;

/** This is loaded on the pre-Mixin phase of Knot, <b>BE VERY CAREFUL WHAT IS LOADED</b> */
class ProtoModBuilder extends CommonModBuilder {
	private final Map<String, List<String>> listeners = new HashMap<>();

	public ProtoModBuilder(String modID, SemanticVersion version) {
		super(modID, version);
	}

	public <T> ProtoModBuilder withListener(String type, String instance) {
		listeners.computeIfAbsent(type, k -> new ArrayList<>()).add(instance);
		return this;
	}

	@Override
	public ProtoModMetadata build() {
		return new ProtoModMetadata(id, name, description, version, license, iconPath, contact, authors.build(), contributors.build(),
				Collections.unmodifiableMap(listeners), mixins.build(), depends.build(), recommends.build(), suggests.build(), conflicts.build(), breaks.build(), custom.build());
	}
}