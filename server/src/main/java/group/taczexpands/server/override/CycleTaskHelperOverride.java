package group.taczexpands.server.override;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class CycleTaskHelperOverride {
    public static final List<CycleTaskHelperOverride.CycleTaskTicker> CYCLE_TASKS = new LinkedList<>();
    public static final List<CycleTaskHelperOverride.CycleTaskTicker> TEMP_CYCLE_TASKS = new LinkedList<>();

        public static void addCycleTask(BooleanSupplier task, long periodMs, int cycles, Runnable callback) {
        CycleTaskHelperOverride.CycleTaskTicker ticker = new CycleTaskHelperOverride.CycleTaskTicker(task, periodMs, cycles, callback);
        if (ticker.tick()) {
            CYCLE_TASKS.add(ticker);
        } else {
            callback.run();
        }
    }


    public static void tick() {
        TEMP_CYCLE_TASKS.addAll(CYCLE_TASKS);
        CYCLE_TASKS.clear();
        TEMP_CYCLE_TASKS.removeIf(ticker -> {
            var result = ticker.tick();
            if (!result) {
                ticker.callback.run();
                return true;
            }
            return false;
        });
        CYCLE_TASKS.addAll(TEMP_CYCLE_TASKS);
        TEMP_CYCLE_TASKS.clear();
    }

    public static class CycleTaskTicker {
        public final BooleanSupplier task;
        public final float periodS;
        public final int cycles;
        public float delayS = 0;
        public long timestamp = -1;
        public float compensation = 0;
        public int count = 0;
        public Runnable callback;

        public CycleTaskTicker(BooleanSupplier task, long periodMs, int cycles, Runnable callback) {
            this.task = task;
            this.periodS = periodMs / 1000f;
            this.cycles = cycles;
            this.callback = callback;
        }

        public CycleTaskTicker(BooleanSupplier task, long delayMs, long periodMs, int cycles, Runnable callback) {
            this.delayS = delayMs / 1000f;
            this.timestamp = System.currentTimeMillis();
            this.task = task;
            this.periodS = periodMs / 1000f;
            this.cycles = cycles;
            this.callback = callback;
        }

        public boolean tick() {
            if (timestamp == -1) {
                timestamp = System.currentTimeMillis();
                if (cycles > 0 && ++count > cycles) {
                    return false;
                }
                return task.getAsBoolean();
            }
            float duration = (System.currentTimeMillis() - timestamp) / 1000f + compensation;
            if (delayS > 0) {
                if (delayS > duration) {
                    delayS = delayS - duration;
                    return true;
                } else {
                    delayS = 0;
                    duration = duration - delayS + periodS;
                }
            }
            if (duration > periodS) {
                compensation = duration;
                timestamp = System.currentTimeMillis();
                while (compensation > periodS) {
                    if (cycles > 0 && ++count > cycles) {
                        return false;
                    }
                    if (!task.getAsBoolean()) {
                        return false;
                    }
                    compensation -= periodS;
                }
            }
            return true;
        }
    }
}
