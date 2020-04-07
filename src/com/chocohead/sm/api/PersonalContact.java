package com.chocohead.sm.api;

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.chocohead.sm.loader.PreMixinClassloaded;

@PreMixinClassloaded
public interface PersonalContact extends PredefinedContactInformation {
	PersonalContact EMPTY = new PersonalContact() {
		@Override
		public URL getHomepage() {
			return null;
		}

		@Override
		public URL getEmail() {
			return null;
		}

		@Override
		public URI getIRC() {
			return null;
		}

		@Override
		public String getTwitter() {
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

	URL getHomepage();

	URL getEmail();

	URI getIRC();

	/** Twitter handle */
	String getTwitter();

	/** Discord username */
	String getDiscord();

	@Override
	default String[] getDefinitions() {
		return new String[] {"homepage", "email", "irc", "twitter", "discord"};
	}

	@Override
	default Optional<String> get(String key) {
		switch (key) {
		case "homepage":
			return Optional.ofNullable(getHomepage()).map(URL::toExternalForm);

		case "email":
			return Optional.ofNullable(getEmail()).map(URL::toExternalForm);

		case "irc":
			return Optional.ofNullable(getIRC()).map(URI::toString);

		case "twitter":
			return Optional.ofNullable(getTwitter());

		case "discord":
			return Optional.ofNullable(getDiscord());

		default:
			return Optional.empty();
		}
	}
}