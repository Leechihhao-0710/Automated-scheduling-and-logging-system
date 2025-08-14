package com.example.demo.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.demo.entity.Task;
import com.example.demo.enums.RecurrenceType;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.List;

@Service
public class RecurringTaskService {

    @Autowired
    private TaskService taskService;

    @Autowired
    private HolidayService holidayService;

    public void processWeeklyRecurringTasks() {// weekly task
        List<Task> weeklyTasks = taskService.getActiveRecurringTasksByType(RecurrenceType.WEEKLY);// get the weekly
                                                                                                  // tasks
        LocalDateTime now = LocalDateTime.now();

        for (Task recurringTask : weeklyTasks) {
            LocalDateTime nextWeekDate = calculateNextWeeklyDate(recurringTask, now);

            if (shouldCreateNextInstance(recurringTask, nextWeekDate)) {
                createTaskInstance(recurringTask, nextWeekDate);
            }
        }
    }

    public void processMonthlyRecurringTasks() {
        List<Task> monthlyTasks = taskService.getActiveRecurringTasksByType(RecurrenceType.MONTHLY);
        LocalDateTime now = LocalDateTime.now();

        for (Task recurringTask : monthlyTasks) {
            LocalDateTime nextMonthDate = calculateNextMonthlyDate(recurringTask, now);

            if (shouldCreateNextInstance(recurringTask, nextMonthDate)) {
                createTaskInstance(recurringTask, nextMonthDate);
            }
        }
    }

    private LocalDateTime calculateNextWeeklyDate(Task task, LocalDateTime baseDate) {// the calculation logic to
                                                                                      // calculate next assigned date
        if (task.getRecurringDayOfWeek() == null)
            return baseDate.plusWeeks(1);

        DayOfWeek targetDay = DayOfWeek.of(task.getRecurringDayOfWeek());
        LocalDateTime nextWeek = baseDate.plusWeeks(task.getRecurrenceInterval());

        while (nextWeek.getDayOfWeek() != targetDay) {
            nextWeek = nextWeek.plusDays(1);
        }

        return nextWeek;
    }

    private LocalDateTime calculateNextMonthlyDate(Task task, LocalDateTime baseDate) {// calculation logic to calculate
                                                                                       // next assigned date
        LocalDateTime nextMonth = baseDate.plusMonths(task.getRecurrenceInterval());

        if (task.getRecurringDayOfMonth() != null) {
            int targetDay = task.getRecurringDayOfMonth();
            nextMonth = nextMonth.withDayOfMonth(Math.min(targetDay, nextMonth.toLocalDate().lengthOfMonth()));

            if (task.getSkipWeekends() && holidayService.isWeekendOrHoliday(nextMonth)) {
                // use skip weekend logic to ensure that the next assigned date is not holiday
                nextMonth = holidayService.getNextBusinessDay(nextMonth);
            }
        }

        return nextMonth;
    }

    private boolean shouldCreateNextInstance(Task recurringTask, LocalDateTime nextDate) {
        return recurringTask.getNextExecutionDate() == null ||
                recurringTask.getNextExecutionDate().isBefore(nextDate.minusDays(1));
    }

    private void createTaskInstance(Task recurringTask, LocalDateTime dueDate) {
        Task newTask = new Task();
        newTask.setTitle(recurringTask.getTitle());
        newTask.setDescription(recurringTask.getDescription());
        newTask.setTaskType(recurringTask.getTaskType());
        newTask.setDueDateTime(dueDate);
        newTask.setLocation(recurringTask.getLocation());
        newTask.setCreator(recurringTask.getCreator());
        newTask.setDepartment(recurringTask.getDepartment());
        newTask.setRecurring(false);
        // newTask.setEmailReminder(recurringTask.isEmailReminder());
        // newTask.setReminderDaysBefore(recurringTask.getReminderDaysBefore());

        List<String> employeeIds = recurringTask.getAssignedEmployees()
                .stream()
                .map(emp -> emp.getId())
                .toList();

        taskService.createTaskWithAssignments(newTask,
                recurringTask.getDepartment() != null ? recurringTask.getDepartment().getId() : null,
                employeeIds);

        recurringTask.setNextExecutionDate(dueDate);
        taskService.updateTask(recurringTask.getId(), recurringTask);
    }
}