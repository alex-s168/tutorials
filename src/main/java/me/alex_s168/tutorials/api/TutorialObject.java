package me.alex_s168.tutorials.api;

import uk.co.cablepost.tutorials.rend.EmptyRender;

public class TutorialObject {
    public TutorialObjectRender renderer = new EmptyRender();
    public Interpolator interpolator = Interpolator.byName("snap");
}
