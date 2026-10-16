package com.digixmed.icu.dao;

import com.digixmed.icu.viform.IcuRecordView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
@Repository
public interface IcuRecordRepository extends JpaRepository<IcuRecordView, String> {
    /**
     * 根据 MRN 和记录时间范围查询
     * 解析逻辑: Where mrn = ?1 And recordTime Between ?2 And ?3
     */
    List<IcuRecordView> findByZyhAndRecordTimeBetween(String zyh, Date startTime, Date endTime);

    /**
     * 增量同步用：取 record_time 在窗口内的记录
     * 解析逻辑: Where recordTime >= ?1
     */
    List<IcuRecordView> findByRecordTimeGreaterThanEqual(Date startTime);

    /**
     * 视图是按状态的事件流水（同一 exeid 有 摆药/配药/执行/结束 等多行），
     * 同步只取「执行」事件行：每个 exeid 恰好一行，record_time 即执行时刻
     * 解析逻辑: Where status = ?1
     */
    List<IcuRecordView> findByStatus(String status);

    /**
     * 增量同步用：只取窗口内的「执行」事件行
     * 解析逻辑: Where status = ?1 And recordTime >= ?2
     */
    List<IcuRecordView> findByStatusAndRecordTimeGreaterThanEqual(String status, Date startTime);
}
