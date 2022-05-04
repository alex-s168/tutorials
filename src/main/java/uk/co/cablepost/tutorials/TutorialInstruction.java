package uk.co.cablepost.tutorials;

import java.io.Serializable;

public class TutorialInstruction extends AbstractTutorialObjectInstance /*implements Serializable*/ {
    public int time = 0;
    public Integer relative_time = null;
    public String object_id;
}