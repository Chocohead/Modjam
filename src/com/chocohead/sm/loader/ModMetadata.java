package com.chocohead.sm.loader;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;

import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.metadata.ModDependency;

import com.chocohead.sm.api.DescriptivePerson;
import com.chocohead.sm.api.ProjectContact;
import com.chocohead.sm.api.SaltsModMetadata;
import com.chocohead.sm.impl.SortedModDependency;
import com.chocohead.sm.impl.SortedModDependency.Ordered;

@PreMixinClassloaded
public class ModMetadata implements SaltsModMetadata {
	private final String id, name, description;
	private final SemanticVersion version;
	private final String license;
	private final String iconPath;

	private final ProjectContact contact;
	private final Collection<DescriptivePerson> authors;
	private final Collection<DescriptivePerson> contributors;

	private final Map<Class<? extends Object>, List<? extends Object>> realListeners = new IdentityHashMap<>();
	private final Map<String, List<String>> protoListeners;
	private final List<String> mixins;

	private final Collection<ModDependency> depends;
	private final Collection<ModDependency> recommends;
	private final Collection<ModDependency> suggests;
	private final Collection<ModDependency> conflicts;
	private final Collection<ModDependency> breaks;

	private final Map<String, String> custom;

	ModMetadata(String id, String name, String description, SemanticVersion version, String license, String iconPath,
			ProjectContact contact, Collection<DescriptivePerson> authors, Collection<DescriptivePerson> contributors,
			Map<String, List<String>> listeners, List<String> mixins, Collection<ModDependency> depends,
			Collection<ModDependency> recommends, Collection<ModDependency> suggests, Collection<ModDependency> conflicts,
			Collection<ModDependency> breaks, Map<String, String> custom) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.version = version;
		this.license = license;
		this.iconPath = iconPath;
		this.contact = contact;
		this.authors = authors;
		this.contributors = contributors;
		protoListeners = listeners;
		this.mixins = mixins;
		this.depends = depends;
		this.recommends = recommends;
		this.suggests = suggests;
		this.conflicts = conflicts;
		this.breaks = breaks;
		this.custom = custom;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public SemanticVersion getVersion() {
		return version;
	}

	@Override
	public String getName() {
		return name != null ? name : id;
	}

	@Override
	public String getDescription() {
		return description != null ? description : "";
	}

	@Override
	public String getLicenseName() {
		return license;
	}

	@Override
	public Optional<String> getIconPath() {
		return Optional.ofNullable(iconPath);
	}

	@Override
	public Collection<DescriptivePerson> getDescriptiveAuthors() {
		return authors;
	}

	@Override
	public Collection<DescriptivePerson> getDescriptiveContributors() {
		return contributors;
	}

	@Override
	public ProjectContact getContact() {
		return contact;
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

	public List<String> getMixinConfigs() {
		return mixins;
	}

	private Set<String> modsOrdered(Ordered ordering) {
		return Stream.of(depends, recommends, suggests, conflicts, breaks).flatMap(Collection::stream)
				.filter(Predicates.instanceOf(SortedModDependency.class)).map(SortedModDependency.class::cast)
				.filter(Predicate.isEqual(ordering)).map(SortedModDependency::getModId).collect(ImmutableSet.toImmutableSet());
	}

	public Set<String> modsBefore() {
		return modsOrdered(Ordered.BEFORE);
	}

	public Set<String> modsAfter() {
		return modsOrdered(Ordered.AFTER);
	}

	@Override
	public Collection<ModDependency> getDepends() {
		return depends;
	}

	@Override
	public Collection<ModDependency> getRecommends() {
		return recommends;
	}

	@Override
	public Collection<ModDependency> getSuggests() {
		return suggests;
	}

	@Override
	public Collection<ModDependency> getConflicts() {
		return conflicts;
	}

	@Override
	public Collection<ModDependency> getBreaks() {
		return breaks;
	}

	@Override
	public Optional<String> getCustomData(String key) {
		return Optional.ofNullable(custom.get(key));
	}

	@Override
	public String toString() {
		return "Salts Mod<" + getId() + '>';
	}

	@Override
	public boolean equals(Object that) {
		if (this == that) return true;
		if (that == null) return false;

		return that instanceof SaltsModMetadata && id.equals(((SaltsModMetadata) that).getId());
	}

	@Override
	public int hashCode() {
		return id.hashCode(); //Kinda naff as a hashcode, but shouldn't have conflicting mod IDs
	}
}