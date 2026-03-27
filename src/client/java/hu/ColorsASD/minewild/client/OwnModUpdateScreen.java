package hu.ColorsASD.minewild.client;

import hu.ColorsASD.minewild.installer.OwnModUpdater;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;

public final class OwnModUpdateScreen extends RestartRequiredScreen {
    private static final Text TITLE = Text.literal("Frissítés szükséges");
    private static final Text BUTTON_UPDATE = Text.literal("Frissítés");
    private boolean lastUpdateAvailable;

    public OwnModUpdateScreen() {
        super(TITLE);
        lastUpdateAvailable = !OwnModUpdater.hasUpdateAvailable();
    }

    @Override
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!OwnModUpdater.hasUpdateAvailable()) {
            if (client.currentScreen == this) {
                client.setScreen(new TitleScreen());
            }
            return;
        }
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        boolean updateAvailable = OwnModUpdater.hasUpdateAvailable();
        if (updateAvailable == lastUpdateAvailable) {
            return;
        }
        lastUpdateAvailable = updateAvailable;

        if (updateAvailable) {
            lineOne = Text.literal("Új verzió jelent meg.");
            lineTwo = Text.literal("Kérlek, frissítsd.");
        } else {
            lineOne = Text.literal("Nincs új verzió.");
            lineTwo = Text.literal("");
        }

        if (actionButton != null) {
            actionButton.setMessage(getActionLabel(false));
            actionButton.visible = updateAvailable;
            actionButton.active = updateAvailable;
        }
    }

    @Override
    protected Text getActionLabel(boolean extraMods) {
        return BUTTON_UPDATE;
    }

    @Override
    protected void handleAction() {
        if (!OwnModUpdater.requestUpdate()) {
            return;
        }
        MinecraftClient.getInstance().scheduleStop();
    }
}
