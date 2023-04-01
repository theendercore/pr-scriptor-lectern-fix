package com.ssblur.scriptor.item;

import com.ssblur.scriptor.events.ScriptorEvents;
import com.ssblur.scriptor.helpers.DictionarySavedData;
import com.ssblur.scriptor.helpers.LimitedBookSerializer;
import com.ssblur.scriptor.helpers.targetable.ItemTargetable;
import com.ssblur.scriptor.messages.EnchantNetwork;
import com.ssblur.scriptor.registry.WordRegistry;
import com.ssblur.scriptor.word.Spell;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Spellbook extends Item {
  public Spellbook(Properties properties) {
    super(properties);
  }

  @Override
  public Component getName(ItemStack itemStack) {
    CompoundTag compoundTag = itemStack.getTag();
    if (compoundTag != null) {
      String string = compoundTag.getString("title");
      if (!StringUtil.isNullOrEmpty(string)) {
        return Component.literal(string);
      }
    }

    return super.getName(itemStack);
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
    var result = super.use(level, player, interactionHand);

    Tag tag = player.getItemInHand(interactionHand).getTag();
    if(tag instanceof CompoundTag compound && level instanceof ServerLevel server) {
      var text = compound.getList("pages", Tag.TAG_STRING);
      Spell spell = DictionarySavedData.computeIfAbsent(server).parse(LimitedBookSerializer.decodeText(text));
      if(spell != null) {
        if(spell.cost() > 50) {
          player.sendSystemMessage(Component.translatable("extra.scriptor.fizzle"));
          player.getCooldowns().addCooldown(this, 350);
        }
        spell.cast(player);
        if(!player.isCreative())
          player.getCooldowns().addCooldown(this, (int) Math.round(spell.cost() * 7));
      }
    }

    return result;
  }

  public boolean overrideStackedOnOther(ItemStack itemStack, Slot slot, ClickAction clickAction, Player player) {
    int i = 0;
    if (clickAction == ClickAction.SECONDARY && !slot.getItem().isEmpty()) {
      if(player.getCooldowns().isOnCooldown(this)) return true;
      if(player.isCreative())
        EnchantNetwork.clientUseBookCreative();
      else
        EnchantNetwork.clientUseBook(slot.index);
      return true;
    }
    return false;
  }

  @Override
  public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> list, TooltipFlag tooltipFlag) {
    super.appendHoverText(itemStack, level, list, tooltipFlag);

    if(itemStack.getTag() != null && itemStack.getTag().getCompound("scriptor") != null) {
      var scriptor = itemStack.getTag().getCompound("scriptor");
      if(scriptor.contains("identified")) {
        if(Screen.hasShiftDown())
          for(var key: scriptor.getCompound("identified").getAllKeys()) {
            String[] parts = key.split(":", 2);
            list.add(Component.translatable(parts[0] + ".scriptor." + parts[1]));
          }
        else
          list.add(Component.translatable("extra.scriptor.tome_identified"));
      }
    }
  }
}
