package com.hfstack.royalcrown;

import net.minecraftforge.common.ForgeConfigSpec;

public class RCConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        COMMON = new Common(b);
        COMMON_SPEC = b.build();
    }

    public static class Common {
        public final ForgeConfigSpec.BooleanValue DISABLE_GUARD_RETALIATION;
        public final ForgeConfigSpec.BooleanValue ENABLE_WOLF_MODE;
        public final ForgeConfigSpec.BooleanValue ENABLE_DEFEND_NEUTRALS;
        public final ForgeConfigSpec.DoubleValue GUARD_HELP_RADIUS;

        // novos
        public final ForgeConfigSpec.DoubleValue FORCED_COOLDOWN_MULT;
        public final ForgeConfigSpec.DoubleValue FORCED_DAMAGE_MULT;

        public final ForgeConfigSpec.IntValue TRIAL_REQUIRED_CITIZENS;     // cidadãos mínimos
        public final ForgeConfigSpec.IntValue TRIAL_DEFENSES_REQUIRED;     // quantas defesas
        public final ForgeConfigSpec.IntValue TRIAL_WAVE_KILLS;            // kills p/ contar 1 “defesa”
        public final ForgeConfigSpec.IntValue TRIAL_WAVE_TIMEOUT_TICKS;    // tempo para emendar kills (2 min = 2400)
        public final ForgeConfigSpec.DoubleValue TRIAL_NEAR_RADIUS;          // raio para considerar “perto da colônia”
        public final ForgeConfigSpec.BooleanValue UNIQUE_PER_WORLD;          // só 1 coroa por mundo?
        public final ForgeConfigSpec.BooleanValue ALLOW_RECLAIM;             // permitir reaver se perder?


        Common(ForgeConfigSpec.Builder b) {
            b.push("crown");

            DISABLE_GUARD_RETALIATION = b
                    .comment("Guardas NÃO revidam se você acertar um deles usando a coroa.")
                    .define("disableGuardRetaliation", true);

            ENABLE_WOLF_MODE = b
                    .comment("Guardas ajudam quando o rei ataca QUALQUER mob (modo 'lobo').")
                    .define("enableWolfMode", true);

            ENABLE_DEFEND_NEUTRALS = b
                    .comment("Guardas defendem o rei contra mobs neutros (ex.: golem de ferro).")
                    .define("enableDefendNeutrals", true);

            GUARD_HELP_RADIUS = b
                    .comment("Raio (em blocos) para convocar guardas ao redor do rei.")
                    .defineInRange("guardHelpRadius", 16.0, 4.0, 64.0);

            // ====== inicialize aqui os novos campos ======
            FORCED_COOLDOWN_MULT = b
                    .comment("Multiplicador do cooldown do ataque forçado (baseado no Attack Speed da arma).",
                            "Ex.: 3.0 deixa uma espada (~13 ticks) ≈ 39 ticks (~2s).")
                    .defineInRange("forcedCooldownMultiplier", 3.0, 0.5, 10.0);

            FORCED_DAMAGE_MULT = b
                    .comment("Multiplicador do dano aplicado no ataque forçado (sobre o dano do item).")
                    .defineInRange("forcedDamageMultiplier", 1.0, 0.1, 5.0);
            // ============================================

            TRIAL_REQUIRED_CITIZENS = b
                    .comment("Cidadãos mínimos na colônia para conquistar a coroa.")
                    .defineInRange("trial.requiredCitizens", 4, 1, 500);

            TRIAL_DEFENSES_REQUIRED = b
                    .comment("Quantas defesas completas são necessárias.")
                    .defineInRange("trial.defensesRequired", 2, 1, 100);

            TRIAL_WAVE_KILLS = b
                    .comment("Kills de mobs HOSTIS perto da colônia que contam como 1 defesa.")
                    .defineInRange("trial.waveKills", 2, 1, 100);

            TRIAL_WAVE_TIMEOUT_TICKS = b
                    .comment("Tempo (ticks) para emendar kills num mesmo 'ataque' (padrão ~2 min = 2400).")
                    .defineInRange("trial.waveTimeoutTicks", 2400, 100, 20_000);

            TRIAL_NEAR_RADIUS = b
                    .comment("Raio (blocos) para considerar que o combate foi 'perto da colônia'.")
                    .defineInRange("trial.nearRadius", 48.0, 8.0, 128.0);

            UNIQUE_PER_WORLD = b
                    .comment("Se true, apenas UMA coroa pode ser reclamada no mundo inteiro.")
                    .define("trial.uniquePerWorld", true);

            ALLOW_RECLAIM = b
                    .comment("Permite reaver a coroa se perdida (se UNIQUE_PER_WORLD==true, só o mesmo jogador).")
                    .define("trial.allowReclaim", false);

            b.pop();
        }
    }
}
