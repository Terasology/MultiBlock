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

import com.google.common.collect.Iterables;
import org.terasology.math.Side;
import org.terasology.math.geom.Vector3i;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockUri;
import org.terasology.world.block.family.AbstractBlockFamily;
import org.terasology.world.block.family.SideDefinedBlockFamily;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InvisibleInMultiBlockStructureSidedBlockFamily extends AbstractBlockFamily implements VisibilityEnabledBlockFamily, SideDefinedBlockFamily {
    private Map<Side, Block> visibleBlocks;
    private Map<Side, Block> invisibleBlocks;
    private Side archetypeSide;

    private Map<BlockUri, Block> blockMap = new HashMap<>();

    public InvisibleInMultiBlockStructureSidedBlockFamily(BlockUri uri, Iterable<String> categories, Side archetypeSide,
                                                          Map<Side, Block> visibleBlocks, Map<Side, Block> invisibleBlocks) {
        super(uri, categories);
        this.archetypeSide = archetypeSide;
        this.visibleBlocks = visibleBlocks;
        this.invisibleBlocks = invisibleBlocks;

        for (Map.Entry<Side, Block> visibleBlockEntry : visibleBlocks.entrySet()) {
            Side side = visibleBlockEntry.getKey();
            Block block = visibleBlockEntry.getValue();
            BlockUri blockUri = new BlockUri(uri, "visible." + side.name());
            block.setUri(blockUri);
            block.setBlockFamily(this);
            blockMap.put(blockUri, block);
        }

        for (Map.Entry<Side, Block> invisibleBlockEntry : invisibleBlocks.entrySet()) {
            Side side = invisibleBlockEntry.getKey();
            Block block = invisibleBlockEntry.getValue();
            BlockUri blockUri = new BlockUri(uri, "invisible." + side.name());
            block.setUri(blockUri);
            block.setBlockFamily(this);
            blockMap.put(blockUri, block);
        }
    }

    @Override
    public Block getBlockForPlacement(WorldProvider worldProvider, BlockEntityRegistry blockEntityRegistry, Vector3i location, Side attachmentSide, Side direction) {
        if (attachmentSide.isHorizontal()) {
            return visibleBlocks.get(attachmentSide);
        }
        if (direction != null) {
            return visibleBlocks.get(direction);
        } else {
            return visibleBlocks.get(Side.FRONT);
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
