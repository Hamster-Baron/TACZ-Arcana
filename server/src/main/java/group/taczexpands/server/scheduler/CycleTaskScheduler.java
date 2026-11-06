package group.taczexpands.server.scheduler;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Server-tick scheduler used for burst completion callbacks.
 *
 * The two task buffers separate scheduled callbacks from the current iteration.
 * Access mutable state on the server thread.
 */
public final class CycleTaskScheduler {
    private static final List<CycleTaskTicker> CYCLE_TASKS = new LinkedList<>();
    private static final List<CycleTaskTicker> TEMP_CYCLE_TASKS = new LinkedList<>();

    private CycleTaskScheduler() {}

    public static void addCycleTask(BooleanSupplier task, long periodMs, int cycles, Runnable callback) {
        CycleTaskTicker ticker = new CycleTaskTicker(task, periodMs, cycles, callback);
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

    private static final class CycleTaskTicker {
        private final BooleanSupplier task;
        private final float periodS;
        private final int cycles;
        private float delayS = 0;
        private long timestamp = -1;
        private float compensation = 0;
        private int count = 0;
        private final Runnable callback;

        private CycleTaskTicker(BooleanSupplier task, long periodMs, int cycles, Runnable callback) {
            this.task = task;
            this.periodS = periodMs / 1000f;
            this.cycles = cycles;
            this.callback = callback;
        }

        private CycleTaskTicker(BooleanSupplier task, long delayMs, long periodMs, int cycles, Runnable callback) {
            this.delayS = delayMs / 1000f;
            this.timestamp = System.currentTimeMillis();
            this.task = task;
            this.periodS = periodMs / 1000f;
            this.cycles = cycles;
            this.callback = callback;
        }

        private boolean tick() {
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
