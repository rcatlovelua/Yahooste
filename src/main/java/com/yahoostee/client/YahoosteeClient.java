package com.yahoostee.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YahoosteeClient implements ClientModInitializer {
    public static final String MOD_ID = "yahoostee";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Yahoostee Client (yahoo paste) Initialized!");
    }
}
