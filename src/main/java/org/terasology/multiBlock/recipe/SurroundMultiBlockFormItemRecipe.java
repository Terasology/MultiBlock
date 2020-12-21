/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.multiBlock.recipe;

import com.google.common.base.Predicate;
import org.joml.Vector3f;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.JomlUtil;
import org.terasology.math.Region3i;
import org.terasology.math.geom.Vector3i;
import org.terasology.multiBlock.MultiBlockCallback;
import org.terasology.multiBlock.MultiBlockFormed;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockRegion;
import org.terasology.world.block.BlockRegions;
import org.terasology.world.block.entity.placement.PlaceBlocks;
import org.terasology.world.block.regions.BlockRegionComponent;

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

        Vector3i blockPosition = targetBlock.getPosition();
        BlockEntityRegistry blockEntityRegistry = CoreRegistry.get(BlockEntityRegistry.class);

        // Go to minX, minY, minZ
        int minX = getLastMatchingInDirection(blockEntityRegistry, blockPosition, Vector3i.east()).x;
        int minY = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(minX, blockPosition.y, blockPosition.z), Vector3i.down()).y;
        int minZ = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(minX, minY, blockPosition.z), Vector3i.south()).z;

        // Since we might have been in the mid of X wall, we need to find another minX:
        minX = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(minX, minY, minZ), Vector3i.east()).x;

        // Now lets find maxX, maxY and maxZ
        int maxX = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(minX, minY, minZ), Vector3i.west()).x;
        int maxY = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(maxX, minY, minZ), Vector3i.up()).y;
        int maxZ = getLastMatchingInDirection(blockEntityRegistry, new Vector3i(maxX, maxY, minZ), Vector3i.north()).z;

        // Now check that all the blocks in the region defined by these boundaries match the criteria
        BlockRegion outsideBlockRegion = BlockRegions.encompassing(new org.joml.Vector3i(minX, minY, minZ), new org.joml.Vector3i(maxX, maxY, maxZ));

        if (!sizeFilter.apply(outsideBlockRegion.getSize(new org.joml.Vector3i()))) {
            return false;
        }

        BlockRegion insideBlockRegion = BlockRegions.encompassing(new org.joml.Vector3i(minX + 1, minY + 1, minZ + 1), new org.joml.Vector3i(maxX - 1, maxY - 1, maxZ - 1));
        for (org.joml.Vector3ic blockLocation : BlockRegions.iterableInPlace(outsideBlockRegion)) {
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
        multiBlockEntity.addComponent(new LocationComponent(JomlUtil.from(outsideBlockRegion.center(new Vector3f()))));

        callback.multiBlockFormed(outsideBlockRegion, multiBlockEntity, null);

        multiBlockEntity.send(new MultiBlockFormed(event.getInstigator()));

        return true;
    }


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
