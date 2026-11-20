package com.example.bowboost;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class BowBoostClient implements ClientModInitializer {
    public static KeyMapping BOOST;
    public static KeyMapping HOLD;
    public static KeyMapping GUI;

    @Override
    public void onInitializeClient() {
        BoostConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("bowboost", "main"));

        BOOST = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.bowboost.boost", InputConstants.Type.KEYSYM, InputConstants.KEY_G, category));
        HOLD = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.bowboost.hold", InputConstants.Type.KEYSYM, InputConstants.KEY_H, category));
        GUI = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.bowboost.gui", InputConstants.Type.KEYSYM, InputConstants.KEY_RSHIFT, category));

        ClientTickEvents.END_CLIENT_TICK.register(BoostHandler::tick);
    }
}
