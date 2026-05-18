package me.reil.skybound.core.menu;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.lang.LangManager;
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

    @Override public String getTitle() { return lang().get("menu.bank.title"); }
    @Override public int getSize() { return 27; }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());
        LangManager l = lang();

        // Balance display
        ItemStack info = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(l.get("menu.bank.info"));
            List<String> lore = new ArrayList<String>();
            lore.add(l.get("menu.bank.balance", "{balance}", String.format("%.0f", island.getBankBalance())));
            lore.add(l.get("menu.bank.limit", "{limit}", String.format("%.0f", plugin.getBankManager().getBankLimit(island))));
            infoMeta.setLore(lore);
            info.setItemMeta(infoMeta);
        }
        inventory.setItem(4, info);

        // Deposit buttons
        setBtn(11, Material.LIME_WOOL, l.get("menu.bank.deposit", "{amount}", "100"), l.get("menu.bank.deposit-lore"));
        setBtn(12, Material.LIME_WOOL, l.get("menu.bank.deposit", "{amount}", "1,000"), l.get("menu.bank.deposit-lore"));
        setBtn(13, Material.LIME_WOOL, l.get("menu.bank.deposit", "{amount}", "10,000"), l.get("menu.bank.deposit-lore"));

        // Withdraw buttons
        setBtn(15, Material.RED_WOOL, l.get("menu.bank.withdraw", "{amount}", "100"), l.get("menu.bank.withdraw-lore"));
        setBtn(16, Material.RED_WOOL, l.get("menu.bank.withdraw", "{amount}", "1,000"), l.get("menu.bank.withdraw-lore"));
        setBtn(17, Material.RED_WOOL, l.get("menu.bank.withdraw", "{amount}", "10,000"), l.get("menu.bank.withdraw-lore"));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        double amount = 0;
        boolean deposit = false;

        switch (slot) {
            case 11: amount = 100; deposit = true; break;
            case 12: amount = 1000; deposit = true; break;
            case 13: amount = 10000; deposit = true; break;
            case 15: amount = 100; break;
            case 16: amount = 1000; break;
            case 17: amount = 10000; break;
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
        new BankMenu(player, plugin, island).open();
    }

    private void setBtn(int slot, Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> l = new ArrayList<String>();
            l.add(lore);
            meta.setLore(l);
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }
}
