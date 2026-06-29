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

        public final ForgeConfigSpec.BooleanValue CORONATION_ENABLED;
        public final ForgeConfigSpec.IntValue CORONATION_GATHER_TICKS;
        public final ForgeConfigSpec.IntValue CORONATION_CELEBRATE_TICKS;
        public final ForgeConfigSpec.DoubleValue CORONATION_SEARCH_RADIUS;
        public final ForgeConfigSpec.IntValue CORONATION_MAX_PARTICIPANTS;
        public final ForgeConfigSpec.DoubleValue CORONATION_JUMP_CHANCE;

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
                    .defineInRange("trial.requiredCitizens", 15, 1, 500);

            TRIAL_DEFENSES_REQUIRED = b
                    .comment("Quantas defesas completas são necessárias.")
                    .defineInRange("trial.defensesRequired", 5, 1, 100);

            TRIAL_WAVE_KILLS = b
                    .comment("Kills de mobs HOSTIS perto da colônia que contam como 1 defesa.")
                    .defineInRange("trial.waveKills", 5, 1, 100);

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

            CORONATION_ENABLED = b
                    .comment("Ativa a cerimônia de coroação antes da entrega da coroa.")
                    .define("coronation.enabled", true);

            CORONATION_GATHER_TICKS = b
                    .comment("Duração da fase em que cidadãos são chamados para a cerimônia (ticks).")
                    .defineInRange("coronation.gatherTicks", 220, 20, 2400);

            CORONATION_CELEBRATE_TICKS = b
                    .comment("Duração da fase de celebração da cerimônia (ticks).")
                    .defineInRange("coronation.celebrateTicks", 220, 20, 2400);

            CORONATION_SEARCH_RADIUS = b
                    .comment("Raio para buscar cidadãos que participarão da cerimônia.")
                    .defineInRange("coronation.searchRadius", 64.0, 8.0, 160.0);

            CORONATION_MAX_PARTICIPANTS = b
                    .comment("Número máximo de cidadãos/guardas chamados para a cerimônia.")
                    .defineInRange("coronation.maxParticipants", 24, 1, 100);

            CORONATION_JUMP_CHANCE = b
                    .comment("Chance de cada cidadão pular em cada batida de celebração (0.0 a 1.0).")
                    .defineInRange("coronation.jumpChance", 0.35, 0.0, 1.0);

            b.pop();
        }
    }
}
