// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock2.component;

import org.joml.Vector3i;
import org.terasology.engine.world.block.ForceBlockActive;
import org.terasology.gestalt.entitysystem.component.Component;

/**
 * Not for external use!
 */
@ForceBlockActive
public class MultiBlockMemberComponent implements Component<MultiBlockMemberComponent> {
    public Vector3i mainBlockLocation;

    public MultiBlockMemberComponent() {
    }

    public MultiBlockMemberComponent(Vector3i mainBlockLocation) {
        this.mainBlockLocation = mainBlockLocation;
    }

    public Vector3i getMainBlockLocation() {
        return mainBlockLocation;
    }

    @Override
    public void copyFrom(MultiBlockMemberComponent other) {
        this.mainBlockLocation = new Vector3i(other.mainBlockLocation);
    }
}
