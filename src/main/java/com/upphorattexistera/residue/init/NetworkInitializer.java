package com.upphorattexistera.residue.init;

import com.upphorattexistera.residue.network.*;

public class NetworkInitializer {
    public static void init() {
        FakeLanPacket.register();
        ObserverListPacket.register();
        ObserverMessagePacket.register();
        ObserverMessageRequestPacket.register();
        ObserverHistoryUpdatePacket.register();
    }
}