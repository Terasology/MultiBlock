// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock2.system;

import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.entitySystem.entity.EntityBuilder;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.entity.lifecycleEvents.BeforeRemoveComponent;
import org.terasology.engine.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.engine.entitySystem.entity.lifecycleEvents.OnAddedComponent;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.math.Region3i;
import org.terasology.engine.registry.In;
import org.terasology.engine.registry.Share;
import org.terasology.engine.world.BlockEntityRegistry;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.engine.world.block.entity.placement.PlaceBlocks;
import org.terasology.engine.world.block.family.BlockFamily;
import org.terasology.engine.world.chunks.ChunkConstants;
import org.terasology.engine.world.chunks.event.BeforeChunkUnload;
import org.terasology.health.logic.event.BeforeDamagedEvent;
import org.terasology.math.geom.Vector3i;
import org.terasology.multiBlock2.MultiBlockDefinition;
import org.terasology.multiBlock2.MultiBlockRegistry;
import org.terasology.multiBlock2.block.VisibilityEnabledBlockFamily;
import org.terasology.multiBlock2.component.MultiBlockCandidateComponent;
import org.terasology.multiBlock2.component.MultiBlockComponent;
import org.terasology.multiBlock2.component.MultiBlockMainComponent;
import org.terasology.multiBlock2.component.MultiBlockMemberComponent;
import org.terasology.multiBlock2.event.BeforeMultiBlockUnformed;
import org.terasology.multiBlock2.event.BeforeMultiBlockUnloaded;
import org.terasology.multiBlock2.event.MultiBlockFormed;
import org.terasology.multiBlock2.event.MultiBlockLoaded;
import org.terasology.multiBlock2.recipe.MultiBlockRecipe;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

@RegisterSystem(RegisterMode.AUTHORITY)
@Share(MultiBlockRegistry.class)
public class MultiBlockServerSystem extends BaseComponentSystem implements MultiBlockRegistry, UpdateSubscriberSystem {
    private static final Logger logger = LoggerFactory.getLogger(MultiBlockServerSystem.class);
    private final Map<String, MultiBlockRecipe<?>> multiBlockRecipeMap = new HashMap<>();
    private final Map<Region3i, EntityRef> loadedMultiBlocks = new HashMap<>();
    private final Set<Vector3i> pendingMultiBlockPartsChecks = new HashSet<>();
    @In
    private BlockEntityRegistry blockEntityRegistry;
    @In
    private WorldProvider worldProvider;
    @In
    private EntityManager entityManager;
    private boolean internallyMutating = false;

    // Horrible workaround for the fact, that system is notified about block entities being loaded via 
    // OnActivatedComponent
    // before the chunk they are in is "relevant", we need to keep querying worldProvider, until it was merged to 
    // restore
    // the multi-blocks, also oddly, this update method can be called only when some of the entities from the same chunk
    // have been loaded.
    @Override
    public void update(float delta) {
        Iterator<Vector3i> iterator = pendingMultiBlockPartsChecks.iterator();
        while (iterator.hasNext()) {
            Vector3i position = iterator.next();
            if (worldProvider.isBlockRelevant(position)) {
                EntityRef blockEntity = blockEntityRegistry.getBlockEntityAt(position);
                MultiBlockMainComponent multiBlockMain = blockEntity.getComponent(MultiBlockMainComponent.class);
                if (multiBlockMain != null) {
                    processLoadedMultiBlockMain(blockEntity, multiBlockMain, position);
                } else {
                    MultiBlockMemberComponent multiBlockMember =
                            blockEntity.getComponent(MultiBlockMemberComponent.class);
                    if (multiBlockMember != null) {
                        Vector3i mainBlockLocation = multiBlockMember.getMainBlockLocation();
                        if (worldProvider.isBlockRelevant(mainBlockLocation)) {
                            EntityRef mainBlockEntity = blockEntityRegistry.getBlockEntityAt(mainBlockLocation);
                            multiBlockMain = mainBlockEntity.getComponent(MultiBlockMainComponent.class);
                            // The entities are not loaded at the same time it seems, so I need to wait for the main one
                            // to be loaded, even if it is already relevant
                            if (multiBlockMain != null) {
                                processLoadedMultiBlockMain(mainBlockEntity, multiBlockMain, mainBlockLocation);
                                iterator.remove();
                            }
                        } else {
                            iterator.remove();
                        }
                    } else {
                        iterator.remove();
                    }
                }
            }
        }
    }

