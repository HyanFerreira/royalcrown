package com.hfstack.royalcrown;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.*;

@Mod.EventBusSubscriber(modid = ModMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TownHallInteractHandler {

    private static final boolean USE_DELAY = true;        // true = agenda; false = spawn imediato
    private static final int ADVISOR_DELAY_TICKS = 300;   // tempo até o spawn (ex.: 300 = 15s) | 1200 para ~10min
    private static final int NOTIFY_DELAY_TICKS = 300;   // atraso da mensagem = 15s

    // cooldown antigo da msg de nível — deixei, mas não usamos mais.
    private static final Map<UUID, Integer> LVL_MSG_CD = new HashMap<>();
    private static final int LVL_MSG_COOLDOWN_TICKS = 100;

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().isClientSide()) return;
        if (e.getHand() != InteractionHand.MAIN_HAND) return; // evita offhand duplicado

        ServerLevel sl = (ServerLevel) e.getLevel();
        BlockPos pos = e.getPos();

        // só reage a blocos do MineColonies que tenham “townhall” no path
        Block block = sl.getBlockState(pos).getBlock();
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
        if (key == null || !"minecolonies".equals(key.getNamespace())) return;
        String path = key.getPath();
        if (!(path.contains("townhall") || path.contains("town_hall"))) return;

        AdvisorSpawnData data = AdvisorSpawnData.get(sl);

        // já agendado ou já spawnado? não repete nem mensagem
        if (data.isScheduled() || data.isSpawned()) return;

        // lê nível do Town Hall robustamente
        int level = getTownHallLevelRobust(sl, pos);
        ModMain.LOGGER.debug("[RoyalCrown] TownHall {} nível detectado = {}", pos, level);

        if (level < 1) {
            // silenciado (sem mensagens) — apenas não agenda
            // (Se quiser reativar aviso, descomente o trecho abaixo e use cooldown por jogador)
            /*
            UUID pid = e.getEntity().getUUID();
            int now = sl.getServer().getTickCount();
            int next = LVL_MSG_CD.getOrDefault(pid, 0);
            if (now >= next) {
                e.getEntity().sendSystemMessage(Component.literal(
                        "A Prefeitura ainda é Nível 0. Construa até o Nível 1 para que um Conselheiro seja enviado."
                ));
                LVL_MSG_CD.put(pid, now + LVL_MSG_COOLDOWN_TICKS);
            }
            */
            return;
        }

        // nível OK -> agenda (ou spawn imediato)
        if (!USE_DELAY) {
            RoyalTrials.ensureAdvisorNearTownHall(sl, pos.above());
            data.setSpawned(); // marca p/ não repetir
        } else {
            long due = sl.getGameTime() + ADVISOR_DELAY_TICKS;
            // agenda também a NOTIFICAÇÃO para 5s depois do clique
            data.schedule(sl, pos.above(), due, sl.dimension(), e.getEntity().getUUID(), NOTIFY_DELAY_TICKS);
            // (mensagem não é enviada aqui — ela sai no tick após 5s)

            // depois de data.schedule(...) OU do spawn imediato
            if (e.getEntity() instanceof ServerPlayer sp2) {
                RCAdvancements.grant(sp2, RCAdvancements.ROOT);
            }
        }
    }

    /* =======================================================================
       Leitura do nível do Town Hall — robusta via reflexão
       ======================================================================= */

    private static int getTownHallLevelRobust(ServerLevel sl, BlockPos hutPos) {
        Integer lvl = tryGetLevelFromBlockEntity(sl, hutPos);
        if (lvl != null) return lvl;
        return getTownHallLevelViaColony(sl, hutPos);
    }

    private static Integer tryGetLevelFromBlockEntity(ServerLevel sl, BlockPos pos) {
        try {
            BlockEntity be = sl.getBlockEntity(pos);
            if (be == null) return null;

            for (String name : List.of("getBuildingLevel", "getLevel", "getCurrentLevel")) {
                try {
                    Method m = be.getClass().getMethod(name);
                    Object val = m.invoke(be);
                    if (val instanceof Number n) return n.intValue();
                } catch (Throwable ignored) {
                }
            }

            try {
                Method gb = be.getClass().getMethod("getBuilding");
                Object building = gb.invoke(be);
                if (building != null) {
                    Integer bLvl = getBuildingLevel(building);
                    if (bLvl != null) return bLvl;
                }
            } catch (Throwable ignored) {
            }
        } catch (Throwable t) {
            ModMain.LOGGER.debug("[RoyalCrown] BE reflect failed at {}: {}", pos, t.toString());
        }
        return null;
    }

    private static int getTownHallLevelViaColony(ServerLevel sl, BlockPos hutPos) {
        try {
            Object colonyManager = getColonyManagerInstance();
            if (colonyManager == null) return 0;

            Object colony = invokeBestColonyLookup(colonyManager, sl, hutPos);
            if (colony == null) return 0;

            Object bm = invokeFirstCompatible(
                    colony,
                    new String[]{"getBuildingManager", "getIColonyBuildingManager", "getBuildings", "getColonyBuildingManager"},
                    new Object[]{}
            );
            if (bm == null) return 0;

            Object bAt = invokeFirstCompatible(
                    bm,
                    new String[]{"getBuilding", "getBuildingAt", "getBuildingForPosition"},
                    new Object[]{hutPos}
            );
            if (bAt != null) {
                Integer lvl = getBuildingLevel(bAt);
                if (lvl != null) return lvl;
            }

            Object th = invokeFirstCompatible(
                    bm,
                    new String[]{"getTownHall", "getTownHallBuilding", "getTownHallView"},
                    new Object[]{}
            );
            if (th != null) {
                Integer lvl = getBuildingLevel(th);
                if (lvl != null) return lvl;
            }
        } catch (Throwable t) {
            ModMain.LOGGER.debug("[RoyalCrown] Colony reflect failed: {}", t.toString());
        }
        return 0;
    }

    private static Object getColonyManagerInstance() {
        String[] candidates = {
                "com.minecolonies.api.colony.management.ColonyManager",
                "com.minecolonies.api.colony.ColonyManager"
        };
        for (String cn : candidates) {
            try {
                Class<?> clazz = Class.forName(cn);
                Method m = clazz.getMethod("getInstance");
                Object inst = m.invoke(null);
                if (inst != null) return inst;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object invokeBestColonyLookup(Object colonyManager, ServerLevel sl, BlockPos pos) {
        String[] names = {"getColonyByPos", "getClosestColony", "getColonyByPosition", "getNearestColony", "getColony"};
        Object[][] argSets = new Object[][]{
                {sl, pos},
                {pos, sl},
                {pos},
                {sl}
        };
        for (String n : names) {
            for (Object[] args : argSets) {
                Object out = tryInvoke(colonyManager, n, args);
                if (out != null) return out;
            }
        }
        return null;
    }

    private static Object invokeFirstCompatible(Object target, String[] methodNames, Object[] args) {
        for (String name : methodNames) {
            Object out = tryInvoke(target, name, args);
            if (out != null) return out;
        }
        return null;
    }

    private static Object tryInvoke(Object target, String name, Object[] args) {
        Method[] methods = target.getClass().getMethods();
        for (Method m : methods) {
            if (!m.getName().equals(name)) continue;
            if (m.getParameterCount() != args.length) continue;
            Class<?>[] pts = m.getParameterTypes();
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

    private static Integer getBuildingLevel(Object building) {
        for (String name : Arrays.asList("getBuildingLevel", "getLevel", "getCurrentLevel", "getUpgradeLevel")) {
            try {
                Method m = building.getClass().getMethod(name);
                Object val = m.invoke(building);
                if (val instanceof Number n) return n.intValue();
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
