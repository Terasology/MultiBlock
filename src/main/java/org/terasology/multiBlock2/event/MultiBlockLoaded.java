// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock2.event;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.Event;

public class MultiBlockLoaded implements Event {
    private final String type;
    private final EntityRef mainBlockEntity;

    public MultiBlockLoaded(String type, EntityRef mainBlockEntity) {
        this.type = type;
        this.mainBlockEntity = mainBlockEntity;
    }

    public String getType() {
        return type;
    }

    public EntityRef getMainBlockEntity() {
        return mainBlockEntity;
    }
}
