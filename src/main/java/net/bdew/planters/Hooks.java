package net.bdew.planters;

import com.wurmonline.server.Server;
import com.wurmonline.server.Servers;
import com.wurmonline.server.WurmCalendar;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.sounds.SoundPlayer;

import java.util.List;

public class Hooks {
    private static long lastPolledPlanters = System.currentTimeMillis();

    public static void addItemLoading(Item item) {
        if (PlanterItem.isPlanter(item) && PlanterItem.needsPolling(item)) {
            PlanterTracker.addPlanter(item);
        }
    }

    public static void pollPlanters() {
        long start = System.currentTimeMillis();

        if (start - lastPolledPlanters < Servers.localServer.getFieldGrowthTime())
            return;

        List<Item> planters = PlanterTracker.getPlanters();
        if (planters.isEmpty()) return;

        for (Item planter : planters) {
            Plantable crop = PlanterItem.getPlantable(planter);
            if (crop == null) {
                PlanterTracker.removePlanter(planter);
                continue;
            }
            int stage = PlanterItem.getGrowthStage(planter);
            int tendPower = PlanterItem.getTendPower(planter);
            int tendCount = PlanterItem.getTendCount(planter);

            if (stage == 5 && PlantersMod.canWilt && Server.rand.nextFloat() < 0.5f) stage = 6;
            if (stage < 5) stage++;

            if (crop.planterType == PlanterType.MAGIC && !PlanterItem.isTended(planter)) {
                if (Server.rand.nextFloat() < PlantersMod.magicUntendedDeathChance) {
                    stage = 6;
                }
            }

            PlanterItem.updateData(planter, crop, stage, stage >= 5, tendCount, tendPower);

            if (!PlanterItem.needsPolling(planter)) PlanterTracker.removePlanter(planter);

            if (WurmCalendar.isNight())
                SoundPlayer.playSound("sound.ambient.night.crickets", planter.getTileX(), planter.getTileY(), planter.isOnSurface(), 0f);
            else
                SoundPlayer.playSound("sound.birdsong.bird2", planter.getTileX(), planter.getTileY(), planter.isOnSurface(), 0f);
        }

        PlantersMod.logInfo(String.format("Polled %d planters, took %dms, remaining %d", planters.size(), System.currentTimeMillis() - start, PlanterTracker.trackedCount()));
        lastPolledPlanters = start;
    }

    public static void sendItemHook(Communicator comm, Item item) {
        if (PlanterItem.getPlanterType(item.getTemplateId()) == PlanterType.MAGIC) {
            if (item.getAuxData() != 0 && PlanterItem.getGrowthStage(item) < 6) {
                comm.sendRemoveEffect(item.getWurmId());
                comm.sendAttachEffect(item.getWurmId(), (byte) 0, (byte) 1, (byte) 255, (byte) 255, (byte) 255);
                comm.sendAddEffect(item.getWurmId(), item.getWurmId(), (short) 27, item.getPosX(), item.getPosY(), item.getPosZ(), (byte) 0, "reindeer", Float.MAX_VALUE, 0f);
            }
        }
    }

    public static void removeItemHook(Communicator comm, Item item) {
        if (PlanterItem.getPlanterType(item.getTemplateId()) == PlanterType.MAGIC) {
            comm.sendRemoveEffect(item.getWurmId());
        }
    }
}
