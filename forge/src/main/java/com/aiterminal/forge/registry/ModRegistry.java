package com.aiterminal.forge.registry;

import com.aiterminal.common.AITerminalConstants;
import com.aiterminal.forge.block.TerminalBlock;
import com.aiterminal.forge.block.TerminalBlockEntity;
import com.aiterminal.forge.screen.TerminalMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistry {

    private static final String MOD_ID = AITerminalConstants.MOD_ID;

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final RegistryObject<TerminalBlock> TERMINAL_BLOCK = BLOCKS.register("terminal",
            () -> new TerminalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final RegistryObject<BlockItem> TERMINAL_ITEM = ITEMS.register("terminal",
            () -> new BlockItem(TERMINAL_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<TerminalBlockEntity>> TERMINAL_BE =
            BLOCK_ENTITIES.register("terminal",
                    () -> BlockEntityType.Builder.of(TerminalBlockEntity::new, TERMINAL_BLOCK.get()).build(null));

    public static final RegistryObject<MenuType<TerminalMenu>> TERMINAL_MENU = MENUS.register("terminal",
            () -> IForgeMenuType.create(TerminalMenu::new));

    public static final RegistryObject<CreativeModeTab> TERMINAL_TAB = TABS.register("terminal_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.aiterminal.terminal_tab"))
                    .icon(() -> new ItemStack(TERMINAL_ITEM.get()))
                    .displayItems((params, output) -> output.accept(TERMINAL_ITEM.get()))
                    .build());

    private ModRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        TABS.register(modEventBus);
    }
}
