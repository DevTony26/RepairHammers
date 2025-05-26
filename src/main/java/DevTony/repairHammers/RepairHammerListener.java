package DevTony.repairHammers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Effect;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;

public class RepairHammerListener implements Listener {

    private final RepairHammers plugin;
    private final Value valueUtil;

    public RepairHammerListener(RepairHammers plugin) {
        this.plugin = plugin;
        this.valueUtil = new Value(plugin);
        startActionBarTask();
    }

    @EventHandler
    public void onPlayerUseHammer(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!HammerUtils.isRepairHammer(item, plugin)) return;

        // Only right-click (not left)
        if (event.getAction().name().contains("LEFT")) return;
        event.setCancelled(true); // Prevent normal behavior

        // Get hammer tier and fail chance
        HammerTier tier = HammerUtils.getHammerTier(item);
        int failChance = HammerUtils.getHammerFailChance(item);

        // Calculate repair cost and xp
        Value.TotalResult costs = valueUtil.getTotalForItems(player.getInventory().getContents());

        // Nothing to repair
        if (costs.cost <= 0 && costs.xp <= 0) {
            player.sendMessage(plugin.getMessage("repair_none_available")
                    .replace("{prefix}", plugin.getMessage("prefix")));
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1, 1.2f);
            return;
        }

        // XP check
        if (player.getLevel() < costs.xp) {
            player.sendMessage(ColorUtil.color("&cNot enough XP. &7Required: &b" + costs.xp));
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1, 1f);
            return;
        }

        // Money check if Vault/Economy is enabled
        if (plugin.hasEconomy()) {
            if (!plugin.getEconomy().has(player, costs.cost)) {
                player.sendMessage(ColorUtil.color("&cNot enough money. &7Required: &6" + String.format("%.2f", costs.cost)));
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1, 1f);
                return;
            }
        }

        // Failure check
        try {
            if (failChance > 0
                    && plugin.getServer().getScheduler()
                    .callSyncMethod(plugin, () -> Math.random() * 100).get() < failChance) {
                player.sendMessage(ColorUtil.color("&4Repair attempt failed! &7(No items were repaired)"));
                player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 0, 32);
                player.playSound(player.getLocation(), Sound.ITEM_BREAK, 0.6f, 0.7f);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(ColorUtil.color("&c[RepairHammers] Internal error during repair attempt."));
            return;
        }

        // Deduct XP if needed
        if (costs.xp > 0) {
            player.setLevel(player.getLevel() - costs.xp);
        }
        // Deduct money if economy is enabled
        if (plugin.hasEconomy() && costs.cost > 0) {
            plugin.getEconomy().withdrawPlayer(player, costs.cost);
        }

        // Repair process
        ItemStack[] items = player.getInventory().getContents();
        int repaired = 0;
        for (int i = 0; i < items.length; i++) {
            ItemStack invItem = items[i];
            if (invItem == null) continue;
            // Only repair if damaged and listed in item-value.yml
            if (invItem.getType().getMaxDurability() > 0 && invItem.getDurability() > 0) {
                Value.Entry entry = valueUtil.getEntry(invItem.getType());
                if (entry != null) {
                    invItem.setDurability((short) 0);
                    items[i] = invItem;
                    repaired++;
                }
            }
        }
        player.getInventory().setContents(items);

        // Feedback
        player.sendMessage(ColorUtil.color("&aAll damaged items repaired! &7(&6" + repaired + "&7 items repaired)"));
        player.playSound(player.getLocation(), Sound.ANVIL_USE, 0.85f, 1.08f);
        player.getWorld().playEffect(player.getLocation(), Effect.FIREWORKS_SPARK, 0, 48);

        // Optionally damage or consume hammer here, if you want.

        // Action bar update after repair
        updateActionBar(player);
    }

    @EventHandler
    public void onItemSwitch(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> updateActionBar(player), 2L);
    }

    private void startActionBarTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateActionBar(player);
            }
        }, 0L, 20L); // every second, adjust if desired
    }

    private void updateActionBar(Player player) {
        ItemStack hand = player.getInventory().getItemInHand();
        if (HammerUtils.isRepairHammer(hand, plugin)) {
            HammerTier tier = HammerUtils.getHammerTier(hand);
            int failChance = HammerUtils.getHammerFailChance(hand);

            String hammerName = hand.getItemMeta() != null && hand.getItemMeta().hasDisplayName()
                    ? hand.getItemMeta().getDisplayName()
                    : (tier != null ? tier.getDisplayName() + " Repair Hammer" : "Repair Hammer");

            // Calculate cost and xp needed for current inventory
            Value.TotalResult result = valueUtil.getTotalForItems(player.getInventory().getContents());
            String costStr = String.format("%.2f", result.cost);

            String actionBarMsg = String.format(
                    "%s &7| &eTier: %s &7| &cFail Chance: %d%% &7| &6Cost: %s &7| &bXP: %d",
                    hammerName,
                    tier != null ? tier.getDisplayName() : "&cUnknown",
                    failChance,
                    costStr,
                    result.xp
            );
            sendActionBar(player, ColorUtil.color(actionBarMsg));
        }
    }

    // Works for 1.8.8 - Sends action bar messages via packet
    public static void sendActionBar(Player player, String message) {
        try {
            Object packet = getChatPacket(message, (byte) 2);
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            Method sendPacket = playerConnection.getClass().getMethod("sendPacket", getNmsClass("Packet"));
            sendPacket.invoke(playerConnection, packet);
        } catch (Exception ignored) { }
    }

    private static Object getChatPacket(String msg, byte pos) throws Exception {
        Class<?> chatComp = getNmsClass("IChatBaseComponent");
        Class<?> chatPacket = getNmsClass("PacketPlayOutChat");
        Class<?> serializer = chatComp.getDeclaredClasses()[0];
        Object cmp = serializer.getMethod("a", String.class).invoke(null, "{\"text\":\"" + msg.replace("\"", "\\\"") + "\"}");
        return chatPacket.getConstructor(chatComp, byte.class).newInstance(cmp, pos);
    }

    private static Class<?> getNmsClass(String name) throws Exception {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        return Class.forName("net.minecraft.server." + version + "." + name);
    }
}