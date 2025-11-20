package appeng.client.render.preview;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class BlockRendererPreview {

    private static BlockRendererPreview instance;

    public static BlockRendererPreview getInstance() {
        if (instance == null) {
            instance = new BlockRendererPreview();
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
            HelperRendererView.clearCache();
            return;
        }

        if (currentItem == null || !areItemStacksEqual(currentItem, heldItem)) {
            currentItem = heldItem;
            HelperRendererView.clearCache();
        }

        HelperRendererView.setPlayer(player);
        HelperRendererView.setCachedItemStack(currentItem);
        HelperRendererView.updatePreview(player);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (currentItem == null) return;
        HelperRendererView.setCurrentPartialTicks(event.partialTicks);
        HelperRendererView.handleItem(currentItem);
    }

    private static boolean areItemStacksEqual(ItemStack stack1, ItemStack stack2) {
        if (stack1 == stack2) return true;
        if (stack1 == null || stack2 == null) return false;
        return ItemStack.areItemStacksEqual(stack1, stack2) && ItemStack.areItemStackTagsEqual(stack1, stack2)
                && stack1.stackSize == stack2.stackSize;
    }
}
