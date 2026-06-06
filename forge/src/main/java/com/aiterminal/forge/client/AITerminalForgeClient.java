package com.aiterminal.forge.client;

import com.aiterminal.common.AITerminalConstants;
import com.aiterminal.forge.registry.ModRegistry;
import com.aiterminal.forge.screen.TerminalScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only setup. Loaded only on the physical client (so a dedicated server never references the
 * GUI classes), this binds the terminal menu to its screen during client setup.
 */
@Mod.EventBusSubscriber(modid = AITerminalConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AITerminalForgeClient {

    private AITerminalForgeClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                MenuScreens.register(ModRegistry.TERMINAL_MENU.get(), TerminalScreen::new));
    }
}
