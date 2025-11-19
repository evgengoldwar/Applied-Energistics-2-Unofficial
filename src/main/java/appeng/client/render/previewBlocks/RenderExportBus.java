package appeng.client.render.previewBlocks;

import static appeng.client.render.previewBlocks.ViewHelper.*;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import appeng.api.parts.BusSupport;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.parts.networking.PartCable;

public class RenderExportBus {

    public static void renderExportBusPreview() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
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

        getValidColorGL11();

        boolean shouldPlaceOnNeighborBlock = shouldPlaceOnNeighborBlock();

        if (shouldPlaceOnNeighborBlock) {
            int exportBusX = previewX + placementSide.offsetX;
            int exportBusY = previewY + placementSide.offsetY;
            int exportBusZ = previewZ + placementSide.offsetZ;
            applySideRotation(exportBusX, exportBusY, exportBusZ, placementSide.getOpposite());
        } else {
            applySideRotation(previewX, previewY, previewZ, placementSide);
        }

        renderExportBusBase();
        renderExportBusMiddle();
        renderExportBusConnector();
        renderExportBusFront();

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private static void applySideRotation(double x, double y, double z, ForgeDirection side) {
        GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);

        switch (side) {
            case DOWN:
                GL11.glRotatef(90, 1, 0, 0);
                break;
            case UP:
                GL11.glRotatef(-90, 1, 0, 0);
                break;
            case NORTH:
                GL11.glRotatef(180, 0, 1, 0);
                break;
            case SOUTH:
                break;
            case WEST:
                GL11.glRotatef(-90, 0, 1, 0);
                break;
            case EAST:
                GL11.glRotatef(90, 0, 1, 0);
                break;
        }

        GL11.glTranslated(-0.5, -0.5, -0.5);
    }

    private static void renderExportBusBase() {
        double minX = 4.0 / 16.0;
        double minY = 4.0 / 16.0;
        double minZ = 12.0 / 16.0;
        double maxX = 12.0 / 16.0;
        double maxY = 12.0 / 16.0;
        double maxZ = 14.0 / 16.0;

        ViewHelper.renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void renderExportBusMiddle() {
        double minX = 5.0 / 16.0;
        double minY = 5.0 / 16.0;
        double minZ = 14.0 / 16.0;
        double maxX = 11.0 / 16.0;
        double maxY = 11.0 / 16.0;
        double maxZ = 15.0 / 16.0;

        ViewHelper.renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void renderExportBusConnector() {
        double minX = 6.0 / 16.0;
        double minY = 6.0 / 16.0;
        double minZ = 15.0 / 16.0;
        double maxX = 10.0 / 16.0;
        double maxY = 10.0 / 16.0;
        double maxZ = 16.0 / 16.0;

        ViewHelper.renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void renderExportBusFront() {
        double minX = 6.0 / 16.0;
        double minY = 6.0 / 16.0;
        double minZ = 11.0 / 16.0;
        double maxX = 10.0 / 16.0;
        double maxY = 10.0 / 16.0;
        double maxZ = 12.0 / 16.0;

        ViewHelper.renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static boolean shouldPlaceOnNeighborBlock() {
        TileEntity te = getWorld().getTileEntity(previewX, previewY, previewZ);

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

        return !hasParts(partHost);
    }
}
