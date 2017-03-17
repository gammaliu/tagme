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
package it.acubelab.tagme.wrapper;

import it.acubelab.tagme.RelatednessMeasure;
import it.acubelab.tagme.preprocessing.graphs.OnTheFlyArrayMeasure;
import it.acubelab.tagme.preprocessing.graphs.StoredMeasure;

/**
 * A wrapper-class fon a calculator of reletedness between Wikipedia pages.
 */

public class Relatedness {

	enum RelType
	{
		ON_THE_FLY,
		STORED
	};
	protected String lang;
	private RelatednessMeasure relate;
	
	/** Creates an on-the-fly reletedness calculator.
	 * @param lang The wikipedia language.
	 */
	public Relatedness(String lang){
		relate = new OnTheFlyArrayMeasure(lang);
	}

	/** Creates a reletedness calculator of the specifies type.
	 * @param lang The wikipedia language.
	 * @param type Can be "ON_THE_FLY"  or "STORED". An On-the-fly calculator will do the computations using the graph,
	 * Stored uses pre-processed and stored relatedness values.
	 */
	public Relatedness(String lang,RelType type){
		this.lang=lang;
		switch(type){
		case ON_THE_FLY:{
			relate = new OnTheFlyArrayMeasure(lang);
			break;
			}
		case STORED:{
			relate=new StoredMeasure(lang);
			break;
			}
		
		
		}
	}
	
	/**@param topic_a The Wikipedia-id of the first topic.
	 * @param topic_b The Wikipedia-id of the second topic.
	 * @return the relatedness between the two topics.
	 */
	public float rel(int topic_a,int topic_b){
		return relate.rel(topic_a, topic_b);
	}
}

