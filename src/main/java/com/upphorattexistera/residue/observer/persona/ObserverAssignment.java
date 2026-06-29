package com.upphorattexistera.residue.observer.persona;

import com.google.gson.JsonArray;

public class ObserverAssignment {

    public final String name;
    public int personaId;
    public String gender;
    public String skinFile;
    public int skinStage;
    public JsonArray conversationHistory;

    public ObserverAssignment(String name, int personaId,
                              String skinFile, int skinStage) {
        this.name = name;
        this.personaId = personaId;
        this.skinFile = skinFile;
        this.skinStage = skinStage;
        this.conversationHistory = new JsonArray();
    }
}