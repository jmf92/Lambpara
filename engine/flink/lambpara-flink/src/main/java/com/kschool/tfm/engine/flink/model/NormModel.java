package com.kschool.tfm.engine.flink.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@ToString
public class NormModel {
    public Long timestamp;
    public String sensor_id;
    public Double V;
    public Double I;
    public String sensor_name;
    public String room;
    public Double lat;
    public Double lon;
}