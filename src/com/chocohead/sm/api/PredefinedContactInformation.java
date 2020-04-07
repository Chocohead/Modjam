package com.chocohead.sm.api;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import net.fabricmc.loader.api.metadata.ContactInformation;

import com.chocohead.sm.loader.PreMixinClassloaded;

@PreMixinClassloaded
interface PredefinedContactInformation extends ContactInformation {
	/**
	 * Get the list of properties {@link #get(String)} supports, but might still return {@link Optional#empty() empty} for
	 *
	 * @return The list of properties which {@link #get(String)} supports
	 */
	String[] getDefinitions();

	@Override
	default Map<String, String> asMap() {
		Builder<String, String> builder = ImmutableMap.builder();

		for (String definition : getDefinitions()) {
			get(definition).ifPresent(value -> builder.put(definition, value));
		}

		return builder.build();
	}
}