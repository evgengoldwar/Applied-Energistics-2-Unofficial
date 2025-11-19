package appeng.client.render.previewBlocks;

import static appeng.util.Platform.getEyeOffset;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.glodblock.github.common.parts.PartFluidExportBus;
import com.glodblock.github.common.parts.PartFluidImportBus;
import com.glodblock.github.common.parts.PartFluidInterface;
import com.glodblock.github.common.parts.PartFluidLevelEmitter;
import com.glodblock.github.common.parts.PartFluidPatternTerminal;
import com.glodblock.github.common.parts.PartFluidPatternTerminalEx;
import com.glodblock.github.common.parts.PartFluidStorageBus;
import com.glodblock.github.common.parts.PartFluidTerminal;
import com.glodblock.github.common.parts.PartLevelTerminal;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.parts.automation.PartAnnihilationPlane;
import appeng.parts.automation.PartExportBus;
import appeng.parts.automation.PartFormationPlane;
import appeng.parts.automation.PartImportBus;
import appeng.parts.automation.PartLevelEmitter;
import appeng.parts.misc.PartCableAnchor;
import appeng.parts.misc.PartInterface;
import appeng.parts.misc.PartStorageBus;
import appeng.parts.misc.PartToggleBus;
import appeng.parts.networking.PartCable;
import appeng.parts.networking.PartQuartzFiber;
import appeng.parts.p2p.PartP2PTunnel;
import appeng.parts.reporting.AbstractPartDisplay;
import appeng.parts.reporting.PartDarkPanel;
import appeng.parts.reporting.PartPanel;
import appeng.parts.reporting.PartSemiDarkPanel;
import appeng.util.LookDirection;
import appeng.util.Platform;

public class ViewHelper {

    private static final List<ActionMapping> ACTION_MAPPINGS = new ArrayList<>();
    public static ItemStack cachedItemStack = null;
    private static IPart cachedPart = null;
    public static int previewX, previewY, previewZ;
    private static boolean isValidPosition;
    private static boolean isActive;
    public static float currentPartialTicks;
    public static AEColor currentColor;
    public static ForgeDirection placementSide;
    public static World world;
    public static EntityPlayer player;

    private static class ActionMapping {

        private final Class<?> targetClass;
        private final Consumer<ItemStack> action;

        public ActionMapping(Class<?> targetClass, Consumer<ItemStack> action) {
            this.targetClass = targetClass;
            this.action = action;
        }
    }

    static {
        register(PartCable.class, ViewHelper::handleCable);
        register(AbstractPartDisplay.class, ViewHelper::handleTerminal);
        register(PartFluidTerminal.class, ViewHelper::handleTerminal);
        register(PartP2PTunnel.class, ViewHelper::handleTerminal);
        register(PartPanel.class, ViewHelper::handleTerminal);
        register(PartSemiDarkPanel.class, ViewHelper::handleTerminal);
        register(PartDarkPanel.class, ViewHelper::handleTerminal);
        register(PartStorageBus.class, ViewHelper::handleTerminal);
        register(PartFluidStorageBus.class, ViewHelper::handleTerminal);
        register(PartInterface.class, ViewHelper::handleTerminal);
        register(PartFluidInterface.class, ViewHelper::handleTerminal);
        register(PartToggleBus.class, ViewHelper::handleToggleBus);
        register(PartQuartzFiber.class, ViewHelper::handleQuartzFiber);
        register(PartCableAnchor.class, ViewHelper::handleCableAnchor);
        register(PartImportBus.class, ViewHelper::handleImportBus);
        register(PartExportBus.class, ViewHelper::handleExportBus);
        register(PartFluidImportBus.class, ViewHelper::handleImportBus);
        register(PartFluidExportBus.class, ViewHelper::handleExportBus);
        register(PartFluidLevelEmitter.class, ViewHelper::handleLevelEmitter);
        register(PartLevelEmitter.class, ViewHelper::handleLevelEmitter);
        register(PartAnnihilationPlane.class, ViewHelper::handleAnnihilationPlane);
        register(PartFormationPlane.class, ViewHelper::handleAnnihilationPlane);
        register(PartFluidPatternTerminal.class, ViewHelper::handleTerminal);
        register(PartFluidPatternTerminalEx.class, ViewHelper::handleTerminal);
        register(PartLevelTerminal.class, ViewHelper::handleTerminal);
    }

    private static void register(Class<?> clazz, Consumer<ItemStack> action) {
        try {
            ACTION_MAPPINGS.add(new ActionMapping(clazz, action));
        } catch (NoClassDefFoundError | Exception ignored) {}
    }

    public static void handleItem(ItemStack heldItem) {
        findAction(heldItem).ifPresent(action -> action.accept(heldItem));
    }

    private static Optional<Consumer<ItemStack>> findAction(ItemStack item) {
        return getCachedPart(item).flatMap(
                part -> ACTION_MAPPINGS.stream().filter(mapping -> mapping.targetClass.isInstance(part)).findFirst()
                        .map(mapping -> mapping.action));
    }

