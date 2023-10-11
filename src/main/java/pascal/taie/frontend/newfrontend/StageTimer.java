package pascal.taie.frontend.newfrontend;

import pascal.taie.World;

public class StageTimer {

    private static final StageTimer instance = new StageTimer();

    private long typelessStartTime;

    private long totalTypelessIRTime = 0;

    private long splittingStartTime;

    private long totalSplittingTime = 0;

    private long typingStartTime;

    private long totalTypingTime = 0;

    private long irTime;

    private long cwTime;

    private StageTimer() {
    }

    public static StageTimer getInstance() {
        assert !World.get().getOptions().isPreBuildIR() : "StageTimer does not work concurrently.";
        return instance;
    }

    public void startTypelessIR() {
        typelessStartTime = System.currentTimeMillis();
    }

    public void endTypelessIR() {
        long current = System.currentTimeMillis();
        totalTypelessIRTime += current - typelessStartTime;
    }

    public long getTotalTypelessIRTime() {
        return totalTypelessIRTime;
    }

    public void startSplitting() {
        splittingStartTime = System.currentTimeMillis();
    }

    public void endSplitting() {
        long current = System.currentTimeMillis();
        totalSplittingTime += current - splittingStartTime;
    }

    public long getTotalSplittingTime() {
        return totalSplittingTime;
    }

    public void startTyping() {
        typingStartTime = System.currentTimeMillis();
    }

    public void endTyping() {
        long current = System.currentTimeMillis();
        totalTypingTime += current - typingStartTime;
    }

    public long getTotalTypingTime() {
        return totalTypingTime;
    }

    public void reportIRTime(long irTime) {
        this.irTime = irTime;
    }

    public void reportCWTime(long cwTime) {
        this.cwTime = cwTime;
    }

    public long getIrTime() {
        return irTime;
    }

    public long getCwTime() {
        return cwTime;
    }
}
