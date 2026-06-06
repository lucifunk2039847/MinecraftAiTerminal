package com.aiterminal.forge;

import com.aiterminal.common.AITerminalConstants;
import com.aiterminal.common.api.OpenWebUIClient;
import com.aiterminal.common.config.AITerminalConfig;
import com.aiterminal.forge.registry.ModRegistry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Forge entry point. Loads config, builds the shared HTTP client and registers content. Safe on a
 * dedicated server — the screen factory is registered client-side only (see {@code client} package).
 */
@Mod(AITerminalConstants.MOD_ID)
public final class AITerminalForge {

    private static AITerminalConfig config;
    private static OpenWebUIClient client;

    public AITerminalForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
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
