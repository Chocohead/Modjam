package com.chocohead.sm.impl;

import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModDependency;

public class SimpleModDependency implements ModDependency {
	private final String modID;

	public SimpleModDependency(String modID) {
		this.modID = modID;
	}

	@Override
	public String getModId() {
		return modID;
	}

	@Override
	public boolean matches(Version version) {
		return true;
	}
}