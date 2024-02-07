package top.shadniw.fixiedoubledrop;

import lombok.Getter;
import lombok.SneakyThrows;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class Main extends JavaPlugin implements Listener {

    private boolean hasFixed = false;

    private final String NMS_PACKAGE = "net.minecraft.server.";
    private String SERVER_VERSION;

    private File messagesFile;
    @Getter
    private Properties messages;

    private volatile Method getHandle = null;
    private Class BlockPosition;
    private volatile Method getTileEntity = null;
    private volatile Method isEmpty = null;

    private Class TileEntityMultiblockPart;
    private Class IIEInventory;
    private Method master;
    private Method getDroppedItems;
    private Field formed;

    @SneakyThrows
    @Override
    public void onLoad() {
        messagesFile = new File(getDataFolder(), "/messages.properties");
        if (!messagesFile.exists()) saveResource("messages.properties", true);
        messages = new Properties();
        messages.load(new InputStreamReader(Files.newInputStream(messagesFile.toPath()), StandardCharsets.UTF_8));
        saveMessages();
    }

    @SneakyThrows
    @Override
    public void onEnable() {
        getServerVersion();

        BlockPosition = Class.forName(NMS_PACKAGE + SERVER_VERSION + ".BlockPosition");

        TileEntityMultiblockPart = Class.forName("blusunrize.immersiveengineering.common.blocks.TileEntityMultiblockPart");
        for (Field f : TileEntityMultiblockPart.getDeclaredFields()) {
            if (f.getName().equals("formed")) formed = f;
        }
        for (Method m : TileEntityMultiblockPart.getDeclaredMethods()) {
            if (m.getName().equals("master")) master = m;
        }
        IIEInventory = Class.forName("blusunrize.immersiveengineering.common.util.inventory.IIEInventory");
        for (Method m : IIEInventory.getDeclaredMethods()) {
            if (m.getName().equals("getDroppedItems")) getDroppedItems = m;
        }

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @SneakyThrows
    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Object pos = BlockPosition.getDeclaredConstructor(int.class, int.class, int.class).newInstance(block.getX(), block.getY(), block.getZ());
        Object world = getHandle(event.getBlock().getWorld());
        Object tileEntity = getTileEntity(world, pos);
        if (!TileEntityMultiblockPart.isInstance(tileEntity)) return;
        if (!formed.getBoolean(tileEntity)) return;
        if (!IIEInventory.isInstance(tileEntity)) return;
        Object tileEntityMaster = master.invoke(tileEntity);
        if (tileEntityMaster == null) return;
        List<Object> items = (List<Object>) getDroppedItems.invoke(tileEntityMaster);
        items = items.stream()
                .filter(i -> i != null && !isEmpty(i))
                .collect(Collectors.toList());
        if (items.isEmpty()) return;
        event.setCancelled(true);
        if (event.getPlayer() == null) return;
        event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes(
                '&',
                getMessages().getProperty("message.prefix") + " " + getMessages().getProperty("message.prevent")
        ));
    }

    @EventHandler(ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        if (!hasFixed && event.getPlayer().isOp()) {
            TextComponent open = new TextComponent(ChatColor.translateAlternateColorCodes('&', getMessages().getProperty("message.prefix")));
            TextComponent web = new TextComponent("§6https://github.com/a08381/FixIEDoubleDrop");
            web.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/a08381/FixIEDoubleDrop"));
            open.addExtra(" ");
            open.addExtra("§b本插件开源地址：");
            open.addExtra(" ");
            open.addExtra(web);
            TextComponent dz = new TextComponent("§b如需定制请联系 QQ:1435292151");
            dz.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://wpa.qq.com/msgrd?v=3&uin=1435292151&site=qq&menu=yes"));
            TextComponent copy = new TextComponent("§6[点我复制]");
            copy.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "1435292151"));
            TextComponent tc = new TextComponent(ChatColor.translateAlternateColorCodes('&', getMessages().getProperty("message.prefix")));
            tc.addExtra(" ");
            tc.addExtra(dz);
            tc.addExtra(" ");
            tc.addExtra(copy);
            event.getPlayer().spigot().sendMessage(open);
            event.getPlayer().spigot().sendMessage(tc);
        }
    }

    @SneakyThrows
    public void reloadMessages() {
        if (!messagesFile.exists()) saveResource("messages.properties", true);
        messages.load(new InputStreamReader(Files.newInputStream(messagesFile.toPath()), StandardCharsets.UTF_8));
    }

    @SneakyThrows
    public void saveMessages() {
        messages.store(new OutputStreamWriter(Files.newOutputStream(messagesFile.toPath()), StandardCharsets.UTF_8), "Plugin Message Save");
    }

    public void getServerVersion() {
        String pac = getServer().getClass().getPackage().getName();
        SERVER_VERSION = pac.substring(pac.lastIndexOf('.') + 1);
    }

    @SneakyThrows
    public Object getHandle(World world) {
        if (getHandle == null) {
            for (Method m : world.getClass().getDeclaredMethods()) {
                if (m.getName().equals("getHandle")) {
                    getHandle = m;
                    break;
                }
            }
        }
        assert getHandle != null;
        return getHandle.invoke(world);
    }

    @SneakyThrows
    public Object getTileEntity(Object nmsWorld, Object pos) {
        if (getTileEntity == null) {
            for (Method m : nmsWorld.getClass().getDeclaredMethods()) {
                if (m.getName().equals("getTileEntity") || m.getName().equals("func_148853_f")) {
                    getTileEntity = m;
                    break;
                }
            }
        }
        assert getTileEntity != null;
        return getTileEntity.invoke(nmsWorld, pos);
    }

    @SneakyThrows
    public boolean isEmpty(Object item) {
        if (isEmpty == null) {
            for (Method m : item.getClass().getDeclaredMethods()) {
                if (m.getName().equals("isEmpty") || m.getName().equals("func_190926_b")) {
                    isEmpty = m;
                    break;
                }
            }
        }
        assert isEmpty != null;
        return (boolean) isEmpty.invoke(item);
    }

}
