package cn.shaines.util;


import java.util.*;
import java.util.concurrent.*;

/**
 * @program: loading-blog
 * @description: 线程工具类
 * @author: houyu
 * @create: 2018-12-25 00:47
 */
public class ThreadPoolUtil {

    private static final LoggerFactory.Logger logger = LoggerFactory.getLogger(ThreadPoolUtil.class);

    private ConcurrentMap<String, Long>         threadRefreshTimeMap;               // 线程刷新时间Map
    private Map<String, Future<?>>              threadFutureMap;                    // 线程对应的操作对象
    private ExecutorService                     cachedThreadPool;                   // 缓存线程池
    private Long                                poolRefreshTime;                    // 缓存线程池最后刷新时间
    private boolean needMonitorWorkerFlag = true;                                   // 监听线程是否需要监听,一开始默认是需要监听
    private ScheduledExecutorService            monitorExecutorPool;                // 监听池

    private final Long                          THREAD_TIME_OVER    = 60000L;       // 线程超时自动关闭(60秒)
    private final Long                          POOL_TIME_OVER      = 60000L;       // 池子超时自动关闭(60秒)

    /**
     * 提交线程
     */
    public void submit(Runnable task){
        this.runMonitorWorkerTask();                        // 初始化:运行监听线程
        String id = UUID.randomUUID().toString();           // 创建线程的唯一id
        long timeMillis = System.currentTimeMillis();       // 最新刷新时间
        this.poolRefreshTime = timeMillis;                  // 更新缓存池刷新时间
        Future<?> future = cachedThreadPool.submit(task);   // 提交线程
        threadRefreshTimeMap.put(id, timeMillis);           // 存储提交的线程刷新时间
        threadFutureMap.put(id, future);                    // 存储提交的线程执行对象
        logger.debug("提交执行线程{}", id);
    }

    /**
     * 关闭线程池
     */
    public void shutdown(){
        this.cachedThreadPool.shutdown();                   // 关闭
        this.monitorExecutorPool.shutdown();
        logger.debug("关闭线程池");
    }

    /**
     * 创建监控线程
     */
    private Runnable monitorWorker = new Runnable() {
        @Override
        public void run() {
            try {
                logger.debug("监控线程轮询开始, 发现线程池有线程数量:{}", threadRefreshTimeMap.size());
                List<String> removeIdList = new ArrayList<>();
                String threadId;
                for (Map.Entry<String, Long> entry : threadRefreshTimeMap.entrySet()) {
                    threadId = entry.getKey();
                    if (threadFutureMap.get(threadId).isDone()){
                        // 线程已完成工作,正在待命,newCachedThreadPool机制:如果60秒内无任务,会干掉该线程
                        logger.debug("发现闲时线程,(该线程已完成任务,无需要监听)线程id:{}", threadId);
                        removeIdList.add(threadId);
                    }else if (System.currentTimeMillis() - entry.getValue() > THREAD_TIME_OVER) {
                        // 线程未完成任务,有可能阻塞了,这里我们需要手动干掉该线程,否则有可能是造成资源的浪费
                        logger.debug("发现超时线程,(该线程超时还没完成任务,需要关闭线程)线程id:{}" + threadId);
                        threadFutureMap.get(threadId).cancel(true);
                        removeIdList.add(threadId);
                    }
                }
                for (String id : removeIdList) {
                    threadFutureMap.remove(id);
                    threadRefreshTimeMap.remove(id);
                }
                logger.debug("--监控线程结束, 线程池还剩下线程数量:{}", threadRefreshTimeMap.size());
                if (threadRefreshTimeMap.size() != 0){
                    poolRefreshTime = System.currentTimeMillis();
                }
                if (System.currentTimeMillis() - poolRefreshTime > POOL_TIME_OVER){
                    logger.debug("终止监听池子");
                    cachedThreadPool.shutdown();
                    monitorExecutorPool.shutdown();
                    needMonitorWorkerFlag = true;       // 需要监听标志设置为true, 下次运行时开启监听
                }
            } catch (Exception e) {
                logger.warn("监听线程池线程出现异常", e);
            }
        }
    };

    /**
     * 开启监控线程
     */
    public void runMonitorWorkerTask() {
        if (needMonitorWorkerFlag){
            synchronized (ThreadPoolUtil.class){
                if (needMonitorWorkerFlag){
                    threadFutureMap         = new HashMap<>(16);
                    threadRefreshTimeMap    = new ConcurrentHashMap<>(16);
                    // cachedThreadPool        = Executors.newCachedThreadPool();
                    // cachedThreadPool        = new ThreadPoolExecutor(0, 300, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
                    // ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("custom-thread-pool-util-%d").build();// namedThreadFactory
                    cachedThreadPool        = new ThreadPoolExecutor(50, 100, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadPoolExecutor.AbortPolicy());
                    // monitorExecutorPool     = Executors.newScheduledThreadPool(1);
                    monitorExecutorPool     = new ScheduledThreadPoolExecutor(1);
                    poolRefreshTime         = System.currentTimeMillis();
                    monitorExecutorPool.scheduleAtFixedRate(monitorWorker, 0, 1, TimeUnit.SECONDS);
                    needMonitorWorkerFlag = false;
                }
            }
        }
    }

    /* ---------------------------------------单例模式---------------------------------------*/

    private ThreadPoolUtil() {}

    private interface SingletonHolder {
        ThreadPoolUtil INSTANCE = new ThreadPoolUtil();
    }

    public static ThreadPoolUtil get() {
        return SingletonHolder.INSTANCE;
    }
    /* ---------------------------------------单例模式---------------------------------------*/

    public static void main(String[] args) throws InterruptedException {
        // 测试
        //private final Long                          THREAD_TIME_OVER    = 3000L;       // 线程超时自动关闭(3秒)线程最多工作3秒, 3秒未完成工作的强制停止
        ThreadPoolUtil threadPoolUtil = ThreadPoolUtil.get();
        threadPoolUtil.runMonitorWorkerTask();
        List<Long> timeList = Arrays.asList(4000L, 2000L, 30000L, 4000L, 5000L, 6000L, 7000L, 8000L, 9000L);
        for(int i = 0; i < 1; i++){
            threadPoolUtil.submit(() -> {
                System.err.println(2000 + ":线程开始工作了");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    System.err.println("线程被打破!(强制通知工作)");
                }
                System.err.println(2000 + ":---线程结束工作了");
            });
            Thread.sleep(3000);
        }

        Thread.sleep(15000);
        threadPoolUtil.submit(() -> {
            System.err.println(5000 + ":线程开始工作了");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                System.err.println("线程被打破!(强制通知工作)");
            }
            System.err.println(5000 + ":---线程结束工作了");
        });
    }
}


//        // 阻塞队列线程池的使用: ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(2);
//        /** 阻塞队列, 最大值是200, 添加超过这个最大值就报错 */
//        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(200);
//        /** 线程池, 核心运行最大线程数量是2, 最大的线程池数量是4, 存活时间5分钟 */
//        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 4, 5, TimeUnit.MINUTES, queue);
//        for (int i = 1; i < 201; i++) {
//            executor.execute(new MyRun(i));
//        }
//        executor.shutdown();