package uk.co.cablepost.tutorials;

import java.util.*;

public class Tutorial {
    public String display_name = "Unnamed Tutorial";

    public Float fov = 66.0f;
    public Float force_angle = null;

    public Map<String, TutorialObject> scene_objects = new HashMap<>();
    public List<TutorialInstruction> scene_instructions = new ArrayList<>();

    public int endTime;
}
