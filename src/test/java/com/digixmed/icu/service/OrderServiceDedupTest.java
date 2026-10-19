package com.digixmed.icu.service;

import com.digixmed.icu.viform.IcuRecordView;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 单次用药（isOnce）按 barcode 去重逻辑的单元测试
 * 覆盖：code 多值匹配、取最早执行行、无执行行整组跳过、
 *       非 isOnce（持续用药）组执行+结束全保留、isOnce 组执行+结束只留执行、空值组原样保留
 */
class OrderServiceDedupTest {

    private final OrderService orderService = new OrderService();

    private IcuRecordView row(String exeid, String barcode, String status, String supplyCode, Date recordTime) {
        IcuRecordView r = new IcuRecordView();
        r.setExeid(exeid);
        r.setBarcode(barcode);
        r.setStatus(status);
        r.setSupplyCode(supplyCode);
        r.setRecordTime(recordTime);
        r.setGroupId("g-" + exeid);
        return r;
    }

    /** 构造一条有效配置（code 可为 、 分隔多值） */
    private Document config(String code, boolean isOnce) {
        return new Document("code", code).append("isOnce", isOnce).append("valid", true);
    }

    /** 同一时刻不同秒的时间点，仅用于比较先后 */
    private Date t(int second) {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.OCTOBER, 9, 12, 0, second);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private List<String> ids(List<IcuRecordView> list) {
        List<String> ids = new ArrayList<>();
        for (IcuRecordView r : list) {
            ids.add(r.getExeid());
        }
        return ids;
    }

    /* ==== normalizeCodes ==== */

    @Test
    void normalizeCodes_splitsChineseEnumerationComma() {
        assertEquals(Arrays.asList("103", "104", "105"), OrderService.normalizeCodes("103、 104、、105"));
        assertEquals(Collections.singletonList("1"), OrderService.normalizeCodes("1"));
        assertTrue(OrderService.normalizeCodes(null).isEmpty());
        assertTrue(OrderService.normalizeCodes("").isEmpty());
    }

    /* ==== findDrugMethod ==== */

    @Test
    void findDrugMethod_matchesMultiCodeAndSkipsInvalid() {
        List<Document> configs = Arrays.asList(
                config("2", false) /* 测试内后面覆盖 valid=false */,
                config("103、104、105", true));
        configs.get(0).put("valid", false);

        // 多值 code 命中
        Document hit = OrderService.findDrugMethod("104", configs);
        assertNotNull(hit);
        assertEquals(Boolean.TRUE, hit.getBoolean("isOnce"));

        // valid=false 即使命中 code 也跳过
        assertNull(OrderService.findDrugMethod("2", configs));
        // 未命中 / 空入参
        assertNull(OrderService.findDrugMethod("999", configs));
        assertNull(OrderService.findDrugMethod(null, configs));
        assertNull(OrderService.findDrugMethod(" ", configs));
        assertNull(OrderService.findDrugMethod("104", null));
    }

    /* ==== dedupOnceGroups ==== */

    @Test
    void onceGroup_singleExecKeepsOnlyThatRow() {
        List<Document> configs = Collections.singletonList(config("104", true));
        List<IcuRecordView> input = Arrays.asList(
                row("e1", "BC1", "复核", "104", t(10)),
                row("e2", "BC1", "复核", "104", t(20)),
                row("e3", "BC1", "执行", "104", t(30)));
        Map<String, Integer> result = new HashMap<>();
        List<IcuRecordView> kept = orderService.dedupOnceGroups(input, configs, result);
        assertEquals(Collections.singletonList("e3"), ids(kept));
        assertEquals(0, result.getOrDefault("skippedGroups", 0));
    }

    @Test
    void onceGroup_multipleExecKeepsEarliest() {
        List<Document> configs = Collections.singletonList(config("103、104", true));
        List<IcuRecordView> input = Arrays.asList(
                row("e1", "BC1", "执行", "104", t(50)),
                row("e2", "BC1", "执行", "104", t(10)),
                row("e3", "BC1", "复核", "104", t(60)));
        Map<String, Integer> result = new HashMap<>();
        List<IcuRecordView> kept = orderService.dedupOnceGroups(input, configs, result);
        assertEquals(Collections.singletonList("e2"), ids(kept));
        assertEquals(0, result.getOrDefault("skippedGroups", 0));
    }

