package com.esabook.auzen.audio.tts

import android.os.Looper
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


/**
 * @author []" "Xuefeng Ding"">&quot;mailto:xuefeng.ding@outlook.com&quot; &quot;Xuefeng Ding&quot;
 * Created 2020-07-20 17:25
 */
class ThreadPoolManager private constructor() {

    companion object Holder {
        private val INSTANCE = ThreadPoolManager()
        fun getInstance(): ThreadPoolManager = INSTANCE

        fun isMainThread(): Boolean = Looper.myLooper() == Looper.getMainLooper()
    }

    private val mExecutor: ThreadPoolExecutor

    /**
     * Constructor
     */
    init {
        val corePoolSize = Runtime.getRuntime().availableProcessors() * 2 + 1
        val namedThreadFactory: ThreadFactory = NamedThreadFactory("thread pool")

        mExecutor = ThreadPoolExecutor(
            corePoolSize,
            corePoolSize * 10,
            1,
            TimeUnit.HOURS,
            LinkedBlockingQueue<Runnable>(),
            namedThreadFactory,
            ThreadPoolExecutor.DiscardPolicy()
        )
    }

    /**
     * 执行任务
     * @param runnable 需要执行的异步任务
     */
    fun execute(runnable: Runnable?) {
        if (runnable == null) {
            return
        }
        mExecutor.execute(runnable)
    }

    /**
     * single thread with name
     * @param name 线程名
     * @return 线程执行器
     */
    fun getSingleExecutor(name: String): ScheduledThreadPoolExecutor {
        return getSingleExecutor(name, Thread.NORM_PRIORITY)
    }

    /**
     * single thread with name and priority
     * @param name thread name
     * @param priority thread priority
     * @return Thread Executor
     */
    fun getSingleExecutor(name: String, priority: Int): ScheduledThreadPoolExecutor {
        return ScheduledThreadPoolExecutor(
            1,
            NamedThreadFactory(name, priority)
        )
    }

    /**
     * 从线程池中移除任务
     * @param runnable 需要移除的异步任务
     */
    fun remove(runnable: Runnable?) {
        if (runnable == null) {
            return
        }
        mExecutor.remove(runnable)
    }

    /**
     * 为线程池内的每个线程命名的工厂类
     */
    private class NamedThreadFactory(threadName: String, priority: Int) :
        ThreadFactory {
        private val group: ThreadGroup
        private val threadNumber = AtomicInteger(1)
        private val namePrefix: String
        private val priority: Int

        /**
         * Constructor
         * @param namePrefix 线程名前缀
         */
        constructor(namePrefix: String) : this(namePrefix, Thread.NORM_PRIORITY)

        /**
         * Constructor
         * @param threadName 线程名前缀
         * @param priority 线程优先级
         */
        init {
            val s = System.getSecurityManager()
            group = if ((s != null)) s.threadGroup else Thread.currentThread().threadGroup
            namePrefix = threadName + "-" + POOL_NUMBER.getAndIncrement()
            this.priority = priority
        }

        override fun newThread(r: Runnable): Thread {
            val t = Thread(
                group, r,
                namePrefix + threadNumber.getAndIncrement(),
                0
            )
            if (t.isDaemon) {
                t.isDaemon = false
            }

            t.priority = priority

            when (priority) {
                Thread.MIN_PRIORITY -> android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST)
                Thread.MAX_PRIORITY -> android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
                else -> android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_FOREGROUND)
            }
            return t
        }

        companion object {
            private val POOL_NUMBER = AtomicInteger(1)
        }
    }

}