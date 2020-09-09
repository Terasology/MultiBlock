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
import org.terasology.math.geom.Vector3i;
import org.terasology.multiBlock.MultiBlockCallback;
import org.terasology.multiBlock.MultiBlockFormed;

import java.util.Map;

/**
 * @author Marcin Sciesinski <marcins78@gmail.com>
 */
public class UniformMultiBlockFormItemRecipe implements MultiBlockFormItemRecipe {
    private final Predicate<EntityRef> activatorFilter;
    private final Predicate<ActivateEvent> activateEventFilter;
    private final Predicate<EntityRef> blockFilter;
    private final Predicate<Vector3i> sizeFilter;
    private final String prefab;
    private final MultiBlockCallback<Void> callback;

    public UniformMultiBlockFormItemRecipe(Predicate<EntityRef> activatorFilter,
                                           Predicate<ActivateEvent> activateEventFilter,
                                           Predicate<EntityRef> blockFilter, Predicate<Vector3i> sizeFilter,
                                           String multiBlockPrefab, MultiBlockCallback<Void> callback) {
        this.activatorFilter = activatorFilter;
        this.activateEventFilter = activateEventFilter;
        this.blockFilter = blockFilter;
        this.sizeFilter = sizeFilter;
        this.callback = callback;
        this.prefab = multiBlockPrefab;
    }

    @Override
    public boolean isActivator(EntityRef item) {
        return activatorFilter.apply(item);
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

        if (!blockFilter.apply(target)) {
            return false;
        }

        BlockEntityRegistry blockEntityRegistry = CoreRegistry.get(BlockEntityRegistry.class);

        Vector3i blockPosition = targetBlock.getPosition();
        int minX = getLastMatchingInDirection(blockEntityRegistry, blockPosition, Vector3i.east()).x;
        int maxX = getLastMatchingInDirection(blockEntityRegistry, blockPosition, Vector3i.west()).x;
        int minY = getLastMatchingInDirection(blockEntityRegistry, blockPosition, Vector3i.down()).y;
        int maxY = getLastMatchingInDirection(blockEntityRegistry, blockPosition, Vector3i.up()).y;
        int minZ = getLastMatchingInDirection(blockEntityRegistry, blockPosition, Vector3i.south()).z;
        int maxZ = getLastMatchingInDirection(blockEntityRegistry, blockPosition, Vector3i.north()).z;

        Region3i multiBlockRegion = Region3i.createBounded(new Vector3i(minX, minY, minZ), new Vector3i(maxX, maxY,
                maxZ));

        // Check if the size is accepted
        if (!sizeFilter.apply(multiBlockRegion.size())) {
            return false;
        }

        // Now check that all the blocks in the region defined by these boundaries match the criteria
        for (Vector3i blockLocation : multiBlockRegion) {
            if (!blockFilter.apply(blockEntityRegistry.getBlockEntityAt(blockLocation))) {
                return false;
            }
        }

        // Ok, we got matching blocks now we can form the multi-block
        WorldProvider worldProvider = CoreRegistry.get(WorldProvider.class);

        Map<Vector3i, Block> replacementMap = callback.getReplacementMap(multiBlockRegion, null);

        if (replacementMap != null) {
            // First, replace the blocks in world
            PlaceBlocks placeBlocksEvent = new PlaceBlocks(replacementMap, event.getInstigator());
            worldProvider.getWorldEntity().send(placeBlocksEvent);

            if (placeBlocksEvent.isConsumed()) {
                return false;
            }
        }

        // Create the block region entity
        EntityManager entityManager = CoreRegistry.get(EntityManager.class);
        EntityRef multiBlockEntity = entityManager.create(prefab);
        multiBlockEntity.addComponent(new BlockRegionComponent(multiBlockRegion));
        multiBlockEntity.addComponent(new LocationComponent(multiBlockRegion.center()));

        callback.multiBlockFormed(multiBlockRegion, multiBlockEntity, null);

        multiBlockEntity.send(new MultiBlockFormed(event.getInstigator()));

        return true;
    }

    private Vector3i getLastMatchingInDirection(BlockEntityRegistry blockEntityRegistry, Vector3i location,
                                                Vector3i direction) {
        Vector3i result = location;
        while (true) {
            Vector3i testedLocation = new Vector3i(result.x + direction.x, result.y + direction.y,
                    result.z + direction.z);
            EntityRef blockEntityAt = blockEntityRegistry.getBlockEntityAt(testedLocation);
            if (!blockFilter.apply(blockEntityAt)) {
                return result;
            }
            result = testedLocation;
        }
    }
}
