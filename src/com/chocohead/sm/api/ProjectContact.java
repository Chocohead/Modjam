package com.chocohead.sm.api;

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.chocohead.sm.loader.PreMixinClassloaded;

@PreMixinClassloaded
public interface ProjectContact extends PredefinedContactInformation {
	ProjectContact EMPTY = new ProjectContact() {
		@Override
		public URL getIssues() {
			return null;
		}

		@Override
		public URL getSource() {
			return null;
		}

		@Override
		public URL getHomepage() {
			return null;
		}

		@Override
		public URI getIRC() {
			return null;
		}

		@Override
		public String getDiscord() {
			return null;
		}

		@Override
		public Optional<String> get(String key) {
			return Optional.empty();
		}

		@Override
		public Map<String, String> asMap() {
			return Collections.emptyMap();
		}
	};

	URL getIssues();

	URL getSource();

	URL getHomepage();

	URI getIRC();

	/** Discord invite link */
	String getDiscord();

	@Override
	default String[] getDefinitions() {
		return new String[] {"issues", "sources", "homepage", "irc", "discord"};
	}

	@Override
	default Optional<String> get(String key) {
		switch (key) {
		case "issues":
			return Optional.ofNullable(getIssues()).map(URL::toExternalForm);

		case "sources":
			return Optional.ofNullable(getSource()).map(URL::toExternalForm);

		case "homepage":
			return Optional.ofNullable(getHomepage()).map(URL::toExternalForm);

		case "irc":
			return Optional.ofNullable(getIRC()).map(URI::toString);

		case "discord":
			return Optional.ofNullable(getDiscord());

		default:
			return Optional.empty();
		}
	}
}