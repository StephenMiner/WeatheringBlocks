package me.stephenminer.weatheringBlocks;

import me.stephenminer.weatheringBlocks.listener.RepairBlocks;
import me.stephenminer.weatheringBlocks.listener.WaxStorage;
import me.stephenminer.weatheringBlocks.transition.BlockTransitions;
import me.stephenminer.weatheringBlocks.transition.ProbabilityFlag;
import me.stephenminer.weatheringBlocks.transition.Transition;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WeatheringBlocks extends JavaPlugin {
    public static Pattern HEX_PATTERN = Pattern.compile("(#[A-Fa-f0-9]{6})");
    public Map<Material, BlockTransitions> transitions;
    public Map<String, List<BlockTransitions>> transitionGroups;
    public Set<String> blacklistedWorlds;

    public NamespacedKey itemKey;

    public ChunkManager manager;

    public ConfigFile transitionFile;
    public ConfigFile settingsFile;

    @Override
    public void onEnable() {
        // Plugin startup logic
        registerEvents();
        this.blacklistedWorlds = new HashSet<>();
        transitions = new HashMap<>();
        transitionFile = new ConfigFile(this, "blocks");
        this.settingsFile = new ConfigFile(this, "settings");

        this.itemKey = new NamespacedKey(this, "weathering-item");

        loadTransitions();
        loadBlacklist();
        transitionGroups = new HashMap<>();
        for (BlockTransitions blockTransition : transitions.values()){
            String group = blockTransition.group();
            if (!transitionGroups.containsKey(group))
                transitionGroups.put(group, new ArrayList<>());
            transitionGroups.get(group).add(blockTransition);
        }
        for (List<BlockTransitions> groups : transitionGroups.values())
            groups.sort(Comparator.comparingInt(BlockTransitions::stage));
        manager = new ChunkManager();
        manager.start();
    }

    private void registerEvents(){
        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(new WaxStorage(), this);
        pm.registerEvents(new RepairBlocks(),this);
    }

    private void loadBlacklist(){
        this.blacklistedWorlds = new HashSet<>(this.settingsFile.getConfig().getStringList("blacklist-worlds"));
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
            float chance = (float) this.transitionFile.getConfig().getDouble("transitions." + key + ".chance");
            String group = "general";
            int stage = 0;
            boolean groupingDelay = true;
            boolean lowerTransitionBlocking = true;
            if (this.transitionFile.getConfig().contains("transitions." + key + ".group")) {
                String raw = this.transitionFile.getConfig().getString("transitions." + key + ".group").strip().toLowerCase();
                String[] groupSettings = raw.split(",");
                group = groupSettings[0].strip().toLowerCase();
                if (groupSettings.length >= 2)
                    stage = Integer.parseInt(groupSettings[1].strip().toLowerCase());
                if (groupSettings.length >= 3)
                    groupingDelay = Boolean.parseBoolean(groupSettings[2].strip().toLowerCase());
                if (groupSettings.length >= 4)
                    lowerTransitionBlocking = Boolean.parseBoolean(groupSettings[3].strip().toLowerCase());
            }
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
                float weight;
                try{
                    weight = Float.parseFloat(chanceStr);
                }catch (Exception e){
                    this.getLogger().warning("Skipping potential transition: " + rawTransition + ", " + chanceStr + " is not a proper probability number (in the range 0 - 1)");
                    continue;
                }
               // flagCache.clear();
                int defaultRange = 4;
                boolean defaultRatio = false;
                for (int i = 2; i < Math.min(5, unbox.length); i++){
                    String item = unbox[i].strip().toLowerCase();
                    if (item.contains("ratio;"))
                        defaultRatio = Boolean.parseBoolean(item.split(";")[1]);
                    if (item.contains("range;"))
                        defaultRange = Integer.parseInt(item.split(";")[1]);
                }
                for (int i = 2; i < unbox.length; i++){
                    String item = unbox[i].strip().toLowerCase();
                    if (item.contains("ratio;") || item.contains("range;")) continue;
                    ProbabilityFlag flag = ProbabilityFlag.parseFlag(unbox[i], defaultRange, defaultRatio);
                    if (flag != null)
                        flagCache.add(flag);
                }
                Transition transition = new Transition(transitionMat, weight,  flagCache.toArray(new ProbabilityFlag[0]));
                transitionCache.add(transition);
                flagCache.clear();
            }
            BlockTransitions blockSates = new BlockTransitions(group, stage, groupingDelay, lowerTransitionBlocking, parentMaterial,preChance,chance, transitionCache.toArray(new Transition[0]));
            if (this.transitionFile.getConfig().contains("transitions." + key + ".repairs")){
                Material repairsTo = this.materialFromString(this.transitionFile.getConfig().getString("transitions." + key + ".repairs"));
                blockSates.setRepairTo(repairsTo);
            }

            this.transitions.put(parentMaterial, blockSates);
            transitionCache.clear();
        }
    }

    @Nullable
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

    public ItemStack constructGlue(){
        if (!this.settingsFile.getConfig().contains("glue")) return null;
        String matStr = this.settingsFile.getConfig().getString("glue.name");
        Material type = materialFromString(matStr);
        if (type == null){
            this.getLogger().warning("Failed to parse material " + matStr + ".");
            return null;
        }
        String name = formatColor(ChatColor.translateAlternateColorCodes('&', this.settingsFile.getConfig().getString("glue.name")));
        List<String> lore = new ArrayList<>();
        List<String> temp = this.settingsFile.getConfig().getStringList("glue.lore");
        for (String str : temp){
            lore.add(formatColor(ChatColor.translateAlternateColorCodes('&', str)));
        }
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(this.itemKey, PersistentDataType.STRING, "glue");
        item.setItemMeta(meta);
        return item;
    }

    public String formatColor(String str){
        if (str == null) return null;
        Matcher matcher = HEX_PATTERN.matcher(str);
        while (matcher.find())
            str = str.replace(matcher.group(), "" + ChatColor.of(matcher.group()));
        return str;
    }

    public boolean glueFullRepair(){
        return this.settingsFile.getConfig().getBoolean("settings.glue.full-repair");
    }
    public int glueArea(){
        return this.settingsFile.getConfig().getInt("settings.glue.effect-area");
    }
}
