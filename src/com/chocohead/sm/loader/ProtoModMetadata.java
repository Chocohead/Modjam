package com.chocohead.sm.loader;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.metadata.ModDependency;

import com.chocohead.sm.api.DescriptivePerson;
import com.chocohead.sm.api.ProjectContact;

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
		return new ModMetadata(this, listeners);
	}
}