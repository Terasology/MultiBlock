// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.math.Region3i;
import org.terasology.engine.world.block.Block;
import org.terasology.math.geom.Vector3i;

import java.util.Map;

/**
 * @author Marcin Sciesinski <marcins78@gmail.com>
 */
public interface MultiBlockCallback<T> {
    Map<Vector3i, Block> getReplacementMap(Region3i region, T designDetails);

    void multiBlockFormed(Region3i region, EntityRef entity, T designDetails);
}
