package com.gtnewhorizon.structurelib;

import static com.gtnewhorizon.structurelib.StructureLib.RANDOM;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.IIcon;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.structurelib.entity.fx.WeightlessParticleFX;
import com.gtnewhorizon.structurelib.item.ItemConstructableTrigger;
import com.gtnewhorizon.structurelib.net.RegistryOrderSyncMessage;
import com.gtnewhorizon.structurelib.net.SetChannelDataMessage;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ClientProxy extends CommonProxy {

    private static final short[] RGBA_NO_TINT = { 255, 255, 255, 255 };
    private static final short[] RGBA_RED_TINT = { 255, 128, 128, 0 };
    /**
     * Vanilla {@link EffectRenderer} keeps at most 4000 particles per layer and evicts the surplus with an
     * {@code ArrayList.remove(0)}, so spawning more than that in one projection costs a lot of copying for particles
     * that are thrown away before they are ever drawn.
     */
    private static final int MAX_SPAWNED_PARTICLES_PER_GROUP = 4000;
    private static final Map<HintParticleInfo, HintGroup> allHints = new HashMap<>();
    /**
     * Total number of hints held across {@link #allGroups} and {@link #currentHints}, i.e. what
     * {@link ConfigurationHandler#getMaxRenderCount()} is compared against. Not the same as {@code allHints.size()},
     * which is keyed by position and therefore collapses hints that several holograms put at the same spot.
     */
    private static int storedHintCount;
    /**
     * All batches of hints.
     */
    private static final List<HintGroup> allGroups = new ArrayList<>();

    /**
     * Current batch of hints. Belongs to the same logical multiblock.
     */
    private static HintGroup currentHints;
    /**
     * Player position and squared radius captured when {@link #currentHints} was opened, so the per-hint radius test is
     * plain arithmetic instead of a player lookup and a config lookup.
     */
    private static double hintOriginX, hintOriginY, hintOriginZ;
    private static double hintRadiusSq;
    /**
     * Decorative particles spawned for {@link #currentHints} so far, capped at
     * {@link #MAX_SPAWNED_PARTICLES_PER_GROUP}.
     */
    private static int spawnedParticles;
    /**
     * Collection of all hints. Sorted from farthest (index 0) to nearest (index size - 1)
     * <p>
     * 10k should be more than enough, if not a bit wasteful
     */
    private static final List<HintParticleInfo> allHintsForRender = new ArrayList<>(10000);
    /**
     * Hints with {@link HintParticleInfo#renderThrough} set, collected during the main render pass and drawn afterwards
     * with the depth test off. Keeping them out of the main pass means the depth test is toggled once per frame rather
     * than once per renderThrough boundary in the distance ordering.
     */
    private static final List<HintParticleInfo> deferredRenderThrough = new ArrayList<>();
    /**
     * Sorts farthest first, so transparent hints blend back to front and the nearest
     * {@link ConfigurationHandler#getMaxRenderCount()} hints are the tail of the list. Reads the distance cached by
     * {@link HintParticleInfo#cacheDistance} rather than recomputing it for every comparison.
     */
    private static final Comparator<HintParticleInfo> FARTHEST_FIRST = (a, b) -> Double
            .compare(b.distSqToPlayer, a.distSqToPlayer);
    /**
     * If the diff to current player position is too great sort the allHints. Initial value is very far off below y=0
     * but not huge enough to make a NaN.
     * <p>
     * We are using this supposedly immutable object as a mutable object here. So be aware.
     */
    private static final Vec3 lastPlayerPos = Vec3.createVectorHelper(0, -1e30, 0);
    /**
     * if true rebuild the allHints list from all hintOwners (and sort it)
     */
    private static boolean allHintsDirty = false;
    /**
     * A throttle map for local player. Integrated server will use the throttle map in super class instead.
     */
    private static final Map<Object, Long> localThrottleMap = new HashMap<>();

    @Override
    public void hintParticleTinted(World w, int x, int y, int z, IIcon[] icons, short[] RGBa) {
        ensureHinting();
        if (!isWithinHintRadius(x, y, z)) return;
        if (storedHintCount >= ConfigurationHandler.INSTANCE.getMaxRenderCount()) return;
        HintParticleInfo info = new HintParticleInfo(w, x, y, z, icons, RGBa);
        HintGroup existing = allHints.get(info);
        // this very projection already covered this position
        if (existing == currentHints) return;

        // check and remove colliding holograms
        if (existing != null && ConfigurationHandler.INSTANCE.isRemoveCollidingHologram()) {
            allGroups.remove(existing);
            removeGroup(existing);
        }
        allHints.put(info, currentHints);
        currentHints.getHints().add(info);
        storedHintCount++;
        allHintsDirty = true;

        if (spawnedParticles >= MAX_SPAWNED_PARTICLES_PER_GROUP) return;
        spawnedParticles++;
        EntityFX particle = new WeightlessParticleFX(
                w,
                x + RANDOM.nextFloat() * 0.5F,
                y + RANDOM.nextFloat() * 0.5F,
                z + RANDOM.nextFloat() * 0.5F,
                0,
                0,
                0);
        particle.setRBGColorF(0, 0.6F * RANDOM.nextFloat(), 0.8f);
        Minecraft.getMinecraft().effectRenderer.addEffect(particle);
    }

    @Override
    public void hintParticleTinted(World w, int x, int y, int z, Block block, int meta, short[] RGBa) {
        hintParticleTinted(w, x, y, z, createIIconFromBlock(block, meta), RGBa);
    }

    @Override
    public void hintParticle(World w, int x, int y, int z, IIcon[] icons) {
        hintParticleTinted(w, x, y, z, icons, RGBA_NO_TINT);
    }

    @Override
    public void hintParticle(World w, int x, int y, int z, Block block, int meta) {
        hintParticleTinted(w, x, y, z, createIIconFromBlock(block, meta), RGBA_NO_TINT);
    }

    @Override
    public boolean updateHintParticleTint(EntityPlayer player, World w, int x, int y, int z, short[] rgBa) {
        if (player instanceof EntityPlayerMP) return super.updateHintParticleTint(player, w, x, y, z, rgBa);
        if (player != getCurrentPlayer()) return false; // how?
        HintParticleInfo hint = getHintParticleInfo(w, x, y, z);
        if (hint == null) {
            return false;
        }
        hint.setTint(rgBa);
        return true;
    }

    private static HintParticleInfo getHintParticleInfo(World w, int x, int y, int z) {
        HintParticleInfo info = new HintParticleInfo(w, x, y, z, null, null);
        HintGroup existing = allHints.get(info);
        if (existing != null) {
            for (HintParticleInfo hint : existing.getHints()) {
                if (hint.equals(info)) {
                    return hint;
                }
            }
        }
        return null;
    }

    @Override
    public boolean markHintParticleError(EntityPlayer player, World w, int x, int y, int z) {
        if (player instanceof EntityPlayerMP) return super.markHintParticleError(player, w, x, y, z);
        if (player != getCurrentPlayer()) return false; // how?
        HintParticleInfo hint = getHintParticleInfo(w, x, y, z);
        if (hint == null) {
            return false;
        }
        hint.setTint(RGBA_RED_TINT);
        hint.setRenderThrough();
        return true;
    }

    @Override
    public void addThrottledChat(Object throttleKey, EntityPlayer player, IChatComponent text, short intervalRequired,
            boolean forceUpdateLastSend) {
        if (player instanceof EntityPlayerMP)
            super.addThrottledChat(throttleKey, player, text, intervalRequired, forceUpdateLastSend);
        else if (player != null)
            addThrottledChat(throttleKey, player, text, intervalRequired, forceUpdateLastSend, localThrottleMap);
    }

    static void removeGroup(HintGroup group) {
        List<HintParticleInfo> hints = group.getHints();
        for (int i = 0, size = hints.size(); i < size; i++) {
            HintParticleInfo hint = hints.get(i);
            // a later hologram may have claimed this position, in which case the mapping is not ours to drop
            if (allHints.get(hint) == group) allHints.remove(hint);
        }
        storedHintCount -= hints.size();
        allHintsDirty = true;
    }

    private static IIcon[] createIIconFromBlock(Block block, int meta) {
        IIcon[] ret = new IIcon[6];
        for (int i = 0; i < 6; i++) {
            ret[i] = block.getIcon(i, meta);
        }
        return ret;
    }

    @Override
    public void addClientSideChatMessages(String... messages) {
        GuiNewChat chat = Minecraft.getMinecraft().ingameGUI.getChatGUI();
        for (String s : messages) {
            chat.printChatMessage(new ChatComponentText(s));
        }
    }

    @Override
    public EntityPlayer getCurrentPlayer() {
        return Minecraft.getMinecraft().thePlayer;
    }

    @Override
    public boolean isCurrentPlayer(EntityPlayer player) {
        return player == Minecraft.getMinecraft().thePlayer;
    }

    @Override
    public void startHinting(World w) {
        if (!w.isRemote) return;
        if (currentHints != null) endHinting(w);
        beginGroup();
    }

    private void ensureHinting() {
        if (currentHints == null) beginGroup();
    }

    private static void beginGroup() {
        int maxCoexisting = ConfigurationHandler.INSTANCE.getMaxCoexistingHologram();
        // >= because we are about to add one more
        while (!allGroups.isEmpty() && allGroups.size() >= maxCoexisting) {
            removeGroup(allGroups.remove(0));
        }
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) {
            hintRadiusSq = Double.POSITIVE_INFINITY;
        } else {
            hintOriginX = player.posX;
            hintOriginY = player.posY;
            hintOriginZ = player.posZ;
            double radius = ConfigurationHandler.INSTANCE.getMaxRenderRadius();
            hintRadiusSq = radius * radius;
        }
        spawnedParticles = 0;
        currentHints = new HintGroup();
    }

    @Override
    public void endHinting(World w) {
        if (!w.isRemote || currentHints == null) return;
        if (!currentHints.getHints().isEmpty()) allGroups.add(currentHints);
        // we use the player existence time here as some worlds don't really advance the time ticker
        currentHints.setCreationTime(getCurrentPlayer().ticksExisted);
        currentHints = null;
    }

    private static boolean isWithinHintRadius(int x, int y, int z) {
        double dx = x + 0.5 - hintOriginX;
        double dy = y + 0.5 - hintOriginY;
        double dz = z + 0.5 - hintOriginZ;
        return dx * dx + dy * dy + dz * dz <= hintRadiusSq;
    }

    @Override
    public long getOverworldTime() {
        // there is no overworld, better just hope current world time is ok...
        return Minecraft.getMinecraft().theWorld.getTotalWorldTime();
    }

    @Override
    public void uploadChannels(ItemStack trigger) {
        StructureLib.net.sendToServer(new SetChannelDataMessage(trigger));
    }

    @Override
    public void preInit(FMLPreInitializationEvent e) {
        FMLCommonHandler.instance().bus().register(new FMLEventHandler());
        MinecraftForge.EVENT_BUS.register(new ForgeEventHandler());
    }

    static void markTextureUsed(IIcon icon) {
        if (StructureLib.COMPAT instanceof IStructureCompat)
            ((IStructureCompat) StructureLib.COMPAT).markTextureUsed(icon);
    }

    static void sendRegistryOrderToServer() {
        if (FMLEventHandler.configSent) return;
        FMLEventHandler.configSent = true;
        for (String s : SortedRegistry.ALL_REGISTRIES.keySet()) {
            Pair<List<String>, List<String>> registryOrder = ConfigurationHandler.INSTANCE.getRegistryOrder(s);
            if (registryOrder == null || registryOrder.getKey().isEmpty() && registryOrder.getValue().isEmpty())
                continue;
            StructureLib.net
                    .sendToServer(new RegistryOrderSyncMessage(s, registryOrder.getKey(), registryOrder.getValue()));
        }
    }

    @Override
    public void loadComplete(FMLLoadCompleteEvent evt) {
        super.loadComplete(evt);
        for (Map.Entry<String, WeakReference<SortedRegistry<?>>> e : SortedRegistry.ALL_REGISTRIES.entrySet()) {
            SortedRegistry<?> r = e.getValue().get();
            if (r == null) continue;
            Pair<List<String>, List<String>> p = ConfigurationHandler.INSTANCE.getRegistryOrder(e.getKey());
            r.registerOrdering(getSPPlayerUUID(), p.getKey(), p.getValue());
        }
    }

    public UUID getSPPlayerUUID() {
        return EntityPlayer.func_146094_a(Minecraft.getMinecraft().getSession().func_148256_e());
    }

    public void displayConfigGUI(String category) {
        ConfigElement<Object> element = new ConfigElement<>(
                ConfigurationHandler.INSTANCE.getConfig().getCategory(category));
        GuiConfig guiConfig = new GuiConfig(
                null,
                element.getChildElements(),
                StructureLibAPI.MOD_ID,
                null,
                false,
                false,
                I18n.format(element.getLanguageKey()));
        Minecraft.getMinecraft().displayGuiScreen(guiConfig);
    }

    private static class HintGroup {

        private final List<HintParticleInfo> hints = new ArrayList<>();
        private int creationTime = -1;

        public List<HintParticleInfo> getHints() {
            return hints;
        }

        public void setCreationTime(int creationTime) {
            this.creationTime = creationTime;
        }

        public int getCreationTime() {
            return creationTime;
        }
    }

    private static class HintParticleInfo {

        private final World w;
        // these are the block coordinate for e.g. w.getBlock()
        private final int x, y, z;
        private final IIcon[] icons;
        private short[] tint;
        private boolean renderThrough;
        private double distSqToPlayer;

        private final long creationTime = System.currentTimeMillis(); // use tick time instead maybe

        public HintParticleInfo(World w, int x, int y, int z, IIcon[] icons, short[] tint) {
            this.w = w;
            this.x = x;
            this.y = y;
            this.z = z;
            this.icons = icons;
            this.tint = tint;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof HintParticleInfo that)) return false;

            return x == that.x && y == that.y && z == that.z;
        }

        public void setTint(short[] tint) {
            this.tint = tint;
        }

        public void setRenderThrough() {
            // no need to invalidate allHintsForRender: the render pass partitions on this flag per frame
            this.renderThrough = true;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            result = 31 * result + z;
            return result;
        }

        public boolean isInFrustrum(Frustrum frustrum) {
            return frustrum.isBoxInFrustum(x + 0.25, y + 0.25, z + 0.25, x + 0.75, y + 0.75, z + 0.75);
        }

        public void draw(Tessellator tes, int transparency, double eyeX, double eyeY, double eyeZ, int eyeXint,
                int eyeYint, int eyeZint) {
            double size = 0.5;

            int brightness = w.blockExists(x, 0, z) ? w.getLightBrightnessForSkyBlocks(x, y, z, 0) : 0;
            tes.setBrightness(brightness);

            tes.setColorRGBA((int) (tint[0] * .9F), (int) (tint[1] * .95F), (int) (tint[2] * 1F), transparency);

            double X = (x - eyeXint) + 0.25;
            double Y = (y - eyeYint) + 0.25;
            double Z = (z - eyeZint) + 0.25;
            double worldX = x + 0.25;
            double worldY = y + 0.25;
            double worldZ = z + 0.25;

            // cube is a very special model. its facings can be rendered correctly by viewer distance without using
            // surface normals and view vector
            // here we do a 2 pass render.
            // first pass we draw obstructed faces (i.e. faces that are further away from player)
            // second pass we draw unobstructed faces
            for (int j = 0; j < 2; j++) {
                boolean unobstructedPass = j == 1;
                for (int i = 0; i < 6; i++) {
                    if (icons[i] == null) continue;

                    markTextureUsed(icons[i]);

                    double u = icons[i].getMinU();
                    double U = icons[i].getMaxU();
                    double v = icons[i].getMinV();
                    double V = icons[i].getMaxV();
                    switch (i) { // {DOWN, UP, NORTH, SOUTH, WEST, EAST}
                        case 0:
                            // all these ifs is in form if ((is face unobstructed) != (is in unobstructred pass))
                            if ((worldY >= eyeY) != unobstructedPass) continue;
                            tes.setNormal(0, -1, 0);
                            tes.addVertexWithUV(X, Y, Z + size, u, V);
                            tes.addVertexWithUV(X, Y, Z, u, v);
                            tes.addVertexWithUV(X + size, Y, Z, U, v);
                            tes.addVertexWithUV(X + size, Y, Z, U, v);
                            tes.addVertexWithUV(X + size, Y, Z + size, U, V);
                            tes.addVertexWithUV(X, Y, Z + size, u, V);
                            break;
                        case 1:
                            if ((worldY + size <= eyeY) != unobstructedPass) continue;
                            tes.setNormal(0, 1, 0);
                            tes.addVertexWithUV(X, Y + size, Z, u, v);
                            tes.addVertexWithUV(X, Y + size, Z + size, u, V);
                            tes.addVertexWithUV(X + size, Y + size, Z + size, U, V);
                            tes.addVertexWithUV(X + size, Y + size, Z + size, U, V);
                            tes.addVertexWithUV(X + size, Y + size, Z, U, v);
                            tes.addVertexWithUV(X, Y + size, Z, u, v);
                            break;
                        case 2:
                            if ((worldZ >= eyeZ) != unobstructedPass) continue;
                            tes.setNormal(0, 0, -1);
                            tes.addVertexWithUV(X, Y, Z, U, V);
                            tes.addVertexWithUV(X, Y + size, Z, U, v);
                            tes.addVertexWithUV(X + size, Y + size, Z, u, v);
                            tes.addVertexWithUV(X + size, Y + size, Z, u, v);
                            tes.addVertexWithUV(X + size, Y, Z, u, V);
                            tes.addVertexWithUV(X, Y, Z, U, V);
                            break;
                        case 3:
                            if ((worldZ + size <= eyeZ) != unobstructedPass) continue;
                            tes.setNormal(0, 0, 1);
                            tes.addVertexWithUV(X + size, Y, Z + size, U, V);
                            tes.addVertexWithUV(X + size, Y + size, Z + size, U, v);
                            tes.addVertexWithUV(X, Y + size, Z + size, u, v);
                            tes.addVertexWithUV(X, Y + size, Z + size, u, v);
                            tes.addVertexWithUV(X, Y, Z + size, u, V);
                            tes.addVertexWithUV(X + size, Y, Z + size, U, V);
                            break;
                        case 4:
                            if ((worldX >= eyeX) != unobstructedPass) continue;
                            tes.setNormal(-1, 0, 0);
                            tes.addVertexWithUV(X, Y, Z + size, U, V);
                            tes.addVertexWithUV(X, Y + size, Z + size, U, v);
                            tes.addVertexWithUV(X, Y + size, Z, u, v);
                            tes.addVertexWithUV(X, Y + size, Z, u, v);
                            tes.addVertexWithUV(X, Y, Z, u, V);
                            tes.addVertexWithUV(X, Y, Z + size, U, V);
                            break;
                        case 5:
                            if ((worldX + size <= eyeX) != unobstructedPass) continue;
                            tes.setNormal(1, 0, 0);
                            tes.addVertexWithUV(X + size, Y, Z, U, V);
                            tes.addVertexWithUV(X + size, Y + size, Z, U, v);
                            tes.addVertexWithUV(X + size, Y + size, Z + size, u, v);
                            tes.addVertexWithUV(X + size, Y + size, Z + size, u, v);
                            tes.addVertexWithUV(X + size, Y, Z + size, u, V);
                            tes.addVertexWithUV(X + size, Y, Z, U, V);
                            break;
                    }
                }
            }
        }

        public long getCreationTime() {
            return creationTime;
        }

        private void cacheDistance(double playerX, double playerY, double playerZ) {
            double dx = x + 0.5 - playerX;
            double dy = y + 0.5 - playerY;
            double dz = z + 0.5 - playerZ;
            distSqToPlayer = dx * dx + dy * dy + dz * dz;
        }

        private boolean isWithinRadius(double eyeX, double eyeY, double eyeZ, double radiusSq) {
            double dx = x + 0.5 - eyeX;
            double dy = y + 0.5 - eyeY;
            double dz = z + 0.5 - eyeZ;
            return dx * dx + dy * dy + dz * dz <= radiusSq;
        }
    }

    public static class FMLEventHandler {

        static boolean connected = false;
        static boolean configSent = false;

        @SubscribeEvent
        public void onPlayerLogIn(FMLNetworkEvent.ClientConnectedToServerEvent e) {
            connected = true;
            configSent = false;
        }

        @SubscribeEvent
        public void onPlayerLogOut(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
            connected = false;
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onConfigChange(ConfigChangedEvent.PostConfigChangedEvent e) {
            if (connected && e.modID.equals(StructureLibAPI.MOD_ID)) {
                configSent = false;
                sendRegistryOrderToServer();
            }
        }

        @SubscribeEvent
        public void onClientTick(ClientTickEvent e) {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            if (e.phase != Phase.END || player == null) return;
            boolean sortRequired = false;
            // The allGroups is implicitly sorted by creationTime
            // here we exploit this ordering to reduce iteration size
            int deadline = player.ticksExisted - ConfigurationHandler.INSTANCE.getHintLifespan();
            int i;
            for (i = 0; i < allGroups.size(); i++) {
                if (allGroups.get(i).getCreationTime() > deadline) {
                    break;
                }
            }
            if (i != 0) {
                sortRequired = true;
                // clear this block and go no further
                List<HintGroup> toRemove = allGroups.subList(0, i);
                toRemove.forEach(ClientProxy::removeGroup);
                toRemove.clear();
            }
            if (allHintsDirty) {
                allHintsForRender.clear();
                // we need greatly efficient sequential access and mildly efficient sorting
                // rebuild is sort of rare, and it's expected to lag anyway
                // so no min-heap here
                for (HintGroup c : allGroups) allHintsForRender.addAll(c.getHints());
                allHintsDirty = false;
                sortRequired = true;
            }
            final int size = allHintsForRender.size();
            if (size == 0) return;
            double px = player.posX, py = player.posY, pz = player.posZ;
            if (sortRequired || lastPlayerPos.squareDistanceTo(px, py, pz) > resortThreshold(size)) {
                // only redo sort if player moved some distance
                // if there was a full rebuild, go sort it as well
                for (int j = 0; j < size; j++) allHintsForRender.get(j).cacheDistance(px, py, pz);
                allHintsForRender.sort(FARTHEST_FIRST);
                lastPlayerPos.xCoord = px;
                lastPlayerPos.yCoord = py;
                lastPlayerPos.zCoord = pz;
            }
        }

        private static double resortThreshold(int size) {
            if (size > 100_000) return 64; // 8 blocks
            if (size > 10_000) return 4; // 2 blocks
            return 1e-2; // 0.1 blocks
        }
    }

    public static class ForgeEventHandler {

        @SubscribeEvent
        @SideOnly(Side.CLIENT)
        public void onMouseEvent(MouseEvent event) {
            final EntityPlayer player = Minecraft.getMinecraft().thePlayer;

            if (player == null || player.isDead) {
                return;
            }

            final ItemStack heldItem = player.getHeldItem();

            if (heldItem == null || !(heldItem.getItem() instanceof ItemConstructableTrigger)) return;

            if (event.button != 2 || !event.buttonstate) return;

            // Sink the middle click as soon as a projector is held, so vanilla pick block does not run
            event.setCanceled(true);

            final boolean isSneaking = player.isSneaking();
            if (isSneaking && !ChannelWipeConfirmation.confirm(player)) return;

            ChannelPickHandler.handleWorldPick(
                    player.worldObj,
                    player,
                    heldItem,
                    Minecraft.getMinecraft().objectMouseOver,
                    isSneaking);
        }

        @SubscribeEvent
        public void onEntityJoinWorld(EntityJoinWorldEvent e) {
            if (e.world.isRemote && e.entity instanceof EntityClientPlayerMP) {
                sendRegistryOrderToServer();
            }
        }

        @SubscribeEvent
        public void onWorldLoad(WorldEvent.Load e) {
            if (e.world.isRemote) {
                // flush hints. we are in a different world now.
                allHintsForRender.clear();
                deferredRenderThrough.clear();
                allHints.clear();
                allGroups.clear();
                currentHints = null;
                storedHintCount = 0;
                allHintsDirty = false;
                lastPlayerPos.yCoord = -1e30;
                // clear throttles. hopefully a world switch is enough long as a cool down.
                localThrottleMap.clear();
            }
        }

        private static int flushIfFull(Tessellator tes, int cubeDrawn) {
            if (cubeDrawn + 1 < 4096 / (6 * 2 * 3)) return cubeDrawn;
            tes.draw();
            tes.startDrawing(GL11.GL_TRIANGLES);
            return 0;
        }

        @SubscribeEvent
        public void onRenderWorldLast(RenderWorldLastEvent e) {
            final int size = allHintsForRender.size();
            if (size == 0) return;

            // seriously, I'm not a OpenGL expert, so I'm probably doing a lot of very stupid stuff here.
            // Please consider contributing patches if you find something to optimize.

            Profiler p = Minecraft.getMinecraft().mcProfiler;

            p.startSection("HintParticle");
            p.startSection("Prepare");
            Frustrum frustrum = new Frustrum();
            Entity entitylivingbase = Minecraft.getMinecraft().renderViewEntity;
            double d0 = entitylivingbase.lastTickPosX
                    + (entitylivingbase.posX - entitylivingbase.lastTickPosX) * e.partialTicks;
            double d1 = entitylivingbase.lastTickPosY
                    + (entitylivingbase.posY - entitylivingbase.lastTickPosY) * e.partialTicks;
            double d2 = entitylivingbase.lastTickPosZ
                    + (entitylivingbase.posZ - entitylivingbase.lastTickPosZ) * e.partialTicks;
            int i0 = (int) d0, i1 = (int) d1, i2 = (int) d2;
            frustrum.setPosition(d0, d1, d2);

            // hoist every config lookup out of the hot loop
            final int transparency = ConfigurationHandler.INSTANCE.getHintTransparency();
            final double radius = ConfigurationHandler.INSTANCE.getMaxRenderRadius();
            final double radiusSq = radius * radius;
            // allHintsForRender is sorted farthest first, so the nearest hints the budget should keep are the tail of
            // the list. Skipping a prefix is O(1) and, unlike counting as we draw, does not make hints pop in and out
            // as the player turns around.
            final int from = Math.max(0, size - ConfigurationHandler.INSTANCE.getMaxRenderCount());

            GL11.glPushMatrix();
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT); // TODO figure out original states
            // we need the back facing rendered because the thing is transparent
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND); // enable blend so it is transparent
            GL11.glBlendFunc(GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_SRC_ALPHA);
            // depth test begin as enabled
            Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationBlocksTexture);
            GL11.glTranslated(-d0 + i0, -d1 + i1, -d2 + i2);
            Tessellator tes = Tessellator.instance;
            tes.startDrawing(GL11.GL_TRIANGLES);
            int cubeDrawn = 0;
            for (int i = from; i < size; i++) {
                HintParticleInfo hint = allHintsForRender.get(i);
                if (!hint.isWithinRadius(d0, d1, d2, radiusSq)) continue;
                if (!hint.isInFrustrum(frustrum)) continue;
                if (hint.renderThrough) {
                    // drawn in a second pass, so the depth test is toggled once rather than at every boundary
                    deferredRenderThrough.add(hint);
                    continue;
                }
                cubeDrawn = flushIfFull(tes, cubeDrawn);
                hint.draw(tes, transparency, d0, d1, d2, i0, i1, i2);
                cubeDrawn++;
            }
            p.endStartSection("Draw");
            tes.draw();

            if (!deferredRenderThrough.isEmpty()) {
                p.endStartSection("Prepare");
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                tes.startDrawing(GL11.GL_TRIANGLES);
                cubeDrawn = 0;
                for (int i = 0, deferred = deferredRenderThrough.size(); i < deferred; i++) {
                    cubeDrawn = flushIfFull(tes, cubeDrawn);
                    deferredRenderThrough.get(i).draw(tes, transparency, d0, d1, d2, i0, i1, i2);
                    cubeDrawn++;
                }
                p.endStartSection("Draw");
                tes.draw();
                deferredRenderThrough.clear();
            }
            p.endSection();

            GL11.glPopAttrib();
            GL11.glPopMatrix();
            p.endSection();
        }
    }
}
