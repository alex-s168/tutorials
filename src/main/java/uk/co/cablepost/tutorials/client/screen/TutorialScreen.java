package uk.co.cablepost.tutorials.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.registry.Registry;
import uk.co.cablepost.tutorials.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TutorialScreen extends HandledScreen<TutorialScreenHandler> {

    public static final Identifier BACKGROUND_TEXTURE = new Identifier(Tutorials.MOD_ID, "textures/gui/tutorials/tutorial.png");
    public static final Identifier BACKGROUND_TEXTURE_TUT_COMPLETE = new Identifier(Tutorials.MOD_ID, "textures/gui/tutorials/tutorial_complete.png");
    public static final Identifier BUTTONS_TEXTURE = new Identifier(Tutorials.MOD_ID, "textures/gui/tutorials/tutorial_buttons.png");

    private String[] tutorialOrder = null;
    private Map<String, Tutorial> tutorials = null;
    private Tutorial tutorial = null;

    private float playbackTime = 0f;
    private float lastPlaybackTime = 0f;

    public boolean playing = true;

    private Map<String, AbstractTutorialObjectInstance> scene_objects = new HashMap<>();

    public TutorialScreen(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(new TutorialScreenHandler(), inventory, title);
    }

    public TutorialScreen(PlayerEntity player) {
        this(player.playerScreenHandler, player.getInventory(), new TranslatableText("container.tutorial"));
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

    private void addSceneObject(String key, AbstractTutorialObjectInstance obj) {
        if(scene_objects.containsKey(key)){
            //TODO - add scene error
            //Scene object already exists or a scene object is trying to be an item and a texture
            return;
        }

        obj.show = false;
        obj.lerp = false;

        obj.x = 0f;
        obj.y = 0f;
        obj.z = 0f;

        obj.rotation_x = 0f;
        obj.rotation_y = 0f;
        obj.rotation_z = 0f;

        obj.scale_x = 1f;
        obj.scale_y = 1f;
        obj.scale_z = 1f;

        obj.text = "";
        obj.three_d_sprite = false;

        if(obj instanceof TutorialTextureObjectInstance t_obj) {
            assert client != null;
            try {
                Resource resource = client.getResourceManager().getResource(t_obj.identifier);
                NativeImage nativeImage = NativeImage.read(resource.getInputStream());

                obj.texture_width = nativeImage.getWidth();
                obj.texture_crop_x = obj.texture_width;
                obj.texture_height = nativeImage.getHeight();
                obj.texture_crop_y = obj.texture_height;
                obj.texture_u = 0;
                obj.texture_v = 0;
            } catch (IOException e){
                //TODO - add to tutorial errors
            }
        }

        scene_objects.put(key, obj);
    }

    public boolean setTutorials(Identifier identifier) {
        if(Tutorials.tutorials.containsKey(identifier)) {
            tutorials = Tutorials.tutorials.get(identifier);
            setTutorialOrder();
            //return setTutorial(tutorials.entrySet().iterator().next().getKey());
            return setTutorial(tutorialOrder[0]);
        }
        return false;
    }

    public void setTutorialOrder(){
        tutorialOrder = new String[tutorials.size()];
        int added = 0;
        while(added < tutorials.size()) {
            String highest = null;
            for (Map.Entry<String, Tutorial> entry : tutorials.entrySet()) {
                if(Arrays.stream(tutorialOrder).noneMatch(x -> Objects.equals(x, entry.getKey()))){//not already in tutorialOrder array
                    if(highest == null || tutorials.get(highest).priority < entry.getValue().priority){//highest of left to add
                        highest = entry.getKey();
                    }
                }
            }
            tutorialOrder[added] = highest;
            added++;
        }
    }

    public boolean setTutorial(String tut) {
        playbackTime = -1f;
        tutorial = null;

        if(tutorials.containsKey(tut)) {
            tutorial = tutorials.get(tut);

            scene_objects.clear();

            int lastTime = 0;
            for (TutorialInstruction entry : tutorial.scene_instructions) {
                if(entry.relative_time != null){
                    entry.time = lastTime + entry.relative_time;
                }
                lastTime = entry.time;
            }

            tutorial.endTime = lastTime;

            for (Map.Entry<String, TutorialObject> entry : tutorial.scene_objects.entrySet()) {

                if(entry.getValue().item_name != null){
                    //Item
                    TutorialItemObjectInstance x = new TutorialItemObjectInstance();
                    x.itemStack = new ItemStack(Registry.ITEM.get(new Identifier(entry.getValue().item_mod_name, entry.getValue().item_name)));
                    addSceneObject(entry.getKey(), x);
                }

                if(entry.getValue().texture_path != null){
                    //Texture
                    TutorialTextureObjectInstance x = new TutorialTextureObjectInstance();
                    x.identifier = new Identifier(entry.getValue().texture_mod_name, entry.getValue().texture_path);
                    addSceneObject(entry.getKey(), x);
                }

                if(entry.getValue().item_name == null && entry.getValue().texture_path == null){
                    //Just text
                    AbstractTutorialObjectInstance x = new AbstractTutorialObjectInstance();
                    addSceneObject(entry.getKey(), x);
                }
            }
        }
        return tutorial != null;
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        if(tutorial != null && playing) {
            playbackTime += delta;
        }

        DiffuseLighting.disableGuiDepthLighting();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        if(tutorial != null && playbackTime >= tutorial.endTime + 20){
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

        if(tutorials != null) {
            int c = 0;
            //for (Map.Entry<String, Tutorial> entry : tutorials.entrySet()) {
            for (String s : tutorialOrder) {
                //Tutorial tut = entry.getValue();
                Tutorial tut = tutorials.get(s);
                boolean mouseOver = mouseX >= withMinusBgWidth + 256 + 2 && mouseX <= withMinusBgWidth + 256 + 2 + 108 && mouseY >= heightMinusBgHeight + 15 + c && mouseY <= heightMinusBgHeight + 15 + c + 20;
                if (Objects.equals(tut.display_name, tutorial.display_name)) {
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

        //RenderSystem.viewport((this.width - backgroundWidth) / 2 * scaleFactor, (this.height - backgroundHeight) / 2 * scaleFactor, 256 * scaleFactor, backgroundHeight * scaleFactor);
        RenderSystem.viewport(((this.width - backgroundWidth) / 2 + 15) * scaleFactor, ((this.height - backgroundHeight) / 2 + 15) * scaleFactor, (256 - 30) * scaleFactor, (backgroundHeight - 30) * scaleFactor);
        Matrix4f matrix4f = Matrix4f.translate(0f, -0.25f, -1f);
        //matrix4f.multiply(Matrix4f.viewboxMatrix(66.0f, (float)backgroundHeight / 256f, 0.01f, 1000.0f));
        matrix4f.multiply(Matrix4f.viewboxMatrix(tutorial.fov, ((float)backgroundHeight - 30f) / (256f - 30f), 0.01f, 1000.0f));
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(matrix4f);
        matrices.push();
        MatrixStack.Entry entry = matrices.peek();
        entry.getPositionMatrix().loadIdentity();
        entry.getNormalMatrix().loadIdentity();

        matrices.translate(0.0, 0.0f, 1999.84f);
        matrices.scale(0.2f, 0.24f, 0.2f);

        matrices.translate(-0f, -2f, -10f);

        float sceneRotation = (tutorial.force_angle != null) ? tutorial.force_angle : (((float)mouseX / realWidth * 1000) + 90 + 45);
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(sceneRotation));

        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

        //MinecraftClient.getInstance().getItemRenderer().renderItem(new ItemStack(Items.STONE), ModelTransformation.Mode.NONE, 255, OverlayTexture.DEFAULT_UV, matrices, immediate, 0);

        if(tutorial != null) {
            for (int i = 0; i < tutorial.scene_instructions.size(); i++) {
                TutorialInstruction instruction = tutorial.scene_instructions.get(i);
                if(instruction.time > lastPlaybackTime && instruction.time <= playbackTime){
                    //apply instruction now
                    if(scene_objects.containsKey(instruction.object_id)) {
                        AbstractTutorialObjectInstance obj = scene_objects.get(instruction.object_id);

                        obj.show = (instruction.show == null) ? obj.show : instruction.show;
                        obj.lerp = (instruction.lerp == null) ? obj.lerp : instruction.lerp;

                        obj.x = (instruction.x == null) ? obj.x : instruction.x;
                        obj.y = (instruction.y == null) ? obj.y : instruction.y;
                        obj.z = (instruction.z == null) ? obj.z : instruction.z;

                        obj.rotation_x = (instruction.rotation_x == null) ? obj.rotation_x : instruction.rotation_x;
                        obj.rotation_y = (instruction.rotation_y == null) ? obj.rotation_y : instruction.rotation_y;
                        obj.rotation_z = (instruction.rotation_z == null) ? obj.rotation_z : instruction.rotation_z;

                        //obj.rotation_order = instruction.rotation_order;

                        obj.scale_x = (instruction.scale_x == null) ? obj.scale_x : instruction.scale_x;
                        obj.scale_y = (instruction.scale_y == null) ? obj.scale_y : instruction.scale_y;
                        obj.scale_z = (instruction.scale_z == null) ? obj.scale_z : instruction.scale_z;

                        obj.text = (instruction.text == null) ? obj.text : instruction.text;
                        obj.three_d_sprite = (instruction.three_d_sprite == null) ? obj.three_d_sprite : instruction.three_d_sprite;

                        obj.block_state = (instruction.block_state == null) ? obj.block_state : instruction.block_state;

                        obj.texture_width = (instruction.texture_width == null) ? obj.texture_width : instruction.texture_width;
                        obj.texture_crop_x = (instruction.texture_crop_x == null) ? obj.texture_crop_x : instruction.texture_crop_x;
                        obj.texture_height = (instruction.texture_height == null) ? obj.texture_height : instruction.texture_height;
                        obj.texture_crop_y = (instruction.texture_crop_y == null) ? obj.texture_crop_y : instruction.texture_crop_y;
                        obj.texture_u = (instruction.texture_u == null) ? obj.texture_u : instruction.texture_u;
                        obj.texture_v = (instruction.texture_v == null) ? obj.texture_v : instruction.texture_v;
                    }
                    else{
                        //TODO - add tutorial error: ("Could not find scene object with id: " + instruction.object_id);
                    }
                }
            }

            for (Map.Entry<String, AbstractTutorialObjectInstance> sceneItemEntry : scene_objects.entrySet()) {
                if (sceneItemEntry.getValue() instanceof TutorialTextureObjectInstance sceneItem) {

                    if (!sceneItem.show) {
                        continue;
                    }

                    RenderSystem.setShaderTexture(0, sceneItem.identifier);

                    matrices.translate(sceneItem.x, sceneItem.y, sceneItem.z);

                    matrices.scale(0.25f * sceneItem.scale_x, 0.25f * sceneItem.scale_y, 0.25f * sceneItem.scale_z);

                    if(sceneItemEntry.getValue().three_d_sprite) {
                        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-sceneItem.rotation_x));
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-sceneItem.rotation_y));
                        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(-sceneItem.rotation_z));

                        matrices.translate(sceneItem.texture_crop_x /2f, -sceneItem.texture_crop_y /2f, 0f);

                        matrices.translate(-sceneItem.texture_crop_x, 0f, 0f);
                        DrawableHelper.drawTexture(
                                matrices,
                                0,//x
                                0,//y
                                0,//z
                                0,//u
                                0,//v
                                sceneItem.texture_crop_x,//w
                                sceneItem.texture_crop_y,//h
                                sceneItem.texture_width,//tw
                                sceneItem.texture_height//th
                        );
                        matrices.translate(sceneItem.texture_crop_x, 0f, 0f);
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180f));
                        DrawableHelper.drawTexture(
                                matrices,
                                0,//x
                                0,//y
                                0,//z
                                0,//u
                                0,//v
                                sceneItem.texture_crop_x,//w
                                sceneItem.texture_crop_y,//h
                                sceneItem.texture_width,//tw
                                sceneItem.texture_height//th
                        );
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180f));

                        matrices.translate(-sceneItem.texture_crop_x /2f, sceneItem.texture_crop_y /2f, 0f);

                        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(sceneItem.rotation_z));
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(sceneItem.rotation_y));
                        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(sceneItem.rotation_x));
                    }
                    else{
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-sceneRotation + 180f));
                        matrices.translate(-sceneItem.texture_crop_x /2f, -sceneItem.texture_crop_y /2f, 0f);
                        DrawableHelper.drawTexture(
                                matrices,
                                0,//x
                                0,//y
                                0,//z
                                0,//u
                                0,//v
                                sceneItem.texture_crop_x,//w
                                sceneItem.texture_crop_y,//h
                                sceneItem.texture_width,//tw
                                sceneItem.texture_height//th
                        );
                        matrices.translate(sceneItem.texture_crop_x /2f, sceneItem.texture_crop_y /2f, 0f);
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(+sceneRotation - 180f));
                    }

                    matrices.scale(4f / sceneItem.scale_x, 4f / sceneItem.scale_y, 4f / sceneItem.scale_z);

                    matrices.translate(-sceneItem.x, -sceneItem.y, -sceneItem.z);
                }
            }

            for (Map.Entry<String, AbstractTutorialObjectInstance> sceneItemEntry : scene_objects.entrySet()) {

                if(sceneItemEntry.getValue() instanceof TutorialItemObjectInstance sceneItem) {

                    if (!sceneItem.show) {
                        continue;
                    }

                    matrices.translate(sceneItem.x, sceneItem.y, sceneItem.z);

                    matrices.scale(sceneItem.scale_x, sceneItem.scale_y, sceneItem.scale_z);

                    //if (sceneItem.rotation_order.equalsIgnoreCase("xyz")) {
                        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(sceneItem.rotation_x));
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(sceneItem.rotation_y));
                        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(sceneItem.rotation_z));
                    //}
                    //TODO - the other other 11: XYX, XZX, YXY, YZY, ZXZ, ZYZ, XYZ, XZY, YZX, YXZ, ZXY, ZYX


                    if(Objects.equals(sceneItem.block_state, "")) {
                        MinecraftClient.getInstance().getItemRenderer().renderItem(
                                sceneItem.itemStack,
                                ModelTransformation.Mode.NONE,
                                255,
                                OverlayTexture.DEFAULT_UV,
                                matrices,
                                immediate,
                                0
                        );
                    }
                    else {
                        try {
                            NbtCompound blockStateNbt = NbtHelper.fromNbtProviderString(sceneItem.block_state);
                            BlockState blockState = NbtHelper.toBlockState(blockStateNbt);//Not sure how this works!
                            MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(blockState, matrices, immediate, 255, OverlayTexture.DEFAULT_UV);
                        } catch (CommandSyntaxException e) {
                            CommandSyntaxException e2 = e;//here so can break on it and see error
                            //TODO - add error to scene: Invalid block state
                        }
                    }

                    //if (sceneItem.rotation_order.equalsIgnoreCase("xyz")) {
                        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(-sceneItem.rotation_z));
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-sceneItem.rotation_y));
                        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-sceneItem.rotation_x));
                    //}
                    //TODO - the other other 11: XYX, XZX, YXY, YZY, ZXZ, ZYZ, XYZ, XZY, YZX, YXZ, ZXY, ZYX

                    matrices.scale(1f / sceneItem.scale_x, 1f / sceneItem.scale_y, 1f / sceneItem.scale_z);

                    matrices.translate(-sceneItem.x, -sceneItem.y, -sceneItem.z);
                }

                AbstractTutorialObjectInstance sceneItemEntryValue = sceneItemEntry.getValue();

                String text = sceneItemEntryValue.text;
                if(text != null && !text.equals("")) {
                    int bgColor = (int)(0.25f * 255.0f) << 24;
                    Matrix4f matrix4fText = matrices.peek().getPositionMatrix();
                    matrices.translate(sceneItemEntryValue.x, sceneItemEntryValue.y, sceneItemEntryValue.z);
                    matrices.scale(-0.04f, -0.04f, 0.04f);



                    if(sceneItemEntryValue.three_d_sprite){
                        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(sceneItemEntryValue.rotation_x));
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(sceneItemEntryValue.rotation_y));
                        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(sceneItemEntryValue.rotation_z));

                        matrices.scale(sceneItemEntryValue.scale_x, sceneItemEntryValue.scale_y, sceneItemEntryValue.scale_z);

                        textRenderer.draw(text, -textRenderer.getWidth(text) / 2f, 0f, 0xAAFFFFFF, false, matrix4fText, immediate, false, bgColor, 255);
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180f));
                        textRenderer.draw(text, -textRenderer.getWidth(text) / 2f, 0f, 0xAAFFFFFF, false, matrix4fText, immediate, false, bgColor, 255);
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180f));

                        matrices.scale(1f / sceneItemEntryValue.scale_x, 1f / sceneItemEntryValue.scale_y, 1f / sceneItemEntryValue.scale_z);

                        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(-sceneItemEntryValue.rotation_z));
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-sceneItemEntryValue.rotation_y));
                        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-sceneItemEntryValue.rotation_x));
                    }
                    else{
                        matrices.scale(sceneItemEntryValue.scale_x, sceneItemEntryValue.scale_y, sceneItemEntryValue.scale_z);

                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(sceneRotation + 180f));
                        textRenderer.draw(text, -textRenderer.getWidth(text) / 2f, 0f, 0xAAFFFFFF, false, matrix4fText, immediate, false, bgColor, 255);
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-sceneRotation - 180f));

                        matrices.scale(1f / sceneItemEntryValue.scale_x, 1f / sceneItemEntryValue.scale_y, 1f / sceneItemEntryValue.scale_z);
                    }

                    matrices.scale(1f/-0.04f, 1f/-0.04f, 1f/0.04f);
                    matrices.translate(-sceneItemEntryValue.x, -sceneItemEntryValue.y, -sceneItemEntryValue.z);
                }
            }
        }

        immediate.draw();
        matrices.pop();
        RenderSystem.viewport(0, 0, this.client.getWindow().getFramebufferWidth(), this.client.getWindow().getFramebufferHeight());
        RenderSystem.restoreProjectionMatrix();
        DiffuseLighting.enableGuiDepthLighting();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        if(tutorial != null && playbackTime >= tutorial.endTime + 20){
            RenderSystem.setShaderTexture(0, BUTTONS_TEXTURE);
            //this.drawTexture(matrices, withMinusBgWidth + 208, heightMinusBgHeight + 180, 0, 69, 20, 20);
            this.drawTexture(matrices, withMinusBgWidth + 208-10, heightMinusBgHeight + 180-10, 0, 90, 32, 30);
        }

        if(playing) {
            lastPlaybackTime = playbackTime;
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices,mouseX,mouseY,delta);
    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY){
        if(tutorial != null){
            textRenderer.draw(matrices, tutorial.display_name, (float)this.titleX, (float)this.titleY, 0x404040);
        }
        else{
            textRenderer.draw(matrices, this.title, (float)this.titleX, (float)this.titleY, 0x404040);
        }

        //MinecraftClient.getInstance().getItemRenderer().renderInGuiWithOverrides(new ItemStack(Items.DEAD_BUBBLE_CORAL), 20, 20, 0, 0);

        if(tutorials != null) {
            int c = 0;
            //for (Map.Entry<String, Tutorial> entry : tutorials.entrySet()) {
            for(int i = 0; i < tutorialOrder.length; i++){
                //Tutorial tut = entry.getValue();
                Tutorial tut = tutorials.get(tutorialOrder[i]);
                this.textRenderer.draw(matrices, tut.display_name, 256 + 2 + 5, c + 15 + 6, 0xc6c6c6);
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
        int withMinusBgWidth = (this.width - this.backgroundWidth) / 2;
        int heightMinusBgHeight = (this.height - this.backgroundHeight) / 2;

        if(tutorials != null) {
            int c = 0;
            //for (Map.Entry<String, Tutorial> entry : tutorials.entrySet()) {
            for(int i = 0; i < tutorialOrder.length; i++){
                if(mouseX >= withMinusBgWidth + 256 + 2 && mouseX <= withMinusBgWidth + 256 + 2 + 108 && mouseY >= heightMinusBgHeight + 15 + c && mouseY <= heightMinusBgHeight + 15 + c + 20){
                    try {
                        setTutorial(tutorialOrder[i]);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                c += 20;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
