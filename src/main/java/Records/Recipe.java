package Records;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.Map;

public record Recipe(
        String name,
        String displayName,
        String[] shape,
        Map<Character, Material> ingredients,
        Material resultMaterial,
        Map<Enchantment, Integer> enchantments
) {}
