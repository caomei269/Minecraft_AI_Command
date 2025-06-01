package com.aicommand.deepseek;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 游戏数据收集器 - 收集当前游戏状态信息用于AI分析
 * Game Data Collector - Collects current game state information for AI analysis
 */
public class GameDataCollector {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * 收集玩家相关数据 / Collect player-related data
     */
    public static PlayerData collectPlayerData() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        
        if (player == null) {
            return new PlayerData();
        }
        
        PlayerData data = new PlayerData();
        data.playerName = player.getName().getString();
        data.health = player.getHealth();
        data.maxHealth = player.getMaxHealth();
        data.foodLevel = player.getFoodData().getFoodLevel();
        data.experienceLevel = player.experienceLevel;
        data.gameMode = mc.gameMode.getPlayerMode().getName();
        
        // 玩家位置 / Player position
        BlockPos pos = player.blockPosition();
        data.positionX = pos.getX();
        data.positionY = pos.getY();
        data.positionZ = pos.getZ();
        
        // 玩家朝向 / Player facing direction
        data.facing = player.getDirection().getName();
        
        return data;
    }
    
    /**
     * 收集物品栏数据 / Collect inventory data
     */
    public static InventoryData collectInventoryData() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        
        if (player == null) {
            return new InventoryData();
        }
        
        InventoryData data = new InventoryData();
        
        // 主手物品 / Main hand item
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty()) {
            data.mainHandItem = getItemInfo(mainHand);
        }
        
        // 副手物品 / Off hand item
        ItemStack offHand = player.getOffhandItem();
        if (!offHand.isEmpty()) {
            data.offHandItem = getItemInfo(offHand);
        }
        
        // 物品栏物品 / Inventory items
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                data.inventoryItems.add(getItemInfo(stack));
            }
        }
        
        return data;
    }
    
    /**
     * 收集世界环境数据 / Collect world environment data
     */
    public static WorldData collectWorldData() {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        LocalPlayer player = mc.player;
        
        if (level == null || player == null) {
            return new WorldData();
        }
        
        WorldData data = new WorldData();
        BlockPos playerPos = player.blockPosition();
        
        // 世界基本信息 / Basic world information
        data.dimensionName = level.dimension().location().toString();
        data.dayTime = level.getDayTime();
        data.isDay = (level.getDayTime() % 24000) < 12000; // 判断是否为白天 / Check if it's day time
        data.isRaining = level.isRaining();
        data.isThundering = level.isThundering();
        
        // 生物群系 / Biome
        Biome biome = level.getBiome(playerPos).value();
        ResourceLocation biomeLocation = ForgeRegistries.BIOMES.getKey(biome);
        if (biomeLocation != null) {
            data.biomeName = biomeLocation.toString();
        }
        
        // 玩家脚下的方块 / Block under player
        BlockPos belowPos = playerPos.below();
        BlockState blockBelow = level.getBlockState(belowPos);
        data.blockBelow = getBlockInfo(blockBelow);
        
        // 玩家周围的方块 / Blocks around player
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (x == 0 && z == 0) continue; // 跳过玩家位置 / Skip player position
                
                BlockPos nearbyPos = playerPos.offset(x, 0, z);
                BlockState nearbyBlock = level.getBlockState(nearbyPos);
                if (!nearbyBlock.isAir()) {
                    data.nearbyBlocks.add(getBlockInfo(nearbyBlock));
                }
            }
        }
        
        return data;
    }
    
    /**
     * 获取物品信息 / Get item information
     */
    private static String getItemInfo(ItemStack stack) {
        ResourceLocation itemLocation = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String itemName = itemLocation != null ? itemLocation.toString() : "unknown";
        
        if (stack.getCount() > 1) {
            return itemName + " x" + stack.getCount();
        }
        return itemName;
    }
    
    /**
     * 获取方块信息 / Get block information
     */
    private static String getBlockInfo(BlockState blockState) {
        Block block = blockState.getBlock();
        ResourceLocation blockLocation = ForgeRegistries.BLOCKS.getKey(block);
        return blockLocation != null ? blockLocation.toString() : "unknown";
    }
    
    /**
     * 生成完整的游戏上下文信息 / Generate complete game context information
     */
    public static String generateGameContext() {
        try {
            PlayerData playerData = collectPlayerData();
            InventoryData inventoryData = collectInventoryData();
            WorldData worldData = collectWorldData();
            
            StringBuilder context = new StringBuilder();
            context.append("=== 当前游戏状态 / Current Game State ===\n");
            
            // 玩家信息 / Player information
            context.append("玩家信息 / Player Info:\n");
            context.append(String.format("- 玩家: %s\n", playerData.playerName));
            context.append(String.format("- 生命值: %.1f/%.1f\n", playerData.health, playerData.maxHealth));
            context.append(String.format("- 饥饿值: %d/20\n", playerData.foodLevel));
            context.append(String.format("- 经验等级: %d\n", playerData.experienceLevel));
            context.append(String.format("- 游戏模式: %s\n", playerData.gameMode));
            context.append(String.format("- 位置: %d, %d, %d\n", playerData.positionX, playerData.positionY, playerData.positionZ));
            context.append(String.format("- 朝向: %s\n", playerData.facing));
            
            // 物品栏信息 / Inventory information
            context.append("\n物品栏信息 / Inventory Info:\n");
            if (inventoryData.mainHandItem != null) {
                context.append(String.format("- 主手: %s\n", inventoryData.mainHandItem));
            }
            if (inventoryData.offHandItem != null) {
                context.append(String.format("- 副手: %s\n", inventoryData.offHandItem));
            }
            if (!inventoryData.inventoryItems.isEmpty()) {
                context.append("- 物品栏物品: ");
                context.append(String.join(", ", inventoryData.inventoryItems.subList(0, Math.min(10, inventoryData.inventoryItems.size()))));
                if (inventoryData.inventoryItems.size() > 10) {
                    context.append(String.format(" (还有%d个物品)", inventoryData.inventoryItems.size() - 10));
                }
                context.append("\n");
            }
            
            // 世界信息 / World information
            context.append("\n世界信息 / World Info:\n");
            context.append(String.format("- 维度: %s\n", worldData.dimensionName));
            context.append(String.format("- 时间: %s\n", worldData.isDay ? "白天" : "夜晚"));
            if (worldData.isRaining) {
                context.append("- 天气: 下雨\n");
            }
            if (worldData.isThundering) {
                context.append("- 天气: 雷暴\n");
            }
            if (worldData.biomeName != null) {
                context.append(String.format("- 生物群系: %s\n", worldData.biomeName));
            }
            if (worldData.blockBelow != null) {
                context.append(String.format("- 脚下方块: %s\n", worldData.blockBelow));
            }
            
            context.append("\n请根据以上游戏状态信息生成合适的Minecraft命令。\n");
            context.append("Please generate appropriate Minecraft commands based on the above game state information.\n");
            
            return context.toString();
            
        } catch (Exception e) {
            LOGGER.error("Error collecting game data", e);
            return "无法获取游戏数据 / Unable to collect game data: " + e.getMessage();
        }
    }
    
    // 数据类 / Data classes
    public static class PlayerData {
        public String playerName = "";
        public float health = 0;
        public float maxHealth = 0;
        public int foodLevel = 0;
        public int experienceLevel = 0;
        public String gameMode = "";
        public int positionX = 0;
        public int positionY = 0;
        public int positionZ = 0;
        public String facing = "";
    }
    
    public static class InventoryData {
        public String mainHandItem = null;
        public String offHandItem = null;
        public List<String> inventoryItems = new ArrayList<>();
    }
    
    public static class WorldData {
        public String dimensionName = "";
        public long dayTime = 0;
        public boolean isDay = true;
        public boolean isRaining = false;
        public boolean isThundering = false;
        public String biomeName = null;
        public String blockBelow = null;
        public List<String> nearbyBlocks = new ArrayList<>();
    }
}