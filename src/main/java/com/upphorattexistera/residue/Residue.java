package com.upphorattexistera.residue;

import com.upphorattexistera.residue.init.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Residue implements ModInitializer {
    public static final String MOD_ID = "residue";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {

        // 1. Базовые данные и конфиги
        DataInitializer.init();

        // 2. Регистрация сущностей
        EntityInitializer.init();

        // 3. Регистрация сетевых пакетов
        NetworkInitializer.init();

        // 4. Регистрация контекстов
        ContextInitializer.init();

        // 5. Регистрация событий и тиков
        EventInitializer.init();

        // 6. Регистрация команд
        CommandInitializer.init();

        LOGGER.info("[residue] initialized successfully");
    }
}