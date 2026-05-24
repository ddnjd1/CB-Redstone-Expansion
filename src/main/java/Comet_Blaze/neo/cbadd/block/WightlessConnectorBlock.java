package Comet_Blaze.neo.cbadd.block;

import Comet_Blaze.neo.cbadd.entity.ConnectorTileEntity;
import Comet_Blaze.neo.cbadd.init.CbaddModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class WightlessConnectorBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    // 碰撞箱：厚度在 FACING 的反方向（朝向玩家），长宽各 12 像素
    private static final VoxelShape SHAPE_NORTH = Block.box(2, 2, 12, 14, 14, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(2, 2, 0, 14, 14, 4);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 2, 2, 4, 14, 14);
    private static final VoxelShape SHAPE_WEST  = Block.box(12, 2, 2, 16, 14, 14);
    private static final VoxelShape SHAPE_UP    = Block.box(2, 0, 2, 14, 4, 14);
    private static final VoxelShape SHAPE_DOWN  = Block.box(2, 12, 2, 14, 16, 14);

    public WightlessConnectorBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.0f)
                .noOcclusion()
                .isValidSpawn((state, getter, pos, type) -> false));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWER);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST  -> SHAPE_EAST;
            case WEST  -> SHAPE_WEST;
            case UP    -> SHAPE_UP;
            case DOWN  -> SHAPE_DOWN;
        };
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getNearestLookingDirection().getOpposite();
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // ---------- 红石行为：像拉杆一样强充能背面 ----------
    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // 弱信号：只向 FACING 的反方向输出 (即充能自身依附的方块)
        if (direction == state.getValue(FACING)) {
            return state.getValue(POWER);
        }
        return 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // 强信号：向 FACING 的反方向输出强充能信号 (即穿透方块)
        if (direction == state.getValue(FACING)) {
            return state.getValue(POWER);
        }
        return 0;
    }

    // ---------- 实体 ----------
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConnectorTileEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
                                                                  BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return type == CbaddModBlockEntities.CONNECTOR.get()
                ? (lvl, pos, st, be) -> ((ConnectorTileEntity) be).tick()
                : null;
    }
}
