/*
 * Copyright 2015 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.multiBlock2.recipe;

import org.terasology.math.geom.Vector3i;
import org.terasology.multiBlock2.MultiBlockDefinition;

/**
 * Detects if a MultiBlock should be formed by placing a block at the specified location.
 * It is invoked only if the block matches the MultiBlockCandidate type matches the one defined for this
 * recipe in the MultiBlockRegistry.
 *
 * If a MultiBlock could be created for this specific block placement, a non null MultiBlockDefinition should
 * be returned containing all the required data. In addition this class can contain some extra information about
 * this multi block that will be passed to MultiBlockFormed event.
 *
 * If a MultiBlock should not be created for this specific block placement - the method of this interface should
 * return a <code>null</code> value.
 *
 * @param <T>
 */
public interface MultiBlockRecipe<T extends MultiBlockDefinition> {
    public T detectFormingMultiBlock(Vector3i location);
}
