package com.ricedotwho.rsa.screen.sidl;

import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;
import com.ricedotwho.rsa.utils.api.SessionAPI;
import com.ricedotwho.rsm.utils.Accessor;
import lombok.Getter;
import net.minecraft.client.User;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Taken from FedMaps
 *  <a href="https://github.com/Hypericat/FedMaps/blob/master/src/main/java/me/hypericats/fedmaps/screens/SSIDScreen.java">...</a>
 */
public class SessionLoginScreen extends Screen implements Accessor {

    private static final Pattern TOKEN_REGEX = Pattern.compile("(?:accessToken:\"|token:)?([A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+)");

    private static SessionLoginScreen instance;
    @Getter
    private static User user;
    private final Screen parent;
    private EditBox sessionField;
    private String feedBackMessage = "";
    private int feedBackColor = 0xFFFFFFFF;
    private int centerX = 0;
    private int centerY = 0;

    public static SessionLoginScreen getInstance() {
        if (instance == null) instance = new SessionLoginScreen(null);
        return instance;
    }

    private SessionLoginScreen(Screen parent) {
        super(Component.literal("SessionLogin"));
        this.parent = parent;
    }

    @Override
    public void onClose() {
        mc.setScreen(parent);
    }

    @Override
    protected void init() {
        StringWidget ssidText = new StringWidget(Component.literal("SSID"), mc.font);

        centerX = this.width / 2 - 50;
        centerY = 60;

        sessionField = new EditBox(mc.font, 100, 20, user == null ? Component.empty() : Component.literal(user.getSessionId()));
        ssidText.setWidth(100);
        ssidText.setPosition(centerX, centerY + 35);
        sessionField.setPosition(centerX, centerY + 45);
        sessionField.setMaxLength(10000);

        this.addRenderableWidget(ssidText);
        this.addRenderableWidget(ssidText);
        this.addRenderableWidget(sessionField);
        this.addRenderableWidget(Button.builder(Component.literal("Login"), button -> login()).width(100).pos(centerX, centerY + 70).build());
        this.addRenderableWidget(Button.builder(Component.literal("Reset"), button -> reset()).width(100).pos(centerX, centerY + 95).build());
        this.addRenderableWidget(Button.builder(Component.literal("Copy SSID"), button -> copySSID()).width(100).pos(centerX, centerY + 120).build());
        this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose()).width(100).pos(centerX, centerY + 145).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float deltaTicks) {
        gfx.drawString(mc.font, feedBackMessage, centerX + 50 - (mc.font.width(feedBackMessage) >> 1), centerY, feedBackColor, true);
        String currentUser = "Current Account: " + mc.getUser().getName();
        gfx.drawString(mc.font, currentUser, centerX + 50 - (mc.font.width(currentUser) >> 1), centerY + 10, 0xFFFFFFFF, true);
        super.render(gfx, mouseX, mouseY, deltaTicks);
    }

    private void login() {
        if (sessionField.getValue().isBlank()) {
            feedBackMessage = "Please enter an SSID!";
            feedBackColor = 0xFF8f0000;
            return;
        }

        String ssidText = parseToken(sessionField.getValue().trim());
        String[] info = null;

        for (int i = 0; i < 10; i++) {
            try {
                info = SessionAPI.getProfileInfo(ssidText);
                break;
            } catch (MalformedJsonException | JsonSyntaxException json) {
                feedBackMessage = "Ran out of retries, network error!";
                feedBackColor = 0xFF8f0000;
                System.err.println("Failed to parse json! Retries left: " + i);
            } catch (IOException e) {
                feedBackMessage = "Failed to poll API for username and UUID!";
                feedBackColor = 0xFF8f0000;
                return;
            } catch (Exception e) {
                feedBackMessage = "Invalid SSID!";
                e.printStackTrace();
                feedBackColor = 0xFF8f0000;
                return;
            }
        }

        if (info == null) return;

        try {
            user = new User(info[0], SessionAPI.undashedToUUID(info[1]), ssidText, Optional.empty(), Optional.empty());
        } catch (Exception e) {
            feedBackMessage = "Failed to parse UUID from string!";
            feedBackColor = 0xFF8f0000;
            return;
        }

        feedBackMessage = "Successfully updated session!";
        feedBackColor = 0xFF009405;
    }

    private String parseToken(String input) {
        if (input == null || input.isEmpty()) return "";

        Matcher matcher = TOKEN_REGEX.matcher(input);
        if (matcher.find()) return matcher.group(1);

        return "";
    }

    public static void reset() {
        user = null;
    }

    public static void copySSID() {
        mc.keyboardHandler.setClipboard(mc.getUser().getAccessToken());
    }
}
