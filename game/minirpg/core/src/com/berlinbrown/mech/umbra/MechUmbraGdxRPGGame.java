package com.berlinbrown.mech.umbra;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.*;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.berlinbrown.mech.umbra.rpg.Character;
import com.berlinbrown.mech.umbra.world.DarkCourtyardScene;

/** A compact, asset-light dark fantasy RPG vertical slice. */
public class MechUmbraGdxRPGGame implements ApplicationListener {
    private static final float MOVE_SPEED = 4.2f, TURN_SPEED = 110f, ENCOUNTER_DISTANCE = 2.5f, ATTACK_INTERVAL = .72f;
    public static final Character hero = new Character("The Wanderer");
    public static final Character enemy = new Character("Hollow Guard");
    public static float timeElapsed;
    public static int counter;
    public static String lastMessage = "Find the broken gate beyond the sanctuary.";
    public static boolean onOffMsg;

    private final Vector3 heroPosition = new Vector3(0, 0, 8), clickTarget = new Vector3(), facing = new Vector3(0, 0, -1);
    private final Vector3 cameraOffset = new Vector3(0, 8.5f, 11.5f);
    private final Plane groundPlane = new Plane(Vector3.Y, 0f);
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private DarkCourtyardScene scene;
    private Model heroBodyModel, heroHeadModel, markerModel;
    private ModelInstance heroBody, heroHead, moveMarker;
    private Stage uiStage;
    private Skin skin;
    private Label statusLabel, promptLabel;
    private Table encounterPanel;
    private Texture portraitTexture;
    private boolean hasClickTarget, encounterOpen, autoCombat, gameOver;
    private float heroYaw, attackTimer;

