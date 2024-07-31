package uk.co.cablepost.tutorials;

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

    public record Parsed(
        Tutorial tut,
        TutorialInstruction.Keyframe[] keyframes
    ) {}

    public Parsed finishParse() throws Exception {
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
            TutorialObject obj = scene_objects.get(instr.object_id);

            last = obj.renderer.parseKeyframe(last, instr.keyframeData);
            keyframes[k] = new TutorialInstruction.Keyframe(instr.baseData, last);
        }

        return new Parsed(this, keyframes);
    }
}
