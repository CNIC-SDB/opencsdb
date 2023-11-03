package com.linkeddata.portal.utils.threadpool;

import com.linkeddata.portal.entity.ExportRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * 多线程执行类
 *
 * @author wangzhiliang
 */
@Slf4j
public class ThreadUtil {
    private static final int CORE_POOL_SIZE = 3;
    private static final int MAX_POOL_SIZE = 8;
    private static final int QUEUE_CAPACITY =  31;
    private static final Long KEEP_ALIVE_TIME = 1L;
    /**
     * @author wangzhiliang
     */
    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(QUEUE_CAPACITY), new ThreadPoolFactory(Executors.defaultThreadFactory(), "exportGraph"), new ThreadPoolExecutor.CallerRunsPolicy());

    /**
     * 多线程调用
     *
     * @author wangzhiliang
     */
    public static Object callingThread(ExportRequest request) {
        Callable<Object> exportGraph = new ThreadRunAble(request);
        return executor.submit(exportGraph);

    }
    /**
     * 配和调用使用 判断 子线程是否全部执行完成
     *
     * @author wangzhiliang
     * @date 20220629
     */
    public static void getExecutor() {
        //利用死循环对主线程进行堵塞直到 子线程全部执行完成
        log.info("关闭线程池");
        executor.shutdown();
        while (true) {
            if (executor.isTerminated()) {
                log.info("任务总数: {} ； 已经完成任务数: {} ", executor.getTaskCount(), executor.getCompletedTaskCount());
                log.info("全部执行完毕");
                break;
            }
        }
    }
    /**
     * 查询 化合物关联 mesh 关联关系  线程池
     * @author wangzhiliang
     * @date 20230215
     */
    private static ThreadPoolExecutor executorChem = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(QUEUE_CAPACITY), new ThreadPoolFactory(Executors.defaultThreadFactory(), "ChemSameAsMesh"), new ThreadPoolExecutor.CallerRunsPolicy());

    /**
     * 查询 化合物关联 mesh 关联关系 多线程调用
     * @author wangzhiliang
     * @date 20230215
     * */
    /**
     * 多线程调用
     * @param chemSparql 查询化合物语句
     * @author wangzhiliang
     * @date 20230215
     */
    public static Object chemSameAsMeshCalling(String chemSparql, String fusekiIp, String fusekiGraph,String chemEndPoint) {
        Callable<Object> chemSameAsMesh = new ChemSameAsMeshRunAble(chemSparql,fusekiIp,fusekiGraph ,chemEndPoint);
        return executorChem.submit(chemSameAsMesh );

    }
    /**
     * 配和调用使用 判断 子线程是否全部执行完成
     *
     * @author wangzhiliang
     * @date 20220629
     */
    public static void getExecutorChem() {
        //利用死循环对主线程进行堵塞直到 子线程全部执行完成
        log.info("关闭线程池");
        executorChem.shutdown();
        while (true) {
            if (executorChem.isTerminated()) {
                log.info("任务总数: {} ； 已经完成任务数: {} ", executorChem.getTaskCount(), executorChem.getCompletedTaskCount());
                log.info("全部执行完毕");
                break;
            }
        }
    }
}
