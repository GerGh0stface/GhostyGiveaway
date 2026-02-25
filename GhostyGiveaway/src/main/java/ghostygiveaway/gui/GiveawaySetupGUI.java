package ghostygiveaway.gui;

import ghostygiveaway.GhostyGiveaway;
import ghostygiveaway.GiveawayManager;
import ghostygiveaway.utils.ColorUtils;
import ghostygiveaway.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 4-row GUI layout (36 slots):
 *
 * Row 0 : [■][■][■][■][■][■][■][■][■]   border
 * Row 1 : [■][📖][■][■][PRIZE][■][■][■][■]
 * Row 2 : [■][🔴][⏰H][🟢][■][🔴][⏰M][🟢][■]
 * Row 3 : [■][■][❌][■][■][■][✅][■][■]
 */
public class GiveawaySetupGUI {

    public static final int SIZE        = 36;
    public static final int PRIZE_SLOT  = 13;
    public static final int INFO_SLOT   = 10;
    public static final int HOUR_DEC    = 19;
    public static final int HOUR_DISP   = 20;
    public static final int HOUR_INC    = 21;
    public static final int MIN_DEC     = 23;
    public static final int MIN_DISP    = 24;
    public static final int MIN_INC     = 25;
    public static final int CANCEL_SLOT = 29;
    public static final int START_SLOT  = 33;

    private final GhostyGiveaway plugin;

    public GiveawaySetupGUI(GhostyGiveaway plugin) {
        this.plugin = plugin;
    }

    public Inventory create(Player player, SetupSession session) {
        String title = plugin.getLangManager().get("gui-title");
        Inventory inv = Bukkit.createInventory(player, SIZE, title);
        populate(inv, session);
        return inv;
    }

    public void refresh(Inventory inv, SetupSession session) {
        inv.setItem(INFO_SLOT,  buildInfo(session));
        inv.setItem(HOUR_DISP,  buildHourDisplay(session));
        inv.setItem(MIN_DISP,   buildMinDisplay(session));
        inv.setItem(START_SLOT, buildStart(session));
    }

    private void populate(Inventory inv, SetupSession session) {
        ItemStack filler = make(Material.BLACK_STAINED_GLASS_PANE, " ", new ArrayList<>());
        for (int i = 0; i < SIZE; i++) inv.setItem(i, filler);

        inv.setItem(PRIZE_SLOT,  buildPrizeMarkerPublic());
        inv.setItem(INFO_SLOT,   buildInfo(session));
        inv.setItem(HOUR_DEC,    make(Material.RED_DYE,  ColorUtils.colorize("&c&l◀ &bHours"),  plugin.getLangManager().getList("gui-hours-lore")));
        inv.setItem(HOUR_DISP,   buildHourDisplay(session));
        inv.setItem(HOUR_INC,    make(Material.LIME_DYE, ColorUtils.colorize("&a&l▶ &bHours"),  plugin.getLangManager().getList("gui-hours-lore")));
        inv.setItem(MIN_DEC,     make(Material.RED_DYE,  ColorUtils.colorize("&c&l◀ &bMinutes"),plugin.getLangManager().getList("gui-minutes-lore")));
        inv.setItem(MIN_DISP,    buildMinDisplay(session));
        inv.setItem(MIN_INC,     make(Material.LIME_DYE, ColorUtils.colorize("&a&l▶ &bMinutes"),plugin.getLangManager().getList("gui-minutes-lore")));
        inv.setItem(CANCEL_SLOT, buildCancel());
        inv.setItem(START_SLOT,  buildStart(session));
    }

    // ── Item builders ─────────────────────────────────────────────────────────

    public ItemStack buildPrizeMarkerPublic() {
        String name = plugin.getLangManager().get("gui-prize-slot-name");
        List<String> lore = plugin.getLangManager().getList("gui-prize-slot-lore");
        return make(Material.LIME_STAINED_GLASS_PANE, name, lore);
    }

    private ItemStack buildInfo(SetupSession session) {
        String name = plugin.getLangManager().get("gui-info-name");
        List<String> lore = plugin.getLangManager().getList("gui-info-lore",
                Map.of("time", TimeUtils.format(session.getTotalSeconds())));
        return make(Material.BOOK, name, lore);
    }

    private ItemStack buildHourDisplay(SetupSession session) {
        String name = plugin.getLangManager().get("gui-hours-name", "hours", String.valueOf(session.getHours()));
        return make(Material.CLOCK, name, new ArrayList<>());
    }

    private ItemStack buildMinDisplay(SetupSession session) {
        String name = plugin.getLangManager().get("gui-minutes-name", "minutes", String.valueOf(session.getMinutes()));
        return make(Material.CLOCK, name, new ArrayList<>());
    }

    private ItemStack buildCancel() {
        String name = plugin.getLangManager().get("gui-cancel-name");
        List<String> lore = plugin.getLangManager().getList("gui-cancel-lore");
        return make(Material.BARRIER, name, lore);
    }

    private ItemStack buildStart(SetupSession session) {
        String prizeName = session.hasPrize()
                ? GiveawayManager.getPrizeName(session.getPrizeItem())
                : ColorUtils.colorize("&cNot set");
        String name = plugin.getLangManager().get("gui-start-name");
        List<String> lore = plugin.getLangManager().getList("gui-start-lore",
                Map.of("time", TimeUtils.format(session.getTotalSeconds()), "prize", prizeName));
        Material mat = (session.hasPrize() && session.hasValidDuration())
                ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK;
        return make(mat, name, lore);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ItemStack make(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(name));
            if (!lore.isEmpty()) {
                meta.setLore(lore.stream().map(ColorUtils::colorize).collect(Collectors.toList()));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
