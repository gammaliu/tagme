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

import it.acubelab.ExternalSortUtils;
import it.acubelab.tagme.config.Config.RepositoryDirs;
import it.acubelab.tagme.config.ConfigurationException;
import it.acubelab.tagme.config.TagmeConfig;
import it.unimi.dsi.lang.MutableString;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;

public abstract class Dataset<T> 
{
	protected static Logger log = Logger.getLogger(Dataset.class);
	
	public static final String TMP_DIR_NAME = "tmp", TMP_NAME = ".tagme.tmp";
	public static File getTmpDir(){
		File dir = new File(TagmeConfig.get().getRepositoryDir(), TMP_DIR_NAME);
		dir.mkdirs();
		return dir;
	}
	public static File createTmpFile(){
		return new File(getTmpDir(), System.currentTimeMillis()+TMP_NAME);
	}
	
	protected String file;
	protected String lang;
	protected Dataset(String lang){
		this.file = this.getClass().getSimpleName();
		this.lang = lang;
	}
	@Override
	public String toString(){
		return "["+lang.toUpperCase()+"/"+file+"]";
	}
	
	@SuppressWarnings("unchecked")
	public T restoreDataset () throws IOException
	{
		log.info(toString()+" Restoring (Memory "+ExternalSortUtils.memSize(false)+")...");
		ObjectInputStream ois = null;
		try {
			File dir = RepositoryDirs.SUPPORT.getDir(lang);
			File f = new File(dir, file);
			if (f.exists()){
				ois = new ObjectInputStream(new FileInputStream(f));
				try {
					T set = (T) ois.readObject();
					return set;
				} catch (ClassNotFoundException e) {
					return null;
				}
			} else return null;
		} finally{
			try {ois.close();}
			catch (Exception e){}
		}
	}
	
	protected void storeDataset(Object obj) throws IOException
	{
		File dir = RepositoryDirs.SUPPORT.getDir(lang);
		File f = new File(dir, file);
		if (f.exists()) f.delete();
		f.createNewFile();
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new FileOutputStream(f));
			oos.writeObject(obj);
		} finally{
			try{ oos.close();}
			catch(Exception e) {}
		}
	}
	protected int getSize(T obj){
		if (obj instanceof Collection<?>) return ((Collection<?>)obj).size();
		else if (obj instanceof Map<?,?>) return ((Map<?,?>)obj).size();
		else return -1;
	}
	protected String printSize(T obj){
		int s = getSize(obj);
		return s>=0 ? " ("+s+" items)":"";
	}
	
	public T getDataset() throws IOException
	{
		T old = restoreDataset();
		if (old != null) {
			log.info(toString()+" Found"+printSize(old));
			return old;
		}
		else {
			log.info(toString()+" Unable to find stored set, now parsing...");
			File dir = RepositoryDirs.SUPPORT.getDir(lang);
			if (!dir.exists() && ! dir.mkdirs())
				throw new ConfigurationException("Unable to create directory "+dir.getAbsolutePath());

			T set = parseSet();
			log.info(toString()+" Storing set"+printSize(set)+"...");
			storeDataset(set);
			log.info(toString()+" Stored.");
			return set;
		}
	}
	
	public void forceParsing() throws IOException
	{
		log.info(toString()+" Force parsing...");
		T set = parseSet();
		log.info(toString()+" Storing set"+printSize(set)+"...");
		storeDataset(set);
		log.info(toString()+" Stored.");
	}
	
	@Override
	public boolean equals(Object o) {
		if (! o.getClass().equals(this.getClass())) return false;
		else return this.lang.equals(((Dataset<?>)o).lang);
	}
	@Override
	public int hashCode() {
		return new String(this.getClass().getSimpleName()+this.lang).hashCode();
	}
	
	protected abstract T parseSet() throws IOException;
	
	public String getFile() {
		return file;
	}
	public String getLang() {
		return lang;
	}

	public static String cleanPageName(String name){
		return name.replace('_', ' ');
	}
	public static MutableString cleanPageName(MutableString name){
		return name.replace('_', ' ');
	}

}
