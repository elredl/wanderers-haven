package com.wanderershaven.item;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

public final class QuiverItem extends Item {

	private static final int SLOT_COUNT = 5;

	public QuiverItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}

		QuiverContainer container = new QuiverContainer(stack);
		MenuProvider provider = new net.minecraft.world.SimpleMenuProvider((syncId, inventory, ignored) ->
			new HopperMenu(syncId, inventory, container) {
				@Override
				public void removed(Player closingPlayer) {
					super.removed(closingPlayer);
					container.saveToStack();
				}
			},
			Component.translatable("item.wanderers_haven.quiver"));
		serverPlayer.openMenu(provider);
		return InteractionResult.CONSUME;
	}

	private static final class QuiverContainer extends SimpleContainer {

		private final ItemStack stack;

		private QuiverContainer(ItemStack stack) {
			super(SLOT_COUNT);
			this.stack = stack;

			ItemContainerContents savedContents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
			NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
			savedContents.copyInto(items);
			for (int slot = 0; slot < SLOT_COUNT; slot++) {
				setItem(slot, items.get(slot).copy());
			}
		}

		@Override
		public void setChanged() {
			super.setChanged();
			saveToStack();
		}

		@Override
		public boolean canPlaceItem(int slot, ItemStack stack) {
			return stack.getItem() instanceof ArrowItem;
		}

		private void saveToStack() {
			List<ItemStack> items = new ArrayList<>(SLOT_COUNT);
			for (int slot = 0; slot < SLOT_COUNT; slot++) {
				items.add(getItem(slot).copy());
			}
			stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
		}
	}
}
