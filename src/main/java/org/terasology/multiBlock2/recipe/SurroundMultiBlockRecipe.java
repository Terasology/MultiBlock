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
    public T detectFormingMultiBlock(Vector3ic location) {
        EntityRef target = blockEntityRegistry.getBlockEntityAt(location);

        if (!outsideBlock.apply(target)) {
            return null;
        }

        // Go to minX, minY, minZ
        int minX = getLastMatchingInDirection( location, Direction.RIGHT.asVector3i()).x;
        int minY = getLastMatchingInDirection( new Vector3i(minX, location.y(), location.z()), Direction.DOWN.asVector3i()).y;
        int minZ = getLastMatchingInDirection( new Vector3i(minX, minY, location.z()), Direction.BACKWARD.asVector3i()).z;

        // Since we might have been in the mid of X wall, we need to find another minX:
        minX = getLastMatchingInDirection(new Vector3i(minX, minY, minZ), Direction.RIGHT.asVector3i()).x;

        // Now lets find maxX, maxY and maxZ
        int maxX = getLastMatchingInDirection(new Vector3i(minX, minY, minZ), Direction.LEFT.asVector3i()).x;
        int maxY = getLastMatchingInDirection(new Vector3i(maxX, minY, minZ), Direction.UP.asVector3i()).y;
        int maxZ = getLastMatchingInDirection(new Vector3i(maxX, maxY, minZ), Direction.FORWARD.asVector3i()).z;

        // Now check that all the blocks in the region defined by these boundaries match the criteria
        BlockRegion outsideBlockRegion = new BlockRegion(minX, minY, minZ).union(maxX, maxY, maxZ);

        if (!sizeFilter.apply(outsideBlockRegion.getSize(new Vector3i()))) {
            return null;
        }

        BlockRegion insideBlockRegion = new BlockRegion(minX + 1, minY + 1, minZ + 1).union(maxX - 1, maxY - 1, maxZ - 1);
        for (Vector3ic blockLocation : outsideBlockRegion) {
            EntityRef blockEntity = blockEntityRegistry.getBlockEntityAt(blockLocation);
            if (insideBlockRegion.contains(blockLocation)) {
                if (!insideBlock.apply(blockEntity)) {
                    return null;
                }
            } else if (!outsideBlock.apply(blockEntity)) {
                return null;
            }
        }

        return createMultiBlockDefinition(outsideBlockRegion);
    }

    protected abstract T createMultiBlockDefinition(BlockRegionc region);

    private Vector3i getLastMatchingInDirection(Vector3ic location, Vector3ic direction) {
        Vector3i result = new Vector3i(location);
        while (true) {
            Vector3i testedLocation = result.add(direction, new Vector3i());
            EntityRef blockEntityAt = blockEntityRegistry.getBlockEntityAt(testedLocation);
            if (!outsideBlock.apply(blockEntityAt)) {
                return result;
            }
            result = testedLocation;
        }
    }
}
