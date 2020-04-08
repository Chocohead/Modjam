package com.chocohead.sm.loader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

import org.spongepowered.asm.mixin.Mixins;

import net.fabricmc.loader.api.FabricLoader;

import com.chocohead.sm.api.SaltsModMetadata;

@PreMixinClassloaded
public class PreLoader implements Runnable {
	static final List<Entry<SaltsModMetadata, File>> EXTRA_RESOURCE_PACKS = new ArrayList<>();
	static final List<ModMetadata> MODS = new ArrayList<>();
	static final Logger LOGGER = LogManager.getLogger();
	static ResourceLoader resourceLoader = new ResourceLoader();

	@Override
	public void run() {
		classLoadingForcer();

		CassetteLoader.loadCassettes(resourceLoader); //First we need to make sure all the cassettes are loaded

		LOGGER.debug("Commencing loading");
		Set<ModMetadata> mods = loadMods(); //Then we'll read all the mods we can find

		//Sorting and general dependency resolution should happen next
		//That's effort though so without any mod to depend on isn't important
		MODS.addAll(mods);

		for (ModMetadata mod : MODS) {
			for (String mixin : mod.getMixinConfigs()) {
				Mixins.addConfiguration(mixin); //Add the Mixins the loaded mods may have
			}
		}
	}

	/** Define classes which the main and resource threads will otherwise deadlock each other over on a dedicated server */
	private static void classLoadingForcer() {
		StringUtils.countMatches(null, 'X');
		ImmutableMap.of().entrySet().iterator();
	}

	private static Set<ModMetadata> loadMods() {
		Set<URL> mods; //As much as I love a good Enumeration, sometimes knowing the size is good too
		try {
			mods = Sets.newHashSet(Iterators.forEnumeration(PreLoader.class.getClassLoader().getResources("titus.mod.xml")));
		} catch (IOException e) {//Well this isn't good
			throw new RuntimeException("Fatal error finding mods on the classpath", e);
		}

		LOGGER.debug("Found {} mod XML URLs", mods.size());
		Set<ModMetadata> modsList = new HashSet<>();

		for (URL mod : mods) {
			LOGGER.debug("Reading mod(s) defined at {}", mod);

			try (InputStream in = mod.openStream()) {
				Set<ModMetadata> parsedMods = ModParser.read(in, FabricLoader.getInstance().getEnvironmentType());
				LOGGER.debug("Found {} mod definitions: {}", parsedMods.size(), parsedMods);

				for (ModMetadata parsedMod : parsedMods) {
					if (!modsList.add(parsedMod)) {
						throw new IllegalStateException("Duplicate mods with mod ID " + parsedMod.getId());
					}
				}

				if (!parsedMods.isEmpty()) {
					switch (mod.getProtocol()) {
					case "file": {
						ModMetadata parsedMod = parsedMods.iterator().next(); //Not especially important which mod wins if there's multiple
						try {
							EXTRA_RESOURCE_PACKS.add(new SimpleImmutableEntry<>(parsedMod, new File(mod.toURI()).getParentFile()));
						} catch (URISyntaxException e) {
							throw new AssertionError("File URL didn't correlate to a real file?", e);
						}
						break;
					}

					case "jar":
						break; //Who'd want a jar as a mod?

					case "salts_mill":
						break; //Already handled via CassetteLoader

					default: //Apparently it's readable, wonder where it came from
						throw new AssertionError("Unexpected mod source: " + mod);
					}
				}
			} catch (IOException e) {
				throw new RuntimeException("Error reading mod from " + mod, e);
			}
		}

		return modsList;
	}
}