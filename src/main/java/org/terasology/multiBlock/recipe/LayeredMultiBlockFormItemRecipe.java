// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock.recipe;

import com.google.common.base.Predicate;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.logic.common.ActivateEvent;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.math.Region3i;
import org.terasology.engine.registry.CoreRegistry;
import org.terasology.engine.world.BlockEntityRegistry;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.engine.world.block.entity.placement.PlaceBlocks;
import org.terasology.engine.world.block.regions.BlockRegionComponent;
import org.terasology.math.geom.Vector2i;
import org.terasology.math.geom.Vector3i;
import org.terasology.multiBlock.MultiBlockCallback;
import org.terasology.multiBlock.MultiBlockFormed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marcin Sciesinski <marcins78@gmail.com>
 */
public class LayeredMultiBlockFormItemRecipe implements MultiBlockFormItemRecipe {
    private final Predicate<EntityRef> itemFilter;
    private final Predicate<Vector2i> sizeFilter;
    private final Predicate<ActivateEvent> activateEventFilter;
    private final String prefab;
    private final MultiBlockCallback<int[]> callback;

    private final List<LayerDefinition> layerDefinitions = new ArrayList<>();

    public LayeredMultiBlockFormItemRecipe(Predicate<EntityRef> itemFilter, Predicate<Vector2i> sizeFilter,
                                           Predicate<ActivateEvent> activateEventFilter, String prefab,
                                           MultiBlockCallback<int[]> callback) {
        this.itemFilter = itemFilter;
        this.sizeFilter = sizeFilter;
        this.activateEventFilter = activateEventFilter;
        this.prefab = prefab;
        this.callback = callback;
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
                if (processDetectionForLayer(event, i, targetBlock.getPosition())) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean processDetectionForLayer(ActivateEvent event, int layerIndex, Vector3i basePosition) {
        BlockEntityRegistry blockEntityRegistry = CoreRegistry.get(BlockEntityRegistry.class);
        LayerDefinition layerDefinition = layerDefinitions.get(layerIndex);
        Predicate<EntityRef> entityFilter = layerDefinition.entityFilter;
        int minX = getLastMatchingInDirection(blockEntityRegistry, entityFilter, basePosition, Vector3i.east()).x;
        int maxX = getLastMatchingInDirection(blockEntityRegistry, entityFilter, basePosition, Vector3i.west()).x;
        int minZ = getLastMatchingInDirection(blockEntityRegistry, entityFilter, basePosition, Vector3i.south()).z;
        int maxZ = getLastMatchingInDirection(blockEntityRegistry, entityFilter, basePosition, Vector3i.north()).z;

        // First check if the size is accepted at all
        Vector2i multiBlockHorizontalSize = new Vector2i(maxX - minX + 1, maxZ - minZ + 1);
        if (!sizeFilter.apply(multiBlockHorizontalSize)) {
            return false;
        }

        int minY = getLastMatchingInDirection(blockEntityRegistry, entityFilter, basePosition, Vector3i.down()).y;
        int maxY = getLastMatchingInDirection(blockEntityRegistry, entityFilter, basePosition, Vector3i.up()).y;

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
            int lastMatchingY = getLastMatchingInDirection(blockEntityRegistry, upLayerDefinition.entityFilter,
                    new Vector3i(basePosition.x, lastLayerYUp, basePosition.z), Vector3i.up()).y;
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
            int lastMatchingY = getLastMatchingInDirection(blockEntityRegistry, downLayerDefinition.entityFilter,
                    new Vector3i(basePosition.x, lastLayerYUp, basePosition.z), Vector3i.down()).y;
            // Layer height
            int downLayerHeight = lastLayerYDown - lastMatchingY;
            if (downLayerDefinition.minHeight > downLayerHeight || downLayerDefinition.maxHeight < downLayerHeight) {
                return false;
            }
            layerHeights[i] = downLayerHeight;
            lastLayerYDown -= downLayerHeight;
        }

        // We detected the boundaries of the possible multi-block, now we need to validate that all blocks in the 
        // region (for each layer) match
        int validationY = lastLayerYDown;
        for (int i = 0; i < layerHeights.length; i++) {
            if (layerHeights[i] > 0) {
                Region3i layerRegion = Region3i.createBounded(new Vector3i(minX, validationY, minZ),
                        new Vector3i(maxX, validationY + layerHeights[i] - 1, maxZ));
                LayerDefinition validateLayerDefinition = layerDefinitions.get(i);
                for (Vector3i position : layerRegion) {
                    if (!validateLayerDefinition.entityFilter.apply(blockEntityRegistry.getBlockEntityAt(position))) {
                        return false;
                    }
                }
                validationY += layerHeights[i];
            }
        }

        Region3i multiBlockRegion = Region3i.createBounded(new Vector3i(minX, lastLayerYDown, minZ),
                new Vector3i(maxX, lastLayerYUp, maxZ));

        if (callback != null) {
            Map<Vector3i, Block> replacementMap = callback.getReplacementMap(multiBlockRegion, layerHeights);

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
        multiBlockEntity.addComponent(new LocationComponent(multiBlockRegion.center()));

        if (callback != null) {
            callback.multiBlockFormed(multiBlockRegion, multiBlockEntity, layerHeights);
        }

        multiBlockEntity.send(new MultiBlockFormed(event.getInstigator()));

        return true;
    }

    private Vector3i getLastMatchingInDirection(BlockEntityRegistry blockEntityRegistry,
                                                Predicate<EntityRef> entityFilter, Vector3i location,
                                                Vector3i direction) {
        Vector3i result = location;
        while (true) {
            Vector3i testedLocation = new Vector3i(result.x + direction.x, result.y + direction.y,
                    result.z + direction.z);
            EntityRef blockEntityAt = blockEntityRegistry.getBlockEntityAt(testedLocation);
            if (!entityFilter.apply(blockEntityAt)) {
                return result;
            }
            result = testedLocation;
        }
    }

    private static final class LayerDefinition {
        private final int minHeight;
        private final int maxHeight;
        private final Predicate<EntityRef> entityFilter;

        private LayerDefinition(int minHeight, int maxHeight, Predicate<EntityRef> entityFilter) {
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.entityFilter = entityFilter;
        }
    }
}
