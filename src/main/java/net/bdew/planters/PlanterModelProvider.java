package net.bdew.planters;

import com.wurmonline.server.WurmCalendar;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.structures.Structure;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import com.wurmonline.shared.util.MaterialUtilities;
import org.gotti.wurmunlimited.modsupport.items.ModelNameProvider;

public class PlanterModelProvider implements ModelNameProvider {
    public final String baseModel;

    public PlanterModelProvider(String baseModel) {
        this.baseModel = baseModel;
    }

    private static boolean isWinter() {
        return GmCommands.forceWinter.orElse(WurmCalendar.isWinter());
    }

    @Override
    public String getModelName(Item item) {
        StringBuilder sb = new StringBuilder(baseModel);
        Plantable plant = PlanterItem.getPlantable(item);

        if (plant != null && plant.planterType != PlanterType.TREE && plant.planterType != PlanterType.BUSH) {
            sb.append(plant.modelName);

            int growth = PlanterItem.getGrowthStage(item);
            if (growth < 5) {
                sb.append("young.");
                if (!PlanterItem.isTended(item))
                    sb.append("untended.");
            } else {
                sb.append("ripe.");
                if (growth > 5) {
                    sb.append("wilted.");
                }
            }
        } else sb.append("dirt.");

        if (PlanterItem.isInfected(item))
            sb.append("infected.");
        else if (item.getDamage() >= 50f)
            sb.append("decayed.");

        if (isWinter() && item.isOnSurface()) {
            VolaTile vt = Zones.getTileOrNull(item.getTilePos(), true);
            if (vt != null) {
                Structure st = vt.getStructure();
                if (st == null || st.isTypeBridge())
                    sb.append("winter.");
            }
        }

        sb.append(MaterialUtilities.getMaterialString(item.getMaterial()));

        return sb.toString();

    }
}
