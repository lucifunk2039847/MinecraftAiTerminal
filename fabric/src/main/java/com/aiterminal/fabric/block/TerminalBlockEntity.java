package com.aiterminal.fabric.block;

import com.aiterminal.common.terminal.TerminalLine;
import com.aiterminal.fabric.registry.ModRegistry;
import com.aiterminal.fabric.screen.TerminalScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the (client-side) conversation history and loading flag. History is intentionally not
 * persisted to NBT. Implements Fabric's {@link ExtendedScreenHandlerFactory} so the block can
 * open the terminal screen handler and ship its {@link BlockPos} to the client.
 */
public class TerminalBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<BlockPos> {

    private static final int MAX_ENTRIES = 50;

    private final List<String> outputHistory = new ArrayList<>();
    private boolean isLoading;

    public TerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.TERMINAL_BE, pos, state);
    }

    public List<String> getOutputHistory() {
        return outputHistory;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void setLoading(boolean loading) {
        this.isLoading = loading;
    }

    public void addUserLine(String text) {
        add(TerminalLine.encode(TerminalLine.Kind.USER, "> " + text));
    }

    public void addResponse(String text) {
        add(TerminalLine.encode(TerminalLine.Kind.RESPONSE, text));
    }

    public void addError(String text) {
        add(TerminalLine.encode(TerminalLine.Kind.ERROR, "! " + text));
    }

    public void addInfo(String text) {
        add(TerminalLine.encode(TerminalLine.Kind.INFO, text));
    }

    public void clearHistory() {
        outputHistory.clear();
    }

    private void add(String encoded) {
        outputHistory.add(encoded);
        while (outputHistory.size() > MAX_ENTRIES) {
            outputHistory.remove(0);
        }
    }

    // ---- ExtendedScreenHandlerFactory ------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.aiterminal.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new TerminalScreenHandler(containerId, inventory, this);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return this.worldPosition;
    }
}
