package git.jbredwards.baubleye;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.cap.IBaublesItemHandler;
import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemElytra;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.util.EnumActionResult;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 *
 * @author jbred
 *
 */
@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.Name("Baubley Elytra Plugin")
@Mod(modid = "baubleye", name = "Baubley Elytra", version = "1.1", dependencies = "required-after:baubles")
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
        static final Map<String, Pair<String, String>> CHEST_TO_BAUBLE = ImmutableMap.<String, Pair<String, String>>builder()
                .put("net.minecraft.client.entity.EntityPlayerSP"              , Pair.of("onLivingUpdate"      , "func_70636_d"))
                .put("net.minecraft.client.renderer.entity.layers.LayerCape"   , Pair.of("doRenderLayer"       , "func_177141_a"))
                .put("net.minecraft.client.renderer.entity.layers.LayerElytra" , Pair.of("doRenderLayer"       , "func_177141_a"))
                .put("net.minecraft.entity.EntityLivingBase"                   , Pair.of("updateElytra"        , "func_184616_r"))
                .put("net.minecraft.item.ItemElytra"                           , Pair.of("onItemRightClick"    , "func_77659_a"))
                .put("net.minecraft.network.NetHandlerPlayServer"              , Pair.of("processEntityAction" , "func_147357_a"))
                .put("vazkii.quark.vanity.client.layer.LayerBetterElytra"      , Pair.of("doRenderLayer"       , "doRenderLayer"))
                .build();

        @Override
        public byte[] transform(@Nonnull String name, @Nonnull String transformedName, @Nonnull byte[] basicClass) {
            if(CHEST_TO_BAUBLE.containsKey(transformedName)) {
                final ClassNode classNode = new ClassNode();
                new ClassReader(basicClass).accept(classNode, 0);

                //use obfuscated method name if necessary
                final String methodName = FMLLaunchHandler.isDeobfuscatedEnvironment()
                        ? CHEST_TO_BAUBLE.get(transformedName).getLeft()
                        : CHEST_TO_BAUBLE.get(transformedName).getRight();

                //ItemElytra implements IBauble at runtime
                final boolean isItemElytra = "net.minecraft.item.ItemElytra".equals(transformedName);
                if(isItemElytra) classNode.interfaces.add("git/jbredwards/baubleye/BaubleyElytra$IElytraBauble");

                all: //iterate through all the instructions to fix all hardcoded vanilla elytra checks
                for(MethodNode method : classNode.methods) {
                    if(method.name.equals(methodName)) {
                        for(AbstractInsnNode insn : method.instructions.toArray()) {
                            if(isItemElytra) { //allow players to right click elytra to put it in the baubles slot
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
        default BaubleType getBaubleType(@Nonnull ItemStack itemstack) { return BaubleType.BODY; }

        @Nullable
        @Override
        default String getUnlocalizedName(@Nonnull ItemStack par1ItemStack) { return null; }
    }


    @SuppressWarnings("unused")
    public static final class Hooks
    {
        @Nonnull
        public static EnumActionResult equipElytraBauble(@Nonnull EntityPlayer player, @Nonnull ItemStack held) {
            final IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
            if(handler.getStackInSlot(5).isEmpty()) {
                handler.setStackInSlot(5, held.copy());
                player.playSound(SoundEvents.ITEM_ARMOR_EQIIP_ELYTRA, 1, 1);
                held.shrink(1);

                return EnumActionResult.SUCCESS;
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
                final ItemStack bauble = BaublesApi.getBaublesHandler((EntityPlayer)entity).getStackInSlot(5);
                if(bauble.getItem() instanceof ItemElytra) return bauble;
            }

            return armor;
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