    @Test
    void onceGroup_noExecSkipsWholeGroupAndCounts() {
        List<Document> configs = Collections.singletonList(config("104", true));
        List<IcuRecordView> input = Arrays.asList(
                row("e1", "BC1", "复核", "104", t(10)),
                row("e2", "BC1", "复核", "104", t(20)));
        Map<String, Integer> result = new HashMap<>();
        List<IcuRecordView> kept = orderService.dedupOnceGroups(input, configs, result);
        assertTrue(kept.isEmpty());
        assertEquals(1, result.getOrDefault("skippedGroups", 0));
    }

    @Test
    void nonOnceOrUnmatchedGroupKeepsAllRows() {
        List<Document> configs = Arrays.asList(config("1", false), config("104", true));
        List<IcuRecordView> input = Arrays.asList(
                // isOnce=false 的组：原样保留
                row("n1", "BC2", "执行", "1", t(10)),
                row("n2", "BC2", "执行", "1", t(20)),
                // 匹配不到配置的组：原样保留
                row("n3", "BC3", "执行", "999", t(10)),
                row("n4", "BC3", "执行", "999", t(20)));
        Map<String, Integer> result = new HashMap<>();
        List<IcuRecordView> kept = orderService.dedupOnceGroups(input, configs, result);
        assertEquals(Arrays.asList("n1", "n2", "n3", "n4"), ids(kept));
        assertEquals(0, result.getOrDefault("skippedGroups", 0));
    }

    @Test
    void nullBarcodeAndNullSupplyCodeRowsKeptAsIs() {
        List<Document> configs = Collections.singletonList(config("104", true));
        List<IcuRecordView> input = Arrays.asList(
                // barcode 为空：不参与分组，逐条保留
                row("b1", null, "执行", "104", t(10)),
                row("b2", "", "执行", "104", t(20)),
                // supplyCode 为空的组：匹配不到，整组保留
                row("s1", "BC4", "执行", null, t(10)),
                row("s2", "BC4", "执行", null, t(20)));
        Map<String, Integer> result = new HashMap<>();
        List<IcuRecordView> kept = orderService.dedupOnceGroups(input, configs, result);
        assertEquals(Arrays.asList("b1", "b2", "s1", "s2"), ids(kept));
        assertEquals(0, result.getOrDefault("skippedGroups", 0));
    }

    @Test
    void continuousGroup_execAndEndBothKept() {
        // 持续用药（isOnce=false）：执行+结束 两行都保留，各事件一条文档
        List<Document> configs = Collections.singletonList(config("124", false));
        List<IcuRecordView> input = Arrays.asList(
                row("c1", "BC7", "执行", "124", t(10)),
                row("c2", "BC7", "结束", "124", t(20)));
        Map<String, Integer> result = new HashMap<>();
        List<IcuRecordView> kept = orderService.dedupOnceGroups(input, configs, result);
        assertEquals(Arrays.asList("c1", "c2"), ids(kept));
        assertEquals(0, result.getOrDefault("skippedGroups", 0));
    }

    @Test
    void onceGroup_execAndEndKeepsOnlyExec() {
        // 单次用药（isOnce=true）：组内有执行+结束时只留执行行
        List<Document> configs = Collections.singletonList(config("104", true));
        List<IcuRecordView> input = Arrays.asList(
                row("o1", "BC8", "执行", "104", t(10)),
                row("o2", "BC8", "结束", "104", t(20)));
        Map<String, Integer> result = new HashMap<>();
        List<IcuRecordView> kept = orderService.dedupOnceGroups(input, configs, result);
        assertEquals(Collections.singletonList("o1"), ids(kept));
        assertEquals(0, result.getOrDefault("skippedGroups", 0));
    }

    @Test
    void mixedGroups_onlyOnceGroupsAreDeduped() {
        List<Document> configs = Collections.singletonList(config("104", true));
        List<IcuRecordView> input = Arrays.asList(
                // isOnce 组：只留执行行
                row("o1", "BC5", "复核", "104", t(10)),
                row("o2", "BC5", "执行", "104", t(20)),
                // 非 isOnce 组：全部保留，且保持输入顺序
                row("x1", "BC6", "执行", "1", t(30)),
                row("x2", "BC6", "执行", "1", t(40)));
        Map<String, Integer> result = new HashMap<>();
        List<IcuRecordView> kept = orderService.dedupOnceGroups(input, configs, result);
        assertEquals(Arrays.asList("o2", "x1", "x2"), ids(kept));
        assertEquals(0, result.getOrDefault("skippedGroups", 0));
    }
}
