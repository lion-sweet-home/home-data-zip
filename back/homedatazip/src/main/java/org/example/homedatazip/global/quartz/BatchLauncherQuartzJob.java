package org.example.homedatazip.global.quartz;

import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Quartz Job. JobDataMap의 batchJobBeanName으로 등록된 Spring Batch Job을 실행한다.
 * 모든 Batch 스케줄링은 이 Job 하나로 통합
 */
@Slf4j
@DisallowConcurrentExecution
public class BatchLauncherQuartzJob extends QuartzJobBean {

    public static final String JOB_DATA_KEY_BATCH_JOB_BEAN_NAME = "batchJobBeanName";

    private JobLauncher jobLauncher;
    private ApplicationContext applicationContext;

    public void setJobLauncher(JobLauncher jobLauncher) {
        this.jobLauncher = jobLauncher;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        // SchedulerContext에서 Bean 조회 (JobFactory autowiring 실패 시 대비)
        ApplicationContext ctx = this.applicationContext;
        JobLauncher launcher = this.jobLauncher;
        if (ctx == null || launcher == null) {
            try {
                SchedulerContext schedulerCtx = context.getScheduler().getContext();
                ctx = (ApplicationContext) schedulerCtx.get(QuartzConfig.SCHEDULER_CONTEXT_APP_CTX);
                launcher = (JobLauncher) schedulerCtx.get(QuartzConfig.SCHEDULER_CONTEXT_JOB_LAUNCHER);
            } catch (SchedulerException e) {
                throw new JobExecutionException("SchedulerContext 조회 실패", e);
            }
        }
        if (ctx == null || launcher == null) {
            throw new JobExecutionException("ApplicationContext 또는 JobLauncher를 SchedulerContext에서 찾을 수 없습니다.");
        }

        String batchJobBeanName = context.getMergedJobDataMap().getString(JOB_DATA_KEY_BATCH_JOB_BEAN_NAME);
        if (batchJobBeanName == null || batchJobBeanName.isBlank()) {
            throw new JobExecutionException("JobDataMap에 " + JOB_DATA_KEY_BATCH_JOB_BEAN_NAME + " 가 필요합니다.");
        }

        try {
            Job job = ctx.getBean(batchJobBeanName, Job.class);
            JobParametersBuilder paramsBuilder = new JobParametersBuilder()
                    .addLong("runId", System.currentTimeMillis());

            // tradeRentBackfillJob: fromYmd, toYmd 필요 (window-months 기반)
            if ("tradeRentBackfillJob".equals(batchJobBeanName)) {
                int windowMonths = 2;
                try {
                    String wm = ctx.getEnvironment()
                            .getProperty("spring.batch.trade-rent.window-months", "2");
                    windowMonths = Integer.parseInt(wm);
                } catch (Exception ignored) { }
                var zone = java.time.ZoneId.of("Asia/Seoul");
                var fmt = java.time.format.DateTimeFormatter.ofPattern("yyyyMM");
                var toYm = java.time.YearMonth.now(zone).minusMonths(1);
                var fromYm = toYm.minusMonths(windowMonths);
                paramsBuilder.addString("fromYmd", fromYm.format(fmt));
                paramsBuilder.addString("toYmd", toYm.format(fmt));
            }

            JobParameters params = paramsBuilder.toJobParameters();

            log.info("[Quartz] Spring Batch 실행 시작: jobName={}", batchJobBeanName);
            var execution = launcher.run(job, params);
            log.info("[Quartz] Spring Batch 실행 완료: jobName={}, status={}",
                    batchJobBeanName, execution.getStatus());
        } catch (Exception e) {
            log.error("[Quartz] Spring Batch 실행 실패: jobName={}", batchJobBeanName, e);
            throw new JobExecutionException(e);
        }
    }
}
