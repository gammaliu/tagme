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

import it.acubelab.tagme.preprocessing.TopicSearcher;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Config {
	
	
	public static enum IndexType
	{
	
		GRAPH("graph.WikipediaGraph"),
		IN_GRAPH("graph.WikipediaInGraph");
		
		final String value;
		IndexType(String v){
			value = v;
		}
	
	}
	
	public static enum WikipediaFiles
	{
		TITLES ("wiki-latest-page.sql"),
		TITLES_NS0 ("wiki-latest-all-titles-in-ns0"),
		ARTICLES ("wiki-latest-pages-articles.xml"),
		ABSTRACTS ("wiki-latest-abstract.xml"),
		PAGE_LINKS ("wiki-latest-pagelinks.sql"),
		CATEGORIES ("wiki-latest-category.sql"),
		CAT_LINKS ("wiki-latest-categorylinks.sql"),
		REDIRECTS ("wiki-latest-redirect.sql"),
		DBPEDIA_CAT ("article_categories.ttl");
		
		final String filename;
		WikipediaFiles(String n){
			filename = n;
		}
		public File getSourceFile(String lang){
			return new File(RepositoryDirs.SOURCE.getDir(lang), lang+filename);
		}
		public String getSourcePath(WikipediaFiles f, String lang){
			return getSourceFile(lang).getAbsolutePath();
		}

	}
	
	public static enum RepositoryDirs
	{
		SOURCE,
		SUPPORT,
		ANCHORS,
		OLD_ANCHORS,
		ANCHOR_TRIE,
		RELATEDNESS_GRAPH,
		TOPICS,
		WIKIPEDIA,
		GRAPH,
		IN_GRAPH;
		
		public File getDir(String lang)
		{
			String repo = TagmeConfig.get().getRepositoryDir();
			File dir = new File(repo, lang);
			return new File(dir.getAbsolutePath(), this.name().toLowerCase());
		}
		public String getPath(String lang)
		{
			return getDir(lang).getAbsolutePath();
		}

	}

	
	public Config(){}
	
	String loggingFilePath;
	String stopwordDir;
	String repositoryDir;

	Object2IntMap<String> degree;
	Set<String> langs = new HashSet<String>();
	List<String> inits = new ArrayList<String>();
	HashMap<String, ParamSetting> settings = new HashMap<String, ParamSetting>();
	
	public void setDegree(String l,int d){
		degree.put(l,d);
	}
	
	
	
	public int getDegree(String l){
		if(degree==null){
			degree=new Object2IntOpenHashMap<String>();
//			for(String lang:langs){
//				IntSet allwid= DatasetLoader.get(new AllWIDs(lang));
//				IntSet ignore = DatasetLoader.get(new IgnoreWIDs(lang));
//				IntSet disambiguation = DatasetLoader.get(new DisambiguationWIDs(lang));
//				IntSet list = DatasetLoader.get(new ListPageWIDs(lang));
//				Int2IntMap redirect = DatasetLoader.get(new RedirectMap(lang));
//				allwid.removeAll(ignore);
//				allwid.removeAll(list);
//				allwid.removeAll(redirect.keySet());
//				allwid.removeAll(disambiguation);
		}
		try{
			degree.put(l, new TopicSearcher(l).numTopics());
		} catch (IOException ioe) {
				throw new ConfigurationException("Unable to load Topic Index", ioe);
		}
		
		return degree.get(l);
	}
	
	public String getLoggingFilePath() {
		return loggingFilePath;
	}
	public void setLoggingFilePath(String loggingFilePath) {
		this.loggingFilePath = loggingFilePath;
	}
	public void addLang(String lang) {
		this.langs.add(lang);
	}
	public Set<String> getLangs(){
		return this.langs;
	}
	public void addInit(String clazz){
		this.inits.add(clazz);
	}
	public List<String> getInits(){
		return this.inits;
	}
	public void addSetting(ParamSetting s){
		this.settings.put(s.getName(), s);
	}
	static final ParamSetting EMPTY_SETTING = new ParamSetting();
	public ParamSetting getSetting(String name){
		if (settings.containsKey(name))
			return settings.get(name);
		else return EMPTY_SETTING;
	}
	public String getStopwordDir() {
		return stopwordDir;
	}
	public void setStopwordDir(String stopwordDir) {
		this.stopwordDir = stopwordDir;
	}
	public String getRepositoryDir() {
		return repositoryDir;
	}
	public void setRepositoryDir(String repositoryDir) {
		this.repositoryDir = repositoryDir;
	}

}
