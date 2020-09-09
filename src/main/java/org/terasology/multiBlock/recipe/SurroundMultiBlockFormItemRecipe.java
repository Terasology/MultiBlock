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
public class SurroundMultiBlockFormItemRecipe implements MultiBlockFormItemRecipe {
    private final Predicate<EntityRef> activator;
    private final Predicate<EntityRef> outsideBlock;
    private final Predicate<EntityRef> insideBlock;
    private final Predicate<Vector3i> sizeFilter;
    private final Predicate<ActivateEvent> activateEventFilter;
    private final MultiBlockCallback<Void> callback;
    private final String prefab;

    public SurroundMultiBlockFormItemRecipe(Predicate<EntityRef> activator, Predicate<EntityRef> outsideBlock,
                                            Predicate<EntityRef> insideBlock,
                                            Predicate<Vector3i> sizeFilter,
                                            Predicate<ActivateEvent> activateEventFilter,
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

        Vector3i blockPosition = targetBlock.getPosition();
        BlockEntityRegistry blockEntityRegistry = CoreRegistry.get(BlockEntityRegistry.class);

        // Go to minX, minY, minZ
        int minX = getLastMatchingInDirection(blockEntityRegistry, blockPosition, Vector3i.east()).x;
        int minY = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(minX, blockPosition.y,
                blockPosition.z), Vector3i.down()).y;
        int minZ = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(minX, minY, blockPosition.z),
                Vector3i.south()).z;

        // Since we might have been in the mid of X wall, we need to find another minX:
        minX = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(minX, minY, minZ), Vector3i.east()).x;

        // Now lets find maxX, maxY and maxZ
        int maxX = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(minX, minY, minZ), Vector3i.west()).x;
        int maxY = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(maxX, minY, minZ), Vector3i.up()).y;
        int maxZ = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(maxX, maxY, minZ), Vector3i.north()).z;

        // Now check that all the blocks in the region defined by these boundaries match the criteria
        Region3i outsideBlockRegion = Region3i.createBounded(new Vector3i(minX, minY, minZ), new Vector3i(maxX, maxY,
                maxZ));

        if (!sizeFilter.apply(outsideBlockRegion.size())) {
            return false;
        }

        Region3i insideBlockRegion = Region3i.createBounded(new Vector3i(minX + 1, minY + 1, minZ + 1),
                new Vector3i(maxX - 1, maxY - 1, maxZ - 1));
        for (Vector3i blockLocation : outsideBlockRegion) {
            EntityRef blockEntity = blockEntityRegistry.getBlockEntityAt(blockLocation);
            if (insideBlockRegion.encompasses(blockLocation)) {
                if (!insideBlock.apply(blockEntity)) {
                    return false;
                }
            } else if (!outsideBlock.apply(blockEntity)) {
                return false;
            }
        }

        // Ok, we got matching blocks now we can form the multi-block
        Map<Vector3i, Block> replacementBlockMap = callback.getReplacementMap(outsideBlockRegion, null);

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
        multiBlockEntity.addComponent(new LocationComponent(outsideBlockRegion.center()));

        callback.multiBlockFormed(outsideBlockRegion, multiBlockEntity, null);

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
            if (!outsideBlock.apply(blockEntityAt)) {
                return result;
            }
            result = testedLocation;
        }
    }
}
