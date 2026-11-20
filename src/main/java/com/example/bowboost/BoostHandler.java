package com.example.bowboost;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bow boost state machine. Runs once per client tick (50 ms). The camera interpolates
 * between xRotO and xRot every rendered frame, so the look movement is smooth.
 */
public final class BoostHandler {
    private BoostHandler() {}

    // sequence state
    private static boolean active = false;
    private static boolean jumped = false, released = false;
    private static long t0, lookStart, jumpAt, releaseAt, jumpOff, lookBackStart;
    private static float startPitch;
    private static boolean armed = true; // hold mode: stop using the bow once before re-triggering

    // ping tracking
    private static final List<Integer> pings = new ArrayList<>();
    private static long lastPingSample = 0;
    private static volatile int lastMedian = 0;

    // ---------- helpers ----------
    public static int currentPing() {
        return BoostConfig.manualPing > 0 ? BoostConfig.manualPing : lastMedian;
    }

    public static int releaseDelay() {
        return Math.max(0, Math.round(BoostConfig.releaseBase + (float) (BoostConfig.pingFactor * currentPing())));
    }

    private static void samplePing(Minecraft mc) {
        long now = System.currentTimeMillis();
        if (now - lastPingSample < 1000) return;
        lastPingSample = now;
        ClientPacketListener conn = mc.getConnection();
        if (conn == null || mc.player == null) return;
        PlayerInfo info = conn.getPlayerInfo(mc.player.getUUID());
        if (info == null || info.getLatency() <= 0) return;
        pings.add(info.getLatency());
        if (pings.size() > 10) pings.remove(0);
        List<Integer> sorted = new ArrayList<>(pings);
        Collections.sort(sorted);
        lastMedian = sorted.get(sorted.size() / 2);
    }

    private static boolean holdingBow(LocalPlayer p) {
        return p.getMainHandItem().is(Items.BOW);
    }

    private static boolean drawingBow(LocalPlayer p) {
        return p.isUsingItem() && p.getUseItem().is(Items.BOW);
    }

    private static void msg(LocalPlayer p, String s) {
        p.sendSystemMessage(Component.literal("\u00a7b[BowBoost] \u00a7f" + s));
    }

    private static float ease(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t); // smoothstep
    }

    // ---------- sequence ----------
    private static void start(Minecraft mc, boolean alreadyDrawing) {
        LocalPlayer p = mc.player;
        if (!holdingBow(p)) {
            msg(p, "Hold a bow first.");
            return;
        }
        long now = System.currentTimeMillis();
        long delay = releaseDelay();
        long elapsed = alreadyDrawing ? p.getTicksUsingItem() * 50L : 0L;

        t0 = now - elapsed;
        startPitch = p.getXRot();
        lookStart = now;
        jumpAt = Math.max(t0 + BoostConfig.chargeMs - delay, now + BoostConfig.lookMs);
        releaseAt = jumpAt + delay;
        jumped = false;
        released = false;
        jumpOff = 0;
        active = true;
        if (alreadyDrawing) armed = false;

        if (!alreadyDrawing) {
            mc.options.keyUse.setDown(true);
            if (mc.gameMode != null) mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
        }
    }

    private static void abort(Minecraft mc) {
        if (!active) return;
        mc.options.keyUse.setDown(false);
        mc.options.keyJump.setDown(false);
        active = false;
    }

    // ---------- per tick ----------
    public static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            active = false;
            return;
        }
        samplePing(mc);

        while (BowBoostClient.GUI.consumeClick()) mc.setScreen(new BoostScreen());
        while (BowBoostClient.HOLD.consumeClick()) {
            BoostConfig.holdMode = !BoostConfig.holdMode;
            BoostConfig.save();
            msg(p, "Hold-bow mode " + (BoostConfig.holdMode ? "\u00a7aON" : "\u00a7cOFF"));
        }
        boolean boostPressed = false;
        while (BowBoostClient.BOOST.consumeClick()) boostPressed = true;

        boolean using = drawingBow(p);

        if (!active) {
            if (!using) armed = true;
            if (mc.screen != null) return;
            if (boostPressed) start(mc, false);
            else if (BoostConfig.holdMode && using && armed) start(mc, true);
            return;
        }

        if (mc.screen != null || !holdingBow(p)) {
            abort(mc);
            return;
        }

        long now = System.currentTimeMillis();

        // Smooth look (interpolated by the renderer between xRotO and xRot)
        float target = BoostConfig.lookDeg;
        float len = Math.max(1, BoostConfig.lookMs);
        float pitch;
        if (released && BoostConfig.returnLook) {
            pitch = target + (startPitch - target) * ease((now - lookBackStart) / len);
        } else if (!released) {
            pitch = startPitch + (target - startPitch) * ease((now - lookStart) / len);
        } else {
            pitch = target;
        }
        p.xRotO = p.getXRot();
        p.setXRot(pitch);

        // Jump
        if (!jumped && now >= jumpAt) {
            mc.options.keyJump.setDown(true);
            jumped = true;
            jumpOff = now + 100;
        }
        if (jumped && jumpOff > 0 && now >= jumpOff) {
            mc.options.keyJump.setDown(false);
            jumpOff = 0;
        }

        // Release the arrow
        if (!released && now >= releaseAt) {
            mc.options.keyUse.setDown(false);
            released = true;
            lookBackStart = now;
        }

        // Finish once we've looked back up
        if (released && jumpOff == 0) {
            long wait = BoostConfig.returnLook ? BoostConfig.lookMs : 0;
            if (now >= lookBackStart + wait) active = false;
        }
    }
}