    @Override public void create() {
        hero.maxHealthPoints = hero.healthPoints = 100; hero.attackDamage = 24; hero.defense = 5;
        enemy.maxHealthPoints = enemy.healthPoints = 82; enemy.attackDamage = 13; enemy.defense = 3;
        Gdx.gl.glClearColor(.012f, .018f, .025f, 1f);
        modelBatch = new ModelBatch();
        camera = new PerspectiveCamera(54f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = .1f; camera.far = 120f;
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, .13f, .16f, .19f, 1f));
        environment.add(new DirectionalLight().set(.32f, .38f, .46f, -.7f, -1f, -.35f));
        environment.add(new PointLight().set(1f, .43f, .14f, -5f, 3.2f, 3f, 22f));
        environment.add(new PointLight().set(1f, .38f, .11f, 5f, 3.2f, -5f, 18f));
        scene = new DarkCourtyardScene(); scene.create();
        createHero(); createUi(); updateHeroModels(); updateCamera(true);
        Gdx.input.setInputProcessor(new InputMultiplexer(uiStage, new WorldInput()));
    }

    private void createHero() {
        ModelBuilder mb = new ModelBuilder(); long attrs = Usage.Position | Usage.Normal;
        heroBodyModel = mb.createCapsule(.52f, 2.15f, 16, mat(.12f, .16f, .19f), attrs);
        heroHeadModel = mb.createSphere(.76f, .82f, .72f, 16, 12, mat(.58f, .43f, .31f), attrs);
        markerModel = mb.createCylinder(1.05f, .045f, 1.05f, 32, mat(.22f, .75f, .88f), attrs);
        heroBody = new ModelInstance(heroBodyModel); heroHead = new ModelInstance(heroHeadModel); moveMarker = new ModelInstance(markerModel);
    }

    private Material mat(float r, float g, float b) { return new Material(ColorAttribute.createDiffuse(new Color(r,g,b,1))); }

    private void createUi() {
        uiStage = new Stage(new ScreenViewport()); skin = new Skin();
        BitmapFont small = makeFont(17), title = makeFont(28);
        skin.add("small", small); skin.add("title-font", title);
        skin.add("panel", DarkCourtyardScene.solidTexture(new Color(.035f,.045f,.052f,.92f)));
        skin.add("button", DarkCourtyardScene.solidTexture(new Color(.25f,.19f,.12f,1)));
        skin.add("button-over", DarkCourtyardScene.solidTexture(new Color(.47f,.30f,.12f,1)));
        skin.add("button-down", DarkCourtyardScene.solidTexture(new Color(.12f,.09f,.07f,1)));
        skin.add("default", new Label.LabelStyle(small, new Color(.86f,.82f,.70f,1)));
        TextButton.TextButtonStyle bs = new TextButton.TextButtonStyle();
        bs.font=title; bs.fontColor=new Color(.94f,.84f,.61f,1); bs.up=skin.newDrawable("button"); bs.over=skin.newDrawable("button-over"); bs.down=skin.newDrawable("button-down"); skin.add("default",bs);
        portraitTexture = new Texture(Gdx.files.internal("ui/umbra-hero.png")); portraitTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Image portrait = new Image(new TextureRegionDrawable(new TextureRegion(portraitTexture)));
        Table root=new Table(); root.setFillParent(true); root.top().left().pad(18);
        Table card=new Table(); card.setBackground(skin.newDrawable("panel")); card.pad(10); card.add(portrait).size(88).padRight(12);
        Table stats=new Table(); stats.add(new Label("THE WANDERER",new Label.LabelStyle(title,new Color(.88f,.70f,.38f,1)))).left().row();
        statusLabel=new Label("",skin); stats.add(statusLabel).left().padTop(5); card.add(stats).left(); root.add(card).left(); root.add().expandX();
        Label quest=new Label("SANCTUARY OF ASH\nWASD  move / turn   •   Click ground to travel   •   ESC quit",skin); quest.setAlignment(Align.right); root.add(quest).right().top().pad(12); root.row();
        promptLabel=new Label("",skin); promptLabel.setAlignment(Align.center); root.add(promptLabel).colspan(3).expand().bottom().center().padBottom(22); uiStage.addActor(root);
        encounterPanel=new Table(); encounterPanel.setBackground(skin.newDrawable("panel")); encounterPanel.pad(24);
        encounterPanel.add(new Label("A HOLLOW GUARD BLOCKS YOUR PATH",new Label.LabelStyle(title,new Color(.84f,.57f,.28f,1)))).colspan(2).padBottom(10).row();
        encounterPanel.add(new Label("The world is paused. Choose your response.",skin)).colspan(2).padBottom(18).row();
        TextButton attack=new TextButton("ATTACK",skin), flee=new TextButton("FLEE",skin);
        encounterPanel.add(attack).width(180).height(54).padRight(10); encounterPanel.add(flee).width(180).height(54); encounterPanel.pack(); encounterPanel.setVisible(false); uiStage.addActor(encounterPanel);
        attack.addListener(new ClickListener(){@Override public void clicked(InputEvent e,float x,float y){beginCombat();}});
        flee.addListener(new ClickListener(){@Override public void clicked(InputEvent e,float x,float y){fleeEncounter();}});
    }

    private BitmapFont makeFont(int size) {
        FreeTypeFontGenerator g=new FreeTypeFontGenerator(Gdx.files.internal("fonts/Roboto-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter p=new FreeTypeFontGenerator.FreeTypeFontParameter(); p.size=size; p.magFilter=p.minFilter=Texture.TextureFilter.Linear;
        BitmapFont f=g.generateFont(p); g.dispose(); return f;
    }

    @Override public void render() {
        float delta=Math.min(Gdx.graphics.getDeltaTime(),.05f); timeElapsed+=delta;
        if(!encounterOpen&&!gameOver) updateMovement(delta); if(autoCombat&&!gameOver) updateCombat(delta);
        scene.update(delta); updateHeroModels(); updateCamera(false); updateUi();
        Gdx.gl.glViewport(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight()); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);
        modelBatch.begin(camera); for(ModelInstance item:scene.getInstances()) modelBatch.render(item,environment);
        modelBatch.render(heroBody,environment); modelBatch.render(heroHead,environment); if(hasClickTarget&&!encounterOpen) modelBatch.render(moveMarker,environment); modelBatch.end();
        uiStage.act(delta); uiStage.draw();
    }

    private void updateMovement(float delta) {
        boolean moved=false;
        if(Gdx.input.isKeyPressed(Input.Keys.A)){heroYaw+=TURN_SPEED*delta;hasClickTarget=false;}
        if(Gdx.input.isKeyPressed(Input.Keys.D)){heroYaw-=TURN_SPEED*delta;hasClickTarget=false;}
        facing.set(0,0,-1).rotate(Vector3.Y,heroYaw); float direction=0;
        if(Gdx.input.isKeyPressed(Input.Keys.W))direction++; if(Gdx.input.isKeyPressed(Input.Keys.S))direction--;
        if(direction!=0){heroPosition.mulAdd(facing,MOVE_SPEED*direction*delta);hasClickTarget=false;moved=true;}
        else if(hasClickTarget){Vector3 toward=clickTarget.cpy().sub(heroPosition);toward.y=0;if(toward.len2()<.12f)hasClickTarget=false;else{float distance=toward.len();toward.nor();heroYaw=(float)Math.toDegrees(Math.atan2(-toward.x,-toward.z));facing.set(toward);heroPosition.mulAdd(toward,Math.min(MOVE_SPEED*delta,distance));moved=true;}}
        heroPosition.x=MathUtils.clamp(heroPosition.x,-13.5f,13.5f); heroPosition.z=MathUtils.clamp(heroPosition.z,-14f,13.5f);
        if(moved&&scene.isEnemyAlive()&&heroPosition.dst(scene.getEnemyPosition())<ENCOUNTER_DISTANCE)openEncounter();
    }

    private void openEncounter(){encounterOpen=true;hasClickTarget=false;lastMessage="Time stands still around the Hollow Guard.";encounterPanel.setVisible(true);centerEncounterPanel();}
    private void beginCombat(){encounterPanel.setVisible(false);autoCombat=true;attackTimer=.2f;lastMessage="Steel answers the dead.";}
    private void fleeEncounter(){encounterPanel.setVisible(false);encounterOpen=false;autoCombat=false;heroPosition.mulAdd(heroPosition.cpy().sub(scene.getEnemyPosition()).nor(),4f);lastMessage="You retreat toward sanctuary.";}
    private void updateCombat(float delta){attackTimer-=delta;if(attackTimer>0)return;attackTimer=ATTACK_INTERVAL;if(!hero.isAlive()||!enemy.isAlive())return;enemy.takeDamage(hero.attackDamage);counter++;
        if(!enemy.isAlive()){scene.defeatEnemy();autoCombat=false;encounterOpen=false;lastMessage="The Hollow Guard falls. The ruined gate is open.";return;}
        hero.takeDamage(enemy.attackDamage);if(!hero.isAlive()){autoCombat=false;gameOver=true;lastMessage="You have fallen. Press R to rise again.";}else lastMessage="Auto-attacking… blades strike in the gloom.";}
    private void updateHeroModels(){heroBody.transform.setToTranslation(heroPosition.x,1.08f,heroPosition.z).rotate(Vector3.Y,heroYaw);heroHead.transform.setToTranslation(heroPosition.x,2.35f,heroPosition.z);moveMarker.transform.setToTranslation(clickTarget.x,.08f,clickTarget.z);}
    private void updateCamera(boolean snap){Vector3 desired=heroPosition.cpy().add(cameraOffset);if(snap)camera.position.set(desired);else camera.position.lerp(desired,.09f);camera.lookAt(heroPosition.x,1.1f,heroPosition.z-2.3f);camera.up.set(Vector3.Y);camera.update();}
    private void updateUi(){statusLabel.setText("HP  "+Math.max(0,hero.healthPoints)+" / "+hero.maxHealthPoints+"\nHollow Guard  "+Math.max(0,enemy.healthPoints)+" / "+enemy.maxHealthPoints);promptLabel.setText(lastMessage);}
    private void centerEncounterPanel(){encounterPanel.setPosition((Gdx.graphics.getWidth()-encounterPanel.getWidth())/2f,(Gdx.graphics.getHeight()-encounterPanel.getHeight())/2f);}

    private class WorldInput extends InputAdapter {
        @Override public boolean keyDown(int keycode){if(keycode==Input.Keys.ESCAPE){Gdx.app.exit();return true;}if(keycode==Input.Keys.R&&gameOver){hero.healthPoints=hero.maxHealthPoints;enemy.healthPoints=enemy.maxHealthPoints;heroPosition.set(0,0,8);scene.resetEnemy();gameOver=encounterOpen=autoCombat=false;lastMessage="The sanctuary grants one more chance.";return true;}return false;}
        @Override public boolean touchDown(int x,int y,int pointer,int button){if(encounterOpen||gameOver||button!=Input.Buttons.LEFT)return false;Ray ray=camera.getPickRay(x,y);if(Intersector.intersectRayPlane(ray,groundPlane,clickTarget)){clickTarget.x=MathUtils.clamp(clickTarget.x,-13.5f,13.5f);clickTarget.z=MathUtils.clamp(clickTarget.z,-14f,13.5f);hasClickTarget=true;lastMessage="Traveling…";return true;}return false;}
    }
    @Override public void resize(int w,int h){camera.viewportWidth=w;camera.viewportHeight=h;camera.update();uiStage.getViewport().update(w,h,true);centerEncounterPanel();}
    @Override public void pause(){} @Override public void resume(){}
    @Override public void dispose(){modelBatch.dispose();heroBodyModel.dispose();heroHeadModel.dispose();markerModel.dispose();scene.dispose();portraitTexture.dispose();uiStage.dispose();skin.dispose();}
}
