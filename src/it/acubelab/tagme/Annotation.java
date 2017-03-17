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

import java.io.Serializable;


/**
 * A class representing an annotation.
 */
public class Annotation implements Serializable,Comparable<Annotation>{

	/**
	 *
	 */
	private static final long serialVersionUID = -7852189401841462217L;

	public static enum LongMode{
		NEVER,
		BY_CHAR,//TODO
		BY_ANCHOR,
		BY_LP //TODO
	}

	public static final LongMode DEFAULT_LONG_TEXT_MODE = LongMode.BY_ANCHOR;
	public static final boolean DEFAULT_SUBSTRING_PRUNING = true;
	public static final int DEFAULT_LONG_TEXT_LIMIT = 10;
	public static final float DEFAULT_LINK_PROB_FACTOR = 5.0f;

	public static float
	DEFAULT_RHO = 0.1f,
	MIN_RHO = 0.02f,
	MAX_RHO = 0.3f;


	public static final String
	MODULE = "annotation",
	PARAM_LONG_TEXT_LIMIT = "longTextLimit"	,
	PARAM_SUBSTRING_PRUNING = "substringPruning",
	PARAM_LONG_TEXT_MODE = "longTextMode",
	PARAM_LINK_PROB_FACTOR = "lpFactor";

	public static final int NOT_DISAMBIGUATED = -2;

	int start;
	int end;
	Anchor anchor;

	boolean tagged;

	boolean ignored;
	boolean pruned;
	Annotation equal = null;
	Annotation superior = null;

	int topic;
	float votes;
	int n_votes;
	int id;
	float coherence;
	float rho;

	public int ID(){
		return id;
	}

	public final boolean isDisambiguated(){
		return topic >= 0;
	}

	public Anchor getAnchor() {
		return anchor;
	}

	public boolean isTagged(){
		return tagged;
	}

	public boolean isIgnored() {
		return ignored;
	}

	public boolean isPruned() {
		return pruned;
	}

	public Annotation getEqual()
	{
		if (equal==null) return null;
		Annotation eq = this;
		while(eq.equal != null)
			eq = eq.equal;
		
		return eq;
	}
	public Annotation getSuperior()
	{
		if (superior==null) return null;
		Annotation su = this;
		while(su.superior != null)
			su = su.superior;
		return su;
	}

	public int getStart(){
		return start;
	}
	public int getEnd(){
		return end;
	}

	public int getTopic() {
		return topic;
	}

	public float getVotes() {
		return votes;
	}

	public int getN_votes() {
		return n_votes;
	}

	public float getCoherence() {
		return coherence;
	}

	public float getRho() {
		return rho;
	}

	@Override
	public boolean equals(Object o){
		if(o instanceof Annotation){
			Annotation a =(Annotation)o;
			if(this.topic== a.topic ) return true;
		}
		return false;
	}

	@Override
	public int compareTo(Annotation obj) {
		if(this.getRho()>obj.getRho()) return -1;
		if(this.getRho()<obj.getRho()) return 1;
		return 0;
	}
}
