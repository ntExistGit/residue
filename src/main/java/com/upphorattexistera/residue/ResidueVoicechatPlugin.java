package com.upphorattexistera.residue;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;

public class ResidueVoicechatPlugin implements VoicechatPlugin {

    public static VoicechatApi voicechatApi;

    @Override
    public String getPluginId() {
        return "residuevoicechatplugin";
    }

    @Override
    public void initialize(VoicechatApi api) {
        voicechatApi = api;
    }
}