package appeng.client.render.previewBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

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
import appeng.api.networking.IGridHost;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.items.parts.ItemMultiPart;
import appeng.parts.networking.PartCable;

public class ViewHelper {

    private static final List<ActionMapping> ACTION_MAPPINGS = new ArrayList<>();

    private static ItemStack cachedItemStack = null;
    private static IPart cachedPart = null;
    public static int previewX, previewY, previewZ;
    private static boolean isValidPosition;
    private static boolean isActive;
    private static float currentPartialTicks;
    private static AEColor currentColor;

    private static class ActionMapping {
        private final Class<?> targetClass;
        private final Consumer<ItemStack> action;

        public ActionMapping(Class<?> targetClass, Consumer<ItemStack> action) {
            this.targetClass = targetClass;
            this.action = action;
        }
    }

    private static void register(Class<?> clazz, Consumer<ItemStack> action) {
        ACTION_MAPPINGS.add(new ActionMapping(clazz, action));
    }

    public static void handleItem(ItemStack heldItem) {
        findAction(heldItem).ifPresent(action -> action.accept(heldItem));
    }

    private static Optional<Consumer<ItemStack>> findAction(ItemStack item) {
        return getCachedPart(item).flatMap(
                part -> ACTION_MAPPINGS.stream()
                        .filter(mapping -> mapping.targetClass.isInstance(part))
                        .findFirst()
                        .map(mapping -> mapping.action));
    }

    private static Optional<IPart> getCachedPart(ItemStack item) {
        if (item == null || !(item.getItem() instanceof ItemMultiPart itemMultiPart)) {
            clearCache();
            return Optional.empty();
        }

        if (cachedItemStack != null && areItemStacksEqual(cachedItemStack, item)) {
            return Optional.ofNullable(cachedPart);
        }

        cachedItemStack = item.copy();
        try {
            cachedPart = itemMultiPart.createPartFromItemStack(item);
        } catch (Exception e) {
            clearCache();
        }

        return Optional.ofNullable(cachedPart);
    }

    private static boolean areItemStacksEqual(ItemStack stack1, ItemStack stack2) {
        if (stack1 == stack2) return true;
        if (stack1 == null || stack2 == null) return false;

        return ItemStack.areItemStacksEqual(stack1, stack2) &&
                ItemStack.areItemStackTagsEqual(stack1, stack2) &&
                stack1.stackSize == stack2.stackSize;
    }

    public static void clearCache() {
        cachedItemStack = null;
        cachedPart = null;
    }

    static {
        register(PartCable.class, ViewHelper::handleCable);
    }

    private static void handleCable(ItemStack item) {
        AECableType type = getCableType(cachedItemStack);
        if (!isActive || type == null) return;
        currentColor = getCableColor(cachedItemStack);
        renderCablePreview(currentPartialTicks, isDenseCable(type));
    }

    private static AECableType getCableType(ItemStack itemStack) {
        return getCachedPart(itemStack)
                .filter(PartCable.class::isInstance)
                .map(PartCable.class::cast)
                .map(PartCable::getCableConnectionType)
                .orElse(AECableType.NONE);
    }

    private static AEColor getCableColor(ItemStack itemStack) {
        return getCachedPart(itemStack)
                .filter(PartCable.class::isInstance)
                .map(PartCable.class::cast)
                .map(PartCable::getCableColor)
                .orElse(AEColor.Transparent);
    }

    public static void updatePreview(EntityPlayer player) {
        MovingObjectPosition mop = getTargetedBlock(player, 6.0);

        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            isActive = false;
            return;
        }

        previewX = mop.blockX;
        previewY = mop.blockY;
        previewZ = mop.blockZ;
        ForgeDirection placementSide = ForgeDirection.getOrientation(mop.sideHit);

        previewX += placementSide.offsetX;
        previewY += placementSide.offsetY;
        previewZ += placementSide.offsetZ;

