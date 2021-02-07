// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock2.system;

import com.google.common.collect.Iterables;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityBuilder;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.BeforeRemoveComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnAddedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.health.event.BeforeDamagedEvent;
import org.terasology.logic.location.LocationComponent;
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
import org.terasology.registry.In;
import org.terasology.registry.Share;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockRegion;
import org.terasology.world.block.entity.placement.PlaceBlocks;
import org.terasology.world.block.family.BlockFamily;
import org.terasology.world.chunks.Chunks;
import org.terasology.world.chunks.event.BeforeChunkUnload;

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

    @In
    private BlockEntityRegistry blockEntityRegistry;
    @In
    private WorldProvider worldProvider;
    @In
    private EntityManager entityManager;

    private Map<String, MultiBlockRecipe<?>> multiBlockRecipeMap = new HashMap<>();

    private Map<BlockRegion, EntityRef> loadedMultiBlocks = new HashMap<>();

    private boolean internallyMutating = false;

    private Set<Vector3i> pendingMultiBlockPartsChecks = new HashSet<>();

    // Horrible workaround for the fact, that system is notified about block entities being loaded via OnActivatedComponent
    // before the chunk they are in is "relevant", we need to keep querying worldProvider, until it was merged to restore
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
                    MultiBlockMemberComponent multiBlockMember = blockEntity.getComponent(MultiBlockMemberComponent.class);
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
        for (Map.Entry<BlockRegion, EntityRef> regionEntityRefEntry : loadedMultiBlocks.entrySet()) {
            if (regionEntityRefEntry.getKey().contains(location)) {
                EntityRef entity = regionEntityRefEntry.getValue();
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
    public void onMultiBlockCandidatePlaced(OnAddedComponent event, EntityRef entity, MultiBlockCandidateComponent candidate, BlockComponent block) {
        for (String type : candidate.getType()) {
            MultiBlockRecipe<?> recipe = multiBlockRecipeMap.get(type);
            if (recipe != null) {
                MultiBlockDefinition definition = recipe.detectFormingMultiBlock(block.getPosition(new Vector3i()));
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
    public void onMultiBlockPartRemoved(BeforeRemoveComponent event, EntityRef entity, MultiBlockMainComponent multiBlockMain, BlockComponent block) {
        if (!internallyMutating) {
            destroyMultiBlock(entity);
        }
    }

    @ReceiveEvent
    public void onMultiBlockPartRemoved(BeforeRemoveComponent event, EntityRef entity, MultiBlockMemberComponent multiBlockMember, BlockComponent block) {
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
        BlockRegion chunkRegion = getChunkRegion(beforeChunkUnload.getChunkPos());
        Iterator<Map.Entry<BlockRegion, EntityRef>> iterator = loadedMultiBlocks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockRegion, EntityRef> multiBlock = iterator.next();
            if (chunkRegion.intersectsBlockRegion(multiBlock.getKey())) {
                EntityRef multiBlockEntity = multiBlock.getValue();
                MultiBlockComponent component = multiBlockEntity.getComponent(MultiBlockComponent.class);
                multiBlockEntity.send(new BeforeMultiBlockUnloaded(component.getType(), component.getMainBlockEntity()));
                iterator.remove();
                multiBlockEntity.destroy();
            }
        }
    }

    //    @ReceiveEvent
//    public void afterChunkLoaded(OnChunkLoaded chunkLoaded, EntityRef world) {
//        BlockRegion chunkRegion = getChunkRegion(chunkLoaded.getChunkPos());
//        for (Vector3ic vector3i : chunkRegion) {
//            EntityRef blockEntity = blockEntityRegistry.getBlockEntityAt(vector3i);
//            MultiBlockMainComponent multiBlockMain = blockEntity.getComponent(MultiBlockMainComponent.class);
//            if (multiBlockMain != null) {
//                processLoadedMultiBlockMain(blockEntity, multiBlockMain, vector3i);
//            } else {
//                MultiBlockMemberComponent multiBlockMember = blockEntity.getComponent(MultiBlockMemberComponent.class);
//                if (multiBlockMember != null && worldProvider.isBlockRelevant(multiBlockMember.getMainBlockLocation())) {
//                    Vector3i mainBlockLocation = multiBlockMember.getMainBlockLocation();
//                    EntityRef mainBlockEntity = blockEntityRegistry.getBlockEntityAt(mainBlockLocation);
//                    processLoadedMultiBlockMain(mainBlockEntity, mainBlockEntity.getComponent(MultiBlockMainComponent.class), mainBlockLocation);
//                }
//            }
//        }
//    }
//
    @ReceiveEvent
    public void onMultiBlockBeingLoaded(OnActivatedComponent event, EntityRef entity, MultiBlockMainComponent multiBlockMain, BlockComponent block) {
        if (!internallyMutating) {
            pendingMultiBlockPartsChecks.add(block.getPosition(new Vector3i()));
//            processLoadedMultiBlockMain(entity, multiBlockMain,  block.getPosition());
        }
    }

    @ReceiveEvent
    public void onMultiBlockBeingLoaded(OnActivatedComponent event, EntityRef entity, MultiBlockMemberComponent multiBlockMember, BlockComponent block) {
        if (!internallyMutating) {
            pendingMultiBlockPartsChecks.add(block.getPosition(new Vector3i()));
//            if (worldProvider.isBlockRelevant(mainLocation)) {
//                EntityRef mainBlockEntity = blockEntityRegistry.getBlockEntityAt(mainLocation);
//                MultiBlockMainComponent multiBlockMain = mainBlockEntity.getComponent(MultiBlockMainComponent.class);
//                processLoadedMultiBlockMain(mainBlockEntity, multiBlockMain, mainBlockEntity.getComponent(BlockComponent.class).getPosition());
//            }
        }
    }

    @ReceiveEvent
    public void onUnloadedMultiBlockBeingDamaged(BeforeDamagedEvent event, EntityRef entity, MultiBlockMemberComponent multiBlockMember, BlockComponent block) {
        if (isEntityPartOfNotFullyLoadedMultiBlock(entity)) {
            event.consume();
        }
    }

    @ReceiveEvent
    public void onUnloadedMultiBlockBeingDamaged(BeforeDamagedEvent event, EntityRef entity, MultiBlockMainComponent multiBlockMain, BlockComponent block) {
        if (isEntityPartOfNotFullyLoadedMultiBlock(entity)) {
            event.consume();
        }
    }

    @ReceiveEvent
    public void onMultiBlockBlocksReplaced(PlaceBlocks event, EntityRef world) {
        for (Vector3ic vector3i : event.getBlocks().keySet()) {
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
            multiBlockMain = blockEntityRegistry.getBlockEntityAt(mainBlockLocation).getComponent(MultiBlockMainComponent.class);
            return !worldProvider.isRegionRelevant(multiBlockMain.getAabb());
        }
        return false;
    }

    private void processLoadedMultiBlockMain(EntityRef mainBlockEntity, MultiBlockMainComponent multiBlockMain, Vector3i position) {
        if (!loadedMultiBlocks.containsKey(multiBlockMain.getAabb())
                && worldProvider.isRegionRelevant(multiBlockMain.getAabb())) {
            EntityRef multiBlockEntity = createMultiBlockEntity(mainBlockEntity, position, multiBlockMain.getMultiBlockType());

            loadedMultiBlocks.put(new BlockRegion(multiBlockMain.getAabb()), multiBlockEntity);
            multiBlockMain.setMultiBlockEntity(multiBlockEntity);

            multiBlockEntity.send(new MultiBlockLoaded(multiBlockMain.getMultiBlockType(), mainBlockEntity));
        }
    }

    private BlockRegion getChunkRegion(Vector3ic chunkPos) {
        //TODO: provide this as utility on Chunks?
        return new BlockRegion(chunkPos.x() << Chunks.POWER_X,
                        chunkPos.y() << Chunks.POWER_Y,
                        chunkPos.z() << Chunks.POWER_Z).setSize(Chunks.CHUNK_SIZE);
    }

    private void destroyMultiBlock(EntityRef multiBlockMainBlockEntity) {
        MultiBlockMainComponent mainBlockComponent = multiBlockMainBlockEntity.getComponent(MultiBlockMainComponent.class);
        EntityRef multiBlockEntity = mainBlockComponent.getMultiBlockEntity();
        multiBlockEntity.send(new BeforeMultiBlockUnformed(mainBlockComponent.getMultiBlockType()));

        loadedMultiBlocks.remove(mainBlockComponent.getAabb());

        internallyMutating = true;
        try {
            for (Vector3ic memberLocation : mainBlockComponent.getMultiBlockMembers()) {
                setBlockVisibilityIfNeeded(memberLocation, true);
                EntityRef memberEntity = blockEntityRegistry.getBlockEntityAt(memberLocation);
                memberEntity.removeComponent(MultiBlockMemberComponent.class);
            }
            Vector3i mainBlockPosition = multiBlockMainBlockEntity.getComponent(BlockComponent.class).getPosition(new Vector3i());
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
        BlockRegion region = memberLocations.stream().reduce(new BlockRegion(mainLocation), BlockRegion::union, BlockRegion::union);

        EntityRef mainBlockEntity = blockEntityRegistry.getBlockEntityAt(mainLocation);

        EntityRef multiBlockEntity = createMultiBlockEntity(mainBlockEntity, mainLocation, multiBlockType);

        internallyMutating = true;
        try {
            setBlockVisibilityIfNeeded(mainLocation, false);

            mainBlockEntity.addComponent(
                    new MultiBlockMainComponent(new LinkedList<>(memberLocations), region, multiBlockEntity, multiBlockType));

            for (Vector3i memberLocation : memberLocations) {
                setBlockVisibilityIfNeeded(memberLocation, false);
                blockEntityRegistry.getBlockEntityAt(memberLocation).addComponent(new MultiBlockMemberComponent(mainLocation));
            }
        } finally {
            internallyMutating = false;
        }

        loadedMultiBlocks.put(region, multiBlockEntity);

        multiBlockEntity.send(new MultiBlockFormed<>(multiBlockType, definition));
    }

    private void setBlockVisibilityIfNeeded(Vector3ic location, boolean visible) {
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
        LocationComponent locationComponent = new LocationComponent(new Vector3f(mainLocation));
        MultiBlockComponent multiBlockComponent = new MultiBlockComponent(multiBlockType, mainBlockEntity);

        EntityBuilder entityBuilder = entityManager.newBuilder();
        entityBuilder.setPersistent(false);
        entityBuilder.addComponent(locationComponent);
        entityBuilder.addComponent(multiBlockComponent);
        return entityBuilder.build();
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
        for (Vector3i vector3i : Iterables.concat(definition.getMemberBlocks(), Collections.singleton(definition.getMainBlock()))) {
            EntityRef blockEntityAt = blockEntityRegistry.getBlockEntityAt(vector3i);
            MultiBlockMainComponent multiBlockMain = blockEntityAt.getComponent(MultiBlockMainComponent.class);
            if (multiBlockMain != null) {
                result.add(blockEntityAt);
            } else {
                MultiBlockMemberComponent multiBlockMember = blockEntityAt.getComponent(MultiBlockMemberComponent.class);
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
