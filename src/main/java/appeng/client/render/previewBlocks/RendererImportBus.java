package appeng.client.render.previewBlocks;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.glodblock.github.common.parts.PartFluidImportBus;

import appeng.parts.automation.PartImportBus;

public class RendererImportBus extends AbstractRendererPreview implements IRenderPreview {

    @Override
    public void renderPreview() {
        EntityPlayer player = ViewHelper.getPlayer();
        if (player == null) return;

        double playerX = player.lastTickPosX
                + (player.posX - player.lastTickPosX) * ViewHelper.getCurrentPartialTicks();
        double playerY = player.lastTickPosY
                + (player.posY - player.lastTickPosY) * ViewHelper.getCurrentPartialTicks();
        double playerZ = player.lastTickPosZ
                + (player.posZ - player.lastTickPosZ) * ViewHelper.getCurrentPartialTicks();

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
        int previewX = ViewHelper.getPreviewX();
        int previewY = ViewHelper.getPreviewY();
        int previewZ = ViewHelper.getPreviewZ();
        ForgeDirection placementSide = ViewHelper.getPlacementSide();

        if (shouldPlaceOnNeighborBlock) {
            int importBusX = previewX + placementSide.offsetX;
            int importBusY = previewY + placementSide.offsetY;
            int importBusZ = previewZ + placementSide.offsetZ;
            applySideRotation(importBusX, importBusY, importBusZ, placementSide.getOpposite());
        } else {
            applySideRotation(previewX, previewY, previewZ, placementSide);
        }

        renderImportBusBase();
        renderImportBusMiddle();
        renderImportBusConnector();

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    @Override
    public List<Class<?>> validItemClass() {
        return ViewHelper.getValidClasses(PartImportBus.class, PartFluidImportBus.class);
    }

    private void renderImportBusBase() {
        double minX = 4.0 / 16.0;
        double minY = 4.0 / 16.0;
        double minZ = 14.0 / 16.0;
        double maxX = 12.0 / 16.0;
        double maxY = 12.0 / 16.0;
        double maxZ = 1.0;

        renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void renderImportBusMiddle() {
        double minX = 5.0 / 16.0;
        double minY = 5.0 / 16.0;
        double minZ = 13.0 / 16.0;
        double maxX = 11.0 / 16.0;
        double maxY = 11.0 / 16.0;
        double maxZ = 14.0 / 16.0;

        renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void renderImportBusConnector() {
        double minX = 6.0 / 16.0;
        double minY = 6.0 / 16.0;
        double minZ = 11.0 / 16.0;
        double maxX = 10.0 / 16.0;
        double maxY = 10.0 / 16.0;
        double maxZ = 13.0 / 16.0;

        renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
