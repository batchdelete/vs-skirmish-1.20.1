package madmike.skirmish.component.components.player;

import com.tiviacz.travelersbackpack.component.ComponentUtils;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

import java.util.HashMap;
import java.util.Map;

public class PlayerInventoryComponent implements ComponentV3 {

    // VANILLA
    private final DefaultedList<ItemStack> storedMain =
            DefaultedList.ofSize(36, ItemStack.EMPTY);
    private final DefaultedList<ItemStack> storedArmor =
            DefaultedList.ofSize(4, ItemStack.EMPTY);
    private final DefaultedList<ItemStack> storedOffhand =
            DefaultedList.ofSize(1, ItemStack.EMPTY);

    // TRAVELERS BACKPACK
    private ItemStack storedBackpack = ItemStack.EMPTY;

    // TRINKETS: group -> slot -> list
    private final Map<String, Map<String, DefaultedList<ItemStack>>> storedTrinkets =
            new HashMap<>();


    // ============================================================
    // SAVE INVENTORY
    // ============================================================
    public void saveInventory(ServerPlayerEntity player) {

        PlayerInventory inv = player.getInventory();

        // Vanilla
        for (int i = 0; i < 36; i++)
            storedMain.set(i, inv.getStack(i).copy());

        for (int i = 0; i < 4; i++)
            storedArmor.set(i, inv.armor.get(i).copy());

        storedOffhand.set(0, inv.offHand.get(0).copy());

        // Backpack
        ItemStack bc = ComponentUtils.getWearingBackpack(player);
        if (bc != null ) {
            storedBackpack = bc.copy();
        } else {
            storedBackpack = ItemStack.EMPTY;
        }

        // ---- TRINKETS ----
        storedTrinkets.clear();

        TrinketComponent trinkets =
                TrinketsApi.getTrinketComponent(player).orElse(null);

        if (trinkets != null) {
            for (var groupEntry : trinkets.getInventory().entrySet()) {
                String group = groupEntry.getKey();
                storedTrinkets.put(group, new HashMap<>());

                for (var slotEntry : groupEntry.getValue().entrySet()) {
                    String slot = slotEntry.getKey();
                    TrinketInventory originalList = slotEntry.getValue();

                    DefaultedList<ItemStack> copyList =
                            DefaultedList.ofSize(originalList.size(), ItemStack.EMPTY);

                    for (int i = 0; i < originalList.size(); i++)
                        copyList.set(i, originalList.getStack(i).copy());

                    storedTrinkets.get(group).put(slot, copyList);
                }
            }
        }
    }

    // ============================================================
    // RESTORE INVENTORY
    // ============================================================
    public void restoreInventory(ServerPlayerEntity player) {

        PlayerInventory inv = player.getInventory();

        // Vanilla
        for (int i = 0; i < 36; i++)
            inv.setStack(i, storedMain.get(i).copy());

        for (int i = 0; i < 4; i++)
            inv.armor.set(i, storedArmor.get(i).copy());

        inv.offHand.set(0, storedOffhand.get(0).copy());

        // Backpack
        if (!storedBackpack.isEmpty()) {
            ComponentUtils.equipBackpack(player, storedBackpack);
        }

        // ---- TRINKETS ----
        TrinketComponent trinkets =
                TrinketsApi.getTrinketComponent(player).orElse(null);

        if (trinkets != null) {
            for (var groupEntry : storedTrinkets.entrySet()) {
                String group = groupEntry.getKey();

                for (var slotEntry : groupEntry.getValue().entrySet()) {
                    String slot = slotEntry.getKey();
                    DefaultedList<ItemStack> list = slotEntry.getValue();

                    TrinketInventory target = trinkets.getInventory().get(group).get(slot);

                    for (int i = 0; i < list.size(); i++)
                        target.setStack(i, list.get(i).copy());
                }
            }
        }

        inv.markDirty();
        player.currentScreenHandler.sendContentUpdates();

        if (trinkets != null)
            trinkets.update();
    }

    // ============================================================
    // NBT SAVE
    // ============================================================
    @Override
    public void writeToNbt(NbtCompound tag) {

        // VANILLA
        Inventories.writeNbt(tag, storedMain);
        tag.put("Armor", Inventories.writeNbt(new NbtCompound(), storedArmor));
        tag.put("Offhand", Inventories.writeNbt(new NbtCompound(), storedOffhand));

        // BACKPACK ITEM
        if (!storedBackpack.isEmpty()) {
            tag.put("Backpack", storedBackpack.writeNbt(new NbtCompound()));
        }

        // TRINKETS
        NbtCompound trinketTag = new NbtCompound();

        for (var groupEntry : storedTrinkets.entrySet()) {
            NbtCompound groupTag = new NbtCompound();

            for (var slotEntry : groupEntry.getValue().entrySet()) {
                NbtCompound slotTag = Inventories.writeNbt(new NbtCompound(), slotEntry.getValue());
                groupTag.put(slotEntry.getKey(), slotTag);
            }

            trinketTag.put(groupEntry.getKey(), groupTag);
        }

        tag.put("Trinkets", trinketTag);
    }

    // ============================================================
    // NBT LOAD
    // ============================================================
    @Override
    public void readFromNbt(NbtCompound tag) {

        Inventories.readNbt(tag, storedMain);
        Inventories.readNbt(tag.getCompound("Armor"), storedArmor);
        Inventories.readNbt(tag.getCompound("Offhand"), storedOffhand);

        if (tag.contains("Backpack"))
            storedBackpack = ItemStack.fromNbt(tag.getCompound("Backpack"));
        else
            storedBackpack = ItemStack.EMPTY;

        // ---- TRINKETS ----
        storedTrinkets.clear();

        NbtCompound trinketTag = tag.getCompound("Trinkets");
        for (String group : trinketTag.getKeys()) {
            NbtCompound groupTag = trinketTag.getCompound(group);

            storedTrinkets.put(group, new HashMap<>());

            for (String slot : groupTag.getKeys()) {
                NbtCompound slotTag = groupTag.getCompound(slot);

                // Count entries in the saved list
                int size = slotTag.getList("Items", NbtElement.COMPOUND_TYPE).size();

                // Rebuild the slot list
                DefaultedList<ItemStack> list =
                        DefaultedList.ofSize(size, ItemStack.EMPTY);

                Inventories.readNbt(slotTag, list);

                storedTrinkets.get(group).put(slot, list);
            }
        }
    }
}
