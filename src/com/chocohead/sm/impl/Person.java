package com.chocohead.sm.impl;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import com.chocohead.sm.api.DescriptivePerson;
import com.chocohead.sm.api.PersonalContact;
import com.chocohead.sm.loader.PreMixinClassloaded;

@PreMixinClassloaded
public final class Person implements DescriptivePerson, PersonalContact {
	public static DescriptivePerson create(String name, URL homepage, URL email, URI IRC, String twitter, String discord, Map<String, String> others) {
		assert name != null;

		if (homepage == null && email == null && IRC == null && discord == null && others.isEmpty()) {
			return new DescriptivePerson() {
				@Override
				public String getName() {
					return name;
				}

				@Override
				public PersonalContact getContact() {
					return PersonalContact.EMPTY;
				}
			};
		} else {
			return new Person(name, homepage, email, IRC, twitter, discord, others);
		}
	}

	private final String name;
	private final URL homepage;
	private final URL email;
	private final URI IRC;
	private final String twitter;
	private final String discord;
	private final Map<String, String> map;

	private Person(String name, URL homepage, URL email, URI IRC, String twitter, String discord, Map<String, String> others) {
		this.name = name;
		this.homepage = homepage;
		this.email = email;
		this.IRC = IRC;
		this.twitter = twitter;
		this.discord = discord;
		map = makeMap(others);
	}

	private Map<String, String> makeMap(Map<String, String> extra) {
		if (extra.isEmpty()) {
			return PersonalContact.super.asMap();
		} else {
			return ImmutableMap.<String, String>builder().putAll(PersonalContact.super.asMap()).putAll(extra).build();
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public PersonalContact getContact() {
		return this;
	}

	@Override
	public URL getHomepage() {
		return homepage;
	}

	@Override
	public URL getEmail() {
		return email;
	}

	@Override
	public URI getIRC() {
		return IRC;
	}

	@Override
	public String getTwitter() {
		return twitter;
	}

	@Override
	public String getDiscord() {
		return discord;
	}

	@Override
	public Map<String, String> asMap() {
		return map;
	}
}