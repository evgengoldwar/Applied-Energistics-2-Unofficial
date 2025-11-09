package appeng.container.implementations;

import java.util.ArrayList;
import java.util.List;

import appeng.container.slot.SlotPatternTerm;
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.tile.inventory.InvOperation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
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
import appeng.container.slot.IOptionalSlotHost;
import appeng.container.slot.OptionalSlotFake;
import appeng.container.slot.SlotFake;
import appeng.container.slot.SlotFakeCraftingMatrix;
import appeng.container.slot.SlotInaccessible;
import appeng.container.slot.SlotPatternOutputs;
import appeng.container.slot.SlotRestrictedInput;
import appeng.parts.reporting.PartInterfaceTerminal;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.BiggerAppEngInventory;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;

public class ContainerEditorPattern extends AEBaseContainer implements IOptionalSlotHost, IAEAppEngInventory {

    private static final int VANILLA_Y_OFFSET = 167;
    private static final int VANILLA_SLOT_SIZE = 18;
    private static final int CRAFTING_SLOTS_OFFSET_Y = 93;
    private static final int CRAFTING_SLOTS_OFFSET_X = 18;
    private static final int OUTPUT_SLOTS_OFFSET_Y_SMALL = 93;
    private static final int OUTPUT_SLOTS_OFFSET_X_SMALL = 110;
    private static final int PATTERN_SLOTS_OFFSET_Y_SMALL = 88;
    private static final int PATTERN_SLOTS_OFFSET_X_SMALL = 147;
    private static final int INPUT_SLOTS_OFFSET_Y_SMALL = 93;
    private static final int INPUT_SLOTS_OFFSET_X_SMALL = 18;
    private static final int INPUT_SLOTS = 9;
    private static final int OUTPUT_SLOTS = 3;

    private final Slot patternValue;
    private ICraftingPatternDetails patternDetails;
    private final List<SlotFake> inputSlots = new ArrayList<>();
    private ItemStack originalPatternStack;
    private final IInventory crafting;
    private final SlotFakeCraftingMatrix[] craftingSlots = new SlotFakeCraftingMatrix[9];

    // Разделенные слоты как в ContainerPatternTerm
    private final OptionalSlotFake craftingOutputSlot; // Отдельный слот для крафтинга
    private final OptionalSlotFake[] processingOutputSlots = new OptionalSlotFake[3]; // Только для процессинга

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

    // Временное хранилище для данных при переключении режимов
    private ItemStack[] savedCraftingInputs = new ItemStack[9];
    private ItemStack[] savedCraftingOutputs = new ItemStack[1];
    private ItemStack[] savedProcessingInputs = new ItemStack[9];
    private ItemStack[] savedProcessingOutputs = new ItemStack[3];

    public ContainerEditorPattern(final InventoryPlayer ip, final PartInterfaceTerminal te) {
        super(ip, te);

        final AppEngInternalInventory crafting = new BiggerAppEngInventory(this, 9) {};
        final AppEngInternalInventory processingOutput = new BiggerAppEngInventory(this, 3) {};

        this.crafting = crafting;

        // Создаем слоты крафтинговой матрицы (3x3) - ТОЧНО КАК В ContainerPatternTerm
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

        // Создаем выходной слот для крафтинга - ОТДЕЛЬНЫЙ (как craftSlot в ContainerPatternTerm)
        this.craftingOutputSlot = new OptionalSlotFake(
                this.cOut,
                this,
                0,
                OUTPUT_SLOTS_OFFSET_X_SMALL,
                OUTPUT_SLOTS_OFFSET_Y_SMALL,
                0,
                0,
                2);
        this.addSlotToContainer(this.craftingOutputSlot);
        // НЕ добавляем в общие списки!

        // Создаем выходные слоты для процессинга (3 слота) - ОТДЕЛЬНЫЕ (как outputSlots в ContainerPatternTerm)
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
            // НЕ добавляем в общие списки!
        }

