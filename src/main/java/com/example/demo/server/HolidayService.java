package com.example.demo.server;

import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalDateTime;

@Service
public class HolidayService {

    public boolean isWeekendOrHoliday(LocalDateTime date) {
        // to set the holiday date(christmas day) according to different countries
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return true;
        }
        return isPublicHoliday(date);
    }

    public LocalDateTime getNextBusinessDay(LocalDateTime date) {
        LocalDateTime nextDay = date;

        while (isWeekendOrHoliday(nextDay)) {
            nextDay = nextDay.plusDays(1);
        }
        return nextDay;
    }

    private boolean isPublicHoliday(LocalDateTime date) {

        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 1 && day == 1)
            return true;
        if (month == 12 && day == 25)
            return true;

        return false;
    }
}
