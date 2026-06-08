package com.upphorattexistera.residuemod;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

public class ResidueVoicechatPlugin implements VoicechatPlugin{

    public static VoicechatApi voicechatApi;
    private static ConcurrentHashMap<UUID, Long> cooldowns;

    @Nullable
    public static VoicechatServerApi voicechatServerApi;

    @Nullable
    private OpusDecoder decoder;

    @Override
    public String getPluginId() {
        return "residuevoicechatplugin";
    }

    @Override
    public void initialize(VoicechatApi api) {
        voicechatApi = api;
        cooldowns = new ConcurrentHashMap<>();
    }
}
