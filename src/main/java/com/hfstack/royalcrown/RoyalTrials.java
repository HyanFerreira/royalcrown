package com.hfstack.royalcrown;

import com.hfstack.royalcrown.network.OpenAdvisorScreenS2C;
import com.hfstack.royalcrown.network.RCNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.event.TickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RoyalTrials {

    private static final ResourceLocation MINECOLONIES_CITIZEN = new ResourceLocation("minecolonies", "citizen");
    // no topo:
    private static final ResourceLocation TOWN_HALL_BLOCK_1 = new ResourceLocation("minecolonies", "blockhuttownhall"); // principal
    private static final ResourceLocation TOWN_HALL_BLOCK_2 = new ResourceLocation("minecolonies", "townhall");         // fallback

    // sessão de “onda” de defesa: kills recentes que contam como 1 defesa
    private static final Map<UUID, Integer> sessionKills = new HashMap<>();
    private static final Map<UUID, Integer> sessionExpireTick = new HashMap<>();

    private static final Map<UUID, Integer> MSG_COOLDOWN = new HashMap<>();
    private static final int MSG_COOLDOWN_TICKS = 80; // ~4s

    private static final String ADVISOR_TAG = "RoyalAdvisor";

    private static boolean canNotify(Player p, ServerLevel sl) {
        int now = sl.getServer().getTickCount();
        int next = MSG_COOLDOWN.getOrDefault(p.getUUID(), 0);
        if (now < next) return false;
        MSG_COOLDOWN.put(p.getUUID(), now + MSG_COOLDOWN_TICKS);
        return true;
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new RoyalTrials());
    }

    private static final Map<UUID, Integer> READY_CHECK_NEXT = new HashMap<>();

    /* ------------ UTILs ------------ */

    private static boolean isMinecoloniesCitizen(Entity e) {
        if (e == null) return false;
        var key = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        return key != null && key.equals(MINECOLONIES_CITIZEN);
    }

    /**
     * Considera “perto da colônia” se houver algum cidadão a R blocos do ponto.
     */
    private static boolean nearColony(Level level, double x, double y, double z, double radius) {
        if (level.isClientSide()) return false;
        List<LivingEntity> list = level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius),
                RoyalTrials::isMinecoloniesCitizen
        );
        return !list.isEmpty();
    }

    /**
     * Conta cidadãos visíveis numa área (para o requisito de população).
     */
    public static int countCitizensAround(Level level, Player center, double radius) {
        List<LivingEntity> list = level.getEntitiesOfClass(
                LivingEntity.class,
                center.getBoundingBox().inflate(radius),
                RoyalTrials::isMinecoloniesCitizen
        );
        return list.size();
    }

    private static boolean isTownHallBlock(BlockState state) {
        var key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return key != null && (key.equals(TOWN_HALL_BLOCK_1) || key.equals(TOWN_HALL_BLOCK_2));
    }

    /* ------------ EVENTOS ------------ */

    public static void ensureAdvisorNearTownHall(ServerLevel sl, BlockPos base) {
        // já existe um por perto?
        if (!sl.getEntitiesOfClass(RoyalAdvisorEntity.class, new AABB(base).inflate(48)).isEmpty())
            return;

        spawnAdvisorNearTownHall(sl, base);
    }

    /**
     * Notifica UMA ÚNICA VEZ que todas as provas foram concluídas.
     */
    private static void notifyReadyOnce(ServerLevel sl, Player p) {
        var tag = p.getPersistentData();
        if (tag.getBoolean("RC_ReadyNotified")) return;

        int reqCit = RCConfig.COMMON.TRIAL_REQUIRED_CITIZENS.get();
        int reqDef = RCConfig.COMMON.TRIAL_DEFENSES_REQUIRED.get();
        int citizens = getColonyCitizenCount(sl, p);
        int defDone = RoyalProgressData.get(sl).getDefenses(p.getUUID());

        if (citizens >= reqCit && defDone >= reqDef) {
            tag.putBoolean("RC_ReadyNotified", true);
            p.sendSystemMessage(Component.translatable("msg.royalcrown.ready"));
        }
    }

    /**
     * Mata mob hostil perto da colônia -> conta para defesa (em “ondas”).
     */
    @SubscribeEvent
    public void onHostileKilledNearColony(LivingDeathEvent e) {
        if (e.getEntity().level().isClientSide()) return;
        if (!(e.getEntity() instanceof Enemy)) return;

        Entity src = e.getSource().getEntity();
        if (!(src instanceof Player p)) return;

        ServerLevel sl = (ServerLevel) e.getEntity().level();

        // ✅ Só começa a contar depois que o jogador aceita no Conselheiro (INTRO4)
        if (!p.getPersistentData().getBoolean("RC_Accepted")) return;

        double R = RCConfig.COMMON.TRIAL_NEAR_RADIUS.get();
        if (!nearColony(sl, e.getEntity().getX(), e.getEntity().getY(), e.getEntity().getZ(), R)) return;

        RoyalProgressData data = RoyalProgressData.get(sl);
        int reqDef = RCConfig.COMMON.TRIAL_DEFENSES_REQUIRED.get();

        // já terminou as defesas? nada a fazer (evita spam)
        if (data.getDefenses(p.getUUID()) >= reqDef) return;

        int now = sl.getServer().getTickCount();
        int timeout = RCConfig.COMMON.TRIAL_WAVE_TIMEOUT_TICKS.get();

        if (now > sessionExpireTick.getOrDefault(p.getUUID(), 0)) {
            sessionKills.put(p.getUUID(), 0);
        }

        int newKills = sessionKills.getOrDefault(p.getUUID(), 0) + 1;
        sessionKills.put(p.getUUID(), newKills);
        sessionExpireTick.put(p.getUUID(), now + timeout);

        int needWave = RCConfig.COMMON.TRIAL_WAVE_KILLS.get();
        if (newKills >= needWave) {
            data.addDefense(p.getUUID(), 1);
            sessionKills.put(p.getUUID(), 0);

            int nowDone = data.getDefenses(p.getUUID());
            int reqCit = RCConfig.COMMON.TRIAL_REQUIRED_CITIZENS.get();
            int citizens = getColonyCitizenCount(sl, p);

            if (nowDone >= reqDef) {
                // Já terminou as defesas — verifica se JÁ tem população suficiente.
                if (citizens >= reqCit) {
                    // As DUAS metas estão feitas -> notifica uma vez
                    notifyReadyOnce(sl, p);
                } else {
                    // Falta população ainda — feedback leve
                    p.displayClientMessage(
                            Component.translatable("msg.royalcrown.defense_done_both", nowDone, reqDef, citizens, reqCit),
                            true
                    );
                }
            } else {
                // Ainda faltam defesas — mensagem de progresso normal
                p.displayClientMessage(Component.translatable("msg.royalcrown.defense_done", nowDone, reqDef), true);
            }
        } else {
            p.displayClientMessage(Component.translatable("msg.royalcrown.defense_progress", newKills, needWave), true);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Player p = e.player;
        if (p.level().isClientSide()) return;
        if (!(p.level() instanceof ServerLevel sl)) return;

        // Só vale após aceitar a missão
        if (!p.getPersistentData().getBoolean("RC_Accepted")) return;

        // Checa a cada ~3s por jogador
        int now = sl.getServer().getTickCount();
        int next = READY_CHECK_NEXT.getOrDefault(p.getUUID(), 0);
        if (now < next) return;
        READY_CHECK_NEXT.put(p.getUUID(), now + 60); // 60 ticks ~ 3s

        notifyReadyOnce(sl, p);
    }


    public static BlockPos findNearestTownHall(ServerLevel sl, BlockPos center, int radius) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int y1 = Math.max(sl.getMinBuildHeight(), center.getY() - 8);
        int y2 = Math.min(sl.getMaxBuildHeight() - 1, center.getY() + 8);

        BlockPos best = null;
        int bestDist = Integer.MAX_VALUE;

        for (int y = y1; y <= y2; y++) {
            for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    pos.set(x, y, z);
                    BlockState state = sl.getBlockState(pos);
                    var key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                    if (key != null && (key.equals(TOWN_HALL_BLOCK_1) || key.equals(TOWN_HALL_BLOCK_2))) {
                        int d = pos.distManhattan(center);
                        if (d < bestDist) {
                            bestDist = d;
                            best = pos.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    public static void giveCrownIfMissing(Player p) {
        // evita duplicar se já tiver uma
        boolean has = false;
        for (ItemStack s : p.getInventory().items) {
            if (!s.isEmpty() && s.getItem() == ModRegistry.CROWN.get()) {
                has = true;
                break;
            }
        }
        if (!has) {
            ItemStack crown = new ItemStack(ModRegistry.CROWN.get());
            if (!p.addItem(crown)) {
                p.drop(crown, false);
            }
        }
    }

    public static void celebrate(ServerLevel sl, Player p) {
        // Mensagem persistente no chat
        p.sendSystemMessage(Component.translatable("msg.royalcrown.crowned_toast"));

        // Som de conquista
        sl.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);

        // 3 fogos dourados ao redor do jogador
        for (int i = 0; i < 3; i++) {
            double dx = p.getX() + (sl.random.nextDouble() - 0.5) * 2.5;
            double dz = p.getZ() + (sl.random.nextDouble() - 0.5) * 2.5;
            spawnGoldFirework(sl, dx, p.getY() + 0.5, dz, 2);
        }
    }

    private static void spawnGoldFirework(ServerLevel sl, double x, double y, double z, int flight) {
        ItemStack fw = new ItemStack(Items.FIREWORK_ROCKET);
        CompoundTag tag = new CompoundTag();
        CompoundTag fireworks = new CompoundTag();
        ListTag explosions = new ListTag();

        CompoundTag exp = new CompoundTag();
        exp.putByte("Type", (byte) 1);               // estrela
        exp.putIntArray("Colors", new int[]{0xFFD700}); // dourado
        exp.putBoolean("Trail", true);
        exp.putBoolean("Flicker", true);
        explosions.add(exp);

        fireworks.put("Explosions", explosions);
        fireworks.putByte("Flight", (byte) flight);
        tag.put("Fireworks", fireworks);
        fw.setTag(tag);

        FireworkRocketEntity rocket = new FireworkRocketEntity(sl, x, y, z, fw);
        sl.addFreshEntity(rocket);
    }

    @SubscribeEvent
    public void onAdvisorInteract(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract e) {
        if (e.getLevel().isClientSide()) return;
        if (e.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(e.getTarget() instanceof LivingEntity npc)) return;
        if (!npc.getPersistentData().getBoolean(ADVISOR_TAG)) return;

        Player p = e.getEntity();
        ServerLevel sl = (ServerLevel) e.getLevel();

        int reqCit = RCConfig.COMMON.TRIAL_REQUIRED_CITIZENS.get();
        int reqDef = RCConfig.COMMON.TRIAL_DEFENSES_REQUIRED.get();

        // ainda não aceitou -> INTRO1
        if (!p.getPersistentData().getBoolean("RC_Accepted")) {
            RCNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> (ServerPlayer) p),
                    new OpenAdvisorScreenS2C(
                            OpenAdvisorScreenS2C.Mode.INTRO1,
                            reqCit, reqDef, 0, 0, npc.getId()));
            e.setCancellationResult(InteractionResult.SUCCESS);
            e.setCanceled(true);
            return;
        }

        // já aceitou
        int citizens = getColonyCitizenCount(sl, p);
        int defDone = RoyalProgressData.get(sl).getDefenses(p.getUUID());
        boolean canCrown = (citizens >= reqCit && defDone >= reqDef);

        var data = RoyalProgressData.get(sl);
        boolean unique = RCConfig.COMMON.UNIQUE_PER_WORLD.get();

        // se já está coroado (ou mundo já tem um coroado), fazemos fluxo de despedida
        boolean alreadyCrownedHere = (unique && data.isCrownGiven());
        boolean thisPlayerIsCrowned = p.getPersistentData().getBoolean("RC_Crowned");

        if (thisPlayerIsCrowned || alreadyCrownedHere) {
            RCNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> (ServerPlayer) p),
                    new OpenAdvisorScreenS2C(
                            OpenAdvisorScreenS2C.Mode.FAREWELL1,
                            reqCit, reqDef, citizens, defDone, npc.getId()));
            e.setCancellationResult(InteractionResult.SUCCESS);
            e.setCanceled(true);
            return;
        }

        // pode coroar -> abre a janelinha de CLAIM
        if (canCrown && (!unique || !data.isCrownGiven())) {
            RCNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> (ServerPlayer) p),
                    new OpenAdvisorScreenS2C(
                            OpenAdvisorScreenS2C.Mode.CLAIM1,
                            reqCit, reqDef, citizens, defDone, npc.getId()));
        } else {
            // STATUS normal
            RCNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> (ServerPlayer) p),
                    new OpenAdvisorScreenS2C(
                            OpenAdvisorScreenS2C.Mode.STATUS,
                            reqCit, reqDef, citizens, defDone, npc.getId()));
        }

        e.setCancellationResult(InteractionResult.SUCCESS);
        e.setCanceled(true);
    }

    static void spawnAdvisorNearTownHall(ServerLevel sl, BlockPos base) {
        // Evita duplicado perto do Town Hall
        var already = sl.getEntitiesOfClass(
                RoyalAdvisorEntity.class,
                new AABB(base).inflate(32),
                e -> true
        );
        if (!already.isEmpty()) return;

        RoyalAdvisorEntity adv = ModRegistry.ROYAL_ADVISOR.get().create(sl);
        if (adv == null) return;

        // Procura um bloco livre próximo
        BlockPos spawn = base;
        boolean found = false;
        for (int dx = -3; dx <= 3 && !found; dx++) {
            for (int dz = -3; dz <= 3 && !found; dz++) {
                BlockPos p = base.offset(dx, 0, dz);
                if (sl.isEmptyBlock(p) && sl.getBlockState(p.below()).isSolidRender(sl, p.below())) {
                    spawn = p;
                    found = true;
                }
            }
        }

        adv.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, sl.random.nextFloat() * 360f, 0f);
        adv.setHome(base, 14);
        adv.setPersistenceRequired();
        adv.getPersistentData().putBoolean(ADVISOR_TAG, true); // tag pro diálogo
        adv.setCustomName(Component.translatable("entity.royalcrown.royal_advisor"));
        adv.setCustomNameVisible(true);

        sl.addFreshEntity(adv);
    }

    // ======== POPULAÇÃO EXATA DA COLÔNIA (via reflexão) ========

    private static final int FALLBACK_POP_RADIUS = 200;

    public static int getColonyCitizenCount(ServerLevel sl, Player p) {
        // Tenta via API do MineColonies
        try {
            Object colonyManager = getColonyManagerInstance();
            if (colonyManager != null) {
                Object colony = invokeBestColonyLookup(colonyManager, sl, p.blockPosition());
                if (colony != null) {
                    // 1) métodos diretos que retornam int
                    Integer direct = tryGetInt(colony,
                            new String[]{"getCitizenCount", "getPopulation", "getPopulationSize", "getNumberOfCitizens"});
                    if (direct != null) return direct;

                    // 2) getCitizens() -> Collection
                    Object coll = invokeFirstCompatible(colony,
                            new String[]{"getCitizens", "getCitizenList"}, new Object[]{});
                    if (coll instanceof java.util.Collection<?> c) return c.size();

                    // 3) via citizen manager
                    Object cm = invokeFirstCompatible(colony,
                            new String[]{"getCitizenManager", "getCitizenDataManager", "getColonyCitizenManager"}, new Object[]{});
                    if (cm != null) {
                        Integer viaMgr = tryGetInt(cm,
                                new String[]{"getCurrentCitizenCount", "getTotalCitizenCount", "getCitizenCount",
                                        "getPopulation", "getPopulationSize"});
                        if (viaMgr != null) return viaMgr;

                        Object coll2 = invokeFirstCompatible(cm,
                                new String[]{"getCitizens", "getCitizenList"}, new Object[]{});
                        if (coll2 instanceof java.util.Collection<?> c2) return c2.size();
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // Fallback: conta entidades de cidadão num raio
        return countCitizensAround(sl, p, FALLBACK_POP_RADIUS);
    }

    private static Integer tryGetInt(Object target, String[] methodNames) {
        for (String n : methodNames) {
            try {
                var m = target.getClass().getMethod(n);
                Object v = m.invoke(target);
                if (v instanceof Number num) return num.intValue();
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object getColonyManagerInstance() {
        String[] candidates = {
                "com.minecolonies.api.colony.management.ColonyManager",
                "com.minecolonies.api.colony.ColonyManager"
        };
        for (String cn : candidates) {
            try {
                Class<?> clazz = Class.forName(cn);
                var m = clazz.getMethod("getInstance");
                Object inst = m.invoke(null);
                if (inst != null) return inst;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object invokeBestColonyLookup(Object colonyManager, ServerLevel sl, BlockPos pos) {
        String[] names = {"getColonyByPos", "getClosestColony", "getColonyByPosition", "getNearestColony", "getColony"};
        Object[][] argSets = new Object[][]{{sl, pos}, {pos, sl}, {pos}, {sl}};
        for (String n : names) {
            for (Object[] args : argSets) {
                Object out = tryInvoke(colonyManager, n, args);
                if (out != null) return out;
            }
        }
        return null;
    }

    private static Object invokeFirstCompatible(Object target, String[] names, Object[] args) {
        for (String n : names) {
            Object out = tryInvoke(target, n, args);
            if (out != null) return out;
        }
        return null;
    }

    private static Object tryInvoke(Object target, String name, Object[] args) {
        for (var m : target.getClass().getMethods()) {
            if (!m.getName().equals(name)) continue;
            if (m.getParameterCount() != args.length) continue;
            var pts = m.getParameterTypes();
            boolean ok = true;
            for (int i = 0; i < pts.length; i++) {
                Object a = args[i];
                if (a != null && !wrap(pts[i]).isAssignableFrom(a.getClass())) {
                    ok = false;
                    break;
                }
            }
            if (!ok) continue;
            try {
                return m.invoke(target, args);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Class<?> wrap(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == int.class) return Integer.class;
        if (c == boolean.class) return Boolean.class;
        if (c == long.class) return Long.class;
        if (c == double.class) return Double.class;
        if (c == float.class) return Float.class;
        if (c == byte.class) return Byte.class;
        if (c == short.class) return Short.class;
        if (c == char.class) return Character.class;
        return c;
    }

}
