package appeng.client.render.preview;

import appeng.parts.networking.PartQuartzFiber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.opengl.GL11;

import java.util.List;


public class RendererQuartzFiber extends AbstractRendererPreview implements IRenderPreview{
    @Override
    public void renderPreview() {
        EntityPlayer player = HelperRendererView.getPlayer();
        if (player == null) return;

        double playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * HelperRendererView.getCurrentPartialTicks();
        double playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * HelperRendererView.getCurrentPartialTicks();
        double playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * HelperRendererView.getCurrentPartialTicks();

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
            int fiberX = previewX + placementSide.offsetX;
            int fiberY = previewY + placementSide.offsetY;
            int fiberZ = previewZ + placementSide.offsetZ;
            applySideRotation(fiberX, fiberY, fiberZ, placementSide.getOpposite());
        } else {
            applySideRotation(previewX, previewY, previewZ, placementSide);
        }

        renderQuartzFiberSolid();

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    @Override
    public List<Class<?>> validItemClass() {
        return HelperRendererView.getValidClasses(PartQuartzFiber.class);
    }

    private void renderQuartzFiberSolid() {
        double minX = 6.0 / 16.0;
        double minY = 6.0 / 16.0;
        double minZ = 10.0 / 16.0;
        double maxX = 10.0 / 16.0;
        double maxY = 10.0 / 16.0;
        double maxZ = 1.0;

        renderSolidCube(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void renderSolidCube(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        Tessellator tessellator = Tessellator.instance;

        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        GL11.glLineWidth(2.0f);

        tessellator.startDrawing(GL11.GL_QUADS);

        tessellator.addVertex(minX, minY, minZ);
        tessellator.addVertex(maxX, minY, minZ);
        tessellator.addVertex(maxX, minY, maxZ);
        tessellator.addVertex(minX, minY, maxZ);

        tessellator.addVertex(minX, maxY, minZ);
        tessellator.addVertex(minX, maxY, maxZ);
        tessellator.addVertex(maxX, maxY, maxZ);
        tessellator.addVertex(maxX, maxY, minZ);

        tessellator.addVertex(minX, minY, minZ);
        tessellator.addVertex(minX, maxY, minZ);
        tessellator.addVertex(maxX, maxY, minZ);
        tessellator.addVertex(maxX, minY, minZ);

        tessellator.addVertex(minX, minY, maxZ);
        tessellator.addVertex(maxX, minY, maxZ);
        tessellator.addVertex(maxX, maxY, maxZ);
        tessellator.addVertex(minX, maxY, maxZ);

        tessellator.addVertex(minX, minY, minZ);
        tessellator.addVertex(minX, minY, maxZ);
        tessellator.addVertex(minX, maxY, maxZ);
        tessellator.addVertex(minX, maxY, minZ);

        tessellator.addVertex(maxX, minY, minZ);
        tessellator.addVertex(maxX, maxY, minZ);
        tessellator.addVertex(maxX, maxY, maxZ);
        tessellator.addVertex(maxX, minY, maxZ);

        tessellator.draw();

        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
    }
}
