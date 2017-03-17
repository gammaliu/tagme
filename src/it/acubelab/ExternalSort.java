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

import it.acubelab.tagme.preprocessing.Dataset;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.lang.MutableString;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * A multi-way external merge sort that use a TreeMap (Java Red-Black Tree) as 
 * internal sorting data structure. This implementation is 100% pure Java and 
 * is able to scale over GBs of data better than the unix <code>sort</code> command.
 * <br><br>
 * This class has a main method so that it can be used via command line in a easy way.
 * Being a standard Java class it can be instantiated and configured by means of
 * its "set" methods. The sort process can be started by the {@link #run()} method.
 * By default this class reads from stdin and write in stdout. This behavior can
 * be changed using the {@link #setInFile(String)} and the {@link #setOutFile(String)}
 * method.
 * 
 * @author Claudio Corsi
 * @author Paolo Ferragina
 * 
 */
public class ExternalSort {
	
	public static final long DEFAULT_RUN_SIZE = (long)(50 * Math.pow(2, 20)); 	// 50MB
	public static final int DEFAULT_PAGE_SIZE = 102400; 	// 100KB
	
	public static final String VERSION = "0.1.3 - run!";
	
	protected boolean verbose = false;
	
	protected HashMap<Long, Reader> runsMap;	// # run -> file reader
	protected InputStream in = System.in;
	protected PrintStream out = System.out;
	protected String outfile;
	protected String infile;
	protected int[] columns = new int[0];
	protected char sep = '\t';
	protected long runSize = DEFAULT_RUN_SIZE;
//		protected long runSize = getOptimalChunkSize();
	protected int pageSize = DEFAULT_PAGE_SIZE;
	protected long elapsedSecs = 0;
	
	protected long numberOfDumpedRows = 0;
	protected long numberOfInputRows = 0;
	
	protected boolean reverse = false;
	protected boolean numeric = false;
	protected boolean uniq = false;
	protected String currKey = null;
	protected boolean dist = false;
	protected String prevKey = null;
	protected long rowsCount = 0;
	
	protected boolean EOF = false;   // End of file while reading the current run
	protected MutableString buff = new MutableString(1024);  	// 1KB buffer size
	
	protected boolean extract = false;
	
	private TreeMap<String, Tuple> map;
	
	
	/** 
	 * Create a new ExternalSort.
	 * 
	 */
	public ExternalSort() {}
	
	/**
	 * A reverse comparator.
	 * 
	 * @author data
	 *
	 */
	public class ReverseComparator implements Comparator<String> {
		public final int compare(String o1, String o2) {
			return -o1.compareTo(o2);
		}
	}
	
	/**
	 * Used to store all the Strings associated to a sorting key with their run's ids.
	 * 
	 * @author Claudio Corsi
	 *
	 */
	protected class Tuple {

		public List<String> lines = new ArrayList<String>();
		public long[] run;
		
		public Tuple() {}
		
		public final void appendRun(long runNumber) {
			
			if (run == null) {
				run = new long[] { runNumber };
				return;
			}
			
			int size = run.length + 1;
			long[] tmp = new long[size];
			System.arraycopy(run, 0, tmp, 0, run.length);
			tmp[size - 1] = runNumber;
			run = tmp;
		}
		
		public void append(String x) {
			lines.add(x);
		}
	}
	
	/**
	 * Used to compare two strings by their sorting columns (aka sorted keys).
	 * 
	 * @author Claudio Corsi
	 *
	 */
	protected class SortingKey implements Comparable<SortingKey> {
		
		public String key, row;
		
		private boolean reverse = false;
		
		public SortingKey(String row, String key) {
			this.row = row;
			this.key = key;
		}
		
		public SortingKey(String row, String cols, boolean reverse) {
			this.row = row;
			this.key = cols;
			this.reverse = reverse;
		}
		
		public void setReverse(boolean reverse) {
			this.reverse = reverse;
		}
		
		public int length() { 
			
			return (key != row) ? 80+ key.length() *2 + row.length()*2 : 40+row.length()*2;
			
		}
		
		public final int compareTo(SortingKey k) {
			return (reverse) ? -key.compareTo(k.key) : key.compareTo(k.key);
		}
	}
	
	/**
	 * Set true to have log messages on stdout during the sorting process. 
	 * 
	 * @param verbose
	 */
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
		
		System.out.println(" >> Extsort version: " + VERSION);
	}

	/**
	 * Set true to sort in reverse order (ascendent instead of descendent).
	 * 
	 * @param reverse
	 */
	public void setReverse(boolean reverse) {
		if (reverse && verbose) System.out.println(" >> Reverse order enabled.");
		this.reverse = reverse;
	}
	
	/**
	 * Compare the sorting values (rows or columns) as numerical values. In other
	 * words this flag will cause the right padding of the sorting values. 
	 * 
	 * @param numeric
	 */
	public void setNumeric(boolean numeric) {
		this.numeric = numeric;
		if (verbose) System.out.println(" >> Numeric sort enabled.");
	}
	
	/**
	 * Instead to sort, dump the frequencies of the
	 * sorting keys in their sorted order (not in the frequency values order). 
	 * 
	 * @param dist
	 */
	public void setKeysDistribution(boolean dist) {
		this.dist = dist;
		if (verbose && dist) System.out.println(" >> Keys distribution enabled, dumping frequencies.");
	}
	
	/**
	 * Remove duplicates from the result. In case of equals sorting values (rows or columns) only
	 * one of these is kept. 
	 * 
	 * @param uniq
	 */
	public void setUniq(boolean uniq) {
		this.uniq = uniq;
		if (uniq && verbose) System.out.println(" >> Uniq enabled.");
	}
	
	/**
	 * Set the size of the chunk (run) of text to sort in memory at the first stage of 
	 * the algorithm. This is the maximum size of memory available to sort. If
	 * not set the default value is 50MB.
	 * 
	 * @param runSize the size of memory available expressed in bytes
	 */
	public void setRunSize(long runSize) {
		this.runSize = runSize;
	}

	/**
	 * Set the page size to use in the second stage of the algorithm (pagination of the sorted runs).
	 * This value should be chosen depending on the disk page size. The default value is 
	 * 100KB.
	 * 
	 * @param pageSize the page size expressed in bytes.
	 */
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}
	
	/**
	 * Set the columns to sort. If not specified then the input file will be sorted considering the entire content
	 * of the rows (divided by new lines). If specified each row will be divided into columns and sorted
	 * accordingly to the value of the specified rows. The order of the columns matter. For example
	 * if the columns list is [1, 0, 5] then for first the column 1 of each row is compared. In case
	 * of the same value the column 0 is compared and, at the end, the column 5. 
	 * Two rows are considered equals if all the specified columns contain the same value
	 * 
	 * @see #setUniq(boolean)
	 * @see #setSeparator(char) 
	 * 
	 * @param columns the list of columns to sort
	 */
	public void setColumns(int[] columns) {
		this.columns = columns;
		
		if (verbose) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < columns.length; i++) sb.append("" + columns[i] + ", ");
			System.out.println(" >> Sorting by column(s): " + sb.toString().subSequence(0, sb.length() - 2));
		}
	}

	/**
	 * Set the character to use to split the rows in columns. Default value is tab ('\t').
	 *   
	 * @param sep
	 */
	public void setSeparator(char sep) {
		this.sep = sep;
		
		if (verbose) System.out.println(" >> Field separator: '" + sep + "'");
	}
	
	/**
	 * If true, dump out only the sorting column(s) omitting the other ones.
	 * The dumped column(s) will be sorted respecting the sorting parameters.
	 * If no columns are selected (sort by the entire rows) this option
	 * doesn't have effect. 
	 * 
	 * @param extract true to dump out only the sorting column(s)
	 */
	public void setExtract(boolean extract) {
		this.extract = extract;
		
		if (verbose) System.out.println(" >> Extract the sorting column(s).");
	}

	private void resetProgressInfos() {
		// reset some class value
	    elapsedSecs = 0;
	    numberOfDumpedRows = 0;
	    numberOfInputRows = 0;
	}
	
	/**
	 * Set the output file. By default the result is written in stdout.
	 * 
	 * @param outfile
	 * @throws FileNotFoundException
	 */
	public void setOutFile(String outfile) throws FileNotFoundException {
		this.outfile = outfile;
		
		if (verbose) System.out.println(" >> Output file: " + outfile);
	}
	
	/**
	 * Set the input file to sort. By default is the stdin.  
	 * 
	 * @param infile
	 * @throws FileNotFoundException
	 */
	public void setInFile(String infile) throws FileNotFoundException {
		this.infile = infile;
		this.in = new FileInputStream(infile);
	}
	
	protected void updateProgressInfos(long start) {
		long rowsPerSec = 0;
		elapsedSecs = (System.currentTimeMillis() - start) / 1000;
		if (elapsedSecs > 0) rowsPerSec = numberOfDumpedRows / elapsedSecs;
		System.out.println(" >> Input rows: " + numberOfInputRows + ", dumped rows: " + numberOfDumpedRows + " @ " + rowsPerSec + " rows/sec.");
	}
	
	/**
	 * Start the sorting process. This method can take much time to complete.
	 * 
	 * @throws IOException
	 */
	public void run() throws IOException {
		
		System.runFinalization(); System.gc();
		runSize = getOptimalRunSize();
		
		if (verbose) System.out.println(" >> Input file: " + infile == null ? "<stdin>" : infile);
		if (verbose) System.out.println(" >> Page size: " + ExternalSortUtils.formatSize(pageSize));
		if (verbose) System.out.println(" >> Run size : " + ExternalSortUtils.formatSize(runSize));
		if (verbose) System.out.println(" >> Creating the sorted runs...");
		
		FastBufferedReader fbr = new FastBufferedReader(new InputStreamReader(in));
		
		long start = System.currentTimeMillis();
		
		runsMap = new HashMap<Long, Reader>();

		List<SortingKey> chunk = new ArrayList<SortingKey>();
		
		long chunkCounter = 0;
		long tmpFileSize = 0;
		rowsCount = 0;
		
//			int maxSize = 0;
		while(!EOF) {
			
			long currChunkSize = 0;
			
			while(currChunkSize <= runSize) {
//					long keysSize = 0; 
				
				if (fbr.readLine(buff) == null) {
					EOF = true;
					break;
				}
				
				String line = buff.toString();
				String key = ExternalSortUtils.getKey(line, columns, sep, numeric);
				SortingKey s = new SortingKey(line, key, reverse);
				chunk.add(s);
				
//				if (rowsCount % 100000 == 0) System.out.printf("rows=%,d, size=%,d, memory=%,d\n", rowsCount, currChunkSize, Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
				rowsCount++;
				//currChunkSize += buff.length();  // count the number of CHARS
				currChunkSize += s.length(); //i don't now why, but now it's ok with 4
				
			} 
			
			chunkCounter++;
			
			// a chunk has been loaded, let's sort it...
			File tmpFile = createSortedRun(chunk);
			
			runsMap.put(chunkCounter, new FastBufferedReader(new FileReader(tmpFile)));
			
			tmpFileSize += tmpFile.length();
			
			if (verbose) System.out.println(" >> Created sorted run " + chunkCounter + " (" + ExternalSortUtils.formatSize(tmpFile.length()) + " on disk for " + chunk.size() + " strings)");
			
			chunk.clear();
		}

		fbr.close();
		
		long stop = System.currentTimeMillis();
		
		if (verbose) System.out.println(" >> Generated " + chunkCounter + " sorted runs in " + ExternalSortUtils.elapsedTime(start, stop));
		if (verbose) System.out.println(" >> Sorted runs size  : " + tmpFileSize + " bytes.");
		if (verbose) System.out.println(" >> Original file size: " + new File(infile).length() + " bytes.");
		
		// Init the in-memory sorting data structure
		initDataStructure();
		
		resetProgressInfos();

		// Loading the first pages
		if (verbose) System.out.print(" >> Loading initial pages... ");
		Iterator<Long> iter = runsMap.keySet().iterator();
		while (iter.hasNext()) {
			Long runNumber = (Long)iter.next();
			loadNextPage(runNumber);
		}
		if (verbose) System.out.println("done.");
		
		start = System.currentTimeMillis();
		if (verbose) System.out.println(" >> Dumping sorted rows.");

		dumpSortedRows();
		
		stop = System.currentTimeMillis();
		if (verbose) System.out.println(" >> Dump complete in " + ExternalSortUtils.elapsedTime(start, stop));
		
		if (verbose) System.out.print(" >> Closing streams... ");
		for (Reader r : runsMap.values()) r.close();
		if (verbose) System.out.println("done.");
		
		if (outfile != null) {
			if (verbose) System.out.println(" >> Original file size: " + new File(infile).length() + " bytes.");
			if (verbose) System.out.println(" >> Sorted file size  : " + (new File(outfile).length()) + " bytes.");
		}
		System.gc();
	}
	
	protected File createSortedRun(List<SortingKey> chunk) throws IOException {
		
		//File tmp = File.createTempFile("run", ".txt");
		File tmp=Dataset.createTmpFile();
		tmp.deleteOnExit();
		
		Collections.sort(chunk);

		// FIXME: this is synchronized, can we avoid it?
		BufferedWriter bw = new BufferedWriter(new FileWriter(tmp));
		
		for (SortingKey e : chunk) {
			bw.write(e.row);
			bw.write('\n');
		}
		
		bw.close();
		
	/*	FastBufferedWriter out = new FastBufferedWriter(tmp);
		
		for (SortingKey e : chunk) {
			out.append(e.row);
			out.append('\n');
		}
		
		out.close();
		*/
		return tmp;
	}
	
	protected void initDataStructure() {
		if (reverse)
			map = new TreeMap<String, Tuple>(new ReverseComparator());
		else
			map = new TreeMap<String, Tuple>();
	}
	
	protected void loadNextPage(long runNumber) throws IOException {
		
		int currPageSize = 0;
		String key = null;
		
		FastBufferedReader reader = (FastBufferedReader)runsMap.get(runNumber);
		
		for (MutableString row = reader.readLine(buff); row != null; row = reader.readLine(buff)) {
			
			String line = buff.toString();
			
			numberOfInputRows++;
			
			currPageSize += buff.length();
			
			key = ExternalSortUtils.getKey(line, columns, sep, numeric);
			
			Tuple tuple = new Tuple();
			Tuple oldTuple = (Tuple)map.put(key, tuple);
			
			if (oldTuple != null) {
				tuple.run = oldTuple.run;
				tuple.lines = oldTuple.lines;
			}
			
			if (!uniq || (tuple.lines.size() == 0 && !key.equals(currKey))) 
				tuple.append(line);
			
			if (currPageSize >= pageSize) {
				tuple.appendRun(runNumber);
				break;
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	protected void dumpSortedRows() throws IOException {
		
		if (outfile != null) out = new PrintStream(outfile);
		
		// FIXME: synchronized Writer! Use a not synch one to speed up the I/O 
		FastBufferedWriter bw = new FastBufferedWriter(out, 16384); // 16KB buffer size
		
		long start = System.currentTimeMillis();

		// count how many times a sorting key has been seen
		int freq = 0;
		
		while (!map.isEmpty()) {
			long[] toLoad = null;
			Set entrySet = map.entrySet();
			Iterator iter = entrySet.iterator();
			while (iter.hasNext()) {
				
				Entry<String, Tuple> entry = (Entry)iter.next();
				iter.remove();

				currKey = entry.getKey();
				prevKey = (prevKey == null) ? currKey : prevKey;
				
				Tuple tuple = (Tuple)entry.getValue();
				List<String> lines = tuple.lines;
				
				for(int i = 0; i < lines.size(); i++) {
					
					if (dist) {  // dump the distribution
						if (!currKey.equals(prevKey)) {
							String str = (numeric) ? ExternalSortUtils.trimLeftZeros(prevKey) : prevKey;
							bw.write(str);
							bw.write("\t");
							bw.writeInt(freq);
							bw.write("\n");
							freq = 0;
							prevKey = currKey;
							numberOfDumpedRows++;
						}
						
						freq++;
					}
					else {
						if (extract)
							bw.write((numeric) ? ExternalSortUtils.trimLeftZeros(currKey) : currKey);
						else
							bw.write(lines.get(i));
						
						bw.write("\n");
						numberOfDumpedRows++;
					}
					
					if (verbose && (numberOfDumpedRows % 1000000) == 0 && numberOfDumpedRows != 0) 
						updateProgressInfos(start);
				}
				
				if (tuple.run != null) {
					toLoad = tuple.run;
					break;
				}
			}
			
			if (toLoad != null) {
				for (int i = 0; i < toLoad.length; i++) {
					loadNextPage(toLoad[i]);
				}
			}
		}
		
		if (dist) {
			String str = (numeric) ? ExternalSortUtils.trimLeftZeros(prevKey) : prevKey;
			bw.write(str);
			bw.write("\t");
			bw.writeInt(freq);
			bw.write("\n");
			numberOfDumpedRows++;
		}
		
		bw.flush();
		
		// close if it is not the stdout
		if (out != System.out) out.close();
		
		if (verbose) {
			updateProgressInfos(start);
			System.out.println(" >> Dumped rows: " + numberOfDumpedRows);
		}
	}
	
	public static long getOptimalRunSize(){
		return 
			(Runtime.getRuntime().maxMemory() - 
			 Runtime.getRuntime().totalMemory() + 
			 Runtime.getRuntime().freeMemory()) / 2; 
	}
	

	
}

