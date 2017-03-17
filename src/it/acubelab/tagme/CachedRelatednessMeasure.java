/*******************************************************************************
 * Copyright 2014 A3 lab (Dipartimento di Informatica, Università di Pisa)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package it.acubelab.tagme;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;

public class CachedRelatednessMeasure extends RelatednessMeasure
{
	
	public static final int DEFAULT_CACHE_INITIAL_SIZE = 8000000; //8M

	Long2FloatOpenHashMap cache;
	RelatednessMeasure realRel;
	
	public CachedRelatednessMeasure(String lang) {
		this(lang, RelatednessMeasure.create(lang), DEFAULT_CACHE_INITIAL_SIZE);
	}
	public CachedRelatednessMeasure(String lang, int cacheInitSize) {
		this(lang, RelatednessMeasure.create(lang), cacheInitSize);
	}
	public CachedRelatednessMeasure(String lang, RelatednessMeasure rel) {
		this(lang, rel, DEFAULT_CACHE_INITIAL_SIZE);
	}
	public CachedRelatednessMeasure(String lang, RelatednessMeasure rel, int cacheInitSize) {
		super(lang);
		this.cache=new Long2FloatOpenHashMap(cacheInitSize);
		this.realRel = rel;
	}
	
	protected long key(long a, long b){
		return Math.min(a, b)+(Math.max(a, b) << 30);
	}

	@Override
	public float rel(int a, int b)
	{
		long key=key(a,b);
		if(cache.containsKey(key)) {
			return cache.get(key);
		} else {
			float rel = realRel.rel(a, b);
			cache.put(key,rel);
			return rel;
		}
	}

	public int cacheSize(){
		return cache.size();
	}
	public void emptyCache(){
		this.cache.clear();
	}
}
