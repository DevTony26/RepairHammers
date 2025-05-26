package DevTony.repairHammers;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Value {

    private final RepairHammers plugin;
    private Map<String, Entry> values = new HashMap<>();

    public static class Entry {
        public final double cost;
        public final int xp;

        public Entry(double cost, int xp) {
            this.cost = cost;
            this.xp = xp;
        }
    }

    public Value(RepairHammers plugin) {
        this.plugin = plugin;
        loadValues();
    }

    private void loadValues() {
        values.clear();
        File dataFile = new File(plugin.getDataFolder(), "item-value.yml");
        if (!dataFile.exists()) {
            plugin.saveResource("item-value.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : yaml.getKeys(false)) {
            double cost = yaml.getDouble(key + ".cost", 0);
            int xp = yaml.getInt(key + ".xp", 0);
            values.put(key.toUpperCase(), new Entry(cost, xp));
        }
    }

    public Entry getEntry(Material type) {
        return values.get(type.name());
    }

    public TotalResult getTotalForItems(ItemStack[] items) {
        double totalCost = 0;
        int totalXp = 0;

        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) continue;
            if (item.getType().getMaxDurability() <= 0) continue; // only repairables
            if (item.getDurability() <= 0) continue; // only damaged

            Entry entry = getEntry(item.getType());
            if (entry != null) {
                totalCost += entry.cost;
                totalXp += entry.xp;
            }
        }

        return new TotalResult(totalCost, totalXp);
    }

    public static class TotalResult {
        public final double cost;
        public final int xp;

        public TotalResult(double cost, int xp) {
            this.cost = cost;
            this.xp = xp;
        }
    }

    /**
     * Show a summary of total repair cost and XP for damaged items in a player's inventory.
     * @param player the player to check
     */
    public void showRepairSummary(Player player) {
        TotalResult result = getTotalForItems(player.getInventory().getContents());
        String prefix = plugin.getMessage("prefix");

        if (result.cost > 0 || result.xp > 0) {
            player.sendMessage(ColorUtil.color(
                    String.format("%s &aRepair Summary:&r &eTotal cost: &6%.2f &7| &eTotal XP: &b%d",
                            prefix != null ? prefix : "", result.cost, result.xp)
            ));
        } else {
            player.sendMessage(ColorUtil.color(
                    String.format("%s &7You have no damaged repairable items in your inventory.",
                            prefix != null ? prefix : "")
            ));
        }
    }

    // Reloads item values from disk
    public void reloadValues() {
        loadValues();
    }
}