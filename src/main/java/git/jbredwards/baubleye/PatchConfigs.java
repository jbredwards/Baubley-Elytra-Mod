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

import com.cleanroommc.configanytime.ConfigAnytime;
import net.minecraftforge.common.config.Config;
import org.apache.logging.log4j.LogManager;

import javax.annotation.Nonnull;

/**
 * Hold the config settings for patches in a separate class, so they can be loaded early.
 * @author jbred
 *
 */
@SuppressWarnings("BooleanMethodIsAlwaysInverted")
@Config(modid = "baubleye", name = "baubleye_patches")
public final class PatchConfigs
{
    @Config.Ignore
    public static boolean failed = false;

    @Config.RequiresMcRestart
    @Config.LangKey("config.baubleye.enabled")
    public static boolean enabled = true;

    @Config.RequiresMcRestart
    @Config.LangKey("config.baubleye.patchBaublesMissingCreativeInventory")
    public static boolean patchBaublesCreativeInventory = true;
    public static boolean patchBaublesCreativeInventory() {
        return enabled && patchBaublesCreativeInventory;
    }

    @Config.RequiresMcRestart
    @Config.LangKey("config.baubleye.patchBaublesEnchantments")
    public static boolean patchBaublesEnchantments = true;
    public static boolean patchBaublesEnchantments() {
        return enabled && patchBaublesEnchantments;
    }

    @Config.RequiresMcRestart
    @Config.LangKey("config.baubleye.patchBaublesItemSync")
    public static boolean patchBaublesItemSync = true;
    public static boolean patchBaublesItemSync() {
        return enabled && patchBaublesItemSync;
    }

    @Config.RequiresMcRestart
    @Config.LangKey("config.baubleye.patchBaublesPlayerDrops")
    public static boolean patchBaublesPlayerDrops = true;
    public static boolean patchBaublesPlayerDrops() {
        return enabled && patchBaublesPlayerDrops;
    }

    /**
     * Initializes patch config settings using ConfigAnytime.
     */
    public static void init() {}
    static {
        try { ConfigAnytime.register(PatchConfigs.class); }
        catch(@Nonnull final Throwable e) {
            failed = true;

            @Nonnull final String error = "ConfigAnytime mod is not present, \"baubleye_patches.cfg\" could not be read!";
            LogManager.getLogger("Baubley Elytra").warn(error, e);
        }
    }
}