    private static Optional<IPart> getCachedPart(ItemStack item) {
        if (item == null || !(item.getItem() instanceof IPartItem iPartItem)) {
            clearCache();
            return Optional.empty();
        }

        if (cachedItemStack != null && areItemStacksEqual(cachedItemStack, item)) {
            return Optional.ofNullable(cachedPart);
        }

        cachedItemStack = item.copy();
        try {
            cachedPart = iPartItem.createPartFromItemStack(item);
        } catch (Exception e) {
            clearCache();
        }

        return Optional.ofNullable(cachedPart);
    }

    public static boolean hasParts(IPartHost partHost) {
        if (partHost.getPart(ForgeDirection.UNKNOWN) != null) {
            return true;
        }

        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            if (partHost.getPart(dir) != null) {
                return true;
            }
        }

        return false;
    }

    private static boolean areItemStacksEqual(ItemStack stack1, ItemStack stack2) {
        if (stack1 == stack2) return true;
        if (stack1 == null || stack2 == null) return false;
        return ItemStack.areItemStacksEqual(stack1, stack2) && ItemStack.areItemStackTagsEqual(stack1, stack2)
                && stack1.stackSize == stack2.stackSize;
    }

    public static void clearCache() {
        cachedItemStack = null;
        cachedPart = null;
    }

    private static void handleCable(ItemStack item) {
        AECableType type = getCableType(cachedItemStack);
        if (!isActive || type == null) return;
        currentColor = getCableColor(cachedItemStack);
        RenderCable.renderCablePreview(RenderCable.isDenseCable(type));
    }

    private static void handleTerminal(ItemStack item) {
        if (!isActive) return;
        RenderTerminal.renderTerminalPreview();
    }

    private static void handleToggleBus(ItemStack item) {
        if (!isActive) return;
        RenderToggleBus.renderToggleBusPreview();
    }

    private static void handleCableAnchor(ItemStack item) {
        if (!isActive) return;
        RenderCableAnchor.renderCableAnchorPreview();
    }

    private static void handleQuartzFiber(ItemStack item) {
        if (!isActive) return;
        RenderQuartzFiber.renderQuartzFiberPreview();
    }

    private static void handleImportBus(ItemStack item) {
        if (!isActive) return;
        RenderImportBus.renderImportBusPreview();
    }

    private static void handleExportBus(ItemStack item) {
        if (!isActive) return;
        RenderExportBus.renderExportBusPreview();
    }

    private static void handleLevelEmitter(ItemStack item) {
        if (!isActive) return;
        RenderLevelEmitter.renderLevelEmitterPreview();
    }

    private static void handleAnnihilationPlane(ItemStack item) {
        if (!isActive) return;
        RenderAnnihilationPlane.renderAnnihilationPlanePreview();
    }

    public static AECableType getCableType(ItemStack itemStack) {
        return getCachedPart(itemStack).filter(PartCable.class::isInstance).map(PartCable.class::cast)
                .map(PartCable::getCableConnectionType).orElse(AECableType.NONE);
    }

    private static AEColor getCableColor(ItemStack itemStack) {
        return getCachedPart(itemStack).filter(PartCable.class::isInstance).map(PartCable.class::cast)
                .map(PartCable::getCableColor).orElse(AEColor.Transparent);
    }

    public static void updatePreview(EntityPlayer player) {
        MovingObjectPosition mopBlock = getTargetedBlock(player);

        if (mopBlock == null || mopBlock.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            isActive = false;
            return;
        }

        final LookDirection dir = Platform.getPlayerRay(player, getEyeOffset(player));
        Block block = player.worldObj.getBlock(mopBlock.blockX, mopBlock.blockY, mopBlock.blockZ);

        if (block == null) {
            isActive = false;
            return;
        }

        final MovingObjectPosition mop = block.collisionRayTrace(
                player.worldObj,
                mopBlock.blockX,
                mopBlock.blockY,
                mopBlock.blockZ,
                dir.getA(),
                dir.getB());

        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            isActive = false;
            return;
        }

        boolean isCable = isCableItem();
        boolean isTerminal = isTerminalItem();
        boolean isToggleBus = isToggleBusItem();
        boolean isCableAnchor = isCableAnchorItem();
        boolean isQuartzFiber = isQuartzFiberItem();
        boolean isImportBus = isImportBusItem();
        boolean isExportBus = isExportBusItem();
        boolean isLevelEmitter = isLevelEmitterItem();
        boolean isAnnihilationPlane = isAnnihilationPlaneItem();

        previewX = mop.blockX;
        previewY = mop.blockY;
        previewZ = mop.blockZ;
        placementSide = ForgeDirection.getOrientation(mop.sideHit);

        if (isTerminal || isToggleBus
                || isCableAnchor
                || isQuartzFiber
                || isImportBus
                || isExportBus
                || isLevelEmitter
                || isAnnihilationPlane) {
            isValidPosition = RenderTerminal
                    .canPlaceParts(player.worldObj, placementSide, previewX, previewY, previewZ);
        } else if (isCable) {
            isValidPosition = RenderCable.canPlaceCable();
        } else {
            isValidPosition = false;
        }

        isActive = true;
    }

    private static boolean isCableItem() {
        return getCachedPart(cachedItemStack).filter(PartCable.class::isInstance).isPresent();
    }

    private static boolean isTerminalItem() {
        return isItemOfClasses(
                AbstractPartDisplay.class,
                PartP2PTunnel.class,
                PartPanel.class,
                PartDarkPanel.class,
                PartSemiDarkPanel.class,
                PartStorageBus.class,
                PartInterface.class,
                PartFluidTerminal.class,
                PartFluidStorageBus.class,
                PartFluidPatternTerminalEx.class,
                PartFluidPatternTerminal.class,
                PartLevelTerminal.class);
    }

    private static boolean isToggleBusItem() {
        return getCachedPart(cachedItemStack).filter(PartToggleBus.class::isInstance).isPresent();
    }

    private static boolean isCableAnchorItem() {
        return getCachedPart(cachedItemStack).filter(PartCableAnchor.class::isInstance).isPresent();
    }

    private static boolean isQuartzFiberItem() {
        return getCachedPart(cachedItemStack).filter(PartQuartzFiber.class::isInstance).isPresent();
    }

    private static boolean isImportBusItem() {
        return isItemOfClasses(PartImportBus.class, PartFluidImportBus.class);
    }

    private static boolean isExportBusItem() {
        return isItemOfClasses(PartExportBus.class, PartFluidExportBus.class);
    }

    private static boolean isLevelEmitterItem() {
        return isItemOfClasses(PartLevelEmitter.class, PartFluidLevelEmitter.class);
    }

    private static boolean isAnnihilationPlaneItem() {
        return isItemOfClasses(PartFormationPlane.class, PartAnnihilationPlane.class);
    }

    private static boolean isItemOfClasses(Class<?>... classes) {

        Optional<IPart> cachedPart = getCachedPart(cachedItemStack);
        if (!cachedPart.isPresent()) {
            return false;
        }

        IPart part = cachedPart.get();
        for (Class<?> clazz : classes) {
            try {
                if (clazz.isInstance(part)) {
                    return true;
                }
            } catch (NoClassDefFoundError | Exception ignored) {}
        }
        return false;
    }

    public static void updatePartialTicks(float partialTicks) {
        currentPartialTicks = partialTicks;
    }

    public static boolean canPlaceBlockAt(net.minecraft.world.World world, int x, int y, int z) {
        if (world.isAirBlock(x, y, z)) {
            return true;
        }
        Block block = world.getBlock(x, y, z);
        return block != null && block.isReplaceable(world, x, y, z);
    }

    public static void getValidColorGL11() {
        if (isValidPosition) {
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.6f);
        } else {
            GL11.glColor4f(1.0f, 0.0f, 0.0f, 0.6f);
        }
    }

    public static void setPreviewOffset() {
        previewX += placementSide.offsetX;
        previewY += placementSide.offsetY;
        previewZ += placementSide.offsetZ;
    }

    private static MovingObjectPosition getTargetedBlock(EntityPlayer player) {
        return Platform.rayTrace(player, true, false);
    }

    public static void renderWireframeCube(double minX, double minY, double minZ, double maxX, double maxY,
            double maxZ) {
        Tessellator tessellator = Tessellator.instance;

        GL11.glLineWidth(3.0f);
        tessellator.startDrawing(GL11.GL_LINES);

        tessellator.addVertex(minX, minY, minZ);
        tessellator.addVertex(maxX, minY, minZ);

        tessellator.addVertex(maxX, minY, minZ);
        tessellator.addVertex(maxX, minY, maxZ);

        tessellator.addVertex(maxX, minY, maxZ);
        tessellator.addVertex(minX, minY, maxZ);

        tessellator.addVertex(minX, minY, maxZ);
        tessellator.addVertex(minX, minY, minZ);

        tessellator.addVertex(minX, maxY, minZ);
        tessellator.addVertex(maxX, maxY, minZ);

        tessellator.addVertex(maxX, maxY, minZ);
        tessellator.addVertex(maxX, maxY, maxZ);

        tessellator.addVertex(maxX, maxY, maxZ);
        tessellator.addVertex(minX, maxY, maxZ);

        tessellator.addVertex(minX, maxY, maxZ);
        tessellator.addVertex(minX, maxY, minZ);

        tessellator.addVertex(minX, minY, minZ);
        tessellator.addVertex(minX, maxY, minZ);

        tessellator.addVertex(maxX, minY, minZ);
        tessellator.addVertex(maxX, maxY, minZ);

        tessellator.addVertex(maxX, minY, maxZ);
        tessellator.addVertex(maxX, maxY, maxZ);

        tessellator.addVertex(minX, minY, maxZ);
        tessellator.addVertex(minX, maxY, maxZ);

        tessellator.draw();
    }

    public static World getWorld() {
        return player.worldObj;
    }

    public static EntityPlayer getPlayer() {
        return player;
    }

    public static void setPlayer(EntityPlayer player) {
        ViewHelper.player = player;
    }
}
