//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.svisvi.neuropricel.misc;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.api.sound.AbstractOnlineSoundInstance;
import gg.moonflower.etched.api.sound.OnlineRecordSoundInstance;
import gg.moonflower.etched.api.sound.StopListeningSound;
import gg.moonflower.etched.api.sound.source.AudioSource;
import gg.moonflower.etched.api.sound.source.AudioSource.AudioFileType;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.common.block.AlbumJukeboxBlock;
import gg.moonflower.etched.common.block.RadioBlock;
import gg.moonflower.etched.common.blockentity.AlbumJukeboxBlockEntity;
import gg.moonflower.etched.core.mixin.client.GuiAccessor;
import gg.moonflower.etched.core.mixin.client.LevelRendererAccessor;
import gg.moonflower.etched.core.registry.EtchedTags;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.DoubleSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.svisvi.neuropricel.block.entity.PricelBlockEntity;
import org.jetbrains.annotations.Nullable;

public class DristSoundTracker {
    private static final Int2ObjectArrayMap<SoundInstance> ENTITY_PLAYING_SOUNDS = new Int2ObjectArrayMap();
    private static final Set<String> FAILED_URLS = new HashSet();
    private static final Component RADIO = Component.translatable("sound_source.neuropricel.pricel");

    private static synchronized void setRecordPlayingNearby(Level level, BlockPos pos, boolean playing) {
        BlockState state = level.getBlockState(pos);
        if (state.is(EtchedTags.AUDIO_PROVIDER) || state.is(Blocks.JUKEBOX)) {
            for(LivingEntity livingEntity : level.getEntitiesOfClass(LivingEntity.class, (new AABB(pos)).inflate((double)3.0F))) {
                livingEntity.setRecordPlayingNearby(pos, playing);
            }
        }

    }

    public static @Nullable SoundInstance getEntitySound(int entity) {
        return (SoundInstance)ENTITY_PLAYING_SOUNDS.get(entity);
    }

    public static void setEntitySound(int entity, @Nullable SoundInstance instance) {
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        if (instance == null) {
            SoundInstance old = (SoundInstance)ENTITY_PLAYING_SOUNDS.remove(entity);
            if (old != null) {
                if (old instanceof StopListeningSound) {
                    ((StopListeningSound)old).stopListening();
                }

                soundManager.stop(old);
            }
        } else {
            ENTITY_PLAYING_SOUNDS.put(entity, instance);
            soundManager.play(instance);
        }

    }

    public static AbstractOnlineSoundInstance getEtchedRecord(final String url, final Component title, final Entity entity, int attenuationDistance, boolean stream) {
        Objects.requireNonNull(entity);
        DoubleSupplier var10008 = entity::getX;
        Objects.requireNonNull(entity);
        DoubleSupplier var10009 = entity::getY;
        Objects.requireNonNull(entity);
        return new OnlineRecordSoundInstance(url, entity, attenuationDistance, new MusicDownloadListener(title, var10008, var10009, entity::getZ) {
            public void onSuccess() {
                if (entity.isAlive() && DristSoundTracker.ENTITY_PLAYING_SOUNDS.containsKey(entity.getId())) {
                    if (PlayableRecord.canShowMessage(entity.getX(), entity.getY(), entity.getZ())) {
                        Minecraft.getInstance().gui.setNowPlaying(title);
                    }
                } else {
                    this.clearComponent();
                }

            }

            public void onFail() {
                Minecraft.getInstance().gui.setOverlayMessage(Component.translatable("record.etched.downloadFail", new Object[]{title}), true);
                DristSoundTracker.FAILED_URLS.add(url);
            }
        }, stream ? AudioFileType.STREAM : AudioFileType.FILE);
    }

    public static AbstractOnlineSoundInstance getEtchedRecord(String url, Component title, Entity entity, boolean stream) {
        return getEtchedRecord(url, title, entity, 16, stream);
    }

