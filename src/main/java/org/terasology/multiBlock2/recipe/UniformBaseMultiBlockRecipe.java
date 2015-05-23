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
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.math.Region3i;
import org.terasology.math.geom.Vector3i;
import org.terasology.multiBlock2.MultiBlockDefinition;
import org.terasology.world.BlockEntityRegistry;

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
    public T detectFormingMultiBlock(Vector3i location) {
        EntityRef target = blockEntityRegistry.getBlockEntityAt(location);

        if (!baseEntityPredicate.apply(target)) {
            return null;
        }

        int minX = getLastMatchingInDirection(target, location, Vector3i.east()).x;
        int maxX = getLastMatchingInDirection(target, location, Vector3i.west()).x;
        int minY = getLastMatchingInDirection(target, location, Vector3i.down()).y;
        int maxY = getLastMatchingInDirection(target, location, Vector3i.up()).y;
        int minZ = getLastMatchingInDirection(target, location, Vector3i.south()).z;
        int maxZ = getLastMatchingInDirection(target, location, Vector3i.north()).z;

        Region3i multiBlockRegion = Region3i.createBounded(new Vector3i(minX, minY, minZ), new Vector3i(maxX, maxY, maxZ));

        // Check if the size is accepted
        if (!sizeFilter.apply(multiBlockRegion.size())) {
            return null;
        }

        // Now check that all the blocks in the region defined by these boundaries match the criteria
        for (Vector3i blockLocation : multiBlockRegion) {
            if (!baseEntityPredicate.apply(blockEntityRegistry.getBlockEntityAt(blockLocation))) {
                return null;
            }
        }

        return createMultiBlockDefinition(multiBlockRegion);
    }

    protected abstract T createMultiBlockDefinition(Region3i multiBlockRegion);

    private Vector3i getLastMatchingInDirection(EntityRef targetEntity, Vector3i location, Vector3i direction) {
        Vector3i result = location;
        while (true) {
            Vector3i testedLocation = new Vector3i(result.x + direction.x, result.y + direction.y, result.z + direction.z);
            EntityRef blockEntityAt = blockEntityRegistry.getBlockEntityAt(testedLocation);
            if (!otherEntitiesPredicate.test(targetEntity, blockEntityAt)) {
                return result;
            }
            result = testedLocation;
        }
    }
}
