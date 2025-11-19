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
        if (heldItem == null) {
            currentItem = null;
            ViewHelper.clearCache();
            return;
        }

        if (currentItem == null || !areItemStacksEqual(currentItem, heldItem)) {
            currentItem = heldItem;
            ViewHelper.clearCache();
        }

        ViewHelper.setPlayer(player);
        ViewHelper.updatePreview(player);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (currentItem == null) return;
        ViewHelper.updatePartialTicks(event.partialTicks);
        ViewHelper.handleItem(currentItem);
    }

    private static boolean areItemStacksEqual(ItemStack stack1, ItemStack stack2) {
        if (stack1 == stack2) return true;
        if (stack1 == null || stack2 == null) return false;
        return ItemStack.areItemStacksEqual(stack1, stack2) && ItemStack.areItemStackTagsEqual(stack1, stack2)
                && stack1.stackSize == stack2.stackSize;
    }
}
