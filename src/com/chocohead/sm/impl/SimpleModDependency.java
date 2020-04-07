package com.chocohead.sm.impl;

import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModDependency;

import com.chocohead.sm.loader.PreMixinClassloaded;

@PreMixinClassloaded
public class SimpleModDependency implements ModDependency {
	private final String modID;
	private final String modType;

	public SimpleModDependency(String modID, String type) {
		this.modID = modID;
		modType = type;
	}

	@Override
	public String getModId() {
		return modID;
	}

	@Override
	public boolean matches(Version version) {
		return true;
	}

	/** @since 0.3 */
	public String getModType() {
		return modType;
	}
}