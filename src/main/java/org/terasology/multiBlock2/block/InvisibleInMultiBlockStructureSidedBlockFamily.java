// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock2.block;

import com.google.common.collect.Maps;
import org.terasology.assets.management.AssetManager;
import org.terasology.engine.math.Rotation;
import org.terasology.engine.math.Side;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockBuilderHelper;
import org.terasology.engine.world.block.BlockUri;
import org.terasology.engine.world.block.family.AbstractBlockFamily;
import org.terasology.engine.world.block.family.BlockPlacementData;
import org.terasology.engine.world.block.family.RegisterBlockFamily;
import org.terasology.engine.world.block.family.SideDefinedBlockFamily;
import org.terasology.engine.world.block.loader.BlockFamilyDefinition;
import org.terasology.engine.world.block.loader.SectionDefinitionData;
import org.terasology.engine.world.block.shapes.BlockShape;
import org.terasology.naming.Name;

import java.util.HashMap;
import java.util.Map;

@RegisterBlockFamily("invisibleInMultiBlockHorizontal")
public class InvisibleInMultiBlockStructureSidedBlockFamily extends AbstractBlockFamily implements VisibilityEnabledBlockFamily, SideDefinedBlockFamily {
    @In
    private AssetManager assetManager;

    private Map<Side, Block> visibleBlocks;
    private Map<Side, Block> invisibleBlocks;
    private Side archetypeSide;

    private Map<BlockUri, Block> blockMap = new HashMap<>();

    public InvisibleInMultiBlockStructureSidedBlockFamily(BlockFamilyDefinition family, BlockBuilderHelper blockBuilder) {
        super(family, blockBuilder);
        SectionDefinitionData visibleBlockData = family.getData().getBaseSection();
        visibleBlocks = constructHorizontalBlocks(family, visibleBlockData,
                blockBuilder);

        SectionDefinitionData invisibleBlockData = InvisibleBlockUtil.createInvisibleBlockSectionData(family,
                assetManager);
        invisibleBlocks = constructHorizontalBlocks(family, invisibleBlockData,
                blockBuilder);

        BlockUri familyUri = new BlockUri(family.getUrn());

        archetypeSide = Side.FRONT;

        for (Map.Entry<Side, Block> visibleBlockEntry : visibleBlocks.entrySet()) {
            Side side = visibleBlockEntry.getKey();
            Block block = visibleBlockEntry.getValue();
            BlockUri blockUri = new BlockUri(familyUri, new Name("visible." + side.name()));
            block.setUri(blockUri);
            block.setBlockFamily(this);
            blockMap.put(blockUri, block);
        }

        for (Map.Entry<Side, Block> invisibleBlockEntry : invisibleBlocks.entrySet()) {
            Side side = invisibleBlockEntry.getKey();
            Block block = invisibleBlockEntry.getValue();
            BlockUri blockUri = new BlockUri(familyUri, new Name("invisible." + side.name()));
            block.setUri(blockUri);
            block.setBlockFamily(this);
            blockMap.put(blockUri, block);
        }
    }

    private Map<Side, Block> constructHorizontalBlocks(BlockFamilyDefinition family, SectionDefinitionData sectionDefData, BlockBuilderHelper blockBuilder) {
        Map<Side, Block> blocks = Maps.newHashMap();

        String name = family.getUrn().getResourceName().toString();
        BlockShape shape = sectionDefData.getShape();
        for (Rotation rot : Rotation.horizontalRotations()) {
            BlockUri blockUri = new BlockUri(family.getUrn(), new Name(rot.getYaw().ordinal() + "." + rot.getPitch().ordinal() + "." + rot.getRoll().ordinal()));
            blocks.put(rot.rotate(Side.FRONT), blockBuilder.constructCustomBlock(name, shape, rot, sectionDefData, blockUri, this));
        }
        return blocks;
    }

    @Override
    public Block getBlockForPlacement(BlockPlacementData data) {
        if (data.attachmentSide.isHorizontal()) {
            return visibleBlocks.get(data.attachmentSide);
        } else {
            Side secondaryDirection = Side.inDirection(-data.viewingDirection.x(), 0, -data.viewingDirection.z());
            return visibleBlocks.get(secondaryDirection);
        }
    }

    @Override
    public Block getArchetypeBlock() {
        return visibleBlocks.get(archetypeSide);
    }

    @Override
    public Block getBlockForSide(Side side) {
        return visibleBlocks.get(side);
    }

    @Override
    public Block getBlockFor(BlockUri blockUri) {
        return blockMap.get(blockUri);
    }

    @Override
    public Block getInvisibleBlock(Block currentBlock) {
        return invisibleBlocks.get(getSide(currentBlock));
    }

    @Override
    public Block getVisibleBlock(Block currentBlock) {
        return visibleBlocks.get(getSide(currentBlock));
    }

    @Override
    public Iterable<Block> getBlocks() {
        return blockMap.values();
    }

    @Override
    public Side getSide(Block block) {
        for (Map.Entry<Side, Block> sideBlockEntry : visibleBlocks.entrySet()) {
            if (block == sideBlockEntry.getValue()) {
                return sideBlockEntry.getKey();
            }
        }
        for (Map.Entry<Side, Block> sideBlockEntry : invisibleBlocks.entrySet()) {
            if (block == sideBlockEntry.getValue()) {
                return sideBlockEntry.getKey();
            }
        }
        return null;
    }


}
