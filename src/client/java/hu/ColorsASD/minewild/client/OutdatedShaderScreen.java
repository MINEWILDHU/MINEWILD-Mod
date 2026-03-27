package hu.ColorsASD.minewild.client;

import hu.ColorsASD.minewild.installer.ModInstaller;
import hu.ColorsASD.minewild.installer.ShaderPackInstaller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;

public final class OutdatedShaderScreen extends RestartRequiredScreen {
    private static final Text TITLE = Text.literal("Shader frissítés szükséges");
    private static final Text BUTTON_UPDATE = Text.literal("Frissítés");
    private boolean lastOutdatedShaderDetected;

    public OutdatedShaderScreen() {
        super(TITLE);
        lastOutdatedShaderDetected = !ShaderPackInstaller.hasOutdatedShaderDetected();
    }

    @Override
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (ModInstaller.isRestartRequired()) {
            if (ModInstaller.hasOutdatedModsDetected() && !ModInstaller.hasExtraModsDetected()) {
                if (!(client.currentScreen instanceof OutdatedModsScreen)) {
                    client.setScreen(new OutdatedModsScreen());
                }
                return;
            }
            if (!(client.currentScreen instanceof RestartRequiredScreen)) {
                client.setScreen(new RestartRequiredScreen());
            }
            return;
        }
        if (!ShaderPackInstaller.hasOutdatedShaderDetected()) {
            if (client.currentScreen == this) {
                client.setScreen(new TitleScreen());
            }
            return;
        }
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        boolean outdatedShader = ShaderPackInstaller.hasOutdatedShaderDetected();
        if (outdatedShader == lastOutdatedShaderDetected) {
            return;
        }
        lastOutdatedShaderDetected = outdatedShader;

        if (outdatedShader) {
            lineOne = Text.literal("Elavult shader van.");
            lineTwo = Text.literal("Kérlek, frissítsd.");
        } else {
            lineOne = Text.literal("Nincs elavult shader.");
            lineTwo = Text.literal("");
        }

        if (actionButton != null) {
            actionButton.setMessage(getActionLabel(false));
            actionButton.visible = outdatedShader;
            actionButton.active = outdatedShader;
        }
    }

    @Override
    protected Text getActionLabel(boolean extraMods) {
        return BUTTON_UPDATE;
    }

    @Override
    protected void handleAction() {
        ShaderPackInstaller.requestOutdatedShaderDeletion();
        MinecraftClient.getInstance().scheduleStop();
    }
}
