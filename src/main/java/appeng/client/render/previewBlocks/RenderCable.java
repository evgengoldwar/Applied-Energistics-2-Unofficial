package appeng.client.render.previewBlocks;

import static appeng.client.render.previewBlocks.ViewHelper.*;

import appeng.me.helpers.AENetworkProxy;
import appeng.tile.grid.AENetworkInvTile;
import appeng.tile.grid.AENetworkPowerTile;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import appeng.api.implementations.parts.IPartCable;
import appeng.api.networking.IGridHost;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.parts.networking.PartCable;

import java.util.EnumSet;
import java.util.function.Supplier;

public class RenderCable {

    private static boolean isShortest = false;

    public static void renderCablePreview(boolean isDense) {
        EntityPlayer player = getPlayer();
        if (player == null) return;

        double playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * currentPartialTicks;
        double playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * currentPartialTicks;
        double playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * currentPartialTicks;

        GL11.glPushMatrix();
        GL11.glTranslated(-playerX, -playerY, -playerZ);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false);

        ViewHelper.getValidColorGL11();

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

    public static boolean canPlaceCable() {
        AECableType cableType = getCableType(cachedItemStack);
        boolean isDense = false;
        if (cableType != null) {
            isDense = cableType == AECableType.DENSE || cableType == AECableType.DENSE_COVERED;
        }
        TileEntity te = getWorld().getTileEntity(
                previewX,
                previewY,
                previewZ);
        TileEntity neighborTe = getWorld().getTileEntity(
                previewX + placementSide.offsetX,
                previewY + placementSide.offsetY,
                previewZ + placementSide.offsetZ);

        if (te == null) {
            setPreviewOffset();
            if (neighborTe != null) {
                if (neighborTe instanceof IPartHost partHost) {
                    IPart centerPart = partHost.getPart(ForgeDirection.UNKNOWN);
                    if (isDense) return false;
                    if (centerPart == null) return true;
                }
            }
            return canPlaceBlockAt(getWorld(), previewX, previewY, previewZ);
        }

        if (te instanceof IPartHost partHost) {
            IPart centerPart = partHost.getPart(ForgeDirection.UNKNOWN);
            if (centerPart != null) {
                setPreviewOffset();
            } else if (isDense) {
                setPreviewOffset();
            } else return true;
            if (neighborTe != null) {
                if (neighborTe instanceof IPartHost partHostNeighbor) {
                    IPart centerPartNeighbor = partHostNeighbor.getPart(ForgeDirection.UNKNOWN);
                    if (isDense) return false;
                    return centerPartNeighbor == null;
                }
            } else {
                return canPlaceBlockAt(getWorld(), previewX, previewY, previewZ);
            }
            return false;
        }

        if (neighborTe instanceof IPartHost partHost) {
            IPart centerPart = partHost.getPart(ForgeDirection.UNKNOWN);
            if (centerPart != null) {
                setPreviewOffset();
                return false;
            }
            return true;
        }
        if (te instanceof IGridHost gridHost) {
            setPreviewOffset();
            return canConnectToGridHost(gridHost, placementSide.getOpposite()) || canPlaceBlockAt(getWorld(), previewX, previewY, previewZ);
        }

        return false;
    }

