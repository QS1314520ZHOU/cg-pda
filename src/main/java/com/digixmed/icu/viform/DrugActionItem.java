package com.digixmed.icu.viform;

import lombok.Data;
import java.time.LocalDateTime;
@Data
public class DrugActionItem {
    private LocalDateTime time;
    private String accountId;
    private Integer dripSpeed;
    private String action;
    private Double quickAddAmount;
}
