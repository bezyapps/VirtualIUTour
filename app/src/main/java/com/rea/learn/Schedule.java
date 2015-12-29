package com.rea.learn;


import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by ericbhatti on 12/29/15.
 */
public class Schedule {
    String startTime;
    String endTime;
    String className;
    Boolean isCurrent;

    public Schedule(String startTime, String endTime, String className) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.className = className;
    }

    public Schedule(String startTime, String endTime, String className, Boolean isCurrent) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.className = className;
        this.isCurrent = isCurrent;
    }

    public Boolean getIsCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(Boolean isCurrent) {
        this.isCurrent = isCurrent;
    }

    public Schedule() {
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String toString() {
       return className + " --> " + startTime + " - " + endTime;
    }
}
