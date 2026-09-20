package me.reil.skybound.core.command.island;

import me.reil.skybound.api.trade.TradeOffer;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.menu.TradeMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class IslandTradeCommandHandler implements IslandSubCommandHandler {

    private final SkyBoundPlugin plugin;
    private final IslandCommandContext context;

    public IslandTradeCommandHandler(IslandCommandContext context) {
        this.context = context;
        this.plugin = context.plugin();
    }

    @Override
    public boolean handle(Player player, String[] args) {
        if (!"trade".equalsIgnoreCase(args[0])) {
            return false;
        }

        if (args.length >= 3 && "sell".equalsIgnoreCase(args[1])) {
            sell(player, args);
            return true;
        }

        new TradeMenu(player, plugin).open();
        return true;
    }

    private void sell(Player player, String[] args) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            context.lang().send(player, "trade.empty-hand");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            context.lang().send(player, "number.price-required");
            return;
        }

        if (!isValidPrice(price)) {
            context.lang().send(player, "number.price-positive");
            return;
        }

        TradeOffer offer = plugin.getTradeManager().createOffer(player, hand, price);
        if (offer != null) {
            context.lang().send(player, "trade.offer-created", "{price}", String.format("%.0f", price));
        } else {
            context.lang().send(player, "trade.offer-failed");
        }
    }

    private boolean isValidPrice(double price) {
        return price > 0.0 && !Double.isNaN(price) && !Double.isInfinite(price);
    }
}
