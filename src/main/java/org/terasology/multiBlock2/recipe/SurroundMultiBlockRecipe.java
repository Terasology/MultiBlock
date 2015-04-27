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

/**
 * @author Marcin Sciesinski <marcins78@gmail.com>
 */
public abstract class SurroundMultiBlockRecipe<T extends MultiBlockDefinition> implements MultiBlockRecipe<T> {
    private BlockEntityRegistry blockEntityRegistry;
    private Predicate<EntityRef> outsideBlock;
    private Predicate<EntityRef> insideBlock;
    private Predicate<Vector3i> sizeFilter;

    public SurroundMultiBlockRecipe(BlockEntityRegistry blockEntityRegistry, Predicate<EntityRef> outsideBlock,
                                    Predicate<EntityRef> insideBlock, Predicate<Vector3i> sizeFilter) {
        this.blockEntityRegistry = blockEntityRegistry;
        this.outsideBlock = outsideBlock;
        this.insideBlock = insideBlock;
        this.sizeFilter = sizeFilter;
    }

    @Override
    public T detectFormingMultiBlock(Vector3i location) {
        EntityRef target = blockEntityRegistry.getBlockEntityAt(location);

        if (!outsideBlock.apply(target)) {
            return null;
        }

        // Go to minX, minY, minZ
        int minX = getLastMatchingInDirection(blockEntityRegistry, location, Vector3i.east()).x;
        int minY = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(minX, location.y, location.z), Vector3i.down()).y;
        int minZ = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(minX, minY, location.z), Vector3i.south()).z;

        // Since we might have been in the mid of X wall, we need to find another minX:
        minX = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(minX, minY, minZ), Vector3i.east()).x;

        // Now lets find maxX, maxY and maxZ
        int maxX = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(minX, minY, minZ), Vector3i.west()).x;
        int maxY = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(maxX, minY, minZ), Vector3i.up()).y;
        int maxZ = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(maxX, maxY, minZ), Vector3i.north()).z;

        // Now check that all the blocks in the region defined by these boundaries match the criteria
        Region3i outsideBlockRegion = Region3i.createBounded(new Vector3i(minX, minY, minZ), new Vector3i(maxX, maxY, maxZ));

        if (!sizeFilter.apply(outsideBlockRegion.size())) {
            return null;
        }

        Region3i insideBlockRegion = Region3i.createBounded(new Vector3i(minX + 1, minY + 1, minZ + 1), new Vector3i(maxX - 1, maxY - 1, maxZ - 1));
        for (Vector3i blockLocation : outsideBlockRegion) {
            EntityRef blockEntity = blockEntityRegistry.getBlockEntityAt(blockLocation);
            if (insideBlockRegion.encompasses(blockLocation)) {
                if (!insideBlock.apply(blockEntity)) {
                    return null;
                }
            } else if (!outsideBlock.apply(blockEntity)) {
                return null;
            }
        }

        return createMultiBlockDefinition(outsideBlockRegion);
    }

    protected abstract T createMultiBlockDefinition(Region3i region);

    private Vector3i getLastMatchingInDirection(BlockEntityRegistry blockEntityRegistry, Vector3i location, Vector3i direction) {
        Vector3i result = location;
        while (true) {
            Vector3i testedLocation = new Vector3i(result.x + direction.x, result.y + direction.y, result.z + direction.z);
            EntityRef blockEntityAt = blockEntityRegistry.getBlockEntityAt(testedLocation);
            if (!outsideBlock.apply(blockEntityAt)) {
                return result;
            }
            result = testedLocation;
        }
    }
}
