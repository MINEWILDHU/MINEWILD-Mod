package hu.ColorsASD.minewild.client;

import com.mojang.blaze3d.systems.RenderSystem;
import hu.ColorsASD.minewild.installer.ShaderPackInstaller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ShaderPreferenceScreen extends Screen {
    private static final Text TITLE = Text.literal("Shader beállítás");
    private static final Text LINE_ONE = Text.literal("Betöltsük a shader beállításokat?");
    private static final Text BUTTON_YES = Text.literal("Igen");
    private static final Text BUTTON_NO = Text.literal("Nem");
    private static final Identifier LOGO_TEXTURE = ClientCompat.id("minewild", "textures/gui/restart_logo.png");
    private static final int LOGO_TEXTURE_WIDTH = 965;
    private static final int LOGO_TEXTURE_HEIGHT = 965;
    private static final int LINE_HEIGHT = 9;
    private static final int LINE_SPACING = 3;
    private static final int LOGO_TOP_PADDING = 20;
    private static final int LOGO_GAP = 16;
    private static final int MESSAGE_GAP = 16;
    private static final int BUTTON_GAP = 12;

    private int messageY;
    private int logoX;
    private int logoY;
    private int logoWidth;
    private int logoHeight;

    public ShaderPreferenceScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        int maxLogoWidth = Math.max(1, Math.min(256, this.width - 40));
        int maxLogoHeight = Math.max(1, Math.min(128, this.height / 4));
        float scale = Math.min((float) maxLogoWidth / LOGO_TEXTURE_WIDTH, (float) maxLogoHeight / LOGO_TEXTURE_HEIGHT);
        logoWidth = Math.max(1, Math.round(LOGO_TEXTURE_WIDTH * scale));
        logoHeight = Math.max(1, Math.round(LOGO_TEXTURE_HEIGHT * scale));
        logoX = (this.width - logoWidth) / 2;
        logoY = LOGO_TOP_PADDING;

        messageY = logoY + logoHeight + LOGO_GAP;

        int buttonWidth = 120;
        int buttonHeight = 20;
        int totalButtonsWidth = buttonWidth * 2 + BUTTON_GAP;
        int buttonsX = (this.width - totalButtonsWidth) / 2;
        int buttonY = messageY + LINE_HEIGHT + MESSAGE_GAP;
        int maxButtonY = this.height - 40;
        if (buttonY > maxButtonY) {
            buttonY = maxButtonY;
            messageY = buttonY - LINE_HEIGHT - MESSAGE_GAP;
        }

        this.addDrawableChild(ButtonWidget.builder(BUTTON_YES, button -> handleChoice(true))
                .dimensions(buttonsX, buttonY, buttonWidth, buttonHeight)
                .build());

        this.addDrawableChild(ButtonWidget.builder(BUTTON_NO, button -> handleChoice(false))
                .dimensions(buttonsX + buttonWidth + BUTTON_GAP, buttonY, buttonWidth, buttonHeight)
                .build());
    }

    @Override
    public void tick() {
        super.tick();
        if (ShaderPackInstaller.hasUserPreference()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.currentScreen == this) {
                client.setScreen(new TitleScreen());
            }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        drawBackground(context);
        super.render(context, mouseX, mouseY, delta);
        ClientCompat.resetScissor(context, this.width, this.height);
        renderMessage(context);
        ClientCompat.resetScissor(context, this.width, this.height);
        drawLogo(context);
    }

    @Override
    public void renderBackground(DrawContext context) {
    }

    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    public void renderBackgroundTexture(DrawContext context) {
        drawBackground(context);
    }

    public void renderInGameBackground(DrawContext context) {
        drawBackground(context);
    }

    private void handleChoice(boolean enabled) {
        ShaderPackInstaller.applyUserPreference(enabled);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.currentScreen == this) {
            client.setScreen(new TitleScreen());
        }
    }

    private void renderMessage(DrawContext context) {
        context.drawCenteredTextWithShadow(this.textRenderer, LINE_ONE, this.width / 2, messageY, 0xFFFFFFFF);
    }

    private void drawBackground(DrawContext context) {
        ClientCompat.disablePanorama();
        ClientCompat.resetScissor(context, this.width, this.height);
        if (ClientCompat.isMinecraft1211OrBelow()) {
            ClientCompat.disableDepthTest();
            ClientCompat.depthMask(false);
            RenderSystem.clearColor(0.0f, 0.0f, 0.0f, 1.0f);
            RenderSystem.clear(0x4000, MinecraftClient.IS_SYSTEM_MAC);
        }
        if (ClientCompat.isMinecraft1214OrBelow()) {
            ClientCompat.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            ClientCompat.enableBlend();
            ClientCompat.defaultBlendFunc();
        }
        context.fill(0, 0, this.width, this.height, 0xFF101010);
        if (ClientCompat.isMinecraft1211OrBelow()) {
            ClientCompat.depthMask(true);
            ClientCompat.enableDepthTest();
        }
    }

    private void drawLogo(DrawContext context) {
        if (LOGO_TEXTURE == null) {
            return;
        }
        ClientCompat.disableDepthTest();
        ClientCompat.depthMask(false);
        ClientCompat.enableBlend();
        ClientCompat.defaultBlendFunc();
        ClientCompat.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        MatrixStack matrices = ClientCompat.getMatrixStack(context);
        if (matrices != null) {
            matrices.push();
            matrices.translate(0.0, 0.0, 200.0);
        }
        try {
            if (ClientCompat.isMinecraft1216OrAbove() && ClientCompat.shouldScaleLogo()) {
                ClientCompat.forceNearestSampler(LOGO_TEXTURE);
            }
            if (ClientCompat.isMinecraft1211OrAbove() || ClientCompat.isMinecraft1201Through1210()) {
                ClientCompat.drawTexture(context, LOGO_TEXTURE, logoX, logoY, logoWidth, logoHeight,
                        LOGO_TEXTURE_WIDTH, LOGO_TEXTURE_HEIGHT);
            } else {
                ClientCompat.prepareGuiTexture(LOGO_TEXTURE);
                boolean manualOk = ClientCompat.drawTextureManual(context, LOGO_TEXTURE, logoX, logoY, logoWidth, logoHeight);
                if (!manualOk) {
                    ClientCompat.drawTextureLegacy(context, LOGO_TEXTURE, logoX, logoY, logoWidth, logoHeight,
                            LOGO_TEXTURE_WIDTH, LOGO_TEXTURE_HEIGHT);
                }
            }
        } finally {
            if (matrices != null) {
                matrices.pop();
            }
            ClientCompat.depthMask(true);
            ClientCompat.enableDepthTest();
        }
    }
}
