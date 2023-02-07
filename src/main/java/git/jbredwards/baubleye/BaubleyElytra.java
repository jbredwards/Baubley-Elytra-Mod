package git.jbredwards.baubleye;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.cap.IBaublesItemHandler;
import baubles.client.gui.GuiPlayerExpanded;
import baubles.common.network.PacketHandler;
import baubles.common.network.PacketOpenBaublesInventory;
import baubles.common.network.PacketOpenNormalInventory;
import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemElytra;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumActionResult;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author jbred
 *
 */
@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.Name("Baubley Elytra Plugin")
@Mod(modid = "baubleye", name = "Baubley Elytra", version = "1.3.1", dependencies = "required-after:baubles")
public final class BaubleyElytra implements IFMLLoadingPlugin, Opcodes
{
    /**
     * This class exists because the launcher don't allow {@link IClassTransformer IClassTransformers}
     * to be the same class as {@link IFMLLoadingPlugin IFMLLoadingPlugins}
     */
    @SuppressWarnings("unused")
    public static final class Transformer implements IClassTransformer
    {
        @Nonnull
        static final Map<String, Pair<String, String>> OBFUSCATION_MAP = ImmutableMap.<String, Pair<String, String>>builder()
                //mod support
                .put("git.jbredwards.customizableelytra.mod.client.layer.LayerCustomizableElytra", Pair.of("doRenderLayer", "func_177141_a"))
                .put("goblinbob.mobends.standard.client.renderer.entity.layers.LayerCustomCape", Pair.of("doRenderLayer", "func_177141_a"))
                .put("goblinbob.mobends.standard.client.renderer.entity.layers.LayerCustomElytra", Pair.of("doRenderLayer", "func_177141_a"))
                .put("vazkii.quark.vanity.client.layer.LayerBetterElytra", Pair.of("doRenderLayer", "doRenderLayer"))
                //baubles
                .put("baubles.common.container.SlotBauble", Pair.of("canTakeStack", "func_82869_a"))
                .put("baubles.common.network.PacketSync", Pair.of("toBytes", "toBytes"))
                //vanilla
                .put("net.minecraft.client.entity.EntityPlayerSP"              , Pair.of("onLivingUpdate"      , "func_70636_d"))
                .put("net.minecraft.client.renderer.entity.layers.LayerCape"   , Pair.of("doRenderLayer"       , "func_177141_a"))
                .put("net.minecraft.client.renderer.entity.layers.LayerElytra" , Pair.of("doRenderLayer"       , "func_177141_a"))
                .put("net.minecraft.entity.EntityLivingBase"                   , Pair.of("updateElytra"        , "func_184616_r"))
                .put("net.minecraft.item.ItemElytra"                           , Pair.of("onItemRightClick"    , "func_77659_a"))
                .put("net.minecraft.network.NetHandlerPlayServer"              , Pair.of("processEntityAction" , "func_147357_a"))
                .build();