    @Override
    public void registerMultiBlockType(String multiBlockCandidate, MultiBlockRecipe<?> multiBlockRecipe) {
        multiBlockRecipeMap.put(multiBlockCandidate, multiBlockRecipe);
    }

    @Override
    public EntityRef getMultiBlockAtLocation(Vector3i location, String type) {
        for (Map.Entry<Region3i, EntityRef> region3iEntityRefEntry : loadedMultiBlocks.entrySet()) {
            if (region3iEntityRefEntry.getKey().encompasses(location)) {
                EntityRef entity = region3iEntityRefEntry.getValue();
                MultiBlockComponent multiBlock = entity.getComponent(MultiBlockComponent.class);
                EntityRef mainBlockEntity = multiBlock.getMainBlockEntity();
                MultiBlockMainComponent mainComponent = mainBlockEntity.getComponent(MultiBlockMainComponent.class);
                if (mainComponent.getMultiBlockType().equals(type)) {
                    if (mainBlockEntity.getComponent(BlockComponent.class).getPosition().equals(location)
                            || mainComponent.getMultiBlockMembers().contains(location)) {
                        return entity;
                    }
                }
                return null;
            }
        }
        return null;
    }

    @ReceiveEvent
    public void onMultiBlockCandidatePlaced(OnAddedComponent event, EntityRef entity,
                                            MultiBlockCandidateComponent candidate, BlockComponent block) {
        for (String type : candidate.getType()) {
            MultiBlockRecipe<?> recipe = multiBlockRecipeMap.get(type);
            if (recipe != null) {
                MultiBlockDefinition definition = recipe.detectFormingMultiBlock(block.getPosition());
                Set<EntityRef> multiBlockMainBlockEntities = getMultiBlockMainBlocksInTheWay(definition);
                if (areAllMultiBlocksInTheWayRelevant(multiBlockMainBlockEntities)) {
                    // Destroy all multi blocks in the way
                    for (EntityRef multiBlockMainBlockEntity : multiBlockMainBlockEntities) {
                        destroyMultiBlock(multiBlockMainBlockEntity);
                    }

                    createMultiBlock(definition);
                }
            }
        }
    }

    @ReceiveEvent
    public void onMultiBlockPartRemoved(BeforeRemoveComponent event, EntityRef entity,
                                        MultiBlockMainComponent multiBlockMain, BlockComponent block) {
        if (!internallyMutating) {
            destroyMultiBlock(entity);
        }
    }

    @ReceiveEvent
    public void onMultiBlockPartRemoved(BeforeRemoveComponent event, EntityRef entity,
                                        MultiBlockMemberComponent multiBlockMember, BlockComponent block) {
        if (!internallyMutating) {
            Vector3i mainBlockLocation = multiBlockMember.getMainBlockLocation();
            if (worldProvider.isBlockRelevant(mainBlockLocation)) {
                EntityRef mainBlockEntity = blockEntityRegistry.getBlockEntityAt(mainBlockLocation);
                destroyMultiBlock(mainBlockEntity);
            } else {
                logger.error("Part of the MultiBlock is getting removed when it's not fully loaded");
            }
        }
    }

    @ReceiveEvent
    public void beforeChunkUnloaded(BeforeChunkUnload beforeChunkUnload, EntityRef world) {
        Region3i chunkRegion = getChunkRegion(beforeChunkUnload.getChunkPos());
        Iterator<Map.Entry<Region3i, EntityRef>> iterator = loadedMultiBlocks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Region3i, EntityRef> multiBlock = iterator.next();
            if (!chunkRegion.intersect(multiBlock.getKey()).isEmpty()) {
                EntityRef multiBlockEntity = multiBlock.getValue();
                MultiBlockComponent component = multiBlockEntity.getComponent(MultiBlockComponent.class);
                multiBlockEntity.send(new BeforeMultiBlockUnloaded(component.getType(),
                        component.getMainBlockEntity()));
                iterator.remove();
                multiBlockEntity.destroy();
            }
        }
    }

    //    @ReceiveEvent
