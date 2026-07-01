package com.upphorattexistera.residue.config.category;

import com.upphorattexistera.residue.client.ai.LLMServerController;
import com.upphorattexistera.residue.client.ai.LLMServerManager;
import com.upphorattexistera.residue.client.ai.TTSServerController;
import com.upphorattexistera.residue.client.ai.TTSServerManager;
import com.upphorattexistera.residue.config.*;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import java.util.concurrent.CompletableFuture;

public final class AI_CPP_Category {
    private AI_CPP_Category() {}

    public static ConfigCategory build(DownloadStatusLabel statusLabel,
                                       LlmServerStatusLabel serverStatusLabel,
                                       DownloadStatusLabel ttsStatusLabel,
                                       TtsServerStatusLabel ttsServerStatusLabel) {

        return ConfigCategory.createBuilder()
                .name(Text.translatable("residue.config.llama"))

                // === Общий язык ===
                .option(Option.<Language>createBuilder()
                        .name(Text.translatable("residue.config.language"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.language.desc")))
                        .binding(Language.ENGLISH,
                                () -> ResidueConfig.INSTANCE.language,
                                value -> ResidueConfig.INSTANCE.language = value)
                        .controller(opt -> EnumControllerBuilder.create(opt)
                                .enumClass(Language.class)
                                .formatValue(val -> Text.literal(val.displayName)))
                        .build())

                // === HF Token ===
                .option(Option.<String>createBuilder()
                        .name(Text.translatable("residue.config.hf_token"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.hf_token.desc")))
                        .binding("",
                                () -> ResidueConfig.INSTANCE.huggingFaceToken,
                                value -> ResidueConfig.INSTANCE.huggingFaceToken = value)
                        .controller(StringControllerBuilder::create)
                        .build())

                // === LLM Settings ===
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

                .option(Option.<Boolean>createBuilder()
                        .name(Text.translatable("residue.config.llm_think"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.llm_think.desc")))
                        .binding(false,
                                () -> ResidueConfig.INSTANCE.llmThink,
                                value -> ResidueConfig.INSTANCE.llmThink = value)
                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                .trueFalseFormatter().coloured(true))
                        .build())

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
                                    client.player.sendMessage(Text.translatable("residue.message.download_already_in_progress"));
                                return;
                            }
                            if (client.player != null)
                                client.player.sendMessage(Text.translatable("residue.message.download_started"));

                            CompletableFuture.runAsync(() -> {
                                try {
                                    LLMServerManager.getInstance().downloadAndSetup();
                                    client.execute(() -> {
                                        if (client.player != null)
                                            client.player.sendMessage(Text.translatable("residue.message.download_completed"));
                                    });
                                } catch (InterruptedException e) {
                                    client.execute(() -> {
                                        if (client.player != null)
                                            client.player.sendMessage(Text.translatable("residue.message.download_cancelled"));
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    client.execute(() -> {
                                        if (client.player != null)
                                            client.player.sendMessage(Text.translatable("residue.message.download_error", e.getMessage()));
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
                                client.player.sendMessage(Text.translatable("residue.message.download_cancelling"));
                        })
                        .build())

                .option(LabelOption.create(Text.translatable("residue.config.llm_server_manual.label")))
                .option(serverStatusLabel.getOption())
                .option(ButtonOption.createBuilder()
                        .name(Text.translatable("residue.config.llm_server_toggle"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.llm_server_toggle.desc")))
                        .text(Text.translatable("residue.config.llm_server_toggle.button"))
                        .action((screen, option) ->
                                LLMServerController.toggle(serverStatusLabel::setRunning))
                        .build())

                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("residue.config.llm_max_history"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.llm_max_history.desc")))
                        .binding(40,
                                () -> ResidueConfig.INSTANCE.maxHistorySize,
                                value -> ResidueConfig.INSTANCE.maxHistorySize = value)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(10, 100).step(1))
                        .build())

                // === TTS Section ===
                .option(LabelOption.create(Text.translatable("residue.config.tts.label")))

                .option(Option.<Boolean>createBuilder()
                        .name(Text.translatable("residue.config.tts_enable"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.tts_enable.desc")))
                        .binding(false,
                                () -> ResidueConfig.INSTANCE.ttsEnable,
                                value -> ResidueConfig.INSTANCE.ttsEnable = value)
                        .controller(BooleanControllerBuilder::create)
                        .build())

                .option(Option.<Double>createBuilder()
                        .name(Text.translatable("residue.config.tts_audible_distance"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.tts_audible_distance.desc")))
                        .binding(32.0,
                                () -> ResidueConfig.INSTANCE.ttsAudibleDistance,
                                value -> ResidueConfig.INSTANCE.ttsAudibleDistance = value)
                        .controller(opt -> DoubleSliderControllerBuilder.create(opt)
                                .range(8.0, 128.0).step(4.0))
                        .build())

                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("residue.config.tts_pre_roll"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.tts_pre_roll.desc")))
                        .binding(300,
                                () -> ResidueConfig.INSTANCE.ttsPreRollMs,
                                value -> ResidueConfig.INSTANCE.ttsPreRollMs = value)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(0, 2000).step(50))
                        .build())

                .option(Option.<TTSTokenizer>createBuilder()
                        .name(Text.translatable("residue.config.tts_tokenizer"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.tts_tokenizer.desc")))
                        .binding(TTSTokenizer.Q4_K_M,
                                () -> ResidueConfig.INSTANCE.ttsTokenizer,
                                value -> {
                                    ResidueConfig.INSTANCE.ttsTokenizer = value;
                                    ttsStatusLabel.reset();
                                })
                        .controller(opt -> EnumControllerBuilder.create(opt)
                                .enumClass(TTSTokenizer.class)
                                .formatValue(val -> Text.literal(val.displayName)))
                        .build())

                .option(Option.<String>createBuilder()
                        .name(Text.translatable("residue.config.tts_custom_tokenizer_name"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.tts_custom_tokenizer_name.desc")))
                        .binding("",
                                () -> ResidueConfig.INSTANCE.ttsCustomTokenizerName,
                                value -> ResidueConfig.INSTANCE.ttsCustomTokenizerName = value)
                        .controller(StringControllerBuilder::create)
                        .build())

                .option(Option.<TTSTalker>createBuilder()
                        .name(Text.translatable("residue.config.tts_talker"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.tts_talker.desc")))
                        .binding(TTSTalker.QWEN_TALKER_1_7B_CUSTOM_Q4,
                                () -> ResidueConfig.INSTANCE.ttsTalker,
                                value -> {
                                    ResidueConfig.INSTANCE.ttsTalker = value;
                                    ttsStatusLabel.reset();
                                })
                        .controller(opt -> EnumControllerBuilder.create(opt)
                                .enumClass(TTSTalker.class)
                                .formatValue(val -> Text.literal(val.displayName)))
                        .build())

                .option(Option.<String>createBuilder()
                        .name(Text.translatable("residue.config.tts_custom_talker_name"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.tts_custom_talker_name.desc")))
                        .binding("",
                                () -> ResidueConfig.INSTANCE.ttsCustomTalkerName,
                                value -> ResidueConfig.INSTANCE.ttsCustomTalkerName = value)
                        .controller(StringControllerBuilder::create)
                        .build())

                .group(ListOption.<String>createBuilder()
                        .name(Text.translatable("residue.config.tts_enabled_speakers"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.tts_enabled_speakers.desc")))
                        .binding(
                                new java.util.ArrayList<>(TTSSpeakers.ALL),
                                () -> ResidueConfig.INSTANCE.ttsEnabledSpeakers,
                                value -> ResidueConfig.INSTANCE.ttsEnabledSpeakers = value)
                        .controller(opt -> DropdownStringControllerBuilder.create(opt)
                                .values(TTSSpeakers.ALL)
                                .allowAnyValue(false)
                                .allowEmptyValue(false))
                        .initial(TTSSpeakers.ALL.get(0))
                        .build())

                .option(ttsStatusLabel.getOption())

                .option(ButtonOption.createBuilder()
                        .name(Text.translatable("residue.config.tts_download"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.tts_download.desc")))
                        .text(Text.translatable("residue.config.tts_download.button"))
                        .action((screen, option) -> {
                            MinecraftClient client = MinecraftClient.getInstance();
                            if (ttsStatusLabel.getState() == DownloadStatusLabel.State.DOWNLOADING) {
                                if (client.player != null)
                                    client.player.sendMessage(Text.translatable("residue.message.download_already_in_progress"));
                                return;
                            }
                            if (client.player != null)
                                client.player.sendMessage(Text.translatable("residue.message.tts_download_started"));

                            CompletableFuture.runAsync(() -> {
                                try {
                                    TTSServerManager.getInstance().downloadAndSetup();
                                    client.execute(() -> {
                                        if (client.player != null)
                                            client.player.sendMessage(Text.translatable("residue.message.tts_download_completed"));
                                    });
                                } catch (InterruptedException e) {
                                    client.execute(() -> {
                                        if (client.player != null)
                                            client.player.sendMessage(Text.translatable("residue.message.download_cancelled"));
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    client.execute(() -> {
                                        if (client.player != null)
                                            client.player.sendMessage(Text.translatable("residue.message.download_error", e.getMessage()));
                                    });
                                }
                            });
                        })
                        .build())

                .option(ButtonOption.createBuilder()
                        .name(Text.translatable("residue.config.tts_cancel"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.tts_cancel.desc")))
                        .text(Text.translatable("residue.config.tts_cancel.button"))
                        .action((screen, option) -> {
                            TTSServerManager.getInstance().cancelDownload();
                            MinecraftClient client = MinecraftClient.getInstance();
                            if (client.player != null)
                                client.player.sendMessage(Text.translatable("residue.message.download_cancelling"));
                        })
                        .build())

                .option(LabelOption.create(Text.translatable("residue.config.tts_server_manual.label")))
                .option(ttsServerStatusLabel.getOption())
                .option(ButtonOption.createBuilder()
                        .name(Text.translatable("residue.config.tts_server_toggle"))
                        .description(OptionDescription.of(
                                Text.translatable("residue.config.tts_server_toggle.desc")))
                        .text(Text.translatable("residue.config.tts_server_toggle.button"))
                        .action((screen, option) ->
                                TTSServerController.toggle(ttsServerStatusLabel::setRunning))
                        .build())

                .build();
    }
}