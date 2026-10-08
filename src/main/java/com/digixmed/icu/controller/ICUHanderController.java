package com.digixmed.icu.controller;

import com.digixmed.icu.service.OrderService;
import com.digixmed.icu.viform.IcuRecordView;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RequestMapping({"/ICU"})
@RestController
public class ICUHanderController {
    @Autowired
    private OrderService orderService;

    @GetMapping({"/test"})
    @ApiOperation("测试获取PDA视图数据")
    public String test() {
        List<IcuRecordView> recordsAll = orderService.getRecordsAll();
        return recordsAll.toString();
    }

    @GetMapping({"/synOrderExecute"})
    @ApiOperation("同步医嘱执行记录（最近N小时）到DataCenter.VI_ICU_HSZ_YYZXJL，按 exeId upsert：没有新增，有则修改")
    public String synOrderExecute() {
        Map<String, Integer> result = this.orderService.synOrderExecute();
        return "同步完成：扫描 " + result.get("scan") + " 条，过滤后 " + result.get("filtered")
                + " 条，新增 " + result.get("insert") + " 条，更新 " + result.get("update")
                + " 条（按 exeId upsert：没有新增，有则修改）";
    }

}
