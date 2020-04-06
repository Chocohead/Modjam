package com.chocohead.sm.loader;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.metadata.ModDependency;

import com.chocohead.sm.api.DescriptivePerson;
import com.chocohead.sm.api.ProjectContact;
import com.chocohead.sm.util.ClassToInstancesMap;

public class ModMetadata extends CommonModMetadata {
	private final ClassToInstancesMap<Object> listeners;

	ModMetadata(String id, String name, String description, SemanticVersion version, String license, String iconPath,
				ProjectContact contact, Collection<DescriptivePerson> authors, Collection<DescriptivePerson> contributors,
				ClassToInstancesMap<Object> listeners, List<String> mixins, Collection<ModDependency> depends,
				Collection<ModDependency> recommends, Collection<ModDependency> suggests, Collection<ModDependency> conflicts,
				Collection<ModDependency> breaks, Map<String, String> custom) {
		super(id, name, description, version, license, iconPath, contact, authors, contributors, mixins, depends, recommends, suggests, conflicts, breaks, custom);

		this.listeners = listeners;
	}

	ModMetadata(ProtoModMetadata proto, ClassToInstancesMap<Object> listeners) {
		super(proto);

		this.listeners = listeners;
	}

	public <T> List<? extends T> getListeners(Class<T> type) {
		return listeners.getOrDefault(type, Collections.emptyList());
	}
}