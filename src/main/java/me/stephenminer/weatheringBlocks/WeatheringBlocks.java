package me.stephenminer.weatheringBlocks;

import me.stephenminer.weatheringBlocks.transition.BlockTransitions;
import me.stephenminer.weatheringBlocks.transition.ProbabilityFlag;
import me.stephenminer.weatheringBlocks.transition.Transition;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class WeatheringBlocks extends JavaPlugin {
    public Map<Material, BlockTransitions> transitions;

    public ChunkManager manager;

    public ConfigFile transitionFile;

    @Override
    public void onEnable() {
        // Plugin startup logic
        transitions = new HashMap<>();
        transitionFile = new ConfigFile(this, "blocks");
        loadTransitions();
        manager = new ChunkManager();
        manager.start();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        transitions.clear();
        manager.stop();
    }

    public void loadTransitions(){
        Set<String> keys = this.transitionFile.getConfig().getConfigurationSection("transitions").getKeys(false);
        List<ProbabilityFlag> flagCache = new ArrayList<>();
        List<Transition> transitionCache = new ArrayList<>();
        for (String key : keys){
            Material parentMaterial = materialFromString(key);
            if (parentMaterial == null){
                this.getLogger().severe("Failed to generate transition for " + key + ": " + key + " isn't a real material!");
                continue;
            }
            float preChance = (float) this.transitionFile.getConfig().getDouble("transitions." + key + ".pre-chance");
            List<String> rawTransitions = this.transitionFile.getConfig().getStringList("transitions." + key + ".states");
            for (String rawTransition : rawTransitions){
                String[] unbox = rawTransition.split(",");
                if (unbox.length < 2) {
                    this.getLogger().warning("Skipping potential transition: " + rawTransition + ", not enough arguments!");
                    continue;
                }
                String matStr = unbox[0].trim();
                Material transitionMat = materialFromString(matStr);
                if (transitionMat == null){
                    this.getLogger().warning("Skipping potential transition:" + rawTransition + ", " + matStr + " isn't a real material for child transition!");
                    continue;
                }
                String chanceStr = unbox[1].trim();
                float chance;
                try{
                    chance = Float.parseFloat(chanceStr);
                }catch (Exception e){
                    this.getLogger().warning("Skipping potential transition: " + rawTransition + ", " + chanceStr + " is not a proper probability number (in the range 0 - 1)");
                    continue;
                }
               // flagCache.clear();
                int defaultRange = 4;
                boolean defaultRatio = false;
                for (int i = 2; i < Math.min(4, unbox.length); i++){
                    String item = unbox[i].strip();
                    if (item.contains("ratio;"))
                        defaultRatio = Boolean.parseBoolean(item.split(";")[1]);
                    if (item.contains("range;"))
                        defaultRange = Integer.parseInt(item.split(";")[1]);
                }
                for (int i = 4; i < unbox.length; i++){
                    ProbabilityFlag flag = ProbabilityFlag.parseFlag(unbox[i], defaultRange, defaultRatio);
                    if (flag != null)
                        flagCache.add(flag);
                }
                Transition transition = new Transition(transitionMat, chance, flagCache.toArray(new ProbabilityFlag[0]));
                transitionCache.add(transition);
                flagCache.clear();
            }
            BlockTransitions blockSates = new BlockTransitions(parentMaterial,preChance,transitionCache.toArray(new Transition[0]));
            this.transitions.put(parentMaterial, blockSates);
            transitionCache.clear();
        }
    }

    public Material materialFromString(String str){
        str = str.toLowerCase().strip();
        Material mat = null;
        try {
            mat = Registry.MATERIAL.get(NamespacedKey.minecraft(str.toLowerCase().trim()));
        }catch (Exception e){
            JavaPlugin.getPlugin(WeatheringBlocks.class).getLogger().info("Failed to get minecraft namespace for " + str +  ", attempting generic namespace generation");
        }
        if (mat == null){
            NamespacedKey key = NamespacedKey.fromString(str);
            mat = Registry.MATERIAL.get(key);
        }
        if (mat == null){
            JavaPlugin.getPlugin(WeatheringBlocks.class).getLogger().warning("Failed to generate material from: " + str );
        }
        return mat;
    }
}
