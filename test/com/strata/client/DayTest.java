package com.strata.client;

import com.strata.core.DayCycle;

public class DayTest {
    static int failures = 0;

    static void check(boolean cond, String msg) {
        if (!cond) { failures++; System.out.println("FAIL: " + msg); }
    }

    public static void main(String[] args) {
        long day = GameClient.DAY_LENGTH;
        float dawn = GameClient.dayAmount(0);
        float noon = GameClient.dayAmount(day / 4);
        float dusk = GameClient.dayAmount(day / 2);
        float midnight = GameClient.dayAmount(day * 3 / 4);
        System.out.println("dawn=" + dawn + " noon=" + noon + " dusk=" + dusk + " midnight=" + midnight);
        check(noon == 1.0f, "full day at noon");
        check(midnight == 0.0f, "dark at midnight");
        check(dawn > 0.05f && dawn < 0.6f, "dawn partial");
        check(dusk > 0.05f && dusk < 0.6f, "dusk partial");
        for (long t = 0; t < day; t += 137) {
            float d = GameClient.dayAmount(t);
            check(d >= 0.0f && d <= 1.0f, "range @" + t);
        }
        check(DayCycle.subFor(1.0F) == 0, "noon subtracts nothing");
        check(DayCycle.subFor(0.0F) == 11, "midnight subtracts 11");
        check(DayCycle.subFor(0.5F) == 6, "half day subtracts 6");
        check(DayCycle.subFor(-2.0F) == 11, "low clamps");
        check(DayCycle.subFor(99.0F) == 0, "high clamps");
        int last = DayCycle.subFor(0.0F);
        for (int i = 1; i <= 10; i++) {
            int s = DayCycle.subFor(i / 10.0F);
            check(s <= last, "monotonic grade (" + last + " -> " + s + ")");
            last = s;
        }
        check(DayCycle.nextStop(0, day) == day / 4, "dawn -> noon");
        check(DayCycle.nextStop(day / 4, day) == day / 2, "noon -> dusk");
        check(DayCycle.nextStop(day / 2, day) == day * 3 / 4, "dusk -> midnight");
        check(DayCycle.nextStop(day * 3 / 4, day) == day, "midnight -> dawn (wrap)");
        check(DayCycle.nextStop(day / 4 + 1, day) == day / 2, "mid-step advances");
        check(DayCycle.nextStop(day * 5, day) == day * 5 + day / 4, "multi-day base kept");
        check(DayCycle.stopName(0, day).equals("dawn"), "name dawn");
        check(DayCycle.stopName(day / 2, day).equals("dusk"), "name dusk");
        check(DayCycle.stopName(DayCycle.nextStop(day * 3 / 4, day), day).equals("dawn"), "wrap names dawn");
        if (failures == 0) System.out.println("DAY PASS");
        else { System.out.println(failures + " FAILURES"); System.exit(1); }
    }
}

