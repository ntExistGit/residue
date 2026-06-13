package com.upphorattexistera.residue.config.category;

import com.upphorattexistera.residue.client.ai.LLMServerManager;
import com.upphorattexistera.residue.config.DownloadStatusLabel;
import com.upphorattexistera.residue.config.LLMBackend;
import com.upphorattexistera.residue.config.LLMModel;
import com.upphorattexistera.residue.config.ResidueConfig;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;

public final class LlamaCategory {

    private LlamaCategory() {}

    /**
     * @param statusLabel лейбл прогресса, созданный в ResidueConfigScreen и переданный
     *                    в LLMServerManager.setStatusLabel() до вызова этого метода
     */
    public static ConfigCategory build(DownloadStatusLabel statusLabel) {
        return ConfigCategory.createBuilder()
                .name(Text.translatable("residue.config.llama"))

                .option(Option.<Boolean>createBuilder()
                        .name(Text.translatable("residue.config.llm_enable"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.llm_enable.desc")))
                        .binding(true,
                                () -> ResidueConfig.INSTANCE.llmEnable,
                                value -> ResidueConfig.INSTANCE.llmEnable = value)
                        .controller(BooleanControllerBuilder::create)
                        .build())

                .option(Option.<LLMBackend>createBuilder()
                        .name(Text.translatable("residue.config.llm_backend"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.llm_backend.desc")))
                        .binding(LLMBackend.AUTO,
                                () -> ResidueConfig.INSTANCE.llmBackend,
                                value -> ResidueConfig.INSTANCE.llmBackend = value)
                        .controller(opt -> EnumControllerBuilder.create(opt)
                                .enumClass(LLMBackend.class)
                                .formatValue(val -> Text.literal(val.displayName)))
                        .build())

                .option(Option.<LLMModel>createBuilder()
                        .name(Text.translatable("residue.config.llm_model"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.llm_model.desc")))
                        .binding(LLMModel.QWEN_3B,
                                () -> ResidueConfig.INSTANCE.llmModel,
                                value -> {
                                    ResidueConfig.INSTANCE.llmModel = value;
                                    statusLabel.reset();
                                })
                        .controller(opt -> EnumControllerBuilder.create(opt)
                                .enumClass(LLMModel.class)
                                .formatValue(val -> Text.literal(val.displayName)))
                        .build())

                .option(Option.<String>createBuilder()
                        .name(Text.translatable("residue.config.custom_model_name"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.custom_model_name.desc")))
                        .binding("",
                                () -> ResidueConfig.INSTANCE.customModelName,
                                value -> ResidueConfig.INSTANCE.customModelName = value)
                        .controller(StringControllerBuilder::create)
                        .build())

                .option(Option.<String>createBuilder()
                        .name(Text.translatable("residue.config.hf_token"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.hf_token.desc")))
                        .binding("",
                                () -> ResidueConfig.INSTANCE.huggingFaceToken,
                                value -> ResidueConfig.INSTANCE.huggingFaceToken = value)
                        .controller(StringControllerBuilder::create)
                        .build())

                // Лейбл прогресса загрузки
                .option(statusLabel.getOption())

                .option(ButtonOption.createBuilder()
                        .name(Text.translatable("residue.config.llm_download"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.llm_download.desc")))
                        .text(Text.translatable("residue.config.llm_download.button"))
                        .action((screen, option) -> {
                            MinecraftClient client = MinecraftClient.getInstance();

                            if (statusLabel.getState() == DownloadStatusLabel.State.DOWNLOADING) {
                                if (client.player != null)
                                    client.player.sendMessage(
                                            Text.translatable("residue.message.download_already_in_progress"));
                                return;
                            }

                            if (client.player != null)
                                client.player.sendMessage(
                                        Text.translatable("residue.message.download_started"));

                            CompletableFuture.runAsync(() -> {
                                try {
                                    LLMServerManager.getInstance().downloadAndSetup();
                                    client.execute(() -> {
                                        if (client.player != null)
                                            client.player.sendMessage(Text.translatable(
                                                    "residue.message.download_completed"));
                                    });
                                } catch (InterruptedException e) {
                                    client.execute(() -> {
                                        if (client.player != null)
                                            client.player.sendMessage(
                                                    Text.translatable("residue.message.download_cancelled"));
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    client.execute(() -> {
                                        if (client.player != null)
                                            client.player.sendMessage(
                                                    Text.translatable("residue.message.download_error", e.getMessage()));
                                    });
                                }
                            });
                        })
                        .build())

                .option(ButtonOption.createBuilder()
                        .name(Text.translatable("residue.config.llm_cancel"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.llm_cancel.desc")))
                        .text(Text.translatable("residue.config.llm_cancel.button"))
                        .action((screen, option) -> {
                            LLMServerManager.getInstance().cancelDownload();
                            MinecraftClient client = MinecraftClient.getInstance();
                            if (client.player != null)
                                client.player.sendMessage(
                                        Text.translatable("residue.message.download_cancelling"));
                        })
                        .build())

                .build();
    }
}