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
public class SurroundMultiBlockFormItemRecipe implements MultiBlockFormItemRecipe {
    private Predicate<EntityRef> activator;
    private Predicate<EntityRef> outsideBlock;
    private Predicate<EntityRef> insideBlock;
    private Predicate<org.joml.Vector3i> sizeFilter;
    private Predicate<ActivateEvent> activateEventFilter;
    private MultiBlockCallback<Void> callback;
    private String prefab;

    public SurroundMultiBlockFormItemRecipe(Predicate<EntityRef> activator, Predicate<EntityRef> outsideBlock, Predicate<EntityRef> insideBlock,
                                            Predicate<org.joml.Vector3i> sizeFilter, Predicate<ActivateEvent> activateEventFilter,
                                            String prefab, MultiBlockCallback<Void> callback) {
        this.activator = activator;
        this.outsideBlock = outsideBlock;
        this.insideBlock = insideBlock;
        this.sizeFilter = sizeFilter;
        this.activateEventFilter = activateEventFilter;
        this.callback = callback;
        this.prefab = prefab;
    }

    @Override
    public boolean isActivator(EntityRef item) {
        return activator.apply(item);
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

        if (!outsideBlock.apply(target)) {
            return false;
        }

        Vector3i blockPosition = targetBlock.getPosition(new Vector3i());
        BlockEntityRegistry blockEntityRegistry = CoreRegistry.get(BlockEntityRegistry.class);

        // Go to minX, minY, minZ
        int minX = getLastMatchingInDirection(blockEntityRegistry, blockPosition, Direction.RIGHT.asVector3i()).x();
        int minY = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(minX, blockPosition.y, blockPosition.z), Direction.DOWN.asVector3i()).y();
        int minZ = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(minX, minY, blockPosition.z), Direction.BACKWARD.asVector3i()).z();

        // Since we might have been in the mid of X wall, we need to find another minX:
        minX = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(minX, minY, minZ), Direction.RIGHT.asVector3i()).x();

        // Now lets find maxX, maxY and maxZ
        int maxX = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(minX, minY, minZ), Direction.LEFT.asVector3i()).x();
        int maxY = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(maxX, minY, minZ), Direction.UP.asVector3i()).y();
        int maxZ = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(maxX, maxY, minZ), Direction.FORWARD.asVector3i()).z();

        // Now check that all the blocks in the region defined by these boundaries match the criteria
        BlockRegion outsideBlockRegion = new BlockRegion(minX, minY, minZ).union(maxX, maxY, maxZ);

        if (!sizeFilter.apply(outsideBlockRegion.getSize(new org.joml.Vector3i()))) {
            return false;
        }

        BlockRegion insideBlockRegion =
            new BlockRegion(minX + 1, minY + 1, minZ + 1).union(maxX - 1, maxY - 1, maxZ - 1);
        for (org.joml.Vector3ic blockLocation : outsideBlockRegion) {
            EntityRef blockEntity = blockEntityRegistry.getBlockEntityAt(blockLocation);
            if (insideBlockRegion.contains(blockLocation)) {
                if (!insideBlock.apply(blockEntity)) {
                    return false;
                }
            } else if (!outsideBlock.apply(blockEntity)) {
                return false;
            }
        }

        // Ok, we got matching blocks now we can form the multi-block
        Map<org.joml.Vector3i, Block> replacementBlockMap = callback.getReplacementMap(outsideBlockRegion, null);

        if (replacementBlockMap != null) {
            WorldProvider worldProvider = CoreRegistry.get(WorldProvider.class);

            // First, replace the blocks in world
            PlaceBlocks placeBlocksEvent = new PlaceBlocks(replacementBlockMap, event.getInstigator());
            worldProvider.getWorldEntity().send(placeBlocksEvent);

            if (placeBlocksEvent.isConsumed()) {
                return false;
            }
        }

        // Create the block region entity
        EntityManager entityManager = CoreRegistry.get(EntityManager.class);
        EntityRef multiBlockEntity = entityManager.create(prefab);
        multiBlockEntity.addComponent(new BlockRegionComponent(outsideBlockRegion));
        multiBlockEntity.addComponent(new LocationComponent(outsideBlockRegion.center(new Vector3f())));

        callback.multiBlockFormed(outsideBlockRegion, multiBlockEntity, null);

        multiBlockEntity.send(new MultiBlockFormed(event.getInstigator()));

        return true;
    }


    private Vector3ic getLastMatchingInDirection(BlockEntityRegistry blockEntityRegistry, Vector3ic location, Vector3ic direction) {
        Vector3ic result = location;
        while (true) {
            Vector3i testedLocation = new Vector3i(result.x() + direction.x(), result.y() + direction.y(), result.z() + direction.z());
            EntityRef blockEntityAt = blockEntityRegistry.getBlockEntityAt(testedLocation);
            if (!outsideBlock.apply(blockEntityAt)) {
                return result;
            }
            result = testedLocation;
        }
    }
}
