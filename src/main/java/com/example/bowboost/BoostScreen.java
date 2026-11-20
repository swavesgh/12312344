package com.example.bowboost;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;

public class BoostScreen extends Screen {
    private Button info, bReturn, bHold, bBoostKey, bHoldKey;
    private KeyMapping capturing = null;

    public BoostScreen() {
        super(Component.literal("Bow Boost"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2 - 100;
        int y = this.height / 2 - 120;

        info = Button.builder(Component.literal(""), b -> {}).bounds(cx, y, 200, 20).build();
        info.active = false;
        addRenderableWidget(info);

        addRenderableWidget(new Opt(cx, y += 24, "Look time: ", " ms", 100, 800, BoostConfig.lookMs, false,
                v -> BoostConfig.lookMs = (int) v));
        addRenderableWidget(new Opt(cx, y += 22, "Bow charge: ", " ms", 300, 1500, BoostConfig.chargeMs, false,
                v -> BoostConfig.chargeMs = (int) v));
        addRenderableWidget(new Opt(cx, y += 22, "Release delay: ", " ms", 0, 400, BoostConfig.releaseBase, false,
                v -> BoostConfig.releaseBase = (int) v));
        addRenderableWidget(new Opt(cx, y += 22, "Ping factor: ", "x", 0, 2, BoostConfig.pingFactor, true,
                v -> BoostConfig.pingFactor = v));
        addRenderableWidget(new Opt(cx, y += 22, "Look angle: ", " deg", 30, 90, BoostConfig.lookDeg, false,
                v -> BoostConfig.lookDeg = (int) v));
        addRenderableWidget(new Opt(cx, y += 22, "Manual ping (0 = auto): ", " ms", 0, 500, BoostConfig.manualPing, false,
                v -> BoostConfig.manualPing = (int) v));

        bReturn = Button.builder(Component.empty(), b -> {
            BoostConfig.returnLook = !BoostConfig.returnLook;
            refresh();
        }).bounds(cx, y += 26, 200, 20).build();
        bHold = Button.builder(Component.empty(), b -> {
            BoostConfig.holdMode = !BoostConfig.holdMode;
            refresh();
        }).bounds(cx, y += 22, 200, 20).build();
        bBoostKey = Button.builder(Component.empty(), b -> {
            capturing = BowBoostClient.BOOST;
            refresh();
        }).bounds(cx, y += 22, 200, 20).build();
        bHoldKey = Button.builder(Component.empty(), b -> {
            capturing = BowBoostClient.HOLD;
            refresh();
        }).bounds(cx, y += 22, 200, 20).build();

        addRenderableWidget(bReturn);
        addRenderableWidget(bHold);
        addRenderableWidget(bBoostKey);
        addRenderableWidget(bHoldKey);
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(cx, y += 28, 200, 20).build());
        refresh();
    }

    private void refresh() {
        bReturn.setMessage(Component.literal("Look back up after boost: " + (BoostConfig.returnLook ? "ON" : "OFF")));
        bHold.setMessage(Component.literal("Hold-bow mode: " + (BoostConfig.holdMode ? "ON" : "OFF")));
        bBoostKey.setMessage(capturing == BowBoostClient.BOOST
                ? Component.literal("Press a key / mouse button...")
                : Component.literal("Boost key: ").append(BowBoostClient.BOOST.getTranslatedKeyMessage()));
        bHoldKey.setMessage(capturing == BowBoostClient.HOLD
                ? Component.literal("Press a key / mouse button...")
                : Component.literal("Hold-mode toggle key: ").append(BowBoostClient.HOLD.getTranslatedKeyMessage()));
    }

    @Override
    public void tick() {
        super.tick();
        if (info != null) {
            info.setMessage(Component.literal("Ping: " + BoostHandler.currentPing()
                    + " ms  |  Release delay: " + BoostHandler.releaseDelay() + " ms"));
        }
    }

    private void bind(InputConstants.Key key) {
        capturing.setKey(key);
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
        capturing = null;
        refresh();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (capturing != null) {
            bind(event.key() == InputConstants.KEY_ESCAPE
                    ? InputConstants.UNKNOWN
                    : InputConstants.Type.KEYSYM.getOrCreate(event.key()));
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (capturing != null) {
            if (event.button() >= 2) { // middle / side buttons
                bind(InputConstants.Type.MOUSE.getOrCreate(event.button()));
            } else {
                capturing = null;
                refresh();
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        BoostConfig.save();
        super.onClose();
    }

    /** Generic labelled slider. */
    private static class Opt extends AbstractSliderButton {
        private final String label, suffix;
        private final double min, max;
        private final boolean decimal;
        private final DoubleConsumer setter;

        Opt(int x, int y, String label, String suffix, double min, double max, double cur,
            boolean decimal, DoubleConsumer setter) {
            super(x, y, 200, 20, Component.empty(), (cur - min) / (max - min));
            this.label = label;
            this.suffix = suffix;
            this.min = min;
            this.max = max;
            this.decimal = decimal;
            this.setter = setter;
            updateMessage();
        }

        private double val() {
            double v = min + this.value * (max - min);
            return decimal ? Math.round(v * 100.0) / 100.0 : Math.round(v);
        }

        @Override
        protected void updateMessage() {
            if (label == null) return;
            double v = val();
            setMessage(Component.literal(label + (decimal ? String.valueOf(v) : String.valueOf((long) v)) + suffix));
        }

        @Override
        protected void applyValue() {
            setter.accept(val());
        }
    }
}
