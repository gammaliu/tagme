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
import it.acubelab.tagme.config.ConfigurationException;
import it.acubelab.tagme.config.TagmeConfig;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

public abstract class TextDataset {

	public static Logger log = Logger.getLogger(TextDataset.class);
	
	public static final String SEP = "\t";
	public static final char SEP_CHAR = '\t';
	
	protected String file;
	protected String lang;
	protected TextDataset(String lang){
		this.file = this.getClass().getSimpleName();
		this.lang = lang;
	}

	@Override
	public String toString(){
		return "["+lang.toUpperCase()+"/"+file+"]";
	}
	protected File restoreFile() throws IOException
	{
		log.info(toString()+" Restoring file...");
		File dir = RepositoryDirs.SUPPORT.getDir(lang);
		File f = new File(dir, file);
		return f;
	}
	
	public File getFile() throws IOException
	{
		File old = restoreFile();
		if (old.exists()) {
			log.info(toString()+" Found ("+String.format("%,d", FileUtils.sizeOf(old)/FileUtils.ONE_MB)+" MBs)");
			return old;
		}
		else {
			File dir = RepositoryDirs.SUPPORT.getDir(lang);
			if (!dir.exists() && ! dir.mkdirs())
				throw new ConfigurationException("Unable to create directory "+dir.getAbsolutePath());

			log.info(toString()+" Unable to find TextDataset, now parsing...");
			File f = new File(getPath());
			parseFile(f);
			log.info(toString()+" Parsed.");
			return f;
		}
	}
	
	public void forceParsing() throws IOException
	{
		log.info(toString()+" Force parsing...");
		File f = new File(getPath());
		parseFile(f);
		log.info(toString()+" Parsed.");
	}
	
	protected String getPath(){
		File dir = RepositoryDirs.SUPPORT.getDir(lang);
		File f = new File(dir, file);
		return f.getAbsolutePath();
	}
	
	protected abstract void parseFile(File file) throws IOException;

}
