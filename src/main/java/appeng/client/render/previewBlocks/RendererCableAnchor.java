package appeng.client.render.previewBlocks;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import appeng.parts.misc.PartCableAnchor;

public class RendererCableAnchor extends AbstractRendererPreview implements IRenderPreview {

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
        ForgeDirection placementSide = ViewHelper.getPlacementSide();

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

        if (shouldPlaceOnNeighborBlock) {
            int anchorX = previewX + placementSide.offsetX;
            int anchorY = previewY + placementSide.offsetY;
            int anchorZ = previewZ + placementSide.offsetZ;
            applySideRotation(anchorX, anchorY, anchorZ, placementSide.getOpposite());
        } else {
            applySideRotation(previewX, previewY, previewZ, placementSide);
        }

        renderCableAnchorBase();

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private void renderCableAnchorBase() {
        double minX = 7.0 / 16.0;
        double minY = 7.0 / 16.0;
        double minZ = 10.0 / 16.0;
        double maxX = 9.0 / 16.0;
        double maxY = 9.0 / 16.0;
        double maxZ = 1.0;

        renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public List<Class<?>> validItemClass() {
        return ViewHelper.getValidClasses(PartCableAnchor.class);
    }
}
