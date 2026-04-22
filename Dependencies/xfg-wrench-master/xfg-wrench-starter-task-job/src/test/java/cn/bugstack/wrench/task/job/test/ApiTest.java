package cn.bugstack.wrench.task.job.test;

import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.concurrent.CountDownLatch;

/**
 * 单测
 *
 * @author 小傅哥
 * 2025/9/9 07:18
 */
public class ApiTest {

    @Test
    public void test() throws InterruptedException {
        // 初始化任务调度器
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("test-task-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();

        // 添加任务
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                System.out.println("111");
            }
        }, new CronTrigger("0/3 * * * * ?"));

        // 添加任务
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                System.out.println("222");
            }
        }, new CronTrigger("0/3 * * * * ?"));


        new CountDownLatch(1).await();
    }

}
