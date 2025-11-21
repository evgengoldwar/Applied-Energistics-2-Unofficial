package appeng.client.render.previewBlocks;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.glodblock.github.common.parts.PartFluidInterface;
import com.glodblock.github.common.parts.PartFluidPatternTerminal;
import com.glodblock.github.common.parts.PartFluidPatternTerminalEx;
import com.glodblock.github.common.parts.PartFluidStorageBus;
import com.glodblock.github.common.parts.PartFluidTerminal;
import com.glodblock.github.common.parts.PartLevelTerminal;

import appeng.parts.misc.PartInterface;
import appeng.parts.misc.PartStorageBus;
import appeng.parts.p2p.PartP2PTunnel;
import appeng.parts.reporting.AbstractPartDisplay;
import appeng.parts.reporting.PartDarkPanel;
import appeng.parts.reporting.PartPanel;
import appeng.parts.reporting.PartSemiDarkPanel;

public class RendererTerminal extends AbstractRendererPreview implements IRenderPreview {

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
            int terminalX = previewX + placementSide.offsetX;
            int terminalY = previewY + placementSide.offsetY;
            int terminalZ = previewZ + placementSide.offsetZ;
            applySideRotation(terminalX, terminalY, terminalZ, placementSide.getOpposite());
        } else {
            applySideRotation(previewX, previewY, previewZ, placementSide);
        }

        renderBase(2.0, 2.0, 14.0, 14.0, 14.0, 16.0);
        renderBase(4.0, 4.0, 13.0, 12.0, 12.0, 14.0);

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    @Override
    public List<Class<?>> validItemClass() {
        return ViewHelper.getValidClasses(
                AbstractPartDisplay.class,
                PartFluidTerminal.class,
                PartP2PTunnel.class,
                PartPanel.class,
                PartSemiDarkPanel.class,
                PartDarkPanel.class,
                PartStorageBus.class,
                PartFluidStorageBus.class,
                PartInterface.class,
                PartFluidInterface.class,
                PartFluidPatternTerminal.class,
                PartFluidPatternTerminalEx.class,
                PartLevelTerminal.class);
    }
}
