package com.aiterminal.neoforge.screen;

import com.aiterminal.neoforge.block.TerminalBlockEntity;
import com.aiterminal.neoforge.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Slotless container menu. Its only job is to carry the {@link TerminalBlockEntity} reference
 * across to the client screen (the block position is written/read through the menu buffer).
 */
public class TerminalMenu extends AbstractContainerMenu {

    @Nullable
    private final TerminalBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    /** Client-side constructor: the block position is read from the network buffer. */
    public TerminalMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, resolve(inventory, buf.readBlockPos()));
    }

    /** Server-side / shared constructor. */
    public TerminalMenu(int containerId, Inventory inventory, @Nullable TerminalBlockEntity blockEntity) {
        super(ModRegistry.TERMINAL_MENU.get(), containerId);
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
        return stillValid(access, player, ModRegistry.TERMINAL_BLOCK.get());
    }
}
