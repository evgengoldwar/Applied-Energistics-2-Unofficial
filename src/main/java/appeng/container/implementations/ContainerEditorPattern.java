package appeng.container.implementations;

import java.util.ArrayList;
import java.util.List;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.IOptionalSlotHost;
import appeng.container.slot.OptionalSlotFake;
import appeng.container.slot.SlotFake;
import appeng.container.slot.SlotFakeCraftingMatrix;
import appeng.container.slot.SlotInaccessible;
import appeng.parts.reporting.PartInterfaceTerminal;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.BiggerAppEngInventory;
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.tile.inventory.InvOperation;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;

public class ContainerEditorPattern extends AEBaseContainer implements IOptionalSlotHost, IAEAppEngInventory {

    private static final int VANILLA_Y_OFFSET = 167;
    private static final int VANILLA_SLOT_SIZE = 18;

    private static final int CRAFTING_SLOTS_OFFSET_X = 18;
    private static final int CRAFTING_SLOTS_OFFSET_Y = 93;

    private static final int CRAFTING_OUTPUT_SLOT_OFFSET_X_SMALL = 110;
    private static final int CRAFTING_OUTPUT_SLOT_OFFSET_Y_SMALL = 111;

    private static final int OUTPUT_SLOTS_OFFSET_X_SMALL = 110;
    private static final int OUTPUT_SLOTS_OFFSET_Y_SMALL = 93;

    private static final int PATTERN_SLOTS_OFFSET_X_SMALL = 147;
    private static final int PATTERN_SLOTS_OFFSET_Y_SMALL = 131;

    private static final int INPUT_SLOTS_OFFSET_X_SMALL = 18;
    private static final int INPUT_SLOTS_OFFSET_Y_SMALL = 93;

    private final Slot patternValue;
    private final ItemStack[] savedCraftingInputs = new ItemStack[9];
    private final ItemStack[] savedCraftingOutputs = new ItemStack[1];
    private final ItemStack[] savedProcessingInputs = new ItemStack[9];
    private final ItemStack[] savedProcessingOutputs = new ItemStack[3];
    private final List<SlotFake> inputSlots = new ArrayList<>();
    private final IInventory crafting;
    private final SlotFakeCraftingMatrix[] craftingSlots = new SlotFakeCraftingMatrix[9];
    private final AppEngSlot craftingOutputSlot;
    private final OptionalSlotFake[] processingOutputSlots = new OptionalSlotFake[3];
    private final AppEngInternalInventory cOut = new AppEngInternalInventory(null, 1);

    @GuiSync(97)
    public boolean craftingMode = true;

    @GuiSync(96)
    public boolean substitute = false;

    @GuiSync(95)
    public boolean beSubstitute = true;

    private ContainerInterfaceTerminal sourceContainer;
    private long sourceEntryId = -1;
    private int sourceSlot = -1;
    private ICraftingPatternDetails patternDetails;
    private ItemStack originalPatternStack;

