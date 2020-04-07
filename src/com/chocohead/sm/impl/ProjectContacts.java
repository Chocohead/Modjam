package com.chocohead.sm.impl;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import com.chocohead.sm.api.ProjectContact;
import com.chocohead.sm.loader.PreMixinClassloaded;

@PreMixinClassloaded
public final class ProjectContacts implements ProjectContact {
	public static ProjectContact create(URL issues, URL source, URL homepage, URI IRC, String discord, Map<String, String> others) {
		if (issues == null && source == null && homepage == null && IRC == null && discord == null && others.isEmpty()) {
			return ProjectContact.EMPTY;
		} else {
			return new ProjectContacts(issues, source, homepage, IRC, discord, others);
		}
	}

	private final URL issues;
	private final URL source;
	private final URL homepage;
	private final URI IRC;
	private final String discord;
	private final Map<String, String> map;

	private ProjectContacts(URL issues, URL source, URL homepage, URI IRC, String discord, Map<String, String> others) {
		this.issues = issues;
		this.source = source;
		this.homepage = homepage;
		this.IRC = IRC;
		this.discord = discord;
		map = makeMap(others);
	}

	private Map<String, String> makeMap(Map<String, String> extra) {
		if (extra.isEmpty()) {
			return ProjectContact.super.asMap();
		} else {
			return ImmutableMap.<String, String>builder().putAll(ProjectContact.super.asMap()).putAll(extra).build();
		}
	}

	@Override
	public URL getIssues() {
		return issues;
	}

	@Override
	public URL getSource() {
		return source;
	}

	@Override
	public URL getHomepage() {
		return homepage;
	}

	@Override
	public URI getIRC() {
		return IRC;
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
