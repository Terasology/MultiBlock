// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock2;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.math.geom.Vector3i;
import org.terasology.multiBlock2.recipe.MultiBlockRecipe;

public interface MultiBlockRegistry {
    void registerMultiBlockType(String multiBlockCandidate, MultiBlockRecipe<?> multiBlockRecipe);

    EntityRef getMultiBlockAtLocation(Vector3i location, String type);
}
