package hu.ColorsASD.minewild.mixin.client;

import hu.ColorsASD.minewild.client.ClientExitOnDisconnect;
import hu.ColorsASD.minewild.client.ClientPauseMenuLinks;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin {
    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void minewild$applyPauseMenuLinks(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        ClientPauseMenuLinks.handleScreenInit(screen);
        minewild$arrangePauseMenu(screen);
    }

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void minewild$refreshPauseMenuLinks(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        ClientPauseMenuLinks.handleScreenInit(screen);
        minewild$arrangePauseMenu(screen);
    }

    @Inject(method = "disconnect", at = @At("HEAD"), require = 0)
    private void minewild$exitAfterDisconnect(CallbackInfo ci) {
        ClientExitOnDisconnect.request();
    }

    @Unique
    private void minewild$arrangePauseMenu(Screen screen) {
        ButtonWidget returnButton = null;
        ButtonWidget disconnectButton = null;
        ButtonWidget webstoreButton = null;
        ButtonWidget websiteButton = null;
        ButtonWidget discordButton = null;
        ButtonWidget supportButton = null;
        ButtonWidget optionsButton = null;
        ButtonWidget advancementsButton = null;
        ButtonWidget statsButton = null;

        for (Element element : screen.children()) {
            if (!(element instanceof ButtonWidget button)) {
                continue;
            }
            if (returnButton == null && ClientPauseMenuLinks.isReturnToGameButton(button.getMessage())) {
                returnButton = button;
                continue;
            }
            if (ClientPauseMenuLinks.isWebstoreButton(button.getMessage())) {
                webstoreButton = button;
                continue;
            }
            if (websiteButton == null && ClientPauseMenuLinks.isWebsiteButton(button.getMessage())) {
                websiteButton = button;
                continue;
            }
            if (discordButton == null && ClientPauseMenuLinks.isDiscordButton(button.getMessage())) {
                discordButton = button;
                continue;
            }
            if (supportButton == null && ClientPauseMenuLinks.isSupportButton(button.getMessage())) {
                supportButton = button;
                continue;
            }
            if (optionsButton == null && ClientPauseMenuLinks.isOptionsButton(button.getMessage())) {
                optionsButton = button;
                continue;
            }
            if (advancementsButton == null && ClientPauseMenuLinks.isAdvancementsButton(button.getMessage())) {
                advancementsButton = button;
                continue;
            }
            if (statsButton == null && ClientPauseMenuLinks.isStatsButton(button.getMessage())) {
                statsButton = button;
                continue;
            }
            if (disconnectButton == null && ClientPauseMenuLinks.isDisconnectButton(button.getMessage())) {
                disconnectButton = button;
            }
        }

        if (returnButton != null) {
            ClientPauseMenuLinks.resetReturnToGameLabel(returnButton);
        }

        ButtonWidget anchor = returnButton != null ? returnButton : disconnectButton;
        if (anchor == null) {
            return;
        }

        int spacing = 4;
        int fullWidth = anchor.getWidth();
        int buttonHeight = anchor.getHeight();
        int baseX = anchor.getX();
        int baseY = anchor.getY();

        if (webstoreButton == null) {
            webstoreButton = ClientPauseMenuLinks.createWebstoreButton(baseX, baseY, fullWidth, buttonHeight);
            ((ScreenInvokerMixin) (Object) screen).minewild$invokeAddDrawableChild(webstoreButton);
        }

        int row1Y = baseY;
        int row2Y = row1Y + buttonHeight + spacing;
        int row3Y = row2Y + buttonHeight + spacing;
        int row4Y = row3Y + buttonHeight + spacing;
        int row5Y = row4Y + buttonHeight + spacing;
        int row6Y = row5Y + buttonHeight + spacing;

        int columnWidth = (fullWidth - spacing) / 2;
        int rightX = baseX + columnWidth + spacing;

        ClientPauseMenuLinks.placeButton(returnButton, baseX, row1Y, fullWidth);
        ClientPauseMenuLinks.placeButton(webstoreButton, baseX, row2Y, fullWidth);
        ClientPauseMenuLinks.placeButton(advancementsButton, baseX, row3Y, columnWidth);
        ClientPauseMenuLinks.placeButton(statsButton, rightX, row3Y, columnWidth);
        ClientPauseMenuLinks.placeButton(discordButton, baseX, row4Y, columnWidth);
        ClientPauseMenuLinks.placeButton(supportButton, rightX, row4Y, columnWidth);
        ClientPauseMenuLinks.placeButton(websiteButton, baseX, row5Y, columnWidth);
        ClientPauseMenuLinks.placeButton(optionsButton, rightX, row5Y, columnWidth);
        if (disconnectButton != null) {
            ClientPauseMenuLinks.placeButton(disconnectButton, baseX, row6Y, fullWidth);
        }
    }
}
