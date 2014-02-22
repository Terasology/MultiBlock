/*
 * Copyright 2014 MovingBlocks
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
package org.terasology.multiBlock;

import com.google.common.base.Predicate;
import org.terasology.math.Vector2i;

public class Basic2DSizeFilter implements Predicate<Vector2i> {
    private int minHorizontal;
    private int maxHorizontal;

    public Basic2DSizeFilter(int horizontal1Size, int horizontal2Size) {
        minHorizontal = Math.min(horizontal1Size, horizontal2Size);
        maxHorizontal = Math.max(horizontal1Size, horizontal2Size);
    }

    @Override
    public boolean apply(Vector2i value) {
        return minHorizontal == Math.min(value.x, value.y)
                && maxHorizontal == Math.max(value.x, value.y);
    }
}
