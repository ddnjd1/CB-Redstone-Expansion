package Comet_Blaze.neo.cbadd.item;

import org.lwjgl.glfw.GLFW;

public enum KeyBindings {
    W(GLFW.GLFW_KEY_W),
    S(GLFW.GLFW_KEY_S),
    A(GLFW.GLFW_KEY_A),
    D(GLFW.GLFW_KEY_D),
    UP(GLFW.GLFW_KEY_UP),
    DOWN(GLFW.GLFW_KEY_DOWN),
    LEFT(GLFW.GLFW_KEY_LEFT),
    RIGHT(GLFW.GLFW_KEY_RIGHT),
    E(GLFW.GLFW_KEY_E),
    R(GLFW.GLFW_KEY_R),
    T(GLFW.GLFW_KEY_T),
    Y(GLFW.GLFW_KEY_Y),
    Q(GLFW.GLFW_KEY_Q),
    F(GLFW.GLFW_KEY_F),
    G(GLFW.GLFW_KEY_G);

    private final int keyCode;

    KeyBindings(int keyCode) {
        this.keyCode = keyCode;
    }
    public int getCode() {
        return keyCode;
    }

    public static KeyBindings fromCode(int keyCode) {
        for (KeyBindings bind : values()) {
            if (bind.keyCode == keyCode) return bind;
        }
        return null;
    }
//no use ony a api
    public boolean needsInterception() {
        return this == Q || this == F || this == G;
    }
}
