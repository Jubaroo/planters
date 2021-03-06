package net.bdew.planters.gen;

import com.google.common.io.LittleEndianDataInputStream;

import java.io.*;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public class GenProps {
    private static String readString(LittleEndianDataInputStream in) throws IOException {
        int len = in.readInt();
        byte[] b = new byte[len];
        for (int i = 0; i < len; i++)
            b[i] = in.readByte();
        return new String(b, "UTF-8");
    }

    public static void main(String[] args) {
        File dir = new File("pack");
        try (PrintStream out = new PrintStream("pack/properties.xml")) {
            out.println("<properties>");
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                if (!file.canRead() || !file.getName().endsWith(".wom")) continue;
                int totalFaces = 0;
                int totalVerts = 0;
                int totalMats = 0;
                StringBuilder modelPropsSB = new StringBuilder();
                StringBuilder generalSB = new StringBuilder();
                try (LittleEndianDataInputStream in = new LittleEndianDataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
                    int meshes = in.readInt();
                    for (int m = 0; m < meshes; m++) {
                        boolean hasTangents = in.read() != 0;
                        boolean hasBinormal = in.read() != 0;
                        boolean hasVertexColor = in.read() != 0;
                        String name = readString(in);
                        int verts = in.readInt();
                        for (int v = 0; v < verts; v++) {
                            in.readFloat(); // x
                            in.readFloat(); // y
                            in.readFloat(); // z
                            in.readFloat(); // nx
                            in.readFloat(); // ny
                            in.readFloat(); // nz
                            in.readFloat(); // u
                            in.readFloat(); // v
                            if (hasVertexColor) {
                                in.readFloat(); // r
                                in.readFloat(); // g
                                in.readFloat(); // b
                            }
                            if (hasTangents) {
                                in.readFloat(); // x
                                in.readFloat(); // y
                                in.readFloat(); // z
                            }
                            if (hasBinormal) {
                                in.readFloat(); // x
                                in.readFloat(); // y
                                in.readFloat(); // z
                            }
                        }

                        int faces = in.readInt();
                        for (int v = 0; v < faces; v++)
                            in.readShort();

                        int mats = in.readInt();
                        boolean hadPlank = false;
                        boolean hadCloth = false;
                        boolean hadPlanter = false;
                        boolean hadLegs = false;
                        boolean hadStone = false;

                        totalFaces += faces;
                        totalVerts += verts;
                        totalMats += mats;
//                        System.out.println(String.format(" - %s (%d verts / %d faces / %d mats)", name, verts, faces, mats));

                        for (int mi = 0; mi < mats; mi++) {
                            String tex = readString(in);
                            String matName = readString(in);

                            if (tex.contains("Plank_oak")) hadPlank = true;
                            if (tex.contains("SmallStone")) hadPlank = true;
                            if (tex.contains("wicker")) hadPlank = true;
                            if (tex.contains("cloth")) hadCloth = true;
                            if (tex.contains("frame") || tex.contains("bush-wood")) hadPlanter = true;
                            if (tex.contains("bush-metal")) hadLegs = true;
                            if (tex.contains("cave_slate")) hadStone = true;

                            in.readByte(); // enabled

                            in.readByte(); // property exists
                            in.readFloat(); // emissive R
                            in.readFloat(); // emissive G
                            in.readFloat(); // emissive B
                            in.readFloat(); // emissive A

                            in.readByte(); // property exists
                            in.readFloat(); // shiny

                            in.readByte(); // property exists
                            in.readFloat(); // specular R
                            in.readFloat(); // specular G
                            in.readFloat(); // specular B
                            in.readFloat(); // specular A

                            in.readByte(); // property exists
                            in.readFloat(); // transparency R
                            in.readFloat(); // transparency G
                            in.readFloat(); // transparency B
                            in.readFloat(); // transparency A

//                            System.out.println(String.format("    - %s -> %s", matName, tex));
                        }
                        if (file.getName().contains("planter-tree")) {
                            if (hadPlank)
                                modelPropsSB.append(String.format("<meshMask><mesh>%s</mesh><mask>secondary_pm.png</mask></meshMask>", name));
                            else if (hadStone)
                                modelPropsSB.append(String.format("<meshMask><mesh>%s</mesh><mask>planter_pm.png</mask></meshMask>", name));
                        } else if (file.getName().contains("planter-bush")) {
                            if (hadPlanter)
                                modelPropsSB.append(String.format("<meshMask><mesh>%s</mesh><mask>bush_pm.png</mask></meshMask>", name));
                            else if (hadLegs)
                                modelPropsSB.append(String.format("<meshMask><mesh>%s</mesh><mask>secondary_pm.png</mask></meshMask>", name));
                        } else {
                            if (hadPlank) {
                                modelPropsSB.append(String.format("<meshMask><mesh>%s</mesh><mask>planter_pm.png</mask></meshMask>", name));
                            }
                            if (hadCloth) {
                                modelPropsSB.append(String.format("<meshMask><mesh>%s</mesh><mask>cloth_pm.png</mask></meshMask>", name));
                            }
                        }
                    }
                    System.out.println(String.format("%s: %d meshes / %d vertices / %d faces / %d materials", file.getName(), meshes, totalVerts, totalFaces / 3, totalMats));
                }

                if ((file.getName().contains("shroom-magic") && !file.getName().contains("wilted")) || file.getName().contains("basket-magic") || file.getName().contains("shroom-item")) {
                    generalSB.append("<customEffect><reindeer><nullName>sparkle</nullName></reindeer></customEffect>");

                }

                if (modelPropsSB.length() > 0) {
                    out.println(String.format("<%s><modelProperties>%s</modelProperties>%s</%s>", file.getName(), modelPropsSB.toString(), generalSB.toString(), file.getName()));
                } else if (generalSB.length() > 0) {
                    out.println(String.format("<%s>%s</%s>", file.getName(), generalSB.toString(), file.getName()));
                }
            }
            out.println("</properties>");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

