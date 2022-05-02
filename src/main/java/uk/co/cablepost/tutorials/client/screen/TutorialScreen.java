package uk.co.cablepost.tutorials.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.resource.Resource;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.sound.SoundEvents;
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
    public int playbackSpeed = 1;

    private final Map<String, AbstractTutorialObjectInstance> scene_objects = new HashMap<>();

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

    private void addSceneObject(String key, AbstractTutorialObjectInstance obj) {
        if(scene_objects.containsKey(key)){
            tutorial.error_message = "Scene object '" + key + "' already exists or a scene object is trying to be an item and a texture";
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
                tutorial.error_message = "Failed to load tutorial texture: " + e;
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
        playbackSpeed = 1;
        playbackTime = -1000f;
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
                    x.asBlock = entry.getValue().as_block != null ? entry.getValue().as_block : false;
                    x.namespace = entry.getValue().item_mod_name;
                    x.path = entry.getValue().item_name;
                    addSceneObject(entry.getKey(), x);
                }

                if(entry.getValue().texture_path != null){
                    //Texture
                    TutorialTextureObjectInstance x = new TutorialTextureObjectInstance();
                    x.identifier = new Identifier(entry.getValue().texture_mod_name, entry.getValue().texture_path);
                    x.namespace = entry.getValue().texture_mod_name;
                    x.path = entry.getValue().texture_path;
                    addSceneObject(entry.getKey(), x);
                }

                if(entry.getValue().item_name == null && entry.getValue().texture_path == null){
                    //Just text
                    AbstractTutorialObjectInstance x = new AbstractTutorialObjectInstance();
                    addSceneObject(entry.getKey(), x);
                }
            }
        }

        playbackSpeed = 1;
        playbackTime = -8f;
        return tutorial != null;
    }

    float lerp(float a, float b, float f)
    {
        return a + f * (b - a);
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        if(tutorial != null && playing) {
            playbackTime += delta * (float)playbackSpeed;
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

        float sceneRotation = tutorial.force_angle ? tutorial.angle : (((float)mouseX / realWidth * 1000) + 90 + 45);
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(sceneRotation));

        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

        //MinecraftClient.getInstance().getItemRenderer().renderItem(new ItemStack(Items.STONE), ModelTransformation.Mode.NONE, 255, OverlayTexture.DEFAULT_UV, matrices, immediate, 0);

        if(tutorial != null) {

            tutorial.last_fov_instruction = null;
            tutorial.next_fov_instruction = null;
            tutorial.last_angle_instruction = null;
            tutorial.next_angle_instruction = null;

            for (int i = 0; i < tutorial.scene_instructions.size(); i++) {
                TutorialInstruction instruction = tutorial.scene_instructions.get(i);

                if(instruction.fov != null){
                    if(
                            instruction.time <= playbackTime &&
                            (
                                    tutorial.last_fov_instruction == null ||
                                    tutorial.scene_instructions.get(tutorial.last_fov_instruction).time > instruction.time
                            )
                    ){
                        tutorial.last_fov_instruction = i;
                    }

                    if(
                            instruction.time > playbackTime &&
                            (
                                    tutorial.next_fov_instruction == null ||
                                    tutorial.scene_instructions.get(tutorial.next_fov_instruction).time < instruction.time
                            )
                    ){
                        tutorial.next_fov_instruction = i;
                    }
                }

                if(instruction.angle != null){
                    if(
                            instruction.time <= playbackTime &&
                                    (
                                            tutorial.last_angle_instruction == null ||
                                                    tutorial.scene_instructions.get(tutorial.last_angle_instruction).time > instruction.time
                                    )
                    ){
                        tutorial.last_angle_instruction = i;
                    }

                    if(
                            instruction.time > playbackTime &&
                                    (
                                            tutorial.next_angle_instruction == null ||
                                                    tutorial.scene_instructions.get(tutorial.next_angle_instruction).time < instruction.time
                                    )
                    ){
                        tutorial.next_angle_instruction = i;
                    }
                }
            }

            for (Map.Entry<String, AbstractTutorialObjectInstance> sceneItemEntry : scene_objects.entrySet()) {
                AbstractTutorialObjectInstance sceneItem = sceneItemEntry.getValue();
                sceneItem.last_instruction = null;
                sceneItem.next_instruction = null;

                for (int i = 0; i < tutorial.scene_instructions.size(); i++) {
                    TutorialInstruction instruction = tutorial.scene_instructions.get(i);
                    if(Objects.equals(instruction.object_id, sceneItemEntry.getKey())){
                        if(
                                instruction.time <= playbackTime &&
                                (
                                        sceneItem.last_instruction == null ||
                                        tutorial.scene_instructions.get(sceneItem.last_instruction).time > instruction.time
                                )
                        ){
                            sceneItem.last_instruction = i;
                        }

                        if(
                                instruction.time > playbackTime &&
                                (
                                        sceneItem.next_instruction == null ||
                                        tutorial.scene_instructions.get(sceneItem.next_instruction).time < instruction.time
                                )
                        ){
                            sceneItem.next_instruction = i;
                        }
                    }
                }
            }

            for (int i = 0; i < tutorial.scene_instructions.size(); i++) {
                TutorialInstruction instruction = tutorial.scene_instructions.get(i);
                if(instruction.time > lastPlaybackTime && instruction.time <= playbackTime){
                    //apply instruction now
                    if(instruction.object_id != null && scene_objects.containsKey(instruction.object_id)) {
                        AbstractTutorialObjectInstance obj = scene_objects.get(instruction.object_id);

                        obj.show = (instruction.show == null) ? obj.show : instruction.show;
                        obj.lerp = (instruction.lerp == null) ? obj.lerp : instruction.lerp;

                        obj.x = (instruction.x == null) ? obj.x : instruction.x;
                        instruction.x = obj.x;
                        obj.y = (instruction.y == null) ? obj.y : instruction.y;
                        instruction.y = obj.y;
                        obj.z = (instruction.z == null) ? obj.z : instruction.z;
                        instruction.z = obj.z;

                        obj.rotation_x = (instruction.rotation_x == null) ? obj.rotation_x : instruction.rotation_x;
                        instruction.rotation_x = obj.rotation_x;
                        obj.rotation_y = (instruction.rotation_y == null) ? obj.rotation_y : instruction.rotation_y;
                        instruction.rotation_y = obj.rotation_y;
                        obj.rotation_z = (instruction.rotation_z == null) ? obj.rotation_z : instruction.rotation_z;
                        instruction.rotation_z = obj.rotation_z;

                        //obj.rotation_order = instruction.rotation_order;

                        obj.scale_x = (instruction.scale_x == null) ? obj.scale_x : instruction.scale_x;
                        instruction.scale_x = obj.scale_x;
                        obj.scale_y = (instruction.scale_y == null) ? obj.scale_y : instruction.scale_y;
                        instruction.scale_y = obj.scale_y;
                        obj.scale_z = (instruction.scale_z == null) ? obj.scale_z : instruction.scale_z;
                        instruction.scale_z = obj.scale_z;

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
                    else if(instruction.object_id != null){
                        tutorial.error_message = "Could not find scene object with id: " + instruction.object_id;
                    }

                    tutorial.lerp_fov = (instruction.lerp_fov == null) ? tutorial.lerp_fov : instruction.lerp_fov;
                    tutorial.lerp_angle = (instruction.lerp_angle == null) ? tutorial.lerp_angle : instruction.lerp_angle;

                    tutorial.fov = (instruction.fov == null) ? tutorial.fov : instruction.fov;
                    tutorial.angle = (instruction.angle == null) ? tutorial.angle : instruction.angle;
                    tutorial.force_angle = (instruction.force_angle == null) ? tutorial.force_angle : instruction.force_angle;
                }
            }

            if(tutorial.lerp_fov && tutorial.last_fov_instruction != null && tutorial.next_fov_instruction != null){
                TutorialInstruction last_instruction = tutorial.scene_instructions.get(tutorial.last_fov_instruction);
                TutorialInstruction next_instruction = tutorial.scene_instructions.get(tutorial.next_fov_instruction);
                float timeProg01 = (playbackTime - last_instruction.time) / (next_instruction.time - last_instruction.time);

                tutorial.fov = lerp(
                        last_instruction.fov,
                        next_instruction.fov,
                        timeProg01
                );
            }

            if(tutorial.lerp_angle && tutorial.last_angle_instruction != null && tutorial.next_angle_instruction != null){
                TutorialInstruction last_instruction = tutorial.scene_instructions.get(tutorial.last_angle_instruction);
                TutorialInstruction next_instruction = tutorial.scene_instructions.get(tutorial.next_angle_instruction);
                float timeProg01 = (playbackTime - last_instruction.time) / (next_instruction.time - last_instruction.time);

                tutorial.angle = lerp(
                        last_instruction.angle,
                        next_instruction.angle,
                        timeProg01
                );
            }

            for (Map.Entry<String, AbstractTutorialObjectInstance> sceneItemEntry : scene_objects.entrySet()) {

                AbstractTutorialObjectInstance sceneItemEntryValue = sceneItemEntry.getValue();

                if(sceneItemEntryValue.lerp && sceneItemEntryValue.last_instruction != null && sceneItemEntryValue.next_instruction != null){

                    TutorialInstruction last_instruction = tutorial.scene_instructions.get(sceneItemEntryValue.last_instruction);
                    TutorialInstruction next_instruction = tutorial.scene_instructions.get(sceneItemEntryValue.next_instruction);

                    float timeProg01 = (playbackTime - last_instruction.time) / (next_instruction.time - last_instruction.time);

                    if(next_instruction.x != null) {
                        sceneItemEntryValue.x = lerp(
                                last_instruction.x,
                                next_instruction.x,
                                timeProg01
                        );
                    }

                    if(next_instruction.y != null) {
                        sceneItemEntryValue.y = lerp(
                                last_instruction.y,
                                next_instruction.y,
                                timeProg01
                        );
                    }

                    if(next_instruction.z != null) {
                        sceneItemEntryValue.z = lerp(
                                last_instruction.z,
                                next_instruction.z,
                                timeProg01
                        );
                    }

                    if(next_instruction.rotation_x != null) {
                        sceneItemEntryValue.rotation_x = lerp(
                                last_instruction.rotation_x,
                                next_instruction.rotation_x,
                                timeProg01
                        );
                    }

                    if(next_instruction.rotation_y != null) {
                        sceneItemEntryValue.rotation_y = lerp(
                                last_instruction.rotation_y,
                                next_instruction.rotation_y,
                                timeProg01
                        );
                    }

                    if(next_instruction.rotation_z != null) {
                        sceneItemEntryValue.rotation_z = lerp(
                                last_instruction.rotation_z,
                                next_instruction.rotation_z,
                                timeProg01
                        );
                    }

                    if(next_instruction.scale_x != null) {
                        sceneItemEntryValue.scale_x = lerp(
                                last_instruction.scale_x,
                                next_instruction.scale_x,
                                timeProg01
                        );
                    }

                    if(next_instruction.scale_y != null) {
                        sceneItemEntryValue.scale_y = lerp(
                                last_instruction.scale_y,
                                next_instruction.scale_y,
                                timeProg01
                        );
                    }

                    if(next_instruction.scale_z != null) {
                        sceneItemEntryValue.scale_z = lerp(
                                last_instruction.scale_z,
                                next_instruction.scale_z,
                                timeProg01
                        );
                    }
                }

                if (sceneItemEntryValue instanceof TutorialTextureObjectInstance sceneItem) {

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

                AbstractTutorialObjectInstance sceneItemEntryValue = sceneItemEntry.getValue();

                if(sceneItemEntryValue instanceof TutorialItemObjectInstance sceneItem) {

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


                    if(sceneItem.asBlock) {
                        //Identifier id = Registry.ITEM.getId(sceneItem.itemStack.getItem());
                        Identifier id = new Identifier(sceneItem.namespace, sceneItem.path);
                        String block_state_string = id + "[" + (sceneItem.block_state != null ? sceneItem.block_state : "") + "]";
                        BlockArgumentParser blockArgumentParser = new BlockArgumentParser(new StringReader(block_state_string), true);
                        try {
                            blockArgumentParser.parseBlockId();
                            blockArgumentParser.parseBlockProperties();
                        } catch (CommandSyntaxException e) {
                            System.out.println("Tutorial error: " + e);
                        }
                        BlockState block_state = blockArgumentParser.getBlockState();
                        if(block_state != null) {
                            matrices.translate(-0.5f, 0, -0.5f);
                            MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(block_state, matrices, immediate, 255, OverlayTexture.DEFAULT_UV);
                            matrices.translate(0.5f, 0, 0.5f);
                        }
                        else{
                            tutorial.error_message = "'" + block_state_string + "' was invalid for '" + sceneItemEntry.getKey() + "'";
                        }
                    }
                    else {
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

                    //if (sceneItem.rotation_order.equalsIgnoreCase("xyz")) {
                        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(-sceneItem.rotation_z));
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-sceneItem.rotation_y));
                        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-sceneItem.rotation_x));
                    //}
                    //TODO - the other other 11: XYX, XZX, YXY, YZY, ZXZ, ZYZ, XYZ, XZY, YZX, YXZ, ZXY, ZYX

                    matrices.scale(1f / sceneItem.scale_x, 1f / sceneItem.scale_y, 1f / sceneItem.scale_z);

                    matrices.translate(-sceneItem.x, -sceneItem.y, -sceneItem.z);
                }

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

        //--- Draw an item as prevents textures going over the play buttons?
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
        //---

        immediate.draw();
        matrices.pop();
        RenderSystem.viewport(0, 0, this.client.getWindow().getFramebufferWidth(), this.client.getWindow().getFramebufferHeight());
        RenderSystem.restoreProjectionMatrix();
        DiffuseLighting.enableGuiDepthLighting();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        RenderSystem.setShaderTexture(0, BUTTONS_TEXTURE);

        if(tutorial != null && playbackTime >= tutorial.endTime + 20){
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

        if(playing) {
            lastPlaybackTime = playbackTime;
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
        int withMinusBgWidth = (this.width - this.backgroundWidth) / 2;
        int heightMinusBgHeight = (this.height - this.backgroundHeight) / 2;

        int related_c = 0;
        for (String related_item : tutorial.related_items) {
            if(related_c < 7) {
                ItemStack related_item_item_stack = new ItemStack(Registry.ITEM.get(new Identifier(related_item)));
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
        if(tutorial != null){
            textRenderer.draw(matrices, tutorial.display_name, (float)this.titleX, (float)this.titleY, 0x404040);
            if(tutorial.error_message != null && !tutorial.error_message.equals("")){
                textRenderer.draw(matrices, tutorial.error_message, 30, 30, 0xFF4040);//TODO - wrap text
            }
        }
        else{
            textRenderer.draw(matrices, this.title, (float)this.titleX, (float)this.titleY, 0x404040);
        }

        if(tutorial.related_items.size() > 0){
            textRenderer.draw(matrices, "Related items:", 256f + 2f, 180f, 0x404040);
        }

        int related_c = 0;
        for (String related_item : tutorial.related_items) {
            if(related_c < 7) {
                ItemStack related_item_item_stack = new ItemStack(Registry.ITEM.get(new Identifier(related_item)));
                MinecraftClient.getInstance().getItemRenderer().renderInGuiWithOverrides(related_item_item_stack, 256 + 2 + (related_c * 18), 190, 0, 0);
                related_c++;
            }
        }

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
        if(client == null){
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int withMinusBgWidth = (this.width - this.backgroundWidth) / 2;
        int heightMinusBgHeight = (this.height - this.backgroundHeight) / 2;

        if(tutorials != null) {
            int c = 0;
            //for (Map.Entry<String, Tutorial> entry : tutorials.entrySet()) {
            for(int i = 0; i < tutorialOrder.length; i++){
                if(mouseX >= withMinusBgWidth + 256 + 2 && mouseX <= withMinusBgWidth + 256 + 2 + 108 && mouseY >= heightMinusBgHeight + 15 + c && mouseY <= heightMinusBgHeight + 15 + c + 20){
                    try {
                        client.player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.3f, 1.0f);
                        setTutorial(tutorialOrder[i]);
                    } catch (Exception e) {
                        if (tutorial == null) {
                            tutorial = new Tutorial();
                            tutorial.display_name = "Tutorial that failed to load :(";
                        }
                        tutorial.error_message = "Error loading tutorial: " + e;
                    }
                }
                c += 20;
            }

            if(tutorial != null && playing){
                for(int i = 0; i <= 3; i++){
                    int x = withMinusBgWidth + 128 + (18 * (i - 2));
                    int y = heightMinusBgHeight + 224 - 18 - 6;

                    boolean mouseOver =
                            mouseX >= x &&
                            mouseX <= x + 18 &&
                            mouseY >= y &&
                            mouseY <= y + 18
                    ;

                    if(mouseOver){
                        if(playbackTime >= tutorial.endTime + 20){
                            if(i != 0){
                                for (String s : tutorialOrder) {
                                    Tutorial tut = tutorials.get(s);
                                    if (Objects.equals(tut.display_name, tutorial.display_name)) {
                                        setTutorial(s);
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
