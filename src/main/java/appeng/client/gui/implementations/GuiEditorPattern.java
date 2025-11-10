package appeng.client.gui.implementations;

import java.io.IOException;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import appeng.api.config.ActionItems;
import appeng.api.config.ItemSubstitution;
import appeng.api.config.PatternBeSubstitution;
import appeng.api.config.Settings;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.container.implementations.ContainerEditorPattern;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.parts.reporting.PartInterfaceTerminal;

public class GuiEditorPattern extends AEBaseGui {

    private static final int INVENTORY_STRING_Y_OFFSET = 101;
    private static final int PATTERN_STRING_Y_OFFSET = 179;
    private static final int SMALL_BUTTON_Y_OFFSET = 17;
    private static final int COLOR = 0x404040;

    private static final int TAB_CRAFT_BUTTON_X_OFFSET = 172;
    private static final int TAB_CRAFT_BUTTON_Y_OFFSET = 74;

    private static final int TAB_PROCESS_BUTTON_X_OFFSET = 172;
    private static final int TAB_PROCESS_BUTTON_Y_OFFSET = 74;

    private static final int SUBSTITUTIONS_BUTTON_X_OFFSET = 84;
    private static final int SUBSTITUTIONS_BUTTON_Y_OFFSET = 70 + SMALL_BUTTON_Y_OFFSET;

    private static final int BE_SUBSTITUTIONS_BUTTON_X_OFFSET = 84;
    private static final int BE_SUBSTITUTIONS_BUTTON_Y_OFFSET = 80 + SMALL_BUTTON_Y_OFFSET;

    private static final int CLEAR_BUTTON_X_OFFSET = 74;
    private static final int CLEAR_BUTTON_Y_OFFSET = 70 + SMALL_BUTTON_Y_OFFSET;

    private static final int DOUBLE_BUTTON_X_OFFSET = 74;
    private static final int DOUBLE_BUTTON_Y_OFFSET = 80 + SMALL_BUTTON_Y_OFFSET;

    private static final int ENCODE_BUTTON_X_OFFSET = 147;
    private static final int ENCODE_BUTTON_Y_OFFSET = 108;

    private final ContainerEditorPattern container;
    private GuiTabButton tabCraftButton;
    private GuiTabButton tabProcessButton;
    private GuiImgButton substitutionsEnabledBtn;
    private GuiImgButton substitutionsDisabledBtn;
    private GuiImgButton beSubstitutionsEnabledBtn;
    private GuiImgButton beSubstitutionsDisabledBtn;
    private GuiImgButton encodeBtn;
    private GuiImgButton clearBtn;
    private GuiImgButton doubleBtn;

    public GuiEditorPattern(InventoryPlayer ip, PartInterfaceTerminal host) {
        super(new ContainerEditorPattern(ip, host));
        this.container = (ContainerEditorPattern) this.inventorySlots;
        this.xSize = 256;
        this.ySize = 256;
    }

