/*
 * Decompiled with CFR 0.152.
 */
package tr.kasabatr.yaris;

final class LapProgress {
    final int checkpointCount;
    final int totalLaps;
    private int nextCheckpoint;
    private int lap = 1;
    private boolean finished;

    LapProgress(int n, int n2) {
        if (n < 1 || n2 < 1) {
            throw new IllegalArgumentException("Boş parkur");
        }
        this.checkpointCount = n;
        this.totalLaps = n2;
    }

    int nextCheckpoint() {
        return this.nextCheckpoint;
    }

    int lap() {
        return this.lap;
    }

    boolean complete() {
        return this.finished;
    }

    boolean recordCheckpoint(int n) {
        if (this.finished || n != this.nextCheckpoint || n >= this.checkpointCount) {
            return false;
        }
        ++this.nextCheckpoint;
        return true;
    }

    Finish recordFinish() {
        if (this.finished || this.nextCheckpoint != this.checkpointCount) {
            return Finish.NOT_READY;
        }
        if (this.lap == this.totalLaps) {
            this.finished = true;
            return Finish.RACE_FINISHED;
        }
        ++this.lap;
        this.nextCheckpoint = 0;
        return Finish.NEXT_LAP;
    }

    static enum Finish {
        NOT_READY,
        NEXT_LAP,
        RACE_FINISHED;

    }
}
