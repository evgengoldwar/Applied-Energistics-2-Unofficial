package appeng.container.implementations;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
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
import appeng.parts.reporting.PartInterfaceTerminal;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.BiggerAppEngInventory;
import appeng.util.item.AEItemStack;

public class ContainerEditorPattern extends AEBaseContainer implements IOptionalSlotHost {

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

    private final Slot patternValue;
    private ICraftingPatternDetails patternDetails;
    private final List<SlotFake> inputSlots = new ArrayList<>();
    private final List<SlotFake> outputSlots = new ArrayList<>();
    private ItemStack originalPatternStack;
    private final IInventory crafting;
    private final SlotFakeCraftingMatrix[] craftingSlots = new SlotFakeCraftingMatrix[9];
    private final OptionalSlotFake[] outputSlotsArray = new OptionalSlotFake[3];
    private final AppEngInternalInventory cOut = new AppEngInternalInventory(null, 1);

    private static final int INPUT_SLOTS = 9;
    private static final int OUTPUT_SLOTS = 3;
    private static final int OUTPUT_SLOTS_CRAFTING_MODE = 1;
    @GuiSync(97)
    public boolean craftingMode = true;

    @GuiSync(96)
    public boolean substitute = false;

    @GuiSync(95)
    public boolean beSubstitute = true;

    private ContainerInterfaceTerminal sourceContainer;
    private long sourceEntryId = -1;
    private int sourceSlot = -1;

