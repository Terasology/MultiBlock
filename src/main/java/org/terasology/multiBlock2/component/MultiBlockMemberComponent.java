// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock2.component;

import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.world.block.ForceBlockActive;
import org.terasology.math.geom.Vector3i;

/**
 * Not for external use!
 */
@ForceBlockActive
public class MultiBlockMemberComponent implements Component {
    private Vector3i mainBlockLocation;

    public MultiBlockMemberComponent() {
    }

    public MultiBlockMemberComponent(Vector3i mainBlockLocation) {
        this.mainBlockLocation = mainBlockLocation;
    }

    public Vector3i getMainBlockLocation() {
        return mainBlockLocation;
    }
}
