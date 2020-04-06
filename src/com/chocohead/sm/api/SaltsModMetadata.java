package com.chocohead.sm.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;

public interface SaltsModMetadata extends ModMetadata {
	//Mod information >>
	@Override
	default String getType() {
		return "Salts Mill";
	}

	@Override
	SemanticVersion getVersion();

	Collection<DescriptivePerson> getDescriptiveAuthors();

	@Override
	default Collection<Person> getAuthors() {
		//Box it up given Fabric doesn't do Collection<? extends Person>
		return new ArrayList<>(getDescriptiveAuthors());
	}

	Collection<DescriptivePerson> getDescriptiveContributors();

	@Override
	default Collection<Person> getContributors() {
		//Box it up given Fabric doesn't do Collection<? extends Person>
		return new ArrayList<>(getDescriptiveContributors());
	}

	@Override
	ProjectContact getContact();

	String getLicenseName();

	@Override
	default Collection<String> getLicense() {
		return Collections.singleton(getLicenseName());
	}
	//<< Mod information

	//Mod icons >>
	Optional<String> getIconPath();

	@Override
	default Optional<String> getIconPath(int size) {
		return getIconPath();
	}
	//<< Mod icons

	//Custom data handling >>
	Optional<String> getCustomData(String key);

	@Override
	default boolean containsCustomValue(String key) {
		return getCustomData(key).isPresent();
	}

	@Override
	default CustomValue getCustomValue(String key) {
		return getCustomData(key).map(value -> new CustomValue() {
			@Override
			public CvType getType() {
				return CvType.STRING;
			}

			@Override
			public String getAsString() {
				return value;
			}

			@Override
			public CvObject getAsObject() {
				throw new ClassCastException("Can't implitly convert String to Object");
			}

			@Override
			public CvArray getAsArray() {
				throw new ClassCastException("Can't implitly convert String to Array");
			}

			@Override
			public Number getAsNumber() {
				throw new ClassCastException("Can't implitly convert String to Number");
			}

			@Override
			public boolean getAsBoolean() {
				throw new ClassCastException("Can't implitly convert String to Boolean");
			}
		}).orElse(null);
	}

	@Override
	default boolean containsCustomElement(String key) {
		return containsCustomValue(key);
	}

	@Override
	default JsonElement getCustomElement(String key) {
		return getCustomData(key).map(JsonPrimitive::new).orElse(null);
	}
}