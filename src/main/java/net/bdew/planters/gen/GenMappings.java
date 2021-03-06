package net.bdew.planters.gen;

import net.bdew.planters.Plantable;
import net.bdew.planters.PlanterItem;
import net.bdew.planters.PlanterType;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GenMappings {
    static class Mapping {
        public final String key, val;

        public Mapping(String key, String val) {
            this.key = key;
            this.val = val;
        }
    }

    static class ResURL {
        public final String file;
        public final Map<String, String> overrides;

        public ResURL(String file, Map<String, String> overrides) {
            this.file = file;
            this.overrides = overrides;
        }

        public ResURL(String file) {
            this(file, new HashMap<>());
        }

        public ResURL override(String key, String val) {
            Map<String, String> mc = new HashMap<>(overrides);
            mc.put(key, val);
            return new ResURL(file, mc);
        }

        public ResURL tex(String matName, String texName) {
            return override(matName + ".texture", texName);
        }

        public ResURL val(String key, String val) {
            return override(key, val);
        }

        public String build() {
            if (overrides.isEmpty()) return file;
            return file + "?" +
                    overrides.entrySet().stream()
                            .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
                            .collect(Collectors.joining("&"));
        }
    }

    private static ResURL model(String fn) {
        return new ResURL(fn);
    }

    private static HashMap<Plantable, String> texNames = new HashMap<>();

    static {
        texNames.put(Plantable.Carrots, "carrot");
        texNames.put(Plantable.Tomatoes, "tomato");
        texNames.put(Plantable.Peapods, "peas");
        texNames.put(Plantable.Strawberries, "strawberry");
    }

    private static ArrayList<Mapping> mappings = new ArrayList<>();
    private static int longest = 0;
    private static PrintStream output = System.out;
    private static int totalMappings = 0;

    private static void addMapping(String key, ResURL val) {
        if (key.endsWith(".")) key = key.substring(0, key.length() - 1);
        mappings.add(new Mapping(key, val.build()));
        if (key.length() > longest) longest = key.length();
        totalMappings++;
    }

    private static void emitSection(String title) {
        output.println(String.format("########## %s #########", title));
        output.println();
        mappings.forEach(m -> output.println(String.format("%-" + longest + "s = %s", m.key, m.val)));
        output.println();
        longest = 0;
        mappings.clear();
    }

    private static void generateVariants(String key, String suffix, ResURL base, String matName, String decayMatTex, String dirtName, String winterTex, String decaySoilTex, String extraDecayMat, String extraDecayTex) {
        ResURL decay = base.tex(matName, decayMatTex);
        if (extraDecayMat != null && extraDecayTex != null)
            decay = decay.tex(extraDecayMat, extraDecayTex);
        addMapping(key + suffix, base);
        addMapping(key + ".decayed" + suffix, decaySoilTex == null ? decay : decay.tex(dirtName, decaySoilTex));
        addMapping(key + ".winter" + suffix, base.tex(dirtName, winterTex));
        addMapping(key + ".decayed.winter" + suffix, decay.tex(dirtName, winterTex));
        addMapping(key + ".infected" + suffix, decay.tex(dirtName, "mycelium.dds"));
        addMapping(key + ".infected.winter" + suffix, decay.tex(dirtName, "mycelium-winter.dds"));
    }

    private static void generateVariantsSprite(String key, String suffix, ResURL base, String matName, String decayMatTex, String dirtName, String spriteTex, String spriteFile) {
        ResURL spr = base.tex(spriteTex, spriteFile);
        addMapping(key + suffix, spr);
        addMapping(key + ".decayed" + suffix, spr.tex(matName, decayMatTex).tex(dirtName, "farmland.jpg"));
        addMapping(key + ".winter" + suffix, spr.tex(dirtName, "farm_winter.jpg"));
        addMapping(key + ".decayed.winter" + suffix, spr.tex(matName, decayMatTex).tex(dirtName, "farm_winter.jpg"));
        addMapping(key + ".infected" + suffix, spr.tex(matName, decayMatTex).tex(dirtName, "mycelium.dds"));
        addMapping(key + ".infected.winter" + suffix, spr.tex(matName, decayMatTex).tex(dirtName, "mycelium-winter.dds"));
    }

    private static void generateStages(String baseModel, Plantable plant, String base, String soilMat, String winterSoilTex, String decaySoilTex, String extraDecayMat, String extraDecayTex) {
        generateVariants(baseModel + plant.modelName + "young", "", model(String.format("%s-wood-tended.wom", base)), "oakplank", "woodbridge_decay.png", soilMat, winterSoilTex, decaySoilTex, extraDecayMat, extraDecayTex);
        generateVariants(baseModel + plant.modelName + "young.untended", "", model(String.format("%s-wood-young.wom", base)), "oakplank", "woodbridge_decay.png", soilMat, winterSoilTex, decaySoilTex, extraDecayMat, extraDecayTex);
        generateVariants(baseModel + plant.modelName + "ripe", "", model(String.format("%s-wood-ripe.wom", base)), "oakplank", "woodbridge_decay.png", soilMat, winterSoilTex, decaySoilTex, extraDecayMat, extraDecayTex);
        generateVariants(baseModel + plant.modelName + "ripe.wilted", "", model(String.format("%s-wood-wilted.wom", base)), "oakplank", "woodbridge_decay.png", soilMat, winterSoilTex, decaySoilTex, extraDecayMat, extraDecayTex);

        generateVariants(baseModel + plant.modelName + "young", ".stone", model(String.format("%s-stone-tended.wom", base)), "stone", "SmallStoneDmg.jpg", soilMat, winterSoilTex, decaySoilTex, extraDecayMat, extraDecayTex);
        generateVariants(baseModel + plant.modelName + "young.untended", ".stone", model(String.format("%s-stone-young.wom", base)), "stone", "SmallStoneDmg.jpg", soilMat, winterSoilTex, decaySoilTex, extraDecayMat, extraDecayTex);
        generateVariants(baseModel + plant.modelName + "ripe", ".stone", model(String.format("%s-stone-ripe.wom", base)), "stone", "SmallStoneDmg.jpg", soilMat, winterSoilTex, decaySoilTex, extraDecayMat, extraDecayTex);
        generateVariants(baseModel + plant.modelName + "ripe.wilted", ".stone", model(String.format("%s-stone-wilted.wom", base)), "stone", "SmallStoneDmg.jpg", soilMat, winterSoilTex, decaySoilTex, extraDecayMat, extraDecayTex);
    }

    private static void generateStagesSprite(Plantable plant, String base) {
        generateVariantsSprite(PlanterItem.BASEMODEL + plant.modelName + "young", "", model(String.format("%s-wood-tended.wom", base)), "oakplank", "woodbridge_decay.png", "farmwurm", "sprite_wheat", fixedTexName(plant));
        generateVariantsSprite(PlanterItem.BASEMODEL + plant.modelName + "young.untended", "", model(String.format("%s-wood-young.wom", base)), "oakplank", "woodbridge_decay.png", "farmwurm", "sprite_wheat", fixedTexName(plant));
        generateVariantsSprite(PlanterItem.BASEMODEL + plant.modelName + "ripe", "", model(String.format("%s-wood-ripe.wom", base)), "oakplank", "woodbridge_decay.png", "farmwurm", "sprite_wheat", fixedTexName(plant));
        generateVariantsSprite(PlanterItem.BASEMODEL + plant.modelName + "ripe.wilted", "", model(String.format("%s-wood-wilted.wom", base)), "oakplank", "woodbridge_decay.png", "farmwurm", "sprite_wheat", fixedTexName(plant));

        generateVariantsSprite(PlanterItem.BASEMODEL + plant.modelName + "young", ".stone", model(String.format("%s-stone-tended.wom", base)), "stone", "SmallStoneDmg.jpg", "farmwurm", "sprite_wheat", fixedTexName(plant));
        generateVariantsSprite(PlanterItem.BASEMODEL + plant.modelName + "young.untended", ".stone", model(String.format("%s-stone-young.wom", base)), "stone", "SmallStoneDmg.jpg", "farmwurm", "sprite_wheat", fixedTexName(plant));
        generateVariantsSprite(PlanterItem.BASEMODEL + plant.modelName + "ripe", ".stone", model(String.format("%s-stone-ripe.wom", base)), "stone", "SmallStoneDmg.jpg", "farmwurm", "sprite_wheat", fixedTexName(plant));
        generateVariantsSprite(PlanterItem.BASEMODEL + plant.modelName + "ripe.wilted", ".stone", model(String.format("%s-stone-wilted.wom", base)), "stone", "SmallStoneDmg.jpg", "farmwurm", "sprite_wheat", fixedTexName(plant));
    }

    private static String fixedTexName(Plantable plant) {
        if (texNames.containsKey(plant)) return "crops/" + texNames.get(plant) + ".dds";
        return "crops/" + plant.modelName + "dds";
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            try {
                output = new PrintStream(new FileOutputStream(args[0]));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            addMapping(PlanterItem.BASEMODEL, model("planter-wood.wom"));
            addMapping(PlanterItem.BASEMODEL + "unfinished", model("unfinished-wood.wom"));
            addMapping(PlanterItem.BASEMODEL + "unfinished.stone", model("unfinished-stone.wom"));
            generateVariants(PlanterItem.BASEMODEL + "dirt", "", model("planter-wood.wom"), "oakplank", "woodbridge_decay.png", "dirtwurm", "dirt_winter.jpg", null, null, null);
            generateVariants(PlanterItem.BASEMODEL + "dirt", ".stone", model("planter-stone.wom"), "stone", "SmallStoneDmg.jpg", "dirtwurm", "dirt_winter.jpg", null, null, null);
            emitSection("Base");

            addMapping(PlanterItem.BASEMODEL + "magic", model("magic-wood.wom"));
            addMapping(PlanterItem.BASEMODEL + "magic.unfinished", model("magic-wood-unfinished.wom"));
            addMapping(PlanterItem.BASEMODEL + "magic.unfinished.stone", model("magic-stone-unfinished.wom"));
            generateVariants(PlanterItem.BASEMODEL + "magic.dirt", "", model("magic-wood.wom"), "oakplank", "woodbridge_decay.png", "moss", "moss_winter.png", "moss.jpg", "treeMat", "oaktex Old.png");
            generateVariants(PlanterItem.BASEMODEL + "magic.dirt", ".stone", model("magic-stone.wom"), "stone", "SmallStoneDmg.jpg", "moss", "moss_winter.png", "moss.jpg", "treeMat", "oaktex Old.png");
            emitSection("Magic");

            generateVariants(PlanterItem.BASEMODEL + "tree.wood", "", model("planter-tree.wom").tex("tree_planter", "~graphics.jar/texture/terrain/cave_wood.dds"), "wood", "plank-oak decay.dds", "dirt", "dirt_winter.jpg", null, "tree_planter", "~graphics.jar/structures/Houses/floorWood_decay.dds");
            generateVariants(PlanterItem.BASEMODEL + "tree.pottery", "", model("planter-tree.wom").tex("tree_planter", "~graphics.jar/texture/terrain/cave_brick.dds"), "wood", "plank-oak decay.dds", "dirt", "dirt_winter.jpg", null, "tree_planter", "~graphics.jar/structures/Houses/brick_decay.dds");
            generateVariants(PlanterItem.BASEMODEL + "tree.marble", "", model("planter-tree.wom").tex("tree_planter", "~graphics.jar/texture/terrain/cave_marble.dds"), "wood", "plank-oak decay.dds", "dirt", "dirt_winter.jpg", null, "tree_planter", "~graphics.jar/structures/Houses/marble_decay.dds");
            generateVariants(PlanterItem.BASEMODEL + "tree.clay", "", model("planter-tree.wom").tex("tree_planter", "~graphics.jar/texture/terrain/cave_rendered.dds"), "wood", "plank-oak decay.dds", "dirt", "dirt_winter.jpg", null, "tree_planter", "~graphics.jar/structures/Houses/plaster_decay.dds");
            generateVariants(PlanterItem.BASEMODEL + "tree.stone", "", model("planter-tree.wom").tex("tree_planter", "~graphics.jar/texture/terrain/cave_rounded.dds"), "wood", "plank-oak decay.dds", "dirt", "dirt_winter.jpg", null, "tree_planter", "~graphics.jar/structures/Houses/rounded_decay.dds");
            generateVariants(PlanterItem.BASEMODEL + "tree.sand", "", model("planter-tree.wom").tex("tree_planter", "~graphics.jar/texture/terrain/cave_sandstone.dds"), "wood", "plank-oak decay.dds", "dirt", "dirt_winter.jpg", null, "tree_planter", "~graphics.jar/structures/Houses/sandstone_decay.dds");
            generateVariants(PlanterItem.BASEMODEL + "tree.slate", "", model("planter-tree.wom").tex("tree_planter", "~graphics.jar/texture/terrain/cave_slate.dds"), "wood", "plank-oak decay.dds", "dirt", "dirt_winter.jpg", null, "tree_planter", "~graphics.jar/structures/Houses/slate_decay.dds");
            generateVariants(PlanterItem.BASEMODEL + "tree.brick", "", model("planter-tree.wom").tex("tree_planter", "~graphics.jar/texture/terrain/cave_stonehouse.dds"), "wood", "plank-oak decay.dds", "dirt", "dirt_winter.jpg", null, "tree_planter", "~graphics.jar/structures/Houses/StoneWallDmg.dds");
            emitSection("Trees");

            addMapping(PlanterItem.BASEMODEL + "tree.wood.unfinished", model("planter-tree-unfinished.wom").tex("tree_planter", "~graphics.jar/texture/terrain/cave_wood.dds"));
            addMapping(PlanterItem.BASEMODEL + "tree.pottery.unfinished", model("planter-tree-unfinished.wom").tex("tree_planter", "~graphics.jar/texture/terrain/cave_brick.dds"));
            addMapping(PlanterItem.BASEMODEL + "tree.marble.unfinished", model("planter-tree-unfinished.wom").tex("tree_planter", "~graphics.jar/texture/terrain/cave_marble.dds"));
            addMapping(PlanterItem.BASEMODEL + "tree.clay.unfinished", model("planter-tree-unfinished.wom").tex("tree_planter", "~graphics.jar/texture/terrain/cave_rendered.dds"));
            addMapping(PlanterItem.BASEMODEL + "tree.stone.unfinished", model("planter-tree-unfinished.wom").tex("tree_planter", "~graphics.jar/texture/terrain/cave_rounded.dds"));
            addMapping(PlanterItem.BASEMODEL + "tree.sand.unfinished", model("planter-tree-unfinished.wom").tex("tree_planter", "~graphics.jar/texture/terrain/cave_sandstone.dds"));
            addMapping(PlanterItem.BASEMODEL + "tree.slate.unfinished", model("planter-tree-unfinished.wom").tex("tree_planter", "~graphics.jar/texture/terrain/cave_slate.dds"));
            addMapping(PlanterItem.BASEMODEL + "tree.brick.unfinished", model("planter-tree-unfinished.wom").tex("tree_planter", "~graphics.jar/texture/terrain/cave_stonehouse.dds"));
            emitSection("Trees Unfinished");

            generateVariants(PlanterItem.BASEMODEL + "bush.wood", "", model("planter-bush.wom").tex("planter", "bush-wood.dds"), "planter", "bush-wood-decay.dds", "dirt", "dirt_winter.jpg", null, null, null);
            generateVariants(PlanterItem.BASEMODEL + "bush.metal", "", model("planter-bush.wom").tex("planter", "bush-metal.dds"), "planter", "bush-metal-decay.dds", "dirt", "dirt_winter.jpg", null, null, null);
            addMapping(PlanterItem.BASEMODEL + "bush.wood.unfinished", model("planter-bush-unfinished.wom").tex("frame", "bush-metal.dds"));
            addMapping(PlanterItem.BASEMODEL + "bush.metal.unfinished", model("planter-bush-unfinished.wom").tex("frame", "bush-metal.dds"));
            emitSection("Bushes");


            addMapping("model.food.mushroom.magic", model("shroom-item.wom"));
            addMapping("model.pile.magicmushroom", model("basket-magic.wom"));
            addMapping("model.container.basket", model("basket-empty.wom"));
            addMapping("model.container.basket.fabric", model("basket-fabric.wom"));
            addMapping("model.container.basket.mushroom", model("basket-mixed.wom"));
            addMapping("model.container.basket.mushroom.magic", model("basket-magic.wom"));
            emitSection("Items");

            for (Plantable plant : Plantable.values()) {
                if (plant.modelName.contains("mushroom")) {
                    String color = plant.modelName.split("\\.")[1];
                    generateStages(PlanterItem.BASEMODEL, plant, "shroom-" + color, "Soil", "dirt_winter.jpg", null, null, null);
                } else if (plant.planterType == PlanterType.MAGIC) {
                    generateStages(PlanterItem.BASEMODEL + "magic.", plant, "shroom-magic", "moss", "moss_winter.png", "moss.jpg", "treeMat", "oaktex Old.png");
                } else if (plant == Plantable.Cabbage) {
                    generateStages(PlanterItem.BASEMODEL, plant, "cabbage", "farmwurm", "farm_winter.jpg", "farmland.jpg", null, null);
                } else if (plant == Plantable.Pumpkin) {
                    generateStages(PlanterItem.BASEMODEL, plant, "pumpkin", "farmwurm", "farm_winter.jpg", "farmland.jpg", null, null);
                } else if (plant.planterType != PlanterType.TREE && plant.planterType != PlanterType.BUSH) {
                    // Sprite crops
                    if (plant.water) {
                        generateStagesSprite(plant, "water");
                    } else {
                        generateStagesSprite(plant, "sprite");
                    }
                } else continue;
                emitSection(plant.displayName);
            }
        } finally {
            if (args.length > 0) {
                output.close();
            }
        }
        System.out.println(String.format("Generated %d mappings", totalMappings));
    }
}