        isValidPosition = canPlaceBlockAt(player.worldObj, previewX, previewY, previewZ);
        isActive = true;
    }

    public static void updatePartialTicks(float partialTicks) {
        currentPartialTicks = partialTicks;
    }

    private static MovingObjectPosition getTargetedBlock(EntityPlayer player, double reach) {
        Vec3 playerPos = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3 lookVec = player.getLook(1.0F);
        Vec3 targetPos = playerPos.addVector(lookVec.xCoord * reach, lookVec.yCoord * reach, lookVec.zCoord * reach);
        return player.worldObj.rayTraceBlocks(playerPos, targetPos, true);
    }

    private static boolean canPlaceBlockAt(net.minecraft.world.World world, int x, int y, int z) {
        if (world.isAirBlock(x, y, z)) {
            return true;
        }

        net.minecraft.block.Block block = world.getBlock(x, y, z);
        return block != null && block.isReplaceable(world, x, y, z);
    }

    public static void getValidColorGL11() {
        if (isValidPosition) {
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.6f);
        } else {
            GL11.glColor4f(1.0f, 0.0f, 0.0f, 0.6f);
        }
    }

    private static void renderCablePreview(float partialTicks, boolean isDense) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;

        if (player == null) return;

        double playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        GL11.glPushMatrix();
        GL11.glTranslated(-playerX, -playerY, -playerZ);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false);

        getValidColorGL11();

        if (isDense) {
            renderCableCore(3.0);
            renderDenseCableConnections();
        } else {
            renderCableCore(0.0);
            renderCableConnections();
        }

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private static void renderCableCore(double size) {
        double minX = previewX + (6.0 - size) / 16.0;
        double minY = previewY + (6.0 - size) / 16.0;
        double minZ = previewZ + (6.0 - size) / 16.0;
        double maxX = previewX + (10.0 + size) / 16.0;
        double maxY = previewY + (10.0 + size) / 16.0;
        double maxZ = previewZ + (10.0 + size) / 16.0;

        renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void renderWireframeCube(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
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

    private static void renderCableConnections() {
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            if (shouldRenderConnection(direction)) {
                renderNormalConnection(direction);
            }
        }
    }

    private static void renderNormalConnection(ForgeDirection direction) {
        double minX, minY, minZ, maxX, maxY, maxZ;

        switch (direction) {
            case DOWN:
                minX = previewX + 6.0 / 16.0;
                minY = previewY - 1.0 + 10.0 / 16.0;
                minZ = previewZ + 6.0 / 16.0;
                maxX = previewX + 10.0 / 16.0;
                maxY = previewY + 6.0 / 16.0;
                maxZ = previewZ + 10.0 / 16.0;
                break;
            case UP:
                minX = previewX + 6.0 / 16.0;
                minY = previewY + 10.0 / 16.0;
                minZ = previewZ + 6.0 / 16.0;
                maxX = previewX + 10.0 / 16.0;
                maxY = previewY + 1.0 + 6.0 / 16.0;
                maxZ = previewZ + 10.0 / 16.0;
                break;
            case NORTH:
                minX = previewX + 6.0 / 16.0;
                minY = previewY + 6.0 / 16.0;
                minZ = previewZ - 1.0 + 10.0 / 16.0;
                maxX = previewX + 10.0 / 16.0;
                maxY = previewY + 10.0 / 16.0;
                maxZ = previewZ + 6.0 / 16.0;
                break;
            case SOUTH:
                minX = previewX + 6.0 / 16.0;
                minY = previewY + 6.0 / 16.0;
                minZ = previewZ + 10.0 / 16.0;
                maxX = previewX + 10.0 / 16.0;
                maxY = previewY + 10.0 / 16.0;
                maxZ = previewZ + 1.0 + 6.0 / 16.0;
                break;
            case WEST:
                minX = previewX - 1.0 + 10.0 / 16.0;
                minY = previewY + 6.0 / 16.0;
                minZ = previewZ + 6.0 / 16.0;
                maxX = previewX + 6.0 / 16.0;
                maxY = previewY + 10.0 / 16.0;
                maxZ = previewZ + 10.0 / 16.0;
                break;
            case EAST:
                minX = previewX + 10.0 / 16.0;
                minY = previewY + 6.0 / 16.0;
                minZ = previewZ + 6.0 / 16.0;
                maxX = previewX + 1.0 + 6.0 / 16.0;
                maxY = previewY + 10.0 / 16.0;
                maxZ = previewZ + 10.0 / 16.0;
                break;
            default:
                return;
        }

        renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static boolean shouldRenderConnection(ForgeDirection direction) {
        TileEntity te = Minecraft.getMinecraft().theWorld.getTileEntity(
                previewX + direction.offsetX,
                previewY + direction.offsetY,
                previewZ + direction.offsetZ);

        if (te == null) {
            return false;
        }

        if (te instanceof IPartHost) {
            return hasConnectablePart((IPartHost) te, direction.getOpposite());
        }

        if (te instanceof IGridHost) {
            return canConnectToGridHost((IGridHost) te, direction.getOpposite());
        }

        return false;
    }

    private static boolean hasConnectablePart(IPartHost partHost, ForgeDirection side) {
        IPart centerPart = partHost.getPart(ForgeDirection.UNKNOWN);
        if (canConnectToPart(centerPart, side)) {
            return true;
        }

        for (ForgeDirection partSide : ForgeDirection.VALID_DIRECTIONS) {
            IPart part = partHost.getPart(partSide);
            if (canConnectToPart(part, side)) {
                return true;
            }
        }

        if (partHost instanceof IGridHost gridHost) {
            return canConnectToGridHost(gridHost, side) && !isCable(centerPart);
        }

        return false;
    }

    private static boolean isCable(IPart iPart) {
        return iPart instanceof PartCable;
    }

    private static boolean canConnectToGridHost(IGridHost gridHost, ForgeDirection side) {
        try {
            AECableType connectionType = gridHost.getCableConnectionType(side);
            return connectionType != null && connectionType != AECableType.NONE;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean canConnectToPart(IPart part, ForgeDirection side) {
        if (part instanceof IPartCable) {
            AEColor color = ((IPartCable) part).getCableColor();
            return isColorCapabilities(color, currentColor);
        }
        return false;
    }

    private static boolean isColorCapabilities(AEColor color1, AEColor color2) {
        if (color1 == AEColor.Transparent || color2 == AEColor.Transparent) return true;
        return color1.matches(color2);
    }

    private static boolean isDenseCable(AECableType type) {
        return type == AECableType.DENSE || type == AECableType.DENSE_COVERED;
    }

    private static void renderDenseCableConnections() {
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            if (shouldRenderConnection(direction)) {
                AECableType neighborType = getNeighborCableType(direction);
                if (isDenseCable(neighborType)) {
                    renderDenseConnection(direction);
                } else {
                    renderNormalConnectionForDense(direction);
                }
            }
        }
    }

    private static AECableType getNeighborCableType(ForgeDirection direction) {
        int neighborX = previewX + direction.offsetX;
        int neighborY = previewY + direction.offsetY;
        int neighborZ = previewZ + direction.offsetZ;

        TileEntity te = Minecraft.getMinecraft().theWorld.getTileEntity(neighborX, neighborY, neighborZ);
        if (te == null) {
            return AECableType.NONE;
        }

        if (te instanceof IPartHost partHost) {
            IPart centerPart = partHost.getPart(ForgeDirection.UNKNOWN);
            if (centerPart instanceof IGridHost gridHost) {
                AECableType connectionType = gridHost.getCableConnectionType(direction.getOpposite());
                if (connectionType == null) return AECableType.NONE;
                if (gridHost instanceof PartCable cable) {
                    AEColor color = cable.getCableColor();
                    if (isColorCapabilities(color, currentColor)) {
                        return connectionType;
                    }
                }
            }

            for (ForgeDirection partSide : ForgeDirection.VALID_DIRECTIONS) {
                IPart part = partHost.getPart(partSide);
                if (part instanceof IGridHost gridHost) {
                    AECableType connectionType = gridHost.getCableConnectionType(direction.getOpposite());
                    if (connectionType != null && connectionType != AECableType.NONE) {
                        return connectionType;
                    }
                }
            }

            return AECableType.NONE;
        }

        if (te instanceof IGridHost gridHost) {
            AECableType connectionType = gridHost.getCableConnectionType(direction.getOpposite());
            if (connectionType != null && connectionType != AECableType.NONE) {
                return connectionType;
            }
        }

        return AECableType.NONE;
    }

    private static void renderDenseConnection(ForgeDirection direction) {
        double minX, minY, minZ, maxX, maxY, maxZ;
        switch (direction) {
            case DOWN:
                minX = previewX + 4.0 / 16.0;
                minY = previewY - 1.0 + 12.0 / 16.0;
                minZ = previewZ + 4.0 / 16.0;
                maxX = previewX + 12.0 / 16.0;
                maxY = previewY + 3.0 / 16.0;
                maxZ = previewZ + 12.0 / 16.0;
                break;
            case UP:
                minX = previewX + 4.0 / 16.0;
                minY = previewY + 13.0 / 16.0;
                minZ = previewZ + 4.0 / 16.0;
                maxX = previewX + 12.0 / 16.0;
                maxY = previewY + 1.0 + 3.0 / 16.0;
                maxZ = previewZ + 12.0 / 16.0;
                break;
            case NORTH:
                minX = previewX + 4.0 / 16.0;
                minY = previewY + 4.0 / 16.0;
                minZ = previewZ - 1.0 + 12.0 / 16.0;
                maxX = previewX + 12.0 / 16.0;
                maxY = previewY + 12.0 / 16.0;
                maxZ = previewZ + 3.0 / 16.0;
                break;
            case SOUTH:
                minX = previewX + 4.0 / 16.0;
                minY = previewY + 4.0 / 16.0;
                minZ = previewZ + 13.0 / 16.0;
                maxX = previewX + 12.0 / 16.0;
                maxY = previewY + 12.0 / 16.0;
                maxZ = previewZ + 1.0 + 3.0 / 16.0;
                break;
            case WEST:
                minX = previewX - 1.0 + 12.0 / 16.0;
                minY = previewY + 4.0 / 16.0;
                minZ = previewZ + 4.0 / 16.0;
                maxX = previewX + 3.0 / 16.0;
                maxY = previewY + 12.0 / 16.0;
                maxZ = previewZ + 12.0 / 16.0;
                break;
            case EAST:
                minX = previewX + 13.0 / 16.0;
                minY = previewY + 4.0 / 16.0;
                minZ = previewZ + 4.0 / 16.0;
                maxX = previewX + 1.0 + 3.0 / 16.0;
                maxY = previewY + 12.0 / 16.0;
                maxZ = previewZ + 12.0 / 16.0;
                break;
            default:
                return;
        }

        renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void renderNormalConnectionForDense(ForgeDirection direction) {
        double minX, minY, minZ, maxX, maxY, maxZ;
        switch (direction) {
            case DOWN:
                minX = previewX + 6.0 / 16.0;
                minY = previewY - 1.0 + 10.0 / 16.0;
                minZ = previewZ + 6.0 / 16.0;
                maxX = previewX + 10.0 / 16.0;
                maxY = previewY + 3.0 / 16.0;
                maxZ = previewZ + 10.0 / 16.0;
                break;
            case UP:
                minX = previewX + 6.0 / 16.0;
                minY = previewY + 13.0 / 16.0;
                minZ = previewZ + 6.0 / 16.0;
                maxX = previewX + 10.0 / 16.0;
                maxY = previewY + 1.0 + 6.0 / 16.0;
                maxZ = previewZ + 10.0 / 16.0;
                break;
            case NORTH:
                minX = previewX + 6.0 / 16.0;
                minY = previewY + 6.0 / 16.0;
                minZ = previewZ - 1.0 + 10.0 / 16.0;
                maxX = previewX + 10.0 / 16.0;
                maxY = previewY + 10.0 / 16.0;
                maxZ = previewZ + 3.0 / 16.0;
                break;
            case SOUTH:
                minX = previewX + 6.0 / 16.0;
                minY = previewY + 6.0 / 16.0;
                minZ = previewZ + 13.0 / 16.0;
                maxX = previewX + 10.0 / 16.0;
                maxY = previewY + 10.0 / 16.0;
                maxZ = previewZ + 1.0 + 6.0 / 16.0;
                break;
            case WEST:
                minX = previewX - 1.0 + 10.0 / 16.0;
                minY = previewY + 6.0 / 16.0;
                minZ = previewZ + 6.0 / 16.0;
                maxX = previewX + 3.0 / 16.0;
                maxY = previewY + 10.0 / 16.0;
                maxZ = previewZ + 10.0 / 16.0;
                break;
            case EAST:
                minX = previewX + 13.0 / 16.0;
                minY = previewY + 6.0 / 16.0;
                minZ = previewZ + 6.0 / 16.0;
                maxX = previewX + 1.0 + 6.0 / 16.0;
                maxY = previewY + 10.0 / 16.0;
                maxZ = previewZ + 10.0 / 16.0;
                break;
            default:
                return;
        }

        renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
    }
}