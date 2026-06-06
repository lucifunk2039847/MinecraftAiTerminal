package com.aiterminal.fabric.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Placeable terminal block (Fabric). Right-clicking opens the terminal screen handler, which the
 * server initiates (so the GUI only appears client-side). Backed by a {@link TerminalBlockEntity}.
 */
public class TerminalBlock extends BaseEntityBlock {

    public static final MapCodec<TerminalBlock> CODEC = simpleCodec(TerminalBlock::new);

    public TerminalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<TerminalBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TerminalBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TerminalBlockEntity terminal) {
                player.openMenu(terminal);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
