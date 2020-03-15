package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.input.ChaseCamera;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.scene.shape.Box;
import com.jme3.scene.Spatial;
import com.jme3.scene.Geometry;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.util.TangentBinormalGenerator;

/**
 *
 * @author normen, with edits by Zathras Majorly Modified by George Sandrowicz
 * for a game idea's design and testing.
 */
public class Main extends SimpleApplication
        implements ActionListener {

    public static final Quaternion ROLL180 = new Quaternion().fromAngleAxis(FastMath.PI, new Vector3f(0, 0, 1));
    public static final Quaternion PITCH180 = new Quaternion().fromAngleAxis(FastMath.PI, new Vector3f(1, 0, 0));
    public float playersped = 0.4f;
    Box boxMesh;
    private int crosshairindx = 0;
    private String[] Crosshairs = {"+", "O", "0", "-", "(+)", "[+]", "<+>", "(O)", "[O]", "<O>", "(0)", "[0]", "<0>", "(-)", "[-]", "<->"};
    private Spatial sceneModel;
    private BulletAppState bappstate;
    private RigidBodyControl landscape;
    private CharacterControl player;
    BitmapText ch;
    Geometry boxGeo;
    private Vector3f walkDirection = new Vector3f();
    private boolean left = false, right = false, up = false, down = false;
    private Spatial modelMesh;
    private RigidBodyControl physicsObject3d;

    //Temporary vectors used on each frame.
    //They here to avoid instanciating new vectors on each frame
    private Vector3f camDir;
    private Vector3f camLeft;

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {

        //modelMesh = assetManager.loadModel("Models/spiderex1.j3o");
        //TangentBinormalGenerator.generate(modelMesh);
        //physicsObject3d = new RigidBodyControl(0f);
        camDir = new Vector3f();
        camLeft = new Vector3f();
 
        /**
         * Set up Physics
         */
        bappstate = new BulletAppState();
        stateManager.attach(bappstate);
        //bulletAppState.setDebugEnabled(true);

        // We re-use the flyby camera for rotation, while positioning is handled by physics
        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        bappstate.update(5);
        setUpKeys();
        setUpLight();
        // We load the scene from the zip file and adjust its size.
        assetManager.registerLocator("town.zip", ZipLocator.class);
        sceneModel = assetManager.loadModel("main.scene");
        sceneModel.setLocalScale(2f);

        // We set up collision detection for the scene by creating a
        // compound collision shape and a static RigidBodyControl with mass zero.
        CollisionShape sceneShape
                = CollisionShapeFactory.createMeshShape(sceneModel);
        landscape = new RigidBodyControl(sceneShape, 0);
        sceneModel.addControl(landscape);

        /**
         * We set up collision detection for the player by creating a capsule
         * collision shape and a CharacterControl. The CharacterControl offers
         * extra settings for size, stepheight, jumping, falling, and gravity.
         * We also put the player in its starting position.
         */
        BoxCollisionShape capsuleShape = new BoxCollisionShape(new Vector3f(1.5f, 6f, 1));
        player = new CharacterControl(capsuleShape, 0.05f);
        player.setJumpSpeed(15);
        player.setFallSpeed(35);

        // We attach the scene and the player to the rootnode and the physics space,
        // to make them appear in the game world.
        rootNode.attachChild(sceneModel);
        bappstate.getPhysicsSpace().add(landscape);

        // You can change the gravity of individual physics objects before or after
        //they are added to the PhysicsSpace, but it must be set before MOVING the
        //physics location.
        player.setGravity(new Vector3f(0, -40f, 0));
        player.setPhysicsLocation(new Vector3f(0, 10, 0));
        initCrossHairs();

        boxMesh = new Box(0.5f, player.getCollisionGroup(), 0.5f);
        boxGeo = new Geometry("chrply", boxMesh);
    }

    private void setUpLight() {
        // We add light so we see the scene
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(1.3f));
        rootNode.addLight(al);

        DirectionalLight dl = new DirectionalLight();
        dl.setColor(ColorRGBA.Gray);
        dl.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
        rootNode.addLight(dl);
        /* this shadow needs a directional light */
        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, 1024, 2);
        dlsr.setLight(dl);
        viewPort.addProcessor(dlsr);

    }

    /**
     * We over-write some navigational key mappings here, so we can add
     * physics-controlled walking and jumping:
     */
    private void setUpKeys() {
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("CursorChange", new KeyTrigger(KeyInput.KEY_0));
        inputManager.addListener(this, "Left");
        inputManager.addListener(this, "Right");
        inputManager.addListener(this, "Up");
        inputManager.addListener(this, "Down");
        inputManager.addListener(this, "CursorChange");
    }

    protected void initCrossHairs() {
        //setDisplayStatView(false);
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText(Crosshairs[crosshairindx]); // crosshairs
        ch.setLocalTranslation( // center
                settings.getWidth() / 2 - ch.getLineWidth() / 2,
                settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
        guiNode.attachChild(ch);
    }

    /**
     * These are our custom actions triggered by key presses. We do not walk
     * yet, we just keep track of the direction the user pressed.
     */
    /**
     * This is the main event loop--walking happens here. We check in which
     * direction the player is walking by interpreting the camera direction
     * forward (camDir) and to the side (camLeft). The setWalkDirection()
     * command is what lets a physics-controlled player walk. We also make sure
     * here that the camera moves with player.
     */
    @Override
    public void simpleUpdate(float tpf) {
        camDir.set(cam.getDirection()).multLocal(playersped + (playersped * (7 / 8)));
        camLeft.set(cam.getLeft()).multLocal(playersped);
        walkDirection.set(0, 0, 0);
        Vector3f forwrds = new Vector3f(camDir.x, 0, camDir.z);
        if (left) {
            walkDirection.addLocal(camLeft);
        }
        if (right) {
            walkDirection.addLocal(camLeft.negate());
        }
        if (up) {
            walkDirection.addLocal(forwrds);
        }
        if (down) {
            walkDirection.addLocal(forwrds.negate());
        }

        player.setWalkDirection(walkDirection);
        cam.setLocation(new Vector3f(player.getPhysicsLocation().x, player.getPhysicsLocation().y + 3, player.getPhysicsLocation().z));
        Material boxMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        boxMat.setBoolean("UseMaterialColors", true);
        boxMat.setColor("Ambient", ColorRGBA.Green);
        boxMat.setColor("Diffuse", ColorRGBA.Green);
        boxGeo.setMaterial(boxMat);
        boxGeo.addControl(player);
        bappstate.getPhysicsSpace().add(player);
        rootNode.attachChild(boxGeo);
    }

    @Override
    public void onAction(String binding, boolean isPressed, float tpf) {
        if (isPressed) {
            rootNode.detachChild(boxGeo);
        }
        if (binding.equals("Left")) {
            left = isPressed;
        } else if (binding.equals("Right")) {
            right = isPressed;
        } else if (binding.equals("Up")) {
            up = isPressed;
        } else if (binding.equals("Down")) {
            down = isPressed;
        } else if (binding.equals("CursorChange")) {
            if (isPressed) {
                if (crosshairindx < Crosshairs.length) {
                    crosshairindx++;
                    if (crosshairindx >= Crosshairs.length) {
                        crosshairindx = 0;
                    }
                }
                ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
                ch.setText(Crosshairs[crosshairindx]); // crosshairs
                ch.setLocalTranslation( // center
                        settings.getWidth() / 2 - ch.getLineWidth() / 2,
                        settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
            }
        }
    }
}
