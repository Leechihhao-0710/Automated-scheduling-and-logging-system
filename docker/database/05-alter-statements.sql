

ALTER TABLE tasks 
ADD COLUMN recurring_day_of_week INT COMMENT 'Weekly recurring day (1-7, Monday-Sunday)',
ADD COLUMN recurring_day_of_month INT COMMENT 'Monthly recurring day (1-31)',
ADD COLUMN skip_weekends BOOLEAN DEFAULT TRUE COMMENT 'Skip weekends for monthly tasks',
ADD COLUMN next_execution_date DATETIME(6) COMMENT 'Next scheduled execution date for recurring tasks';

