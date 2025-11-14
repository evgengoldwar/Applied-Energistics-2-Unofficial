package appeng.client.render.previewBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import appeng.api.implementations.parts.IPartCable;
import appeng.api.parts.BusSupport;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.items.parts.ItemMultiPart;
import appeng.parts.networking.PartCable;
import appeng.parts.reporting.AbstractPartDisplay;

public class ViewHelper {

    private static final List<ActionMapping> ACTION_MAPPINGS = new ArrayList<>();
    private static ItemStack cachedItemStack = null;
    private static IPart cachedPart = null;
    public static int previewX, previewY, previewZ;
    private static boolean isValidPosition;
    private static boolean isActive;
    public static float currentPartialTicks;
    public static AEColor currentColor;
    public static ForgeDirection placementSide;

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
    }

    private static void register(Class<?> clazz, Consumer<ItemStack> action) {
        ACTION_MAPPINGS.add(new ActionMapping(clazz, action));
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
        if (item == null || !(item.getItem() instanceof ItemMultiPart)) {
            clearCache();
            return Optional.empty();
        }

        if (cachedItemStack != null && areItemStacksEqual(cachedItemStack, item)) {
            return Optional.ofNullable(cachedPart);
        }

        cachedItemStack = item.copy();
        try {
            ItemMultiPart itemMultiPart = (ItemMultiPart) item.getItem();
            cachedPart = itemMultiPart.createPartFromItemStack(item);
        } catch (Exception e) {
            clearCache();
        }

        return Optional.ofNullable(cachedPart);
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

    private static AECableType getCableType(ItemStack itemStack) {
        return getCachedPart(itemStack).filter(PartCable.class::isInstance).map(PartCable.class::cast)
                .map(PartCable::getCableConnectionType).orElse(AECableType.NONE);
    }

    private static AEColor getCableColor(ItemStack itemStack) {
        return getCachedPart(itemStack).filter(PartCable.class::isInstance).map(PartCable.class::cast)
                .map(PartCable::getCableColor).orElse(AEColor.Transparent);
    }

    public static void updatePreview(EntityPlayer player) {
        MovingObjectPosition mop = getTargetedBlock(player, 6.0);

        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            isActive = false;
            return;
        }

        boolean isCable = isCableItem(cachedItemStack);
        boolean isTerminal = isTerminalItem(cachedItemStack);

        previewX = mop.blockX;
        previewY = mop.blockY;
        previewZ = mop.blockZ;
        placementSide = ForgeDirection.getOrientation(mop.sideHit);

        if (isTerminal) {
            isValidPosition = canPlaceTerminalOnSide(player.worldObj, previewX, previewY, previewZ, placementSide);
        } else if (isCable) {
            previewX += placementSide.offsetX;
            previewY += placementSide.offsetY;
            previewZ += placementSide.offsetZ;
            isValidPosition = canPlaceBlockAt(player.worldObj, previewX, previewY, previewZ);
        } else {
            isValidPosition = false;
        }

        isActive = true;
    }

    private static boolean isCableItem(ItemStack item) {
        return getCachedPart(item).filter(PartCable.class::isInstance).isPresent();
    }

    private static boolean isTerminalItem(ItemStack item) {
        return getCachedPart(item).filter(AbstractPartDisplay.class::isInstance).isPresent();
    }

    private static boolean canPlaceTerminalOnSide(net.minecraft.world.World world, int x, int y, int z,
            ForgeDirection side) {
        int neighborX = x + side.offsetX;
        int neighborY = y + side.offsetY;
        int neighborZ = z + side.offsetZ;
        TileEntity te = world.getTileEntity(x, y, z);
        TileEntity neighborTe = world.getTileEntity(neighborX, neighborY, neighborZ);
        boolean canPlaceOnNeighborBlock = canPlaceBlockAt(world, neighborX, neighborY, neighborZ);

        if (te instanceof IPartHost partHost) {
            return canPlaceOnPartHost(partHost, side, canPlaceOnNeighborBlock, neighborTe);
        }

        if (neighborTe instanceof IPartHost partHost) {
            return canPlaceOnPartHost(partHost, side, canPlaceOnNeighborBlock, null);
        }

        return canPlaceOnNeighborBlock;
    }

    private static boolean canPlaceOnPartHost(IPartHost partHost, ForgeDirection side, boolean canPlaceOnNeighborBlock,
            TileEntity te) {
        IPart existingPart = partHost.getPart(side);
        if (existingPart != null && !shouldPlaceOnNeighborBlock()) {
            return false;
        }

        IPart centerPart = partHost.getPart(ForgeDirection.UNKNOWN);

        if (centerPart instanceof IPartCable cablePart) {
            BusSupport busSupport = cablePart.supportsBuses();
            if (busSupport == BusSupport.CABLE) {
                return true;
            } else {
                return canPlaceOnNeighborBlock;
            }
        }

        if (hasParts(partHost) && partHost.getPart(placementSide.getOpposite()) == null) {
            return true;
        }

        if (te instanceof IPartHost neighborPartHost) {
            return canPlaceOnPartHost(neighborPartHost, side, canPlaceOnNeighborBlock, null);
        }

        return canPlaceOnNeighborBlock;
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

    public static boolean shouldPlaceOnNeighborBlock() {
        TileEntity te = Minecraft.getMinecraft().theWorld.getTileEntity(previewX, previewY, previewZ);

        if (!(te instanceof IPartHost partHost)) {
            return true;
        }

        IPart existingPart = partHost.getPart(placementSide);
        if (existingPart != null) {
            return true;
        }

        IPart centerPart = partHost.getPart(ForgeDirection.UNKNOWN);

        if (centerPart instanceof PartCable cablePart) {
            BusSupport busSupport = cablePart.supportsBuses();
            return busSupport != BusSupport.CABLE;
        }

        if (hasParts(partHost)) {
            return false;
        }

        return true;
    }

    public static void updatePartialTicks(float partialTicks) {
        currentPartialTicks = partialTicks;
    }

    private static boolean canPlaceBlockAt(net.minecraft.world.World world, int x, int y, int z) {
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

    private static MovingObjectPosition getTargetedBlock(EntityPlayer player, double reach) {
        Vec3 playerPos = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3 lookVec = player.getLook(1.0F);
        Vec3 targetPos = playerPos.addVector(lookVec.xCoord * reach, lookVec.yCoord * reach, lookVec.zCoord * reach);
        return player.worldObj.rayTraceBlocks(playerPos, targetPos, true);
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
}
