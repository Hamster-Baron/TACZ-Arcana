package group.taczexpands.server.mixin.accessor;

import com.tacz.guns.util.block.BlockRayTrace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

@Mixin(BlockRayTrace.class)
public interface IAccessorBlockRayTrace {
    @Invoker("performRayTrace")
    static <T> T performRayTrace(ClipContext context, BiFunction<ClipContext, BlockPos, T> hitFunction, Function<ClipContext, T> missFactory) {
        return null;
    }

    @Invoker("getBlockHitResult")
    static BlockHitResult getBlockHitResult(Level level, ClipContext rayTraceContext, BlockPos blockPos, BlockState blockState) {
        return null;
    }

    @Accessor("IGNORES")
    static Predicate<BlockState> getIGNORES() {
        return null;
    }
}
