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

import org.terasology.assets.management.AssetManager;
import org.terasology.math.Rotation;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockBuilderHelper;
import org.terasology.world.block.BlockUri;
import org.terasology.world.block.family.BlockFamily;
import org.terasology.world.block.family.BlockFamilyFactory;
import org.terasology.world.block.family.RegisterBlockFamilyFactory;
import org.terasology.world.block.loader.BlockFamilyDefinition;
import org.terasology.world.block.loader.SectionDefinitionData;
import org.terasology.world.block.shapes.BlockShape;

@RegisterBlockFamilyFactory("invisibleInMultiBlock")
public class InvisibleInMultiBlockStructureBlockFamilyFactory implements BlockFamilyFactory {

    private AssetManager assetManager = CoreRegistry.get(AssetManager.class);

    @Override
    public BlockFamily createBlockFamily(BlockFamilyDefinition family, BlockBuilderHelper blockBuilder) {
        final Block visibleBlock = blockBuilder.constructSimpleBlock(family);
        final Block invisibleBlock = createInvisibleBlock(family, blockBuilder);
        BlockUri familyUri = new BlockUri(family.getUrn());
        return new InvisibleInMultiBlockStructureBlockFamily(familyUri, family.getCategories(), visibleBlock, invisibleBlock);
    }

    private Block createInvisibleBlock(BlockFamilyDefinition family, BlockBuilderHelper blockBuilder) {
        SectionDefinitionData invisibleSectionDefData =InvisibleBlockUtil.createInvisibleBlockSectionData(family,
                assetManager);
        String invisibleBlockName = family.getUrn().getResourceName().toString();
        BlockShape shape = invisibleSectionDefData.getShape();
        Rotation rotation = Rotation.none();
        return blockBuilder.constructCustomBlock(invisibleBlockName, shape, rotation, invisibleSectionDefData);
    }


}
