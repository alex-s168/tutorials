package uk.co.cablepost.tutorials.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.cablepost.tutorials.*;
import uk.co.cablepost.tutorials.client.TutorialsClient;
import uk.co.cablepost.tutorials.util.ArrUtil;

import java.util.*;

public class TutorialScreen extends HandledScreen<TutorialScreenHandler> {

    public static final Identifier BACKGROUND_TEXTURE = new Identifier(TutorialsClient.MOD_ID, "textures/gui/tutorials/tutorial.png");
    public static final Identifier BACKGROUND_TEXTURE_TUT_COMPLETE = new Identifier(TutorialsClient.MOD_ID, "textures/gui/tutorials/tutorial_complete.png");
    public static final Identifier BUTTONS_TEXTURE = new Identifier(TutorialsClient.MOD_ID, "textures/gui/tutorials/tutorial_buttons.png");

    @Nullable
    private List<Tutorial.Parsed> tutorials = null;

    @Nullable
    private Tutorial.Parsed tutorial = null;

    @Nullable
    private Map<String, int[]> keyframesByObject = null;

    private int lastKeyframe = 0;

    @Nullable
    private Map<String, State> state = null;

    private record State(
            @NotNull
            TutorialInstruction.Keyframe kf,

            @Nullable
            TutorialInstruction instr
    ) {}

    @Nullable
    private Identifier tutorialItem = null;

    private float playbackTime = 0f;

    public boolean playing = true;
    public int playbackSpeed = 1;

    public int firstPlaybackFrames = 0;

    public TutorialScreen(PlayerInventory inventory, Text title) {
        super(new TutorialScreenHandler(), inventory, title);
    }

    public TutorialScreen(PlayerEntity player) {
        this(player.getInventory(), new TranslatableText("container.tutorial"));
        this.passEvents = true;
        backgroundWidth = 256 + 128;
        backgroundHeight = 224;
        titleX = 15;
        titleY = 5;
    }

    @Override
    protected void init(){
        super.init();

    }

    public boolean setTutorials(Identifier identifier) {
        if (TutorialsClient.tutorialsByItems.containsKey(identifier)) {
            tutorialItem = identifier;
            tutorials = TutorialsClient.tutorialsByItems.get(identifier);

            tutorials.sort(Comparator.comparingInt(o -> o.tut().priority));

            setTutorial(tutorials.get(0));
            return true;
        }
        return false;
    }

    public void setTutorial(Tutorial.Parsed tut) {
        playbackSpeed = 1;
        playbackTime = -1000f;
        firstPlaybackFrames = 0;

        tutorial = tut;

        state = new HashMap<>();

        for (Map.Entry<String, TutorialObject> object : tut.tut().scene_objects.entrySet()) {
            var kf = TutorialInstruction.Keyframe.getDefault(object.getValue().renderer.defaultKeyframe());
            state.put(object.getKey(), new State(kf, null));
        }

        for (int i = 0; i < tut.tut().scene_instructions.size(); i ++) {
            var instr = tut.tut().scene_instructions.get(i);
            if (instr.time > 0) {
                break; // break, not continue  because sorted by time
            }

            var kf = tut.keyframes()[i];

            state.put(instr.object_id, new State(kf, instr));

            lastKeyframe = i;
        }

        playbackTime = -8f;

        keyframesByObject = new HashMap<>();

        for (int i = 0; i < tut.keyframes().length; i ++) {
            var instr = tut.tut().scene_instructions.get(i);
            var list = keyframesByObject.computeIfAbsent(instr.object_id, (k) -> new int[0]);
            list = ArrUtil.concat(list, i);
            keyframesByObject.put(instr.object_id, list);
        }
    }

