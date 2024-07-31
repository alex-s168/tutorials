package me.alex_s168.tutorials.api;

import net.minecraft.util.Identifier;

public interface TutorialProvider {
    void getTutorials(TutorialRegister dest);

    interface TutorialRegister {
        void register(Identifier ident, Tutorial tutorial);
        void register(Identifier ident, String tutorial);
    }
}
