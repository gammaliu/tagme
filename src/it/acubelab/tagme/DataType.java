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
import it.acubelab.tagme.preprocessing.anchors.AnchorSearcher;
import it.acubelab.tagme.preprocessing.anchors.AnchorTernaryTrie;
import it.acubelab.tagme.preprocessing.anchors.AnchorTrie;
import it.acubelab.tagme.preprocessing.anchors.LuceneAnchorSearcher;

import java.io.IOException;

public enum DataType{
	LUCENE,
	TRIE,
	TERNARY_TRIE;
	public AnchorSearcher create(String lang){
		try {
			switch(this){
			case LUCENE: return new LuceneAnchorSearcher(lang);
			case TRIE: return new AnchorTrie.Searcher(lang);
			case TERNARY_TRIE: return new AnchorTernaryTrie.Searcher(lang);
			}
			throw new ConfigurationException("No registered type for Anchor searcher: "+this.name());
		} catch (IOException ioe){
			throw new ConfigurationException("Unable to get data for Anchor searcher", ioe);
		}

	}

	public static AnchorSearcher createAnchorSearcher(String lang) {
		DataType data = TagmeConfig.get().getSetting(TagmeParser.MODULE).getEnumParam(TagmeParser.PARAM_DATA, TagmeParser.DEFAULT_DATA, DataType.class);
		return data.create(lang);
	}
}
