package com.aiterminal.neoforge.registry;

import com.aiterminal.common.AITerminalConstants;
import com.aiterminal.neoforge.block.TerminalBlock;
import com.aiterminal.neoforge.block.TerminalBlockEntity;
import com.aiterminal.neoforge.screen.TerminalMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRegistry {

    private static final String MOD_ID = AITerminalConstants.MOD_ID;

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredBlock<TerminalBlock> TERMINAL_BLOCK = BLOCKS.register("terminal",
            () -> new TerminalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final DeferredItem<BlockItem> TERMINAL_ITEM =
            ITEMS.registerSimpleBlockItem("terminal", TERMINAL_BLOCK);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TerminalBlockEntity>> TERMINAL_BE =
            BLOCK_ENTITIES.register("terminal",
                    () -> BlockEntityType.Builder.of(TerminalBlockEntity::new, TERMINAL_BLOCK.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<TerminalMenu>> TERMINAL_MENU =
            MENUS.register("terminal", () -> IMenuTypeExtension.create(TerminalMenu::new));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TERMINAL_TAB =
            TABS.register("terminal_tab", () -> CreativeModeTab.builder()
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
