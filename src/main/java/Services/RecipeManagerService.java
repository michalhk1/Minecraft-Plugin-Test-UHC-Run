package Services;

import Records.Recipe;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RecipeManagerService {

    private final Plugin _plugin;

    public RecipeManagerService(Plugin plugin) {
        _plugin = plugin;
    }

    public void registerRecipes() {
        loadRecipesFromJson("/recipes/recipes.json")
                .forEach(recipe -> registerRecipe(createRecipe(recipe)));

        disableVanillaRecipe("shield");
        disableVanillaRecipe("grindstone");
    }

    private void disableVanillaRecipe(String vanillaName) {
        NamespacedKey key = NamespacedKey.minecraft(vanillaName);
        boolean removed = _plugin.getServer().removeRecipe(key);

        if (!removed) {
            _plugin.getLogger().warning("Nepodařilo se odstranit vanilla recept: " + vanillaName);
        }
    }

    private java.util.List<Recipe> loadRecipesFromJson(String resourcePath) {
        java.util.List<Recipe> result = new java.util.ArrayList<>();

        try (InputStream is = _plugin.getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                _plugin.getLogger().warning("Recipe JSON nenalezen: " + resourcePath);
                return result;
            }

            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8)
            ).getAsJsonObject();

            for (var element : root.getAsJsonArray("recipes")) {
                JsonObject obj = element.getAsJsonObject();

                String name = obj.get("name").getAsString();
                String displayName = obj.get("displayName").getAsString();

                var shapeArray = obj.getAsJsonArray("shape");
                String[] shape = new String[shapeArray.size()];
                for (int i = 0; i < shapeArray.size(); i++) {
                    shape[i] = shapeArray.get(i).getAsString();
                }

                Map<Character, Material> ingredients = new HashMap<>();
                JsonObject ingredientsObj = obj.getAsJsonObject("ingredients");
                for (var entry : ingredientsObj.entrySet()) {
                    char key = entry.getKey().charAt(0);
                    Material material = Material.valueOf(entry.getValue().getAsString());
                    ingredients.put(key, material);
                }

                Material resultMaterial = Material.valueOf(obj.get("resultMaterial").getAsString());

                Map<Enchantment, Integer> enchantments = new HashMap<>();
                if (obj.has("enchantments")) {
                    JsonObject enchantsObj = obj.getAsJsonObject("enchantments");
                    for (var entry : enchantsObj.entrySet()) {
                        Enchantment enchant = resolveEnchantment(entry.getKey());
                        if (enchant == null) {
                            _plugin.getLogger().warning("Neznámý enchant v receptu " + name + ": " + entry.getKey());
                            continue;
                        }
                        enchantments.put(enchant, entry.getValue().getAsInt());
                    }
                }

                result.add(new Recipe(name, displayName, shape, ingredients, resultMaterial, enchantments));
            }

        } catch (Exception e) {
            _plugin.getLogger().severe("Chyba při načítání receptů z " + resourcePath + ": " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    private Enchantment resolveEnchantment(String name) {
        NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
        return io.papermc.paper.registry.RegistryAccess
                .registryAccess()
                .getRegistry(io.papermc.paper.registry.RegistryKey.ENCHANTMENT)
                .get(key);
    }

    private void registerRecipe(ShapedRecipe recipe) {
        _plugin.getServer().addRecipe(recipe);
    }

    private ShapedRecipe createRecipe(Recipe recipe) {
        NamespacedKey key = new NamespacedKey(_plugin, recipe.name());

        ItemStack result = new ItemStack(recipe.resultMaterial());
        ItemMeta meta = result.getItemMeta();
        meta.displayName(Component.text(recipe.displayName()).color(NamedTextColor.GOLD));

        if (recipe.enchantments() != null) {
            for (var entry : recipe.enchantments().entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
        }

        result.setItemMeta(meta);

        ShapedRecipe shapedRecipe = new ShapedRecipe(key, result);
        shapedRecipe.shape(recipe.shape());

        for (var entry : recipe.ingredients().entrySet()) {
            shapedRecipe.setIngredient(entry.getKey(), entry.getValue());
        }

        return shapedRecipe;
    }
}