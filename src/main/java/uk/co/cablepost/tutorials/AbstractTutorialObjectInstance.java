package uk.co.cablepost.tutorials;

public class AbstractTutorialObjectInstance {

    public String namespace;
    public String path;

    public Boolean show = null;//used to hide when done

    public Boolean lerp = null;

    public Float x = null;
    public Float y = null;
    public Float z = null;

    public Float rotation_x = null;
    public Float rotation_y = null;
    public Float rotation_z = null;

    public Float scale_x = null;
    public Float scale_y = null;
    public Float scale_z = null;

    public String text = null;//render on a plane?
    public Boolean three_d_sprite = null;

    public String block_state = "";

    public Integer texture_width = null;
    public Integer texture_crop_x = null;
    public Integer texture_height = null;
    public Integer texture_crop_y = null;
    public Integer texture_u = null;
    public Integer texture_v = null;
}
