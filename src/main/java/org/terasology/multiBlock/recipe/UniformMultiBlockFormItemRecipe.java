// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock.recipe;

import com.google.common.base.Predicate;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.logic.common.ActivateEvent;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.math.Direction;
import org.terasology.engine.registry.CoreRegistry;
import org.terasology.engine.world.BlockEntityRegistry;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.engine.world.block.BlockRegion;
import org.terasology.engine.world.block.entity.placement.PlaceBlocks;
import org.terasology.engine.world.block.regions.BlockRegionComponent;
import org.terasology.multiBlock.MultiBlockCallback;
import org.terasology.multiBlock.MultiBlockFormed;

import java.util.Map;

/**
 * @author Marcin Sciesinski <marcins78@gmail.com>
 */
public class UniformMultiBlockFormItemRecipe implements MultiBlockFormItemRecipe {
    private Predicate<EntityRef> activatorFilter;
    private Predicate<ActivateEvent> activateEventFilter;
    private Predicate<EntityRef> blockFilter;
    private Predicate<Vector3ic> sizeFilter;
    private String prefab;
    private MultiBlockCallback<Void> callback;

    public UniformMultiBlockFormItemRecipe(Predicate<EntityRef> activatorFilter, Predicate<ActivateEvent> activateEventFilter,
                                           Predicate<EntityRef> blockFilter, Predicate<Vector3ic> sizeFilter,
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

        Vector3i blockPosition = targetBlock.getPosition(new Vector3i());
        int minX = getLastMatchingInDirection(blockEntityRegistry, blockPosition, Direction.RIGHT.asVector3i()).x;
        int maxX = getLastMatchingInDirection(blockEntityRegistry, blockPosition, Direction.LEFT.asVector3i()).x;
        int minY = getLastMatchingInDirection(blockEntityRegistry, blockPosition, Direction.DOWN.asVector3i()).y;
        int maxY = getLastMatchingInDirection(blockEntityRegistry, blockPosition, Direction.UP.asVector3i()).y;
        int minZ = getLastMatchingInDirection(blockEntityRegistry, blockPosition, Direction.BACKWARD.asVector3i()).z;
        int maxZ = getLastMatchingInDirection(blockEntityRegistry, blockPosition, Direction.FORWARD.asVector3i()).z;

        BlockRegion multiBlockRegion =
                new BlockRegion(minX, minY, minZ).union(maxX, maxY, maxZ);

        // Check if the size is accepted
        if (!sizeFilter.apply(multiBlockRegion.getSize(new org.joml.Vector3i()))) {
            return false;
        }

        // Now check that all the blocks in the region defined by these boundaries match the criteria
        for (org.joml.Vector3ic blockLocation : multiBlockRegion) {
            if (!blockFilter.apply(blockEntityRegistry.getBlockEntityAt(blockLocation))) {
                return false;
            }
        }

        // Ok, we got matching blocks now we can form the multi-block
        WorldProvider worldProvider = CoreRegistry.get(WorldProvider.class);

        Map<org.joml.Vector3i, Block> replacementMap = callback.getReplacementMap(multiBlockRegion, null);

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
        multiBlockEntity.addComponent(new LocationComponent(multiBlockRegion.center(new Vector3f())));

        callback.multiBlockFormed(multiBlockRegion, multiBlockEntity, null);

        multiBlockEntity.send(new MultiBlockFormed(event.getInstigator()));

        return true;
    }

    private Vector3i getLastMatchingInDirection(BlockEntityRegistry blockEntityRegistry, Vector3ic location, Vector3ic direction) {
        Vector3i result = new Vector3i(location);
        Vector3i testLocation = new Vector3i();
        while (true) {
            result.add(direction, testLocation);
            EntityRef blockEntityAt = blockEntityRegistry.getBlockEntityAt(testLocation);
            if (!blockFilter.apply(blockEntityAt)) {
                return result;
            }
            result.set(testLocation);
        }
    }
}
