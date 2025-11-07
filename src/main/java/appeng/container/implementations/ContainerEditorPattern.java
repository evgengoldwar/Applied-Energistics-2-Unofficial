package appeng.container.implementations;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.container.guisync.GuiSync;
import appeng.container.slot.SlotFake;
import appeng.parts.reporting.PartInterfaceTerminal;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.ITerminalHost;
import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotInaccessible;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.item.AEItemStack;
import java.util.List;
import java.util.ArrayList;

public class ContainerEditorPattern extends AEBaseContainer {

    private final Slot patternValue;
    private ICraftingPatternDetails patternDetails;
    private final List<SlotFake> inputSlots = new ArrayList<>();
    private final List<SlotFake> outputSlots = new ArrayList<>();
    private ItemStack originalPatternStack;

    // Слоты для входов (3x3 сетка как в верстаке)
    private static final int INPUT_SLOTS = 9;
    // Слоты для выходов (до 3 слотов)
    private static final int OUTPUT_SLOTS = 3;

    @GuiSync(97)
    public boolean craftingMode = true;

    @GuiSync(96)
    public boolean substitute = false;

    @GuiSync(95)
    public boolean beSubstitute = true;

    public ContainerEditorPattern(final InventoryPlayer ip, final PartInterfaceTerminal te) {
        super(ip, te);

        // Слот для отображения самого паттерна
        patternValue = new SlotInaccessible(new AppEngInternalInventory(null, 1), 0, 34, 113);
        this.addSlotToContainer(patternValue);

        // Создаем слоты для входов (3x3 сетка) - FAKE слоты для взаимодействия
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                SlotFake inputSlot = new SlotFake(new AppEngInternalInventory(null, INPUT_SLOTS), x + y * 3, 18 + x * 18, 17 + y * 18);
                inputSlots.add(inputSlot);
                this.addSlotToContainer(inputSlot);
            }
        }

        // Создаем слоты для выходов - FAKE слоты для взаимодействия
        for (int y = 0; y < OUTPUT_SLOTS; y++) {
            SlotFake outputSlot = new SlotFake(new AppEngInternalInventory(null, OUTPUT_SLOTS), y, 110, 17 + y * 18);
            outputSlots.add(outputSlot);
            this.addSlotToContainer(outputSlot);
        }

        this.bindPlayerInventory(ip, 0, 0);
    }

    public IGrid getGrid() {
        final IActionHost h = ((IActionHost) getTarget());
        return h.getActionableNode().getGrid();
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
                    inputSlots.get(i).putStack(stack.copy());
                }
            }
        }

        // Заполняем выходные слоты
        appeng.api.storage.data.IAEStack[] outputs = patternDetails.getOutputs();
        for (int i = 0; i < Math.min(outputs.length, OUTPUT_SLOTS); i++) {
            if (outputs[i] instanceof appeng.api.storage.data.IAEItemStack aeStack) {
                ItemStack stack = aeStack.getItemStack();
                if (stack != null) {
                    outputSlots.get(i).putStack(stack.copy());
                }
            }
        }

        // Устанавливаем сам паттерн в слот
        if (originalPatternStack != null) {
            patternValue.putStack(originalPatternStack.copy());
        }
    }

    public void clear() {
        for (Slot slot : inputSlots) {
            slot.putStack(null);
        }
        for (Slot slot : outputSlots) {
            slot.putStack(null);
        }
    }

    public void encodePattern() {
        if (originalPatternStack == null) return;

        ItemStack patternStack = originalPatternStack.copy();

        // Получаем входы и выходы из слотов
        final ItemStack[] in = getInputs();
        final ItemStack[] out = getOutputs();

        // Проверяем что есть хотя бы один вход и выход
        if (in == null || out == null) {
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
                    tagIn.appendTag(appeng.util.Platform.writeStackNBT(AEItemStack.create(i), new NBTTagCompound(), true));
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
                    tagOut.appendTag(appeng.util.Platform.writeStackNBT(AEItemStack.create(i), new NBTTagCompound(), true));
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

        // Заменяем оригинальный паттерн
        this.originalPatternStack = patternStack;
        this.patternValue.putStack(patternStack);

        // TODO: Отправить пакет для обновления паттерна в интерфейсном терминале
    }

    private ItemStack[] getInputs() {
        final ItemStack[] input = new ItemStack[9];
        boolean hasValue = false;

        for (int x = 0; x < this.inputSlots.size(); x++) {
            input[x] = this.inputSlots.get(x).getStack();
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
            final ItemStack out = this.outputSlots.get(0).getStack();
            if (out != null && out.stackSize > 0) {
                return new ItemStack[] { out };
            }
        } else {
            // Для processing режима - все выходы
            final List<ItemStack> list = new ArrayList<>(3);
            boolean hasValue = false;

            for (final SlotFake outputSlot : this.outputSlots) {
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
        for (Slot slot : inputSlots) {
            slot.putStack(null);
        }
        for (Slot slot : outputSlots) {
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
    }

    private void updateOutputSlotsVisibility() {
        if (!this.craftingMode) {
            // Processing mode - показываем все 3 слота
            for (int i = 0; i < outputSlots.size(); i++) {
//                outputSlots.get(i).setEnabled(true);
            }
        } else {
            // Crafting mode - показываем только первый слот
            for (int i = 0; i < outputSlots.size(); i++) {
//                outputSlots.get(i).setEnabled(i == 0);
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

}