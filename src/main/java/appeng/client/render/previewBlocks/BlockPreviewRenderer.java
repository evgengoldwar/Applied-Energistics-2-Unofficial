package appeng.client.render.previewBlocks;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class BlockPreviewRenderer {

    private static BlockPreviewRenderer instance;

    public static BlockPreviewRenderer getInstance() {
        if (instance == null) {
            instance = new BlockPreviewRenderer();
        }
        return instance;
    }

    private ItemStack currentItem;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return;

        ItemStack heldItem = player.getHeldItem();
        if (heldItem == null) return;
        currentItem = heldItem;

        ViewHelper.setPlayer(player);
        ViewHelper.updatePreview(player);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        ViewHelper.updatePartialTicks(event.partialTicks);
        ViewHelper.handleItem(currentItem);
    }
}
