package com.aiterminal.forge.block;

import com.aiterminal.common.terminal.TerminalLine;
import com.aiterminal.forge.registry.ModRegistry;
import com.aiterminal.forge.screen.TerminalMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
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
 * persisted to NBT. Implements {@link MenuProvider} so the block can open the terminal menu.
 */
public class TerminalBlockEntity extends BlockEntity implements MenuProvider {

    private static final int MAX_ENTRIES = 50;

    private final List<String> outputHistory = new ArrayList<>();
    private boolean isLoading;
    private boolean streaming;
    private final StringBuilder streamingResponse = new StringBuilder();

    public TerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.TERMINAL_BE.get(), pos, state);
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
        streaming = false;
        streamingResponse.setLength(0);
    }

    // ---- Streaming response ---------------------------------------------

    public boolean isStreaming() {
        return streaming;
    }

    public String getStreamingText() {
        return streamingResponse.toString();
    }

    /** Begin a new streamed assistant response (the live buffer the screen renders). */
    public void beginResponse() {
        streaming = true;
        streamingResponse.setLength(0);
    }

    /** Append a streamed fragment to the live buffer. */
    public void appendResponse(String fragment) {
        if (streaming && fragment != null) {
            streamingResponse.append(fragment);
        }
    }

    /** Commit the live buffer as a history entry and stop streaming. */
    public void endResponse() {
        if (streaming) {
            streaming = false;
            if (streamingResponse.length() > 0) {
                add(TerminalLine.encode(TerminalLine.Kind.RESPONSE, streamingResponse.toString()));
            }
            streamingResponse.setLength(0);
        }
    }

    private void add(String encoded) {
        outputHistory.add(encoded);
        while (outputHistory.size() > MAX_ENTRIES) {
            outputHistory.remove(0);
        }
    }

    // ---- MenuProvider ----------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.aiterminal.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new TerminalMenu(containerId, inventory, this);
    }
}
