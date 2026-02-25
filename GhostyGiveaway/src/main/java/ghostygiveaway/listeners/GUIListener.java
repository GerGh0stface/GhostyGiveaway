package ghostygiveaway.listeners;

import ghostygiveaway.GhostyGiveaway;
import ghostygiveaway.gui.GiveawaySetupGUI;
import ghostygiveaway.gui.SetupSession;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIListener implements Listener {

    private final GhostyGiveaway plugin;
    private final Map<UUID, SetupSession> sessions     = new HashMap<>();
    private final Map<UUID, Inventory>   openInventories = new HashMap<>();

    public GUIListener(GhostyGiveaway plugin) {
        this.plugin = plugin;
    }

    // ── Called from command ───────────────────────────────────────────────────

    public void openSetupGUI(Player player) {
        SetupSession session = new SetupSession();
        Inventory inv = plugin.getSetupGUI().create(player, session);
        sessions.put(player.getUniqueId(), session);
        openInventories.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    // ── Events ────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (!sessions.containsKey(uuid)) return;

        Inventory openInv = openInventories.get(uuid);
        if (openInv == null || !event.getInventory().equals(openInv)) return;

        SetupSession session = sessions.get(uuid);
        int rawSlot = event.getRawSlot();
        ClickType click = event.getClick();

        // ── Shift-click from player inventory → place into prize slot ──────
        if (event.isShiftClick() && rawSlot >= GiveawaySetupGUI.SIZE) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;
            if (isDecorItem(openInv.getItem(GiveawaySetupGUI.PRIZE_SLOT))) {
                openInv.setItem(GiveawaySetupGUI.PRIZE_SLOT, clicked.clone());
                event.setCurrentItem(null);
                session.setPrizeItem(clicked.clone());
                plugin.getSetupGUI().refresh(openInv, session);
            }
            return;
        }

        // ── Prize slot: allow natural pickup/place, sync state after ────────
        if (rawSlot == GiveawaySetupGUI.PRIZE_SLOT) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                ItemStack slotItem = openInv.getItem(GiveawaySetupGUI.PRIZE_SLOT);
                if (slotItem == null || slotItem.getType().isAir() || isDecorItem(slotItem)) {
                    session.setPrizeItem(null);
                    openInv.setItem(GiveawaySetupGUI.PRIZE_SLOT,
                            plugin.getSetupGUI().buildPrizeMarkerPublic());
                } else {
                    session.setPrizeItem(slotItem.clone());
                }
                plugin.getSetupGUI().refresh(openInv, session);
            });
            return; // let it proceed naturally
        }

        // ── Block all other GUI-internal clicks ──────────────────────────────
        if (rawSlot < GiveawaySetupGUI.SIZE) {
            event.setCancelled(true);
        } else {
            return; // player inventory – allow freely
        }

        // ── Button handling ──────────────────────────────────────────────────
        switch (rawSlot) {
            case GiveawaySetupGUI.HOUR_DEC  -> { session.adjustHours(click.isShiftClick()   ? -5  : -1);  plugin.getSetupGUI().refresh(openInv, session); }
            case GiveawaySetupGUI.HOUR_INC  -> { session.adjustHours(click.isShiftClick()   ?  5  :  1);  plugin.getSetupGUI().refresh(openInv, session); }
            case GiveawaySetupGUI.MIN_DEC   -> { session.adjustMinutes(click.isShiftClick() ? -10 : -1);  plugin.getSetupGUI().refresh(openInv, session); }
            case GiveawaySetupGUI.MIN_INC   -> { session.adjustMinutes(click.isShiftClick() ?  10 :  1);  plugin.getSetupGUI().refresh(openInv, session); }
            case GiveawaySetupGUI.START_SLOT  -> handleStart(player, session, openInv);
            case GiveawaySetupGUI.CANCEL_SLOT -> player.closeInventory(); // onClose returns item
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (!sessions.containsKey(uuid)) return;

        Inventory openInv = openInventories.get(uuid);
        if (openInv == null || !event.getInventory().equals(openInv)) return;

        // Cancel any drag that touches non-prize GUI slots
        for (int slot : event.getRawSlots()) {
            if (slot < GiveawaySetupGUI.SIZE && slot != GiveawaySetupGUI.PRIZE_SLOT) {
                event.setCancelled(true);
                return;
            }
        }

        // If dragging into prize slot, sync state
        if (event.getRawSlots().contains(GiveawaySetupGUI.PRIZE_SLOT)) {
            SetupSession session = sessions.get(uuid);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                ItemStack slotItem = openInv.getItem(GiveawaySetupGUI.PRIZE_SLOT);
                session.setPrizeItem((slotItem != null && !slotItem.getType().isAir()) ? slotItem.clone() : null);
                plugin.getSetupGUI().refresh(openInv, session);
            });
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (!sessions.containsKey(uuid)) return;
        Inventory openInv = openInventories.get(uuid);
        if (openInv == null || !event.getInventory().equals(openInv)) return;

        SetupSession session = sessions.remove(uuid);
        openInventories.remove(uuid);

        // Return prize item to player if they didn't start the giveaway
        if (session != null && session.hasPrize()) {
            ItemStack prize = session.getPrizeItem();
            openInv.setItem(GiveawaySetupGUI.PRIZE_SLOT, null);
            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(prize);
            overflow.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void handleStart(Player player, SetupSession session, Inventory inv) {
        // Re-read prize from slot in case events were missed
        ItemStack slotItem = inv.getItem(GiveawaySetupGUI.PRIZE_SLOT);
        if (slotItem != null && !slotItem.getType().isAir() && !isDecorItem(slotItem)) {
            session.setPrizeItem(slotItem.clone());
        }

        if (!session.hasPrize()) {
            player.sendMessage(plugin.getLangManager().get("no-prize-set"));
            return;
        }
        if (!session.hasValidDuration()) {
            player.sendMessage(plugin.getLangManager().get("no-time-set"));
            return;
        }

        ItemStack prize    = session.getPrizeItem();
        long duration      = session.getTotalSeconds();
        UUID uuid          = player.getUniqueId();

        // Remove session before closing to prevent item return
        sessions.remove(uuid);
        openInventories.remove(uuid);
        inv.setItem(GiveawaySetupGUI.PRIZE_SLOT, null);
        player.closeInventory();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getGiveawayManager().startGiveaway(player, prize, duration);
        });
    }

    private boolean isDecorItem(ItemStack item) {
        if (item == null) return true;
        return item.getType() == Material.BLACK_STAINED_GLASS_PANE
                || item.getType() == Material.LIME_STAINED_GLASS_PANE;
    }
}
