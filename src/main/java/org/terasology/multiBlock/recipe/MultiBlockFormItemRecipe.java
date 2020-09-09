// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock.recipe;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.logic.common.ActivateEvent;

/**
 * @author Marcin Sciesinski <marcins78@gmail.com>
 */
public interface MultiBlockFormItemRecipe {
    boolean isActivator(EntityRef item);

    boolean processActivation(ActivateEvent event);
}
