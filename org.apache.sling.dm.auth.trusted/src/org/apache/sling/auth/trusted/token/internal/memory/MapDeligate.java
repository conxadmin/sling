/**
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.auth.trusted.token.internal.memory;


import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.sling.auth.trusted.token.api.memory.Cache;

public class MapDeligate<K, V> implements Map<K, V> {

  private Cache<V> cache;

  public MapDeligate(Cache<V> cache) {
    this.cache = cache;
  }

  public void clear() {
    cache.clear();
  }

  public boolean containsKey(Object key) {
    return cache.containsKey((java.lang.String) key);
  }

  public boolean containsValue(Object value) {
    throw new UnsupportedOperationException("This map is lookup only.");
  }

  public Set<Entry<K, V>> entrySet() {
    throw new UnsupportedOperationException("This map is lookup only.");
  }

  public V get(Object key) {
    return cache.get((String) key);
  }

  public boolean isEmpty() {
    return false;
  }

  public Set<K> keySet() {
    throw new UnsupportedOperationException("This map is lookup only.");
  }

  public V put(K key, V value) {
    return cache.put((String) key, value);
  }

  public void putAll(Map<? extends K, ? extends V> m) {
    throw new UnsupportedOperationException("This map is singly add only, use an iterator or loop.");
  }

  public int size() {
    throw new UnsupportedOperationException("This map is lookup only.");
  }

  public V remove(Object key) {
    V value = cache.get((String) key);
    cache.remove((String) key);
    return value;
  }

  public Collection<V> values() {
    throw new UnsupportedOperationException("This map is lookup only.");
  }


}