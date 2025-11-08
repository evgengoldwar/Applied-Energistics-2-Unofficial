package appeng.client.gui.implementations;

import appeng.api.config.ActionItems;
import appeng.api.config.ItemSubstitution;
import appeng.api.config.PatternBeSubstitution;
import appeng.api.config.Settings;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.parts.reporting.PartInterfaceTerminal;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;

import appeng.client.gui.AEBaseGui;
import appeng.container.implementations.ContainerEditorPattern;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;

import java.io.IOException;

public class GuiEditorPattern extends AEBaseGui {

    private static final int VANILLA_Y_OFFSET = 167;
    private static final int INVENTORY_STRING_Y_OFFSET = 100;
    private static final int PATTERN_STRING_Y_OFFSET = 182;

    private static final String SUBSITUTION_DISABLE = "0";
    private static final String SUBSITUTION_ENABLE = "1";

    private static final String CRAFTMODE_CRAFTING = "1";
    private static final String CRAFTMODE_PROCESSING = "0";

    private final ContainerEditorPattern container;

    private GuiTabButton tabCraftButton;
    private GuiTabButton tabProcessButton;
    private GuiImgButton substitutionsEnabledBtn;
    private GuiImgButton substitutionsDisabledBtn;
    private GuiImgButton beSubstitutionsEnabledBtn;
    private GuiImgButton beSubstitutionsDisabledBtn;
    private GuiImgButton encodeBtn;
    private GuiImgButton clearBtn;

    public GuiEditorPattern(InventoryPlayer ip, PartInterfaceTerminal host) {
        super(new ContainerEditorPattern(ip, host));
        this.container = (ContainerEditorPattern) this.inventorySlots;
        this.xSize = 256;
        this.ySize = 256;
    }

