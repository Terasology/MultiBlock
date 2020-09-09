// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock2.block;

import org.terasology.engine.world.block.Block;

public interface VisibilityEnabledBlockFamily {
    Block getInvisibleBlock(Block currentBlock);

    Block getVisibleBlock(Block currentBlock);
}
