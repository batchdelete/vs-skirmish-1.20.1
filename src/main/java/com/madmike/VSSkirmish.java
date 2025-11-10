package com.madmike;

import com.madmike.command.SkirmishCommand;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VSSkirmish implements ModInitializer {
	public static final String MOD_ID = "vs-skirmish";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		LOGGER.info("VS Skirmish init");

		SkirmishCommand.register();

		LOGGER.info("VS Skirmish done");
	}
}