package com.berlinbrown.mech.umbra.world;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Disposable;
import java.util.*;

/** One self-contained level. Add areas by implementing this same small lifecycle. */
public class DarkCourtyardScene implements Disposable {
    private final List<Model> models=new ArrayList<Model>();
    private final List<ModelInstance> instances=new ArrayList<ModelInstance>();
    private final List<ModelInstance> enemyParts=new ArrayList<ModelInstance>();
    private final Vector3 enemyPosition=new Vector3(1.5f,0,-7.5f);
    private float pulse; private boolean enemyAlive=true;

    public void create(){ModelBuilder mb=new ModelBuilder();long a=Usage.Position|Usage.Normal;
        Model floor=track(mb.createBox(30,.35f,31,mat(.10f,.115f,.105f),a));instances.add(at(floor,0,-.2f,0));
        Model stone=track(mb.createBox(2.2f,2.4f,2.2f,mat(.16f,.17f,.16f),a));
        for(int x=-12;x<=12;x+=4){instances.add(at(stone,x,1.1f,-14.6f));instances.add(at(stone,x,1.1f,14.6f));}
        for(int z=-12;z<=12;z+=4){instances.add(at(stone,-14.6f,1.1f,z));instances.add(at(stone,14.6f,1.1f,z));}
        Model pillar=track(mb.createCylinder(1.1f,4.8f,1.1f,10,mat(.19f,.19f,.17f),a));
        instances.add(at(pillar,-8,2.2f,-8));instances.add(at(pillar,8,2.2f,-8));instances.add(at(pillar,-8,2.2f,7));instances.add(at(pillar,8,2.2f,7));
        Model sanctuary=track(mb.createCylinder(9,.25f,9,32,mat(.14f,.18f,.17f),a));instances.add(at(sanctuary,0,.04f,7));
        Model brazier=track(mb.createCone(1.15f,2.4f,1.15f,10,mat(.25f,.14f,.07f),a));
        Model flame=track(mb.createSphere(.58f,1.2f,.58f,12,10,mat(.95f,.28f,.04f),a));
        for(float x:new float[]{-5,5}){instances.add(at(brazier,x,1.1f,3));instances.add(at(flame,x,2.55f,3));}
        addHumanoid(mb,-3.2f,6,new Color(.32f,.25f,.16f,1),new Color(.62f,.48f,.34f,1),false);
        addHumanoid(mb,3.5f,8,new Color(.16f,.25f,.27f,1),new Color(.54f,.39f,.30f,1),false);
        addHumanoid(mb,enemyPosition.x,enemyPosition.z,new Color(.20f,.28f,.20f,1),new Color(.36f,.48f,.31f,1),true);
        Model grave=track(mb.createBox(.8f,1.6f,.25f,mat(.20f,.21f,.19f),a));for(int i=0;i<8;i++)instances.add(at(grave,-10+(i%4)*2.3f,.72f,-3-(i/4)*2.4f));
    }
    private void addHumanoid(ModelBuilder mb,float x,float z,Color cloth,Color skin,boolean hostile){long a=Usage.Position|Usage.Normal;
        Model body=track(mb.createCapsule(.48f,1.9f,14,new Material(ColorAttribute.createDiffuse(cloth)),a));
        Model head=track(mb.createSphere(.68f,.76f,.66f,14,10,new Material(ColorAttribute.createDiffuse(skin)),a));
        ModelInstance b=at(body,x,1,z),h=at(head,x,2.15f,z);instances.add(b);instances.add(h);if(hostile){enemyParts.add(b);enemyParts.add(h);}}
    private Material mat(float r,float g,float b){return new Material(ColorAttribute.createDiffuse(new Color(r,g,b,1)));}
    private Model track(Model m){models.add(m);return m;}
    private ModelInstance at(Model m,float x,float y,float z){ModelInstance i=new ModelInstance(m);i.transform.setToTranslation(x,y,z).rotate(Vector3.Y,MathUtils.random(-6f,6f));return i;}
    public void update(float delta){pulse+=delta;if(enemyAlive){float bob=MathUtils.sin(pulse*2)*.025f;enemyParts.get(0).transform.setToTranslation(enemyPosition.x,1+bob,enemyPosition.z);enemyParts.get(1).transform.setToTranslation(enemyPosition.x,2.15f+bob,enemyPosition.z);}}
    public List<ModelInstance> getInstances(){return instances;} public Vector3 getEnemyPosition(){return enemyPosition;} public boolean isEnemyAlive(){return enemyAlive;}
    public void defeatEnemy(){enemyAlive=false;for(ModelInstance p:enemyParts)instances.remove(p);}public void resetEnemy(){if(enemyAlive)return;enemyAlive=true;instances.addAll(enemyParts);}
    public static Texture solidTexture(Color c){Pixmap p=new Pixmap(1,1,Pixmap.Format.RGBA8888);p.setColor(c);p.fill();Texture t=new Texture(p);p.dispose();return t;}
    @Override public void dispose(){for(Model m:models)m.dispose();}
}