        @Override
        public byte[] transform(@Nonnull String name, @Nonnull String transformedName, @Nonnull byte[] basicClass) {
            //ensure baubles properly syncs item capabilities (this is a problem that happens with customizable elytra)
            final boolean isDropFix = "baubles.common.event.EventHandlerEntity".equals(transformedName); //move baubles item drop handler to this mod
            final boolean isCreativeFix = "baubles.client.gui.GuiBaublesButton".equals(transformedName); //allow the baubles button to work in the creative inventory
            if(isDropFix || isCreativeFix || OBFUSCATION_MAP.containsKey(transformedName)) {
                final ClassNode classNode = new ClassNode();
                new ClassReader(basicClass).accept(classNode, 0);

                //baubles item death drops handler is moved to this mod
                if(isDropFix) classNode.methods.removeIf(method -> method.name.equals("playerDeath"));

                //change the method baubles uses to detect button clicks from mousePressed to mouseReleased,
                //and fix crash resulting from adding the baubles button to the creative inventory
                else if(isCreativeFix) {
                    //remove old mousePressed()
                    classNode.methods.removeIf(methodIn -> methodIn.name.equals(FMLLaunchHandler.isDeobfuscatedEnvironment() ? "mousePressed" : "func_146116_c"));

                    //generate new mousePressed()
                    final MethodNode mousePressed = new MethodNode(ACC_PUBLIC, FMLLaunchHandler.isDeobfuscatedEnvironment() ? "mousePressed" : "func_146116_c", "(Lnet/minecraft/client/Minecraft;II)Z", null, null);
                    final GeneratorAdapter mousePressedGen = new GeneratorAdapter(mousePressed, mousePressed.access, mousePressed.name, mousePressed.desc);
                    mousePressedGen.visitVarInsn(ALOAD, 0);
                    mousePressedGen.visitVarInsn(ALOAD, 1);
                    mousePressedGen.visitVarInsn(ILOAD, 2);
                    mousePressedGen.visitVarInsn(ALOAD, 0);
                    mousePressedGen.visitFieldInsn(GETFIELD, "baubles/client/gui/GuiBaublesButton", "parentGui", "Lnet/minecraft/client/gui/inventory/GuiContainer;");
                    mousePressedGen.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/client/gui/inventory/GuiContainer", "getGuiLeft", "()I", false);
                    mousePressedGen.visitInsn(ISUB);
                    mousePressedGen.visitVarInsn(ILOAD, 3);
                    mousePressedGen.visitMethodInsn(INVOKESPECIAL, "net/minecraft/client/gui/GuiButton", FMLLaunchHandler.isDeobfuscatedEnvironment() ? "mousePressed" : "func_146116_c", "(Lnet/minecraft/client/Minecraft;II)Z", false);
                    mousePressedGen.visitInsn(IRETURN);

                    //generate mouseReleased()
                    final MethodNode mouseReleased = new MethodNode(ACC_PUBLIC, FMLLaunchHandler.isDeobfuscatedEnvironment() ? "mouseReleased" : "func_146118_a", "(II)V", null, null);
                    final GeneratorAdapter mouseReleasedGen = new GeneratorAdapter(mouseReleased, mouseReleased.access, mouseReleased.name, mouseReleased.desc);
                    mouseReleasedGen.visitVarInsn(ALOAD, 0);
                    mouseReleasedGen.visitFieldInsn(GETFIELD, "baubles/client/gui/GuiBaublesButton", "parentGui", "Lnet/minecraft/client/gui/inventory/GuiContainer;");
                    mouseReleasedGen.visitMethodInsn(INVOKESTATIC, "git/jbredwards/baubleye/BaubleyElytra$Hooks", "onBaublesButtonMouseReleased", "(Lnet/minecraft/client/gui/inventory/GuiContainer;)V", false);
                    mouseReleasedGen.visitInsn(RETURN);

                    //don't play the button sound when the button is initially clicked
                    final MethodNode playPressSound = new MethodNode(ACC_PUBLIC, FMLLaunchHandler.isDeobfuscatedEnvironment() ? "playPressSound" : "func_146113_a", "(Lnet/minecraft/client/audio/SoundHandler;)V", null, null);
                    playPressSound.visitInsn(RETURN);

                    classNode.methods.add(mousePressed);
                    classNode.methods.add(mouseReleased);
                    classNode.methods.add(playPressSound);
                }

                else {
                    //use obfuscated method name if necessary
                    String methodName = FMLLaunchHandler.isDeobfuscatedEnvironment()
                            ? OBFUSCATION_MAP.get(transformedName).getLeft()
                            : OBFUSCATION_MAP.get(transformedName).getRight();


                    //ItemElytra implements IBauble at runtime
                    final boolean isItemElytra = "net.minecraft.item.ItemElytra".equals(transformedName);
                    if(isItemElytra) classNode.interfaces.add("git/jbredwards/baubleye/BaubleyElytra$IElytraBauble");

                    all: //iterate through all the instructions to fix all hardcoded vanilla elytra checks
                    for(MethodNode method : classNode.methods) {
                        if(method.name.equals(methodName)) {
                            for(AbstractInsnNode insn : method.instructions.toArray()) {
                                if(isItemElytra) { //allow players to right-click elytra to put it in the baubles slot
                                    if(insn.getOpcode() == GETSTATIC && ((FieldInsnNode)insn).name.equals("FAIL")) {
                                        if(!FMLLaunchHandler.isDeobfuscatedEnvironment()) { //needed when outside intellij, kinda wack lol
                                            final AbstractInsnNode frame = insn.getPrevious().getPrevious().getPrevious();
                                            method.instructions.insert(frame, new FrameNode(F_APPEND, 3, new Object[] {"net/minecraft/item/ItemStack", "net/minecraft/inventory/EntityEquipmentSlot", "net/minecraft/item/ItemStack"}, 0, null));
                                            method.instructions.remove(frame);
                                        }

                                        method.instructions.insertBefore(insn, new VarInsnNode(ALOAD, 2));
                                        method.instructions.insertBefore(insn, new VarInsnNode(ALOAD, 4));
                                        method.instructions.insertBefore(insn, new MethodInsnNode(INVOKESTATIC, "git/jbredwards/baubleye/BaubleyElytra$Hooks", "equipElytraBauble", "(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/util/EnumActionResult;", false));
                                        method.instructions.remove(insn);
                                        break all;
                                    }
                                }

                                //fix baubles packets
                                else if("baubles.common.network.PacketSync".equals(transformedName)) {
                                    if(insn.getOpcode() == INVOKESTATIC) {
                                        if(((MethodInsnNode)insn).name.equals("writeItemStack")) {
                                            ((MethodInsnNode)insn).owner = "git/jbredwards/baubleye/BaubleyElytra$Hooks";
                                            methodName = "fromBytes";
                                            break;
                                        }

                                        else if(((MethodInsnNode)insn).name.equals("readItemStack")) {
                                            ((MethodInsnNode)insn).owner = "git/jbredwards/baubleye/BaubleyElytra$Hooks";
                                            break all;
                                        }
                                    }
                                }

                                //fix baubles curse of binding
                                else if("baubles.common.container.SlotBauble".equals(transformedName)) {
                                    if(insn.getOpcode() == INVOKEVIRTUAL && ((MethodInsnNode)insn).name.equals(FMLLaunchHandler.isDeobfuscatedEnvironment() ? "isEmpty" : "func_190926_b")) {
                                        method.instructions.insert(insn, new MethodInsnNode(INVOKESTATIC, "git/jbredwards/baubleye/BaubleyElytra$Hooks", "isEmptyOrHasBindingCurse", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;)Z", false));
                                        method.instructions.insert(insn, new VarInsnNode(ALOAD, 1));
                                        method.instructions.remove(insn);
                                        break all;
                                    }
                                }

                                //fix elytra checks
                                else if(insn.getOpcode() == INVOKEVIRTUAL && ((MethodInsnNode)insn).name.equals(FMLLaunchHandler.isDeobfuscatedEnvironment() ? "getItemStackFromSlot" : "func_184582_a")) {
                                    method.instructions.insert(insn, new MethodInsnNode(INVOKESTATIC, "git/jbredwards/baubleye/BaubleyElytra$Hooks", "getElytra", "(Lnet/minecraft/entity/EntityLivingBase;)Lnet/minecraft/item/ItemStack;", false));
                                    method.instructions.remove(insn.getPrevious());
                                    method.instructions.remove(insn);
                                    break all;
                                }
                            }
                        }
                    }
                }

                //writes the changes
                final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                classNode.accept(writer);
                return writer.toByteArray();
            }

            return basicClass;
        }
    }

