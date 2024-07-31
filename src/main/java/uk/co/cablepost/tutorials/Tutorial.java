package uk.co.cablepost.tutorials;

import java.util.*;

public class Tutorial {
    public String display_name = "Unnamed Tutorial";

    public int priority = 0;

    public Map<String, TutorialObject> scene_objects = new HashMap<>();
    public List<TutorialInstruction> scene_instructions = new ArrayList<>();

    public List<String> related_items = new ArrayList<>();

    public List<String> show_for_items = new ArrayList<>();

    public int endTime;

    public String error_message = "";
}
