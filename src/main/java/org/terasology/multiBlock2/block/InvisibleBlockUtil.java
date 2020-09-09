// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock2.block;


import org.terasology.engine.world.block.BlockPart;
import org.terasology.engine.world.block.loader.BlockFamilyDefinition;
import org.terasology.engine.world.block.loader.SectionDefinitionData;
import org.terasology.engine.world.block.tiles.BlockTile;
import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.gestalt.assets.management.AssetManager;

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

        for (Map.Entry<BlockPart, BlockTile> entry : invisibleSectionDefData.getBlockTiles().entrySet()) {
            entry.setValue(invisibleTile);
        }
        return invisibleSectionDefData;
    }
}
