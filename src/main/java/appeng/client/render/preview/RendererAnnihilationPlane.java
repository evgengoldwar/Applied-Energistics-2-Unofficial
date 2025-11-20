package appeng.client.render.preview;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import appeng.parts.automation.PartAnnihilationPlane;
import appeng.parts.automation.PartFormationPlane;

public class RendererAnnihilationPlane extends AbstractRendererPreview implements IRenderPreview {

    @Override
    public void renderPreview() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
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
            int planeX = previewX + placementSide.offsetX;
            int planeY = previewY + placementSide.offsetY;
            int planeZ = previewZ + placementSide.offsetZ;
            applySideRotation(planeX, planeY, planeZ, placementSide.getOpposite());
        } else {
            applySideRotation(previewX, previewY, previewZ, placementSide);
        }

        renderAnnihilationPlaneBase();
        renderAnnihilationPlaneConnector();

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    @Override
    public List<Class<?>> validItemClass() {
        return HelperRendererView.getValidClasses(PartAnnihilationPlane.class, PartFormationPlane.class);
    }

    private void renderAnnihilationPlaneBase() {
        double minX = 1.0 / 16.0;
        double minY = 1.0 / 16.0;
        double minZ = 15.0 / 16.0;
        double maxX = 15.0 / 16.0;
        double maxY = 15.0 / 16.0;
        double maxZ = 1.0;

        renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void renderAnnihilationPlaneConnector() {
        double minX = 5.0 / 16.0;
        double minY = 5.0 / 16.0;
        double minZ = 14.0 / 16.0;
        double maxX = 11.0 / 16.0;
        double maxY = 11.0 / 16.0;
        double maxZ = 15.0 / 16.0;

        renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
