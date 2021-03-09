/*
 * Copyright 2015 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.multiBlock2.component;

import org.joml.Vector3i;
import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.world.block.BlockRegion;
import org.terasology.engine.world.block.BlockRegionc;
import org.terasology.engine.world.block.ForceBlockActive;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Not for external use!
 */
@ForceBlockActive
public class MultiBlockMainComponent implements Component {
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
}
