package net.svisvi.neuropricel.block.entity;

import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.common.block.RadioBlock;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.svisvi.neuropricel.init.ModBlockEntities;
import net.svisvi.neuropricel.misc.DristSoundTracker;
import org.jetbrains.annotations.Nullable;

public class PricelBlockEntity extends BlockEntity implements Clearable {
    private String url;
    private boolean loaded;
    public CompletableFuture<String> url_responsed;
    public Player starterPlayer;

    private boolean finishedPlaying = false;

    public PricelBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType) ModBlockEntities.PRICEL_BE.get(), pos, state);
    }

    public boolean isFinishedPlaying() {
        return finishedPlaying;
    }

    public void setFinishedPlaying(boolean finished) {
        this.finishedPlaying = finished;
        setChanged();
    }

    public CompletableFuture<String> getUrl_responsed(){
        return url_responsed;
    }

    public Player getStarterPlayer(){
        return starterPlayer;
    }

    public void setStarterPlayer(Player starterPlayerr){
        starterPlayer = starterPlayerr;
    }

    public void setUrl_responsed(CompletableFuture<String> url_responsed) {
        this.url_responsed = url_responsed;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PricelBlockEntity blockEntity) {
        if (level != null && level.isClientSide()) {
            if (!blockEntity.loaded) {
                blockEntity.loaded = true;
                DristSoundTracker.playRadio(blockEntity.url, state, (ClientLevel)level, pos);
            }

            if (blockEntity.isPlaying()) {
                AABB range = (new AABB(pos)).inflate(3.45);
                List<LivingEntity> livingEntities = level.getEntitiesOfClass(LivingEntity.class, range);
                livingEntities.forEach((living) -> living.setRecordPlayingNearby(pos, true));
            }

        }
    }

    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.url = nbt.contains("Url", 8) ? nbt.getString("Url") : null;
        if (this.loaded) {
            DristSoundTracker.playRadio(this.url, this.getBlockState(), (ClientLevel)this.level, this.getBlockPos());
        }

    }

    public void saveAdditional(CompoundTag nbt) {
        if (this.url != null) {
            nbt.putString("Url", this.url);
        }

    }

    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    public @Nullable ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void clearContent() {
        this.url = null;
        if (this.level != null && this.level.isClientSide()) {
            DristSoundTracker.playRadio(this.url, this.getBlockState(), (ClientLevel)this.level, this.getBlockPos());
        }

    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        if (!Objects.equals(this.url, url)) {
            this.url = url;
            this.finishedPlaying = false;
            this.setChanged();
            if (this.level != null) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
        }

    }

    public boolean isPlaying() {
        BlockState state = this.getBlockState();
        return (!state.hasProperty(RadioBlock.POWERED) || !(Boolean)state.getValue(RadioBlock.POWERED)) && !StringUtil.isNullOrEmpty(this.url);
    }
}