    public ContainerEditorPattern(final InventoryPlayer ip, final PartInterfaceTerminal te) {
        super(ip, te);

        final AppEngInternalInventory pattern = new AppEngInternalInventory(null, 2);
        final AppEngInternalInventory output = new BiggerAppEngInventory(null, 3) {};
        this.crafting = new BiggerAppEngInventory(null, 9) {};

        // Создаем слоты для крафтовой сетки 3x3
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                this.addSlotToContainer(
                        this.craftingSlots[x + y * 3] = new SlotFakeCraftingMatrix(
                                this.crafting,
                                x + y * 3,
                                CRAFTING_SLOTS_OFFSET_X + x * VANILLA_SLOT_SIZE,
                                CRAFTING_SLOTS_OFFSET_Y + y * VANILLA_SLOT_SIZE));
            }
        }

        // Создаем слоты для выходов
        for (int y = 0; y < 3; y++) {
            this.addSlotToContainer(
                    this.outputSlotsArray[y] = new SlotPatternOutputs(
                            output,
                            this,
                            y,
                            OUTPUT_SLOTS_OFFSET_X_SMALL,
                            OUTPUT_SLOTS_OFFSET_Y_SMALL + y * VANILLA_SLOT_SIZE,
                            0,
                            0,
                            1));
            this.outputSlotsArray[y].setRenderDisabled(false);
            this.outputSlots.add(this.outputSlotsArray[y]);
        }

        // Слот для отображения паттерна
        patternValue = new SlotInaccessible(
                new AppEngInternalInventory(null, 1),
                0,
                PATTERN_SLOTS_OFFSET_X_SMALL,
                PATTERN_SLOTS_OFFSET_Y_SMALL);
        this.addSlotToContainer(patternValue);

        // Создаем inputSlots для совместимости
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                SlotFake inputSlot = new SlotFake(
                        new AppEngInternalInventory(null, INPUT_SLOTS),
                        x + y * 3,
                        INPUT_SLOTS_OFFSET_X_SMALL + x * 18,
                        INPUT_SLOTS_OFFSET_Y_SMALL + y * 18);
                inputSlots.add(inputSlot);
            }
        }

        this.bindPlayerInventory(ip, 0, VANILLA_Y_OFFSET);
        updateOutputSlotsVisibility();
    }

    // Добавьте методы для установки исходного входа и слота
    public void setSourceData(ContainerInterfaceTerminal sourceContainer, long sourceEntryId, int sourceSlot) {
        this.sourceContainer = sourceContainer;
        this.sourceEntryId = sourceEntryId;
        this.sourceSlot = sourceSlot;
        System.out.println(
                "Source data set: container=" + sourceContainer + ", entry=" + sourceEntryId + ", slot=" + sourceSlot);
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
            this.craftingMode = details.isCraftable();

            // Читаем настройки из NBT паттерна
            if (patternStack.hasTagCompound()) {
                NBTTagCompound tag = patternStack.getTagCompound();
                this.substitute = tag.getBoolean("substitute");
                this.beSubstitute = tag.getBoolean("beSubstitute");
            }

            initializeSlots();
        }
    }

    private void initializeSlots() {
        if (patternDetails == null) return;

        // Очищаем все слоты
        clearSlots();

        // Заполняем входные слоты
        appeng.api.storage.data.IAEStack[] inputs = patternDetails.getInputs();
        for (int i = 0; i < Math.min(inputs.length, INPUT_SLOTS); i++) {
            if (inputs[i] instanceof appeng.api.storage.data.IAEItemStack aeStack) {
                ItemStack stack = aeStack.getItemStack();
                if (stack != null) {
                    craftingSlots[i].putStack(stack.copy());
                }
            }
        }

        // Заполняем выходные слоты
        appeng.api.storage.data.IAEStack[] outputs = patternDetails.getOutputs();
        for (int i = 0; i < Math.min(outputs.length, OUTPUT_SLOTS); i++) {
            if (outputs[i] instanceof appeng.api.storage.data.IAEItemStack aeStack) {
                ItemStack stack = aeStack.getItemStack();
                if (stack != null) {
                    outputSlotsArray[i].putStack(stack.copy());
                }
            }
        }

        // Устанавливаем сам паттерн в слот
        if (originalPatternStack != null) {
            patternValue.putStack(originalPatternStack.copy());
        }
    }

    public void clear() {
        for (Slot slot : craftingSlots) {
            slot.putStack(null);
        }
        for (Slot slot : outputSlotsArray) {
            slot.putStack(null);
        }
    }

    public void encodePattern() {
        if (originalPatternStack == null) {
            System.out.println("Cannot encode: originalPatternStack is null");
            return;
        }

        System.out.println("Encoding pattern...");

        ItemStack patternStack = originalPatternStack.copy();
        patternStack.stackSize = 1;

        // Получаем входы и выходы из слотов
        final ItemStack[] in = getInputs();
        final ItemStack[] out = getOutputs();

        // Проверяем что есть хотя бы один вход и выход
        if (in == null || out == null) {
            System.out.println("Cannot encode: inputs or outputs are null");
            return;
        }

        // Создаем NBT для паттерна
        final NBTTagCompound encodedValue = new NBTTagCompound();
        final NBTTagList tagIn = new NBTTagList();
        final NBTTagList tagOut = new NBTTagList();

        // Записываем входы
        for (final ItemStack i : in) {
            if (craftingMode) {
                tagIn.appendTag(createItemTag(i));
            } else {
                if (i != null) {
                    tagIn.appendTag(
                            appeng.util.Platform.writeStackNBT(AEItemStack.create(i), new NBTTagCompound(), true));
                } else {
                    tagIn.appendTag(new NBTTagCompound());
                }
            }
        }

        // Записываем выходы
        for (final ItemStack i : out) {
            if (craftingMode) {
                tagOut.appendTag(createItemTag(i));
            } else {
                if (i != null) {
                    tagOut.appendTag(
                            appeng.util.Platform.writeStackNBT(AEItemStack.create(i), new NBTTagCompound(), true));
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

        // Сохраняем оригинального автора если был
        if (originalPatternStack.hasTagCompound() && originalPatternStack.getTagCompound().hasKey("author")) {
            encodedValue.setString("author", originalPatternStack.getTagCompound().getString("author"));
        } else {
            encodedValue.setString("author", this.getPlayerInv().player.getCommandSenderName());
        }

        patternStack.setTagCompound(encodedValue);

        // Обновляем локальную копию
        this.originalPatternStack = patternStack;
        this.patternValue.putStack(patternStack);

        System.out.println("Pattern encoded locally, updating source terminal...");

        // Сохраняем обратно в терминал
        updateSourceTerminal(patternStack);
    }

    // Метод для обновления исходного терминала
    private void updateSourceTerminal(ItemStack updatedPattern) {
        if (sourceEntryId >= 0 && sourceSlot >= 0 && sourceContainer != null) {
            try {
                System.out.println("Updating source terminal: entry=" + sourceEntryId + ", slot=" + sourceSlot);

                // Вызываем метод обновления в контейнере интерфейс терминала
                sourceContainer.updatePattern(sourceEntryId, sourceSlot, updatedPattern);

                System.out.println("Pattern updated in interface terminal successfully");

            } catch (final Exception e) {
                System.out.println("Error updating source terminal: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println(
                    "Cannot update source terminal: invalid source data (entry=" + sourceEntryId
                            + ", slot="
                            + sourceSlot
                            + ", container="
                            + sourceContainer
                            + ")");
        }
    }

    private ItemStack[] getInputs() {
        final ItemStack[] input = new ItemStack[9];
        boolean hasValue = false;

        for (int x = 0; x < this.craftingSlots.length; x++) {
            input[x] = this.craftingSlots[x].getStack();
            if (input[x] != null) {
                hasValue = true;
            }
        }

        if (hasValue) {
            return input;
        }

        return null;
    }

    private ItemStack[] getOutputs() {
        if (this.craftingMode) {
            // Для крафтового режима - только первый выход
            final ItemStack out = this.outputSlotsArray[0].getStack();
            if (out != null && out.stackSize > 0) {
                return new ItemStack[] { out };
            }
        } else {
            // Для processing режима - все выходы
            final List<ItemStack> list = new ArrayList<>(3);
            boolean hasValue = false;

            for (final OptionalSlotFake outputSlot : this.outputSlotsArray) {
                final ItemStack out = outputSlot.getStack();
                if (out != null && out.stackSize > 0) {
                    list.add(out);
                    hasValue = true;
                }
            }

            if (hasValue) {
                return list.toArray(new ItemStack[0]);
            }
        }

        return null;
    }

    private NBTTagCompound createItemTag(final ItemStack i) {
        final NBTTagCompound c = new NBTTagCompound();

        if (i != null) {
            i.writeToNBT(c);
            c.setInteger("Count", i.stackSize);
        }

        return c;
    }

    private void clearSlots() {
        for (Slot slot : craftingSlots) {
            slot.putStack(null);
        }
        for (Slot slot : outputSlotsArray) {
            slot.putStack(null);
        }
    }

    public List<SlotFake> getInputSlots() {
        return inputSlots;
    }

    public List<SlotFake> getOutputSlots() {
        return outputSlots;
    }

    public ICraftingPatternDetails getPatternDetails() {
        return patternDetails;
    }

    public boolean isCraftingMode() {
        return craftingMode;
    }

    public void setCraftingMode(boolean craftingMode) {
        this.craftingMode = craftingMode;
        updateOutputSlotsVisibility();
        this.detectAndSendChanges();
    }

    public void setSubstitute(boolean substitute) {
        this.substitute = substitute;
    }

    public void setBeSubstitute(boolean beSubstitute) {
        this.beSubstitute = beSubstitute;
    }

    private void updateOutputSlotsVisibility() {
        if (!this.craftingMode) {
            // Processing mode - показываем все 3 слота
            for (int i = 0; i < outputSlotsArray.length; i++) {
                outputSlotsArray[i].setRenderDisabled(false);
            }
        } else {
            // Crafting mode - показываем только первый слот
            for (int i = 0; i < outputSlotsArray.length; i++) {
                outputSlotsArray[i].setRenderDisabled(i != 0);
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

    // Методы для умножения/деления стаков
    public void doubleStacks(int val) {
        multiplyOrDivideStacks(
                ((val & 1) != 0 ? ContainerPatternTerm.MULTIPLE_OF_BUTTON_CLICK_ON_SHIFT
                        : ContainerPatternTerm.MULTIPLE_OF_BUTTON_CLICK) * ((val & 2) != 0 ? -1 : 1));
    }

    public void multiplyOrDivideStacks(int multi) {
        if (!isCraftingMode()) {
            if (ContainerPatternTerm.canMultiplyOrDivide(this.craftingSlots, multi)
                    && ContainerPatternTerm.canMultiplyOrDivide(this.outputSlotsArray, multi)) {
                ContainerPatternTerm.multiplyOrDivideStacksInternal(this.craftingSlots, multi);
                ContainerPatternTerm.multiplyOrDivideStacksInternal(this.outputSlotsArray, multi);
            }
            this.detectAndSendChanges();
        }
    }

    @Override
    public void onContainerClosed(final EntityPlayer player) {
        // Автоматически сохраняем изменения при закрытии GUI
        if (originalPatternStack != null && sourceEntryId >= 0) {
            encodePattern(); // Сохраняем текущее состояние
        }
        super.onContainerClosed(player);
    }
}
