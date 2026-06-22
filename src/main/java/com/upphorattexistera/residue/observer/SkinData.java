package com.upphorattexistera.residue.observer;

import com.mojang.authlib.properties.Property;
import org.jetbrains.annotations.Nullable;

public class SkinData {

    public enum Model {
        DEFAULT, // Steve — широкие руки
        SLIM     // Alex — тонкие руки
    }

    public enum Source {
        LOCAL,
        UNKNOWN
    }

    @Nullable
    private final Property texturesProperty;

    private final Model model;
    private final Source source;

    public SkinData(@Nullable Property texturesProperty, Model model, Source source) {
        this.texturesProperty = texturesProperty;
        this.model = model;
        this.source = source;
    }

    public static SkinData unknown() {
        return new SkinData(null, Model.DEFAULT, Source.UNKNOWN);
    }

    @Nullable
    public Property getTexturesProperty() {
        return texturesProperty;
    }

    public Model getModel() {
        return model;
    }

    public Source getSource() {
        return source;
    }

    public boolean hasTextures() {
        return texturesProperty != null;
    }

    public boolean isSlim() {
        return model == Model.SLIM;
    }
}