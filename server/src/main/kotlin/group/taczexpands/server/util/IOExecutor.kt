package group.taczexpands.server.util

import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.util.concurrent.*
import java.util.function.Supplier

object IOExecutor {
    var _scheduledExecutor: ScheduledExecutorService? = null
    var _asyncExecutor: ExecutorService? = null

    val scheduledExecutor: ScheduledExecutorService
        get() {
            if (_scheduledExecutor == null) {
                _scheduledExecutor = Executors
                    .newSingleThreadScheduledExecutor(
                        ThreadFactoryBuilder().setDaemon(true)
                            .setNameFormat("TACZExpands Task Scheduler Timer").build()
                    )
            }
            return _scheduledExecutor!!
        }

    val asyncExecutor: ExecutorService
        get() {
            if (_asyncExecutor == null) {
                _asyncExecutor = Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors(),
                    ThreadFactoryBuilder().setNameFormat("TACZExpands IO Executor - #%d").setDaemon(true).build()
                )
            }
            return _asyncExecutor!!
        }


    fun execute(task: () -> Unit) {
        asyncExecutor.execute(task)
    }

    fun <T> supply(supplier: Supplier<T>): CompletableFuture<T> {
        return CompletableFuture.supplyAsync(supplier, asyncExecutor)
    }

    fun executeDelayed(delay: Long, task: () -> Unit) {
        scheduledExecutor.schedule(task, delay, TimeUnit.MILLISECONDS)
    }

    fun <T> completeDelayed(future: CompletableFuture<T>, result: () -> T, delay: Long) {
        executeDelayed(delay) {
            future.completeAsync(result, asyncExecutor)
        }
    }

}

fun <T> CompletableFuture<T>.completeDelayed(result: T, delay: Long) {
    IOExecutor.completeDelayed(this, { result }, delay)
}