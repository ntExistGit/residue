package com.upphorattexistera.residue;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;

public class ResidueVoicechatPlugin implements VoicechatPlugin {

    public static VoicechatServerApi serverApi;
    public static VoicechatClientApi clientApi;

    @Override
    public String getPluginId() {
        return "residuevoicechatplugin";
    }

    @Override
    public void initialize(VoicechatApi api) {
        if (api instanceof VoicechatServerApi s) serverApi = s;
        if (api instanceof VoicechatClientApi c) clientApi = c;
    }
}