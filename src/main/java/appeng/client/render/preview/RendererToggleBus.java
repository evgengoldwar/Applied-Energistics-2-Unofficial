package appeng.client.render.preview;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import appeng.client.render.previewBlocks.ViewHelper;
import appeng.parts.misc.PartToggleBus;

public class RendererToggleBus extends AbstractRendererPreview implements IRenderPreview {

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
            int toggleBusX = previewX + placementSide.offsetX;
            int toggleBusY = previewY + placementSide.offsetY;
            int toggleBusZ = previewZ + placementSide.offsetZ;
            applySideRotation(toggleBusX, toggleBusY, toggleBusZ, placementSide.getOpposite());
        } else {
            applySideRotation(previewX, previewY, previewZ, placementSide);
        }

        renderToggleBusBase();
        renderToggleBusConnector();

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    @Override
    public List<Class<?>> validItemClass() {
        return HelperRendererView.getValidClasses(PartToggleBus.class);
    }

    private static void renderToggleBusBase() {
        double minX = 6.0 / 16.0;
        double minY = 6.0 / 16.0;
        double minZ = 11.0 / 16.0;
        double maxX = 10.0 / 16.0;
        double maxY = 10.0 / 16.0;
        double maxZ = 1.0;

        ViewHelper.renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void renderToggleBusConnector() {
        double minX = 6.0 / 16.0;
        double minY = 6.0 / 16.0;
        double minZ = 14.0 / 16.0;
        double maxX = 10.0 / 16.0;
        double maxY = 10.0 / 16.0;
        double maxZ = 1.0;

        ViewHelper.renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
