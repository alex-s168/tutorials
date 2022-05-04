package uk.co.cablepost.tutorials;

import java.io.Serializable;

public class TutorialInstruction extends AbstractTutorialObjectInstance /*implements Serializable*/ {
    public int time = 0;
    public Integer relative_time = null;
    public String object_id;

    public Float fov = null;

    public Boolean lerp_fov = null;

    public Boolean force_angle = null;
    public Float angle = null;

    public Boolean lerp_angle = null;
}