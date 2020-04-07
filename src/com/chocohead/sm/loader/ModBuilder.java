package com.chocohead.sm.loader;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import it.unimi.dsi.fastutil.chars.CharArraySet;
import it.unimi.dsi.fastutil.chars.CharSet;

import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.metadata.ModDependency;

import com.chocohead.sm.api.DescriptivePerson;
import com.chocohead.sm.api.ProjectContact;
import com.chocohead.sm.impl.Person;
import com.chocohead.sm.impl.ProjectContacts;
import com.chocohead.sm.impl.SimpleModDependency;
import com.chocohead.sm.impl.SortedModDependency;
import com.chocohead.sm.impl.SortedModDependency.Ordered;

@PreMixinClassloaded
class ModBuilder {
	private final String id;
	private final SemanticVersion version;
	private String name, description;
	private String license;
	private String iconPath;

	private ProjectContact contact = ProjectContact.EMPTY;
	private final ImmutableList.Builder<DescriptivePerson> authors = ImmutableList.builder();
	private final ImmutableList.Builder<DescriptivePerson> contributors = ImmutableList.builder();

	private final Map<String, List<String>> listeners = new HashMap<>(); //Deliberately mutable
	private final ImmutableList.Builder<String> mixins = ImmutableList.builder();

	private final ImmutableList.Builder<ModDependency> depends = ImmutableList.builder();
	private final ImmutableList.Builder<ModDependency> recommends = ImmutableList.builder();
	private final ImmutableList.Builder<ModDependency> suggests = ImmutableList.builder();
	private final ImmutableList.Builder<ModDependency> conflicts = ImmutableList.builder();
	private final ImmutableList.Builder<ModDependency> breaks = ImmutableList.builder();

	private final ImmutableMap.Builder<String, String> custom = ImmutableMap.builder();

	public ModBuilder(String modID, SemanticVersion version) {
		if (modID == null || StringUtils.isBlank(modID)) throw new IllegalArgumentException("Cannot have mod with no name!");
		if (version == null) throw new NullPointerException("Cannot have mod with no version!");

		char first = modID.charAt(0);
		if (first < 'a' || first > 'z') throw new IllegalArgumentException("Mods must start with a lowercase letter a-z");

		CharSet invalidLetters = modID.chars().skip(1).filter(letter -> {//Same restrictions as Identifier#isNamespaceValid
			return (letter < '0' || letter > '9' && letter < 'a' || letter > 'z') && letter != '-' && letter != '_' && letter != '.';
		}).collect(CharArraySet::new, (set, letter) -> set.add((char) letter), CharSet::addAll);
		if (!invalidLetters.isEmpty()) throw new IllegalArgumentException("Invalid characters in mod name: " + invalidLetters);

		id = modID;
		this.version = version;
	}

	public ModBuilder withName(String name) {
		assert this.name == null;
		this.name = name;
		return this;
	}

	public ModBuilder withDescription(String description) {
		assert this.description == null;
		this.description = description;
		return this;
	}

	public ModBuilder withLicense(String license) {
		assert this.license == null;
		this.license = license;
		return this;
	}

	public ModBuilder withIcon(String path) {
		assert iconPath == null;
		iconPath = path;
		return this;
	}

	public class ContactBuilder {
		private URL issues;
		private URL source;
		private URL homepage;
		private URI IRC;
		private String discord;
		private final ImmutableMap.Builder<String, String> others = ImmutableMap.builder();

		ContactBuilder() {
		}

		public ContactBuilder withIssues(URL issues) {
			assert this.issues == null;
			this.issues = issues;
			return this;
		}

		public ContactBuilder withSource(URL source) {
			assert this.source == null;
			this.source = source;
			return this;
		}

		public ContactBuilder withHomepage(URL homepage) {
			assert this.homepage == null;
			this.homepage = homepage;
			return this;
		}

		public ContactBuilder withIRC(URI irc) {
			assert IRC == null;
			IRC = irc;
			return this;
		}

