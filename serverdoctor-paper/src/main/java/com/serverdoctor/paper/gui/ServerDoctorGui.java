package com.serverdoctor.paper.gui;

import com.serverdoctor.api.ServerDoctorApi;
import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.common.model.ConflictReport;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.Recommendation;
import com.serverdoctor.common.model.SecurityRisk;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.storage.StorageProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Read-only inventory GUI for Paper/Folia. Opens via {@code /serverdoctor gui}.
 *
 * <p>Folia-safe: inventories are built and opened on the player's entity-scheduler thread,
 * and the "refresh" action runs the (heavy) analysis on the async scheduler before reopening.
 * Clicks are identified through {@link MenuHolder} (not titles) and always cancelled.
 */
public final class ServerDoctorGui implements Listener {

    public static final String PERMISSION = "serverdoctor.admin";

    private final JavaPlugin plugin;
    private final ServerDoctorApi api;
    private final StorageProvider storage;
    private final GuiSettings settings;

    public ServerDoctorGui(JavaPlugin plugin, ServerDoctorApi api, StorageProvider storage, GuiSettings settings) {
        this.plugin = plugin;
        this.api = api;
        this.storage = storage;
        this.settings = settings;
    }

    /** Register the click listener. Call once during onEnable. */
    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /** Open a screen for a player (hops to the player's region thread, Folia-safe). */
    public void open(Player player, MenuType type) {
        player.getScheduler().run(plugin, task -> {
            Inventory inv = build(type, player);
            player.openInventory(inv);
        }, null);
    }

