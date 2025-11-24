package madmike.skirmish.command.exe;

import com.mojang.brigadier.context.CommandContext;
import g_mungus.vlib.v2.api.VLibAPI;
import g_mungus.vlib.v2.api.extension.ShipExtKt;
import madmike.skirmish.VSSkirmish;
import madmike.skirmish.feature.blocks.SkirmishSpawnBlock;
import net.minecraft.block.BlockState;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.concurrent.atomic.AtomicBoolean;

public class SaveExe {
    public static int executeSave(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        // Get party data
        OpenPACServerAPI api = OpenPACServerAPI.get(ctx.getSource().getServer());
        IPartyManagerAPI pm = api.getPartyManager();
        IServerPartyAPI party = pm.getPartyByOwner(player.getUuid());

        if (party == null) {
            player.sendMessage(Text.literal("§cYou are not the owner of a party."), false);
            return 0;
        }

        // Get the ship the player is currently on
        ServerWorld world = player.getServerWorld();
        ServerShip ship = VSGameUtilsKt.getShipManagingPos(world, player.getBlockPos());
        if (ship == null) {
            player.sendMessage(Text.literal("§cYou are not standing on a ship or there was an error saving it"), false);
            return 0;
        }

        // Scan for a Skirmish Spawn Block
        AtomicBoolean found = new AtomicBoolean(false);
        BlockPos[] foundPos = new BlockPos[1]; // mutable container

        ShipExtKt.forEachBlock(ship, blockPos -> {
            if (found.get()) return null;

            BlockState state = world.getBlockState(blockPos);

            if (state.getBlock() instanceof SkirmishSpawnBlock) {
                found.set(true);
                foundPos[0] = blockPos;
            }
            return null;
        });

        if (foundPos[0] == null) {
            player.sendMessage(Text.literal("§cCould not detect a Skirmish Spawn Block, place one where you would like to spawn during a skirmish."), false);
            return 0;
        }

        Identifier filePath = new Identifier(VSSkirmish.MOD_ID, "/ships/" + party.getId());

        VLibAPI.saveShipToTemplate(ship, filePath, world);


        player.sendMessage(Text.literal("§6[Skirmish Save] §7Saved your party ship."), false);
        return 1;
    }
}
