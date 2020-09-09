// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.common.ActivateEvent;
import org.terasology.engine.logic.inventory.ItemComponent;
import org.terasology.engine.registry.In;
import org.terasology.multiBlock.recipe.MultiBlockFormItemRecipe;

/**
 * @author Marcin Sciesinski <marcins78@gmail.com>
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class MultiBlockFormingSystem extends BaseComponentSystem {
    @In
    private MultiBlockFormRecipeRegistry recipeRegistry;

    @ReceiveEvent
    public void formMultiBlockWithItem(ActivateEvent event, EntityRef item,
                                       ItemComponent itemComponent) {
        for (MultiBlockFormItemRecipe multiBlockFormItemRecipe : recipeRegistry.getMultiBlockFormItemRecipes()) {
            if (multiBlockFormItemRecipe.isActivator(item)) {
                if (multiBlockFormItemRecipe.processActivation(event)) {
                    break;
                }
            }
        }
    }
}
