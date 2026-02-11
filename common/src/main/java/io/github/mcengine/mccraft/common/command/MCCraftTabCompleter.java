package io.github.mcengine.mccraft.common.command;

import io.github.mcengine.mccraft.common.MCCraftProvider;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab completer for the /craft command.
 */
public class MCCraftTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("create", "get", "editor", "help"));
            return filter(subs, args[0]);
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create": {
                if (args.length == 2) {
                    return filterTypes(args[1]);
                }
                if (args.length == 3) {
                    return Collections.singletonList("<id_or_base64>");
                }
                break;
            }
            case "get": {
                if (args.length == 2) {
                    return filterTypes(args[1]);
                }
                break;
            }
            case "editor": {
                if (args.length == 2) {
                    return filterTypes(args[1]);
                }
                break;
            }
            default:
                break;
        }

        return Collections.emptyList();
    }

    private List<String> filterTypes(String input) {
        MCCraftProvider provider = MCCraftProvider.getInstance();
        if (provider == null) return Collections.emptyList();
        try {
            List<String> types = provider.getDb().getTypes();
            return filter(types, input);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}
