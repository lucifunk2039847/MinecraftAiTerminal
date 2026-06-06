package com.aiterminal.fabric.client;

import com.aiterminal.fabric.registry.ModRegistry;
import com.aiterminal.fabric.screen.TerminalScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;

/**
 * Fabric client entry point: binds the terminal screen handler to its screen and sets the block's
 * render layer to cutout (for later visual flexibility). Only loaded on the physical client.
 */
public final class AITerminalFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModRegistry.TERMINAL_MENU, TerminalScreen::new);
        BlockRenderLayerMap.INSTANCE.putBlock(ModRegistry.TERMINAL_BLOCK, RenderType.cutout());
    }
}
