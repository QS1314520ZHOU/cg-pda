package com.digixmed.icu.viform;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.List;

@Data
@Document("drugExe")
public class DrugExe {
    @Id
    private String id;
    private String pid;
    private String mrn;
    private String ordersIDFull;
    private String orderGroup;
    private String status;
    private Date createdTime;
    private Date orderTime;
    private Date planStartTime;
    private Date startTime;
    private Date endTime;
    private String orderType;
    private String frequency;
    private Boolean byDoctor;
    private String orderUser;
    private String methodCode;
    private Boolean isOnce;
    private Double liquidAmount;
    private String liquidAmountUnit;
    private String notes;
    private Boolean hasHighRisk;
    private Boolean takeIn;
    private Double weight;
    private List<DrugItem> drugList;
    private List<DrugActionItem> drugActionList;
    private Boolean isOperate;
    private String recommendSpeed;
    private String recommendSpeedUnit;
    private Double onceYuLiang;
}
