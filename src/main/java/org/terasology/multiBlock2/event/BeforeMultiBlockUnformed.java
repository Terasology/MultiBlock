// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock2.event;

import org.terasology.engine.entitySystem.event.Event;

public class BeforeMultiBlockUnformed implements Event {
    private final String type;

    public BeforeMultiBlockUnformed(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