    // ---- events -------------------------------------------------------------

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuHolder holder)) return;
        event.setCancelled(true); // read-only GUI
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        int size = top.getSize();
        if (slot < 0 || slot >= size) return; // click was in the player's own inventory

        handleClick(player, holder.type(), slot, size);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    private void handleClick(Player player, MenuType type, int slot, int size) {
        if (slot == size - 1) { player.closeInventory(); return; }        // close
        if (slot == size - 5) { refresh(player, type); return; }           // refresh
        if (type != MenuType.MAIN && slot == size - 9) {                   // back
            open(player, MenuType.MAIN);
            return;
        }
        if (type == MenuType.MAIN) {
            switch (slot) {
                case 11 -> open(player, MenuType.PERFORMANCE);
                case 12 -> open(player, MenuType.CONFLICTS);
                case 13 -> open(player, MenuType.SECURITY);
                case 14 -> open(player, MenuType.RECOMMENDATIONS);
                case 15 -> open(player, MenuType.HISTORY);
                default -> { }
            }
        }
    }

    private void refresh(Player player, MenuType type) {
        if (!player.hasPermission(PERMISSION)) return;
        player.getServer().getAsyncScheduler().runNow(plugin, task -> {
            try {
                api.runDiagnostics();
            } catch (Exception ignored) {
                // keep the GUI responsive even if a scan fails
            }
            open(player, type); // open() re-hops to the player's thread
        });
    }

    // ---- screen building ----------------------------------------------------

    private Inventory build(MenuType type, Player player) {
        return switch (type) {
            case MAIN -> buildMain();
            case PERFORMANCE -> buildPerformance();
            case CONFLICTS -> buildConflicts();
            case SECURITY -> buildSecurity();
            case RECOMMENDATIONS -> buildRecommendations();
            case HISTORY -> buildHistory();
        };
    }

    private Inventory inventory(MenuType type, int size, String subtitle) {
        MenuHolder holder = new MenuHolder(type);
        String title = color("&8" + settings.title() + (subtitle.isEmpty() ? "" : " &7— " + subtitle));
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);
        return inv;
    }

    private Inventory buildMain() {
        Inventory inv = inventory(MenuType.MAIN, 27, "");
        Optional<DiagnosticReport> latest = api.getLatestReport();

        int conflicts = api.getConflicts().size();
        int risks = api.getSecurityRisks().size();
        int recs = api.getRecommendations().size();
        PerformanceSnapshot p = api.getPerformanceSnapshot();

        if (latest.isPresent()) {
            Severity sev = latest.get().overallSeverity();
            inv.setItem(4, icon(sevMaterial(sev), sevColor(sev) + "Status: " + sev.name(), List.of(
                    "&7Conflicts: &f" + conflicts,
                    "&7Security risks: &f" + risks,
                    "&7Recommendations: &f" + recs,
                    "&7TPS: &f" + num(p.tps1m()) + "  &7MSPT: &f" + num(p.mspt()))));
        } else {
            inv.setItem(4, icon(Material.LIGHT_GRAY_CONCRETE, "&7Status: no analysis yet", List.of(
                    "&7Click &eRefresh &7to run the first scan.")));
        }

        inv.setItem(11, icon(Material.CLOCK, "&bPerformance", List.of(
                "&7TPS &f" + num(p.tps1m()) + " &8| &7MSPT &f" + num(p.mspt()),
                "&7RAM &f" + p.memory().usedMb() + "&7/&f" + p.memory().maxMb() + " MB",
                "", "&eClick to view")));
        inv.setItem(12, icon(Material.TNT, "&cConflicts", List.of(
                "&7Detected: &f" + conflicts, "", "&eClick to view")));
        inv.setItem(13, icon(Material.SHIELD, "&eSecurity", List.of(
                "&7Risks: &f" + risks, "", "&eClick to view")));
        inv.setItem(14, icon(Material.WRITABLE_BOOK, "&aRecommendations", List.of(
                "&7Count: &f" + recs, "", "&eClick to view")));
        inv.setItem(15, icon(Material.PAPER, "&fHistory", List.of(
                "&7Stored performance snapshots", "", "&eClick to view")));

        inv.setItem(22, navRefresh());
        inv.setItem(26, navClose());
        return inv;
    }

    private Inventory buildPerformance() {
        Inventory inv = inventory(MenuType.PERFORMANCE, 54, "Performance");
        PerformanceSnapshot p = api.getPerformanceSnapshot();

        inv.setItem(20, icon(Material.CLOCK, "&bTPS", List.of(
                "&71m: &f" + num(p.tps1m()),
                "&75m: &f" + num(tpsAt(p, 1)),
                "&715m: &f" + num(tpsAt(p, 2)))));
        inv.setItem(22, icon(Material.REPEATER, "&bMSPT", List.of(
                "&7Milliseconds per tick: &f" + num(p.mspt()))));
        inv.setItem(24, icon(Material.REDSTONE, "&bMemory", List.of(
                "&7Used: &f" + p.memory().usedMb() + " MB",
                "&7Max: &f" + p.memory().maxMb() + " MB",
                "&7Usage: &f" + num(p.memory().usedRatio() * 100.0) + "%")));
        inv.setItem(31, icon(Material.PLAYER_HEAD, "&bRuntime", List.of(
                "&7Players: &f" + p.onlinePlayers(),
                "&7Threads: &f" + p.threadCount(),
                "&7Captured: &f" + p.capturedAt())));

        navBar(inv);
        return inv;
    }

    private Inventory buildConflicts() {
        Inventory inv = inventory(MenuType.CONFLICTS, 54, "Conflicts");
        List<ConflictReport> list = api.getConflicts();
        if (list.isEmpty()) {
            inv.setItem(22, icon(Material.LIME_CONCRETE, "&aNo conflicts detected", List.of("&7Nothing to report.")));
        } else {
            int slot = 0;
            for (ConflictReport c : list) {
                if (slot >= 45) break;
                List<String> lore = new ArrayList<>();
                lore.add(sevColor(c.severity()) + c.severity().name());
                lore.addAll(wrap("&7", c.description(), 40));
                inv.setItem(slot++, icon(Material.TNT,
                        sevColor(c.severity()) + c.pluginA() + " &8× " + sevColor(c.severity()) + c.pluginB(), lore));
            }
        }
        navBar(inv);
        return inv;
    }

    private Inventory buildSecurity() {
        Inventory inv = inventory(MenuType.SECURITY, 54, "Security");
        List<SecurityRisk> list = api.getSecurityRisks();
        if (list.isEmpty()) {
            inv.setItem(22, icon(Material.LIME_CONCRETE, "&aNo security risks", List.of("&7Nothing to report.")));
        } else {
            int slot = 0;
            for (SecurityRisk r : list) {
                if (slot >= 45) break;
                List<String> lore = new ArrayList<>();
                lore.add(sevColor(r.severity()) + r.severity().name() + " &8· &7" + r.type().name());
                lore.addAll(wrap("&7", r.description(), 40));
                inv.setItem(slot++, icon(Material.SHIELD, "&e" + r.pluginName(), lore));
            }
        }
        navBar(inv);
        return inv;
    }

    private Inventory buildRecommendations() {
        Inventory inv = inventory(MenuType.RECOMMENDATIONS, 54, "Recommendations");
        List<Recommendation> list = api.getRecommendations();
        if (list.isEmpty()) {
            inv.setItem(22, icon(Material.LIME_CONCRETE, "&aNo recommendations", List.of("&7Nothing to suggest.")));
        } else {
            int slot = 0;
            for (Recommendation r : list) {
                if (slot >= 45) break;
                List<String> lore = new ArrayList<>();
                lore.add(sevColor(r.severity()) + r.severity().name() + " &8· &7" + r.category().name());
                lore.addAll(wrap("&7", r.description(), 40));
                inv.setItem(slot++, icon(Material.WRITABLE_BOOK, "&a" + r.title(), lore));
            }
        }
        navBar(inv);
        return inv;
    }

    private Inventory buildHistory() {
        Inventory inv = inventory(MenuType.HISTORY, 54, "History");
        List<PerformanceSnapshot> list;
        try {
            list = storage.performance().recent(45);
        } catch (Exception ex) {
            list = List.of();
        }
        if (list.isEmpty()) {
            inv.setItem(22, icon(Material.PAPER, "&7No history yet", List.of("&7Snapshots appear after scans.")));
        } else {
            int slot = 0;
            for (PerformanceSnapshot p : list) {
                if (slot >= 45) break;
                inv.setItem(slot++, icon(Material.PAPER, "&f" + p.capturedAt(), List.of(
                        "&7TPS: &f" + num(p.tps1m()) + " &8| &7MSPT: &f" + num(p.mspt()),
                        "&7RAM: &f" + p.memory().usedMb() + " MB &8| &7Players: &f" + p.onlinePlayers())));
            }
        }
        navBar(inv);
        return inv;
    }

    // ---- item helpers -------------------------------------------------------

    private void navBar(Inventory inv) {
        int size = inv.getSize();
        inv.setItem(size - 9, icon(Material.ARROW, "&7Back", List.of("&8Return to the main menu")));
        inv.setItem(size - 5, navRefresh());
        inv.setItem(size - 1, navClose());
    }

    private ItemStack navRefresh() {
        return icon(Material.NETHER_STAR, "&eRefresh", List.of("&8Run a new analysis now"));
    }

    private ItemStack navClose() {
        return icon(Material.BARRIER, "&cClose", List.of());
    }

    private ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            if (lore != null && !lore.isEmpty()) {
                List<String> colored = new ArrayList<>(lore.size());
                for (String l : lore) colored.add(color(l));
                meta.setLore(colored);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private static String num(double v) {
        return Double.isNaN(v) ? "n/a" : String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    private static double tpsAt(PerformanceSnapshot p, int i) {
        double[] t = p.tps();
        return t != null && t.length > i ? t[i] : Double.NaN;
    }

    private static Material sevMaterial(Severity s) {
        return switch (s) {
            case CRITICAL -> Material.RED_CONCRETE;
            case HIGH     -> Material.ORANGE_CONCRETE;
            case MEDIUM   -> Material.YELLOW_CONCRETE;
            case LOW      -> Material.LIGHT_BLUE_CONCRETE;
            case INFO     -> Material.CYAN_CONCRETE;
            case OK       -> Material.LIME_CONCRETE;
        };
    }

    private static String sevColor(Severity s) {
        return switch (s) {
            case CRITICAL -> "&4";
            case HIGH     -> "&c";
            case MEDIUM   -> "&6";
            case LOW      -> "&b";
            case INFO     -> "&3";
            case OK       -> "&a";
        };
    }

    private static List<String> wrap(String prefix, String text, int width) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (line.length() + word.length() + 1 > width && line.length() > 0) {
                out.add(prefix + line);
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) out.add(prefix + line);
        return out;
    }
}
