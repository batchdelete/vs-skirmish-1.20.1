package madmike.skirmish.event.events;

import madmike.skirmish.logic.SkirmishManager;

public class ShipHelmBlockDestroyedEvent {
    public static void register() {
        ShipHelmDestroyedCallback.EVENT.register((world, pos, state, player, ship) -> {
            if (ship == null) return;

            SkirmishManager manager = SkirmishManager.INSTANCE;

            if (manager.isShipInSkirmish(ship.getId())) {
                manager.endSkirmishForShip(player.getServer(), ship.getId());
            }
        });
    }
}
