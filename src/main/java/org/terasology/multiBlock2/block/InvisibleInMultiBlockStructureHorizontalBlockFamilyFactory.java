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
import com.google.gson.JsonObject;
import org.terasology.asset.AssetUri;
import org.terasology.math.Side;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockUri;
import org.terasology.world.block.family.BlockBuilderHelper;
import org.terasology.world.block.family.BlockFamily;
import org.terasology.world.block.family.BlockFamilyFactory;
import org.terasology.world.block.family.RegisterBlockFamilyFactory;
import org.terasology.world.block.loader.BlockDefinition;

import java.util.Map;

@RegisterBlockFamilyFactory("invisibleInMultiBlockHorizontal")
public class InvisibleInMultiBlockStructureHorizontalBlockFamilyFactory implements BlockFamilyFactory {
    @Override
    public BlockFamily createBlockFamily(BlockBuilderHelper blockBuilder, AssetUri blockDefUri, BlockDefinition blockDefinition, JsonObject blockDefJson) {
        BlockDefinition invisibleBlockDefinition = new BlockDefinition();
        invisibleBlockDefinition.attachmentAllowed = blockDefinition.attachmentAllowed;
        invisibleBlockDefinition.categories = blockDefinition.categories;
        invisibleBlockDefinition.climbable = blockDefinition.climbable;
        invisibleBlockDefinition.colorOffset = blockDefinition.colorOffset;
        invisibleBlockDefinition.colorOffsets =blockDefinition.colorOffsets;
        invisibleBlockDefinition.colorSource = blockDefinition.colorSource;
        invisibleBlockDefinition.colorSources = blockDefinition.colorSources;
        invisibleBlockDefinition.debrisOnDestroy = blockDefinition.debrisOnDestroy;
        invisibleBlockDefinition.displayName = blockDefinition.displayName;
        invisibleBlockDefinition.doubleSided = blockDefinition.doubleSided;
        invisibleBlockDefinition.entity = blockDefinition.entity;
        invisibleBlockDefinition.grass = blockDefinition.grass;
        invisibleBlockDefinition.hardness = blockDefinition.hardness;
        invisibleBlockDefinition.ice = blockDefinition.ice;
        invisibleBlockDefinition.inventory = blockDefinition.inventory;
        invisibleBlockDefinition.invisible = true;
        invisibleBlockDefinition.lava = blockDefinition.lava;
        invisibleBlockDefinition.liquid = blockDefinition.liquid;
        invisibleBlockDefinition.luminance = blockDefinition.luminance;
        invisibleBlockDefinition.mass = blockDefinition.mass;
        invisibleBlockDefinition.penetrable = blockDefinition.penetrable;
        invisibleBlockDefinition.replacementAllowed = blockDefinition.replacementAllowed;
        invisibleBlockDefinition.inventory = blockDefinition.inventory;
        invisibleBlockDefinition.shadowCasting = blockDefinition.shadowCasting;
        invisibleBlockDefinition.shape = blockDefinition.shape;
        invisibleBlockDefinition.shapes = blockDefinition.shapes;
        invisibleBlockDefinition.sounds = blockDefinition.sounds;
        invisibleBlockDefinition.supportRequired = blockDefinition.supportRequired;
        invisibleBlockDefinition.targetable = blockDefinition.targetable;
        invisibleBlockDefinition.rotation = blockDefinition.rotation;
        invisibleBlockDefinition.tint = blockDefinition.tint;
        invisibleBlockDefinition.translucent = true;
        invisibleBlockDefinition.water = blockDefinition.water;
        invisibleBlockDefinition.waving = blockDefinition.waving;

        invisibleBlockDefinition.tile = "MultiBlock:InvisibleBlock";
        invisibleBlockDefinition.tiles = null;

        BlockUri familyUri = new BlockUri(blockDefUri.getModuleName(), blockDefUri.getAssetName());

        Map<Side, Block> visibleBlocks = Maps.newHashMap();
        visibleBlocks.putAll(blockBuilder.constructHorizontalRotatedBlocks(blockDefUri, blockDefinition));

        Map<Side, Block> invisibleBlocks = Maps.newHashMap();
        invisibleBlocks.putAll(blockBuilder.constructHorizontalRotatedBlocks(blockDefUri, invisibleBlockDefinition));

        return new InvisibleInMultiBlockStructureSidedBlockFamily(familyUri, blockDefinition.categories, Side.FRONT, visibleBlocks, invisibleBlocks);
    }
}
