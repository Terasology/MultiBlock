// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock2.component;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.gestalt.entitysystem.component.Component;

/**
 * Not for external use!
 */
public class MultiBlockComponent implements Component<MultiBlockComponent> {
    public String type;
    public EntityRef mainBlockEntity;

    public MultiBlockComponent() {
    }

    public MultiBlockComponent(String type, EntityRef mainBlockEntity) {
        this.type = type;
        this.mainBlockEntity = mainBlockEntity;
    }

    public String getType() {
        return type;
    }

    public EntityRef getMainBlockEntity() {
        return mainBlockEntity;
    }

    @Override
    public void copyFrom(MultiBlockComponent other) {
        this.type = other.type;
        this.mainBlockEntity = other.mainBlockEntity;
    }
}
