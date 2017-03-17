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
package it.acubelab.tagme.preprocessing;

import it.acubelab.tagme.config.ConfigurationException;
import it.acubelab.tagme.config.TagmeConfig;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Pattern;

public class WikiPatterns {

	
	public static final String FILENAME = "wikipatterns.properties";
	public static enum Type{
		DISAMB_CAT,
		DISAMB_SUFFIX,
		LIST_CAT,
		PEOPLE_CAT,
		ANCHOR_START,
		ANCHOR_STOPWORDS,
		PAGE_DATE,
		PAGE_IGNORE
	}
	
	private static Properties props = null;
	private static HashMap<String, WordTrie> anchor_sw = new HashMap<String, WordTrie>();
	
	public static String getPatternString(String lang, Type t){
		if (props == null){
			synchronized(WikiPatterns.class)
			{
				if (props == null){
					props = new Properties();
					File f = new File(TagmeConfig.get().getRepositoryDir(), FILENAME);
					InputStreamReader in = null;
					try{
						in = new InputStreamReader(new FileInputStream(f), Charset.forName("UTF-8"));
						props.load(in);
					} catch (IOException ioe){
						props = null;
						throw new ConfigurationException(ioe);
					} finally {
						try {in.close();}
						catch (Exception e){}
					}
				}
			}
		}
		return props.getProperty(lang+"."+t.name().toLowerCase());
	}
	public static Pattern getPattern(String lang, Type t){
		return Pattern.compile(getPatternString(lang, t));
	}
	public static WordTrie getWordSet(String lang, Type t){
		String wordlist = getPatternString(lang, t);
		ObjectOpenHashSet<String> words = new ObjectOpenHashSet<String>(wordlist.split(","));
		return new WordTrie(words);
	}
	
	public static WordTrie getAnchorStopwords(String lang){
		WordTrie sw = anchor_sw.get(lang);
		if (sw == null){
			synchronized (WikiPatterns.class) {
				if (!anchor_sw.containsKey(lang)){
					anchor_sw.put(lang, getWordSet(lang, Type.ANCHOR_STOPWORDS));
				}
			}
		}
		return anchor_sw.get(lang);
	}
	

}
