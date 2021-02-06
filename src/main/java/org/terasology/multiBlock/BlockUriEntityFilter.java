// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock;

import com.google.common.base.Predicate;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockUri;

/**
 * @author Marcin Sciesinski <marcins78@gmail.com>
 */
public class BlockUriEntityFilter implements Predicate<EntityRef> {
    private BlockUri blockUri;

    public BlockUriEntityFilter(BlockUri blockUri) {
        this.blockUri = blockUri;
    }

    @Override
    public boolean apply(EntityRef entity) {
        BlockComponent component = entity.getComponent(BlockComponent.class);
        return component != null && component.block.getURI().equals(blockUri);
    }
}
