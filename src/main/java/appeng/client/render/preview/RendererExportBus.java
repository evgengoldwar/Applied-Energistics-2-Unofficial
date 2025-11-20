package appeng.client.render.preview;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.glodblock.github.common.parts.PartFluidExportBus;

import appeng.parts.automation.PartExportBus;

public class RendererExportBus extends AbstractRendererPreview implements IRenderPreview {

    @Override
    public void renderPreview() {
        EntityPlayer player = HelperRendererView.getPlayer();
        if (player == null) return;

        double playerX = player.lastTickPosX
                + (player.posX - player.lastTickPosX) * HelperRendererView.getCurrentPartialTicks();
        double playerY = player.lastTickPosY
                + (player.posY - player.lastTickPosY) * HelperRendererView.getCurrentPartialTicks();
        double playerZ = player.lastTickPosZ
                + (player.posZ - player.lastTickPosZ) * HelperRendererView.getCurrentPartialTicks();

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
        int previewX = HelperRendererView.getPreviewX();
        int previewY = HelperRendererView.getPreviewY();
        int previewZ = HelperRendererView.getPreviewZ();
        ForgeDirection placementSide = HelperRendererView.getPlacementSide();

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

    @Override
    public List<Class<?>> validItemClass() {
        return HelperRendererView.getValidClasses(PartExportBus.class, PartFluidExportBus.class);
    }

    private void renderExportBusBase() {
        double minX = 4.0 / 16.0;
        double minY = 4.0 / 16.0;
        double minZ = 12.0 / 16.0;
        double maxX = 12.0 / 16.0;
        double maxY = 12.0 / 16.0;
        double maxZ = 14.0 / 16.0;

        renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void renderExportBusMiddle() {
        double minX = 5.0 / 16.0;
        double minY = 5.0 / 16.0;
        double minZ = 14.0 / 16.0;
        double maxX = 11.0 / 16.0;
        double maxY = 11.0 / 16.0;
        double maxZ = 15.0 / 16.0;

        renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void renderExportBusConnector() {
        double minX = 6.0 / 16.0;
        double minY = 6.0 / 16.0;
        double minZ = 15.0 / 16.0;
        double maxX = 10.0 / 16.0;
        double maxY = 10.0 / 16.0;
        double maxZ = 1.0;

        renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void renderExportBusFront() {
        double minX = 6.0 / 16.0;
        double minY = 6.0 / 16.0;
        double minZ = 11.0 / 16.0;
        double maxX = 10.0 / 16.0;
        double maxY = 10.0 / 16.0;
        double maxZ = 12.0 / 16.0;

        renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
