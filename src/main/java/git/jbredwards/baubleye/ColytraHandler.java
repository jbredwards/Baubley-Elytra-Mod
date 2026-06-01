/*
 * Copyright (C) <2026 to Present> <jbredwards>
 *
 * All rights are reserved, except where explicitly granted by the original
 * copyright holder or where explicitly granted by the Mod Permissions License as
 * published by Jbredwards, either version 1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.
 *
 * See the Mod Permissions License for more details
 * <https://www.github.com/jbredwards/mod-permissions-license>.
 */

package git.jbredwards.baubleye;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.annotation.Nonnull;

/**
 * Disables and remaps the Colytra "bauble elytra" item.
 * @author jbred
 *
 */
public final class ColytraHandler
{
    static void init() throws ReflectiveOperationException {
        FieldUtils.writeStaticField(Class.forName("c4.colytra.proxy.CommonProxy"), "baublesLoaded", Boolean.FALSE);
        MinecraftForge.EVENT_BUS.register(ColytraHandler.class);
    }

    @SubscribeEvent
    static void remapBaubleElytraItem(@Nonnull final RegistryEvent.MissingMappings<Item> event) {
        event.getAllMappings().stream().filter(mapping -> mapping.key.toString().equals("colytra:elytra_bauble")).forEach(mapping -> mapping.remap(Items.ELYTRA));
    }
}
