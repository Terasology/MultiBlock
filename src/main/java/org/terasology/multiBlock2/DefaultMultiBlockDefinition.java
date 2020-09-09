// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock2;

import org.terasology.math.geom.Vector3i;

import java.util.Collection;
import java.util.Collections;

public class DefaultMultiBlockDefinition implements MultiBlockDefinition {
    private final String multiBlockType;
    private final Vector3i mainBlock;
    private final Collection<Vector3i> memberBlocks;

    public DefaultMultiBlockDefinition(String multiBlockType, Vector3i mainBlock, Collection<Vector3i> memberBlocks) {
        this.multiBlockType = multiBlockType;
        this.mainBlock = mainBlock;
        this.memberBlocks = memberBlocks;
    }

    @Override
    public String getMultiBlockType() {
        return multiBlockType;
    }

    @Override
    public Vector3i getMainBlock() {
        return mainBlock;
    }

    @Override
    public Collection<Vector3i> getMemberBlocks() {
        return Collections.unmodifiableCollection(memberBlocks);
    }
}
