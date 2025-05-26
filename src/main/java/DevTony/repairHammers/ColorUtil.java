package DevTony.repairHammers;

import org.bukkit.ChatColor;

public class ColorUtil {
    public static String color(String message) {
        if (message == null) return null;
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}