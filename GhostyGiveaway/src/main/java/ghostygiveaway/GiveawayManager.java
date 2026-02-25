package ghostygiveaway;

import ghostygiveaway.utils.ColorUtils;
import ghostygiveaway.utils.TimeUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class GiveawayManager {

    private final GhostyGiveaway plugin;
    private final Map<String, Giveaway> active = new HashMap<>();
    private final Random random = new Random();
    private static final String ID_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public GiveawayManager(GhostyGiveaway plugin) {
        this.plugin = plugin;
    }

    // ── Public API ──────────────────────────────────────────────────────────

    public Giveaway startGiveaway(Player creator, ItemStack prize, long durationSeconds) {
        String id = generateId();
        Giveaway giveaway = new Giveaway(id, prize, creator, durationSeconds);
        active.put(id, giveaway);
        broadcastStart(giveaway);
        scheduleCountdown(giveaway);
        return giveaway;
    }

    public JoinResult joinGiveaway(Player player, String id) {
        id = id.toUpperCase();
        Giveaway g = active.get(id);
        if (g == null)                         return JoinResult.NOT_FOUND;
        if (g.isEnded())                       return JoinResult.ENDED;
        if (g.hasParticipant(player.getUniqueId())) return JoinResult.ALREADY_JOINED;
        g.addParticipant(player.getUniqueId());
        return JoinResult.SUCCESS;
    }

    public void endGiveaway(String id) {
        Giveaway g = active.get(id.toUpperCase());
        if (g != null && !g.isEnded()) conclude(g);
    }

    public Map<String, Giveaway> getActiveGiveaways() { return Collections.unmodifiableMap(active); }

    public Giveaway getGiveaway(String id) { return active.get(id.toUpperCase()); }

    public void cancelAll() {
        active.values().forEach(g -> {
            if (g.getCountdownTask() != null) g.getCountdownTask().cancel();
            g.setEnded(true);
        });
        active.clear();
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void scheduleCountdown(Giveaway giveaway) {
        List<Long> reminders = plugin.getConfig().getLongList("reminders");
        new BukkitRunnable() {
            @Override public void run() {
                if (giveaway.isEnded()) { cancel(); return; }
                long rem = giveaway.getRemainingSeconds() - 1;
                giveaway.setRemainingSeconds(rem);
                if (rem <= 0) { cancel(); conclude(giveaway); return; }
                if (reminders.contains(rem)) broadcastReminder(giveaway, rem);
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void conclude(Giveaway g) {
        g.setEnded(true);
        if (g.getCountdownTask() != null) g.getCountdownTask().cancel();
        active.remove(g.getId());

        UUID winnerUUID = g.pickWinner();
        if (winnerUUID == null) {
            Bukkit.broadcastMessage(plugin.getLangManager().get(
                    "giveaway-end-no-winner", "id", g.getId()));
            return;
        }

        String winnerName = resolvePlayerName(winnerUUID);
        String prizeName  = getPrizeName(g.getPrize());

        Bukkit.broadcastMessage(plugin.getLangManager().get("giveaway-end-winner",
                "winner", winnerName, "prize", prizeName, "id", g.getId()));

        Player winner = Bukkit.getPlayer(winnerUUID);
        if (winner != null && winner.isOnline()) {
            winner.getInventory().addItem(g.getPrize());
            winner.sendMessage(plugin.getLangManager().get(
                    "giveaway-winner-notify", "prize", prizeName, "id", g.getId()));
        } else {
            plugin.getLogger().warning("[GhostyGiveaway] Winner " + winnerName
                    + " is offline – prize for giveaway " + g.getId() + " was not delivered.");
        }
    }

    private void broadcastStart(Giveaway g) {
        String prize = getPrizeName(g.getPrize());
        String time  = TimeUtils.format(g.getDurationSeconds());

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(plugin.getLangManager().get("giveaway-start-line1"));
            p.sendMessage(plugin.getLangManager().get("giveaway-start-line2", "prize", prize));
            p.sendMessage(plugin.getLangManager().get("giveaway-start-line3", "time", time));
            p.sendMessage(plugin.getLangManager().get("giveaway-start-line4", "host", g.getCreatorName()));
            p.sendMessage(plugin.getLangManager().get("giveaway-start-line5", "id", g.getId()));
            p.spigot().sendMessage(buildJoinComponent(g));
        }
    }

    private void broadcastReminder(Giveaway g, long rem) {
        String prize = getPrizeName(g.getPrize());
        String msg   = plugin.getLangManager().get("reminder",
                "id", g.getId(), "time", TimeUtils.format(rem), "prize", prize);
        TextComponent line = new TextComponent(msg + " ");
        line.addExtra(buildJoinComponent(g));
        Bukkit.getOnlinePlayers().forEach(p -> p.spigot().sendMessage(line));
    }

    private TextComponent buildJoinComponent(Giveaway g) {
        String joinText    = plugin.getLangManager().get("giveaway-join-text");
        String hoverText   = plugin.getLangManager().get("giveaway-join-hover");
        String joinCommand = plugin.getLangManager().get("giveaway-join-command", "id", g.getId());

        TextComponent comp = new TextComponent(joinText);
        comp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, joinCommand));
        comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(new ComponentBuilder(hoverText).create())));
        return comp;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String generateId() {
        String id;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) sb.append(ID_CHARS.charAt(random.nextInt(ID_CHARS.length())));
            id = sb.toString();
        } while (active.containsKey(id));
        return id;
    }

    public static String getPrizeName(ItemStack item) {
        if (item == null) return "Unknown";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName())
            return ColorUtils.stripColor(meta.getDisplayName());
        String raw = item.getType().name().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" "))
            if (!word.isEmpty())
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase()).append(" ");
        String name = sb.toString().trim();
        if (item.getAmount() > 1) name += " x" + item.getAmount();
        return name;
    }

    private String resolvePlayerName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        return Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName()).orElse(uuid.toString());
    }

    // ── Result enum ───────────────────────────────────────────────────────────

    public enum JoinResult { SUCCESS, NOT_FOUND, ENDED, ALREADY_JOINED }
}
