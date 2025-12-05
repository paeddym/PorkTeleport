package paeddym.porkteleport;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.Set;

public class PorkTeleport implements ModInitializer {
    public static final String MOD_ID = "porkteleport";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    @Override
    public void onInitialize() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            
            if (!world.isClient && (stack.isOf(Items.PORKCHOP) || stack.isOf(Items.COOKED_PORKCHOP))) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    teleportToNether(serverPlayer);
                    // Consume one porkchop
                    stack.decrement(1);
                    return ActionResult.SUCCESS;
                }
            }
            
            return ActionResult.PASS;
        });
    }
    
    private void teleportToNether(ServerPlayerEntity player) {
        ServerWorld destWorld = player.getServer().getWorld(World.NETHER);
        if (destWorld == null) return;
        
        BlockPos spawnPos = destWorld.getSpawnPos();
        
        // Find a safe position - start at spawn and look for air blocks
        BlockPos safePos = findSafePosition(destWorld, spawnPos);
        
        if (safePos != null) {
            player.teleport(
                destWorld,
                safePos.getX() + 0.5,
                safePos.getY(),
                safePos.getZ() + 0.5,
                Set.of(),
                player.getYaw(),
                player.getPitch(),
                false
            );
            LOGGER.info("Teleported {} to Nether at {}", player.getName().getString(), safePos);
        }
    }
    
    private BlockPos findSafePosition(ServerWorld world, BlockPos start) {
        // Search in a spiral pattern around the spawn point
        int maxRadius = 16;
        
        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int xOffset = -radius; xOffset <= radius; xOffset++) {
                for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                    // Only check the outer ring of the current radius
                    if (Math.abs(xOffset) != radius && Math.abs(zOffset) != radius) {
                        continue;
                    }
                    
                    int x = start.getX() + xOffset;
                    int z = start.getZ() + zOffset;
                    
                    // Search vertically at this x,z coordinate
                    BlockPos safePos = findSafeYLevel(world, x, z, start.getY());
                    if (safePos != null) {
                        return safePos;
                    }
                }
            }
        }
        
        // Last resort: find any safe position nearby
        return findSafeYLevel(world, start.getX(), start.getZ(), 64);
    }
    
    private BlockPos findSafeYLevel(ServerWorld world, int x, int z, int startY) {
        int minY = world.getBottomSectionCoord() * 16;
        int maxY = world.getTopSectionCoord() * 16;
        
        // Search down first
        for (int y = startY; y > minY + 1; y--) {
            if (isSafePosition(world, x, y, z)) {
                return new BlockPos(x, y, z);
            }
        }
        
        // Search up
        for (int y = startY; y < maxY - 2; y++) {
            if (isSafePosition(world, x, y, z)) {
                return new BlockPos(x, y, z);
            }
        }
        
        return null;
    }
    
    private boolean isSafePosition(ServerWorld world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockPos above = pos.up();
        BlockPos below = pos.down();
        
        // Check that:
        // 1. The position at feet is air or passable
        // 2. The position at head is air or passable  
        // 3. The position below is solid (not air, not lava, not fire)
        // 4. Not in lava or fire at feet level
        
        boolean feetClear = world.getBlockState(pos).isAir() || 
                           world.getBlockState(pos).getCollisionShape(world, pos).isEmpty();
        boolean headClear = world.getBlockState(above).isAir() || 
                           world.getBlockState(above).getCollisionShape(world, above).isEmpty();
        boolean solidGround = !world.getBlockState(below).isAir() && 
                             !world.getBlockState(below).getCollisionShape(world, below).isEmpty();
        boolean notInLava = !world.getBlockState(pos).isLiquid();
        boolean notInFire = !world.getBlockState(pos).toString().contains("fire");
        
        return feetClear && headClear && solidGround && notInLava && notInFire;
    }
}