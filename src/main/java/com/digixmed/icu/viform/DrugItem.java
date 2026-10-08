package com.digixmed.icu.viform;

import lombok.Data;

@Data
public class DrugItem {
    private String name;
    private String code;
    private String specs;
    private Double dose;
    private String unit;
    private Double liquidAmount;
}
