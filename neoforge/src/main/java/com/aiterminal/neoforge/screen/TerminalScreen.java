package com.aiterminal.neoforge.screen;

import com.aiterminal.common.api.OpenWebUIClient;
import com.aiterminal.common.config.AITerminalConfig;
import com.aiterminal.common.terminal.TerminalLine;
import com.aiterminal.neoforge.AITerminalNeoForge;
import com.aiterminal.neoforge.block.TerminalBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Retro green-on-black terminal screen (NeoForge). Scrollable output on top, single-line input with
 * Ask/Clear at the bottom. The answer is <b>streamed</b> token-by-token (no read timeout); each
 * fragment is applied on the client thread so text appears live. Closing the screen cancels any
 * in-flight request.
 */
public class TerminalScreen extends AbstractContainerScreen<TerminalMenu> {

    private static final int COLOR_BG = 0xFF1A1A1A;
    private static final int COLOR_BORDER = 0xFF00CC00;
    private static final int COLOR_USER = 0xFFFFFFFF;
    private static final int COLOR_RESPONSE = 0xFF00FF00;
    private static final int COLOR_ERROR = 0xFFFFFF00;
    private static final int COLOR_INFO = 0xFF55FF55;

    private EditBox input;
    private int scrollRow;
    private boolean followTail = true;
    private CompletableFuture<Void> activeRequest;
    private volatile boolean requestCancelled;

    public TerminalScreen(TerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 300;
        this.imageHeight = 200;
    }

    @Override
    protected void init() {
        super.init();
        int innerX = leftPos + 8;
        int innerWidth = imageWidth - 16;
        int rowY = topPos + imageHeight - 24;

        int clearW = 42;
        int askW = 38;
        int clearX = innerX + innerWidth - clearW;
        int askX = clearX - 2 - askW;
        int boxW = askX - 2 - innerX;

        input = new EditBox(this.font, innerX, rowY, boxW, 16, Component.translatable("gui.aiterminal.input_hint"));
        input.setMaxLength(512);
        input.setHint(Component.translatable("gui.aiterminal.input_hint"));
        input.setBordered(true);
        addRenderableWidget(input);

        addRenderableWidget(Button.builder(Component.translatable("gui.aiterminal.ask"), b -> submit())
                .bounds(askX, rowY - 1, askW, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.aiterminal.clear"), b -> clear())
                .bounds(clearX, rowY - 1, clearW, 18).build());

        setInitialFocus(input);

        TerminalBlockEntity be = menu.getBlockEntity();
        if (be != null && be.getOutputHistory().isEmpty()) {
            be.addInfo(Component.translatable("gui.aiterminal.welcome").getString());
        }
    }

    private void submit() {
        TerminalBlockEntity be = menu.getBlockEntity();
        if (be == null || be.isLoading() || be.isStreaming()) {
            return;
        }
        String question = input.getValue().trim();
        if (question.isEmpty()) {
            return;
        }

        AITerminalConfig config = AITerminalNeoForge.config();
        if (config.getOpenwebuiModel() == null || config.getOpenwebuiModel().isBlank()) {
            be.addError(Component.translatable("gui.aiterminal.no_model").getString());
            input.setValue("");
            followTail = true;
            return;
        }

        be.addUserLine(question);
        be.setLoading(true);
        be.beginResponse();
        input.setValue("");
        followTail = true;
        requestCancelled = false;

        final int max = config.getMaxResponseLength();
        final int[] streamed = {0};
        final boolean[] truncated = {false};

        activeRequest = AITerminalNeoForge.client().askStreaming(question, new OpenWebUIClient.StreamListener() {
            @Override
            public void onToken(String text) {
                Minecraft.getInstance().execute(() -> {
                    if (requestCancelled || truncated[0]) {
                        return;
                    }
                    String add = text;
                    if (max > 0 && streamed[0] + add.length() > max) {
                        add = add.substring(0, Math.max(0, max - streamed[0]));
                        be.appendResponse(add);
                        be.appendResponse("\n[...truncated]");
                        truncated[0] = true;
                    } else {
                        be.appendResponse(add);
                        streamed[0] += add.length();
                    }
                    followTail = true;
                });
            }

            @Override
            public void onComplete() {
                Minecraft.getInstance().execute(() -> {
                    if (requestCancelled) {
                        return;
                    }
                    be.endResponse();
                    be.setLoading(false);
                    followTail = true;
                });
            }

            @Override
            public void onError(String message) {
                Minecraft.getInstance().execute(() -> {
                    if (requestCancelled) {
                        return;
                    }
                    be.endResponse();
                    be.addError(message);
                    be.setLoading(false);
                    followTail = true;
                });
            }
        });
    }

    private void clear() {
        TerminalBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            be.clearHistory();
            scrollRow = 0;
            followTail = true;
        }
    }

