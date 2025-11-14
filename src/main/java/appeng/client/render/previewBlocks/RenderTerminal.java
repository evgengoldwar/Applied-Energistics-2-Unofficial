package appeng.client.render.previewBlocks;

import static appeng.client.render.previewBlocks.ViewHelper.*;

import appeng.api.implementations.parts.IPartCable;
import appeng.api.parts.BusSupport;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.parts.networking.PartCable;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

public class RenderTerminal {

    public static void renderTerminalPreview() {
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
            int terminalX = previewX + placementSide.offsetX;
            int terminalY = previewY + placementSide.offsetY;
            int terminalZ = previewZ + placementSide.offsetZ;
            applySideRotation(terminalX, terminalY, terminalZ, placementSide.getOpposite());
        } else {
            applySideRotation(previewX, previewY, previewZ, placementSide);
        }

        renderTerminalBase();
        renderTerminalDisplayFrame();

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

    private static void renderTerminalBase() {
        double minX = 2.0 / 16.0;
        double minY = 2.0 / 16.0;
        double minZ = 14.0 / 16.0;
        double maxX = 14.0 / 16.0;
        double maxY = 14.0 / 16.0;
        double maxZ = 16.0 / 16.0;

        ViewHelper.renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void renderTerminalDisplayFrame() {
        double minX = 4.0 / 16.0;
        double minY = 4.0 / 16.0;
        double minZ = 13.0 / 16.0;
        double maxX = 12.0 / 16.0;
        double maxY = 12.0 / 16.0;
        double maxZ = 14.0 / 16.0;

        ViewHelper.renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static boolean canPlacePartHost(World world, ForgeDirection side, int x, int y, int z) {
        int neighborX = x + side.offsetX;
        int neighborY = y + side.offsetY;
        int neighborZ = z + side.offsetZ;

        TileEntity te = world.getTileEntity(x, y, z);
        TileEntity neighborTe = world.getTileEntity(neighborX, neighborY, neighborZ);
        boolean canPlaceOnNeighbor = canPlaceBlockAt(world, neighborX, neighborY, neighborZ);

        if (!shouldPlaceOnNeighborBlock() && checkTe(te, side, canPlaceOnNeighbor)) {
            return true;
        }

        return checkTe(neighborTe, side, canPlaceOnNeighbor);
    }

    private static boolean checkTe(TileEntity te, ForgeDirection side, boolean canPlaceOnNeighbor) {
        if (!(te instanceof IPartHost partHost)) {
            return canPlaceOnNeighbor;
        }

        if (partHost.getPart(side) != null && !shouldPlaceOnNeighborBlock()) {
            return false;
        }

        if (partHost.getPart(side.getOpposite()) != null) {
            return false;
        }

        IPart centerPart = partHost.getPart(ForgeDirection.UNKNOWN);
        if (centerPart instanceof IPartCable cable) {
            return cable.supportsBuses() == BusSupport.CABLE;
        }

        return hasParts(partHost);
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
}
