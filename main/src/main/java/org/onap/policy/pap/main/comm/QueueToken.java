/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pap.main.comm;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Token that can be placed within a publisher's queue. The item that a token references
 * may be replaced any time up until it is set to {@code null}. Once it has been set to
 * {@code null}, it cannot be replaced.
 *
 * @param <T> type of object referenced by the token
 */
public class QueueToken<T> {

    /**
     * Wraps the item.
     */
    private final AtomicReference<T> ref;

    /**
     * Constructs the object.
     *
     * @param item initial token item
     */
    public QueueToken(T item) {
        ref = new AtomicReference<>(item);
    }

    /**
     * Gets the item referenced by this token.
     *
     * @return the item referenced by this token
     */
    public final T get() {
        return ref.get();
    }

    /**
     * Replaces the token's item. If the current item is {@code null}, then it is left
     * unchanged.
     *
     * @param newItem the new item
     * @return the original item
     */
    public T replaceItem(T newItem) {
        T oldItem;
        while ((oldItem = ref.get()) != null) {
            if (ref.compareAndSet(oldItem, newItem)) {
                break;
            }
        }

        // it was already null, or we successfully replaced the item
        return oldItem;
    }
}
