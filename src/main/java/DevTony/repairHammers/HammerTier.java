package DevTony.repairHammers;

import org.bukkit.ChatColor;

public enum HammerTier {
    COMMON("Common", ChatColor.WHITE, 30, 50),
    UNCOMMON("Uncommon", ChatColor.GREEN, 20, 29),
    RARE("Rare", ChatColor.BLUE, 10, 19),
    EPIC("Epic", ChatColor.LIGHT_PURPLE, 5, 9),
    LEGENDARY("Legendary", ChatColor.GOLD, 1, 4);

    private final String name;
    private final ChatColor color;
    private final int defaultMinFail;
    private final int defaultMaxFail;

    HammerTier(String name, ChatColor color, int minFail, int maxFail) {
        this.name = name;
        this.color = color;
        this.defaultMinFail = minFail;
        this.defaultMaxFail = maxFail;
    }

    public String getDisplayName() {
        return color + name;
    }

    public int getDefaultMinFail() {
        return defaultMinFail;
    }

    public int getDefaultMaxFail() {
        return defaultMaxFail;
    }
}