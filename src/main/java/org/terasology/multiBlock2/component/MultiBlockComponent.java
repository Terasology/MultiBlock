// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock2.component;

import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.entitySystem.entity.EntityRef;

/**
 * Not for external use!
 */
public class MultiBlockComponent implements Component {
    private String type;
    private EntityRef mainBlockEntity;

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
}
