package com.aicommand.deepseek;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = AICommand.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeyBindings {
    
    public static final String KEY_CATEGORY = "key.categories.aicommand";
    public static final String KEY_OPEN_GUI = "key.aicommand.open_gui";
    
    public static KeyMapping OPEN_GUI_KEY;
    
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        OPEN_GUI_KEY = new KeyMapping(
            KEY_OPEN_GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G, // Default key: G
            KEY_CATEGORY
        );
        event.register(OPEN_GUI_KEY);
    }
    
    @Mod.EventBusSubscriber(modid = AICommand.MODID, value = Dist.CLIENT)
    public static class ClientEvents {
        
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                while (OPEN_GUI_KEY.consumeClick()) {
                    Minecraft.getInstance().setScreen(new AICommandScreen());
                }
            }
        }
    }
}