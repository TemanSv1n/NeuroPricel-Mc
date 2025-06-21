package net.svisvi.neuropricel.init;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.svisvi.neuropricel.NeuropricelMod;
import net.svisvi.neuropricel.block.entity.PricelBlockEntity;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, NeuropricelMod.MODID);

    public static final RegistryObject<BlockEntityType<PricelBlockEntity>> PRICEL_BE = REGISTRY.register("pricel_be", () ->
            BlockEntityType.Builder.of(PricelBlockEntity::new,
                    ModBlocks.PRICEL.get()).build(null));


    public static void register(IEventBus eventBus){
        REGISTRY.register(eventBus);
    }

}
