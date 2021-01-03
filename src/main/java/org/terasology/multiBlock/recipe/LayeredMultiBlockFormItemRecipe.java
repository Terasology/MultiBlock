// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock.recipe;

import com.google.common.base.Predicate;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.Direction;
import org.terasology.multiBlock.MultiBlockCallback;
import org.terasology.multiBlock.MultiBlockFormed;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockRegion;
import org.terasology.world.block.entity.placement.PlaceBlocks;
import org.terasology.world.block.regions.BlockRegionComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marcin Sciesinski <marcins78@gmail.com>
 */
public class LayeredMultiBlockFormItemRecipe implements MultiBlockFormItemRecipe {
    private Predicate<EntityRef> itemFilter;
    private Predicate<Vector2i> sizeFilter;
    private Predicate<ActivateEvent> activateEventFilter;
    private String prefab;
    private MultiBlockCallback<int[]> callback;

    private List<LayerDefinition> layerDefinitions = new ArrayList<>();

    private BlockEntityRegistry blockEntityRegistry;

    public LayeredMultiBlockFormItemRecipe(Predicate<EntityRef> itemFilter, Predicate<Vector2i> sizeFilter,
                                           Predicate<ActivateEvent> activateEventFilter, String prefab, MultiBlockCallback<int[]> callback) {
        this.itemFilter = itemFilter;
        this.sizeFilter = sizeFilter;
        this.activateEventFilter = activateEventFilter;
        this.prefab = prefab;
        this.callback = callback;
        this.blockEntityRegistry = CoreRegistry.get(BlockEntityRegistry.class);
    }

    @Override
    public boolean isActivator(EntityRef item) {
        return itemFilter.apply(item);
    }

    public void addLayer(int minHeight, int maxHeight, Predicate<EntityRef> entityFilter) {
        if (minHeight > maxHeight || minHeight < 0) {
            throw new IllegalArgumentException("Invalid values for minHeight and maxHeight");
        }
        layerDefinitions.add(new LayerDefinition(minHeight, maxHeight, entityFilter));
    }

    @Override
    public boolean processActivation(ActivateEvent event) {
        if (!activateEventFilter.apply(event)) {
            return false;
        }

        EntityRef target = event.getTarget();
        BlockComponent targetBlock = target.getComponent(BlockComponent.class);
        if (targetBlock == null) {
            return false;
        }

        for (int i = 0; i < layerDefinitions.size(); i++) {
            LayerDefinition layerDefinition = layerDefinitions.get(i);
            if (layerDefinition.entityFilter.apply(target)) {
                if (processDetectionForLayer(event, i, targetBlock.getPosition(new Vector3i()))) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean processDetectionForLayer(ActivateEvent event, int layerIndex, Vector3i basePosition) {
        LayerDefinition layerDefinition = layerDefinitions.get(layerIndex);
        Predicate<EntityRef> entityFilter = layerDefinition.entityFilter;
        int minX = getLastMatchingInDirection( entityFilter, basePosition, Direction.RIGHT.asVector3i()).x;
        int maxX = getLastMatchingInDirection( entityFilter, basePosition, Direction.LEFT.asVector3i()).x;
        int minZ = getLastMatchingInDirection( entityFilter, basePosition, Direction.BACKWARD.asVector3i()).z;
        int maxZ = getLastMatchingInDirection( entityFilter, basePosition, Direction.FORWARD.asVector3i()).z;

        // First check if the size is accepted at all
        Vector2i multiBlockHorizontalSize = new Vector2i(maxX - minX + 1, maxZ - minZ + 1);
        if (!sizeFilter.apply(multiBlockHorizontalSize)) {
            return false;
        }

        int minY = getLastMatchingInDirection( entityFilter, basePosition, Direction.DOWN.asVector3i()).y;
        int maxY = getLastMatchingInDirection( entityFilter, basePosition,  Direction.UP.asVector3i()).y;

        // Then check if this layer height is accepted
        int layerHeight = maxY - minY + 1;
        if (layerDefinition.minHeight > layerHeight || layerDefinition.maxHeight < layerHeight) {
            return false;
        }

        int[] layerHeights = new int[layerDefinitions.size()];
        layerHeights[layerIndex] = layerHeight;

        // Go up the stack and match layers
        int lastLayerYUp = maxY;
        for (int i = layerIndex + 1; i < layerDefinitions.size(); i++) {
            LayerDefinition upLayerDefinition = layerDefinitions.get(i);
            int lastMatchingY = getLastMatchingInDirection(upLayerDefinition.entityFilter,
                    new Vector3i(basePosition.x, lastLayerYUp, basePosition.z), Direction.UP.asVector3i()).y;
            // Layer height
            int upLayerHeight = lastMatchingY - lastLayerYUp;
            if (upLayerDefinition.minHeight > upLayerHeight || upLayerDefinition.maxHeight < upLayerHeight) {
                return false;
            }
            layerHeights[i] = upLayerHeight;
            lastLayerYUp += upLayerHeight;
        }

        // Go down the stack and match layers
        int lastLayerYDown = minY;
        for (int i = layerIndex - 1; i >= 0; i--) {
            LayerDefinition downLayerDefinition = layerDefinitions.get(i);
            int lastMatchingY = getLastMatchingInDirection(downLayerDefinition.entityFilter,
                    new Vector3i(basePosition.x, lastLayerYUp, basePosition.z), Direction.DOWN.asVector3i()).y;
            // Layer height
            int downLayerHeight = lastLayerYDown - lastMatchingY;
            if (downLayerDefinition.minHeight > downLayerHeight || downLayerDefinition.maxHeight < downLayerHeight) {
                return false;
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
                        return false;
                    }
                }
                validationY += layerHeights[i];
            }
        }

        BlockRegion multiBlockRegion = new BlockRegion(minX, lastLayerYDown, minZ).union(maxX, lastLayerYUp, maxZ);

        if (callback != null) {
            Map<org.joml.Vector3i, Block> replacementMap = callback.getReplacementMap(multiBlockRegion, layerHeights);

            if (replacementMap != null) {
                // Ok, now we can replace the blocks
                WorldProvider worldProvider = CoreRegistry.get(WorldProvider.class);
                EntityRef worldEntity = worldProvider.getWorldEntity();
                PlaceBlocks placeBlocksEvent = new PlaceBlocks(replacementMap, event.getInstigator());
                worldEntity.send(placeBlocksEvent);

                if (placeBlocksEvent.isConsumed()) {
                    return false;
                }
            }
        }

        // Create the block region entity
        EntityManager entityManager = CoreRegistry.get(EntityManager.class);
        EntityRef multiBlockEntity = entityManager.create(prefab);
        multiBlockEntity.addComponent(new BlockRegionComponent(multiBlockRegion));
        multiBlockEntity.addComponent(new LocationComponent(multiBlockRegion.center(new Vector3f())));

        if (callback != null) {
            callback.multiBlockFormed(multiBlockRegion, multiBlockEntity, layerHeights);
        }

        multiBlockEntity.send(new MultiBlockFormed(event.getInstigator()));

        return true;
    }

    private Vector3i getLastMatchingInDirection( Predicate<EntityRef> entityFilter, Vector3ic location, Vector3ic direction) {
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
