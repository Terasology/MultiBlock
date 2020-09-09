// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock2.component;

import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.math.Region3i;
import org.terasology.engine.world.block.ForceBlockActive;
import org.terasology.math.geom.Vector3i;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Not for external use!
 */
@ForceBlockActive
public class MultiBlockMainComponent implements Component {
    private List<Vector3i> multiBlockMembers;
    private Region3i aabb;
    private EntityRef multiBlockEntity;
    private String multiBlockType;

    public MultiBlockMainComponent() {
    }

    public MultiBlockMainComponent(List<Vector3i> multiBlockMembers, Region3i aabb, EntityRef multiBlockEntity,
                                   String multiBlockType) {
        this.multiBlockMembers = multiBlockMembers;
        this.aabb = aabb;
        this.multiBlockEntity = multiBlockEntity;
        this.multiBlockType = multiBlockType;
    }

    public Collection<Vector3i> getMultiBlockMembers() {
        return Collections.unmodifiableList(multiBlockMembers);
    }

    public Region3i getAabb() {
        return aabb;
    }

    public EntityRef getMultiBlockEntity() {
        return multiBlockEntity;
    }

    public void setMultiBlockEntity(EntityRef multiBlockEntity) {
        this.multiBlockEntity = multiBlockEntity;
    }

    public String getMultiBlockType() {
        return multiBlockType;
    }
}
