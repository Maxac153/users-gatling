package ru.gatling.helpers;

import lombok.extern.slf4j.Slf4j;
import ru.gatling.models.profile.Profile;
import ru.gatling.models.profile.Step;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

@Slf4j
public class LoggerHelper {
    private static final String LINE_SEPARATOR = "+---------------------------------------------------------------------------------------------------------+";

    public static void logProfileDurationMaxInfo(Profile profileDurationMax) {
        log.info(LINE_SEPARATOR);
        log.info("|                                             Longest Scenario                                            |");
        log.info(LINE_SEPARATOR);
        logProfileInfo(profileDurationMax);
    }

    public static void logProfileInfo(Profile profile) {
        if (profile != null) {
            SimpleDateFormat sdfDateTime = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
            sdfDateTime.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));

            int durationProfile = profile.getSteps().stream()
                    .mapToInt(step -> (int) ((step.getRampTime() + step.getHoldTime()) * 60))
                    .sum();

            Calendar calendarStartRumpUp = Calendar.getInstance();
            Calendar calendarStart = Calendar.getInstance();
            Calendar calendarEnd = Calendar.getInstance();

            calendarStart.add(Calendar.SECOND, (int) (profile.getSteps().get(0).getRampTime() * 60));
            calendarEnd.add(Calendar.SECOND, durationProfile);

            long seconds = (calendarEnd.getTimeInMillis() - calendarStartRumpUp.getTimeInMillis()) / 1000;
            ArrayList<String> stepLogs = new ArrayList<>();
            Calendar stepStart = (Calendar) calendarStartRumpUp.clone();

            for (int i = 0; i < profile.getSteps().size(); i++) {
                Step step = profile.getSteps().get(i);
                stepStart.add(Calendar.SECOND, (int) (step.getRampTime() * 60));
                Calendar stepEnd = (Calendar) stepStart.clone();
                stepEnd.add(Calendar.SECOND, (int) (step.getHoldTime() * 60));
                stepLogs.add(String.format("from=%d&to=%d", stepStart.getTime().getTime(), stepEnd.getTime().getTime()));
                stepStart = stepEnd;
            }

            log.info(String.format("|  Scenario Name:       %-80.80s  |", profile.getScenarioName()));
            log.info(LINE_SEPARATOR);
            log.info(String.format("|  Scenario Duration:   %-80.80s  |", sdfDateTime.format(calendarStartRumpUp.getTime())));
            log.info(LINE_SEPARATOR);
            log.info(String.format("|  Scenario End Time:   %-80.80s  |", sdfDateTime.format(calendarEnd.getTime().getTime())));
            log.info(LINE_SEPARATOR);
            log.info(String.format("|  Scenario Duration:   %-80.80s  |", DataFormatHelper.format(seconds)));
            log.info(LINE_SEPARATOR);
            log.info(String.format("|  Grafana (RampUp):    %-80.80s  |", "from=" + calendarStartRumpUp.getTime().getTime() + "&to=" + calendarEnd.getTime().getTime()));
            log.info(LINE_SEPARATOR);
            log.info(String.format("|  Grafana:             %-80.80s  |", "from=" + calendarStart.getTime().getTime() + "&to=" + calendarEnd.getTime().getTime()));
            log.info(LINE_SEPARATOR);

            for (int i = 0; i < stepLogs.size(); i++) {
                log.info(String.format("|  Step #%-3.3s            %-80.80s  |", i + 1, stepLogs.get(i)));
                log.info(LINE_SEPARATOR);
            }
        }
    }
}
