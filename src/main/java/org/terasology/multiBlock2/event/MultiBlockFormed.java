// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock2.event;

import org.terasology.engine.entitySystem.event.Event;
import org.terasology.multiBlock2.MultiBlockDefinition;

public class MultiBlockFormed<T extends MultiBlockDefinition> implements Event {
    private final String type;
    private final T multiBlockDefinition;

    public MultiBlockFormed(String type, T multiBlockDefinition) {
        this.type = type;
        this.multiBlockDefinition = multiBlockDefinition;
    }

    public String getType() {
        return type;
    }

    public T getMultiBlockDefinition() {
        return multiBlockDefinition;
    }
}
