// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock2.event;

import org.terasology.engine.entitySystem.event.Event;
import org.joml.Vector3i;

import java.util.List;

/**
 * Notification event which can be sent to any block Entity within the Structure Template to create a
 * multiBlock entity with the region corresponding to the Structure Template.
 */
public class SendRegionEvent implements Event {
    public List<Vector3i> regions;

    public SendRegionEvent(List<Vector3i> regions) {
        this.regions = regions;
    }
}
