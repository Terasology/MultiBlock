// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.multiBlock;

import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.registry.Share;
import org.terasology.multiBlock.recipe.MultiBlockFormItemRecipe;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Marcin Sciesinski <marcins78@gmail.com>
 */
@RegisterSystem
@Share(value = MultiBlockFormRecipeRegistry.class)
public class MultiBlockFormRecipeRegistryImpl extends BaseComponentSystem implements MultiBlockFormRecipeRegistry {
    private final Set<MultiBlockFormItemRecipe> itemRecipes = new HashSet<>();

    @Override
    public void addMultiBlockFormItemRecipe(MultiBlockFormItemRecipe recipe) {
        itemRecipes.add(recipe);
    }

    @Override
    public Collection<MultiBlockFormItemRecipe> getMultiBlockFormItemRecipes() {
        return Collections.unmodifiableCollection(itemRecipes);
    }
}
