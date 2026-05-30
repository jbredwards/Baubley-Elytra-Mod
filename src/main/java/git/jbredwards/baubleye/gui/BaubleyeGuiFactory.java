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

package git.jbredwards.baubleye.gui;

import com.google.common.collect.Lists;
import git.jbredwards.baubleye.BaubleyElytra;
import git.jbredwards.baubleye.PatchConfigs;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.DefaultGuiFactory;
import net.minecraftforge.fml.client.config.DummyConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

/**
 *
 * @author jbred
 *
 */
public final class BaubleyeGuiFactory extends DefaultGuiFactory
{
    public BaubleyeGuiFactory() {
        super("baubleye", "configgui.baubleye.title");
    }

    @Nonnull
    @Override
    public GuiScreen createConfigGui(@Nonnull final GuiScreen parentScreen) {
        @Nonnull final IConfigElement general = new DummyConfigElement.DummyCategoryElement("dummy1",
                "configgui.baubleye.general",
                ConfigElement.from(BaubleyElytra.ConfigHandler.class).getChildElements());

        @Nonnull final IConfigElement patches = new DummyConfigElement.DummyCategoryElement("dummy2",
                PatchConfigs.failed ? "configgui.baubleye.patches_failed" : "configgui.baubleye.patches",
                PatchConfigs.failed ? Lists.newArrayList() : ConfigElement.from(PatchConfigs.class).getChildElements(),
                PatchCategory.class);

        return new GuiConfigTranslucent(parentScreen, Lists.newArrayList(general, patches), modid, false, false, I18n.format(title), null);
    }

    @SideOnly(Side.CLIENT)
    public static final class PatchCategory extends GuiConfigEntries.CategoryEntry
    {
        public PatchCategory(@Nonnull final GuiConfig owningScreen, @Nonnull final GuiConfigEntries owningEntryList, @Nonnull final IConfigElement configElement) {
            super(owningScreen, owningEntryList, configElement);
        }

        @Override
        public boolean enabled() {
            return !configElement.getChildElements().isEmpty();
        }
    }
}
