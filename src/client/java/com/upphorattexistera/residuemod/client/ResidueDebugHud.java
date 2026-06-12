package com.upphorattexistera.residuemod.client;

import com.upphorattexistera.residuemod.WorldState;
import com.upphorattexistera.residuemod.config.ResidueConfig;
import com.upphorattexistera.residuemod.memory.MemoryManager;
import com.upphorattexistera.residuemod.observer.ObserverSessionManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ResidueDebugHud implements HudElement {

    private static final Identifier ID = Identifier.of("residue", "debug_hud");

    private static final int COLOR_LABEL    = 0xFFAAAAAA;
    private static final int COLOR_VALUE    = 0xFFFFFFFF;
    private static final int COLOR_STAGE    = 0xFFFFAA00;
    private static final int COLOR_OBSERVER = 0xFFCC88FF;
    private static final int PADDING        = 6;
    private static final int LINE_HEIGHT    = 10;

    public static void register() {
        HudElementRegistry.addLast(ID, new ResidueDebugHud());
    }

    @Override
    public void extractRenderState(DrawContext context, RenderTickCounter tickCounter) {

        if (!ResidueConfig.INSTANCE.debugMode) return;

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.getDebugHud().shouldShowDebugHud()) return;

        int memory    = MemoryManager.getMemory();
        int max       = ResidueConfig.INSTANCE.maxMemory;
        int attention = MemoryManager.getAttention();
        long ticks    = WorldState.ticks;

        var sessions = ObserverSessionManager.getSessions();

        String stage = getStage(memory, max);

        List<Line> lines = new ArrayList<>();
        lines.add(new Line("[ RESIDUE DEBUG ]",               COLOR_STAGE,    true));
        lines.add(new Line("Memory:     " + memory + " / " + max, COLOR_VALUE,    false));
        lines.add(new Line("Stage:       " + stage,             COLOR_STAGE,    false));
        lines.add(new Line("Attention:   " + attention,         COLOR_VALUE,    false));
        if (sessions.isEmpty()) {
            lines.add(new Line("Observer:  none", COLOR_OBSERVER, false));
        } else {
            lines.add(new Line("Observer:  " + sessions.size(), COLOR_OBSERVER, false));
            for (ObserverSessionManager.Session s : sessions) {
                long age = (ticks - s.joinedAtTick) / 20;
                long remaining = (s.disconnectAtTick - ticks) / 20;
                lines.add(new Line(
                        "  " + s.observer.getName()
                                + " +" + age + "s"
                                + " -" + remaining + "s",
                        COLOR_LABEL,
                        false
                ));
            }
        }

        int screenWidth = context.getScaledWindowWidth();
        int boxWidth    = 160;
        int boxHeight   = PADDING * 2 + lines.size() * LINE_HEIGHT + (lines.size() - 1) * 2;
        int x           = screenWidth - boxWidth - 4;
        int y           = 24;

        context.fill(
                x - PADDING,
                y - PADDING,
                x + boxWidth + PADDING,
                y + boxHeight,
                0x99000000
        );

        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            int lineY = y + i * (LINE_HEIGHT + 2);
            if (line.bold()) {
                context.drawTextWithShadow(client.textRenderer, line.text(), x, lineY, line.color());
            } else {
                context.drawText(client.textRenderer, line.text(), x, lineY, line.color(), false);
            }
        }
    }

    private static String getStage(int memory, int max) {
        int s1 = (int) (max * 0.20);
        int s2 = (int) (max * 0.40);
        int s3 = (int) (max * 0.60);
        int s4 = (int) (max * 0.80);

        if (memory < s1) return "0 — dormant";
        if (memory < s2) return "1 — echo";
        if (memory < s3) return "2 — presence";
        if (memory < s4) return "3 — fracture";
        return "4 — critical";
    }

    private record Line(String text, int color, boolean bold) {}
}