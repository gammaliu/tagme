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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.digester3.Digester;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.xml.sax.SAXException;

public class TagmeConfig {

	public static final boolean DEFAULT_INITIALIZE = true;

	public static final String INIT_METHOD = "init";
	public static final String CONFIG_PATH_PROP = "tagme.config";


	static Logger log = Logger.getLogger(TagmeConfig.class);

	public static final String
	GERMAN  = "de",
	ENGLISH = "en",
	SPANISH = "es",
	FRENCH  = "fr",
	ITALIAN = "it";

	private static volatile Config conf = null;
	private static String filepath = null;
	private TagmeConfig(){}

	public static Config get()
	{
		if (conf == null)
			throw new ConfigurationException("You must call the init method for configuring TAGME!");
		return conf;
	}


	protected static void parseConfig(String configFilePath) throws ConfigurationException
	{
		try {
			File configFile = new File(configFilePath);

			//avoids logging warning while parsing config
			Logger digesterLog = Logger.getLogger("org.apache.commons");
			digesterLog.setLevel(Level.ERROR);

			Digester digester = new Digester();
			digester.setValidating(false);
			setRules(digester);

			conf = (Config)digester.parse(configFile);

		} catch (Exception e) {
			throw new ConfigurationException("Error while parsing config file (Path:"+configFilePath+")", e);
		}

		try {

			DOMConfigurator.configure(conf.getLoggingFilePath());
			log.info("Log4J configured. (file:"+conf.getLoggingFilePath()+")");
			log.info("Configuration done. (file:"+configFilePath+")");

		} catch (Exception e){
			throw new ConfigurationException("Error while configuring Log4j (Path:"+conf.getLoggingFilePath()+")", e);
		}

	}
	public static void init(String configFilePath){
		init(configFilePath, DEFAULT_INITIALIZE);
	}
	public static void init(String configFilePath, boolean initialize)
	{
		if (conf == null)
		{
			synchronized(TagmeConfig.class)
			{
				if (conf == null)
				{
					//parse config
					if (configFilePath == null || configFilePath.length() == 0) {
						//search config file in JVM property JUST FOR DEBUGGING PURPOSE
						configFilePath = System.getProperty(CONFIG_PATH_PROP);
						if (configFilePath == null || configFilePath.length() == 0)
							throw new ConfigurationException("The path of TAGME config file is empty and the JVM System Property "+CONFIG_PATH_PROP+" is not set");
					}
					parseConfig(configFilePath);

					filepath = configFilePath;


					if (initialize)
					{
						log.info("Initializing methods...");
						for(String i : get().getInits())
						{
							try {

								Class<?> c = Class.forName(i);
								Method m = c.getMethod(INIT_METHOD);
								m.invoke(null);
								log.info("\t"+c.getSimpleName()+": Initialization done.");

							} catch (ClassNotFoundException cnfe) {
								log.error("\t"+i+": Unable to find initialization class", cnfe);
							} catch (Exception e){
								log.error("\t"+i+": Error while invoking init method", e);
							}
						}
					}
					log.info("Startup proccess successfully completed.");
				}
			}
		}


//			for(String lang:conf.getLangs()){
//				IntSet allwid= DatasetLoader.get(new AllWIDs(lang));
//				IntSet ignore = DatasetLoader.get(new IgnoreWIDs(lang));
//				IntSet disambiguation = DatasetLoader.get(new DisambiguationWIDs(lang));
//				//IntSet list = DatasetLoader.get(new ListPageWIDs(lang));
//				Int2IntMap redirect = DatasetLoader.get(new RedirectMap(lang));
//				allwid.removeAll(ignore);
//				//.removeAll(list);
//				allwid.removeAll(redirect.keySet());
//				allwid.removeAll(disambiguation);
//				conf.setDegree(lang, allwid.size());
//			}
		
	}

	public static void init()
	{
		init(null);
	}

	protected static void setRules(Digester d)
	{
		d.addObjectCreate("tagme", Config.class);

		d.addCallMethod("tagme/logConfiguration", "setLoggingFilePath", 0);
		d.addCallMethod("tagme/stopword", "setStopwordDir", 0);
		d.addCallMethod("tagme/repository", "setRepositoryDir", 0);

		d.addCallMethod("tagme/inits/class", "addInit", 0);
		d.addCallMethod("tagme/langs/lang", "addLang", 0);

		d.addObjectCreate("tagme/setting", ParamSetting.class);
		d.addSetNext("tagme/setting", "addSetting", ParamSetting.class.getName());
		d.addCallMethod("tagme/setting", "setName", 1);
		d.addCallParam("tagme/setting", 0, "name");

		d.addCallMethod("tagme/setting/param", "addParam", 2);
		d.addCallParam("tagme/setting/param", 0, "name");
		d.addCallParam("tagme/setting/param", 1, "value");

	}

	public static final String JNDI_CONFIG_FILE_PATH_PARAM = "configFilePath";

	/**
	 * 	In the XML context configuration you have to set this environment variable:<br/>
	 * 	<pre>
	 	&lt;Context ...>
  			&lt;Environment name="configFilePath" value="....."
  				type="java.lang.String" override="false"/>
		&lt;/Context></pre>
		In the web.xml of the application you have to insert the refs to that variable<br/>
		<pre>
		&lt;env-entry>
  			&lt;env-entry-name>configFilePath&lt;/param-name>
  			&lt;env-entry-type>java.lang.String&lt;/env-entry-type>
		&lt;/env-entry></pre>
	 * @param moduleName
	 * @return
	 */
	public static ModuleConfig parseModuleConfigFromJNDI(String moduleName)
	{
		get();

		String configFilePath = null;
		try {
			configFilePath = (String) ((Context)  new InitialContext().lookup("java:comp/env")).lookup(JNDI_CONFIG_FILE_PATH_PARAM);
		} catch (NamingException ne) {}
		if (configFilePath == null) throw new ConfigurationException("Unable to find parameter "+JNDI_CONFIG_FILE_PATH_PARAM+" in JNDI environment");

		return parseModuleConfig(moduleName, configFilePath);
	}
	/**
	 * Parse the main TAGME config file
	 * @param moduleName
	 * @return
	 */
	public static ModuleConfig parseModuleConfig(String moduleName)
	{
		return parseModuleConfig(moduleName, filepath);
	}
	public static ModuleConfig parseModuleConfig(String moduleName, String module_filepath)
	{
		get();

		Digester d = new Digester();
		d.setValidating(false);

		ModuleConfig mc = new ModuleConfig();
		mc.setName(moduleName);

		d.push(mc);

		d.addCallMethod("*/"+moduleName+"/param", "addParam", 2);
		d.addCallParam("*/"+moduleName+"/param", 0, "name");
		d.addCallParam("*/"+moduleName+"/param", 1, "value");

		//		d.addCallMethod("*/"+moduleName+"/inits/class", "addInit", 0);

		try {
			d.parse(new File(module_filepath));
		} catch (IOException e) {
			throw new ConfigurationException("Error while parsing configuration for module "+moduleName,e);
		} catch (SAXException e) {
			throw new ConfigurationException("Error while parsing configuration for module "+moduleName,e);
		}
		mc.setConfigFilePath(module_filepath);
		log.info("Module ["+moduleName+"] configured. ("+module_filepath+")");
		return mc;
	}


	public static void main(String[] args ) throws Throwable
	{
		TagmeConfig.init("/home/d.vitale/workspace/tagme/config.xml", false);

		Config c = TagmeConfig.get();
		System.out.println(c);
	}
}
