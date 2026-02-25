package ghostygiveaway.commands;

import ghostygiveaway.Giveaway;
import ghostygiveaway.GhostyGiveaway;
import ghostygiveaway.GiveawayManager;
import ghostygiveaway.utils.TimeUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class GiveawayCommand implements CommandExecutor, TabCompleter {

    private final GhostyGiveaway plugin;

    public GiveawayCommand(GhostyGiveaway plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "start"  -> handleStart(sender);
            case "join"   -> handleJoin(sender, args);
            case "end"    -> handleEnd(sender, args);
            case "list"   -> handleList(sender);
            case "reload" -> handleReload(sender);
            default       -> sendHelp(sender);
        }
        return true;
    }

    // ── Subcommands ───────────────────────────────────────────────────────────

    private void handleStart(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLangManager().get("player-only")); return;
        }
        if (!hasPerm(player, "ghostygiveaway.start")) {
            player.sendMessage(plugin.getLangManager().get("no-permission")); return;
        }
        plugin.getGuiListener().openSetupGUI(player);
    }

    private void handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLangManager().get("player-only")); return;
        }
        if (!hasPerm(player, "ghostygiveaway.join")) {
            player.sendMessage(plugin.getLangManager().get("no-permission")); return;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.getLangManager().get("help-join")); return;
        }

        String id = args[1].toUpperCase();
        switch (plugin.getGiveawayManager().joinGiveaway(player, id)) {
            case SUCCESS       -> player.sendMessage(plugin.getLangManager().get("join-success",   "id", id));
            case ALREADY_JOINED-> player.sendMessage(plugin.getLangManager().get("already-joined", "id", id));
            case NOT_FOUND     -> player.sendMessage(plugin.getLangManager().get("giveaway-not-found","id", id));
            case ENDED         -> player.sendMessage(plugin.getLangManager().get("giveaway-ended-msg"));
        }
    }

    private void handleEnd(CommandSender sender, String[] args) {
        if (!hasPerm(sender, "ghostygiveaway.end")) {
            sender.sendMessage(plugin.getLangManager().get("no-permission")); return;
        }
        if (args.length < 2) { sender.sendMessage(plugin.getLangManager().get("help-end")); return; }

        String id = args[1].toUpperCase();
        Giveaway g = plugin.getGiveawayManager().getGiveaway(id);
        if (g == null || g.isEnded()) {
            sender.sendMessage(plugin.getLangManager().get("giveaway-not-found", "id", id)); return;
        }
        plugin.getGiveawayManager().endGiveaway(id);
        sender.sendMessage(plugin.getLangManager().get("giveaway-ended-admin", "id", id));
    }

    private void handleList(CommandSender sender) {
        if (!hasPerm(sender, "ghostygiveaway.list")) {
            sender.sendMessage(plugin.getLangManager().get("no-permission")); return;
        }
        var active = plugin.getGiveawayManager().getActiveGiveaways();
        if (active.isEmpty()) {
            sender.sendMessage(plugin.getLangManager().get("no-active-giveaways")); return;
        }
        sender.sendMessage(plugin.getLangManager().get("active-giveaways-header"));
        active.values().forEach(g -> sender.sendMessage(plugin.getLangManager().get(
                "active-giveaway-entry",
                "id", g.getId(),
                "prize", GiveawayManager.getPrizeName(g.getPrize()),
                "time", TimeUtils.format(g.getRemainingSeconds()),
                "count", String.valueOf(g.getParticipants().size())
        )));
    }

    private void handleReload(CommandSender sender) {
        if (!hasPerm(sender, "ghostygiveaway.reload")) {
            sender.sendMessage(plugin.getLangManager().get("no-permission")); return;
        }
        plugin.reloadConfig();
        plugin.getLangManager().reload();
        sender.sendMessage(plugin.getLangManager().get("reload-success"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getLangManager().get("help-header"));
        sender.sendMessage(plugin.getLangManager().get("help-join"));
        if (hasPerm(sender, "ghostygiveaway.start"))  sender.sendMessage(plugin.getLangManager().get("help-start"));
        if (hasPerm(sender, "ghostygiveaway.end"))    sender.sendMessage(plugin.getLangManager().get("help-end"));
        if (hasPerm(sender, "ghostygiveaway.list"))   sender.sendMessage(plugin.getLangManager().get("help-list"));
        if (hasPerm(sender, "ghostygiveaway.reload")) sender.sendMessage(plugin.getLangManager().get("help-reload"));
        sender.sendMessage(plugin.getLangManager().get("help-footer"));
    }

    // ── Tab completion ────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (String sub : List.of("join", "start", "end", "list", "reload")) {
                if (sub.startsWith(args[0].toLowerCase())) completions.add(sub);
            }
        } else if (args.length == 2 &&
                (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("end"))) {
            String input = args[1].toUpperCase();
            plugin.getGiveawayManager().getActiveGiveaways().keySet().stream()
                    .filter(id -> id.startsWith(input))
                    .forEach(completions::add);
        }
        return completions;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private boolean hasPerm(CommandSender sender, String perm) {
        return sender.hasPermission(perm) || sender.hasPermission("ghostygiveaway.admin");
    }
}
