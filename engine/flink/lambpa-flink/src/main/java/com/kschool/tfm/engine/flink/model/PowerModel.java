package com.kschool.tfm.engine.flink.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PowerModel {
    public Long timestamp;
    public String sensor_id;
    public String sensor_name;
    public String room;
    public Double lat;
    public Double lon;
    public Double V;
    public Double I;
    public Double Pkw;
    public Double price;
    public Double power_cons;
}
