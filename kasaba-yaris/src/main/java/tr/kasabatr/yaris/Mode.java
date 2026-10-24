/*
 * Decompiled with CFR 0.152.
 */
package tr.kasabatr.yaris;

enum Mode {
    BOAT("tekne", "Tekne Yarışı"),
    HORSE("at", "At Yarışı");

    final String command;
    final String title;

    private Mode(String string2, String string3) {
        this.command = string2;
        this.title = string3;
    }

    static Mode parse(String string) {
        for (Mode mode : Mode.values()) {
            if (!mode.command.equalsIgnoreCase(string) && !mode.name().equalsIgnoreCase(string)) continue;
            return mode;
        }
        return null;
    }
}
