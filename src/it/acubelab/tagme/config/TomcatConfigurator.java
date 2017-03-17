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

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;


/**
 * You must add this listener to Tomcat configuration to automatically configure TAGME on Tomcat startup
 * Place the following tag on Server.xml of your Tomcat installation
 * <Listener className="it.acubelab.tagme.config.TomcatConfigurator"/></code>
 * Optionally, provide the config file path, adding an attibute to the tag above, eg.
 * <Listener className="it.acubelab.tagme.config.TomcatConfigurator" configFilePath="..." initialize="..." />
 * @author Administrator
 *
 */
public class TomcatConfigurator implements LifecycleListener {

	/**
	 * Whether to call init methods registered on tagme configurations (tag <init>)
	 */
	private static boolean initialize = true;
	private static String configFilePath = null;
	
	@Override
	public void lifecycleEvent(LifecycleEvent event) 
	{
		if (Lifecycle.START_EVENT.equals(event.getType()))
		{
			TagmeConfig.init(configFilePath, initialize);
		}
	}

	public void setConfigFilePath(String path)
	{
		configFilePath = path;
	}
	public void setInitialize(String init)
	{
		try{ initialize = Boolean.parseBoolean(init);}
		catch(Exception e){}
	}
	
}
