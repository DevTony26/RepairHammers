package DevTony.repairHammers;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.milkbowl.vault.economy.Economy; // Vault API
import org.bukkit.plugin.RegisteredServiceProvider;

public final class RepairHammers extends JavaPlugin {
    private NamespacedKey hammerKey;

    // Economy (Vault)
    private Economy economy;
    private File messagesFile;
    private YamlConfiguration messagesConfig;

    @Override
    public void onEnable() {
        this.hammerKey = new NamespacedKey(this, HammerUtils.HAMMER_KEY);
        saveDefaultConfig();
        this.getCommand("hammer").setExecutor(new HammerCommand(this));
        this.getCommand("hammer").setTabCompleter(new HammerTabCompleter());
        getServer().getPluginManager().registerEvents(new RepairHammerListener(this), this);

        // Setup economy (Vault) if present
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            setupEconomy();
            saveDefaultMessages();

        }

    }
    private void saveDefaultMessages() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getMessage(String key) {
        if (messagesConfig == null) {
            saveDefaultMessages(); // initialize if needed
        }
        String message = messagesConfig.getString(key, "");
        if (message == null) {
            return "";
        }
        // Optionally replace placeholders, e.g. %prefix%
        if (message.contains("%prefix%")) {
            String prefix = messagesConfig.getString("prefix", "");
            message = message.replace("%prefix%", prefix);
        }
        return ColorUtil.color(message);
    }


    public boolean hasEconomy() {
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }

    private void setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
        }
    }

    // These now use "hammers" as top-level in config.yml
    public int getMinFailChance(HammerTier tier) {
        FileConfiguration config = getConfig();
        String path = "hammers." + tier.name() + ".minFail";
        return config.getInt(path, tier.getDefaultMinFail());
    }

    public int getMaxFailChance(HammerTier tier) {
        FileConfiguration config = getConfig();
        String path = "hammers." + tier.name() + ".maxFail";
        return config.getInt(path, tier.getDefaultMaxFail());
    }

    // Get Hammer name from config
    public String getHammerName(HammerTier tier, int failChance) {
        FileConfiguration config = getConfig();
        String path = "hammers." + tier.name() + ".item.name";
        String name = config.getString(path, tier.getDisplayName() + " Repair Hammer");
        name = name.replace("{fail_chance}", String.valueOf(failChance));
        name = ColorUtil.color(name);
        return name;
    }

    // Get lore from config
    public List<String> getHammerLore(HammerTier tier, int failChance) {
        FileConfiguration config = getConfig();
        String path = "hammers." + tier.name() + ".item.lore";
        List<String> lore = config.getStringList(path);
        // Default fallback
        if (lore == null || lore.isEmpty()) {
            lore = Arrays.asList("&7Right-click to repair all items.", "&bFailure Chance: &c" + failChance + "%");
        }
        for (int i = 0; i < lore.size(); i++) {
            String entry = lore.get(i).replace("{fail_chance}", String.valueOf(failChance));
            lore.set(i, ColorUtil.color(entry));
        }
        return lore;
    }

    // Command Executor
    public static class HammerCommand implements CommandExecutor {

        private final RepairHammers plugin;
        private final Random random = new Random();

        public HammerCommand(RepairHammers plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (args.length < 3 || !args[0].equalsIgnoreCase("give")) {
                sender.sendMessage(ColorUtil.color("&cUsage: /hammer give <player> <tier>"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ColorUtil.color("&cPlayer not found."));
                return true;
            }

            HammerTier tier;
            try {
                tier = HammerTier.valueOf(args[2].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ColorUtil.color("&cInvalid tier. (COMMON/UNCOMMON/RARE/EPIC/LEGENDARY)"));
                return true;
            }
            int min = plugin.getMinFailChance(tier);
            int max = plugin.getMaxFailChance(tier);
            int failChance = min + random.nextInt((max - min) + 1);
            ItemStack hammer = HammerUtils.createHammer(tier, failChance, plugin);

            target.getInventory().addItem(hammer);
            target.sendMessage(ColorUtil.color("&aYou received a " + tier.getDisplayName()
                    + " Repair Hammer &7(Failure Chance: &c" + failChance + "%&7)"));
            return true;
        }
    }

    public static class HammerTabCompleter implements TabCompleter {
        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (args.length == 1) {
                return Arrays.asList("give");
            }
            if (args.length == 2) {
                return null; // Let Bukkit handle player completion
            }
            if (args.length == 3) {
                return Arrays.asList("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY");
            }
            return null;
        }
    }
}