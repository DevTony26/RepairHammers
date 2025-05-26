package DevTony.repairHammers;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.ArrayList;

// Utility methods for creating, identifying, and extracting data from custom "Repair Hammer" items in Bukkit/Spigot plugins.

public class HammerUtils {

    public static final String HAMMER_KEY = "repair_hammer";

    // Generates a hidden tag for item identification
    private static String hammerTag(HammerTier tier, int failChance) {
        return "§7[RepairHammer:" + tier.name() + ":" + failChance + "]";
    }

    /**
     * Creates a custom Repair Hammer item.
     *
     * @param tier       Hammer rarity/tier
     * @param failChance Failure chance (percentage)
     * @param plugin     Reference to main plugin (for config access)
     * @return Configured ItemStack representing the hammer
     */
    public static ItemStack createHammer(HammerTier tier, int failChance, RepairHammers plugin) {
        Material mat = Material.getMaterial(
                plugin.getConfig().getString("hammers." + tier.name() + ".material", "STONE_AXE")
        );
        if (mat == null) mat = Material.STONE_AXE;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Set display name from plugin config
        meta.setDisplayName(plugin.getHammerName(tier, failChance));

        // Add lore from config & secret tag
        List<String> lore = new ArrayList<>(plugin.getHammerLore(tier, failChance));
        lore.add(hammerTag(tier, failChance));
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Checks if the given item is a Repair Hammer.
     *
     * @param item   The item to check
     * @param plugin Plugin (for compatibility)
     * @return true if item is a Repair Hammer
     */
    public static boolean isRepairHammer(ItemStack item, RepairHammers plugin) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        for (String line : meta.getLore()) {
            if (line != null && line.contains("[RepairHammer:")) return true;
        }
        return false;
    }

    /**
     * Extracts HammerTier from a Repair Hammer item.
     *
     * @param item ItemStack (hammer)
     * @return HammerTier, or null if not present/invalid
     */
    public static HammerTier getHammerTier(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        if (!item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;
        for (String line : meta.getLore()) {
            if (line != null && line.contains("[RepairHammer:")) {
                try {
                    String raw = line.replace("§7", "").replace("[RepairHammer:", "").replace("]", "");
                    String[] parts = raw.split(":");
                    if (parts.length >= 2) return HammerTier.valueOf(parts[0]);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /**
     * Gets fail chance from a Repair Hammer item.
     *
     * @param item ItemStack (hammer)
     * @return Failure chance as int, or -1 if not found/invalid
     */
    public static int getHammerFailChance(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return -1;
        if (!item.hasItemMeta()) return -1;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return -1;
        for (String line : meta.getLore()) {
            if (line != null && line.contains("[RepairHammer:")) {
                try {
                    String raw = line.replace("§7", "").replace("[RepairHammer:", "").replace("]", "");
                    String[] parts = raw.split(":");
                    if (parts.length >= 2) return Integer.parseInt(parts[parts.length - 1]);
                } catch (Exception ignored) {}
            }
        }
        return -1;
    }
}