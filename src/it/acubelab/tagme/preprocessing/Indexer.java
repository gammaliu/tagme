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

import it.acubelab.tagme.config.Config.RepositoryDirs;
import it.acubelab.tagme.config.Config.WikipediaFiles;
import it.acubelab.tagme.config.ConfigurationException;
import it.acubelab.tagme.config.TagmeConfig;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.net.SMTPAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.TriggeringEventEvaluator;
import org.apache.lucene.index.IndexWriter;

public abstract class Indexer {
	
	static Logger log = Logger.getLogger(Indexer.class);
	
//	
//	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception
	{
		TagmeConfig.init();
		
		Vector<Indexer> indexes = new Vector<Indexer>();
		Vector<String> langs = new Vector<String>();
		
		
		if (args == null || args.length < 2)
			printUsage();
		
		//LANGUAGES
		if (args[1].equals("all")){
			for(String l : TagmeConfig.get().getLangs())
				langs.add(l);
		} else {
			if (!TagmeConfig.get().getLangs().contains(args[1])) 
				printUsage();
			else 
				langs.add(args[1]);
		}
		
		//INDEXES
		String[] types = args[0].split("\\+");
		
		for(String t : types){
			t = t.trim();
			Indexer i = searchFor(t);
			if (i == null) printUsage(t);
			else indexes.add(i);
		}
		
		Logger logMail = null;
		if (args.length > 2 && args[2].contains("@")){
			SMTPAppender emailer = new SMTPAppender(new TriggeringEventEvaluator() {
				@Override
				public boolean isTriggeringEvent(LoggingEvent e) {
					return true;
				}
			});
			emailer.setFrom("tagme@di.unipi.it");
			emailer.setTo(args[2]);
			emailer.setSMTPHost("localhost");
			emailer.setSubject("TAGME Preprocessing notification");
			emailer.setLayout(new PatternLayout("%d %-5p %c{1} - %m%n"));
			emailer.setBufferSize(50);
			emailer.activateOptions();
			logMail = Logger.getLogger("it.acubelab.tagme.MAIL");
			logMail.addAppender(emailer);
		}
		
		
//		RUNNING
		try {
			log.info("Perform indexes build process...");
			for(Indexer i : indexes)
			{
				log.info(">>> "+i.getName().toUpperCase());
				
				for(String lang : langs)
				{
					log.info(" >> "+lang.toUpperCase());
					try {
						makeIndex(i, lang);
					} catch (RuntimeException t){
						throw new Exception("Error during indexing: "+i.getName().toUpperCase()+ " LANG="+lang, t);
					} catch (Throwable t){
						throw new Exception("Error during indexing: "+i.getName().toUpperCase()+ " LANG="+lang, t);
					}
					log.info(" << "+lang.toUpperCase());
				}
				
				log.info("<<< "+i.getName().toUpperCase());
			}
			log.info("Done.");
		} catch (Throwable t){
			log.fatal("ERROR during processing!", t);
			if (logMail!=null) logMail.fatal("ERROR during processing!", t);
			System.exit(1);
		}
		if (logMail != null) 
		{
			logMail.info("Process executed!");
			logMail.info("LANG = "+args[1]);
			logMail.info("INDEXES = "+args[0]);
		}
		
	}
	
	static final String SEARCH_BASEPATH = "it.acubelab.tagme.";
	static final String[] SEARCH_PACKAGES = {
		"",
		"preprocessing.", 
		"preprocessing.support.", 
		"preprocessing.anchors.",
		"preprocessing.graphs."
	};
	@SuppressWarnings("unchecked")
	static Indexer searchFor(String name)
	{
		for(String pkg : SEARCH_PACKAGES){
			try{
				Class<?> clazz = Class.forName(SEARCH_BASEPATH+pkg+name);
				if (Dataset.class.isAssignableFrom(clazz)){
					return new DatasetProcessor((Class<? extends Dataset<?>>) clazz);
				} else if (TextDataset.class.isAssignableFrom(clazz)){
					return new TextDatasetProcessor((Class<? extends TextDataset>) clazz);
				} else if (Indexer.class.isAssignableFrom(clazz)){
					return (Indexer)clazz.newInstance();
				}
				
			} 
			catch (ClassNotFoundException cnfe){} 
			catch (InstantiationException e) {}
			catch (IllegalAccessException e) {}
		}
		return null;
	}
	
