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

import com.google.common.collect.Maps;
import org.terasology.assets.management.AssetManager;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
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

import java.util.Map;

@RegisterBlockFamilyFactory("invisibleInMultiBlockHorizontal")
public class InvisibleInMultiBlockStructureHorizontalBlockFamilyFactory implements BlockFamilyFactory {

    private AssetManager assetManager = CoreRegistry.get(AssetManager.class);

    @Override
    public BlockFamily createBlockFamily(BlockFamilyDefinition familiy, BlockBuilderHelper blockBuilder) {
        SectionDefinitionData visibleBlockData = familiy.getData().getBaseSection();
        Map<Side, Block> visibleBlocks = constructHorizontalBlocks(familiy, visibleBlockData,
                blockBuilder);

        SectionDefinitionData invisibleBlockData = InvisibleBlockUtil.createInvisibleBlockSectionData(familiy,
                assetManager);
        Map<Side, Block> invisibleBlocks = constructHorizontalBlocks(familiy, invisibleBlockData,
                blockBuilder);

        BlockUri familyUri = new BlockUri(familiy.getUrn());
        return new InvisibleInMultiBlockStructureSidedBlockFamily(familyUri, familiy.getCategories(), Side.FRONT,
                visibleBlocks, invisibleBlocks);
    }


    private Map<Side, Block> constructHorizontalBlocks(BlockFamilyDefinition familiy, SectionDefinitionData sectionDefData, BlockBuilderHelper blockBuilder) {
        Map<Side, Block> blockMap = Maps.newHashMap();

        String name = familiy.getUrn().getResourceName().toString();
        BlockShape shape = sectionDefData.getShape();
        for (Rotation rot : Rotation.horizontalRotations()) {
            blockMap.put(rot.rotate(Side.FRONT), blockBuilder.constructCustomBlock(name, shape, rot, sectionDefData));
        }
        return blockMap;
    }



}
