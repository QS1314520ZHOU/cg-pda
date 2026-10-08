package com.digixmed.icu.task;

import com.digixmed.icu.dao.MongoDao;
import com.digixmed.icu.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DictionaryTask {
    private static final Logger log = LoggerFactory.getLogger(DictionaryTask.class);

    @Autowired
    MongoDao mongoDao;

    @Autowired
    OrderService orderService;


    @Value("${digixmed.isTask:false}")
    Boolean isTask;
    @Value("${digixmed.isPatient:false}")
    Boolean isPatient;
    @Value("${digixmed.isSynPatient:false}")
    Boolean isSynPatient;

    @Scheduled(cron = "${digixmed.dicti-corn:0 0/30 * * * ?}")
    public void requestExamData() {
        if (Boolean.TRUE.equals(this.isTask)) {
            log.info("开始同步PDA数据!");
            try {
                this.orderService.synOrderExecute();
                log.info("PDA数据同步完成!");
                return;
            } catch (Exception e) {
                log.error("PDA数据同步异常：" + e.getMessage());
                return;
            }
        }
        log.info("未开启同步字典数据!");
    }

}