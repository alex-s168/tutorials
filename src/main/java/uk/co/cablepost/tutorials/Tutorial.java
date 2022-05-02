package uk.co.cablepost.tutorials;

import java.util.*;

public class Tutorial {
    public String display_name = "Unnamed Tutorial";

    public int priority = 0;

    public Map<String, TutorialObject> scene_objects = new HashMap<>();
    public List<TutorialInstruction> scene_instructions = new ArrayList<>();

    public List<String> related_items = new ArrayList<>();

    public int endTime;

    public String error_message = "";

    public Float fov = 66.0f;

    public Boolean lerp_fov = false;

    public Integer last_fov_instruction = null;
    public Integer next_fov_instruction = null;

    public Boolean force_angle = false;
    public Float angle = 0f;

    public Boolean lerp_angle = false;

    public Integer last_angle_instruction = null;
    public Integer next_angle_instruction = null;
}
