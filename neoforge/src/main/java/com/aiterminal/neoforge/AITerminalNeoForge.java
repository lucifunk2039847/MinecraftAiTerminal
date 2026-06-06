package com.aiterminal.neoforge;

import com.aiterminal.common.AITerminalConstants;
import com.aiterminal.common.api.OpenWebUIClient;
import com.aiterminal.common.config.AITerminalConfig;
import com.aiterminal.neoforge.registry.ModRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

/**
 * NeoForge entry point. Loads the config once, builds the shared HTTP client, and registers
 * all content. Safe on a dedicated server — the GUI is registered client-side only
 * (see {@code client.AITerminalNeoForgeClient}).
 */
@Mod(AITerminalConstants.MOD_ID)
public final class AITerminalNeoForge {

    private static AITerminalConfig config;
    private static OpenWebUIClient client;

    public AITerminalNeoForge(IEventBus modEventBus) {
        config = AITerminalConfig.loadOrCreate(FMLPaths.CONFIGDIR.get());
        client = new OpenWebUIClient(config);
        ModRegistry.register(modEventBus);
    }

    public static AITerminalConfig config() {
        return config;
    }

    public static OpenWebUIClient client() {
        return client;
    }
}
