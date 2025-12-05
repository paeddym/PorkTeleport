package paeddym.porkteleport;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import java.util.Set;
import net.minecraft.entity.Entity.RemovalReason;
 
public class PorkTeleport implements ModInitializer {
    public static final String MOD_ID = "porkteleport";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    @Override
    public void onInitialize() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!world.isClient && (stack.isOf(Items.PORKCHOP) || stack.isOf(Items.COOKED_PORKCHOP))) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    ServerWorld destWorld = serverPlayer.getServer().getWorld(World.NETHER);
                    if (destWorld != null) {
                        serverPlayer.teleport(
                            destWorld,
                            destWorld.getSpawnPos().getX() + 0.5,
                            destWorld.getSpawnPos().getY(),
                            destWorld.getSpawnPos().getZ() + 0.5,
                            Set.of(),
                            serverPlayer.getYaw(),
                            serverPlayer.getPitch(),
                            false
                        );
                    }
                }
            }
            return ActionResult.SUCCESS;
        });
    }
}