//    public void afterChunkLoaded(OnChunkLoaded chunkLoaded, EntityRef world) {
//        Region3i chunkRegion = getChunkRegion(chunkLoaded.getChunkPos());
//        for (Vector3i vector3i : chunkRegion) {
//            EntityRef blockEntity = blockEntityRegistry.getBlockEntityAt(vector3i);
//            MultiBlockMainComponent multiBlockMain = blockEntity.getComponent(MultiBlockMainComponent.class);
//            if (multiBlockMain != null) {
//                processLoadedMultiBlockMain(blockEntity, multiBlockMain, vector3i);
//            } else {
//                MultiBlockMemberComponent multiBlockMember = blockEntity.getComponent(MultiBlockMemberComponent
//                .class);
//                if (multiBlockMember != null && worldProvider.isBlockRelevant(multiBlockMember.getMainBlockLocation
//                ())) {
//                    Vector3i mainBlockLocation = multiBlockMember.getMainBlockLocation();
//                    EntityRef mainBlockEntity = blockEntityRegistry.getBlockEntityAt(mainBlockLocation);
//                    processLoadedMultiBlockMain(mainBlockEntity, mainBlockEntity.getComponent
//                    (MultiBlockMainComponent.class), mainBlockLocation);
//                }
//            }
//        }
//    }
//
    @ReceiveEvent
    public void onMultiBlockBeingLoaded(OnActivatedComponent event, EntityRef entity,
                                        MultiBlockMainComponent multiBlockMain, BlockComponent block) {
        if (!internallyMutating) {
            pendingMultiBlockPartsChecks.add(block.getPosition());
//            processLoadedMultiBlockMain(entity, multiBlockMain,  block.getPosition());
        }
    }

    @ReceiveEvent
    public void onMultiBlockBeingLoaded(OnActivatedComponent event, EntityRef entity,
                                        MultiBlockMemberComponent multiBlockMember, BlockComponent block) {
        if (!internallyMutating) {
            pendingMultiBlockPartsChecks.add(block.getPosition());
//            if (worldProvider.isBlockRelevant(mainLocation)) {
//                EntityRef mainBlockEntity = blockEntityRegistry.getBlockEntityAt(mainLocation);
//                MultiBlockMainComponent multiBlockMain = mainBlockEntity.getComponent(MultiBlockMainComponent.class);
//                processLoadedMultiBlockMain(mainBlockEntity, multiBlockMain, mainBlockEntity.getComponent
//                (BlockComponent.class).getPosition());
//            }
        }
    }

    @ReceiveEvent
    public void onUnloadedMultiBlockBeingDamaged(BeforeDamagedEvent event, EntityRef entity,
                                                 MultiBlockMemberComponent multiBlockMember, BlockComponent block) {
        if (isEntityPartOfNotFullyLoadedMultiBlock(entity)) {
            event.consume();
        }
    }

    @ReceiveEvent
    public void onUnloadedMultiBlockBeingDamaged(BeforeDamagedEvent event, EntityRef entity,
                                                 MultiBlockMainComponent multiBlockMain, BlockComponent block) {
        if (isEntityPartOfNotFullyLoadedMultiBlock(entity)) {
            event.consume();
        }
    }

    @ReceiveEvent
    public void onMultiBlockBlocksReplaced(PlaceBlocks event, EntityRef world) {
        for (Vector3i vector3i : event.getBlocks().keySet()) {
            EntityRef blockEntity = blockEntityRegistry.getBlockEntityAt(vector3i);
            if (isEntityPartOfNotFullyLoadedMultiBlock(blockEntity)) {
                event.consume();
                break;
            }
        }
    }

    private boolean isEntityPartOfNotFullyLoadedMultiBlock(EntityRef entity) {
        MultiBlockMainComponent multiBlockMain = entity.getComponent(MultiBlockMainComponent.class);
        if (multiBlockMain != null) {
            return !worldProvider.isRegionRelevant(multiBlockMain.getAabb());
        }
        MultiBlockMemberComponent multiBlockMember = entity.getComponent(MultiBlockMemberComponent.class);
        if (multiBlockMember != null) {
            Vector3i mainBlockLocation = multiBlockMember.getMainBlockLocation();
            if (!worldProvider.isBlockRelevant(mainBlockLocation)) {
                return true;
            }
            multiBlockMain =
                    blockEntityRegistry.getBlockEntityAt(mainBlockLocation).getComponent(MultiBlockMainComponent.class);
            return !worldProvider.isRegionRelevant(multiBlockMain.getAabb());
        }
        return false;
    }

    private void processLoadedMultiBlockMain(EntityRef mainBlockEntity, MultiBlockMainComponent multiBlockMain,
                                             Vector3i position) {
        if (!loadedMultiBlocks.containsKey(multiBlockMain.getAabb())
                && worldProvider.isRegionRelevant(multiBlockMain.getAabb())) {
            EntityRef multiBlockEntity = createMultiBlockEntity(mainBlockEntity, position,
                    multiBlockMain.getMultiBlockType());

            loadedMultiBlocks.put(multiBlockMain.getAabb(), multiBlockEntity);
            multiBlockMain.setMultiBlockEntity(multiBlockEntity);

            multiBlockEntity.send(new MultiBlockLoaded(multiBlockMain.getMultiBlockType(), mainBlockEntity));
        }
    }

    private Region3i getChunkRegion(Vector3i chunkPos) {
        return Region3i.createFromMinAndSize(
                new Vector3i(chunkPos.x << ChunkConstants.POWER_X,
                        chunkPos.y << ChunkConstants.POWER_Y,
                        chunkPos.z << ChunkConstants.POWER_Z), ChunkConstants.CHUNK_SIZE);
    }

    private void destroyMultiBlock(EntityRef multiBlockMainBlockEntity) {
        MultiBlockMainComponent mainBlockComponent =
                multiBlockMainBlockEntity.getComponent(MultiBlockMainComponent.class);
        EntityRef multiBlockEntity = mainBlockComponent.getMultiBlockEntity();
        multiBlockEntity.send(new BeforeMultiBlockUnformed(mainBlockComponent.getMultiBlockType()));

        loadedMultiBlocks.remove(mainBlockComponent.getAabb());

        internallyMutating = true;
        try {
            for (Vector3i memberLocation : mainBlockComponent.getMultiBlockMembers()) {
                setBlockVisibilityIfNeeded(memberLocation, true);
                EntityRef memberEntity = blockEntityRegistry.getBlockEntityAt(memberLocation);
                memberEntity.removeComponent(MultiBlockMemberComponent.class);
            }
            Vector3i mainBlockPosition = multiBlockMainBlockEntity.getComponent(BlockComponent.class).getPosition();
            setBlockVisibilityIfNeeded(mainBlockPosition, true);
            multiBlockMainBlockEntity.removeComponent(MultiBlockMainComponent.class);
        } finally {
            internallyMutating = false;
        }

        multiBlockEntity.destroy();
    }

    private void createMultiBlock(MultiBlockDefinition definition) {
        Vector3i mainLocation = definition.getMainBlock();
        String multiBlockType = definition.getMultiBlockType();

        Collection<Vector3i> memberLocations = definition.getMemberBlocks();
        Region3i aabb = createAABB(mainLocation, memberLocations);

        EntityRef mainBlockEntity = blockEntityRegistry.getBlockEntityAt(mainLocation);

        EntityRef multiBlockEntity = createMultiBlockEntity(mainBlockEntity, mainLocation, multiBlockType);

        internallyMutating = true;
        try {
            setBlockVisibilityIfNeeded(mainLocation, false);

            mainBlockEntity.addComponent(
                    new MultiBlockMainComponent(new LinkedList<>(memberLocations), aabb, multiBlockEntity,
                            multiBlockType));

            for (Vector3i memberLocation : memberLocations) {
                setBlockVisibilityIfNeeded(memberLocation, false);
                blockEntityRegistry.getBlockEntityAt(memberLocation).addComponent(new MultiBlockMemberComponent(mainLocation));
            }
        } finally {
            internallyMutating = false;
        }

        loadedMultiBlocks.put(aabb, multiBlockEntity);

        multiBlockEntity.send(new MultiBlockFormed<>(multiBlockType, definition));
    }

    private void setBlockVisibilityIfNeeded(Vector3i location, boolean visible) {
        Block currentBlock = worldProvider.getBlock(location);
        BlockFamily blockFamily = currentBlock.getBlockFamily();
        if (blockFamily instanceof VisibilityEnabledBlockFamily) {
            Block blockToUse;
            VisibilityEnabledBlockFamily blockFamilyCast = (VisibilityEnabledBlockFamily) blockFamily;
            if (visible) {
                blockToUse = blockFamilyCast.getVisibleBlock(currentBlock);
            } else {
                blockToUse = blockFamilyCast.getInvisibleBlock(currentBlock);
            }
            worldProvider.setBlock(location, blockToUse);
        }
    }

    private EntityRef createMultiBlockEntity(EntityRef mainBlockEntity, Vector3i mainLocation, String multiBlockType) {
        LocationComponent locationComponent = new LocationComponent(mainLocation.toVector3f());
        MultiBlockComponent multiBlockComponent = new MultiBlockComponent(multiBlockType, mainBlockEntity);

        EntityBuilder entityBuilder = entityManager.newBuilder();
        entityBuilder.setPersistent(false);
        entityBuilder.addComponent(locationComponent);
        entityBuilder.addComponent(multiBlockComponent);
        return entityBuilder.build();
    }

    private Region3i createAABB(Vector3i mainLocation, Collection<Vector3i> memberLocations) {
        Region3i aabb = Region3i.createFromMinAndSize(mainLocation, new Vector3i(1, 1, 1));
        for (Vector3i memberLocation : memberLocations) {
            aabb = aabb.expandToContain(memberLocation);
        }
        return aabb;
    }

    private boolean areAllMultiBlocksInTheWayRelevant(Set<EntityRef> multiBlockMainBlockEntities) {
        if (multiBlockMainBlockEntities == null) {
            return false;
        }
        for (EntityRef multiBlockMainBlockEntity : multiBlockMainBlockEntities) {
            if (!worldProvider.isRegionRelevant(multiBlockMainBlockEntity.getComponent(MultiBlockMainComponent.class).getAabb())) {
                return false;
            }
        }
        return true;
    }

    private Set<EntityRef> getMultiBlockMainBlocksInTheWay(MultiBlockDefinition definition) {
        Set<EntityRef> result = new HashSet<>();
        for (Vector3i vector3i : Iterables.concat(definition.getMemberBlocks(),
                Collections.singleton(definition.getMainBlock()))) {
            EntityRef blockEntityAt = blockEntityRegistry.getBlockEntityAt(vector3i);
            MultiBlockMainComponent multiBlockMain = blockEntityAt.getComponent(MultiBlockMainComponent.class);
            if (multiBlockMain != null) {
                result.add(blockEntityAt);
            } else {
                MultiBlockMemberComponent multiBlockMember =
                        blockEntityAt.getComponent(MultiBlockMemberComponent.class);
                if (multiBlockMember != null) {
                    Vector3i mainLocation = multiBlockMember.getMainBlockLocation();
                    if (worldProvider.isBlockRelevant(mainLocation)) {
                        EntityRef mainBlockEntity = blockEntityRegistry.getBlockEntityAt(mainLocation);
                        multiBlockMain = mainBlockEntity.getComponent(MultiBlockMainComponent.class);
                        if (multiBlockMain == null) {
                            logger.error("Found MultiBlock member pointing at not existing main block.");
                            return null;
                        } else {
                            result.add(mainBlockEntity);
                        }
                    } else {
                        // We can't access all multi-blocks that compose this one, so need to skip forming the new one
                        return null;
                    }
                }
            }
        }
        return result;
    }
}
