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
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.math.Direction;
import org.terasology.engine.registry.CoreRegistry;
import org.terasology.engine.world.BlockEntityRegistry;
import org.terasology.engine.world.block.BlockRegion;
import org.terasology.engine.world.block.BlockRegionc;
import org.terasology.multiBlock2.MultiBlockDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Marcin Sciesinski <marcins78@gmail.com>
 */
public abstract class LayeredMultiBlockRecipe<T extends MultiBlockDefinition> implements MultiBlockRecipe<T> {
    private BlockEntityRegistry blockEntityRegistry;
    private Predicate<Vector2i> sizeFilter;

    private List<LayerDefinition> layerDefinitions = new ArrayList<>();

    public LayeredMultiBlockRecipe(BlockEntityRegistry blockEntityRegistry, Predicate<Vector2i> sizeFilter) {
        this.blockEntityRegistry = blockEntityRegistry;
        this.sizeFilter = sizeFilter;
    }

    public void addLayer(int minHeight, int maxHeight, Predicate<EntityRef> entityFilter) {
        if (minHeight > maxHeight || minHeight < 0) {
            throw new IllegalArgumentException("Invalid values for minHeight and maxHeight");
        }
        layerDefinitions.add(new LayerDefinition(minHeight, maxHeight, entityFilter));
    }

    @Override
    public T detectFormingMultiBlock(Vector3ic location) {
        EntityRef target = blockEntityRegistry.getBlockEntityAt(location);

        for (int i = 0; i < layerDefinitions.size(); i++) {
            LayerDefinition layerDefinition = layerDefinitions.get(i);
            if (layerDefinition.entityFilter.apply(target)) {
                T definition = processDetectionForLayer(i, location);
                if (definition != null) {
                    return definition;
                }
            }
        }

        return null;
    }

    private T processDetectionForLayer(int layerIndex, Vector3ic basePosition) {
        BlockEntityRegistry blockEntityRegistry = CoreRegistry.get(BlockEntityRegistry.class);
        LayerDefinition layerDefinition = layerDefinitions.get(layerIndex);
        Predicate<EntityRef> entityFilter = layerDefinition.entityFilter;
        int minX = getLastMatchingInDirection(entityFilter, basePosition, Direction.RIGHT.asVector3i()).x;
        int maxX = getLastMatchingInDirection(entityFilter, basePosition, Direction.LEFT.asVector3i()).x;
        int minZ = getLastMatchingInDirection(entityFilter, basePosition, Direction.BACKWARD.asVector3i()).z;
        int maxZ = getLastMatchingInDirection(entityFilter, basePosition, Direction.FORWARD.asVector3i()).z;

        // First check if the size is accepted at all
        Vector2i multiBlockHorizontalSize = new Vector2i(maxX - minX + 1, maxZ - minZ + 1);
        if (!sizeFilter.apply(multiBlockHorizontalSize)) {
            return null;
        }

        int minY = getLastMatchingInDirection(entityFilter, basePosition, Direction.DOWN.asVector3i()).y;
        int maxY = getLastMatchingInDirection(entityFilter, basePosition, Direction.UP.asVector3i()).y;

        // Then check if this layer height is accepted
        int layerHeight = maxY - minY + 1;
        if (layerDefinition.minHeight > layerHeight || layerDefinition.maxHeight < layerHeight) {
            return null;
        }

        int[] layerHeights = new int[layerDefinitions.size()];
        layerHeights[layerIndex] = layerHeight;

        // Go up the stack and match layers
        int lastLayerYUp = maxY;
        for (int i = layerIndex + 1; i < layerDefinitions.size(); i++) {
            LayerDefinition upLayerDefinition = layerDefinitions.get(i);
            int lastMatchingY = getLastMatchingInDirection(upLayerDefinition.entityFilter,
                    new Vector3i(basePosition.x(), lastLayerYUp, basePosition.z()), Direction.UP.asVector3i()).y;
            // Layer height
            int upLayerHeight = lastMatchingY - lastLayerYUp;
            if (upLayerDefinition.minHeight > upLayerHeight || upLayerDefinition.maxHeight < upLayerHeight) {
                return null;
            }
            layerHeights[i] = upLayerHeight;
            lastLayerYUp += upLayerHeight;
        }

        // Go down the stack and match layers
        int lastLayerYDown = minY;
        for (int i = layerIndex - 1; i >= 0; i--) {
            LayerDefinition downLayerDefinition = layerDefinitions.get(i);
            int lastMatchingY = getLastMatchingInDirection(downLayerDefinition.entityFilter,
                    new Vector3i(basePosition.x(), lastLayerYUp, basePosition.z()), Direction.DOWN.asVector3i()).y;
            // Layer height
            int downLayerHeight = lastLayerYDown - lastMatchingY;
            if (downLayerDefinition.minHeight > downLayerHeight || downLayerDefinition.maxHeight < downLayerHeight) {
                return null;
            }
            layerHeights[i] = downLayerHeight;
            lastLayerYDown -= downLayerHeight;
        }

        // We detected the boundaries of the possible multi-block, now we need to validate that all blocks in the region (for each layer) match
        int validationY = lastLayerYDown;
        for (int i = 0; i < layerHeights.length; i++) {
            if (layerHeights[i] > 0) {
                BlockRegion layerRegion = new BlockRegion(minX, validationY, minZ).union(maxX, validationY + layerHeights[i] - 1, maxZ);
                LayerDefinition validateLayerDefinition = layerDefinitions.get(i);
                for (Vector3ic position : layerRegion) {
                    if (!validateLayerDefinition.entityFilter.apply(blockEntityRegistry.getBlockEntityAt(position))) {
                        return null;
                    }
                }
                validationY += layerHeights[i];
            }
        }

        BlockRegion multiBlockRegion = new BlockRegion(minX, lastLayerYDown, minZ).union(maxX, lastLayerYUp, maxZ);

        return createMultiBlockDefinition(multiBlockRegion, layerHeights);
    }

    protected abstract T createMultiBlockDefinition(BlockRegionc multiBlockRegion, int[] layerHeights);

    private Vector3i getLastMatchingInDirection(Predicate<EntityRef> entityFilter, Vector3ic location, Vector3ic direction) {
        Vector3i result = new Vector3i(location);
        Vector3i testedLocation = new Vector3i();
        while (true) {
            result.add(direction, testedLocation);
            EntityRef blockEntityAt = blockEntityRegistry.getBlockEntityAt(testedLocation);
            if (!entityFilter.apply(blockEntityAt)) {
                return result;
            }
            result.set(testedLocation);
        }
    }

    private static final class LayerDefinition {
        private int minHeight;
        private int maxHeight;
        private Predicate<EntityRef> entityFilter;

        private LayerDefinition(int minHeight, int maxHeight, Predicate<EntityRef> entityFilter) {
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.entityFilter = entityFilter;
        }
    }
}
