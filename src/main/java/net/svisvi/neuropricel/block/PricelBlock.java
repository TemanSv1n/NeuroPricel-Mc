package net.svisvi.neuropricel.block;

import gg.moonflower.etched.common.blockentity.RadioBlockEntity;
import gg.moonflower.etched.common.menu.RadioMenu;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.network.play.ClientboundSetUrlPacket;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.mixin.client.LevelRendererAccessor;
import gg.moonflower.etched.core.registry.EtchedBlocks;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.PacketDistributor;
import net.svisvi.neuropricel.block.entity.PricelBlockEntity;
import net.svisvi.neuropricel.init.ModBlockEntities;
import net.svisvi.neuropricel.misc.LLMRequestSender;
import net.svisvi.neuropricel.misc.RequestSender;

public class PricelBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED;
    public static final BooleanProperty PROCESSING = BooleanProperty.create("processing");
    private static final Component CONTAINER_TITLE;

    public PricelBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((this.stateDefinition.any().setValue(FACING, Direction.NORTH)).setValue(POWERED, false).setValue(PROCESSING, false));
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        if (level.isClientSide()) {
            if (!getProcessing(state)) {
                ItemStack stack = player.getItemInHand(interactionHand);
                if (stack.getItem() == Items.WRITABLE_BOOK || stack.getItem() == Items.WRITTEN_BOOK) {
                    String content = fetchTextFromBook(stack);
                    if (content != null) {
                        BlockEntity blockEntity = level.getBlockEntity(pos);
                        if (blockEntity instanceof PricelBlockEntity pbe) {
                            pbe.setUrl(null);
                            pbe.setStarterPlayer(null);
                        }}}}
            return InteractionResult.SUCCESS;
        }

        if (!getProcessing(state)) {
            ItemStack stack = player.getItemInHand(interactionHand);
            if (stack.getItem() == Items.WRITABLE_BOOK || stack.getItem() == Items.WRITTEN_BOOK) {
                String content = fetchTextFromBook(stack);
                if (content != null) {
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity instanceof PricelBlockEntity pbe) {
                        pbe.setUrl(null);
                        pbe.setStarterPlayer(null);

                        setProcessing(level, pos, state, true);

                        LLMRequestSender.getAiResponseAsync(content)
                                .thenCompose(response -> {
                                    // Chain the TTS request after getting LLM response
                                    if (response != null) {
                                        pbe.setStarterPlayer(player);
                                        return RequestSender.generateTTSAsync(response);
                                    }
                                    return CompletableFuture.completedFuture(null);
                                })
                                .thenAccept(url -> {
                                    // Handle final result on main thread
                                    Minecraft.getInstance().execute(() -> {
                                        if (blockEntity instanceof PricelBlockEntity pbee) {
                                            if (url != null) {
                                                pbee.setUrl(url);
                                                // Play audio etc.
                                            }
                                            setProcessing(level, pos, state, false);
                                        }
                                    });
                                })
                                .exceptionally(e -> {
                                    // Error handling on main thread
                                    Minecraft.getInstance().execute(() -> {
                                        System.out.println("Error in processing chain: " + e.getMessage());
                                        setProcessing(level, pos, state, false);
                                    });
                                    return null;
                                });
                    }
                }
            }
        }
        return InteractionResult.CONSUME;
    }

    public void setProcessing(Level level, BlockPos pos, BlockState blockState, boolean f){
        level.setBlock(pos, (BlockState)blockState.setValue(PROCESSING, f), 3);
        level.sendBlockUpdated(pos, blockState, level.getBlockState(pos), 3);
    }

    public boolean getProcessing(BlockState blockState){
        return blockState.getValue(PROCESSING);
    }

    public String fetchTextFromBook(ItemStack stack){
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("pages")) {
            return null;
        }
        ListTag pagesTag = tag.getList("pages", 8); // 8 is the ID for String tags
        String pageJson = pagesTag.getString(0);
        // For simple text books, you can parse the JSON or just extract the text directly
        // This is a simplified approach that works for basic text
        String plainText = pageJson.replaceAll("\"text\":\"(.*?)\"", "$1");
        if (plainText.equals("")){
            return null;
        }
        System.out.println(plainText);

        return plainText;
    }

    public String getAiResponse(String request){
        //AI text server is in development now. Skipping this
        return request;
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos())).setValue(POWERED, false);
    }

    public void neighborChanged(BlockState blockState, Level level, BlockPos pos, Block block, BlockPos blockPos2, boolean bl) {
        if (!level.isClientSide()) {
            boolean bl2 = (Boolean)blockState.getValue(POWERED);
            if (bl2 != level.hasNeighborSignal(pos)) {
                level.setBlock(pos, (BlockState)blockState.cycle(POWERED), 2);
                level.sendBlockUpdated(pos, blockState, level.getBlockState(pos), 3);
            }
        }

    }

    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof PricelBlockEntity) {
                if (((PricelBlockEntity)blockEntity).isPlaying()) {
                    level.levelEvent(1011, pos, 0);
                }

                Clearable.tryClear(blockEntity);
            }

            super.onRemove(state, level, pos, newState, moving);
        }

    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 0;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return box(0, 0, 0, 16, 8, 16);
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }


    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> dropsOriginal = super.getDrops(state, builder);
        if (!dropsOriginal.isEmpty())
            return dropsOriginal;
        return Collections.singletonList(new ItemStack(this, 1));
    }



