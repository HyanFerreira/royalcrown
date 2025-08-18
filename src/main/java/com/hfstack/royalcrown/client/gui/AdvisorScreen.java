package com.hfstack.royalcrown.client.gui;

import com.hfstack.royalcrown.ModMain;
import com.hfstack.royalcrown.network.AdvisorActionC2S;
import com.hfstack.royalcrown.network.OpenAdvisorScreenS2C;
import com.hfstack.royalcrown.network.RCNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class AdvisorScreen extends Screen {

    private OpenAdvisorScreenS2C.Mode mode;
    private final int reqCit, reqDef, haveCit, defDone;
    private final int advisorEntityId;

    private Button mainBtn;
    private IconButton closeBtn;

    // layout
    private static final int PAD = 14;
    private static final int CLOSE_SIZE = 20;
    // quão “rente” ao painel (borda interna)
    private static final int CLOSE_INSET = 2;
    // espaço extra para o texto não encostar no X
    private static final int CLOSE_MARGIN = 2;
    private static final int BOX_MAX_W = 360;
    private static final int BOX_H = 160;

    // cores
    private static final int SCRIM = 0x90000000;
    private static final int PANEL = 0xAA000000;

    // assets
    private static final ResourceLocation CANCEL_TEX =
            new ResourceLocation(ModMain.MODID, "textures/gui/cancel.png"); // 32x32

    public AdvisorScreen(OpenAdvisorScreenS2C.Mode mode, int reqCit, int reqDef, int haveCit, int defDone, int advisorEntityId) {
        super(Component.translatable("screen.royalcrown.advisor.title"));
        this.mode = mode;
        this.reqCit = reqCit;
        this.reqDef = reqDef;
        this.haveCit = haveCit;
        this.defDone = defDone;
        this.advisorEntityId = advisorEntityId;
    }

    @Override
    protected void init() {
        int boxW = Math.min(this.width - 60, BOX_MAX_W);
        int boxX = (this.width - boxW) / 2;
        int boxY = (this.height - BOX_H) / 2;

        // rótulo do botão principal (translatable)
        Component label = switch (mode) {
            case INTRO1 -> Component.translatable("button.royalcrown.advisor.intro1");
            case INTRO2 -> Component.translatable("button.royalcrown.advisor.intro2");
            case INTRO3 -> Component.translatable("button.royalcrown.advisor.intro3");
            case INTRO4 -> Component.translatable("button.royalcrown.advisor.intro4");
            case STATUS -> Component.translatable("button.royalcrown.advisor.status");
            case CLAIM1 -> Component.translatable("button.royalcrown.advisor.claim1");
            case CLAIM2 -> Component.translatable("button.royalcrown.advisor.claim2");
            case FAREWELL1 -> Component.translatable("button.royalcrown.advisor.farewell1");
            case FAREWELL2 -> Component.translatable("button.royalcrown.advisor.farewell2");
            case FAREWELL3 -> Component.translatable("button.royalcrown.advisor.farewell3");
        };

        // largura baseada no texto + margem
        int btnW = Math.min(boxW - PAD * 2, Math.max(140, this.font.width(label) + 40));
        this.mainBtn = new ColonistButton(
                boxX + (boxW - btnW) / 2,
                boxY + BOX_H - PAD - 16,
                btnW, 16,
                label,
                b -> onClickMain()
        );
        addRenderableWidget(this.mainBtn);

        // X aparece em todas, EXCETO na tela de aceitar (INTRO4)
        boolean showX = (mode != OpenAdvisorScreenS2C.Mode.INTRO4);
        if (showX) {
            this.closeBtn = new IconButton(
                    boxX + boxW - CLOSE_SIZE - CLOSE_INSET,
                    boxY + CLOSE_INSET,
                    CLOSE_SIZE, CLOSE_SIZE,
                    CANCEL_TEX,
                    // pular direto para a tela de aceitar
                    b -> skipToAccept()
            );
            this.closeBtn.setTooltip(Tooltip.create(Component.translatable("screen.royalcrown.advisor.tooltip.skip")));
            addRenderableWidget(this.closeBtn);
        } else {
            this.closeBtn = null;
        }
    }

    /**
     * Pula diretamente para a tela INTRO4 (aceitar missão).
     */
    private void skipToAccept() {
        if (mode != OpenAdvisorScreenS2C.Mode.INTRO4) {
            mode = OpenAdvisorScreenS2C.Mode.INTRO4;
            rebuildWidgets();
        }
    }

    private void onClickMain() {
        switch (mode) {
            case INTRO1 -> {
                mode = OpenAdvisorScreenS2C.Mode.INTRO2;
                rebuildWidgets();
            }
            case INTRO2 -> {
                mode = OpenAdvisorScreenS2C.Mode.INTRO3;
                rebuildWidgets();
            }
            case INTRO3 -> {
                mode = OpenAdvisorScreenS2C.Mode.INTRO4;
                rebuildWidgets();
            }
            case INTRO4 -> {
                RCNetwork.CHANNEL.sendToServer(new AdvisorActionC2S(AdvisorActionC2S.Action.ACCEPT));
                onClose();
            }
            case STATUS -> onClose();

            case CLAIM1 -> {
                mode = OpenAdvisorScreenS2C.Mode.CLAIM2;
                rebuildWidgets();
            }
            case CLAIM2 -> {
                RCNetwork.CHANNEL.sendToServer(new AdvisorActionC2S(AdvisorActionC2S.Action.CLAIM_CROWN, advisorEntityId));
                onClose();
            }

            case FAREWELL1 -> {
                mode = OpenAdvisorScreenS2C.Mode.FAREWELL2;
                rebuildWidgets();
            }
            case FAREWELL2 -> {
                mode = OpenAdvisorScreenS2C.Mode.FAREWELL3;
                rebuildWidgets();
            }
            case FAREWELL3 -> {
                RCNetwork.CHANNEL.sendToServer(new AdvisorActionC2S(AdvisorActionC2S.Action.FAREWELL_DONE, advisorEntityId));
                onClose();
            }
        }
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();
        init();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        // fundo escurecido
        g.fill(0, 0, width, height, SCRIM);

        int boxW = Math.min(this.width - 60, BOX_MAX_W);
        int boxX = (this.width - boxW) / 2;
        int boxY = (this.height - BOX_H) / 2;

        // painel
        g.fill(boxX - 2, boxY - 2, boxX + boxW + 2, boxY + BOX_H + 2, PANEL);

        // área de texto (reserva canto do X)
        boolean showX = (mode != OpenAdvisorScreenS2C.Mode.INTRO4);
        int rightPad = showX ? (CLOSE_SIZE + CLOSE_INSET + CLOSE_MARGIN) : PAD;
        int textW = boxW - PAD - rightPad;
        int x = boxX + PAD;
        int y = boxY + PAD + 2;

        for (FormattedCharSequence line : split(textFor(mode), textW)) {
            g.drawString(this.font, line, x, y, 0xFFFFFF, false);
            y += this.font.lineHeight + 3;
        }

        super.render(g, mouseX, mouseY, partialTicks);
    }

    private List<FormattedCharSequence> split(Component comp, int width) {
        return this.font.split(comp, width);
    }

    private Component textFor(OpenAdvisorScreenS2C.Mode m) {
        return switch (m) {
            case INTRO1 -> Component.translatable("screen.royalcrown.advisor.intro1.text");
            case INTRO2 -> Component.translatable("screen.royalcrown.advisor.intro2.text");
            case INTRO3 -> Component.translatable("screen.royalcrown.advisor.intro3.text");
            case INTRO4 -> Component.translatable("screen.royalcrown.advisor.intro4.text", reqCit, reqDef);
            case STATUS ->
                    Component.translatable("screen.royalcrown.advisor.status.text", haveCit, reqCit, defDone, reqDef);
            case CLAIM1 -> Component.translatable("screen.royalcrown.advisor.claim1.text");
            case CLAIM2 -> Component.translatable("screen.royalcrown.advisor.claim2.text");
            case FAREWELL1 -> Component.translatable("screen.royalcrown.advisor.farewell1.text");
            case FAREWELL2 -> Component.translatable("screen.royalcrown.advisor.farewell2.text");
            case FAREWELL3 -> Component.translatable("screen.royalcrown.advisor.farewell3.text");
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    /* ================== Botões custom ================== */
    private class ColonistButton extends Button {
        private static final ResourceLocation TEX =
                new ResourceLocation(ModMain.MODID, "textures/gui/colonist_button_medium.png");
        private static final int TEX_W = 74, TEX_H = 16;
        private static final int CAP_L = 18, CAP_R = 18;

        ColonistButton(int x, int y, int w, int h, Component label, OnPress onPress) {
            super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean hovered = isHoveredOrFocused();

            g.blit(TEX, x, y, CAP_L, h, 0, 0, CAP_L, TEX_H, TEX_W, TEX_H);
            int centerW = Math.max(0, w - CAP_L - CAP_R);
            int centerU = CAP_L, centerUW = TEX_W - CAP_L - CAP_R;
            if (centerW > 0) g.blitRepeating(TEX, x + CAP_L, y, centerW, h, centerU, 0, centerUW, TEX_H, TEX_W, TEX_H);
            g.blit(TEX, x + CAP_L + centerW, y, CAP_R, h, TEX_W - CAP_R, 0, CAP_R, TEX_H, TEX_W, TEX_H);

            if (hovered) g.fill(x, y, x + w, y + h, 0x26FFFFFF);

            int tw = AdvisorScreen.this.font.width(getMessage());
            int tx = x + (w - tw) / 2;
            int ty = y + (h - AdvisorScreen.this.font.lineHeight) / 2;
            g.drawString(AdvisorScreen.this.font, getMessage(), tx, ty, 0x1B2C73, false);
        }
    }

    private static class IconButton extends Button {
        private final ResourceLocation tex;

        IconButton(int x, int y, int w, int h, ResourceLocation tex, OnPress onPress) {
            super(x, y, w, h, Component.empty(), onPress, DEFAULT_NARRATION);
            this.tex = tex;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
            g.blit(this.tex, getX(), getY(), getWidth(), getHeight(), 0, 0, 32, 32, 32, 32);
            if (isHoveredOrFocused()) g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x40FF0000);
        }
    }
}
