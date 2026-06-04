package com.flansmod.client.tmt;

import net.minecraft.world.phys.Vec3;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ModelPoolObjEntry extends ModelPoolEntry
{
    public ModelPoolObjEntry()
    {
        fileExtensions = new String[]{"obj"};
    }

    @Override
    public void getModel(File file)
    {
        try(BufferedReader in = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8))
        {
            String s;

            List<PositionTransformVertex> verts = new ArrayList<>();
            List<float[]> uvs = new ArrayList<>();
            List<float[]> normals = new ArrayList<>();
            List<TexturedPolygon> face = new ArrayList<>();

            while((s = in.readLine()) != null)
            {
                if(s.contains("#"))
                {
                    s = s.substring(0, s.indexOf("#"));
                }

                s = s.trim();

                if(s.isEmpty())
                    continue;

                if(s.startsWith("g "))
                {
                    setTextureGroup(s.substring(s.indexOf(" ") + 1).trim());
                    continue;
                }
                if(s.startsWith("v "))
                {
                    s = s.substring(s.indexOf(" ") + 1).trim();
                    float[] v = new float[3];
                    for(int i = 0; i < 3; i++)
                    {
                        int ind = s.indexOf(" ");
                        if(ind > -1)
                            v[i] = Float.parseFloat(s.substring(0, ind));
                        else
                            v[i] = Float.parseFloat(s);
                        s = s.substring(s.indexOf(" ") + 1).trim();
                    }

                    float flt = v[2];
                    v[2] = -v[1];
                    v[1] = flt;

                    verts.add(new PositionTransformVertex(v[0], v[1], v[2], 0, 0));
                    continue;
                }
                if(s.startsWith("vt "))
                {
                    s = s.substring(s.indexOf(" ") + 1).trim();
                    float[] v = new float[2];
                    for(int i = 0; i < 2; i++)
                    {
                        int ind = s.indexOf(" ");
                        if(ind > -1)
                            v[i] = Float.parseFloat(s.substring(0, ind));
                        else
                            v[i] = Float.parseFloat(s);
                        s = s.substring(s.indexOf(" ") + 1).trim();
                    }

                    uvs.add(new float[]{v[0], 1F - v[1]});
                    continue;
                }
                if(s.startsWith("vn "))
                {
                    s = s.substring(s.indexOf(" ") + 1).trim();
                    float[] v = new float[3];
                    for(int i = 0; i < 3; i++)
                    {
                        int ind = s.indexOf(" ");
                        if(ind > -1)
                            v[i] = Float.parseFloat(s.substring(0, ind));
                        else
                            v[i] = Float.parseFloat(s);
                        s = s.substring(s.indexOf(" ") + 1).trim();
                    }

                    float flt = v[2];
                    v[2] = v[1];
                    v[1] = flt;

                    normals.add(new float[]{v[0], v[1], v[2]});
                    continue;
                }
                if(s.startsWith("f "))
                {
                    s = s.substring(s.indexOf(" ") + 1).trim();
                    List<PositionTextureVertex> v = new ArrayList<>();
                    String s1;
                    int finalPhase = 0;
                    float[] normal = new float[]{0F, 0F, 0F};
                    List<Vec3> iNormal = new ArrayList<>();
                    do
                    {
                        int vInt;
                        float[] curUV;
                        float[] curNormals;
                        int ind = s.indexOf(" ");
                        s1 = s;
                        if(ind > -1)
                            s1 = s.substring(0, ind);
                        if(s1.contains("/"))
                        {
                            String[] f = s1.split("/");
                            vInt = Integer.parseInt(f[0]) - 1;
                            if(f[1].isEmpty())
                                f[1] = f[0];
                            int vtInt = Integer.parseInt(f[1]) - 1;
                            if(uvs.size() > vtInt)
                                curUV = uvs.get(vtInt);
                            else
                                curUV = new float[]{0, 0};
                            int vnInt;
                            if(f.length == 3)
                            {
                                if(f[2].isEmpty())
                                    f[2] = f[0];
                                vnInt = Integer.parseInt(f[2]) - 1;
                            }
                            else
                                vnInt = Integer.parseInt(f[0]) - 1;
                            if(normals.size() > vnInt)
                                curNormals = normals.get(vnInt);
                            else
                                curNormals = new float[]{0, 0, 0};
                        }
                        else
                        {
                            vInt = Integer.parseInt(s1) - 1;
                            if(uvs.size() > vInt)
                                curUV = uvs.get(vInt);
                            else
                                curUV = new float[]{0, 0};
                            if(normals.size() > vInt)
                                curNormals = normals.get(vInt);
                            else
                                curNormals = new float[]{0, 0, 0};
                        }

                        iNormal.add(new Vec3(curNormals[0], curNormals[1], curNormals[2]));

                        normal[0] += curNormals[0];
                        normal[1] += curNormals[1];
                        normal[2] += curNormals[2];

                        if(vInt < verts.size())
                        {
                            PositionTransformVertex vertex = verts.get(vInt);
                            v.add(vertex.setTexturePosition(curUV[0], curUV[1]));
                            vertex.addGroup(group);
                        }
                        if(ind > -1)
                            s = s.substring(s.indexOf(" ") + 1).trim();
                        else
                            finalPhase++;
                    } while(finalPhase < 1);

                    float d = (float) Math.sqrt(normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2]);

                    if(d != 0F)
                    {
                        normal[0] /= d;
                        normal[1] /= d;
                        normal[2] /= d;
                    }

                    PositionTextureVertex[] vToArr = v.toArray(new PositionTextureVertex[0]);

                    TexturedPolygon poly = new TexturedPolygon(vToArr);
                    poly.setNormals(normal[0], normal[1], normal[2]);
                    poly.setNormals(iNormal);

                    face.add(poly);
                    texture.addPoly(poly);
                }
            }

            vertices = verts.toArray(new PositionTransformVertex[0]);
            faces = face.toArray(new TexturedPolygon[0]);
        }
        catch(IOException | RuntimeException ignored)
        {
            // Keep legacy behavior: invalid or unreadable OBJ files are ignored by the model pool.
        }
    }
}
