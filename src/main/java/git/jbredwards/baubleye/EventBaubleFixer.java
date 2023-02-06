package git.jbredwards.baubleye;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import baubles.client.gui.GuiBaublesButton;
import cofh.core.enchantment.EnchantmentSoulbound;
import cofh.core.util.helpers.ItemHelper;
import cofh.core.util.helpers.MathHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

/**
 * Fixes the following baubles bugs:
 * -baubles item drops are now added to the player.capturedDrops list when a player dies (can now be manipulated by mods using PlayerDropsEvent)
 * -baubles now drop the same way as all items when a player dies (same math & logic vanilla uses for normal items)
 * -baubles with curse of vanishing now properly disappear when a player dies
 * -cofhcore soulbound now works on items in baubles slots
 * -tombstone soulbound now works on items in baubles slots
 * -baubles gui button now renders for the creative inventory
 *
 * @author jbred
 *
 */
@Mod.EventBusSubscriber(modid = "baubleye")
public final class EventBaubleFixer
{
    @GameRegistry.ObjectHolder("cofhcore:soulbound") public static Enchantment COFH_SOULBOUND = null;
    @GameRegistry.ObjectHolder("tombstone:soulbound") public static Enchantment TOMBSTONE_SOULBOUND = null;

    //properly handle the bauble items dropped when a player dies
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void handleBaublesDeathDrops(@Nonnull PlayerDropsEvent event) {
        final EntityPlayer player = event.getEntityPlayer();
        if(player.world.getGameRules().getBoolean("keepInventory")) return;

        player.captureDrops = true;
        final IBaublesItemHandler baublesInventory = BaublesApi.getBaublesHandler(player);
        for(int slot = 0; slot < baublesInventory.getSlots(); slot++) {
            final ItemStack bauble = baublesInventory.getStackInSlot(slot);
            final boolean hasVanishingCurse = EnchantmentHelper.hasVanishingCurse(bauble);
            final boolean hasAnySoulbound = EnchantmentHelper.getEnchantmentLevel(COFH_SOULBOUND, bauble) > 0
                    || EnchantmentHelper.getEnchantmentLevel(TOMBSTONE_SOULBOUND, bauble) > 0;

            if(!hasVanishingCurse && (player instanceof FakePlayer || !hasAnySoulbound))
                player.dropItem(bauble, true, false);

            if(hasVanishingCurse || player instanceof FakePlayer || !hasAnySoulbound)
                baublesInventory.setStackInSlot(slot, ItemStack.EMPTY);
        }

        player.captureDrops = false;
    }

    //special code for cofh soulbound to decrease the enchantment level each time it gets used
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void keepBaubleSoulboundOnDeath(@Nonnull PlayerEvent.Clone event) {
        if(event.isWasDeath() && COFH_SOULBOUND != null) {
            final EntityPlayer newPlayer = event.getEntityPlayer();
            if(newPlayer instanceof FakePlayer || newPlayer.world.getGameRules().getBoolean("keepInventory")) return;

            final IBaublesItemHandler oldBaubleInventory = BaublesApi.getBaublesHandler(event.getOriginal());
            for(int slot = 0; slot < oldBaubleInventory.getSlots(); slot++) {
                final ItemStack bauble = oldBaubleInventory.getStackInSlot(slot);
                final int level = EnchantmentHelper.getEnchantmentLevel(COFH_SOULBOUND, bauble);

                if(level > 0) {
                    CoFHCoreHelper.handleSoulboundEnchantment(bauble, level);
                    oldBaubleInventory.setStackInSlot(slot, bauble);
                }
            }
        }
    }

    //add the baubles button to the creative player inventory
    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority = EventPriority.NORMAL)
    static void addCreativeBaublesButton(@Nonnull GuiScreenEvent.InitGuiEvent.Post event) {
        if(BaubleyElytra.ConfigHandler.creativeAccessibility && event.getGui() instanceof GuiContainerCreative) {
            final GuiContainerCreative gui = (GuiContainerCreative)event.getGui();
            event.getButtonList().add(new GuiBaublesButton(55, gui, 95, 6, 10, 10, I18n.format("button.baubles")) {
                @Override
                public void drawButton(@Nonnull Minecraft mc, int mouseX, int mouseY, float partialTicks) {
                    visible = gui.getSelectedTabIndex() == CreativeTabs.INVENTORY.getIndex();
                    super.drawButton(mc, mouseX, mouseY, partialTicks);
                }
            });
        }
    }

    //separate from class to prevent issues while CoFHCore isn't installed
    static final class CoFHCoreHelper
    {
        static void handleSoulboundEnchantment(@Nonnull ItemStack stack, int level) {
            if(EnchantmentSoulbound.permanent) {
                if(level > 1) {
                    ItemHelper.removeEnchantment(stack, COFH_SOULBOUND);
                    ItemHelper.addEnchantment(stack, COFH_SOULBOUND, 1);
                }
            }

            else if(MathHelper.RANDOM.nextInt(level + 1) == 0) {
                ItemHelper.removeEnchantment(stack, COFH_SOULBOUND);
                if(level > 1) ItemHelper.addEnchantment(stack, COFH_SOULBOUND, level - 1);
            }
        }
    }
}
