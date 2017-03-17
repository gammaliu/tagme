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

import it.acubelab.tagme.config.ConfigurationException;
import it.acubelab.tagme.config.TagmeConfig;
import it.acubelab.tagme.preprocessing.graphs.NodeStoredMeasure;
import it.acubelab.tagme.preprocessing.graphs.OnTheFlyArrayMeasure;
import it.acubelab.tagme.preprocessing.graphs.OnTheFlyMeasure;
import it.acubelab.tagme.preprocessing.graphs.StoredMeasure;

public abstract class RelatednessMeasure {

	public static final float THRESHOLD = 1f;
	public static final int MIN_INTERSECTION = 2;
	
	public static final String PARAM_RELATEDNESS = "relatedness";
	public static final RelType DEFAULT_RELATEDNESS = RelType.ARRAY_GRAPH;
	
	public static enum RelType{
		COMPRESSED_GRAPH,
		ARRAY_GRAPH,
		MATRIX,
		NODE_MATRIX;
		public RelatednessMeasure create(String lang){
			switch(this){
			case COMPRESSED_GRAPH: return new OnTheFlyMeasure(lang);
			case ARRAY_GRAPH: return new OnTheFlyArrayMeasure(lang);
			case MATRIX: return new StoredMeasure(lang);
			case NODE_MATRIX: return new NodeStoredMeasure(lang);
			}
			throw new ConfigurationException("No registered type for relatedness: "+this.name());
		}
	}
	public static RelatednessMeasure create(String lang) {
		RelType relatedness = TagmeConfig.get().getSetting(Annotation.MODULE).getEnumParam(RelatednessMeasure.PARAM_RELATEDNESS, RelatednessMeasure.DEFAULT_RELATEDNESS, RelType.class);
		return relatedness.create(lang);
	}
	
	protected String lang;
	public RelatednessMeasure(String lang){
		this.lang=lang;
	}
	
	public String getLang(){
		return this.lang;
	}
	
	public abstract float rel(int a,int b);
	

}
