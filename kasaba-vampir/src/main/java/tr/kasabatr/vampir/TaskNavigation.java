package tr.kasabatr.vampir;

/** Coordinates of the 144-block atlas, and directions relative to the viewer. */
final class TaskNavigation {
    private TaskNavigation() {}
    static int pixel(double coordinate) { return Math.clamp((int)Math.floor((coordinate+72)*128/144),0,127); }
    static int world(int pixel) { return (int)Math.floor(-72+(pixel+.5)*144/128); }
    static byte cursor(double coordinate) { return (byte)Math.clamp((int)Math.round((coordinate+72)*256/144-128),-127,127); }
    static int station(GameRoom.Task task) { return task.kind==2&&task.carrying&&!task.completed?task.destination():task.station; }
    static String arrow(float yaw,double dx,double dz) {
        double target=Math.toDegrees(Math.atan2(-dx,dz));
        return new String[]{"↑","↗","→","↘","↓","↙","←","↖"}[Math.floorMod((int)Math.round((target-yaw)/45),8)];
    }
}
