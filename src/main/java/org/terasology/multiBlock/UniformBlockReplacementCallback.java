// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.math.Region3i;
import org.terasology.engine.world.block.Block;
import org.terasology.math.geom.Vector3i;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Marcin Sciesinski <marcins78@gmail.com>
 */
public class UniformBlockReplacementCallback<T> implements MultiBlockCallback<T> {
    private final Block block;

    public UniformBlockReplacementCallback(Block block) {
        this.block = block;
    }

    @Override
    public Map<Vector3i, Block> getReplacementMap(Region3i region, T designDetails) {
        Map<Vector3i, Block> result = new HashMap<>();
        for (Vector3i location : region) {
            result.put(location, block);
        }

        return result;
    }

    @Override
    public void multiBlockFormed(Region3i region, EntityRef entity, T designDetails) {
    }
}
