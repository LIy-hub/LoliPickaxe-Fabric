package com.liymod.combat;

public enum ExecutionAuthority {
    STANDARD(0, false),
    ABSOLUTE_EXECUTION(Integer.MAX_VALUE, true);

    private final int priority;
    private final boolean piercesExecutionDefense;

    ExecutionAuthority(int priority, boolean piercesExecutionDefense) {
        this.priority = priority;
        this.piercesExecutionDefense = piercesExecutionDefense;
    }

    public int priority() {
        return priority;
    }

    public boolean piercesExecutionDefense() {
        return piercesExecutionDefense;
    }
}
