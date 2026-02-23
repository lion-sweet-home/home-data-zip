package org.example.homedatazip.global.quartz;

import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/**
 * Quartz Job에 Spring Bean(JobLauncher, ApplicationContext 등)을 자동 주입하는 JobFactory.
 */
public class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory implements ApplicationContextAware {

    private AutowireCapableBeanFactory beanFactory;

    @Override
    public void setApplicationContext(ApplicationContext context) {
        this.beanFactory = context.getAutowireCapableBeanFactory();
    }

    @Override
    protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
        Object jobInstance = super.createJobInstance(bundle);
        if (beanFactory != null) {
            beanFactory.autowireBean(jobInstance);
        }
        return jobInstance;
    }
}
