package net.fliuxx.aFKGuard.commands;

import net.fliuxx.aFKGuard.AFKGuard;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AFKCommand implements CommandExecutor, TabCompleter {

    private final AFKGuard plugin;
    private final List<String> subCommands = Arrays.asList("check", "reload", "history");

    public AFKCommand(AFKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.formatMessage("&cQuesto comando può essere utilizzato solo da un giocatore."));
                return true;
            }

            Player player = (Player) sender;
            boolean currentStatus = plugin.getAfkManager().isPlayerAFK(player.getUniqueId());
            plugin.getAfkManager().setPlayerAFK(player, !currentStatus);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "check":
                handleCheckCommand(sender, args);
                break;
            case "reload":
                handleReloadCommand(sender);
                break;
            case "history":
                handleHistoryCommand(sender, args);
                break;
            default:
                sender.sendMessage(plugin.formatMessage("&cComando non riconosciuto. Usa /afk, /afk check, /afk history o /afk reload"));
                break;
        }

        return true;
    }

    private void handleCheckCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("afkguard.admin")) {
            sender.sendMessage(plugin.formatMessage(plugin.getConfigManager().getNoPermissionMessage()));
            return;
        }

        if (args.length > 1) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(plugin.formatMessage("&cGiocatore non trovato o non online."));
                return;
            }

            boolean isAfk = plugin.getAfkManager().isPlayerAFK(target.getUniqueId());

            if (isAfk) {
                int inactiveTime = plugin.getAfkManager().getInactiveTime(target.getUniqueId());
                sender.sendMessage(plugin.formatMessage("&6Stato di " + target.getName() + ": &cAFK &7(Inattivo da " + inactiveTime + " secondi)"));
            } else {
                sender.sendMessage(plugin.formatMessage("&6Stato di " + target.getName() + ": &aNon AFK"));
            }
            return;
        }

        sender.sendMessage(plugin.formatMessage("&6Stato AFK di tutti i giocatori:"));
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean isAfk = plugin.getAfkManager().isPlayerAFK(player.getUniqueId());

            if (isAfk) {
                int inactiveTime = plugin.getAfkManager().getInactiveTime(player.getUniqueId());
                sender.sendMessage(plugin.formatMessage("&7- " + player.getName() + ": &cAFK &7(Inattivo da " + inactiveTime + " secondi)"));
            } else {
                sender.sendMessage(plugin.formatMessage("&7- " + player.getName() + ": &aNon AFK"));
            }
        }
    }

    private void handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("afkguard.admin")) {
            sender.sendMessage(plugin.formatMessage(plugin.getConfigManager().getNoPermissionMessage()));
            return;
        }

        plugin.getConfigManager().reloadConfig();
        sender.sendMessage(plugin.formatMessage(plugin.getConfigManager().getConfigReloadedMessage()));
    }

    private void handleHistoryCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("afkguard.admin")) {
            sender.sendMessage(plugin.formatMessage(plugin.getConfigManager().getNoPermissionMessage()));
            return;
        }

        Player target;
        int limit = 10;

        if (args.length > 1) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.formatMessage("&cGiocatore non trovato o non online."));
                return;
            }

            if (args.length > 2) {
                try {
                    limit = Integer.parseInt(args[2]);
                    if (limit <= 0) limit = 10;
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.formatMessage("&cIl limite deve essere un numero. Uso il valore predefinito di 10."));
                }
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(plugin.formatMessage("&cSpecifica un giocatore: /afk history <player> [limit]"));
            return;
        }

        UUID playerUUID = target.getUniqueId();
        List<String> history = plugin.getDatabaseManager().getPlayerAFKHistory(playerUUID, limit);

        sender.sendMessage(plugin.formatMessage("&6Storia AFK di &e" + target.getName() + "&6:"));

        if (history.isEmpty()) {
            sender.sendMessage(plugin.formatMessage("&7Nessun evento AFK registrato per questo giocatore."));
            return;
        }

        for (String entry : history) {
            sender.sendMessage(plugin.formatMessage("&7" + entry));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return subCommands.stream()
                    .filter(subCmd -> subCmd.startsWith(args[0].toLowerCase()))
                    .filter(subCmd -> {
                        if ("reload".equals(subCmd) || "check".equals(subCmd) || "history".equals(subCmd)) {
                            return sender.hasPermission("afkguard.admin");
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        } else if (args.length == 2 && ("check".equals(args[0].toLowerCase()) || "history".equals(args[0].toLowerCase()))
                && sender.hasPermission("afkguard.admin")) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerNames.add(player.getName());
            }
            return playerNames.stream()
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}