    /**
     * Implemented by {@link ItemElytra} at runtime
     */
    @SuppressWarnings("unused")
    public interface IElytraBauble extends IBauble
    {
        @Nonnull
        @Override
        default BaubleType getBaubleType(@Nonnull ItemStack stack) { return ConfigHandler.baubleType; }
    }

    @SuppressWarnings("unused")
    public static final class Hooks
    {
        @Nonnull
        public static EnumActionResult equipElytraBauble(@Nonnull EntityPlayer player, @Nonnull ItemStack held) {
            final IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
            for(int i : ConfigHandler.baubleType.getValidSlots()) {
                if(handler.getStackInSlot(i).isEmpty()) {
                    handler.setStackInSlot(i, held.copy());
                    player.playSound(SoundEvents.ITEM_ARMOR_EQIIP_ELYTRA, 1, 1);
                    held.shrink(1);

                    return EnumActionResult.SUCCESS;
                }
            }

            return EnumActionResult.FAIL;
        }

        /**
         * Used in place of {@link EntityLivingBase#getItemStackFromSlot(EntityEquipmentSlot)} when getting elytra
         */
        @Nonnull
        public static ItemStack getElytra(@Nonnull EntityLivingBase entity) {
            final ItemStack armor = entity.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
            //check isUsable to allow the bauble to be selected if the armor elytra is out of durability
            //this effectively lets players wear two elytra at once (where the bauble one is a fallback)!
            if(armor.getItem() instanceof ItemElytra && ItemElytra.isUsable(armor)) return armor;
            else if(entity instanceof EntityPlayer) {
                final IBaublesItemHandler handler = BaublesApi.getBaublesHandler((EntityPlayer)entity);
                ItemStack ret = ItemStack.EMPTY;

                for(int i : ConfigHandler.baubleType.getValidSlots()) {
                    final ItemStack stack = handler.getStackInSlot(i);
                    if(stack.getItem() instanceof ItemElytra) {
                        if(ItemElytra.isUsable(stack)) return stack;
                        else if(ret.isEmpty()) ret = stack;
                    }
                }

                if(!ret.isEmpty()) return ret;
            }

            return armor;
        }

