package com.sng.aesthetics.racingthroughlife;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class StepCounter {

    private int totalSteps;
    private double activeSeconds;

    private double rollingWindow;

    private Date startTime;
    private int totalStepsInWindow;


    private ArrayList<Date> stepLog;

    public StepCounter(double windowIntervalSec) {
        totalSteps = 0;
        activeSeconds = 0;
        stepLog = new ArrayList<>();
        rollingWindow = windowIntervalSec;
        startTime = null;
    }

    public void increment() {
        if (startTime == null) {
            startTime = Calendar.getInstance().getTime();
        }

        totalStepsInWindow += 1;
        stepLog.add(Calendar.getInstance().getTime());
        cleanup();
    }

    private double timeFromNowSec(Date time) {
        Date curTime = Calendar.getInstance().getTime();
        long timeDeltaMs = curTime.getTime() - time.getTime();
        return timeDeltaMs / 1000.0;
    }

    private double timeFromLastTime(Date time, Date latestTime) {
        long timeDeltaMs = latestTime.getTime() - time.getTime();
        return timeDeltaMs / 1000.0;
    }

    // Remove all step records older than the past minute
    public void cleanup() {
        if (stepLog.size() == 0) {
            return;
        }

        Date latestStep = null;
        while (timeFromNowSec(stepLog.get(0)) > rollingWindow) {
            if (stepLog.size() == 1) {
                latestStep = stepLog.get(0);
            }
            stepLog.remove(0);
            if (stepLog.size() == 0) {
                break;
            }
        }

        // If window has less than 30 steps, don't count it
        if (stepLog.size() == 0 && totalStepsInWindow >= 30) {
            totalSteps += totalStepsInWindow;
            Date oldStartTime = (Date) startTime.clone();
            assert latestStep != null;
            activeSeconds += timeFromLastTime(oldStartTime, latestStep);
            startTime = null;
            totalStepsInWindow = 0;
        }
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public double getCumulativeAvgStepsPerSec() {

        double timeSeconds = activeSeconds;
        double steps = totalSteps;

        if (startTime != null && stepLog.size() != 0) {
            timeSeconds += timeFromLastTime(startTime, stepLog.get(stepLog.size() - 1));
            steps += totalStepsInWindow;
        }

        if (timeSeconds == 0) {
            return 0;
        }

        return steps / timeSeconds;
    }

    // Get average number of steps for window interval,
    // in steps per second
    public double getRollingAvgSteps() {

        if (stepLog.size() <= 3) {
            return 0;
        }

        double minDiff = timeFromNowSec(stepLog.get(0));
        return stepLog.size() / minDiff;
    }

}