//    @Override
//    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
//        return box(0, 0, 0, 16, 16, 16);
//    }


//    public MenuProvider getMenuProvider(BlockState blockState, Level level, BlockPos blockPos) {
//        BlockEntity blockEntity = level.getBlockEntity(blockPos);
//        return new SimpleMenuProvider((menuId, playerInventory, player) -> {
//            RadioMenu var10000 = new RadioMenu;
//            ContainerLevelAccess var10004 = ContainerLevelAccess.create(level, blockPos);
//            Consumer var6;
//            if (blockEntity instanceof PricelBlockEntity var10005) {
//                Objects.requireNonNull((PricelBlockEntity)blockEntity);
//                var6 = var10005::setUrl;
//            } else {
//                var6 = (url) -> {
//                };
//            }
//
//            var10000.<init>(menuId, playerInventory, var10004, var6);
//            return var10000;
//        }, CONTAINER_TITLE);
//    }

    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PricelBlockEntity(pos, state);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, PROCESSING);
    }

    public boolean isPathfindable(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, PathComputationType pathComputationType) {
        return false;
    }

    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if ((Boolean)Etched.CLIENT_CONFIG.showNotes.get() && level.getBlockState(pos.above()).isAir()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof PricelBlockEntity pbe) {
                PricelBlockEntity radio = (PricelBlockEntity)blockEntity;
                if (radio.getUrl() != null) {
                    if (getProcessing(state)){
                        setProcessing(level, pos, state, false);
                        if (pbe.getStarterPlayer() != null) {
                            EtchedMessages.PLAY.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) pbe.getStarterPlayer()), new ClientboundSetUrlPacket(radio.getUrl()));
                        }
                    }
                    Minecraft minecraft = Minecraft.getInstance();
                    Map<BlockPos, SoundInstance> sounds = ((LevelRendererAccessor)minecraft.levelRenderer).getPlayingRecords();
                    if (sounds.containsKey(pos) && minecraft.getSoundManager().isActive((SoundInstance)sounds.get(pos))) {
                        level.addParticle(ParticleTypes.NOTE, (double)pos.getX() + (double)0.5F, (double)pos.getY() + 0.3, (double)pos.getZ() + (double)0.5F, (double)random.nextInt(25) / (double)24.0F, (double)0.0F, (double)0.0F);
                    }

                } else if (state.getValue(PROCESSING)){
                    level.addParticle(ParticleTypes.GLOW, (double)pos.getX() + (double)0.5F, (double)pos.getY() + 0.3, (double)pos.getZ() + (double)0.5F, (double)random.nextInt(25) / (double)24.0F, (double)0.0F, (double)0.0F);
//                    BlockEntity blockEntity = level.getBlockEntity(pos);
//                    if (blockEntity instanceof PricelBlockEntity pbe){
//                    if (.isDone()) {

                }
            }
        }

//        if (level.getBlockState(pos.above()).isAir()){
//            BlockEntity blockEntity = level.getBlockEntity(pos);
//            if (blockEntity instanceof PricelBlockEntity) {
//                PricelBlockEntity radio = (PricelBlockEntity)blockEntity;
//            }
//        }
    }

    private void resetBlock(Level level, BlockPos pos, BlockState originalState) {
        // Store properties we want to keep
        Direction facing = originalState.getValue(FACING);
        boolean powered = originalState.getValue(POWERED);

        // Remove and re-place the block
        level.removeBlock(pos, false);
        BlockState newState = this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(POWERED, powered)
                .setValue(PROCESSING, false);

        level.setBlock(pos, newState, 3);

        // Recreate the block entity
        BlockEntity newEntity = new PricelBlockEntity(pos, newState);
        level.setBlockEntity(newEntity);
    }

    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, (BlockEntityType) ModBlockEntities.PRICEL_BE.get(), PricelBlockEntity::tick);
    }

    static {
        POWERED = BlockStateProperties.POWERED;
        CONTAINER_TITLE = Component.translatable("block.neuropricel.pricel");
    }
}