        //ensure baubles item capability data is synced
        public static void writeItemStack(@Nonnull ByteBuf to, @Nonnull ItemStack stack) {
            new PacketBuffer(to).writeCompoundTag(stack.serializeNBT());
        }

        //ensure baubles item capability data is synced
        @Nonnull
        public static ItemStack readItemStack(@Nonnull ByteBuf to) {
            try { return new ItemStack(Objects.requireNonNull(new PacketBuffer(to).readCompoundTag())); }
            // Unpossible?
            catch(IOException e) { throw new RuntimeException(e); }
        }

        public static boolean isEmptyOrHasBindingCurse(@Nonnull ItemStack stack, @Nonnull EntityPlayer player) {
            return stack.isEmpty() || !player.isCreative() && EnchantmentHelper.hasBindingCurse(stack);
        }

        @SideOnly(Side.CLIENT)
        public static void onBaublesButtonMouseReleased(@Nonnull GuiContainer parentGui) {
            ButtonSoundPlayer.playButtonClickSound();
            //open parent inventory
            if(parentGui instanceof GuiPlayerExpanded) {
                ((GuiPlayerExpanded)parentGui).displayNormalInventory();
                PacketHandler.INSTANCE.sendToServer(new PacketOpenNormalInventory());
            }
            //open baubles inventory
            else PacketHandler.INSTANCE.sendToServer(new PacketOpenBaublesInventory());
        }
    }

    //needed to fix issue#3, prevents client class from being loaded serverside
    private static class ButtonSoundPlayer
    {
        @SideOnly(Side.CLIENT)
        static void playButtonClickSound() {
            Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1));
        }
    }

    @Config(modid = "baubleye")
    @Mod.EventBusSubscriber(modid = "baubleye")
    public static final class ConfigHandler
    {
        @Config.LangKey("config.baubleye.creativeAccessibility")
        public static boolean creativeAccessibility = true;

        @Config.LangKey("config.baubleye.slot")
        @Nonnull public static BaubleType baubleType = BaubleType.BODY;

        @SubscribeEvent
        public static void sync(@Nonnull ConfigChangedEvent.OnConfigChangedEvent event) {
            if("baubleye".equals(event.getModID())) ConfigManager.sync("baubleye", Config.Type.INSTANCE);
        }
    }

    @Nonnull
    @Override
    public String[] getASMTransformerClass() {
        return new String[] {"git.jbredwards.baubleye.BaubleyElytra$Transformer"};
    }

    @Nullable
    @Override
    public String getModContainerClass() { return null; }

    @Nullable
    @Override
    public String getSetupClass() { return null; }

    @Override
    public void injectData(@Nonnull Map<String, Object> map) { }

    @Nullable
    @Override
    public String getAccessTransformerClass() { return null; }
}
