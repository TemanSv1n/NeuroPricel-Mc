package net.svisvi.neuropricel.init;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ModTabs {

    @SubscribeEvent
    public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {

        if (tabData.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            tabData.accept(ModBlocks.PRICEL.get().asItem());
        }

    }
}
