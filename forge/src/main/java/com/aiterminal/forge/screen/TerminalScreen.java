package com.aiterminal.forge.screen;

import com.aiterminal.common.config.AITerminalConfig;
import com.aiterminal.common.terminal.TerminalLine;
import com.aiterminal.forge.AITerminalForge;
import com.aiterminal.forge.block.TerminalBlockEntity;
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

/**
 * Retro green-on-black terminal screen (Forge). Scrollable output on top, single-line input with
 * Ask/Clear at the bottom. Queries run asynchronously; results are applied back on the client thread.
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
        if (be == null || be.isLoading()) {
            return;
        }
        String question = input.getValue().trim();
        if (question.isEmpty()) {
            return;
        }

        AITerminalConfig config = AITerminalForge.config();
        if (config.getOpenwebuiModel() == null || config.getOpenwebuiModel().isBlank()) {
            be.addError(Component.translatable("gui.aiterminal.no_model").getString());
            input.setValue("");
            followTail = true;
            return;
        }

        be.addUserLine(question);
        be.setLoading(true);
        input.setValue("");
        followTail = true;

        AITerminalForge.client().ask(question).whenComplete((response, throwable) -> {
            Minecraft.getInstance().execute(() -> {
                be.setLoading(false);
                followTail = true;
                if (throwable != null) {
                    be.addError("Request failed: " + throwable.getMessage());
                } else if (response.success()) {
                    be.addResponse(truncate(response.content(), config.getMaxResponseLength()));
                } else {
                    be.addError(response.errorMessage());
                }
            });
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

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        if (max > 0 && text.length() > max) {
            return text.substring(0, max) + "\n[...truncated]";
        }
        return text;
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
                String text = TerminalLine.textOf(encoded);
                for (String paragraph : text.split("\n", -1)) {
                    List<FormattedCharSequence> wrapped =
                            this.font.split(Component.literal(paragraph), innerWidth);
                    if (wrapped.isEmpty()) {
                        out.add(new RenderLine(FormattedCharSequence.EMPTY, color));
                    } else {
                        for (FormattedCharSequence seq : wrapped) {
                            out.add(new RenderLine(seq, color));
                        }
                    }
                }
            }
            if (be.isLoading()) {
                int dots = (int) ((System.currentTimeMillis() / 400) % 4);
                String querying = Component.translatable("gui.aiterminal.querying").getString()
                        + ".".repeat(dots);
                out.add(new RenderLine(Component.literal(querying).getVisualOrderText(), COLOR_INFO));
            }
        }
        return out;
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
        if (input != null && input.isFocused() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
            return input.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
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
