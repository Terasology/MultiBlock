// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock2.component;

import org.terasology.engine.entitySystem.Component;

/**
 * Not for external use!
 */
public class TowerTypeComponent implements Component {
    private String effector;
    private String targeter;

    public String getEffector() {
        return effector;
    }

    public String getTargeter() {
        return targeter;
    }
}
