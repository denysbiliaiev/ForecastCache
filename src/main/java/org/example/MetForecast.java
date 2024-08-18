package org.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(value = {"type", "geometry"})
public final class MetForecast {
    public final Properties properties;

    public MetForecast(@JsonProperty("properties") Properties properties) {
        this.properties = properties;
    }
}

@JsonIgnoreProperties(value = {"meta"})
final class Properties {
    public final List<Timeserie> timeseries;

    public Properties(@JsonProperty("timeseries") List<Timeserie> timeseries) {
        this.timeseries = timeseries;
    }
}

final class Timeserie {
    public final String time;
    public final TimeserieData data;

    public Timeserie(@JsonProperty("time") String time,
                     @JsonProperty("data") TimeserieData data) {
        this.time = time;
        this.data = data;
    }
}

@JsonIgnoreProperties(value = {"next_12_hours", "next_1_hours", "next_6_hours"})
final class TimeserieData {
    public final TimeserieInstant instant;

    public TimeserieData(@JsonProperty("instant") TimeserieInstant instant) {
        this.instant = instant;
    }
}

final class TimeserieInstant {
    public final TimeserieDetails details;

    public TimeserieInstant(@JsonProperty("details") TimeserieDetails details) {
        this.details = details;
    }
}

@JsonIgnoreProperties(value = {"air_pressure_at_sea_level", "cloud_area_fraction", "relative_humidity", "wind_from_direction"})
final class TimeserieDetails {
    public final String air_temperature;
    public final String wind_speed;

    public TimeserieDetails(@JsonProperty("air_temperature") String air_temperature,
                            @JsonProperty("wind_speed") String wind_speed) {
        this.air_temperature = air_temperature;
        this.wind_speed = wind_speed;
    }

    @Override
    public String toString() {
        return this.air_temperature + this.wind_speed;
    }
}