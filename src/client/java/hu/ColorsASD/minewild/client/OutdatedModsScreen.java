package hu.ColorsASD.minewild.client;

import hu.ColorsASD.minewild.installer.ModInstaller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class OutdatedModsScreen extends RestartRequiredScreen {
    private static final Text TITLE = Text.literal("Frissítés szükséges");
    private static final Text BUTTON_UPDATE = Text.literal("Frissítés");
    private boolean lastOutdatedModsDetected;

    public OutdatedModsScreen() {
        super(TITLE);
        lastOutdatedModsDetected = !ModInstaller.hasOutdatedModsDetected();
    }

    @Override
    protected void updateMessage() {
        boolean outdatedMods = ModInstaller.hasOutdatedModsDetected();
        if (outdatedMods == lastOutdatedModsDetected) {
            return;
        }
        lastOutdatedModsDetected = outdatedMods;

        if (outdatedMods) {
            lineOne = Text.literal("Elavult modok vannak.");
            lineTwo = Text.literal("Kérlek, frissítsd őket.");
        } else {
            lineOne = Text.literal("Nincs elavult mod.");
            lineTwo = Text.literal("");
        }

        if (actionButton != null) {
            actionButton.setMessage(getActionLabel(false));
            actionButton.visible = outdatedMods;
            actionButton.active = outdatedMods;
        }
    }

    @Override
    protected Text getActionLabel(boolean extraMods) {
        return BUTTON_UPDATE;
    }

    @Override
    protected void handleAction() {
        ModInstaller.requestOutdatedModDeletion();
        MinecraftClient.getInstance().scheduleStop();
    }
}