    public static AbstractOnlineSoundInstance getEtchedRecord(final String url, final Component title, final ClientLevel level, final BlockPos pos, int attenuationDistance, AudioSource.AudioFileType type) {
        BlockState aboveState = level.getBlockState(pos.above());
        boolean muffled = aboveState.is(BlockTags.WOOL);
        final boolean hidden = !aboveState.isAir();
        final Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor)Minecraft.getInstance().levelRenderer).getPlayingRecords();
        return new OnlineRecordSoundInstance(url, (double)((float)pos.getX() + 0.5F), (double)((float)pos.getY() + 0.5F), (double)((float)pos.getZ() + 0.5F), muffled ? 2.0F : 4.0F, muffled ? attenuationDistance / 2 : attenuationDistance, new MusicDownloadListener(title, () -> (double)pos.getX() + (double)0.5F, () -> (double)pos.getY() + (double)0.5F, () -> (double)pos.getZ() + (double)0.5F) {
            public void onSuccess() {
                if (!playingRecords.containsKey(pos)) {
                    this.clearComponent();
                } else {
                    if (!hidden && PlayableRecord.canShowMessage((double)pos.getX() + (double)0.5F, (double)pos.getY() + (double)0.5F, (double)pos.getZ() + (double)0.5F)) {
                        Minecraft.getInstance().gui.setNowPlaying(title);
                    }

                    DristSoundTracker.setRecordPlayingNearby(level, pos, true);
                }

            }

            public void onFail() {
                Minecraft.getInstance().gui.setOverlayMessage(Component.translatable("record.etched.downloadFail", new Object[]{title}), true);
                DristSoundTracker.FAILED_URLS.add(url);
            }
        }, type);
    }

    public static AbstractOnlineSoundInstance getEtchedRecord(String url, Component title, ClientLevel level, BlockPos pos, AudioSource.AudioFileType type) {
        return getEtchedRecord(url, title, level, pos, 16, type);
    }

    private static void playRecord(BlockPos pos, SoundInstance sound) {
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor)Minecraft.getInstance().levelRenderer).getPlayingRecords();
        playingRecords.put(pos, sound);
        soundManager.play(sound);
    }

    private static void playNextRecord(ClientLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        System.out.println("ADOLF TWINKLER");
        if (blockEntity instanceof AlbumJukeboxBlockEntity jukebox) {
            jukebox.next();
            playAlbum((AlbumJukeboxBlockEntity)blockEntity, blockEntity.getBlockState(), level, pos, true);
        } else if (blockEntity instanceof PricelBlockEntity pbe){
            System.out.println("PRICEL HITLER");
            stopPricel(pbe, blockEntity.getBlockState(), level, pos, true);
        }
    }

    public static void playBlockRecord(BlockPos pos, TrackData[] tracks, int track) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            if (track >= tracks.length) {
                setRecordPlayingNearby(level, pos, false);
            } else {
                TrackData trackData = tracks[track];
                String url = trackData.url();
                if (TrackData.isValidURL(url) && !FAILED_URLS.contains(url)) {
                    playRecord(pos, StopListeningSound.create(getEtchedRecord(url, trackData.getDisplayName(), level, pos, AudioFileType.FILE), () -> Minecraft.getInstance().tell(() -> {
                        if (((LevelRendererAccessor)Minecraft.getInstance().levelRenderer).getPlayingRecords().containsKey(pos)) {
                            playBlockRecord(pos, tracks, track + 1);
                        }
                    })));
                } else {
                    playBlockRecord(pos, tracks, track + 1);
                }
            }
        }
    }

    public static void playEntityRecord(ItemStack record, int entityId, int track, int attenuationDistance, boolean loop) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            Entity entity = level.getEntity(entityId);
            if (entity != null) {
                Optional<? extends SoundInstance> sound = ((PlayableRecord)record.getItem()).createEntitySound(record, entity, track, attenuationDistance);
                if (sound.isEmpty()) {
                    if (loop && track != 0) {
                        playEntityRecord(record, entityId, 0, attenuationDistance, true);
                    }

                } else {
                    SoundInstance entitySound = (SoundInstance)ENTITY_PLAYING_SOUNDS.remove(entity.getId());
                    if (entitySound != null) {
                        if (entitySound instanceof StopListeningSound) {
                            ((StopListeningSound)entitySound).stopListening();
                        }

                        Minecraft.getInstance().getSoundManager().stop(entitySound);
                    }

                    SoundInstance var9 = StopListeningSound.create((SoundInstance)sound.get(), () -> Minecraft.getInstance().tell(() -> {
                        ENTITY_PLAYING_SOUNDS.remove(entityId);
                        playEntityRecord(record, entityId, track + 1, attenuationDistance, loop);
                    }));
                    ENTITY_PLAYING_SOUNDS.put(entityId, var9);
                    Minecraft.getInstance().getSoundManager().play(var9);
                }
            }
        }
    }

    public static void playEntityRecord(ItemStack record, int entityId, int track, boolean loop) {
        playEntityRecord(record, entityId, track, 16, loop);
    }

    public static void playBoombox(int entityId, ItemStack record) {
        setEntitySound(entityId, (SoundInstance)null);
        if (!record.isEmpty()) {
            playEntityRecord(record, entityId, 0, 8, true);
        }

    }

    public static void playRadio(@Nullable String url, BlockState state, ClientLevel level, BlockPos pos) {
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor)Minecraft.getInstance().levelRenderer).getPlayingRecords();
        SoundInstance soundInstance = (SoundInstance)playingRecords.get(pos);
        if (soundInstance != null) {
            if (soundInstance instanceof StopListeningSound) {
                ((StopListeningSound)soundInstance).stopListening();
            }

            soundManager.stop(soundInstance);
            playingRecords.remove(pos);
            setRecordPlayingNearby(level, pos, false);
        }

        if (!FAILED_URLS.contains(url)) {
            if (state.hasProperty(RadioBlock.POWERED) && !(Boolean)state.getValue(RadioBlock.POWERED)) {
                if (TrackData.isValidURL(url)) {
                    AbstractOnlineSoundInstance record = getEtchedRecord(url, RADIO, level, pos, 8, AudioFileType.BOTH);
                    record.setLoop(false);
                    //record.
                    SoundInstance sound = null;
                    sound = StopListeningSound.create(record, () -> Minecraft.getInstance().tell(() -> playNextRecord(level, pos)));
                    //sound.
                    if (sound != null) {
                        playRecord(pos, sound);
                        setRecordPlayingNearby(level, pos, true);
                    }

                    //playRecord(pos, record);
                }

            }
        }
    }

    public static void stopPricel(PricelBlockEntity jukebox, BlockState state, ClientLevel level, BlockPos pos, boolean force) {
        System.out.println("ANTIPRICEL");
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingRecords();
        //if (state.hasProperty(AlbumJukeboxBlock.POWERED) && ((Boolean) state.getValue(AlbumJukeboxBlock.POWERED) || force || jukebox.recalculatePlayingIndex(false))) {
            SoundInstance soundInstance = (SoundInstance) playingRecords.get(pos);
            //if (soundInstance != null) {
                if (soundInstance instanceof StopListeningSound) {
                    ((StopListeningSound) soundInstance).stopListening();
                }

                soundManager.stop(soundInstance);
                playingRecords.remove(pos);
                setRecordPlayingNearby(level, pos, false);
            //}
       // }
    }


        public static void playAlbum(AlbumJukeboxBlockEntity jukebox, BlockState state, ClientLevel level, BlockPos pos, boolean force) {
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor)Minecraft.getInstance().levelRenderer).getPlayingRecords();
        if (state.hasProperty(AlbumJukeboxBlock.POWERED) && ((Boolean)state.getValue(AlbumJukeboxBlock.POWERED) || force || jukebox.recalculatePlayingIndex(false))) {
            SoundInstance soundInstance = (SoundInstance)playingRecords.get(pos);
            if (soundInstance != null) {
                if (soundInstance instanceof StopListeningSound) {
                    ((StopListeningSound)soundInstance).stopListening();
                }

                soundManager.stop(soundInstance);
                playingRecords.remove(pos);
                setRecordPlayingNearby(level, pos, false);
            }

            if ((Boolean)state.getValue(AlbumJukeboxBlock.POWERED)) {
                jukebox.stopPlaying();
            }

            if (jukebox.getPlayingIndex() >= 0) {
                ItemStack disc = jukebox.getItem(jukebox.getPlayingIndex());
                SoundInstance sound = null;
                if (disc.getItem() instanceof RecordItem) {
                    sound = StopListeningSound.create(getEtchedRecord(((RecordItem)disc.getItem()).getSound().getLocation().toString(), ((RecordItem)disc.getItem()).getDisplayName(), level, pos, AudioFileType.FILE), () -> Minecraft.getInstance().tell(() -> playNextRecord(level, pos)));
                } else if (disc.getItem() instanceof PlayableRecord) {
                    Optional<TrackData[]> optional = PlayableRecord.getStackMusic(disc);
                    if (optional.isPresent()) {
                        TrackData[] tracks = (TrackData[])optional.get();
                        TrackData track = jukebox.getTrack() >= 0 && jukebox.getTrack() < tracks.length ? tracks[jukebox.getTrack()] : tracks[0];
                        String url = track.url();
                        if (TrackData.isValidURL(url) && !FAILED_URLS.contains(url)) {
                            sound = StopListeningSound.create(getEtchedRecord(url, track.getDisplayName(), level, pos, AudioFileType.FILE), () -> Minecraft.getInstance().tell(() -> playNextRecord(level, pos)));
                        }
                    }
                }

                if (sound != null) {
                    playRecord(pos, sound);
                    setRecordPlayingNearby(level, pos, true);
                }
            }
        }
    }

    static {
        MinecraftForge.EVENT_BUS.addListener((event) -> FAILED_URLS.clear());
    }

    private static class DownloadTextComponent implements Component {
        private ComponentContents contents;
        private FormattedCharSequence visualOrderText;
        private Language decomposedWith;

        public DownloadTextComponent() {
            this.contents = ComponentContents.EMPTY;
            this.visualOrderText = FormattedCharSequence.EMPTY;
            this.decomposedWith = null;
        }

        public ComponentContents getContents() {
            return this.contents;
        }

        public List<Component> getSiblings() {
            return Collections.emptyList();
        }

        public Style getStyle() {
            return Style.EMPTY;
        }

        public FormattedCharSequence getVisualOrderText() {
            Language language = Language.getInstance();
            if (this.decomposedWith != language) {
                this.visualOrderText = language.getVisualOrder(this);
                this.decomposedWith = language;
            }

            return this.visualOrderText;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o != null && this.getClass() == o.getClass()) {
                if (!super.equals(o)) {
                    return false;
                } else {
                    DownloadTextComponent that = (DownloadTextComponent)o;
                    return this.contents.equals(that.contents);
                }
            } else {
                return false;
            }
        }

        public int hashCode() {
            return this.contents.hashCode();
        }

        public String toString() {
            return this.contents.toString();
        }

        public void setText(String text) {
            this.contents = new LiteralContents(text);
            this.decomposedWith = null;
        }
    }

    private abstract static class MusicDownloadListener implements DownloadProgressListener {
        private final Component title;
        private final DoubleSupplier x;
        private final DoubleSupplier y;
        private final DoubleSupplier z;
        private final BlockPos.MutableBlockPos pos;
        private float size;
        private Component requesting;
        private DownloadTextComponent component;

        protected MusicDownloadListener(Component title, DoubleSupplier x, DoubleSupplier y, DoubleSupplier z) {
            this.title = title;
            this.x = x;
            this.y = y;
            this.z = z;
            this.pos = new BlockPos.MutableBlockPos();
        }

        private BlockPos.MutableBlockPos getPos() {
            return this.pos.set(this.x.getAsDouble(), this.y.getAsDouble(), this.z.getAsDouble());
        }

        private void setComponent(Component text) {
            if (this.component != null || Minecraft.getInstance().level != null && Minecraft.getInstance().level.getBlockState(this.getPos().move(Direction.UP)).isAir() && PlayableRecord.canShowMessage(this.x.getAsDouble(), this.y.getAsDouble(), this.z.getAsDouble())) {
                if (this.component == null) {
                    this.component = new DownloadTextComponent();
                    Minecraft.getInstance().gui.setOverlayMessage(this.component, true);
                    ((GuiAccessor)Minecraft.getInstance().gui).setOverlayMessageTime(32767);
                }

                this.component.setText(text.getString());
            }
        }

        protected void clearComponent() {
            if (((GuiAccessor)Minecraft.getInstance().gui).getOverlayMessageString() == this.component) {
                ((GuiAccessor)Minecraft.getInstance().gui).setOverlayMessageTime(60);
                this.component = null;
            }

        }

        public void progressStartRequest(Component component) {
            this.requesting = component;
            this.setComponent(component);
        }

        public void progressStartDownload(float size) {
            this.size = size;
            this.requesting = null;
            this.progressStagePercentage(0);
        }

        public void progressStagePercentage(int percentage) {
            if (this.requesting != null) {
                this.setComponent(this.requesting.copy().append(" " + percentage + "%"));
            } else if (this.size != 0.0F) {
                this.setComponent(Component.translatable("record.etched.downloadProgress", new Object[]{String.format(Locale.ROOT, "%.2f", (float)percentage / 100.0F * this.size), String.format(Locale.ROOT, "%.2f", this.size), this.title}));
            }

        }

        public void progressStartLoading() {
            this.requesting = null;
            this.setComponent(Component.translatable("record.etched.loading", new Object[]{this.title}));
        }

        public void onFail() {
            Minecraft.getInstance().gui.setOverlayMessage(Component.translatable("record.etched.downloadFail", new Object[]{this.title}), true);
        }
    }
}