    @Override
    public void removed() {
        requestCancelled = true;
        if (activeRequest != null) {
            activeRequest.cancel(true);
        }
        TerminalBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            be.endResponse();
            be.setLoading(false);
        }
        super.removed();
    }

    // ---- Rendering -------------------------------------------------------

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, COLOR_BORDER);
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, COLOR_BG);

        int innerX = leftPos + 8;
        int innerWidth = imageWidth - 16;
        int outputTop = topPos + 20;
        int outputBottom = topPos + imageHeight - 30;
        int lineHeight = this.font.lineHeight + 2;
        int visibleRows = Math.max(1, (outputBottom - outputTop) / lineHeight);

        g.fill(innerX, topPos + 17, innerX + innerWidth, topPos + 18, COLOR_BORDER);
        g.fill(innerX, outputBottom + 2, innerX + innerWidth, outputBottom + 3, COLOR_BORDER);

        List<RenderLine> lines = buildLines(innerWidth);
        int maxScroll = Math.max(0, lines.size() - visibleRows);
        if (followTail) {
            scrollRow = maxScroll;
        }
        scrollRow = Math.max(0, Math.min(scrollRow, maxScroll));

        int y = outputTop;
        for (int i = scrollRow; i < lines.size() && i < scrollRow + visibleRows; i++) {
            RenderLine line = lines.get(i);
            g.drawString(this.font, line.seq, innerX, y, line.color);
            y += lineHeight;
        }

        if (maxScroll > 0) {
            String marker = scrollRow < maxScroll ? "v" : (scrollRow > 0 ? "^" : "");
            if (!marker.isEmpty()) {
                g.drawString(this.font, marker, innerX + innerWidth - 6, outputBottom - lineHeight, COLOR_INFO);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, Component.translatable("gui.aiterminal.title"), 8, 6, COLOR_RESPONSE, false);
    }

    private List<RenderLine> buildLines(int innerWidth) {
        List<RenderLine> out = new ArrayList<>();
        TerminalBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            for (String encoded : be.getOutputHistory()) {
                int color = colorFor(TerminalLine.kindOf(encoded));
                wrapInto(out, TerminalLine.textOf(encoded), color, innerWidth);
            }
            if (be.isStreaming()) {
                String live = be.getStreamingText();
                if (live.isEmpty()) {
                    out.add(new RenderLine(queryingSequence(), COLOR_INFO));
                } else {
                    wrapInto(out, live, COLOR_RESPONSE, innerWidth);
                }
            } else if (be.isLoading()) {
                out.add(new RenderLine(queryingSequence(), COLOR_INFO));
            }
        }
        return out;
    }

    private void wrapInto(List<RenderLine> out, String text, int color, int innerWidth) {
        for (String paragraph : text.split("\n", -1)) {
            List<FormattedCharSequence> wrapped = this.font.split(Component.literal(paragraph), innerWidth);
            if (wrapped.isEmpty()) {
                out.add(new RenderLine(FormattedCharSequence.EMPTY, color));
            } else {
                for (FormattedCharSequence seq : wrapped) {
                    out.add(new RenderLine(seq, color));
                }
            }
        }
    }

    private FormattedCharSequence queryingSequence() {
        int dots = (int) ((System.currentTimeMillis() / 400) % 4);
        String querying = Component.translatable("gui.aiterminal.querying").getString() + ".".repeat(dots);
        return Component.literal(querying).getVisualOrderText();
    }

    private static int colorFor(TerminalLine.Kind kind) {
        return switch (kind) {
            case USER -> COLOR_USER;
            case RESPONSE -> COLOR_RESPONSE;
            case ERROR -> COLOR_ERROR;
            case INFO -> COLOR_INFO;
        };
    }

    // ---- Input -----------------------------------------------------------

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (input != null && input.isFocused()) {
                submit();
                return true;
            }
        }
        // While the input box is focused, swallow every key except Escape so game keybinds
        // (inventory 'e', hotbar numbers, movement, etc.) can't fire or close the screen.
        // Printable characters are still inserted via charTyped(), which runs independently.
        if (input != null && input.isFocused() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
            input.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            scrollRow -= (int) Math.signum(scrollY);
            followTail = false;
            if (scrollRow < 0) {
                scrollRow = 0;
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    private record RenderLine(FormattedCharSequence seq, int color) {
    }
}
