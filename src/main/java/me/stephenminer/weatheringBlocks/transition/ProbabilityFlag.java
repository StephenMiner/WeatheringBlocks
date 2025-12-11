package me.stephenminer.weatheringBlocks.transition;

import me.stephenminer.weatheringBlocks.WeatheringBlocks;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

public record ProbabilityFlag(Material mat, float mod, int range, boolean ratio) {

    public static ProbabilityFlag parseFlag(String str, int defaultRange, boolean defaultRatio){
        WeatheringBlocks plugin = JavaPlugin.getPlugin(WeatheringBlocks.class);
        String[] unbox = str.split(";");
        if (unbox.length < 2){
            plugin.getLogger().severe("Failed to create Probability flag:" + str + " did not contain enough arguments!");
            return null;
        }
        String sMat = unbox[0];
        Material mat = plugin.materialFromString(sMat);
        if (mat == null){
            plugin.getLogger().severe("Failed to create ProbabilityFlag: couldnt find material for " + sMat);
            return null;
        }
        float mod;
        try{
            mod = Float.parseFloat(unbox[1]);
        }catch (Exception e){
            plugin.getLogger().severe("Failed to create ProbabilityFlag: couldnt find decimal from " + unbox[1]);
            return null;
        }
        if (unbox.length >= 3) {
            for (int i = 2; i < unbox.length; i++){
                String item = unbox[i];
                if (item.equalsIgnoreCase("true") || item.equalsIgnoreCase("false"))
                    defaultRatio = Boolean.parseBoolean(unbox[2]);
            }
        }
        return new ProbabilityFlag(mat, mod, defaultRange, defaultRatio);
    }


}
