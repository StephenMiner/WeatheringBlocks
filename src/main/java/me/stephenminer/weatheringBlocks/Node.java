package me.stephenminer.weatheringBlocks;

import org.bukkit.Material;

public record Node(Material mat, float baseChance, Node left, Node right) {
}
