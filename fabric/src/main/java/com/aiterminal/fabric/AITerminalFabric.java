package com.aiterminal.fabric;

import com.aiterminal.common.api.OpenWebUIClient;
import com.aiterminal.common.config.AITerminalConfig;
import com.aiterminal.fabric.registry.ModRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric common entry point. Loads config, builds the shared HTTP client and registers content.
 * Safe on a dedicated server — the screen factory is registered in the client entry point only.
 */
public final class AITerminalFabric implements ModInitializer {

    private static AITerminalConfig config;
    private static OpenWebUIClient client;

    @Override
    public void onInitialize() {
        config = AITerminalConfig.loadOrCreate(FabricLoader.getInstance().getConfigDir());
        client = new OpenWebUIClient(config);
        ModRegistry.register();
    }

    public static AITerminalConfig config() {
        return config;
    }

    public static OpenWebUIClient client() {
        return client;
    }
}
