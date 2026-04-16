/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ricedotwho.rsa.component.impl.pathfinding.openset;

import com.ricedotwho.rsa.component.impl.pathfinding.PathNode;

import java.util.Arrays;

public final class BinaryHeapOpenSet implements IOpenSet {

    /**
     * The initial capacity of the heap (2^10)
     */
    private static final int INITIAL_CAPACITY = 1024;

    /**
     * The array backing the heap
     */
    private PathNode[] array;

    /**
     * The size of the heap
     */
    private int size;

    private final float nodeCost;

    public BinaryHeapOpenSet(float nodeCost) {
        this(INITIAL_CAPACITY, nodeCost);
    }

    public BinaryHeapOpenSet(int size, float nodeCost) {
        this.size = 0;
        this.array = new PathNode[size];
        this.nodeCost = nodeCost;
    }

    public int size() {
        return size;
    }

    @Override
    public final void insert(PathNode value) {
        if (size >= array.length - 1) {
            array = Arrays.copyOf(array, array.length << 1);
        }
        size++;
        value.heapPosition = size;
        array[size] = value;
        update(value);
    }

    @Override
    public final void update(PathNode val) {
        int index = val.heapPosition;
        int parentInd = index >>> 1;
        double cost = val.getCost(this.nodeCost);
        PathNode parentNode = array[parentInd];
        while (index > 1 && parentNode.getCost(this.nodeCost) > cost) {
            array[index] = parentNode;
            array[parentInd] = val;
            val.heapPosition = parentInd;
            parentNode.heapPosition = index;
            index = parentInd;
            parentInd = index >>> 1;
            parentNode = array[parentInd];
        }
    }

    @Override
    public final boolean isEmpty() {
        return size == 0;
    }

    @Override
    public final PathNode removeLowest() {
        if (size == 0) {
            throw new IllegalStateException();
        }
        PathNode result = array[1];
        PathNode val = array[size];
        array[1] = val;
        val.heapPosition = 1;
        array[size] = null;
        size--;
        result.heapPosition = -1;
        if (size < 2) {
            return result;
        }
        int index = 1;
        int smallerChild = 2;
        double cost = val.getCost(nodeCost);
        do {
            PathNode smallerChildNode = array[smallerChild];
            double smallerChildCost = smallerChildNode.getCost(nodeCost);
            if (smallerChild < size) {
                PathNode rightChildNode = array[smallerChild + 1];
                double rightChildCost = rightChildNode.getCost(nodeCost);
                if (smallerChildCost > rightChildCost) {
                    smallerChild++;
                    smallerChildCost = rightChildCost;
                    smallerChildNode = rightChildNode;
                }
            }
            if (cost <= smallerChildCost) {
                break;
            }
            array[index] = smallerChildNode;
            array[smallerChild] = val;
            val.heapPosition = smallerChild;
            smallerChildNode.heapPosition = index;
            index = smallerChild;
        } while ((smallerChild <<= 1) <= size);
        return result;
    }
}
