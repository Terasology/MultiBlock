// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock2.component;

import org.joml.Vector3i;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.world.block.BlockRegion;
import org.terasology.engine.world.block.BlockRegionc;
import org.terasology.engine.world.block.ForceBlockActive;
import org.terasology.gestalt.entitysystem.component.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Not for external use!
 */
@ForceBlockActive
public class MultiBlockMainComponent implements Component<MultiBlockMainComponent> {
    private List<Vector3i> multiBlockMembers;
    private BlockRegion aabb;
    private EntityRef multiBlockEntity;
    private String multiBlockType;

    public MultiBlockMainComponent() {
    }

    public MultiBlockMainComponent(List<Vector3i> multiBlockMembers, BlockRegionc aabb, EntityRef multiBlockEntity, String multiBlockType) {
        this.multiBlockMembers = multiBlockMembers;
        this.aabb = new BlockRegion(aabb);
        this.multiBlockEntity = multiBlockEntity;
        this.multiBlockType = multiBlockType;
    }

    public Collection<Vector3i> getMultiBlockMembers() {
        return Collections.unmodifiableList(multiBlockMembers);
    }

    public BlockRegionc getAabb() {
        return aabb;
    }

    public EntityRef getMultiBlockEntity() {
        return multiBlockEntity;
    }

    public String getMultiBlockType() {
        return multiBlockType;
    }

    public void setMultiBlockEntity(EntityRef multiBlockEntity) {
        this.multiBlockEntity = multiBlockEntity;
    }

    @Override
    public void copy(MultiBlockMainComponent other) {
        this.aabb = new BlockRegion(other.aabb);
        this.multiBlockEntity = other.multiBlockEntity;
        this.multiBlockMembers = other.multiBlockMembers.stream()
                .map(Vector3i::new)
                .collect(Collectors.toList());
        this.multiBlockType = other.multiBlockType;
    }
}
