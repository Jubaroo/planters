package net.bdew.planters.actions;

import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.skills.SkillList;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.villages.VillageRole;
import com.wurmonline.server.villages.Villages;
import net.bdew.planters.Plantable;
import net.bdew.planters.PlanterItem;
import net.bdew.planters.PlanterTracker;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.ActionPropagation;

public class SowPerformer implements ActionPerformer {
    @Override
    public short getActionId() {
        return Actions.SOW;
    }

    public static boolean canUse(Creature performer, Item source, Item target) {
        if (performer.isPlayer() && PlanterItem.isPlanter(target) && target.getAuxData() == 0 && target.getParentId() == -10L) {
            if (Plantable.findSeed(source.getTemplateId()) == null) return false;
            Village village = Villages.getVillage(target.getTileX(), target.getTileY(), target.isOnSurface());
            if (village == null) return true;
            VillageRole role = village.getRoleFor(performer);
            return role != null && role.mayFarm();
        } else return false;
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Item target, short num, float counter) {
        if (!canUse(performer, source, target))
            if (PlanterItem.isPlanter(target))
                return propagate(action, ActionPropagation.FINISH_ACTION, ActionPropagation.NO_SERVER_PROPAGATION, ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION);
            else
                return propagate(action, ActionPropagation.SERVER_PROPAGATION, ActionPropagation.ACTION_PERFORMER_PROPAGATION);

        Plantable crop = Plantable.findSeed(source.getTemplateId());
        if (crop == null)
            return propagate(action, ActionPropagation.FINISH_ACTION, ActionPropagation.NO_SERVER_PROPAGATION, ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION);

        if (counter == 1f) {
            performer.getCommunicator().sendNormalServerMessage("You start sowing the seeds.");
            Server.getInstance().broadCastAction(performer.getName() + " starts sowing some seeds.", performer, 5);
            int time = Actions.getQuickActionTime(performer, performer.getSkills().getSkillOrLearn(SkillList.FARMING), null, 0.0);
            performer.sendActionControl("sowing", true, time);
            action.setTimeLeft(time);
        } else {
            if (counter * 10f > action.getTimeLeft()) {
                performer.getStatus().modifyStamina(-2000f);
                Skill farming = performer.getSkills().getSkillOrLearn(SkillList.FARMING);
                farming.skillCheck(crop.difficulty, 0.0, false, 1f);
                PlanterItem.updateData(target, crop, 0, true, 0, 0);
                target.setData2((int) (100.0 - farming.getKnowledge() + source.getQualityLevel() + source.getRarity() * 20 + action.getRarity() * 50));
                performer.getCommunicator().sendNormalServerMessage("You sow the " + crop.displayName + ".");
                source.setWeight(source.getWeightGrams() - source.getTemplate().getWeightGrams(), true);
                Server.getInstance().broadCastAction(performer.getName() + " sows some seeds.", performer, 5);
                PlanterTracker.addPlanter(target);
                return propagate(action, ActionPropagation.FINISH_ACTION, ActionPropagation.NO_SERVER_PROPAGATION, ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION);
            }
        }

        return propagate(action, ActionPropagation.CONTINUE_ACTION, ActionPropagation.NO_SERVER_PROPAGATION, ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION);
    }
}
