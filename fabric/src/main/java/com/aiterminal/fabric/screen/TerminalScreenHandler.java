package com.aiterminal.fabric.screen;

import com.aiterminal.fabric.block.TerminalBlockEntity;
import com.aiterminal.fabric.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Slotless screen handler. Carries the {@link TerminalBlockEntity} reference to the client screen;
 * the block position arrives via the extended screen-handler data (a {@link BlockPos}).
 */
public class TerminalScreenHandler extends AbstractContainerMenu {

    @Nullable
    private final TerminalBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    /** Client-side constructor (the {@link BlockPos} comes from the extended handler data). */
    public TerminalScreenHandler(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, resolve(inventory, pos));
    }

    /** Server-side / shared constructor. */
    public TerminalScreenHandler(int containerId, Inventory inventory, @Nullable TerminalBlockEntity blockEntity) {
        super(ModRegistry.TERMINAL_MENU, containerId);
        this.blockEntity = blockEntity;
        this.access = (blockEntity != null && blockEntity.getLevel() != null)
                ? ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos())
                : ContainerLevelAccess.NULL;
    }

    @Nullable
    private static TerminalBlockEntity resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TerminalBlockEntity terminal ? terminal : null;
    }

    @Nullable
    public TerminalBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) {
            return true;
        }
        return stillValid(access, player, ModRegistry.TERMINAL_BLOCK);
    }
}
