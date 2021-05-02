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


import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.gestalt.assets.management.AssetManager;
import org.terasology.engine.world.block.BlockPart;
import org.terasology.engine.world.block.loader.BlockFamilyDefinition;
import org.terasology.engine.world.block.loader.SectionDefinitionData;
import org.terasology.engine.world.block.tiles.BlockTile;

import java.util.Map;

class InvisibleBlockUtil {
    public static final ResourceUrn INVISIBLE_TILE_URN = new ResourceUrn("MultiBlock:InvisibleBlock");


    private InvisibleBlockUtil() {
        // no instance necessary
    }

    public static SectionDefinitionData createInvisibleBlockSectionData(BlockFamilyDefinition family,
                                                                         AssetManager assetManager) {
        SectionDefinitionData invisibleSectionDefData = family.getData().getBaseSection();
        invisibleSectionDefData.setInvisible(true);
        invisibleSectionDefData.setInvisible(true);

        // Asset always exist as it is in the same module:
        BlockTile invisibleTile = assetManager.getAsset(INVISIBLE_TILE_URN, BlockTile.class).get();

        for (Map.Entry<BlockPart, BlockTile> entry: invisibleSectionDefData.getBlockTiles().entrySet()) {
            entry.setValue(invisibleTile);
        }
        return invisibleSectionDefData;
    }
}
