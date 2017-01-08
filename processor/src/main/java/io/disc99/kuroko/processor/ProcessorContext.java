package io.disc99.kuroko.processor;

import java.util.Set;

public class ProcessorContext {
    private final Set<String> targets;
    public ProcessorContext(Set<String> targets) {
        this.targets = targets;
    }

    public boolean containsTarget(String type) {
        return targets.contains(type);
    }
}
