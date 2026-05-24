package Comet_Blaze.neo.cbadd.entity;

import Comet_Blaze.neo.cbadd.block.ConnectorBlock;
import Comet_Blaze.neo.cbadd.block.WightlessConnectorBlock;
import Comet_Blaze.neo.cbadd.init.CbaddModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class ConnectorTileEntity extends BlockEntity {
    private final List<BlockPos> connectedOutputsRelative = new ArrayList<>();
    private final List<BlockPos> connectedInputsRelative = new ArrayList<>();
    //WightlessConnectorBlock
    private final List<BlockPos> pairedConnectorsRelative = new ArrayList<>();

    private int signal = 0;
    private boolean isInput;

    public ConnectorTileEntity(BlockPos pos, BlockState state) {
        super(CbaddModBlockEntities.CONNECTOR.get(), pos, state);
        if (state.getBlock() instanceof ConnectorBlock connectorBlock) {
            this.isInput = connectorBlock.isInput;
        } else {
            this.isInput = false;
        }
    }

    public boolean isWightless() {
        return this.getBlockState().getBlock() instanceof WightlessConnectorBlock;
    }
    public static int getMaxOutputs() {
        return Comet_Blaze.neo.cbadd.config.RedstoneConnectorConfig.MAX_OUTPUTS_PER_INPUT.get();
    }

    public boolean addOutput(BlockPos outputPos) {
        if (!isInput) return false;
        if (connectedOutputsRelative.size() >= getMaxOutputs()) return false;
        BlockPos relative = outputPos.subtract(worldPosition);
        if (connectedOutputsRelative.contains(relative)) return false;
        connectedOutputsRelative.add(relative);
        setChanged();
        return true;
    }

    public boolean removeOutput(BlockPos outputPos) {
        if (!isInput) return false;
        BlockPos relative = outputPos.subtract(worldPosition);
        boolean removed = connectedOutputsRelative.remove(relative);
        if (removed) setChanged();
        return removed;
    }

    public boolean containsOutput(BlockPos outputPos) {
        if (!isInput) return false;
        return connectedOutputsRelative.contains(outputPos.subtract(worldPosition));
    }

    public List<BlockPos> getConnectedOutputs() {
        List<BlockPos> absoluteList = new ArrayList<>();
        for (BlockPos relative : connectedOutputsRelative) {
            absoluteList.add(worldPosition.offset(relative));
        }
        return absoluteList;
    }

    private void updateOutputs() {
        if (level == null || level.isClientSide || !isInput) return;
        for (BlockPos relative : connectedOutputsRelative) {
            BlockPos outputPos = worldPosition.offset(relative);
            if (isWithinDistance(outputPos, 128) && level.isLoaded(outputPos)) {
                BlockEntity be = level.getBlockEntity(outputPos);
                if (be instanceof ConnectorTileEntity outputTile && !outputTile.isInput()) {
                    outputTile.recalculateSignal();
                }
            }
        }
    }

    public void notifyOutputsRemoved() {
        if (level == null || level.isClientSide || !isInput) return;
        for (BlockPos relative : connectedOutputsRelative) {
            BlockPos outputPos = worldPosition.offset(relative);
            if (isWithinDistance(outputPos, 128) && level.isLoaded(outputPos)) {
                BlockEntity be = level.getBlockEntity(outputPos);
                if (be instanceof ConnectorTileEntity outputTile && !outputTile.isInput()) {
                    outputTile.removeInput(this.worldPosition);
                }
            }
        }
        connectedOutputsRelative.clear();
        setChanged();
    }
    public boolean addInput(BlockPos inputPos) {
        if (isInput) return false;
        BlockPos relative = inputPos.subtract(worldPosition);
        if (connectedInputsRelative.contains(relative)) return false;
        connectedInputsRelative.add(relative);
        setChanged();
        recalculateSignal();
        return true;
    }

    public boolean removeInput(BlockPos inputPos) {
        if (isInput) return false;
        BlockPos relative = inputPos.subtract(worldPosition);
        boolean removed = connectedInputsRelative.remove(relative);
        if (removed) {
            setChanged();
            recalculateSignal();
        }
        return removed;
    }

    public boolean containsInput(BlockPos inputPos) {
        if (isInput) return false;
        return connectedInputsRelative.contains(inputPos.subtract(worldPosition));
    }

    public List<BlockPos> getConnectedInputs() {
        List<BlockPos> absoluteList = new ArrayList<>();
        for (BlockPos relative : connectedInputsRelative) {
            absoluteList.add(worldPosition.offset(relative));
        }
        return absoluteList;
    }

    public void recalculateSignal() {
        if (level == null || level.isClientSide || isInput) return;
        int maxSignal = 0;
        for (BlockPos relative : connectedInputsRelative) {
            BlockPos inputPos = worldPosition.offset(relative);
            if (isWithinDistance(inputPos, 128) && level.isLoaded(inputPos)) {
                BlockEntity be = level.getBlockEntity(inputPos);
                if (be instanceof ConnectorTileEntity inputTile && inputTile.isInput()) {
                    maxSignal = Math.max(maxSignal, inputTile.getSignal());
                }
            }
        }
        for (BlockPos relative : pairedConnectorsRelative) {
            BlockPos pairedPos = worldPosition.offset(relative);
            if (isWithinDistance(pairedPos, 128) && level.isLoaded(pairedPos)) {
                BlockEntity be = level.getBlockEntity(pairedPos);
                if (be instanceof ConnectorTileEntity pairedTile && pairedTile.isWightless()) {
                    maxSignal = Math.max(maxSignal, pairedTile.signal);
                }
            }
        }

        setSignal(maxSignal);
    }

    public void notifyInputsRemoved() {
        if (level == null || level.isClientSide || isInput) return;
        for (BlockPos relative : connectedInputsRelative) {
            BlockPos inputPos = worldPosition.offset(relative);
            if (isWithinDistance(inputPos, 128) && level.isLoaded(inputPos)) {
                BlockEntity be = level.getBlockEntity(inputPos);
                if (be instanceof ConnectorTileEntity inputTile && inputTile.isInput()) {
                    inputTile.removeOutput(this.worldPosition);
                }
            }
        }
        connectedInputsRelative.clear();
        setChanged();
    }

    public boolean addPairedConnector(BlockPos otherPos) {
        if (!this.isWightless()) return false;
        BlockPos relative = otherPos.subtract(worldPosition);
        if (pairedConnectorsRelative.contains(relative)) return false;
        pairedConnectorsRelative.add(relative);
        setChanged();
        recalculateSignal();
        return true;
    }

    public boolean removePairedConnector(BlockPos otherPos) {
        if (!this.isWightless()) return false;
        BlockPos relative = otherPos.subtract(worldPosition);
        boolean removed = pairedConnectorsRelative.remove(relative);
        if (removed) {
            setChanged();
            recalculateSignal();
        }
        return removed;
    }

    public boolean isPairedWith(BlockPos otherPos) {
        return pairedConnectorsRelative.contains(otherPos.subtract(worldPosition));
    }

    public List<BlockPos> getPairedConnectors() {
        List<BlockPos> absoluteList = new ArrayList<>();
        for (BlockPos relative : pairedConnectorsRelative) {
            absoluteList.add(worldPosition.offset(relative));
        }
        return absoluteList;
    }

    public int getSignal() { return signal; }

    public void setSignal(int signal) {
        if (this.signal == signal) return;
        this.signal = signal;
        setChanged();

        if (level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(ConnectorBlock.POWER) && state.getValue(ConnectorBlock.POWER) != signal) {
                level.setBlock(worldPosition, state.setValue(ConnectorBlock.POWER, signal), 3);
            }
        }

        if (level != null && !level.isClientSide && this.isWightless()) {
            for (BlockPos relative : pairedConnectorsRelative) {
                BlockPos pairedPos = worldPosition.offset(relative);
                if (level.isLoaded(pairedPos)) {
                    BlockEntity be = level.getBlockEntity(pairedPos);
                    if (be instanceof ConnectorTileEntity paired && paired.isWightless()) {
                        paired.setSignal(signal);
                    }
                }
            }
        }
    }

    public boolean isInput() { return isInput; }

    public void onNeighborChange() {
        if (level == null || level.isClientSide || !isInput) return;
        int newSignal = level.getBestNeighborSignal(worldPosition);
        if (newSignal != this.signal) {
            setSignal(newSignal);
            updateOutputs();
        }
    }

    private boolean isWithinDistance(BlockPos other, int maxDist) {
        int dx = Math.abs(other.getX() - worldPosition.getX());
        int dz = Math.abs(other.getZ() - worldPosition.getZ());
        return dx <= maxDist && dz <= maxDist;
    }

    //save NBT
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Signal", signal);
        tag.putBoolean("IsInput", isInput);

        ListTag list = new ListTag();
        if (isInput) {
            for (BlockPos relative : connectedOutputsRelative) {
                CompoundTag posTag = new CompoundTag();
                posTag.putInt("X", relative.getX());
                posTag.putInt("Y", relative.getY());
                posTag.putInt("Z", relative.getZ());
                list.add(posTag);
            }
            tag.put("ConnectedOutputsRelative", list);
        } else {
            for (BlockPos relative : connectedInputsRelative) {
                CompoundTag posTag = new CompoundTag();
                posTag.putInt("X", relative.getX());
                posTag.putInt("Y", relative.getY());
                posTag.putInt("Z", relative.getZ());
                list.add(posTag);
            }
            tag.put("ConnectedInputsRelative", list);
        }

        if (!pairedConnectorsRelative.isEmpty()) {
            ListTag pairedList = new ListTag();
            for (BlockPos relative : pairedConnectorsRelative) {
                CompoundTag posTag = new CompoundTag();
                posTag.putInt("X", relative.getX());
                posTag.putInt("Y", relative.getY());
                posTag.putInt("Z", relative.getZ());
                pairedList.add(posTag);
            }
            tag.put("PairedConnectorsRelative", pairedList);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        signal = tag.getInt("Signal");
        isInput = tag.getBoolean("IsInput");

        if (isInput) {
            connectedOutputsRelative.clear();
            if (tag.contains("ConnectedOutputsRelative", Tag.TAG_LIST)) {
                ListTag listTag = tag.getList("ConnectedOutputsRelative", Tag.TAG_COMPOUND);
                for (int i = 0; i < listTag.size(); i++) {
                    CompoundTag posTag = listTag.getCompound(i);
                    connectedOutputsRelative.add(
                            new BlockPos(posTag.getInt("X"), posTag.getInt("Y"), posTag.getInt("Z")));
                }
            }
        } else {
            connectedInputsRelative.clear();
            if (tag.contains("ConnectedInputsRelative", Tag.TAG_LIST)) {
                ListTag listTag = tag.getList("ConnectedInputsRelative", Tag.TAG_COMPOUND);
                for (int i = 0; i < listTag.size(); i++) {
                    CompoundTag posTag = listTag.getCompound(i);
                    connectedInputsRelative.add(
                            new BlockPos(posTag.getInt("X"), posTag.getInt("Y"), posTag.getInt("Z")));
                }
            }
        }

        pairedConnectorsRelative.clear();
        if (tag.contains("PairedConnectorsRelative", Tag.TAG_LIST)) {
            ListTag listTag = tag.getList("PairedConnectorsRelative", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag posTag = listTag.getCompound(i);
                pairedConnectorsRelative.add(
                        new BlockPos(posTag.getInt("X"), posTag.getInt("Y"), posTag.getInt("Z")));
            }
        }
    }

    public void tick() {}
}
