// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock2.component;

import org.terasology.engine.entitySystem.Component;

import java.util.Collections;
import java.util.Set;

public class MultiBlockCandidateComponent implements Component {
    private Set<String> type;

    public Set<String> getType() {
        return Collections.unmodifiableSet(type);
    }
}
