// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock2;

import org.terasology.math.geom.Vector3i;

import java.util.Collection;

public interface MultiBlockDefinition {
    String getMultiBlockType();

    Vector3i getMainBlock();

    Collection<Vector3i> getMemberBlocks();
}
