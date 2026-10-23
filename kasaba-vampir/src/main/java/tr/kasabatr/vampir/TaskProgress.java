package tr.kasabatr.vampir;

/** Public activity includes fake work. Only GameRoom's real count can end the game. */
final class TaskProgress {
    private TaskProgress() {}

    static double visible(int real, int fake, int goal) {
        if (goal <= 0) return 0;
        // Do not announce 100% while genuine work (or the first night) is still pending.
        return Math.min(.99, ((long)Math.max(0, real) + Math.max(0, fake)) / (double)goal);
    }
}
