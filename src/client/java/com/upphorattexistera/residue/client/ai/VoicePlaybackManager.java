package com.upphorattexistera.residue.client.ai;

import com.upphorattexistera.residue.ResidueVoicechatPlugin;
import com.upphorattexistera.residue.config.ResidueConfig;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.audiochannel.ClientEntityAudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.ClientStaticAudioChannel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VoicePlaybackManager {

    /** 960 сэмплов = 40мс при 24kHz — стандартный SVC-фрейм. */
    private static final int FRAME_SIZE = 960;
    private static final int WAV_HEADER_SIZE = 44;

    /**
     * Проигрывает WAV-байты от tts-server.
     * Вызывать из виртуального потока — не блокирует рендер-поток.
     *
     * @param wav          s16le 24kHz mono WAV от tts-server
     * @param observerUuid UUID обсервера для поиска Entity в мире
     * @param channel      POSITIONAL (от Entity) или WHISPER (в голову)
     */
    public static void play(byte[] wav, UUID observerUuid, ObserverVoiceChannel channel) {
        VoicechatClientApi api = ResidueVoicechatPlugin.clientApi;
        if (api == null) return;
        if (wav == null || wav.length <= WAV_HEADER_SIZE) return;

        short[] pcm = wavToShorts(wav);
        if (pcm.length == 0) return;

        List<short[]> frames = splitFrames(pcm);

        switch (channel) {
            case POSITIONAL -> playPositional(api, frames, observerUuid);
            case WHISPER    -> playWhisper(api, frames);
        }
    }

    /**
     * Проверить ДО HTTP-запроса к tts-server — не тратить GPU если игрок не услышит.
     */
    public static boolean shouldSynthesize(ObserverVoiceChannel channel, UUID observerUuid) {
        if (channel == ObserverVoiceChannel.WHISPER) return true;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return false;

        Entity entity = findEntityByUuid(mc, observerUuid);
        if (entity == null) return false;

        double dist = mc.player.getEntityPos().distanceTo(entity.getEntityPos());
        return dist <= ResidueConfig.INSTANCE.ttsAudibleDistance;
    }

    // ----------------------------------------------------------------

    private static void playPositional(VoicechatClientApi api, List<short[]> frames, UUID observerUuid) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        Entity mcEntity = findEntityByUuid(mc, observerUuid);
        if (mcEntity == null) {
            // Entity не найдена — откат к статичному шёпоту
            playWhisper(api, frames);
            return;
        }

        de.maxhenkel.voicechat.api.Entity svcEntity = api.fromEntity(mcEntity);
        ClientEntityAudioChannel svcChannel = api.createEntityAudioChannel(UUID.randomUUID(), svcEntity);
        if (svcChannel == null) return;

        // Дистанция слышимости из конфига — дублируем в самом канале,
        // чтобы SVC не обрезал звук по своему дефолтному радиусу раньше нашего гейта.
        svcChannel.setDistance((float) ResidueConfig.INSTANCE.ttsAudibleDistance);

        sendFrames(svcChannel, frames);
    }

    private static void playWhisper(VoicechatClientApi api, List<short[]> frames) {
        ClientStaticAudioChannel svcChannel = api.createStaticAudioChannel(UUID.randomUUID());
        if (svcChannel == null) return;

        sendFrames(svcChannel, frames);
    }

    /**
     * Для ивентового позиционного шёпота: обсервер "говорит тихо" из своей позиции.
     * Отличается от WHISPER тем, что звук всё равно идёт от Entity в мире,
     * но помечен как whispering — SVC применяет к нему свой whisper-фильтр.
     */
    public static void playPositionalWhisper(byte[] wav, UUID observerUuid) {
        VoicechatClientApi api = ResidueVoicechatPlugin.clientApi;
        if (api == null) return;
        if (wav == null || wav.length <= WAV_HEADER_SIZE) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        Entity mcEntity = findEntityByUuid(mc, observerUuid);
        if (mcEntity == null) {
            // Entity нет — шёпот всё равно воспроизводим статично (жуткий эффект)
            play(wav, observerUuid, ObserverVoiceChannel.WHISPER);
            return;
        }

        short[] pcm = wavToShorts(wav);
        if (pcm.length == 0) return;

        de.maxhenkel.voicechat.api.Entity svcEntity = api.fromEntity(mcEntity);
        ClientEntityAudioChannel svcChannel = api.createEntityAudioChannel(UUID.randomUUID(), svcEntity);
        if (svcChannel == null) return;

        svcChannel.setWhispering(true);
        svcChannel.setDistance((float) ResidueConfig.INSTANCE.ttsAudibleDistance);

        sendFrames(svcChannel, splitFrames(pcm));
    }

    // ----------------------------------------------------------------
    // Общий метод отправки — ClientAudioChannel.play(short[]) принимает
    // сырой PCM напрямую, OpusEncoder не нужен вообще.

    private static void sendFrames(de.maxhenkel.voicechat.api.audiochannel.ClientAudioChannel channel,
                                   List<short[]> frames) {
        for (short[] frame : frames) {
            channel.play(frame);
        }
    }

    // ----------------------------------------------------------------
    // PCM utils

    /** Срезаем 44-байтный WAV-заголовок, читаем s16le → short[]. */
    private static short[] wavToShorts(byte[] wav) {
        int dataLen = wav.length - WAV_HEADER_SIZE;
        if (dataLen <= 0) return new short[0];

        short[] samples = new short[dataLen / 2];
        ByteBuffer buf = ByteBuffer
                .wrap(wav, WAV_HEADER_SIZE, dataLen)
                .order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < samples.length; i++) {
            samples[i] = buf.getShort();
        }
        return samples;
    }

    /** Делим PCM на фреймы по 960 сэмплов. Последний фрейм дополняется нулями. */
    private static List<short[]> splitFrames(short[] pcm) {
        List<short[]> frames = new ArrayList<>();
        int offset = 0;
        while (offset < pcm.length) {
            short[] frame = new short[FRAME_SIZE];
            int toCopy = Math.min(FRAME_SIZE, pcm.length - offset);
            System.arraycopy(pcm, offset, frame, 0, toCopy);
            frames.add(frame);
            offset += FRAME_SIZE;
        }
        return frames;
    }

    private static Entity findEntityByUuid(MinecraftClient mc, UUID uuid) {
        if (mc.world == null) return null;
        for (Entity e : mc.world.getEntities()) {
            if (uuid.equals(e.getUuid())) return e;
        }
        return null;
    }
}