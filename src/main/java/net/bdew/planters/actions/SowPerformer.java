package net.bdew.planters.actions;

import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.skills.SkillList;
import com.wurmonline.server.villages.VillageRole;
import net.bdew.planters.*;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.ActionPropagation;

public class SowPerformer implements ActionPerformer {
    @Override
    public short getActionId() {
        return Actions.SOW;
    }

    public static boolean canUse(Creature performer, Item source, Item target) {
        if (performer.isPlayer() && PlanterItem.isPlanter(target) && target.getAuxData() == 0 && target.getParentId() == -10L && !PlanterItem.isTreeOrBushPlanter(target)) {
            if (Plantable.findSeed(source.getTemplateId(), PlanterItem.getPlanterType(target.getTemplateId())) == null)
                return false;
            return Utils.checkRoleAllows(performer, target, VillageRole::maySowFields);
        } else return false;
    }

    public static boolean actionStart(Creature performer, Item source, Item target) {
        Plantable crop = Plantable.findSeed(source.getTemplateId(), PlanterItem.getPlanterType(target.getTemplateId()));
        if (crop == null) return false;
        performer.playAnimation("drop", false);
        if (crop.planterType == PlanterType.MAGIC) {
            performer.getCommunicator().sendNormalServerMessage("You start throwing source salt into the planter.");
        } else {
            performer.getCommunicator().sendNormalServerMessage("You start sowing the seeds.");
            Server.getInstance().broadCastAction(performer.getName() + " starts sowing some seeds.", performer, 5);
        }
        return true;
    }

    public static boolean actionEnd(Creature performer, Item source, Item target, byte rarity) {
        if (rarity != 0) performer.playPersonalSound("sound.fx.drumroll");
        Plantable crop = Plantable.findSeed(source.getTemplateId(), PlanterItem.getPlanterType(target.getTemplateId()));
        if (crop == null) return false;
        performer.getStatus().modifyStamina(-2000f);
        Skill farming = performer.getSkills().getSkillOrLearn(SkillList.FARMING);
        farming.skillCheck(crop.difficulty, 0.0, false, 1f);
        int sowPower = (int) (100.0 - farming.getKnowledge() + source.getQualityLevel() + source.getRarity() * 20 + rarity * 50);
        PlanterItem.updateData(target, crop, 0, true, 0, sowPower);
        source.setWeight(source.getWeightGrams() - source.getTemplate().getWeightGrams(), true);
        if (crop.planterType == PlanterType.MAGIC) {
            performer.getCommunicator().sendNormalServerMessage("Mushrooms should pop any minute now!");
            Server.getInstance().broadCastAction(performer.getName() + " drops some powder on a planter.", performer, 5);
        } else {
            performer.getCommunicator().sendNormalServerMessage("You sow the " + crop.displayName + ".");
            Server.getInstance().broadCastAction(performer.getName() + " sows some seeds.", performer, 5);
        }
        PlanterTracker.addPlanter(target);
        return true;
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Item target, short num, float counter) {
        if (!canUse(performer, source, target))
            if (PlanterItem.isPlanter(target))
                return propagate(action, ActionPropagation.FINISH_ACTION, ActionPropagation.NO_SERVER_PROPAGATION, ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION);
            else
                return propagate(action, ActionPropagation.SERVER_PROPAGATION, ActionPropagation.ACTION_PERFORMER_PROPAGATION);

        if (counter == 1f) {
            if (!actionStart(performer, source, target))
                return propagate(action, ActionPropagation.FINISH_ACTION, ActionPropagation.NO_SERVER_PROPAGATION, ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION);
            int time = Actions.getQuickActionTime(performer, performer.getSkills().getSkillOrLearn(SkillList.FARMING), null, 0.0);
            performer.sendActionControl("sowing", true, time);
            action.setTimeLeft(time);
        } else {
            if (counter * 10f > action.getTimeLeft()) {
                actionEnd(performer, source, target, action.getRarity());
                return propagate(action, ActionPropagation.FINISH_ACTION, ActionPropagation.NO_SERVER_PROPAGATION, ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION);
            }
        }

        return propagate(action, ActionPropagation.CONTINUE_ACTION, ActionPropagation.NO_SERVER_PROPAGATION, ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION);
    }
}
