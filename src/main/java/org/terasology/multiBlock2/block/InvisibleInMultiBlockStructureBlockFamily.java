// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock2.block;

import org.terasology.assets.management.AssetManager;
import org.terasology.math.Rotation;
import org.terasology.naming.Name;
import org.terasology.registry.In;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockBuilderHelper;
import org.terasology.world.block.BlockUri;
import org.terasology.world.block.family.AbstractBlockFamily;
import org.terasology.world.block.family.BlockPlacementData;
import org.terasology.world.block.family.RegisterBlockFamily;
import org.terasology.world.block.loader.BlockFamilyDefinition;
import org.terasology.world.block.loader.SectionDefinitionData;
import org.terasology.world.block.shapes.BlockShape;

import java.util.Arrays;

@RegisterBlockFamily("invisibleInMultiBlock")
public class InvisibleInMultiBlockStructureBlockFamily extends AbstractBlockFamily implements VisibilityEnabledBlockFamily {
    private static final Name VISIBLE_NAME = new Name("visible");
    private static final Name INVISIBLE_NAME = new Name("invisible");

    @In
    private AssetManager assetManager;

    private Block visibleBlock;
    private Block invisibleBlock;

    public InvisibleInMultiBlockStructureBlockFamily(BlockFamilyDefinition family, BlockBuilderHelper blockBuilder) {
        super(family, blockBuilder);
        BlockUri familyUri = new BlockUri(family.getUrn());
        BlockUri visibleUri = new BlockUri(familyUri, VISIBLE_NAME);
        BlockUri invisibleUri = new BlockUri(familyUri, INVISIBLE_NAME);

        visibleBlock = blockBuilder.constructSimpleBlock(family, visibleUri, this);
        invisibleBlock = createInvisibleBlock(family, blockBuilder, invisibleUri);

        visibleBlock.setUri(visibleUri);
        invisibleBlock.setUri(invisibleUri);
    }

    private Block createInvisibleBlock(BlockFamilyDefinition family, BlockBuilderHelper blockBuilder, BlockUri uri) {
        SectionDefinitionData invisibleSectionDefData = InvisibleBlockUtil.createInvisibleBlockSectionData(family, assetManager);
        String invisibleBlockName = family.getUrn().getResourceName().toString();
        BlockShape shape = invisibleSectionDefData.getShape();
        Rotation rotation = Rotation.none();
        return blockBuilder.constructCustomBlock(invisibleBlockName, shape, rotation, invisibleSectionDefData, uri, this);
    }

    @Override
    public Block getBlockForPlacement(BlockPlacementData data) {
        return visibleBlock;
    }

    @Override
    public Block getArchetypeBlock() {
        return visibleBlock;
    }

    @Override
    public Block getBlockFor(BlockUri blockUri) {
        if (blockUri.getIdentifier().equals(VISIBLE_NAME)) {
            return visibleBlock;
        } else {
            return invisibleBlock;
        }
    }

    @Override
    public Block getInvisibleBlock(Block currentBlock) {
        return invisibleBlock;
    }

    @Override
    public Block getVisibleBlock(Block currentBlock) {
        return visibleBlock;
    }

    @Override
    public Iterable<Block> getBlocks() {
        return Arrays.asList(visibleBlock, invisibleBlock);
    }
}
