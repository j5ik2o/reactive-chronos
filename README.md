# The Chronos is the Akka based Job Scheduler for Scala

__This product is under development.__

## Concepts

Provide the actor as a job to run by the schedule, 
The job is run by a trigger to represent the schedule.   

## Features

- Message-driven
- Asynchronous
- Non-blocking
- Let-it-crash

## Functions

- Scheduler
    - The Scheduler is what to run the Job based on the Trigger.
- Job
    - The Job is the actor based. it's what to execute user defined task.
- Trigger
    - The Trigger is the condition to execute the job.
    - There are various types into the conditions. In the standard, delay, interval, crond, etc.
    - If you wish, you can implement custom Trigger.