        // Создаем слот для отображения значения паттерна
        patternValue = new SlotInaccessible(
                new AppEngInternalInventory(null, 1),
                0,
                PATTERN_SLOTS_OFFSET_X_SMALL,
                PATTERN_SLOTS_OFFSET_Y_SMALL);
        this.addSlotToContainer(patternValue);

        // Создаем входные слоты для процессинга (3x3) - ТОЧНО КАК В ContainerPatternTerm
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                SlotFake inputSlot = new SlotFake(
                        new AppEngInternalInventory(null, INPUT_SLOTS),
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

        appeng.api.storage.data.IAEStack[] inputs = patternDetails.getInputs();
        appeng.api.storage.data.IAEStack[] outputs = patternDetails.getOutputs();

        // ВАЖНО: Заполняем слоты в зависимости от текущего режима паттерна
        boolean patternIsCrafting = this.craftingMode; // Используем текущий режим

        if (patternIsCrafting) {
            // Заполняем слоты крафтинга
            for (int i = 0; i < Math.min(inputs.length, 9); i++) {
                if (inputs[i] instanceof appeng.api.storage.data.IAEItemStack) {
                    appeng.api.storage.data.IAEItemStack aeStack = (appeng.api.storage.data.IAEItemStack) inputs[i];
                    ItemStack stack = aeStack.getItemStack();
                    if (stack != null) {
                        stack = stack.copy();
                        stack.stackSize = 1;
                        craftingSlots[i].putStack(stack);
                    }
                }
            }

            // Заполняем выход крафтинга
            if (outputs != null && outputs.length > 0 && outputs[0] instanceof appeng.api.storage.data.IAEItemStack) {
                appeng.api.storage.data.IAEItemStack aeStack = (appeng.api.storage.data.IAEItemStack) outputs[0];
                ItemStack stack = aeStack.getItemStack();
                if (stack != null) {
                    ItemStack copy = stack.copy();
                    copy.stackSize = 1;
                    craftingOutputSlot.putStack(copy);
                    cOut.setInventorySlotContents(0, copy);
                }
            } else {
                getAndUpdateOutput(); // Обновляем вывод если нет сохраненного
            }
        } else {
            // Заполняем слоты процессинга
            for (int i = 0; i < Math.min(inputs.length, 9); i++) {
                if (inputs[i] instanceof appeng.api.storage.data.IAEItemStack) {
                    appeng.api.storage.data.IAEItemStack aeStack = (appeng.api.storage.data.IAEItemStack) inputs[i];
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

            // Заполняем выходные слоты процессинга
            for (int i = 0; i < Math.min(outputs.length, 3); i++) {
                if (outputs[i] instanceof appeng.api.storage.data.IAEItemStack) {
                    appeng.api.storage.data.IAEItemStack aeStack = (appeng.api.storage.data.IAEItemStack) outputs[i];
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

        debugPatternInfo();

        ItemStack patternStack = originalPatternStack.copy();
        patternStack.stackSize = 1;

        final ItemStack[] in = getInputs();
        final ItemStack[] out = getOutputs();

        if (in == null || out == null) {
            System.out.println("=== PATTERN ENCODING FAILED: Inputs or Outputs are null ===");
            return;
        }

        final NBTTagCompound encodedValue = new NBTTagCompound();
        final NBTTagList tagIn = new NBTTagList();
        final NBTTagList tagOut = new NBTTagList();

        for (final ItemStack i : in) {
            if (craftingMode) {
                // КРАФТИНГ: обычный формат Minecraft
                ItemStack copy = i != null ? i.copy() : null;
                if (copy != null) {
                    copy.stackSize = 1;
                }
                tagIn.appendTag(createItemTag(copy));
            } else {
                // ПРОЦЕССИНГ: формат AE2 с правильными полями
                if (i != null) {
                    AEItemStack aeStack = AEItemStack.create(i);
                    NBTTagCompound itemTag = Platform.writeStackNBT(aeStack, new NBTTagCompound(), true);

                    // ВАЖНО: Убедимся, что Count установлен правильно
                    if (itemTag.hasKey("Count")) {
                        itemTag.setByte("Count", (byte) i.stackSize); // Устанавливаем реальное количество
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
                // КРАФТИНГ: обычный формат Minecraft
                ItemStack copy = i != null ? i.copy() : null;
                if (copy != null) {
                    copy.stackSize = 1;
                }
                tagOut.appendTag(createItemTag(copy));
            } else {
                // ПРОЦЕССИНГ: формат AE2 с правильными полями
                if (i != null) {
                    AEItemStack aeStack = AEItemStack.create(i);
                    NBTTagCompound itemTag = Platform.writeStackNBT(aeStack, new NBTTagCompound(), true);

                    // ВАЖНО: Убедимся, что Count установлен правильно
                    if (itemTag.hasKey("Count")) {
                        itemTag.setByte("Count", (byte) i.stackSize); // Устанавливаем реальное количество
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

        debugEncodedNBT(encodedValue);

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
            } catch (final Exception e) {
                e.printStackTrace();
            }
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

        // ТОЛЬКО ЭТО - слот сам обновится через инвентарь
        this.cOut.setInventorySlotContents(0, is);
        // УБРАТЬ ЭТО: craftingOutputSlot.putStack(is);

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
            // Для крафтинга используем оба возможных источника вывода
            ItemStack result = null;

            // Сначала проверяем cOut инвентарь
            if (this.cOut.getStackInSlot(0) != null) {
                result = this.cOut.getStackInSlot(0).copy();
                result.stackSize = 1;
            }
            // Затем проверяем craftingOutputSlot
            else if (this.craftingOutputSlot.getStack() != null) {
                result = this.craftingOutputSlot.getStack().copy();
                result.stackSize = 1;
            }

            return result != null ? new ItemStack[] { result } : null;
        } else {
            // Для процессинга используем processingOutputSlots
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

    public List<SlotFake> getInputSlots() {
        return inputSlots;
    }

    public OptionalSlotFake getCraftingOutputSlot() {
        return craftingOutputSlot;
    }

    public OptionalSlotFake[] getProcessingOutputSlots() {
        return processingOutputSlots;
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
            // Сохраняем данные крафтинга
            for (int i = 0; i < craftingSlots.length; i++) {
                ItemStack stack = craftingSlots[i].getStack();
                savedCraftingInputs[i] = stack != null ? stack.copy() : null;
            }
            ItemStack output = craftingOutputSlot.getStack();
            savedCraftingOutputs[0] = output != null ? output.copy() : null;
        } else {
            // Сохраняем данные процессинга
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
            // Восстанавливаем данные крафтинга
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
            // Восстанавливаем данные процессинга
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
            // Показываем слоты крафтинга, скрываем слоты процессинга
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
            // Показываем слоты процессинга, скрываем слоты крафтинга
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
            // В режиме крафтинга показываем только craftingOutputSlot, скрываем processingOutputSlots
            craftingOutputSlot.xDisplayPosition = OUTPUT_SLOTS_OFFSET_X_SMALL;
            craftingOutputSlot.yDisplayPosition = OUTPUT_SLOTS_OFFSET_Y_SMALL;

            for (int i = 0; i < processingOutputSlots.length; i++) {
                processingOutputSlots[i].xDisplayPosition = -1000;
                processingOutputSlots[i].yDisplayPosition = -1000;
            }
        } else {
            // В режиме процессинга скрываем craftingOutputSlot, показываем processingOutputSlots
            craftingOutputSlot.xDisplayPosition = -1000;
            craftingOutputSlot.yDisplayPosition = -1000;

            for (int i = 0; i < processingOutputSlots.length; i++) {
                processingOutputSlots[i].xDisplayPosition = OUTPUT_SLOTS_OFFSET_X_SMALL;
                processingOutputSlots[i].yDisplayPosition = OUTPUT_SLOTS_OFFSET_Y_SMALL + i * VANILLA_SLOT_SIZE;
            }
        }
    }

    public void toggleSubstitute() {
        this.substitute = !this.substitute;
    }

    public void toggleBeSubstitute() {
        this.beSubstitute = !this.beSubstitute;
    }

    public ItemStack getEncodedPattern() {
        return originalPatternStack;
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
            multiplyOrDivideStacks(
                    ((val & 1) != 0 ? 64 : 2) * ((val & 2) != 0 ? -1 : 1));
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
        if (originalPatternStack != null && sourceEntryId >= 0) {
            encodePattern();
        }
        super.onContainerClosed(player);
    }

    private static class DummyContainer extends net.minecraft.inventory.Container {
        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return false;
        }
    }

    private void debugPatternInfo() {
        System.out.println(" ");
        System.out.println("=== DEBUG PATTERN INFO ===");

        // Информация о контейнере
        System.out.println("Container Info:");
        System.out.println("  Class: " + this.getClass().getName());
        System.out.println("  Crafting Mode: " + this.craftingMode);
        System.out.println("  Substitute: " + this.substitute);
        System.out.println("  Be Substitute: " + this.beSubstitute);
        System.out.println("  Source Entry ID: " + this.sourceEntryId);
        System.out.println("  Source Slot: " + this.sourceSlot);
        System.out.println("  Source Container: " + (this.sourceContainer != null ? "present" : "null"));

        // Информация о оригинальном паттерне
        System.out.println("Original Pattern Stack:");
        if (this.originalPatternStack != null) {
            System.out.println("  Item: " + this.originalPatternStack.getItem());
            System.out.println("  Display Name: " + this.originalPatternStack.getDisplayName());
            System.out.println("  Stack Size: " + this.originalPatternStack.stackSize);
            System.out.println("  Has NBT: " + this.originalPatternStack.hasTagCompound());

            if (this.originalPatternStack.hasTagCompound()) {
                debugNBTData("Original Pattern NBT", this.originalPatternStack.getTagCompound());
            }
        } else {
            System.out.println("  null");
        }

        // Информация о pattern details
        System.out.println("Pattern Details:");
        if (this.patternDetails != null) {
            System.out.println("  Class: " + this.patternDetails.getClass().getName());

            // Inputs
            appeng.api.storage.data.IAEStack[] inputs = this.patternDetails.getInputs();
            System.out.println("  Inputs (" + (inputs != null ? inputs.length : 0) + "):");
            if (inputs != null) {
                for (int i = 0; i < inputs.length; i++) {
                    if (inputs[i] instanceof appeng.api.storage.data.IAEItemStack) {
                        appeng.api.storage.data.IAEItemStack aeStack = (appeng.api.storage.data.IAEItemStack) inputs[i];
                        ItemStack stack = aeStack != null ? aeStack.getItemStack() : null;
                        System.out.println("    [" + i + "]: " + (stack != null ?
                                stack.getDisplayName() + " (x" + stack.stackSize + ")" : "null"));
                    } else {
                        System.out.println("    [" + i + "]: " + (inputs[i] != null ? inputs[i].toString() : "null"));
                    }
                }
            }

            // Outputs
            appeng.api.storage.data.IAEStack[] outputs = this.patternDetails.getOutputs();
            System.out.println("  Outputs (" + (outputs != null ? outputs.length : 0) + "):");
            if (outputs != null) {
                for (int i = 0; i < outputs.length; i++) {
                    if (outputs[i] instanceof appeng.api.storage.data.IAEItemStack) {
                        appeng.api.storage.data.IAEItemStack aeStack = (appeng.api.storage.data.IAEItemStack) outputs[i];
                        ItemStack stack = aeStack != null ? aeStack.getItemStack() : null;
                        System.out.println("    [" + i + "]: " + (stack != null ?
                                stack.getDisplayName() + " (x" + stack.stackSize + ")" : "null"));
                    } else {
                        System.out.println("    [" + i + "]: " + (outputs[i] != null ? outputs[i].toString() : "null"));
                    }
                }
            }
        } else {
            System.out.println("  null");
        }

        // Текущие входные данные
        ItemStack[] currentInputs = getInputs();
        System.out.println("Current Inputs (" + (currentInputs != null ? currentInputs.length : 0) + "):");
        if (currentInputs != null) {
            for (int i = 0; i < currentInputs.length; i++) {
                ItemStack stack = currentInputs[i];
                System.out.println("  [" + i + "]: " + (stack != null ?
                        stack.getDisplayName() + " (x" + stack.stackSize + ")" : "null"));
            }
        } else {
            System.out.println("  null");
        }

        // Текущие выходные данные
        ItemStack[] currentOutputs = getOutputs();
        System.out.println("Current Outputs (" + (currentOutputs != null ? currentOutputs.length : 0) + "):");
        if (currentOutputs != null) {
            for (int i = 0; i < currentOutputs.length; i++) {
                ItemStack stack = currentOutputs[i];
                System.out.println("  [" + i + "]: " + (stack != null ?
                        stack.getDisplayName() + " (x" + stack.stackSize + ")" : "null"));
            }
        } else {
            System.out.println("  null");
        }

        // Информация о слотах
        System.out.println("Slots Info:");
        System.out.println("  Crafting Slots: " + this.craftingSlots.length);
        for (int i = 0; i < this.craftingSlots.length; i++) {
            ItemStack stack = this.craftingSlots[i].getStack();
            System.out.println("    Crafting[" + i + "]: " + (stack != null ?
                    stack.getDisplayName() + " (x" + stack.stackSize + ")" : "null"));
        }

        System.out.println("  Input Slots: " + this.inputSlots.size());
        for (int i = 0; i < this.inputSlots.size(); i++) {
            ItemStack stack = this.inputSlots.get(i).getStack();
            System.out.println("    Input[" + i + "]: " + (stack != null ?
                    stack.getDisplayName() + " (x" + stack.stackSize + ")" : "null"));
        }

        System.out.println("  Processing Output Slots: " + this.processingOutputSlots.length);
        for (int i = 0; i < this.processingOutputSlots.length; i++) {
            ItemStack stack = this.processingOutputSlots[i].getStack();
            System.out.println("    ProcessingOut[" + i + "]: " + (stack != null ?
                    stack.getDisplayName() + " (x" + stack.stackSize + ")" : "null"));
        }

        ItemStack craftingOut = this.craftingOutputSlot.getStack();
        System.out.println("  Crafting Output Slot: " + (craftingOut != null ?
                craftingOut.getDisplayName() + " (x" + craftingOut.stackSize + ")" : "null"));

        ItemStack cOutStack = this.cOut.getStackInSlot(0);
        System.out.println("  cOut Inventory: " + (cOutStack != null ?
                cOutStack.getDisplayName() + " (x" + cOutStack.stackSize + ")" : "null"));

        System.out.println("=== END DEBUG INFO ===");
        System.out.println(" ");
    }

    /**
     * Рекурсивно выводит все NBT данные
     */
    private void debugNBTData(String prefix, NBTTagCompound nbt) {
        if (nbt == null) {
            System.out.println(prefix + ": null");
            return;
        }

        // Совместимый способ итерации по ключам NBT
        java.util.Set<String> keys = nbt.func_150296_c(); // getKeySet() в более старых версиях
        for (String key : keys) {
            // Вместо getTagId используем проверку типа через методы hasKey
            if (nbt.hasKey(key, 1)) { // BYTE
                System.out.println(prefix + "." + key + ": " + nbt.getByte(key) + " (byte)");
            } else if (nbt.hasKey(key, 2)) { // SHORT
                System.out.println(prefix + "." + key + ": " + nbt.getShort(key) + " (short)");
            } else if (nbt.hasKey(key, 3)) { // INT
                System.out.println(prefix + "." + key + ": " + nbt.getInteger(key) + " (int)");
            } else if (nbt.hasKey(key, 4)) { // LONG
                System.out.println(prefix + "." + key + ": " + nbt.getLong(key) + " (long)");
            } else if (nbt.hasKey(key, 5)) { // FLOAT
                System.out.println(prefix + "." + key + ": " + nbt.getFloat(key) + " (float)");
            } else if (nbt.hasKey(key, 6)) { // DOUBLE
                System.out.println(prefix + "." + key + ": " + nbt.getDouble(key) + " (double)");
            } else if (nbt.hasKey(key, 7)) { // BYTE_ARRAY
                byte[] byteArray = nbt.getByteArray(key);
                System.out.println(prefix + "." + key + ": byte[" + byteArray.length + "]");
            } else if (nbt.hasKey(key, 8)) { // STRING
                System.out.println(prefix + "." + key + ": \"" + nbt.getString(key) + "\" (string)");
            } else if (nbt.hasKey(key, 9)) { // LIST
                NBTTagList list = nbt.getTagList(key, 10); // 10 = COMPOUND
                System.out.println(prefix + "." + key + ": list[" + list.tagCount() + "]");
                for (int i = 0; i < list.tagCount(); i++) {
                    debugNBTData(prefix + "." + key + "[" + i + "]", list.getCompoundTagAt(i));
                }
            } else if (nbt.hasKey(key, 10)) { // COMPOUND
                debugNBTData(prefix + "." + key, nbt.getCompoundTag(key));
            } else if (nbt.hasKey(key, 11)) { // INT_ARRAY
                int[] intArray = nbt.getIntArray(key);
                System.out.println(prefix + "." + key + ": int[" + intArray.length + "]");
            } else {
                System.out.println(prefix + "." + key + ": unknown type");
            }
        }
    }

    /**
     * Выводит отладочную информацию о новом NBT после кодирования
     */
    private void debugEncodedNBT(NBTTagCompound encodedValue) {
        System.out.println("=== ENCODED PATTERN NBT ===");
        if (encodedValue != null) {
            debugNBTData("Encoded", encodedValue);

            // Детальная информация о входах и выходах
            if (encodedValue.hasKey("in")) {
                NBTTagList tagIn = encodedValue.getTagList("in", 10);
                System.out.println("Encoded Inputs (" + tagIn.tagCount() + "):");
                for (int i = 0; i < tagIn.tagCount(); i++) {
                    NBTTagCompound itemTag = tagIn.getCompoundTagAt(i);
                    if (itemTag.hasKey("id")) {
                        // Совместимый способ получения информации о предмете
                        String itemId = itemTag.getString("id");
                        byte count = itemTag.hasKey("Count") ? itemTag.getByte("Count") : 1;
                        short damage = itemTag.hasKey("Damage") ? itemTag.getShort("Damage") : 0;

                        System.out.println("  [" + i + "]: " + itemId +
                                " (Damage: " + damage + ", Count: " + count + ")");
                    } else {
                        System.out.println("  [" + i + "]: empty");
                    }
                }
            }

            if (encodedValue.hasKey("out")) {
                NBTTagList tagOut = encodedValue.getTagList("out", 10);
                System.out.println("Encoded Outputs (" + tagOut.tagCount() + "):");
                for (int i = 0; i < tagOut.tagCount(); i++) {
                    NBTTagCompound itemTag = tagOut.getCompoundTagAt(i);
                    if (itemTag.hasKey("id")) {
                        String itemId = itemTag.getString("id");
                        byte count = itemTag.hasKey("Count") ? itemTag.getByte("Count") : 1;
                        short damage = itemTag.hasKey("Damage") ? itemTag.getShort("Damage") : 0;

                        System.out.println("  [" + i + "]: " + itemId +
                                " (Damage: " + damage + ", Count: " + count + ")");
                    } else {
                        System.out.println("  [" + i + "]: empty");
                    }
                }
            }

            // Дополнительная информация о флагах
            System.out.println("Pattern Flags:");
            System.out.println("  Crafting: " + encodedValue.getBoolean("crafting"));
            System.out.println("  Substitute: " + encodedValue.getBoolean("substitute"));
            System.out.println("  BeSubstitute: " + encodedValue.getBoolean("beSubstitute"));
            if (encodedValue.hasKey("author")) {
                System.out.println("  Author: " + encodedValue.getString("author"));
            }
        } else {
            System.out.println("Encoded NBT: null");
        }
        System.out.println("=== END ENCODED NBT ===");
    }
}