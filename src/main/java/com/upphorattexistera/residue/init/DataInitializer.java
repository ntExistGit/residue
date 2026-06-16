package com.upphorattexistera.residue.init;

import com.upphorattexistera.residue.config.ResidueConfigSerializer;
import com.upphorattexistera.residue.observer.persona.ObserverPersonaLoader;
import com.upphorattexistera.residue.observer.persona.ObserverTypeLoader;

public class DataInitializer {
    public static void init() {
        ResidueConfigSerializer.load();
        ObserverPersonaLoader.load();
        ObserverTypeLoader.load();
    }
}