    @Override
    public void initGui() {
        super.initGui();

        this.tabCraftButton = new GuiTabButton(
                this.guiLeft + TAB_CRAFT_BUTTON_X_OFFSET,
                this.guiTop + TAB_CRAFT_BUTTON_Y_OFFSET,
                new ItemStack(Blocks.crafting_table),
                GuiText.CraftingPattern.getLocal(),
                itemRender);
        this.buttonList.add(this.tabCraftButton);

        this.tabProcessButton = new GuiTabButton(
                this.guiLeft + TAB_PROCESS_BUTTON_X_OFFSET,
                this.guiTop + TAB_PROCESS_BUTTON_Y_OFFSET,
                new ItemStack(Blocks.furnace),
                GuiText.ProcessingPattern.getLocal(),
                itemRender);
        this.buttonList.add(this.tabProcessButton);

        this.substitutionsEnabledBtn = new GuiImgButton(
                this.guiLeft + SUBSTITUTIONS_BUTTON_X_OFFSET,
                this.guiTop + SUBSTITUTIONS_BUTTON_Y_OFFSET,
                Settings.ACTIONS,
                ItemSubstitution.ENABLED);
        this.substitutionsEnabledBtn.setHalfSize(true);
        this.buttonList.add(this.substitutionsEnabledBtn);

        this.substitutionsDisabledBtn = new GuiImgButton(
                this.guiLeft + SUBSTITUTIONS_BUTTON_X_OFFSET,
                this.guiTop + SUBSTITUTIONS_BUTTON_Y_OFFSET,
                Settings.ACTIONS,
                ItemSubstitution.DISABLED);
        this.substitutionsDisabledBtn.setHalfSize(true);
        this.buttonList.add(this.substitutionsDisabledBtn);

        this.beSubstitutionsEnabledBtn = new GuiImgButton(
                this.guiLeft + BE_SUBSTITUTIONS_BUTTON_X_OFFSET,
                this.guiTop + BE_SUBSTITUTIONS_BUTTON_Y_OFFSET,
                Settings.ACTIONS,
                PatternBeSubstitution.ENABLED);
        this.beSubstitutionsEnabledBtn.setHalfSize(true);
        this.buttonList.add(this.beSubstitutionsEnabledBtn);

        this.beSubstitutionsDisabledBtn = new GuiImgButton(
                this.guiLeft + BE_SUBSTITUTIONS_BUTTON_X_OFFSET,
                this.guiTop + BE_SUBSTITUTIONS_BUTTON_Y_OFFSET,
                Settings.ACTIONS,
                PatternBeSubstitution.DISABLED);
        this.beSubstitutionsDisabledBtn.setHalfSize(true);
        this.buttonList.add(this.beSubstitutionsDisabledBtn);

        this.clearBtn = new GuiImgButton(
                this.guiLeft + CLEAR_BUTTON_X_OFFSET,
                this.guiTop + CLEAR_BUTTON_Y_OFFSET,
                Settings.ACTIONS,
                ActionItems.CLOSE);
        this.clearBtn.setHalfSize(true);
        this.buttonList.add(this.clearBtn);

        this.doubleBtn = new GuiImgButton(
                this.guiLeft + DOUBLE_BUTTON_X_OFFSET,
                this.guiTop + DOUBLE_BUTTON_Y_OFFSET,
                Settings.ACTIONS,
                ActionItems.DOUBLE);
        this.doubleBtn.setHalfSize(true);
        this.buttonList.add(this.doubleBtn);

        this.encodeBtn = new GuiImgButton(
                this.guiLeft + ENCODE_BUTTON_X_OFFSET,
                this.guiTop + ENCODE_BUTTON_Y_OFFSET,
                Settings.ACTIONS,
                ActionItems.ENCODE);
        this.buttonList.add(this.encodeBtn);

        updateButtonVisibility();
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        super.actionPerformed(btn);

        try {
            if (this.tabProcessButton == btn) {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("PatternEditor.CraftMode", "true"));
            } else if (this.tabCraftButton == btn) {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("PatternEditor.CraftMode", "false"));
            } else if (this.encodeBtn == btn) {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("PatternEditor.Encode", "1"));
            } else if (this.clearBtn == btn) {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("PatternEditor.Clear", "1"));
            } else if (this.substitutionsEnabledBtn == btn || this.substitutionsDisabledBtn == btn) {
                NetworkHandler.instance.sendToServer(
                        new PacketValueConfig(
                                "PatternEditor.Substitute",
                                this.container.substitute ? "false" : "true"));
            } else if (this.beSubstitutionsEnabledBtn == btn || this.beSubstitutionsDisabledBtn == btn) {
                NetworkHandler.instance.sendToServer(
                        new PacketValueConfig(
                                "PatternEditor.BeSubstitute",
                                this.container.beSubstitute ? "false" : "true"));
            } else if (doubleBtn == btn && !this.container.isCraftingMode()) {
                int val = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ? 1 : 0;
                if (Mouse.isButtonDown(1)) val |= 0b10;
                NetworkHandler.instance
                        .sendToServer(new PacketValueConfig("PatternEditor.Double", String.valueOf(val)));
            }
        } catch (final IOException e) {}
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        fontRendererObj.drawString("Pattern Editor", 8, this.ySize - PATTERN_STRING_Y_OFFSET, COLOR);
        fontRendererObj.drawString(GuiText.inventory.getLocal(), 8, this.ySize - INVENTORY_STRING_Y_OFFSET, COLOR);
    }

    private void updateButtonVisibility() {
        boolean isCraftingMode = this.container.isCraftingMode();

        this.tabCraftButton.visible = isCraftingMode;
        this.tabProcessButton.visible = !isCraftingMode;
        this.doubleBtn.visible = !isCraftingMode;

        this.substitutionsEnabledBtn.visible = this.container.substitute;
        this.substitutionsDisabledBtn.visible = !this.container.substitute;
        this.beSubstitutionsEnabledBtn.visible = this.container.beSubstitute;
        this.beSubstitutionsDisabledBtn.visible = !this.container.beSubstitute;
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float btn) {
        this.updateButtonVisibility();
        super.drawScreen(mouseX, mouseY, btn);
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        boolean isCraftingMode = this.container.isCraftingMode();
        bindTexture(isCraftingMode ? "guis/pattern_editor_crafting.png" : "guis/pattern_editor_processing_small.png");
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
}
