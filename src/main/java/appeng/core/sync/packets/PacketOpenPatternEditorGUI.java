package appeng.core.sync.packets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import appeng.api.networking.IGridHost;
import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.INetworkInfo;
import appeng.helpers.IContainerCraftingPacket;
import appeng.util.Platform;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PacketOpenPatternEditorGUI extends AppEngPacket {

    private final GuiBridge targetGui;
    private final long interfaceId;
    private final int slotIndex;
    private final ItemStack patternStack;

    public PacketOpenPatternEditorGUI(final ByteBuf stream) {
        targetGui = GuiBridge.values()[stream.readInt()];
        interfaceId = stream.readLong();
        slotIndex = stream.readInt();

        ItemStack tempPatternStack = null;
        final DataInputStream dis = new DataInputStream(
                new ByteArrayInputStream(stream.array(), stream.readerIndex(), stream.readableBytes()));
        try {
            NBTTagCompound nbt = readNBTFromStream(dis);
            if (nbt != null) {
                tempPatternStack = ItemStack.loadItemStackFromNBT(nbt);
            }
        } catch (IOException e) {
        }
        patternStack = tempPatternStack;
    }

    public PacketOpenPatternEditorGUI(GuiBridge gui, long interfaceId, int slotIndex, ItemStack patternStack) {
        this.targetGui = gui;
        this.interfaceId = interfaceId;
        this.slotIndex = slotIndex;
        this.patternStack = patternStack != null ? patternStack.copy() : null;

        final ByteBuf data = Unpooled.buffer();

        data.writeInt(getPacketID());
        data.writeInt(targetGui.ordinal());
        data.writeLong(interfaceId);
        data.writeInt(slotIndex);

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(bos);

        try {
            if (patternStack != null) {
                NBTTagCompound nbt = new NBTTagCompound();
                patternStack.writeToNBT(nbt);
                writeNBTToStream(nbt, dos);
            } else {
                writeNBTToStream(null, dos);
            }
        } catch (IOException ignored) {}

        data.writeBytes(bos.toByteArray());

        configureWrite(data);
    }

    @Override
    public void serverPacketData(INetworkInfo manager, AppEngPacket packet, EntityPlayer player) {
        if (player.openContainer instanceof ContainerInterfaceTerminal container) {
            if (patternStack != null) {
                final Object target = container.getTarget();
                if (target instanceof IGridHost) {
                    final ContainerOpenContext context = container.getOpenContext();
                    if (context != null) {
                        final TileEntity te = null;

                        Platform.openGUI(player, te, container.getOpenContext().getSide(), targetGui);
                        System.out.println("===DEBUG INFO===");
                        System.out.println("Interface id: " + interfaceId);
                        System.out.println("Slot index: " + slotIndex);
                        System.out.println("Target gui: " + targetGui);
                        System.out.println("Target gui name: " + targetGui.name());
                        System.out.println("Pattern stack: " + patternStack);
                        System.out.println("Tile entity: " + te);
                        System.out.println("Tile entity class: " + (te != null ? te.getClass().getName() : "null"));
                        System.out.println("Side: " + container.getOpenContext().getSide());
                        System.out.println("Player: " + player.getDisplayName());
                        System.out.println("==========");
                    }
                }
            }
        }
    }


    private NBTTagCompound readNBTFromStream(DataInputStream dis) throws IOException {
        short length = dis.readShort();
        if (length == -1) {
            return null;
        }
        byte[] data = new byte[length];
        dis.readFully(data);
        return net.minecraft.nbt.CompressedStreamTools.func_152457_a(data, NBTSizeTracker.field_152451_a);
    }

    private void writeNBTToStream(NBTTagCompound nbt, DataOutputStream dos) throws IOException {
        if (nbt == null) {
            dos.writeShort(-1);
            return;
        }
        byte[] data = net.minecraft.nbt.CompressedStreamTools.compress(nbt);
        dos.writeShort(data.length);
        dos.write(data);
    }
}