// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock2.recipe;

import org.terasology.math.geom.Vector3i;
import org.terasology.multiBlock2.MultiBlockDefinition;

/**
 * Detects if a MultiBlock should be formed by placing a block at the specified location. It is invoked only if the
 * block matches the MultiBlockCandidate type matches the one defined for this recipe in the MultiBlockRegistry.
 * <p>
 * If a MultiBlock could be created for this specific block placement, a non null MultiBlockDefinition should be
 * returned containing all the required data. In addition this class can contain some extra information about this multi
 * block that will be passed to MultiBlockFormed event.
 * <p>
 * If a MultiBlock should not be created for this specific block placement - the method of this interface should return
 * a <code>null</code> value.
 *
 * @param <T>
 */
public interface MultiBlockRecipe<T extends MultiBlockDefinition> {
    T detectFormingMultiBlock(Vector3i location);
}
