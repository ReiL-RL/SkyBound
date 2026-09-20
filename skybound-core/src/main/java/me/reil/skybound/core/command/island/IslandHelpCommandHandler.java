package me.reil.skybound.core.command.island;

import org.bukkit.entity.Player;

public final class IslandHelpCommandHandler implements IslandSubCommandHandler {

    private final IslandCommandContext context;

    public IslandHelpCommandHandler(IslandCommandContext context) {
        this.context = context;
    }

    @Override
    public boolean handle(Player player, String[] args) {
        if (!"help".equalsIgnoreCase(args[0])) {
            return false;
        }

        String page = args.length > 1 ? args[1].toLowerCase() : "1";
        if ("admin".equals(page) && player.hasPermission("skybound.admin")) {
            sendLangLines(player, "help.admin", 10);
            return true;
        }

        int pageNumber;
        try {
            pageNumber = Integer.parseInt(page);
        } catch (NumberFormatException e) {
            pageNumber = 1;
        }

        switch (pageNumber) {
            case 1:
                sendLangLines(player, "help.page1", 10);
                break;
            case 2:
                sendLangLines(player, "help.page2", 13);
                break;
            case 3:
                sendLangLines(player, "help.page3", 14);
                break;
            case 4:
                sendLangLines(player, "help.page4", 18);
                break;
            default:
                sendLangLines(player, "help.page1", 10);
                break;
        }
        context.lang().send(player, "help.page-footer",
                "{page}", String.valueOf(pageNumber),
                "{pages}", "4");
        return true;
    }

    private void sendLangLines(Player player, String prefix, int count) {
        for (int i = 1; i <= count; i++) {
            context.lang().send(player, prefix + "." + i);
        }
    }
}
