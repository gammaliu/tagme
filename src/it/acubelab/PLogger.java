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
package it.acubelab;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLongArray;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class PLogger {

	public static enum Step {
		SECOND,
		TEN_SECONDS,
		MINUTE, 
		TEN_MINUTES,
		HALF_OUR;
	}
	
	public static long ONE_HOUR = 60*60*1000; 
	
	
	class LogTasker extends TimerTask
	{
		@Override
		public void run() 
		{
			log(false);
		}
	}
	

	protected Logger log;
	protected Level level;
	protected String[] itemNames;
	
	//Not yet implemented
//	protected AtomicInteger threads;
	protected AtomicLongArray counts;
	long endCount;
	int endIndex;
	long period;
	long start, stop;
	String logMsg;
	String logEnd;
	boolean printMemoryStatus = false;
	
	Timer scheduler;
	
	public PLogger(Logger log) {
		this(log, Level.INFO, Step.TEN_SECONDS, "items");
	}
	public PLogger(Logger log, Step step) {
		this(log, Level.INFO, step, "items");
	}
	public PLogger(Logger log, String... itemNames) {
		this(log, Level.INFO, Step.TEN_SECONDS, itemNames);
	}
	public PLogger(Logger log, Step step, String... itemNames) {
		this(log, Level.INFO, step, itemNames);
	}
	public PLogger(Logger log, Level level, Step step, String... itemNames) {
		super();
		this.log = log;
		this.level = level;
		
		this.itemNames = itemNames;
		if (itemNames != null){
			counts = new AtomicLongArray(itemNames.length);
			StringBuilder buf = new StringBuilder("Job: ");
			for(int i=0;i<itemNames.length;i++){
				if (i>0)buf.append(", ");
				buf.append("%,d ");
				buf.append(itemNames[i]);
			}
			//buf.append(". %TT elapsed");
			buf.append(".");
			logMsg = buf.toString();
		} else {
			counts = new AtomicLongArray(1); 
			logMsg = "Job: %,d items.";
		}
		this.logEnd = " %.1f%% ET: %02dh:%02dm";
		
		switch(step){
		case SECOND : this.period = 1000; break;
		case TEN_SECONDS: this.period = 10000; break;
		case MINUTE : this.period = 1000 * 60; break;
		case TEN_MINUTES : this.period = 10 * 1000 * 60; break;
		case HALF_OUR : this.period = 30 * 1000 * 60; break;
		}
		
		this.endIndex = -1;
		this.endCount = -1;
		this.scheduler = null;
	}
	
	public PLogger setEnd(long end){
		return setEnd(0, end);
	}
	public PLogger setEnd(int index, long end){
		if (index >= itemNames.length) throw new IndexOutOfBoundsException();
		this.endIndex = index;
		this.endCount = end;
		return this;
	}
	public PLogger printMemoryStatus(boolean print){
		this.printMemoryStatus = print;
		return this;
	}
	
	public PLogger start(){
		return start("Job started...");
	}
	public PLogger start(String msg)
	{
		if (msg != null)log.log(level, msg);
		for(int i=0;i<counts.length(); i++) counts.set(i, 0);
		start = System.currentTimeMillis();
		stop = -1;
		scheduler = new Timer();
		scheduler.scheduleAtFixedRate(new LogTasker(), period, period);
		return this;
	}
	public void update(){
		update(0,1);
	}
	public void update(int index, int delta){
		counts.addAndGet(index, delta);
	}
	public void update(int index){
		update(index,1);
	}
	public void updateConstant(String index){
		updateConstant(index,1);
	}
	public void updateConstant(String index, int delta) {
		for(int i=0;i<itemNames.length; i++)
			if (itemNames[i] == index){
				update(i,delta);
				return;
			}
	}
	public void update(String index){
		update(index,1);
	}
	public void update(String index, int delta) {
		for(int i=0;i<itemNames.length; i++)
			if (itemNames[i].equalsIgnoreCase(index)){
				update(i,delta);
				return;
			}
	}
	
	public void log(boolean stop)
	{
		String logstring = format();
		
		if (!stop && endCount > 0)
			logstring += formatEnd();
		
		if (printMemoryStatus)
			logstring += " "+ExternalSortUtils.memSize(false);
		
		log.log(level, logstring);
	}

	protected String format(){
		Object[] params = new Object[counts.length()];
		for(int i=0; i<counts.length(); i++) params[i] = counts.get(i);
		return String.format(Locale.US, logMsg, params);
	}
	protected String formatEnd()
	{
		long currentTime = System.currentTimeMillis();
		double completed = (double)counts.get(endIndex) / (double)endCount;
		if (completed > 1) return String.format(Locale.US, " [End reached=%,d]", counts.get(endIndex));
		else {
			long elapsed = currentTime - start;
			long total = (long) (elapsed / completed);
			long time_to_end = total - elapsed;
			long time_to_end_minutes = (time_to_end / (1000*60)) % 60;
			long time_to_end_hours = (time_to_end / (1000*60*60));
			return String.format(Locale.US, logEnd, completed*100.0, time_to_end_hours, time_to_end_minutes);
		}
	}
	
	public void stop()
	{
		if (scheduler == null) return;
		stop = System.currentTimeMillis();
		scheduler.cancel();
		log(true);
		log.log(level, "Job terminated in "+String.format(Locale.US, " %TT", (stop-start-ONE_HOUR))); 
	}

	
	public long counts(){
		return counts.get(0);
	}
	public long counts(int index){
		return counts.get(index);
	}
	
	public int size(){
		return this.counts.length();
	}
	
	
}
