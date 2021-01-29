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
package org.terasology.multiBlock2.recipe;

import com.google.common.base.Predicate;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.math.Direction;
import org.terasology.multiBlock2.MultiBlockDefinition;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.block.BlockRegion;
import org.terasology.world.block.BlockRegionc;

import java.util.function.BiPredicate;

public abstract class UniformBaseMultiBlockRecipe<T extends MultiBlockDefinition> implements MultiBlockRecipe<T> {
    private BlockEntityRegistry blockEntityRegistry;
    private Predicate<EntityRef> baseEntityPredicate;
    private BiPredicate<EntityRef, EntityRef> otherEntitiesPredicate;
    private Predicate<Vector3i> sizeFilter;

    protected UniformBaseMultiBlockRecipe(BlockEntityRegistry blockEntityRegistry, Predicate<EntityRef> baseEntityPredicate,
                                          BiPredicate<EntityRef, EntityRef> otherEntitiesPredicate, Predicate<Vector3i> sizeFilter) {
        this.blockEntityRegistry = blockEntityRegistry;
        this.baseEntityPredicate = baseEntityPredicate;
        this.otherEntitiesPredicate = otherEntitiesPredicate;
        this.sizeFilter = sizeFilter;
    }

    @Override
    public T detectFormingMultiBlock(Vector3ic location) {
        EntityRef target = blockEntityRegistry.getBlockEntityAt(location);

        if (!baseEntityPredicate.apply(target)) {
            return null;
        }

        int minX = getLastMatchingInDirection(target, location, Direction.RIGHT.asVector3i()).x;
        int maxX = getLastMatchingInDirection(target, location, Direction.LEFT.asVector3i()).x;
        int minY = getLastMatchingInDirection(target, location, Direction.DOWN.asVector3i()).y;
        int maxY = getLastMatchingInDirection(target, location, Direction.UP.asVector3i()).y;
        int minZ = getLastMatchingInDirection(target, location, Direction.BACKWARD.asVector3i()).z;
        int maxZ = getLastMatchingInDirection(target, location, Direction.FORWARD.asVector3i()).z;

        BlockRegion multiBlockRegion = new BlockRegion(minX, minY, minZ).union(maxX, maxY, maxZ);

        // Check if the size is accepted
        if (!sizeFilter.apply(multiBlockRegion.getSize(new Vector3i()))) {
            return null;
        }

        // Now check that all the blocks in the region defined by these boundaries match the criteria
        for (Vector3ic blockLocation : multiBlockRegion) {
            if (!baseEntityPredicate.apply(blockEntityRegistry.getBlockEntityAt(blockLocation))) {
                return null;
            }
        }

        return createMultiBlockDefinition(multiBlockRegion);
    }

    protected abstract T createMultiBlockDefinition(BlockRegionc multiBlockRegion);

    private Vector3i getLastMatchingInDirection(EntityRef targetEntity, Vector3ic location, Vector3ic direction) {
        Vector3i result = new Vector3i(location);
        Vector3i testedLocation = new Vector3i();
        while (true) {
            result.add(direction, testedLocation);
            EntityRef blockEntityAt = blockEntityRegistry.getBlockEntityAt(testedLocation);
            if (!otherEntitiesPredicate.test(targetEntity, blockEntityAt)) {
                return result;
            }
            result.set(testedLocation);
        }
    }
}
