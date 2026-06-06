package com.aiterminal.fabric.registry;

import com.aiterminal.common.AITerminalConstants;
import com.aiterminal.fabric.block.TerminalBlock;
import com.aiterminal.fabric.block.TerminalBlockEntity;
import com.aiterminal.fabric.screen.TerminalScreenHandler;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModRegistry {

    private static final String MOD_ID = AITerminalConstants.MOD_ID;

    public static Block TERMINAL_BLOCK;
    public static BlockItem TERMINAL_ITEM;
    public static BlockEntityType<TerminalBlockEntity> TERMINAL_BE;
    public static ExtendedScreenHandlerType<TerminalScreenHandler, BlockPos> TERMINAL_MENU;
    public static CreativeModeTab TERMINAL_TAB;

    private ModRegistry() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void register() {
        TERMINAL_BLOCK = Registry.register(BuiltInRegistries.BLOCK, id("terminal"),
                new TerminalBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(3.0F, 6.0F)
                        .requiresCorrectToolForDrops()
                        .noOcclusion()));

        TERMINAL_ITEM = Registry.register(BuiltInRegistries.ITEM, id("terminal"),
                new BlockItem(TERMINAL_BLOCK, new Item.Properties()));

        TERMINAL_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("terminal"),
                BlockEntityType.Builder.of(TerminalBlockEntity::new, TERMINAL_BLOCK).build(null));

        TERMINAL_MENU = Registry.register(BuiltInRegistries.MENU, id("terminal"),
                new ExtendedScreenHandlerType<>(TerminalScreenHandler::new, BlockPos.STREAM_CODEC));

        TERMINAL_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("terminal_tab"),
                FabricItemGroup.builder()
                        .title(Component.translatable("itemGroup.aiterminal.terminal_tab"))
                        .icon(() -> new ItemStack(TERMINAL_ITEM))
                        .displayItems((params, output) -> output.accept(TERMINAL_ITEM))
                        .build());
    }
}