    @Override
    public void initGui() {
        super.initGui();

        settingsVanillaGui();

//        this.tabCraftButton = new GuiTabButton(
//                this.guiLeft + 173,
//                this.guiTop + this.ySize - 177,
//                new ItemStack(Blocks.crafting_table),
//                GuiText.CraftingPattern.getLocal(),
//                itemRender);
//        this.buttonList.add(this.tabCraftButton);
//
//        this.tabProcessButton = new GuiTabButton(
//                this.guiLeft + 173,
//                this.guiTop + this.ySize - 177,
//                new ItemStack(Blocks.furnace),
//                GuiText.ProcessingPattern.getLocal(),
//                itemRender);
//        this.buttonList.add(this.tabProcessButton);
//
//        this.substitutionsEnabledBtn = new GuiImgButton(
//                this.guiLeft + 84,
//                this.guiTop + this.ySize - 163,
//                Settings.ACTIONS,
//                ItemSubstitution.ENABLED);
//        this.substitutionsEnabledBtn.setHalfSize(true);
//        this.buttonList.add(this.substitutionsEnabledBtn);
//
//        this.substitutionsDisabledBtn = new GuiImgButton(
//                this.guiLeft + 84,
//                this.guiTop + this.ySize - 163,
//                Settings.ACTIONS,
//                ItemSubstitution.DISABLED);
//        this.substitutionsDisabledBtn.setHalfSize(true);
//        this.buttonList.add(this.substitutionsDisabledBtn);
//
//        this.beSubstitutionsEnabledBtn = new GuiImgButton(
//                this.guiLeft + 84,
//                this.guiTop + this.ySize - 153,
//                Settings.ACTIONS,
//                PatternBeSubstitution.ENABLED);
//        this.beSubstitutionsEnabledBtn.setHalfSize(true);
//        this.buttonList.add(this.beSubstitutionsEnabledBtn);
//
//        this.beSubstitutionsDisabledBtn = new GuiImgButton(
//                this.guiLeft + 84,
//                this.guiTop + this.ySize - 153,
//                Settings.ACTIONS,
//                PatternBeSubstitution.DISABLED);
//        this.beSubstitutionsDisabledBtn.setHalfSize(true);
//        this.buttonList.add(this.beSubstitutionsDisabledBtn);
//
//        this.clearBtn = new GuiImgButton(
//                this.guiLeft + 74,
//                this.guiTop + this.ySize - 163,
//                Settings.ACTIONS,
//                ActionItems.CLOSE);
//        this.clearBtn.setHalfSize(true);
//        this.buttonList.add(this.clearBtn);
//
//        this.encodeBtn = new GuiImgButton(
//                this.guiLeft + 147,
//                this.guiTop + this.ySize - 142,
//                Settings.ACTIONS,
//                ActionItems.ENCODE);
//        this.buttonList.add(this.encodeBtn);

//        this.updateButtonVisibility();
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        super.actionPerformed(btn);

        try {
            if (this.tabCraftButton == btn || this.tabProcessButton == btn) {
                NetworkHandler.instance.sendToServer(
                        new PacketValueConfig(
                                "PatternEditor.CraftMode",
                                this.tabProcessButton == btn ? CRAFTMODE_CRAFTING : CRAFTMODE_PROCESSING));
            } else if (this.encodeBtn == btn) {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("PatternEditor.Encode", "1"));
                // Закрываем GUI после сохранения
                this.mc.thePlayer.closeScreen();
            } else if (this.clearBtn == btn) {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("PatternEditor.Clear", "1"));
            } else if (this.substitutionsEnabledBtn == btn || this.substitutionsDisabledBtn == btn) {
                NetworkHandler.instance.sendToServer(
                        new PacketValueConfig(
                                "PatternEditor.Substitute",
                                this.substitutionsEnabledBtn == btn ? SUBSITUTION_DISABLE : SUBSITUTION_ENABLE));
            } else if (this.beSubstitutionsEnabledBtn == btn || this.beSubstitutionsDisabledBtn == btn) {
                NetworkHandler.instance.sendToServer(
                        new PacketValueConfig(
                                "PatternEditor.BeSubstitute",
                                this.beSubstitutionsEnabledBtn == btn ? SUBSITUTION_DISABLE : SUBSITUTION_ENABLE));
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        fontRendererObj.drawString(
                "Pattern Editor",
                8,
                this.ySize - PATTERN_STRING_Y_OFFSET,
                0);

        fontRendererObj.drawString(
                GuiText.inventory.getLocal(),
                8,
                this.ySize - INVENTORY_STRING_Y_OFFSET,
                0);

//        ContainerEditorPattern container = (ContainerEditorPattern) this.inventorySlots;
//        if (container.getPatternDetails() != null) {
//            String mode = container.isCraftingMode() ?
//                    GuiText.CraftingPattern.getLocal() : GuiText.ProcessingPattern.getLocal();
//            fontRendererObj.drawString(mode, 120, 6, 0);
//        }
    }

    private void updateButtonVisibility() {
        if (!this.container.isCraftingMode()) {
            this.tabCraftButton.visible = false;
            this.tabProcessButton.visible = true;
        } else {
            this.tabCraftButton.visible = true;
            this.tabProcessButton.visible = false;
        }

        if (this.container.substitute) {
            this.substitutionsEnabledBtn.visible = true;
            this.substitutionsDisabledBtn.visible = false;
        } else {
            this.substitutionsEnabledBtn.visible = false;
            this.substitutionsDisabledBtn.visible = true;
        }

        this.beSubstitutionsEnabledBtn.visible = this.container.beSubstitute;
        this.beSubstitutionsDisabledBtn.visible = !this.container.beSubstitute;
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float btn) {
//        this.updateButtonVisibility();
        super.drawScreen(mouseX, mouseY, btn);
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        bindTexture("guis/pattern.png");
        drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }

    @Override
    protected void keyTyped(final char character, final int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            this.mc.thePlayer.closeScreen();
        } else {
            super.keyTyped(character, key);
        }
    }

    private void settingsVanillaGui() {
        for (Slot slot : this.inventorySlots.inventorySlots) {
            if (slot.inventory instanceof InventoryPlayer) {
                slot.yDisplayPosition += VANILLA_Y_OFFSET;
            }
        }
    }
}