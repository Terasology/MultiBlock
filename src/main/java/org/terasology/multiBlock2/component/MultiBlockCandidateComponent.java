// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock2.component;

import org.terasology.gestalt.entitysystem.component.Component;

import java.util.Collections;
import java.util.Set;

public class MultiBlockCandidateComponent implements Component<MultiBlockCandidateComponent> {
    private Set<String> type;

    public Set<String> getType() {
        return Collections.unmodifiableSet(type);
    }

    @Override
    public void copy(MultiBlockCandidateComponent other) {
        this.type = other.type;
    }
}