    // region Cable
    private static void renderCableConnections() {
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            if (shouldRenderConnection(direction)) {
                renderNormalConnection(direction, isShortest);
            }
        }
    }

    private static boolean shouldRenderConnection(ForgeDirection direction) {
        TileEntity neighborTe = getWorld().getTileEntity(
                previewX + direction.offsetX,
                previewY + direction.offsetY,
                previewZ + direction.offsetZ);
        TileEntity te = getWorld().getTileEntity(previewX,previewY,previewZ);

        if (te != null) {
            if (te instanceof IPartHost partHost) {
                IPart part = partHost.getPart(direction);
                if (part != null) {
                    isShortest = true;
                    return true;
                }
            }
        }

        if (neighborTe == null) {
            return false;
        }

        if (neighborTe instanceof IPartHost partHost) {
            isShortest = false;
            return hasConnectablePart(partHost, direction.getOpposite());
        }

        if (neighborTe instanceof IGridHost gridHost) {
            isShortest = true;
            return canConnectToGridHost(gridHost, direction.getOpposite());
        }

        return false;
    }

    private static boolean hasConnectablePart(IPartHost partHost, ForgeDirection side) {
        IPart centerPart = partHost.getPart(ForgeDirection.UNKNOWN);
        IPart partSide = partHost.getPart(side);
        if (canConnectToPart(centerPart, side) && partSide == null) {
            return true;
        }

        for (ForgeDirection sides : ForgeDirection.VALID_DIRECTIONS) {
            IPart part = partHost.getPart(sides);
            if (canConnectToPart(part, side) && partSide == null) {
                return true;
            }
        }

//        if (partHost instanceof IGridHost gridHost) {
//            return canConnectToGridHost(gridHost, side) && !(centerPart instanceof  PartCable);
//        }

        return false;
    }

    private static boolean canConnectToGridHost(IGridHost gridHost, ForgeDirection side) {
        AECableType connectionType = gridHost.getCableConnectionType(side);
        if (gridHost instanceof AENetworkInvTile aeNetworkInvTile) {
            return hasConnectableSide(aeNetworkInvTile::getProxy, side);
        }

        if (gridHost instanceof AENetworkPowerTile aeNetworkPowerTile) {
            return hasConnectableSide(aeNetworkPowerTile::getProxy, side);
        }
        return connectionType != null && connectionType != AECableType.NONE;
    }

    private static boolean hasConnectableSide(Supplier<AENetworkProxy> proxySupplier, ForgeDirection side) {
        EnumSet<ForgeDirection> connectableSides = proxySupplier.get().getConnectableSides();
        if (connectableSides.isEmpty()) proxySupplier.get().onReady();
        return !connectableSides.isEmpty() && connectableSides.contains(side);
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

    // endregion

    // region Dense cable

    public static boolean isDenseCable(AECableType type) {
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
    // endregion

    // region render
    private static void renderCableCore(double size) {
        double minX = previewX + (6.0 - size) / 16.0;
        double minY = previewY + (6.0 - size) / 16.0;
        double minZ = previewZ + (6.0 - size) / 16.0;
        double maxX = previewX + (10.0 + size) / 16.0;
        double maxY = previewY + (10.0 + size) / 16.0;
        double maxZ = previewZ + (10.0 + size) / 16.0;

        renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void renderNormalConnection(ForgeDirection direction, boolean shortest) {
        double minX, minY, minZ, maxX, maxY, maxZ;

        switch (direction) {
            case DOWN:
                minX = previewX + 6.0 / 16.0;
                minY = previewY + (shortest ? 0.0 : -1.0 + 10.0 / 16.0);
                minZ = previewZ + 6.0 / 16.0;
                maxX = previewX + 10.0 / 16.0;
                maxY = previewY + (shortest ? 10.0 / 16.0 : 6.0 / 16.0);
                maxZ = previewZ + 10.0 / 16.0;
                break;
            case UP:
                minX = previewX + 6.0 / 16.0;
                minY = previewY + (shortest ? 6.0 / 16.0 : 10.0 / 16.0);
                minZ = previewZ + 6.0 / 16.0;
                maxX = previewX + 10.0 / 16.0;
                maxY = previewY + (shortest ? 1.0 : 1.0 + 6.0 / 16.0);
                maxZ = previewZ + 10.0 / 16.0;
                break;
            case NORTH:
                minX = previewX + 6.0 / 16.0;
                minY = previewY + 6.0 / 16.0;
                minZ = previewZ + (shortest ? 0.0 : -1.0 + 10.0 / 16.0);
                maxX = previewX + 10.0 / 16.0;
                maxY = previewY + 10.0 / 16.0;
                maxZ = previewZ + (shortest ? 10.0 / 16.0 : 6.0 / 16.0);
                break;
            case SOUTH:
                minX = previewX + 6.0 / 16.0;
                minY = previewY + 6.0 / 16.0;
                minZ = previewZ + (shortest ? 6.0 / 16.0 : 10.0 / 16.0);
                maxX = previewX + 10.0 / 16.0;
                maxY = previewY + 10.0 / 16.0;
                maxZ = previewZ + (shortest ? 1.0 : 1.0 + 6.0 / 16.0);
                break;
            case WEST:
                minX = previewX + (shortest ? 0.0 : -1.0 + 10.0 / 16.0);
                minY = previewY + 6.0 / 16.0;
                minZ = previewZ + 6.0 / 16.0;
                maxX = previewX + (shortest ? 10.0 / 16.0 : 6.0 / 16.0);
                maxY = previewY + 10.0 / 16.0;
                maxZ = previewZ + 10.0 / 16.0;
                break;
            case EAST:
                minX = previewX + (shortest ? 6.0 / 16.0 : 10.0 / 16.0);
                minY = previewY + 6.0 / 16.0;
                minZ = previewZ + 6.0 / 16.0;
                maxX = previewX + (shortest ? 1.0 : 1.0 + 6.0 / 16.0);
                maxY = previewY + 10.0 / 16.0;
                maxZ = previewZ + 10.0 / 16.0;
                break;
            default:
                return;
        }

        renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
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
    // endregion
}