    public ContainerEditorPattern(final InventoryPlayer ip, final PartInterfaceTerminal te) {
        super(ip, te);

        final AppEngInternalInventory crafting = new BiggerAppEngInventory(this, 9) {};
        final AppEngInternalInventory processingOutput = new BiggerAppEngInventory(this, 3) {};

        this.crafting = crafting;

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                this.addSlotToContainer(
                        this.craftingSlots[x + y * 3] = new SlotFakeCraftingMatrix(
                                this.crafting,
                                x + y * 3,
                                CRAFTING_SLOTS_OFFSET_X + x * VANILLA_SLOT_SIZE,
                                CRAFTING_SLOTS_OFFSET_Y + y * VANILLA_SLOT_SIZE) {

                            @Override
                            public boolean isItemValid(ItemStack stack) {
                                if (stack != null && craftingMode) {
                                    stack.stackSize = 1;
                                }
                                return super.isItemValid(stack);
                            }

                            @Override
                            public void putStack(ItemStack stack) {
                                if (stack != null && craftingMode) {
                                    stack = stack.copy();
                                    stack.stackSize = 1;
                                }
                                super.putStack(stack);
                            }
                        });
            }
        }

        this.craftingOutputSlot = new AppEngSlot(
                this.cOut,
                0,
                CRAFTING_OUTPUT_SLOT_OFFSET_X_SMALL,
                CRAFTING_OUTPUT_SLOT_OFFSET_Y_SMALL) {

            @Override
            public void putStack(ItemStack stack) {}

            @Override
            public boolean canTakeStack(EntityPlayer player) {
                return false;
            }
        };
        this.addSlotToContainer(this.craftingOutputSlot);

        for (int y = 0; y < 3; y++) {
            OptionalSlotFake processingSlot = new OptionalSlotFake(
                    processingOutput,
                    this,
                    y,
                    OUTPUT_SLOTS_OFFSET_X_SMALL,
                    OUTPUT_SLOTS_OFFSET_Y_SMALL + y * VANILLA_SLOT_SIZE,
                    0,
                    0,
                    1);
            this.processingOutputSlots[y] = processingSlot;
            this.addSlotToContainer(processingSlot);
        }

        patternValue = new SlotInaccessible(
                new AppEngInternalInventory(null, 1),
                0,
                PATTERN_SLOTS_OFFSET_X_SMALL,
                PATTERN_SLOTS_OFFSET_Y_SMALL);
        this.addSlotToContainer(patternValue);

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                SlotFake inputSlot = new SlotFake(
                        new AppEngInternalInventory(null, 9),
                        x + y * 3,
                        INPUT_SLOTS_OFFSET_X_SMALL + x * 18,
                        INPUT_SLOTS_OFFSET_Y_SMALL + y * 18);
                this.addSlotToContainer(inputSlot);
                inputSlots.add(inputSlot);
            }
        }

        this.bindPlayerInventory(ip, 0, VANILLA_Y_OFFSET);
        updateSlotsVisibility();
    }

    @Override
    public void saveChanges() {
        this.detectAndSendChanges();
    }

    @Override
    public void onChangeInventory(IInventory inv, int slot, InvOperation mc, ItemStack removed, ItemStack added) {
        if (inv == this.crafting && craftingMode) {
            getAndUpdateOutput();
        }
        this.detectAndSendChanges();
    }

    public void setSourceData(ContainerInterfaceTerminal sourceContainer, long sourceEntryId, int sourceSlot) {
        this.sourceContainer = sourceContainer;
        this.sourceEntryId = sourceEntryId;
        this.sourceSlot = sourceSlot;
    }

    public World getWorld() {
        return getPlayerInv().player.worldObj;
    }

    public Slot getPatternValue() {
        return patternValue;
    }

    public void setPatternDetails(ICraftingPatternDetails details, ItemStack patternStack) {
        this.patternDetails = details;
        this.originalPatternStack = patternStack;

        if (details != null) {
            if (patternStack.hasTagCompound()) {
                NBTTagCompound tag = patternStack.getTagCompound();
                this.craftingMode = tag.getBoolean("crafting");
                this.substitute = tag.getBoolean("substitute");
                this.beSubstitute = tag.getBoolean("beSubstitute");
            }
            initializeSlots();
        }
    }

    private void initializeSlots() {
        if (patternDetails == null) return;

        clearSlots();

        IAEStack[] inputs = patternDetails.getInputs();
        IAEStack[] outputs = patternDetails.getOutputs();

        boolean patternIsCrafting = this.craftingMode;

        if (patternIsCrafting) {
            for (int i = 0; i < Math.min(inputs.length, 9); i++) {
                if (inputs[i] instanceof IAEItemStack) {
                    IAEItemStack aeStack = (IAEItemStack) inputs[i];
                    ItemStack stack = aeStack.getItemStack();
                    if (stack != null) {
                        stack = stack.copy();
                        stack.stackSize = 1;
                        craftingSlots[i].putStack(stack);
                    }
                }
            }

            if (outputs != null && outputs.length > 0 && outputs[0] instanceof IAEItemStack) {
                IAEItemStack aeStack = (IAEItemStack) outputs[0];
                ItemStack stack = aeStack.getItemStack();
                if (stack != null) {
                    ItemStack copy = stack.copy();
                    copy.stackSize = 1;
                    craftingOutputSlot.putStack(copy);
                    cOut.setInventorySlotContents(0, copy);
                }
            } else {
                getAndUpdateOutput();
            }
        } else {
            for (int i = 0; i < Math.min(inputs.length, 9); i++) {
                if (inputs[i] instanceof IAEItemStack) {
                    IAEItemStack aeStack = (IAEItemStack) inputs[i];
                    ItemStack stack = aeStack.getItemStack();
                    if (stack != null) {
                        ItemStack copy = stack.copy();
                        if (copy.stackSize <= 0) {
                            copy.stackSize = 1;
                        }
                        inputSlots.get(i).putStack(copy);
                    }
                }
            }

            for (int i = 0; i < Math.min(outputs.length, 3); i++) {
                if (outputs[i] instanceof IAEItemStack) {
                    IAEItemStack aeStack = (IAEItemStack) outputs[i];
                    ItemStack stack = aeStack.getItemStack();
                    if (stack != null) {
                        ItemStack copy = stack.copy();
                        if (copy.stackSize <= 0) {
                            copy.stackSize = 1;
                        }
                        processingOutputSlots[i].putStack(copy);
                    }
                }
            }
        }

        if (originalPatternStack != null) {
            patternValue.putStack(originalPatternStack.copy());
        }

        updateSlotsVisibility();
        this.detectAndSendChanges();
    }

    public void clear() {
        for (Slot slot : craftingSlots) {
            slot.putStack(null);
        }
        for (Slot slot : processingOutputSlots) {
            slot.putStack(null);
        }
        for (Slot slot : inputSlots) {
            slot.putStack(null);
        }
        cOut.setInventorySlotContents(0, null);
        craftingOutputSlot.putStack(null);

        if (craftingMode) {
            getAndUpdateOutput();
        }
    }

    public void encodePattern() {
        if (originalPatternStack == null) {
            return;
        }

        ItemStack patternStack = originalPatternStack.copy();
        patternStack.stackSize = 1;

        final ItemStack[] in = getInputs();
        final ItemStack[] out = getOutputs();

        if (in == null || out == null) {
            return;
        }

        final NBTTagCompound encodedValue = new NBTTagCompound();
        final NBTTagList tagIn = new NBTTagList();
        final NBTTagList tagOut = new NBTTagList();

        for (final ItemStack i : in) {
            if (craftingMode) {
                ItemStack copy = i != null ? i.copy() : null;
                if (copy != null) {
                    copy.stackSize = 1;
                }
                tagIn.appendTag(createItemTag(copy));
            } else {
                if (i != null) {
                    AEItemStack aeStack = AEItemStack.create(i);
                    NBTTagCompound itemTag = Platform.writeStackNBT(aeStack, new NBTTagCompound(), true);

                    if (itemTag.hasKey("Count")) {
                        itemTag.setByte("Count", (byte) i.stackSize);
                    } else {
                        itemTag.setByte("Count", (byte) i.stackSize);
                    }

                    tagIn.appendTag(itemTag);
                } else {
                    tagIn.appendTag(new NBTTagCompound());
                }
            }
        }

        for (final ItemStack i : out) {
            if (craftingMode) {
                ItemStack copy = i != null ? i.copy() : null;
                if (copy != null) {
                    copy.stackSize = 1;
                }
                tagOut.appendTag(createItemTag(copy));
            } else {
                if (i != null) {
                    AEItemStack aeStack = AEItemStack.create(i);
                    NBTTagCompound itemTag = Platform.writeStackNBT(aeStack, new NBTTagCompound(), true);

                    if (itemTag.hasKey("Count")) {
                        itemTag.setByte("Count", (byte) i.stackSize);
                    } else {
                        itemTag.setByte("Count", (byte) i.stackSize);
                    }

                    tagOut.appendTag(itemTag);
                } else {
                    tagOut.appendTag(new NBTTagCompound());
                }
            }
        }

        encodedValue.setTag("in", tagIn);
        encodedValue.setTag("out", tagOut);
        encodedValue.setBoolean("crafting", this.craftingMode);
        encodedValue.setBoolean("substitute", this.substitute);
        encodedValue.setBoolean("beSubstitute", this.beSubstitute);

        if (originalPatternStack.hasTagCompound() && originalPatternStack.getTagCompound().hasKey("author")) {
            encodedValue.setString("author", originalPatternStack.getTagCompound().getString("author"));
        } else {
            encodedValue.setString("author", this.getPlayerInv().player.getCommandSenderName());
        }

        patternStack.setTagCompound(encodedValue);
        this.originalPatternStack = patternStack;
        this.patternValue.putStack(patternStack);

        updateSourceTerminal(patternStack);
    }

    private NBTTagCompound createItemTag(final ItemStack i) {
        final NBTTagCompound c = new NBTTagCompound();

        if (i != null && i.getItem() != null) {
            i.writeToNBT(c);
        }

        return c;
    }

    private void updateSourceTerminal(ItemStack updatedPattern) {
        if (sourceEntryId >= 0 && sourceSlot >= 0 && sourceContainer != null) {
            try {
                sourceContainer.updatePattern(sourceEntryId, sourceSlot, updatedPattern);
            } catch (final Exception e) {}
        }
    }

    private ItemStack getAndUpdateOutput() {
        if (!craftingMode) return null;

        final InventoryCrafting ic = new InventoryCrafting(new DummyContainer(), 3, 3);

        for (int x = 0; x < ic.getSizeInventory(); x++) {
            ItemStack stack = this.crafting.getStackInSlot(x);
            if (stack != null && stack.stackSize != 1) {
                stack = stack.copy();
                stack.stackSize = 1;
                this.crafting.setInventorySlotContents(x, stack);
            }
            ic.setInventorySlotContents(x, stack);
        }

        final ItemStack is = CraftingManager.getInstance().findMatchingRecipe(ic, this.getPlayerInv().player.worldObj);

        this.cOut.setInventorySlotContents(0, is);

        return is;
    }

    private ItemStack[] getInputs() {
        if (craftingMode) {
            final ItemStack[] input = new ItemStack[9];
            boolean hasValue = false;

            for (int x = 0; x < this.craftingSlots.length; x++) {
                input[x] = this.craftingSlots[x].getStack();
                if (input[x] != null) {
                    hasValue = true;
                    if (input[x].stackSize != 1) {
                        input[x] = input[x].copy();
                        input[x].stackSize = 1;
                    }
                }
            }

            return hasValue ? input : null;
        } else {
            final ItemStack[] input = new ItemStack[9];
            boolean hasValue = false;

            for (int x = 0; x < this.inputSlots.size(); x++) {
                ItemStack stack = this.inputSlots.get(x).getStack();
                if (stack != null) {
                    hasValue = true;
                    input[x] = stack.copy();
                    if (input[x].stackSize <= 0) {
                        input[x].stackSize = 1;
                    }
                }
            }

            return hasValue ? input : null;
        }
    }

    private ItemStack[] getOutputs() {
        if (this.craftingMode) {
            ItemStack result = null;

            if (this.cOut.getStackInSlot(0) != null) {
                result = this.cOut.getStackInSlot(0).copy();
                result.stackSize = 1;
            } else if (this.craftingOutputSlot.getStack() != null) {
                result = this.craftingOutputSlot.getStack().copy();
                result.stackSize = 1;
            }

            return result != null ? new ItemStack[] { result } : null;
        } else {
            final List<ItemStack> list = new ArrayList<>(3);
            boolean hasValue = false;

            for (final OptionalSlotFake outputSlot : this.processingOutputSlots) {
                final ItemStack out = outputSlot.getStack();
                if (out != null && out.stackSize > 0) {
                    ItemStack copy = out.copy();
                    if (copy.stackSize <= 0) {
                        copy.stackSize = 1;
                    }
                    list.add(copy);
                    hasValue = true;
                }
            }

            return hasValue ? list.toArray(new ItemStack[0]) : null;
        }
    }

    private void clearSlots() {
        for (Slot slot : craftingSlots) {
            slot.putStack(null);
        }
        for (Slot slot : processingOutputSlots) {
            slot.putStack(null);
        }
        for (Slot slot : inputSlots) {
            slot.putStack(null);
        }
        cOut.setInventorySlotContents(0, null);
        craftingOutputSlot.putStack(null);
    }

    public ICraftingPatternDetails getPatternDetails() {
        return patternDetails;
    }

    public boolean isCraftingMode() {
        return craftingMode;
    }

    public void setCraftingMode(boolean craftingMode) {
        if (this.craftingMode != craftingMode) {
            saveCurrentSlotsData();
            this.craftingMode = craftingMode;
            restoreSlotsData();
            updateSlotsVisibility();

            if (craftingMode) {
                getAndUpdateOutput();
            }

            this.detectAndSendChanges();
        }
    }

    private void saveCurrentSlotsData() {
        if (this.craftingMode) {
            for (int i = 0; i < craftingSlots.length; i++) {
                ItemStack stack = craftingSlots[i].getStack();
                savedCraftingInputs[i] = stack != null ? stack.copy() : null;
            }
            ItemStack output = craftingOutputSlot.getStack();
            savedCraftingOutputs[0] = output != null ? output.copy() : null;
        } else {
            for (int i = 0; i < inputSlots.size(); i++) {
                ItemStack stack = inputSlots.get(i).getStack();
                savedProcessingInputs[i] = stack != null ? stack.copy() : null;
            }
            for (int i = 0; i < processingOutputSlots.length; i++) {
                ItemStack stack = processingOutputSlots[i].getStack();
                savedProcessingOutputs[i] = stack != null ? stack.copy() : null;
            }
        }
    }

    private void restoreSlotsData() {
        clearSlots();

        if (this.craftingMode) {
            for (int i = 0; i < Math.min(savedCraftingInputs.length, craftingSlots.length); i++) {
                if (savedCraftingInputs[i] != null) {
                    ItemStack stack = savedCraftingInputs[i].copy();
                    stack.stackSize = 1;
                    craftingSlots[i].putStack(stack);
                }
            }
            if (savedCraftingOutputs[0] != null) {
                ItemStack stack = savedCraftingOutputs[0].copy();
                stack.stackSize = 1;
                craftingOutputSlot.putStack(stack);
                cOut.setInventorySlotContents(0, stack);
            }
        } else {
            for (int i = 0; i < Math.min(savedProcessingInputs.length, inputSlots.size()); i++) {
                if (savedProcessingInputs[i] != null) {
                    ItemStack stack = savedProcessingInputs[i].copy();
                    if (stack.stackSize <= 0) {
                        stack.stackSize = 1;
                    }
                    inputSlots.get(i).putStack(stack);
                }
            }
            for (int i = 0; i < Math.min(savedProcessingOutputs.length, processingOutputSlots.length); i++) {
                if (savedProcessingOutputs[i] != null) {
                    ItemStack stack = savedProcessingOutputs[i].copy();
                    if (stack.stackSize <= 0) {
                        stack.stackSize = 1;
                    }
                    processingOutputSlots[i].putStack(stack);
                }
            }
        }
    }

    public void setSubstitute(boolean substitute) {
        this.substitute = substitute;
    }

    public void setBeSubstitute(boolean beSubstitute) {
        this.beSubstitute = beSubstitute;
    }

    @Override
    public void onUpdate(String field, Object oldValue, Object newValue) {
        super.onUpdate(field, oldValue, newValue);

        if (field.equals("craftingMode")) {
            updateSlotsVisibility();
        }
    }

    @Override
    public void onSlotChange(Slot slot) {
        super.onSlotChange(slot);

        if (craftingMode && isCraftingSlot(slot)) {
            getAndUpdateOutput();
        }
    }

    private boolean isCraftingSlot(Slot slot) {
        for (SlotFakeCraftingMatrix craftingSlot : craftingSlots) {
            if (slot == craftingSlot) {
                return true;
            }
        }
        return false;
    }

    private void updateSlotsVisibility() {
        if (this.craftingMode) {
            for (int i = 0; i < craftingSlots.length; i++) {
                Slot slot = craftingSlots[i];
                slot.xDisplayPosition = CRAFTING_SLOTS_OFFSET_X + (i % 3) * VANILLA_SLOT_SIZE;
                slot.yDisplayPosition = CRAFTING_SLOTS_OFFSET_Y + (i / 3) * VANILLA_SLOT_SIZE;
            }
            for (Slot slot : inputSlots) {
                slot.xDisplayPosition = -1000;
                slot.yDisplayPosition = -1000;
            }
        } else {
            for (Slot slot : craftingSlots) {
                slot.xDisplayPosition = -1000;
                slot.yDisplayPosition = -1000;
            }
            for (int i = 0; i < inputSlots.size(); i++) {
                Slot slot = inputSlots.get(i);
                slot.xDisplayPosition = INPUT_SLOTS_OFFSET_X_SMALL + (i % 3) * VANILLA_SLOT_SIZE;
                slot.yDisplayPosition = INPUT_SLOTS_OFFSET_Y_SMALL + (i / 3) * VANILLA_SLOT_SIZE;
            }
        }

        updateOutputSlotsVisibility();
        this.detectAndSendChanges();
    }

    private void updateOutputSlotsVisibility() {
        if (this.craftingMode) {
            craftingOutputSlot.xDisplayPosition = CRAFTING_OUTPUT_SLOT_OFFSET_X_SMALL;
            craftingOutputSlot.yDisplayPosition = CRAFTING_OUTPUT_SLOT_OFFSET_Y_SMALL;

            for (int i = 0; i < processingOutputSlots.length; i++) {
                processingOutputSlots[i].xDisplayPosition = -1000;
                processingOutputSlots[i].yDisplayPosition = -1000;
            }
        } else {
            craftingOutputSlot.xDisplayPosition = -1000;
            craftingOutputSlot.yDisplayPosition = -1000;

            for (int i = 0; i < processingOutputSlots.length; i++) {
                processingOutputSlots[i].xDisplayPosition = OUTPUT_SLOTS_OFFSET_X_SMALL;
                processingOutputSlots[i].yDisplayPosition = OUTPUT_SLOTS_OFFSET_Y_SMALL + i * VANILLA_SLOT_SIZE;
            }
        }
    }

    @Override
    public boolean isSlotEnabled(int idx) {
        if (idx == 1) {
            return !this.craftingMode;
        } else if (idx == 2) {
            return this.craftingMode;
        } else {
            return false;
        }
    }

    public void doubleStacks(int val) {
        if (!isCraftingMode()) {
            multiplyOrDivideStacks(((val & 1) != 0 ? 64 : 2) * ((val & 2) != 0 ? -1 : 1));
        }
    }

    public void multiplyOrDivideStacks(int multi) {
        if (!isCraftingMode()) {
            for (SlotFake slot : inputSlots) {
                if (slot.getHasStack() && slot.xDisplayPosition >= 0) {
                    ItemStack stack = slot.getStack();
                    if (multi > 0) {
                        stack.stackSize = Math.min(64, stack.stackSize * multi);
                    } else if (multi < 0) {
                        stack.stackSize = Math.max(1, stack.stackSize / Math.abs(multi));
                    }
                }
            }

            for (OptionalSlotFake slot : processingOutputSlots) {
                if (slot.getHasStack() && slot.xDisplayPosition >= 0) {
                    ItemStack stack = slot.getStack();
                    if (multi > 0) {
                        stack.stackSize = Math.min(64, stack.stackSize * multi);
                    } else if (multi < 0) {
                        stack.stackSize = Math.max(1, stack.stackSize / Math.abs(multi));
                    }
                }
            }

            this.detectAndSendChanges();
        }
    }

    @Override
    public void onContainerClosed(final EntityPlayer player) {
        super.onContainerClosed(player);
    }

    private static class DummyContainer extends Container {

        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return false;
        }
    }

    public ContainerInterfaceTerminal getSourceContainer() {
        return sourceContainer;
    }
}
