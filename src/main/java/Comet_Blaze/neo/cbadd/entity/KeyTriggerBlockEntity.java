package Comet_Blaze.neo.cbadd.entity;

import Comet_Blaze.neo.cbadd.init.CbaddModBlockEntities;
import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class KeyTriggerBlockEntity extends BlockEntity {

    public static class BindEntry {
        public final BlockPos relativePos;
        public final String key;

        public BindEntry(BlockPos relativePos, String key) {
            this.relativePos = relativePos;
            this.key = key;
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("RX", relativePos.getX());
            tag.putInt("RY", relativePos.getY());
            tag.putInt("RZ", relativePos.getZ());
            tag.putString("Key", key);
            return tag;
        }

        public static BindEntry fromTag(CompoundTag tag) {
            BlockPos rel = new BlockPos(tag.getInt("RX"), tag.getInt("RY"), tag.getInt("RZ"));
            String key = tag.getString("Key");
            return new BindEntry(rel, key);
        }
    }

    private final List<BindEntry> binds = new ArrayList<>();

    // 记录当前被按下的按键集合（客户端GUI中按住）
    private final Set<String> activeKeys = Sets.newHashSet();
    // 超时保护：上一次收到任何包的时间（游戏刻）
    private long lastPacketTick = 0;
    private static final long TIMEOUT_TICKS = 100; // 5秒

    public KeyTriggerBlockEntity(BlockPos pos, BlockState state) {
        super(CbaddModBlockEntities.KEY_TRIGGER.get(), pos, state);
    }

    /**
     * 公开方法：获取所有绑定条目（只读列表）
     */
    public List<BindEntry> getBinds() {
        return Collections.unmodifiableList(binds);
    }

    public void loadBindsFromItem(ListTag bindListTag) {
        binds.clear();
        for (int i = 0; i < bindListTag.size(); i++) {
            CompoundTag entry = bindListTag.getCompound(i);
            BlockPos absolute = new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z"));
            String key = entry.getString("Key");
            BlockPos relative = absolute.subtract(worldPosition);
            binds.add(new BindEntry(relative, key));
        }
        setChanged();
    }

    public void onKeyPressed(String key) {
        if (level == null || level.isClientSide) return;
        activeKeys.add(key);
        lastPacketTick = level.getGameTime();
        updateOutputs();
        setChanged();
    }

    public void onKeyReleased(String key) {
        if (level == null || level.isClientSide) return;
        activeKeys.remove(key);
        lastPacketTick = level.getGameTime();
        updateOutputs();
        setChanged();
    }

    /**
     * 修复：聚合每个输出方块的按键状态，防止互相覆盖
     */
    private void updateOutputs() {
        if (level == null || level.isClientSide) return;

        // 汇总每个输出方块是否应该激活
        Map<BlockPos, Boolean> outputStates = new HashMap<>();
        for (BindEntry entry : binds) {
            BlockPos outputPos = worldPosition.offset(entry.relativePos);
            if (level.isLoaded(outputPos)) {
                boolean thisKeyActive = activeKeys.contains(entry.key);
                // 使用 OR 逻辑：只要有一个按键按下，该输出方块就应该激活
                outputStates.merge(outputPos, thisKeyActive, (oldVal, newVal) -> oldVal || newVal);
            }
        }

        // 统一设置信号
        for (Map.Entry<BlockPos, Boolean> entry : outputStates.entrySet()) {
            BlockPos outputPos = entry.getKey();
            boolean signalOn = entry.getValue();
            BlockEntity be = level.getBlockEntity(outputPos);
            if (be instanceof ConnectorTileEntity outputTile && !outputTile.isInput()) {
                outputTile.setSignal(signalOn ? 15 : 0);
            }
        }
    }

    public void tick() {
        if (level == null || level.isClientSide) return;
        if (!activeKeys.isEmpty() && (level.getGameTime() - lastPacketTick) > TIMEOUT_TICKS) {
            activeKeys.clear();
            updateOutputs();
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (BindEntry entry : binds) {
            list.add(entry.toTag());
        }
        tag.put("BindList", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        binds.clear();
        if (tag.contains("BindList", Tag.TAG_LIST)) {
            ListTag list = tag.getList("BindList", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                binds.add(BindEntry.fromTag(list.getCompound(i)));
            }
        }
    }
}
