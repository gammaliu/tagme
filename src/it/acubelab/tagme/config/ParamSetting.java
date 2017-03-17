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

import java.util.HashMap;

public class ParamSetting extends HashMap<String, String> 
{
	private static final long serialVersionUID = 7279049902405872730L;
	String name;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public void addParam(String name, String value){
		this.put(name, value);
	}
	
	public String getParam(String name)
	{
		if (!this.containsKey(name))
			throw new ConfigurationException("Unable to find required param ["+name+"]");
		return this.get(name);
	}
	public String getParam(String name, String def)
	{
		if (!this.containsKey(name))
			return def;
		return this.get(name);
	}
	public int getIntParam(String name, int def)
	{
		if (!this.containsKey(name)) return def;
		try {
			return Integer.parseInt(this.get(name));
		} catch (NumberFormatException nfe){
			throw new ConfigurationException("Unable to parse required int param ["+name+"]");
		}
	}
	public int getIntParam(String name)
	{
		if (!this.containsKey(name)) throw new ConfigurationException("Unable to find required param ["+name+"]");
		try {
			return Integer.parseInt(this.get(name));
		} catch (NumberFormatException nfe){
			throw new ConfigurationException("Unable to parse required int param ["+name+"]");
		}
	}
	public float getFloatParam(String name, float def)
	{
		if (!this.containsKey(name)) return def;
		try {
			return Float.parseFloat(this.get(name));
		} catch (NumberFormatException nfe){
			throw new ConfigurationException("Unable to parse required float param ["+name+"]");
		}
	}
	public float getFloatParam(String name)
	{
		if (!this.containsKey(name)) throw new ConfigurationException("Unable to find required param ["+name+"]");
		try {
			return Float.parseFloat(this.get(name));
		} catch (NumberFormatException nfe){
			throw new ConfigurationException("Unable to parse required float param ["+name+"]");
		}
	}
	public boolean getBooleanParam(String name, boolean def)
	{
		if (!this.containsKey(name)) return def;
		return Boolean.parseBoolean(this.get(name));
	}
	public boolean getBooleanParam(String name)
	{
		if (!this.containsKey(name)) throw new ConfigurationException("Unable to find required param ["+name+"]");
		return Boolean.parseBoolean(this.get(name));
	}
	
	public <T extends Enum<T>> T getEnumParam(String name, Class<T> type)
	{
		if (!this.containsKey(name)) throw new ConfigurationException("Unable to find required param ["+name+"]");
		return Enum.valueOf(type, this.get(name));
	}

	public <T extends Enum<T>> T getEnumParam(String name, T def, Class<T> type)
	{
		if (!this.containsKey(name)) return def;
		return Enum.valueOf(type, this.get(name));
	}
	
}
