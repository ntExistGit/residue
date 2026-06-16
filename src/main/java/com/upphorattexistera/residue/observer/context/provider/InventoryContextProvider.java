package com.upphorattexistera.residue.observer.context.provider;

import com.upphorattexistera.residue.observer.context.ObserverContextProvider;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

public class InventoryContextProvider implements ObserverContextProvider {

    @Override
    public String getId() { return "inventory"; }

    @Override
    public Map<String, String> provide(ServerPlayerEntity player) {
        Map<String, String> map = new LinkedHashMap<>();

        List<String> items = new ArrayList<>();
        player.getInventory().getMainStacks().forEach(stack -> {
            if (!stack.isEmpty())
                items.add(stack.getCount() + "x " + stack.getName().getString());
        });

        map.put("hand_item", player.getMainHandStack().isEmpty()
                ? "nothing" : player.getMainHandStack().getName().getString());

        map.put("offhand_item", player.getOffHandStack().isEmpty()
                ? "nothing" : player.getOffHandStack().getName().getString());

        Collections.shuffle(items);
        map.put("items", items.isEmpty() ? "nothing"
                : String.join(", ", items.subList(0, Math.min(3, items.size()))));

        map.put("inventory_empty", items.isEmpty() ? "true" : "false");

        map.put("has_torch", player.getInventory().containsAny(
                stack -> stack.getItem() == Items.TORCH)
                ? "true" : "false");

        map.put("armor_head",  getArmorName(player.getEquipment(EquipmentSlot.HEAD)));
        map.put("armor_chest", getArmorName(player.getEquipment(EquipmentSlot.CHEST)));
        map.put("armor_legs",  getArmorName(player.getEquipment(EquipmentSlot.LEGS)));
        map.put("armor_feet",  getArmorName(player.getEquipment(EquipmentSlot.FEET)));

        return map;
    }

    private String getArmorName(ItemStack stack) {
        return stack.isEmpty() ? "none" : stack.getName().getString();
    }
}