	static void printUsage(){
		printUsage(null);
	}
	static void printUsage(String error)
	{
		StringBuffer b = new StringBuffer();
		if (error != null)
			b.append("Unrecognized argument: ["+error+"]\n");
		b.append("USAGE: Indexer <index_type> <lang> <mailonerror>\n");
		
		b.append("\t<index_Type> = list values of  " +
				"{<Indexer types> | <Dataset<?> types>} separated by a '+' (plus) char");
		
		b.append("\n\t<lang> = {all ");
		for(String lang : TagmeConfig.get().getLangs())
			b.append("| "+lang);
		b.append("}");
		
		System.out.println(b.toString());
		
		System.exit(0);
	}
//	
//
//	public static File getCleanTmpDir(Indexer indexer, Language lang) throws IOException
//	{
//		File tmpBase = SupportFile.getTmpDir();
//		File tmpDir = new File(tmpBase, lang.getCode()+"-"+indexer.getName());
//		tmpDir.mkdirs();
//		FileUtils.cleanDirectory(tmpDir);
//		return tmpDir;
//	}
	
	public static void makeIndex(Indexer indexer, String lang) throws IOException
	{
		//WORKING DIRECTORY
		File tmpBase = Dataset.getTmpDir();
		File workingDir = new File(tmpBase, lang+"_"+indexer.getName());
		workingDir.mkdirs();
		FileUtils.cleanDirectory(workingDir);
		
		log.info("\tStart indexer "+indexer.getName()+"...");
		indexer.makeIndex(lang, workingDir);
		log.info("\tBuilt index for "+indexer.getName());
		
		if (! (indexer instanceof DatasetProcessor || indexer instanceof TextDatasetProcessor)){
			
			log.info("\tCopying files for "+indexer.getName()+"...");
			File realPlace = indexer.getIndexDir(lang);
		
			if (realPlace.exists()) FileUtils.cleanDirectory(realPlace);
			else realPlace.mkdirs();

			FileUtils.copyDirectory(workingDir, realPlace);
				
			log.info("\tRemoving tmp files for "+indexer.getName()+"...");
			
		}
		FileUtils.deleteDirectory(workingDir);
		log.info("\tProcess done for "+indexer.getName()+"!");
			
	}
	

	public static File getFile(WikipediaFiles type, String lang){
		File f = type.getSourceFile(lang);
		if (!f.exists())
			throw new ConfigurationException("Unable to find wikipedia dump in "+f.getAbsolutePath());
		return f;
	}
	public static File getDir(RepositoryDirs type, String lang) {
		File d = type.getDir(lang);
		if (!d.exists() && !d.mkdirs())
			throw new ConfigurationException("Cannot create file "+d.getAbsolutePath());
		return d;
	}
	
	public static File getIndexDir(RepositoryDirs t, String lang){
		return t.getDir(lang);
	}

	
	/**
	 * 
	 *       INSTANCE
	 * 
	 */
	///////////////////////////////////////////////////////////////////////////
	protected void closeIndex(IndexWriter index) throws IOException
	{
		try {
			if (index != null)
				index.close();
		} finally {
			try {
				if (IndexWriter.isLocked(index.getDirectory()))
					IndexWriter.unlock(index.getDirectory());
			} catch (Exception e) { }
		}
	}
	public abstract void makeIndex(String lang, File workingDir) throws IOException;

	public abstract File getIndexDir(String lang);
	
	public String getName(){
		return this.getClass().getSimpleName();
	}
	///////////////////////////////////////////////////////////////////////////

	
	/**
	 * 
	 * CONTAINER FOR Dataset<?> TO BE PROCESSED
	 *
	 */
	private static class DatasetProcessor extends Indexer
	{
		Class<? extends Dataset<?>> theclass;
		
		public DatasetProcessor(Class<? extends Dataset<?>> theclass){
			this.theclass = theclass;
		}
		
		@Override
		public File getIndexDir(String languageCode) {
			return getDir(RepositoryDirs.SUPPORT, languageCode);
		}

		@Override
		public String getName() {
			return theclass.getSimpleName();
		}

		@Override
		public void makeIndex(String languageCode, File workingDir) throws IOException {
			Dataset<?> ss = null;
			try{
				ss = theclass.getConstructor(String.class).newInstance(languageCode);
			} catch (Exception e){
				throw new IOException("Unable to instantiate class: " +theclass.getName());
			}
			ss.forceParsing();
		}
	}
	
	/**
	 * 
	 * CONTAINER FOR TextDataset TO BE PROCESSED
	 *
	 */
	private static class TextDatasetProcessor extends Indexer
	{
		Class<? extends TextDataset> theclass;
		
		public TextDatasetProcessor(Class<? extends TextDataset> theclass){
			this.theclass = theclass;
		}
		
		@Override
		public File getIndexDir(String languageCode) {
			return getDir(RepositoryDirs.SUPPORT, languageCode);
		}

		@Override
		public String getName() {
			return theclass.getSimpleName();
		}

		@Override
		public void makeIndex(String languageCode, File workingDir) throws IOException {
			TextDataset ss = null;
			try{
				ss = theclass.getConstructor(String.class).newInstance(languageCode);
			} catch (Exception e){
				throw new IOException("Unable to instantiate class: " +theclass.getName());
			}
			ss.forceParsing();
		}
	}

}
