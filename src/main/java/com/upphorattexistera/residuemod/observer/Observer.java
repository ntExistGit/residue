package com.upphorattexistera.residuemod.observer;

public class Observer {

    private String name;
    private int weight;
    private boolean used;

    public Observer() {
    }

    public Observer(String name, int weight) {

        this.name = name;
        this.weight = weight;
        this.used = false;
    }

    public String getName() {
        return name;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }
}