package appeng.client.render.previewBlocks;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.glodblock.github.common.parts.PartFluidExportBus;

import appeng.parts.automation.PartExportBus;

public class RendererExportBus extends AbstractRendererPreview implements IRenderPreview {

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
            int exportBusX = previewX + placementSide.offsetX;
            int exportBusY = previewY + placementSide.offsetY;
            int exportBusZ = previewZ + placementSide.offsetZ;
            applySideRotation(exportBusX, exportBusY, exportBusZ, placementSide.getOpposite());
        } else {
            applySideRotation(previewX, previewY, previewZ, placementSide);
        }

        renderBase(4.0, 4.0, 12.0, 12.0, 12.0, 14.0);
        renderBase(5.0, 5.0, 14.0, 11.0, 11.0, 15.0);
        renderBase(6.0, 6.0, 15.0, 10.0, 10.0, 16.0);
        renderBase(6.0, 6.0, 11.0, 10.0, 10.0, 12.0);

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    @Override
    public List<Class<?>> validItemClass() {
        return ViewHelper.getValidClasses(PartExportBus.class, PartFluidExportBus.class);
    }
}
