package com.aenadon.wififix;

class TaskResult {

    private TaskStatus taskStatus;
    private Exception exception;
    private RepairMethod repairMethod;

    TaskResult(TaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }

    TaskResult(TaskStatus taskStatus, Exception exception, RepairMethod repairMethod) {
        this.taskStatus = taskStatus;
        this.exception = exception;
        this.repairMethod = repairMethod;
    }

    TaskStatus getTaskStatus() {
        return taskStatus;
    }

    Exception getException() {
        return exception;
    }

    RepairMethod getRepairMethod() {
        return repairMethod;
    }
}