		public ContactBuilder withDiscord(String invite) {
			assert discord == null;
			discord = invite;
			return this;
		}

		public ContactBuilder withExtra(String name, String value) {
			others.put(name, value);
			return this;
		}

		public ModBuilder butNotMore() {
			assert contact == ProjectContact.EMPTY;
			contact = ProjectContacts.create(issues, source, homepage, IRC, discord, others.build());
			return ModBuilder.this;
		}
	}

	public ContactBuilder withProjectContacts() {
		return new ContactBuilder();
	}

	public class PersonBuilder {
		private final String name;
		private final Consumer<DescriptivePerson> accepter;
		private URL homepage;
		private URL email;
		private URI IRC;
		private String twitter;
		private String discord;
		private final ImmutableMap.Builder<String, String> others = ImmutableMap.builder();

		PersonBuilder(String name, Consumer<DescriptivePerson> accepter) {
			this.name = name;
			this.accepter = accepter;
		}

		public PersonBuilder withHomepage(URL homepage) {
			assert this.homepage == null;
			this.homepage = homepage;
			return this;
		}

		public PersonBuilder withEmail(URL email) {
			assert this.email == null;
			this.email = email;
			return this;
		}

		public PersonBuilder withIRC(URI irc) {
			assert IRC == null;
			IRC = irc;
			return this;
		}

		public PersonBuilder withTwitter(String handle) {
			assert twitter == null;
			twitter = handle;
			return this;
		}

		public PersonBuilder withDiscord(String username) {
			assert discord == null;
			discord = username;
			return this;
		}

		public PersonBuilder withExtra(String name, String value) {
			others.put(name, value);
			return this;
		}

		public ModBuilder butNotMore() {
			accepter.accept(Person.create(name, homepage, email, IRC, twitter, discord, others.build()));
			return ModBuilder.this;
		}
	}

	public PersonBuilder withAuthor(String name) {
		return new PersonBuilder(name, authors::add);
	}

	public PersonBuilder withContributor(String name) {
		return new PersonBuilder(name, contributors::add);
	}

	public <T> ModBuilder withListener(String type, String instance) {
		listeners.computeIfAbsent(type, k -> new ArrayList<>()).add(instance);
		return this;
	}

	public ModBuilder withMixinConfig(String config) {
		mixins.add(config);
		return this;
	}

	public ModBuilder withDependency(String modID, String type) {
		depends.add(new SimpleModDependency(modID, type));
		return this;
	}

	public ModBuilder withOrderedDependency(String modID, String type, Ordered ordering) {
		depends.add(new SortedModDependency(modID, type, ordering));
		return this;
	}

	public ModBuilder withSuggestion(String modID, String type, boolean strongSuggestion) {
		(strongSuggestion ? recommends : suggests).add(new SimpleModDependency(modID, type));
		return this;
	}

	public ModBuilder withOrderedSuggestion(String modID, String type, Ordered ordering, boolean strongSuggestion) {
		(strongSuggestion ? recommends : suggests).add(new SortedModDependency(modID, type, ordering));
		return this;
	}

	public ModBuilder withConflict(String modID, String type, boolean severeConflict) {
		(severeConflict ? breaks : conflicts).add(new SimpleModDependency(modID, type));
		return this;
	}

	public ModBuilder withOrderedConflict(String modID, String type, Ordered ordering, boolean severeConflict) {
		(severeConflict ? breaks : conflicts).add(new SortedModDependency(modID, type, ordering));
		return this;
	}

	public ModBuilder withCustomData(String key, String value) {
		custom.put(key, value);
		return this;
	}

	public ModMetadata build() {
		return new ModMetadata(id, name, description, version, license, iconPath, contact, authors.build(), contributors.build(), listeners,
					mixins.build(), depends.build(), recommends.build(), suggests.build(), conflicts.build(), breaks.build(), custom.build());
	}
}