/*
 * Copyright 2015 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.multiBlock2.block;

import org.terasology.gestalt.assets.management.AssetManager;
import org.terasology.gestalt.naming.Name;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.math.geom.Vector3i;
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
    public Block getBlockForPlacement(Vector3i location, Side attachmentSide, Side direction) {
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
