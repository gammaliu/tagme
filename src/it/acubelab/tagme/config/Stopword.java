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
package it.acubelab.tagme.config;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

public class Stopword 
{
	
	public static final String 
		WEB = "web",
		ALL = "all",
		EXTENSION = ".stopword";
	
	static Logger log = Logger.getLogger(Stopword.class); 
	
	static HashMap<String, Set<String>> stopwords = null;  
	
	public static Set<String> getSW(String langOrType){
		if (stopwords == null) throw new ConfigurationException("Stopword reader not yet initialized");
		return stopwords.get(langOrType);
	}
	
	public static HashSet<String> readFile(File stopwordFile) throws IOException
	{
		BufferedReader reader = null;
		try {
			log.debug("Reading file "+stopwordFile);
			HashSet<String> words = new HashSet<String>();
			reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(stopwordFile), Charset.forName("UTF-8")));
			String line = null;
			while((line=reader.readLine()) != null)
			{
				int i = line.indexOf('|');
				if (i >= 0)
					line = line.substring(0, i);
				line = line.trim();
				if (line.length() > 0){
					words.add(line);
				}
			}
			log.debug("Done. "+words.size()+" words read.");
			return words;
		} finally {
			try {reader.close();}
			catch(Exception e){}
		}
	}
	
	
	public static void init() throws IOException
	{
		log.debug("Reading all stopword file...");
		
		String dir = TagmeConfig.get().getStopwordDir();
		if ( dir == null || dir.length() == 0)
			throw new ConfigurationException("Unable to find stopword location");
		File path = new File(dir);
		
		HashSet<String> all = new HashSet<String>();
		stopwords = new HashMap<String, Set<String>>();
		
		for (String lang : TagmeConfig.get().getLangs())
		{
			log.debug("Reading stopword file for language "+lang);
			File f = new File(path, lang+EXTENSION);
			HashSet<String> sw = readFile(f);
			all.addAll(sw);
			
			stopwords.put(lang, sw);
		}
		//web stopwords
		File f = new File(path, WEB+EXTENSION);
		HashSet<String> sww = readFile(f);
		all.addAll(sww);
		
		stopwords.put(WEB, sww);
		stopwords.put(ALL, all);
				 
		log.info(all.size()+" stopwords successfully read.");
	}

}
