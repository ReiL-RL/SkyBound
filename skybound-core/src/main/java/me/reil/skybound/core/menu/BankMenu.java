package me.reil.skybound.core.menu;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.lang.LangManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class BankMenu extends Menu {

    private final SkyBoundPlugin plugin;
    private final Island island;

    public BankMenu(Player player, SkyBoundPlugin plugin, Island island) {
        super(player);
        this.plugin = plugin;
        this.island = island;
    }

    @Override public String getTitle() { return msg("menu.bank.title"); }
    @Override public int getSize() { return 45; }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());

        // Decorative borders
        ItemStack topGlass = decoration(Material.YELLOW_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) if (i != 4) inventory.setItem(i, topGlass);
        ItemStack sideGlass = decoration(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        for (int row = 1; row <= 3; row++) {
            inventory.setItem(row * 9, sideGlass);
            inventory.setItem(row * 9 + 8, sideGlass);
        }
        ItemStack bottomGlass = decoration(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 36; i < 45; i++) inventory.setItem(i, bottomGlass);

        // Header (slot 4) — bank info
        double balance = island.getBankBalance();
        double limit = plugin.getBankManager().getBankLimit(island);
        double pct = limit <= 0 ? 0 : (balance / limit) * 100;

        ItemStack info = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta iMeta = info.getItemMeta();
        if (iMeta != null) {
            iMeta.setDisplayName(msg("menu.bank.info"));
            List<String> lore = new ArrayList<String>();
            lore.add("");
            lore.add(msg("menu.bank.balance", "{balance}", String.format("%,.0f", balance)));
            lore.add(msg("menu.bank.limit", "{limit}", String.format("%,.0f", limit)));
            lore.add(msg("menu.bank.filled", "{percent}", String.format("%.1f", pct)));
            lore.add("");
            lore.add(progressBar(balance, limit));
            iMeta.setLore(lore);
            info.setItemMeta(iMeta);
        }
        inventory.setItem(4, info);

        // Deposit row (slots 19-21) — green
        depositBtn(19, 100);
        depositBtn(20, 1000);
        depositBtn(21, 10000);

        // Withdraw row (slots 23-25) — red
        withdrawBtn(23, 100);
        withdrawBtn(24, 1000);
        withdrawBtn(25, 10000);

        // Section labels
        ItemStack depositLabel = new ItemStack(Material.LIME_DYE);
        ItemMeta dMeta = depositLabel.getItemMeta();
        if (dMeta != null) {
            dMeta.setDisplayName(msg("menu.bank.deposit-section"));
            depositLabel.setItemMeta(dMeta);
        }
        inventory.setItem(11, depositLabel);

        ItemStack withdrawLabel = new ItemStack(Material.RED_DYE);
        ItemMeta wMeta = withdrawLabel.getItemMeta();
        if (wMeta != null) {
            wMeta.setDisplayName(msg("menu.bank.withdraw-section"));
            withdrawLabel.setItemMeta(wMeta);
        }
        inventory.setItem(15, withdrawLabel);

        addBackButton();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (handleBack(slot)) return;
        double amount = 0;
        boolean deposit = false;

        switch (slot) {
            case 19: amount = 100; deposit = true; break;
            case 20: amount = 1000; deposit = true; break;
            case 21: amount = 10000; deposit = true; break;
            case 23: amount = 100; break;
            case 24: amount = 1000; break;
            case 25: amount = 10000; break;
            default: return;
        }

        LangManager l = lang();
        if (deposit) {
            boolean ok = plugin.getBankManager().deposit(player, island, amount);
            l.send(player, ok ? "bank.deposited" : "bank.cannot-deposit", "{amount}", String.valueOf((int) amount));
        } else {
            boolean ok = plugin.getBankManager().withdraw(player, island, amount);
            l.send(player, ok ? "bank.withdrawn" : "bank.cannot-withdraw", "{amount}", String.valueOf((int) amount));
        }
        new BankMenu(player, plugin, island).withParent(parentMenu).open();
    }

    private void depositBtn(int slot, double amount) {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(msg("menu.bank.deposit", "{amount}", String.format("%,d", (int) amount)));
            List<String> lore = new ArrayList<String>();
            lore.add(msg("menu.bank.deposit-description", "{amount}", String.valueOf((int) amount)));
            lore.add("");
            lore.add(msg("menu.bank.deposit-lore"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    private void withdrawBtn(int slot, double amount) {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(msg("menu.bank.withdraw", "{amount}", String.format("%,d", (int) amount)));
            List<String> lore = new ArrayList<String>();
            lore.add(msg("menu.bank.withdraw-description", "{amount}", String.valueOf((int) amount)));
            lore.add("");
            lore.add(msg("menu.bank.withdraw-lore"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    private String progressBar(double current, double total) {
        int width = 20;
        double pct = total <= 0 ? 1.0 : Math.min(1.0, current / total);
        int filled = (int) Math.round(pct * width);
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.DARK_GRAY).append("[").append(ChatColor.GOLD);
        for (int i = 0; i < filled; i++) sb.append("▉");
        sb.append(ChatColor.DARK_GRAY);
        for (int i = filled; i < width; i++) sb.append("▉");
        sb.append(ChatColor.DARK_GRAY).append("]");
        return sb.toString();
    }

    private ItemStack decoration(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String msg(String key, String... replacements) {
        return lang().get(key, replacements);
    }
}
