package com.chocohead.sm.loader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.spongepowered.asm.mixin.Mixins;

import net.fabricmc.loader.api.FabricLoader;

import com.chocohead.sm.api.SaltsModMetadata;

@PreMixinClassloaded
public class PreLoader implements Runnable {
	static final List<Entry<SaltsModMetadata, File>> EXTRA_RESOURCE_PACKS = new ArrayList<>();
	static final Logger LOGGER = LogManager.getLogger();
	static ResourceLoader resourceLoader = new ResourceLoader();
	static List<ProtoModMetadata> protoMods;

	@Override
	public void run() {
		CassetteLoader.loadCassettes(resourceLoader); //First we need to make sure all the cassettes are loaded

		LOGGER.debug("Commencing loading");
		Set<ProtoModMetadata> mods = loadMods(); //Then we'll read all the mods we can find

		//Sorting and general dependency resolution should happen next
		//That's effort though so without any mod to depend on isn't important
		protoMods = new ArrayList<>(mods);

		for (ProtoModMetadata mod : protoMods) {
			for (String mixin : mod.getMixinConfigs()) {
				Mixins.addConfiguration(mixin); //Add the Mixins the loaded mods may have
			}
		}
	}

	private static Set<ProtoModMetadata> loadMods() {
		Set<URL> mods; //As much as I love a good Enumeration, sometimes knowing the size is good too
		try {
			mods = new HashSet<>(Collections.list(PreLoader.class.getClassLoader().getResources("titus.mod.xml")));
		} catch (IOException e) {//Well this isn't good
			throw new RuntimeException("Fatal error finding mods on the classpath", e);
		}

		LOGGER.debug("Found {} mod XML URLs", mods.size());
		Set<ProtoModMetadata> modsList = new HashSet<>();

		for (URL mod : mods) {
			LOGGER.debug("Reading mod(s) defined at {}", mod);

			try (InputStream in = mod.openStream()) {
				Set<ProtoModMetadata> parsedMods = ModParser.read(in, FabricLoader.getInstance().getEnvironmentType());
				LOGGER.debug("Found {} mod definitions: {}", parsedMods.size(), parsedMods);

				for (ProtoModMetadata parsedMod : parsedMods) {
					if (!modsList.add(parsedMod)) {
						throw new IllegalStateException("Duplicate mods with mod ID " + parsedMod.getId());
					}
				}

				if (!parsedMods.isEmpty()) {
					switch (mod.getProtocol()) {
					case "file": {
						ProtoModMetadata parsedMod = parsedMods.iterator().next(); //Not especially important which mod wins if there's multiple
						try {
							EXTRA_RESOURCE_PACKS.add(new SimpleImmutableEntry<>(parsedMod, new File(mod.toURI()).getParentFile()));
						} catch (URISyntaxException e) {
							throw new AssertionError("File URL didn't correlate to a real file?", e);
						}
						break;
					}

					case "jar":
						break; //Who'd want a jar mod

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