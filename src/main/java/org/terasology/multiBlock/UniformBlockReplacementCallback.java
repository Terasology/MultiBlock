// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock;

import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockRegion;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Marcin Sciesinski <marcins78@gmail.com>
 */
public class UniformBlockReplacementCallback<T> implements MultiBlockCallback<T> {
    private Block block;

    public UniformBlockReplacementCallback(Block block) {
        this.block = block;
    }

    @Override
    public Map<Vector3i, Block> getReplacementMap(BlockRegion region, T designDetails) {
        Map<Vector3i, Block> result = new HashMap<>();
        for (Vector3ic location : region) {
            result.put(new Vector3i(location), block);
        }

        return result;
    }

    @Override
    public void multiBlockFormed(BlockRegion region, EntityRef entity, T designDetails) {
    }
}
