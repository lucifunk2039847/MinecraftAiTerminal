package com.aiterminal.neoforge.client;

import com.aiterminal.common.AITerminalConstants;
import com.aiterminal.neoforge.registry.ModRegistry;
import com.aiterminal.neoforge.screen.TerminalScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-only setup. Loaded only on the physical client (so a dedicated server never references
 * the GUI classes), this binds the terminal menu to its screen.
 */
@EventBusSubscriber(modid = AITerminalConstants.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AITerminalNeoForgeClient {

    private AITerminalNeoForgeClient() {
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModRegistry.TERMINAL_MENU.get(), TerminalScreen::new);
    }
}
