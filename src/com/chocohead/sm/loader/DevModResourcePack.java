package com.chocohead.sm.loader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.ResourceNotFoundException;

import net.fabricmc.fabric.api.resource.ModResourcePack;
import net.fabricmc.fabric.impl.resource.loader.ModResourcePackUtil;

import com.chocohead.sm.api.SaltsModMetadata;

public class DevModResourcePack extends DirectoryResourcePack implements ModResourcePack {
	private final SaltsModMetadata mod;

	public DevModResourcePack(File file, SaltsModMetadata mod) {
		super(file);

		this.mod = mod;
	}

	@Override
	protected boolean containsFile(String name) {
		return ModResourcePackUtil.containsDefault(mod, name) || super.containsFile(name);
	}

	@Override
	protected InputStream openFile(String name) throws IOException {
		try {
			return super.openFile(name);
		} catch (ResourceNotFoundException e) {
			InputStream stream = ModResourcePackUtil.openDefault(mod, name);
			if (stream != null) return stream;

			throw e;
		}
	}

	@Override
	public SaltsModMetadata getFabricModMetadata() {
		return mod;
	}

	@Override
	public String getName() {
		if (mod.getName() != null) {
			return mod.getName();
		} else {
			return "Salts Mill Mod: ".concat(mod.getId());
		}
	}
}