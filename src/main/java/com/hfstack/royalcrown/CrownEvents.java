package com.hfstack.royalcrown;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.*;

public class CrownEvents {

    private static final ResourceLocation MINECOLONIES_CITIZEN = new ResourceLocation("minecolonies", "citizen");

    // player UUID -> target UUID / expiração
    private static final Map<UUID, UUID> ORDER_TARGET = new HashMap<>();

    // guarda -> próximo tick permitido para ataque forçado
    private static final WeakHashMap<Mob, Integer> GUARD_MELEE_CD = new WeakHashMap<>();

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new CrownEvents());
    }

    /**
     * Alvos que NUNCA devem ser atacados pelos guardas por ordem da coroa.
     */
    private static boolean isProtectedColonist(Entity e) {
        if (isMinecoloniesCitizen(e)) return true;                 // guardas/civis da colônia
        if (e instanceof TamableAnimal ta && ta.isTame()) return true;  // lobo, gato, papagaio etc.
        if (e instanceof AbstractHorse ah && ah.isTamed()) return true; // cavalo, burro, mula, lhama, camelo
        return false;
    }

    /* ---------------------- UTIL ---------------------- */

    private static boolean isWearingCrown(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return head.getItem() instanceof ArmorItem armor
                && armor.getMaterial() == ArmorMaterials.GOLD
                && Objects.equals(
                ForgeRegistries.ITEMS.getKey(head.getItem()),
                ForgeRegistries.ITEMS.getKey(ModRegistry.CROWN.get())
        );
    }

    private static boolean isMinecoloniesCitizen(Entity e) {
        if (e == null) return false;
        var key = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        return key != null && key.equals(MINECOLONIES_CITIZEN);
    }

    /**
     * Guarda de MELEE (espada/machado). Arqueiros ficam de fora do ataque forçado.
     */
    private static boolean isMeleeGuard(Mob mob) {
        if (!isMinecoloniesCitizen(mob)) return false;
        Item main = mob.getMainHandItem().getItem();
        return main instanceof SwordItem || main instanceof AxeItem;
    }

    /**
     * Heurística geral de “provável guarda” (inclui arqueiros).
     */
    private static boolean isLikelyGuard(Mob mob) {
        if (!isMinecoloniesCitizen(mob)) return false;
        Item main = mob.getMainHandItem().getItem();
        return main instanceof SwordItem
                || main instanceof AxeItem
                || main instanceof BowItem
                || main instanceof CrossbowItem;
    }

    /**
     * Força perseguir e manter alvo na cabeça da IA.
     */
    private static void rallyNearbyGuards(Player king, LivingEntity target, double radius) {
        if (king.level().isClientSide()) return;
        if (isProtectedColonist(target)) return;
        AABB area = king.getBoundingBox().inflate(radius);

        List<Mob> nearby = king.level().getEntitiesOfClass(
                Mob.class, area,
                mob -> mob.isAlive() && isLikelyGuard(mob) && mob.hasLineOfSight(target)
        );

        for (Mob mob : nearby) {
            mob.setTarget(target);
            mob.setAggressive(true);
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

            PathNavigation nav = mob.getNavigation();
            if (nav != null) nav.moveTo(target, 1.25D);

            // “Empurra” a IA para modo combate
            mob.setLastHurtByMob(null);
            mob.setLastHurtMob(target);
            Brain<?> brain = mob.getBrain();
            if (brain != null) {
                brain.setMemory(MemoryModuleType.ATTACK_TARGET, target);
                brain.eraseMemory(MemoryModuleType.HURT_BY);
            }
        }
    }

    /**
     * Alcance de ataque simples (2 blocos + as larguras dos hitboxes).
     */
    private static double meleeReachSqr(Mob mob, LivingEntity target) {
        double base = 2.0D;
        double extra = mob.getBbWidth() * 0.5D + target.getBbWidth() * 0.5D;
        double reach = base + extra;
        return reach * reach;
    }

    /**
     * Força um ataque de melee respeitando cooldown aproximado do vanilla,
     * e calcula o dano a partir dos atributos do item na mão (sem usar atributos do mob).
     */
    private static void tryForceMeleeAttack(Mob mob, LivingEntity target) {
        if (!isMeleeGuard(mob)) return;
        if (!mob.isAlive() || !target.isAlive()) return;
        if (!mob.getSensing().hasLineOfSight(target)) return;
        if (isProtectedColonist(target)) return;

        int now = mob.tickCount;
        int next = GUARD_MELEE_CD.getOrDefault(mob, 0);
        if (now < next) return;

        double distSqr = mob.distanceToSqr(target);
        if (distSqr <= meleeReachSqr(mob, target)) {
            mob.swing(InteractionHand.MAIN_HAND, true);

            // dano do item * multiplicador da config
            float damage = weaponDamageFromItem(mob) * RCConfig.COMMON.FORCED_DAMAGE_MULT.get().floatValue();
            target.hurt(mob.damageSources().mobAttack(mob), damage);

            // cooldown baseado no Attack Speed do item * multiplicador da config
            int baseCd = attackCooldownFromItemTicks(mob);
            int cd = Math.max(1, (int) Math.round(baseCd * RCConfig.COMMON.FORCED_COOLDOWN_MULT.get().doubleValue()));
            GUARD_MELEE_CD.put(mob, now + cd);
        } else {
            PathNavigation nav = mob.getNavigation();
            if (nav != null) nav.moveTo(target, 1.25D);
        }
    }

    private static int attackCooldownFromItemTicks(Mob mob) {
        // Attack Speed efetivo = 4.0 (base do player) + mods do item (ADD/MUL_BASE/MUL_TOTAL)
        double atkSpeed = 4.0;
        ItemStack stack = mob.getMainHandItem();
        if (!stack.isEmpty()) {
            var mods = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
            var list = mods.get(Attributes.ATTACK_SPEED);
            if (list != null && !list.isEmpty()) {
                double add = 0.0, mulB = 0.0, mulT = 0.0;
                for (var m : list) {
                    switch (m.getOperation()) {
                        case ADDITION -> add += m.getAmount();
                        case MULTIPLY_BASE -> mulB += m.getAmount();
                        case MULTIPLY_TOTAL -> mulT += m.getAmount();
                    }
                }
                atkSpeed = (4.0 + add) * (1.0 + mulB);  // base 4.0 como no player
                atkSpeed = atkSpeed * (1.0 + mulT);
            } else {
                // Fallback por tipo de arma (aprox. vanilla)
                Item it = stack.getItem();
                if (it instanceof SwordItem) return 13; // ~0.625s
                if (it instanceof AxeItem) return 22; // ~1.10s
                return 15;
            }
        }
        atkSpeed = Math.max(0.1, atkSpeed);
        return Math.max(1, (int) Math.round(20.0 / atkSpeed));
    }

    /**
     * Cooldown aproximado vanilla por tipo de arma (em ticks).
     */
    private static int weaponCooldownTicks(Mob mob) {
        Item item = mob.getMainHandItem().getItem();
        if (item instanceof SwordItem) return 13; // ~0.625s
        if (item instanceof AxeItem) return 20; // ~1.0s
        return 15; // fallback
    }

    /**
     * Extrai o dano do item na mão via atributos do PRÓPRIO item (não do mob).
     * Isso evita o crash do generic.attack_damage ausente.
     */
    private static float weaponDamageFromItem(Mob mob) {
        ItemStack stack = mob.getMainHandItem();
        if (stack.isEmpty()) return 2.0F; // mão limpa: um tapa fraquinho

        // Base segura caso o item não exponha modificadores
        float damage = 2.0F;

        // Lê modificadores do item para MAINHAND (inclui Attack Damage do item)
        var mods = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        var list = mods.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        if (list != null && !list.isEmpty()) {
            // Calcula com as 3 operações possíveis (ordem simplificada)
            double base = 1.0;  // base “neutra” pra multiplicações
            double add = 0.0;
            double mulB = 0.0;
            double mulT = 0.0;
            for (var mod : list) {
                switch (mod.getOperation()) {
                    case ADDITION -> add += mod.getAmount();
                    case MULTIPLY_BASE -> mulB += mod.getAmount();
                    case MULTIPLY_TOTAL -> mulT += mod.getAmount();
                }
            }
            double val = (base + add) * (1.0 + mulB);
            val = val * (1.0 + mulT);
            damage = (float) Math.max(1.0, val); // nunca menos de 1
        } else {
            // Sem mods no item? Ajuste mínimo por tipo
            Item it = stack.getItem();
            if (it instanceof SwordItem) damage = 6.0F;  // perto de uma espada de ferro
            else if (it instanceof AxeItem) damage = 7.0F;
        }

        return damage;
    }

    /**
     * Esquece alvo/combate e para o pathfinding.
     */
    private static void pacifyGuard(Mob mob) {
        mob.setTarget(null);
        mob.setAggressive(false);
        PathNavigation nav = mob.getNavigation();
        if (nav != null) nav.stop();

        mob.setLastHurtByMob(null);
        mob.setLastHurtMob(null);
        Brain<?> brain = mob.getBrain();
        if (brain != null) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            brain.eraseMemory(MemoryModuleType.HURT_BY);
            brain.eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
        }
    }

    /**
     * Limpa qualquer cidadão mirando o rei (failsafe).
     */
    private static void clearGuardsTargeting(Player king, double radius) {
        if (king.level().isClientSide()) return;
        AABB area = king.getBoundingBox().inflate(radius);

        List<Mob> nearby = king.level().getEntitiesOfClass(
                Mob.class, area,
                mob -> isMinecoloniesCitizen(mob) && mob.isAlive()
        );

        for (Mob mob : nearby) {
            if (king.equals(mob.getTarget())) {
                pacifyGuard(mob);
            }
        }
    }

    private static void rememberOrder(Player king, LivingEntity target) {
        ORDER_TARGET.put(king.getUUID(), target.getUUID());
    }

    private static LivingEntity findOrderedTarget(Player king) {
        UUID tid = ORDER_TARGET.get(king.getUUID());
        if (tid == null) return null;
        if (!(king.level() instanceof ServerLevel sl)) return null;
        Entity e = sl.getEntity(tid);
        return (e instanceof LivingEntity le && le.isAlive()) ? le : null;
    }

    /* --------------- REGRAS DA COROA --------------- */

    @SubscribeEvent
    public void onPlayerHitsGuard_whileCrowned(LivingAttackEvent e) {
        if (!(e.getSource().getEntity() instanceof Player player)) return;
        if (!isWearingCrown(player)) return;
        if (isMinecoloniesCitizen(e.getEntity())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onGuardTargetChange(LivingChangeTargetEvent e) {
        if (!(e.getEntity() instanceof Mob mob)) return;
        if (!isMinecoloniesCitizen(mob)) return;
        if (e.getNewTarget() instanceof Player player && isWearingCrown(player)) {
            e.setCanceled(true);
            pacifyGuard(mob);
        }
    }

    @SubscribeEvent
    public void onCrownedPlayerAttackedByGuard(LivingAttackEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (!isWearingCrown(player)) return;

        Entity src = e.getSource().getEntity();
        if (src instanceof Mob mob && isMinecoloniesCitizen(mob)) {
            e.setCanceled(true);
            pacifyGuard(mob);
        }
    }

    // REI bate em qualquer mob => guardas (melee e arqueiros) focam; registramos ordem
    @SubscribeEvent
    public void onPlayerAttacks(LivingAttackEvent e) {
        if (!(e.getSource().getEntity() instanceof Player player)) return;
        if (!isWearingCrown(player)) return;

        LivingEntity target = e.getEntity();
        if (target instanceof Player) return;   // evita PVP
        if (isProtectedColonist(target)) return; // evita ordenar ataque em guardas/civis

        if (RCConfig.COMMON.ENABLE_WOLF_MODE.get()) {
            double radius = RCConfig.COMMON.GUARD_HELP_RADIUS.get();
            rallyNearbyGuards(player, target, radius);
            rememberOrder(player, target);
        }
    }

    // Rei leva dano => guardas focam e registramos ordem
    @SubscribeEvent
    public void onPlayerHurt(LivingHurtEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (!isWearingCrown(player)) return;
        if (!RCConfig.COMMON.ENABLE_DEFEND_NEUTRALS.get()) return;

        DamageSource src = e.getSource();
        Entity direct = src.getDirectEntity();
        Entity attacker = src.getEntity();

        LivingEntity badGuy = null;
        if (attacker instanceof LivingEntity le) badGuy = le;
        else if (direct instanceof Projectile p && p.getOwner() instanceof LivingEntity le) badGuy = le;
        else if (direct instanceof LivingEntity le2) badGuy = le2;

        if (badGuy != null && isProtectedColonist(badGuy)) {
            // se for um Mob cidadão (ex.: guarda) que, por algum motivo, te atingiu, pacifica
            if (badGuy instanceof Mob m && isMinecoloniesCitizen(m)) {
                pacifyGuard(m);
            }
            return; // não convoca guardas contra cidadãos/guardas
        }

        if (badGuy != null) {
            double radius = RCConfig.COMMON.GUARD_HELP_RADIUS.get();
            rallyNearbyGuards(player, badGuy, radius);
            rememberOrder(player, badGuy);
        }
    }

    // Tick: reforça ordem + força ataque de melee quando em alcance
    @SubscribeEvent
    public void onCrownedPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Player player = e.player;
        if (player.level().isClientSide()) return;

        double radius = RCConfig.COMMON.GUARD_HELP_RADIUS.get();

        // Se tirou a coroa, limpamos a ordem e saímos
        if (!isWearingCrown(player)) {
            ORDER_TARGET.remove(player.getUUID());
            return;
        }

        // Fail-safe: qualquer cidadão mirando o rei desiste
        clearGuardsTargeting(player, radius);

        // Mantém a ordem ATÉ O ALVO MORRER
        LivingEntity target = findOrderedTarget(player);
        if (target != null && target.isAlive()) {
            // manter perseguição/target (serve para hostis e neutros)
            rallyNearbyGuards(player, target, radius);

            // Só forçar golpes se o alvo NÃO for hostil (hostis usam o ritmo nativo do MineColonies)
            if (!(target instanceof Enemy)) {
                AABB area = player.getBoundingBox().inflate(radius);
                List<Mob> melee = player.level().getEntitiesOfClass(
                        Mob.class, area,
                        mob -> mob.isAlive() && isMeleeGuard(mob) && target.equals(mob.getTarget())
                );
                for (Mob mob : melee) {
                    tryForceMeleeAttack(mob, target);
                }
            }
        } else {
            // alvo morreu/saiu -> limpa a ordem
            ORDER_TARGET.remove(player.getUUID());
        }
    }
}
