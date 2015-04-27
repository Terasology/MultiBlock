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

import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.math.Region3i;
import org.terasology.math.geom.Vector3i;
import org.terasology.world.block.ForceBlockActive;

import java.util.Collections;
import java.util.Set;

/**
 * Not for external use!
 */
@ForceBlockActive
public class MultiBlockMainComponent implements Component {
    private Set<Vector3i> multiBlockMembers;
    private Region3i aabb;
    private EntityRef multiBlockEntity;
    private String multiBlockType;

    public MultiBlockMainComponent() {
    }

    public MultiBlockMainComponent(Set<Vector3i> multiBlockMembers, Region3i aabb, EntityRef multiBlockEntity, String multiBlockType) {
        this.multiBlockMembers = multiBlockMembers;
        this.aabb = aabb;
        this.multiBlockEntity = multiBlockEntity;
        this.multiBlockType = multiBlockType;
    }

    public Set<Vector3i> getMultiBlockMembers() {
        return Collections.unmodifiableSet(multiBlockMembers);
    }

    public Region3i getAabb() {
        return aabb;
    }

    public EntityRef getMultiBlockEntity() {
        return multiBlockEntity;
    }

    public String getMultiBlockType() {
        return multiBlockType;
    }
}
