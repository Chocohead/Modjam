package com.chocohead.sm.loader;

import java.net.URI;
import java.net.URL;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.metadata.ModDependency;

import com.chocohead.sm.api.DescriptivePerson;
import com.chocohead.sm.api.ProjectContact;
import com.chocohead.sm.impl.Person;
import com.chocohead.sm.impl.ProjectContacts;
import com.chocohead.sm.impl.SimpleModDependency;
import com.chocohead.sm.impl.SortedModDependency;
import com.chocohead.sm.impl.SortedModDependency.Ordered;

/** This is loaded on the pre-Mixin phase of Knot, <b>BE VERY CAREFUL WHAT IS LOADED</b> */
abstract class CommonModBuilder {
	protected final String id;
	protected final SemanticVersion version;
	protected String name, description;
	protected String license;
	protected String iconPath;

	protected ProjectContact contact = ProjectContact.EMPTY;
	protected final ImmutableList.Builder<DescriptivePerson> authors = ImmutableList.builder();
	protected final ImmutableList.Builder<DescriptivePerson> contributors = ImmutableList.builder();

	protected final ImmutableList.Builder<String> mixins = ImmutableList.builder();

	protected final ImmutableList.Builder<ModDependency> depends = ImmutableList.builder();
	protected final ImmutableList.Builder<ModDependency> recommends = ImmutableList.builder();
	protected final ImmutableList.Builder<ModDependency> suggests = ImmutableList.builder();
	protected final ImmutableList.Builder<ModDependency> conflicts = ImmutableList.builder();
	protected final ImmutableList.Builder<ModDependency> breaks = ImmutableList.builder();

	protected final ImmutableMap.Builder<String, String> custom = ImmutableMap.builder();

	public CommonModBuilder(String modID, SemanticVersion version) {
		id = modID;
		this.version = version;
	}

	public CommonModBuilder withName(String name) {
		assert this.name == null;
		this.name = name;
		return this;
	}

	public CommonModBuilder withDescription(String description) {
		assert this.description == null;
		this.description = description;
		return this;
	}

	public CommonModBuilder withLicense(String license) {
		assert this.license == null;
		this.license = license;
		return this;
	}

	public CommonModBuilder withIcon(String path) {
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

		public CommonModBuilder butNotMore() {
			assert contact == ProjectContact.EMPTY;
			contact = ProjectContacts.create(issues, source, homepage, IRC, discord, others.build());
			return CommonModBuilder.this;
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

		public CommonModBuilder butNotMore() {
			accepter.accept(Person.create(name, homepage, email, IRC, twitter, discord, others.build()));
			return CommonModBuilder.this;
		}
	}

	public PersonBuilder withAuthor(String name) {
		return new PersonBuilder(name, authors::add);
	}

	public PersonBuilder withContributor(String name) {
		return new PersonBuilder(name, contributors::add);
	}

	public CommonModBuilder withMixinConfig(String config) {
		mixins.add(config);
		return this;
	}

	public CommonModBuilder withDependency(String modID) {
		depends.add(new SimpleModDependency(modID));
		return this;
	}

	public CommonModBuilder withOrderedDependency(String modID, Ordered ordering) {
		depends.add(new SortedModDependency(modID, ordering));
		return this;
	}

	public CommonModBuilder withSuggestion(String modID, boolean strongSuggestion) {
		(strongSuggestion ? recommends : suggests).add(new SimpleModDependency(modID));
		return this;
	}

	public CommonModBuilder withOrderedSuggestion(String modID, Ordered ordering, boolean strongSuggestion) {
		(strongSuggestion ? recommends : suggests).add(new SortedModDependency(modID, ordering));
		return this;
	}

	public CommonModBuilder withConflict(String modID, boolean severeConflict) {
		(severeConflict ? breaks : conflicts).add(new SimpleModDependency(modID));
		return this;
	}

	public CommonModBuilder withOrderedConflict(String modID, Ordered ordering, boolean severeConflict) {
		(severeConflict ? breaks : conflicts).add(new SortedModDependency(modID, ordering));
		return this;
	}

	public CommonModBuilder withCustomData(String key, String value) {
		custom.put(key, value);
		return this;
	}

	public abstract CommonModMetadata build();
}