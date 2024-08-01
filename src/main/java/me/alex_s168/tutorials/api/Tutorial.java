package me.alex_s168.tutorials.api;

import org.jetbrains.annotations.NotNull;
import uk.co.cablepost.tutorials.util.JsonUtil;

import java.util.*;

public class Tutorial {
    public String display_name = "Unnamed Tutorial";

    public int priority = 0;

    public Map<String, TutorialObject> scene_objects = new HashMap<>();
    public List<TutorialInstruction> scene_instructions = new ArrayList<>();

    public List<String> related_items = new ArrayList<>();

    public List<String> show_for_items = new ArrayList<>();

    public String error_message = "";

    public Integer end_time;

    public record Parsed(
        Tutorial tut,
        TutorialInstruction.Keyframe[] keyframes
    ) {}

    private void verify() throws Exception {
        if (end_time == null) {
            throw new Exception("\"end_time\" not set!");
        }

        for (var item : related_items) {
            if (item == null) throw new Exception("Invalid item in \"related_items\"");
        }

        for (var item : show_for_items) {
            if (item == null) throw new Exception("Invalid item in \"show_for_items\"");
        }

        for (var item : scene_instructions) {
            if (item == null) throw new Exception("Invalid instruction in \"scene_instructions\"");
        }

        for (var item : scene_objects.values()) {
            if (item == null) throw new Exception("Invalid object in \"scene_objects\"");
        }
    }

    public Parsed finishParse() throws Exception {
        verify();

        scene_objects.put("camera", new TutorialObject());
        var camBaseData = new TutorialInstruction.BaseData();
        camBaseData.fov = 66f;
        camBaseData.pos = new float[]{ 0f, 2f, 10f };
        scene_instructions.add(new TutorialInstruction(
                "camera",
                0,
                camBaseData,
                JsonUtil.empty()
        ));

        scene_instructions.sort(Comparator.comparingInt((x) -> x.time));

        KeyframeData last = null;

        var keyframes = new TutorialInstruction.Keyframe[scene_instructions.size()];
        for (int k = 0; k < keyframes.length; k ++) {
            TutorialInstruction instr = scene_instructions.get(k);
            TutorialObject obj = getSceneObject(instr.object_id);

            last = obj.renderer.parseKeyframe(last, instr.keyframeData);
            keyframes[k] = new TutorialInstruction.Keyframe(instr.baseData, last);
        }

        return new Parsed(this, keyframes);
    }

    public @NotNull TutorialObject getSceneObject(@NotNull String name) {
        var obj = scene_objects.get(name);
        if (obj == null) {
            var valid = Arrays.toString(scene_objects.keySet().toArray());
            throw new RuntimeException("Unknown scene object " + name + "! Valid scene objects: " + valid);
        }
        return obj;
    }
}
