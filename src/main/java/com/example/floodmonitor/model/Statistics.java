package com.example.floodmonitor.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "statistics")
public class Statistics {
    private String stationId;
    private int measurementCount;
    private double minWaterLevel;
    private double maxWaterLevel;
    private double averageWaterLevel;
    private double averageFlowRate;
    private double rainfallSum;
    private int warningCount;
    private int criticalCount;

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }
    public int getMeasurementCount() { return measurementCount; }
    public void setMeasurementCount(int measurementCount) { this.measurementCount = measurementCount; }
    public double getMinWaterLevel() { return minWaterLevel; }
    public void setMinWaterLevel(double minWaterLevel) { this.minWaterLevel = minWaterLevel; }
    public double getMaxWaterLevel() { return maxWaterLevel; }
    public void setMaxWaterLevel(double maxWaterLevel) { this.maxWaterLevel = maxWaterLevel; }
    public double getAverageWaterLevel() { return averageWaterLevel; }
    public void setAverageWaterLevel(double averageWaterLevel) { this.averageWaterLevel = averageWaterLevel; }
    public double getAverageFlowRate() { return averageFlowRate; }
    public void setAverageFlowRate(double averageFlowRate) { this.averageFlowRate = averageFlowRate; }
    public double getRainfallSum() { return rainfallSum; }
    public void setRainfallSum(double rainfallSum) { this.rainfallSum = rainfallSum; }
    public int getWarningCount() { return warningCount; }
    public void setWarningCount(int warningCount) { this.warningCount = warningCount; }
    public int getCriticalCount() { return criticalCount; }
    public void setCriticalCount(int criticalCount) { this.criticalCount = criticalCount; }
}
