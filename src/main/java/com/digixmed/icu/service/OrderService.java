package com.digixmed.icu.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import com.digixmed.icu.dao.IcuRecordRepository;
import com.digixmed.icu.dao.MongoDao;
import com.digixmed.icu.viform.IcuRecordView;
import com.digixmed.icu.viform.Patient;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class OrderService  {

    /** 视图中需要排除的状态：摆药、配药 */
    private static final Set<String> EXCLUDE_STATUS = new HashSet<>(Arrays.asList("摆药", "配药"));

    /** 写入 VI_ICU_HSZ_YYZXJL 时的 _class，需与对端系统实体一致 */
    private static final String ORDER_EXECUTE_CLASS = "com.digixmed.web.internal.data.entity.OrderExecute";

    @Autowired
    MongoDao mongoDao;

    @Autowired
    private IcuRecordRepository repository;

    /** 增量同步回看窗口（小时），兜住窗口内的护士补录 */
    @Value("${digixmed.order-execute-lookback-hours:24}")
    private int orderExecuteLookbackHours;

    public void syPDA(){
        List<Patient> admittedPatient = this.mongoDao.findAdmittedPatient(List.of("admitted", "wait_admitted", "wait_discharged"));
        if(CollectionUtil.isEmpty(admittedPatient))return;
        Date date = new Date();

        for (Patient patient : admittedPatient) {

        }

    }

    /**
     * 同步医嘱执行记录：只取 record_time 在最近 N 小时（order-execute-lookback-hours，默认24）内的记录
     * 按 exeId upsert：库里没有则新增，有则修改，可重复执行
     */
    public Map<String, Integer> synOrderExecute() {
        Date start = DateUtil.offsetHour(new Date(), -this.orderExecuteLookbackHours);
        List<IcuRecordView> records = repository.findByRecordTimeGreaterThanEqual(start);
        log.info("医嘱执行记录同步：回看 {} 小时，窗口 {} 至今", this.orderExecuteLookbackHours, start);
        return doSynOrderExecute(records);
    }

    /**
     * 把金仓视图 vi_icu_hsz_yzzxjl 的执行记录同步到 DataCenter.VI_ICU_HSZ_YYZXJL
     * 字段映射：exeId=exeid、orderID=groupid、barCode=barcode、planTime=record_time、
     *          exeTime=record_time、status=status、speed=speed(#前数值)、nurseTrueName/nurseUserName=nurseusername
     * 排除 status 为 摆药/配药 的记录
     * 幂等：按 exeId upsert，视图里已同步过的老数据只会被更新，不会重复插入
     *
     * @return scan=视图扫描条数, filtered=过滤后条数, insert=新增条数, update=更新条数
     */
    private Map<String, Integer> doSynOrderExecute(List<IcuRecordView> records) {
        Map<String, Integer> result = new HashMap<>();
        result.put("insert", 0);
        result.put("update", 0);
        result.put("scan", records == null ? 0 : records.size());
        result.put("filtered", 0);
        if (CollectionUtil.isEmpty(records)) {
            log.info("医嘱执行记录同步：视图无数据");
            return result;
        }
        List<Document> docList = new ArrayList<>();
        for (IcuRecordView record : records) {
            String status = record.getStatus() == null ? null : record.getStatus().trim();
            if (status != null && EXCLUDE_STATUS.contains(status)) {
                continue;
            }
            Document doc = new Document();
            doc.put("_class", ORDER_EXECUTE_CLASS);
            doc.put("exeId", record.getExeid());
            // orderID 对应视图的 groupid
            doc.put("orderID", record.getGroupId());
            doc.put("barCode", record.getBarcode());
            // 计划时间取视图的 record_time（不再用 plan_time）
            doc.put("planTime", record.getRecordTime());
            doc.put("exeTime", record.getRecordTime());
            doc.put("status", status);
            doc.put("speed", parseSpeed(record.getSpeed()));
            doc.put("nurseTrueName", record.getNurseUsername());
            // 新增字段：护士用户名
            doc.put("nurseUserName", record.getNurseUsername());
            docList.add(doc);
        }
        result.put("filtered", docList.size());
        if (CollectionUtil.isEmpty(docList)) {
            log.info("医嘱执行记录同步：过滤后无数据，视图共 {} 条", records.size());
            return result;
        }
        Map<String, Integer> count = this.mongoDao.updateOrSaveOrderExecDocs(docList);
        result.putAll(count);
        log.info("医嘱执行记录同步完成：视图 {} 条，过滤后 {} 条，新增 {} 条，更新 {} 条",
                records.size(), docList.size(), count.get("insert"), count.get("update"));
        return result;
    }

    /**
     * speed 形如 "16#滴/分"，按 # 切割取第一段，得到 "16"
     */
    private String parseSpeed(String speed) {
        if (speed == null) {
            return null;
        }
        String value = speed.split("#")[0].trim();
        return StringUtils.isEmpty(value) ? null : value;
    }

    public List<IcuRecordView> getRecordsByTimeRange(String zyh, Date start, Date end) {
        // 直接调用，无需写 SQL
        return repository.findByZyhAndRecordTimeBetween(zyh, start, end);
    }

    public List<IcuRecordView> getRecordsAll() {
        // 直接调用，无需写 SQL
        return repository.findAll();
    }

}