    float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }

    float invLerp(float a, float b, float f) {
        return (f - a) / (b - a);
    }

    boolean isLastKeyFrame() {
        return tutorial != null && lastKeyframe == tutorial.keyframes().length - 1;
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        if (tutorial != null && playing) {
            playbackTime += delta * (float) playbackSpeed;
            if (firstPlaybackFrames < 100){
                firstPlaybackFrames++;
            }
        }

        DiffuseLighting.disableGuiDepthLighting();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        if (isLastKeyFrame()){
            RenderSystem.setShaderTexture(0, BACKGROUND_TEXTURE_TUT_COMPLETE);
        }
        else{
            RenderSystem.setShaderTexture(0, BACKGROUND_TEXTURE);
        }
        int withMinusBgWidth = (this.width - this.backgroundWidth) / 2;
        int heightMinusBgHeight = (this.height - this.backgroundHeight) / 2;
        this.drawTexture(matrices, withMinusBgWidth, heightMinusBgHeight, 0, 0, this.backgroundWidth - 128, this.backgroundHeight);

        RenderSystem.setShaderTexture(0, BUTTONS_TEXTURE);
        this.drawTexture(matrices, withMinusBgWidth + 256, heightMinusBgHeight, 128, 0, 128, this.backgroundHeight);

        if (tutorials != null) {
            int c = 0;
            //for (Map.Entry<String, Tutorial> entry : tutorials.entrySet()) {
            for (Tutorial.Parsed tut : tutorials) {
                boolean mouseOver = mouseX >= withMinusBgWidth + 256 + 2 && mouseX <= withMinusBgWidth + 256 + 2 + 108 && mouseY >= heightMinusBgHeight + 15 + c && mouseY <= heightMinusBgHeight + 15 + c + 20;
                if (Objects.equals(tut.tut().display_name, tutorial.tut().display_name)) {
                    if (mouseOver) {
                        this.drawTexture(matrices, withMinusBgWidth + 256 + 2, heightMinusBgHeight + c + 15, 0, 120, 128 - 20, 20);
                    } else {
                        this.drawTexture(matrices, withMinusBgWidth + 256 + 2, heightMinusBgHeight + c + 15, 0, 0, 128 - 20, 20);
                    }
                } else {
                    if (mouseOver) {
                        this.drawTexture(matrices, withMinusBgWidth + 256 + 2, heightMinusBgHeight + c + 15, 0, 40, 128 - 20, 20);
                    } else {
                        this.drawTexture(matrices, withMinusBgWidth + 256 + 2, heightMinusBgHeight + c + 15, 0, 20, 128 - 20, 20);
                    }
                }
                c += 20;
            }
        }

        int scaleFactor = (int)this.client.getWindow().getScaleFactor();
        int realWidth = this.client.getWindow().getFramebufferWidth();
        int realHeight = this.client.getWindow().getFramebufferHeight();

        if (tutorial != null) {
            assert state != null;
            assert keyframesByObject != null;

            var camera = state.get("camera").kf.base;

            RenderSystem.viewport(((this.width - backgroundWidth) / 2 + 15) * scaleFactor, ((this.height - backgroundHeight) / 2 + 15) * scaleFactor, (256 - 30) * scaleFactor, (backgroundHeight - 30) * scaleFactor);
            Matrix4f matrix4f = Matrix4f.translate(0f, -0.25f, -1f);
            //matrix4f.multiply(Matrix4f.viewboxMatrix(66.0f, (float)backgroundHeight / 256f, 0.01f, 1000.0f));
            matrix4f.multiply(Matrix4f.viewboxMatrix(camera.fov, ((float)backgroundHeight - 30f) / (256f - 30f), 0.01f, 1000.0f));
            RenderSystem.backupProjectionMatrix();
            RenderSystem.setProjectionMatrix(matrix4f);
            matrices.push();
            MatrixStack.Entry entry = matrices.peek();
            entry.getPositionMatrix().loadIdentity();
            entry.getNormalMatrix().loadIdentity();

            if (firstPlaybackFrames > 25) {//hides the scene from rending till ready
                matrices.translate(0.0, 0.0f, 1999.84f);
            }
            matrices.scale(0.2f, 0.24f, 0.2f);

            matrices.translate(-camera.pos[0], -camera.pos[1], -camera.pos[2]);

            float sceneRotation = camera.rot[1]; //;tutorial.force_angle ? tutorial.angle : (((float)mouseX / realWidth * 1000) + 90 + 45);
            matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(sceneRotation));
            matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(camera.rot[0]));

            VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());


            var currentKeyframes = new HashMap<String, TutorialInstruction.Keyframe>();


            /* +++ Keyframe interpolation */
            int newLast = lastKeyframe;
            for (Map.Entry<String, State> object : state.entrySet()) {
                var state = object.getValue();
                var keyframeIds = keyframesByObject.get(object.getKey());
                var next = ArrUtil.first(keyframeIds, (x) -> x > lastKeyframe);
                if (next == null) {
                    currentKeyframes.put(object.getKey(), state.kf);
                    continue;
                }
                newLast = next;
                var nextKf = tutorial.keyframes()[next];
                var nextInst = tutorial.tut().scene_instructions.get(next);
                var obj = tutorial.tut().scene_objects.get(object.getKey());

                var lastTime = state.instr == null ? 0 : state.instr.time;
                var nextTime = nextInst.time;
                obj.interpolator.update(invLerp((float) lastTime, (float) nextTime, playbackTime));

                var interp = state.kf.interpolate(nextKf, obj.interpolator);
                currentKeyframes.put(object.getKey(), interp);
            }
            lastKeyframe = newLast;
            /* --- Keyframe interpolation */


            /* +++ Rendering */
            for (Map.Entry<String, TutorialInstruction.Keyframe> object : currentKeyframes.entrySet()) {
                var obj = tutorial.tut().scene_objects.get(object.getKey());
                var kf = object.getValue();

                if (kf.base.show) {
                    matrices.push();

                    matrices.translate(kf.base.pos[0], kf.base.pos[1], kf.base.pos[2]);

                    matrices.scale(kf.base.scale[0], kf.base.scale[1], kf.base.scale[2]);

                    matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(kf.base.rot[0]));
                    matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(kf.base.rot[1]));
                    matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(kf.base.rot[2]));

                    obj.renderer.render(TutorialsClient.LOGGER, matrices, immediate, textRenderer, kf.other);

                    matrices.pop();
                }
            }
            /* --- Rendering */


            // TODO: wtf
            /* +++ Draw an item as prevents textures going over the play buttons? */
            matrices.translate(0f, 50f, 0f);

            MinecraftClient.getInstance().getItemRenderer().renderItem(
                    Items.ACACIA_BOAT.getDefaultStack(),
                    ModelTransformation.Mode.NONE,
                    255,
                    OverlayTexture.DEFAULT_UV,
                    matrices,
                    immediate,
                    0
            );

            immediate.draw();
            /* --- */


            matrices.pop();

            RenderSystem.viewport(0, 0, this.client.getWindow().getFramebufferWidth(), this.client.getWindow().getFramebufferHeight());
            RenderSystem.restoreProjectionMatrix();
        }

        DiffuseLighting.enableGuiDepthLighting();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, BUTTONS_TEXTURE);

        if (isLastKeyFrame()){
            this.drawTexture(matrices, withMinusBgWidth + 208-10, heightMinusBgHeight + 180-10, 0, 90, 32, 30);
            playbackSpeed = 0;
        }

        for(int i = 0; i <= 3; i++){
            int x = withMinusBgWidth + 128 + (18 * (i - 2));
            int y = heightMinusBgHeight + 224 - 18 - 6;

            boolean mouseOver =
                    mouseX >= x &&
                    mouseX <= x + 18 &&
                    mouseY >= y &&
                    mouseY <= y + 18
            ;

            int v = 140;

            if (mouseOver) {
                v += 18;
            }

            if (playbackSpeed == i) {
                v += 18 * 2;
            }

            this.drawTexture(matrices, x, y, i * 18, v, 18, 18);
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices,mouseX,mouseY,delta);
        this.drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    @Override
    protected void drawMouseoverTooltip(MatrixStack matrices, int x, int y) {
        if (tutorial == null) return;

        int withMinusBgWidth = (this.width - this.backgroundWidth) / 2;
        int heightMinusBgHeight = (this.height - this.backgroundHeight) / 2;

        int related_c = 0;
        for (String related_item : tutorial.tut().related_items) {
            Identifier id = new Identifier(related_item);
            if(related_c < 7 && !Objects.equals(tutorialItem.toString(), id.toString())) {
                ItemStack related_item_item_stack = new ItemStack(Registry.ITEM.get(id));
                //MinecraftClient.getInstance().getItemRenderer().renderInGuiWithOverrides(related_item_item_stack, 256 + 2 + (related_c * 18), 190, 0, 0);
                int rx = withMinusBgWidth + 256 + 2 + (related_c * 18);
                int ry = heightMinusBgHeight + 190;
                if (x >= rx && x <= rx + 16 && y >= ry && y <= ry + 16) {
                    this.renderTooltip(matrices, related_item_item_stack, x, y);
                }
                related_c++;
            }
        }
    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY){
        if (tutorial != null){
            textRenderer.draw(matrices, tutorial.tut().display_name, (float)this.titleX, (float)this.titleY, 0x404040);
            if (tutorial.tut().error_message != null && !tutorial.tut().error_message.isEmpty()){
                textRenderer.draw(matrices, tutorial.tut().error_message, 30, 30, 0xFF4040);//TODO - wrap text
            }

            if (!tutorial.tut().related_items.isEmpty()) {
                textRenderer.draw(matrices, "Related items:", 256f + 2f, 180f, 0x404040);

                int related_c = 0;
                for (String related_item : tutorial.tut().related_items) {
                    Identifier id = new Identifier(related_item);
                    if(related_c < 7 && !Objects.equals(tutorialItem.toString(), id.toString())) {
                        ItemStack related_item_item_stack = new ItemStack(Registry.ITEM.get(id));
                        MinecraftClient.getInstance().getItemRenderer()
                                .renderInGuiWithOverrides(related_item_item_stack, 256 + 2 + (related_c * 18), 190, 0, 0);
                        related_c++;
                    }
                }
            }
        }
        else {
            textRenderer.draw(matrices, this.title, (float)this.titleX, (float)this.titleY, 0x404040);
        }

        if (tutorials != null) {
            int c = 0;
            for (Tutorial.Parsed tut : tutorials) {
                this.textRenderer.draw(matrices, tut.tut().display_name, 256 + 2 + 5, c + 15 + 6, 0xc6c6c6);
                c += 20;
            }
        }
    }

    @Override
    public void handledScreenTick() {
        super.handledScreenTick();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(client == null){
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int withMinusBgWidth = (this.width - this.backgroundWidth) / 2;
        int heightMinusBgHeight = (this.height - this.backgroundHeight) / 2;

        if (tutorials != null) {
            int c = 0;
            for (Tutorial.Parsed tut : tutorials){
                if (mouseX >= withMinusBgWidth + 256 + 2 && mouseX <= withMinusBgWidth + 256 + 2 + 108 && mouseY >= heightMinusBgHeight + 15 + c && mouseY <= heightMinusBgHeight + 15 + c + 20){
                    client.player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.3f, 1.0f);
                    setTutorial(tut);
                    break;
                }
                c += 20;
            }

            if (tutorial != null && playing) {
                for (int i = 0; i <= 3; i++) {
                    int x = withMinusBgWidth + 128 + (18 * (i - 2));
                    int y = heightMinusBgHeight + 224 - 18 - 6;

                    boolean mouseOver =
                            mouseX >= x &&
                            mouseX <= x + 18 &&
                            mouseY >= y &&
                            mouseY <= y + 18
                    ;

                    if (mouseOver){
                        if (isLastKeyFrame()){
                            if (i != 0){
                                for (Tutorial.Parsed tut : tutorials) {
                                    if (Objects.equals(tut.tut().display_name, tutorial.tut().display_name)) {
                                        setTutorial(tut);
                                    }
                                }
                                client.player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.3f, 1.0f);
                                playbackSpeed = i;
                            }
                        }
                        else{
                            if(playbackSpeed != i) {
                                client.player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.3f, 1.0f);
                                playbackSpeed = i;
                            }
                        }
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return true;
    }
}
