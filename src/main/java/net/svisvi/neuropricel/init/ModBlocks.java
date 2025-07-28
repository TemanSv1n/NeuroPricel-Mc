package net.svisvi.neuropricel.init;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.svisvi.neuropricel.NeuropricelMod;
import net.svisvi.neuropricel.block.PricelBlock;

public class ModBlocks {
    public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, NeuropricelMod.MODID);

    public static final RegistryObject<Block> PRICEL = REGISTRY.register("pricel", () -> new PricelBlock(BlockBehaviour.Properties.copy(Blocks.JUKEBOX).noOcclusion()));
}
