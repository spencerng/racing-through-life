package com.sng.aesthetics.racingthroughlife;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class StepCounter {

    private int totalSteps;

    private ArrayList<Date> stepLog;

    public StepCounter() {
        totalSteps = 0;
        stepLog = new ArrayList<>();
    }

    public void increment() {
        totalSteps += 1;
        stepLog.add(Calendar.getInstance().getTime());
        cleanup();
    }

    private double timeFromNowMin(Date time) {
        Date curTime = Calendar.getInstance().getTime();
        long timeDeltaMs = curTime.getTime() - time.getTime();
        return timeDeltaMs / (60.0 * 1000);
    }

    // Remove all step records older than the past minute
    public void cleanup() {
        if (stepLog.size() == 0) {
            return;
        }

        while (timeFromNowMin(stepLog.get(0)) > 1) {
            stepLog.remove(0);
            if (stepLog.size() == 0) {
                return;
            }
        }
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    // Get average number of steps for the past minute
    public double getRollingAvgSteps() {
        cleanup();

        if (stepLog.size() == 0) {
            return 0;
        }

        double minDiff = timeFromNowMin(stepLog.get(0));
        return stepLog.size() / minDiff;
    }

}
