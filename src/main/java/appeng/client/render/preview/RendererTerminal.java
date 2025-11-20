package appeng.client.render.preview;

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

import appeng.client.render.previewBlocks.ViewHelper;
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

    @Override
    public List<Class<?>> validItemClass() {
        return HelperRendererView.getValidClasses(
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

    private static void renderTerminalBase() {
        double minX = 2.0 / 16.0;
        double minY = 2.0 / 16.0;
        double minZ = 14.0 / 16.0;
        double maxX = 14.0 / 16.0;
        double maxY = 14.0 / 16.0;
        double maxZ = 1.0;

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
}
