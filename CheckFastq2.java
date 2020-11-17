import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

/**
 * Produces a summary of any problems observed in a FASTQ file.
 * This program particularly looks for read positions that have a much greater proportion of one particular base than would be expected.
 * This software assumes that fastq files are paired.
 *
 * @author Matthew Wakeling
 */
public class CheckFastq2
{
	public static void main(String[] args) throws IOException {
		// All arguments are fastq files (gzipped) to check.
		ArrayList<Worker> workers = new ArrayList<Worker>();
		for (int i = 0; i < args.length; i += 2) {
			Worker worker = new Worker(args[i], args[i + 1]);
			workers.add(worker);
			Thread t = new Thread(worker);
			t.start();
		}
		int readCount = 0;
		for (Worker worker : workers) {
			String result = worker.getResult();
			if (result != null) {
				BufferedReader sr = new BufferedReader(new StringReader(result));
				String line = sr.readLine();
				while (line != null) {
					try {
						readCount += Integer.parseInt(line);
					} catch (NumberFormatException e) {
						System.out.println(line);
					}
					line = sr.readLine();
				}
			}
		}
		System.out.println(readCount);
	}

	public static String processFastq(String fileName1, String fileName2) throws IOException {
		long[] aCount1 = new long[600];
		long[] cCount1 = new long[600];
		long[] gCount1 = new long[600];
		long[] tCount1 = new long[600];
		long[] otherCount1 = new long[600];
		long[] aCount2 = new long[600];
		long[] cCount2 = new long[600];
		long[] gCount2 = new long[600];
		long[] tCount2 = new long[600];
		long[] otherCount2 = new long[600];
		int readCount1 = 0;
		int readCount2 = 0;
		Violation formatViolations1 = new Violation();
		Violation formatViolations2 = new Violation();
		BufferedReader in1 = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(fileName1))));
		BufferedReader in2 = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(fileName2))));
		String header1 = readLine(in1, formatViolations1);
		String bases1 = readLine(in1, formatViolations1);
		String sep1 = readLine(in1, formatViolations1);
		String quality1 = readLine(in1, formatViolations1);
		String header2 = readLine(in2, formatViolations2);
		String bases2 = readLine(in2, formatViolations2);
		String sep2 = readLine(in2, formatViolations2);
		String quality2 = readLine(in2, formatViolations2);
		boolean shortQuality1 = false;
		boolean shortQuality2 = false;
		boolean doneShortQuality1 = false;
		boolean doneShortQuality2 = false;
		while ((header1 != null) || (header2 != null)) {
			if (header1 != null) {
				if (shortQuality1) {
					if (!doneShortQuality1) {
						formatViolations1.add(1, "Quality string is not the same length as the base string on line " + (readCount1 * 4 + 3));
					}
					doneShortQuality1 = true;
					shortQuality1 = false;
				}
				readCount1++;
				if ((bases1 == null) || (sep1 == null) || (quality1 == null)) {
					formatViolations1.add(2, "File appears to be truncated half-way through a read record");
				}
				if (bases1 != null) {
					for (int i = 0; i < bases1.length(); i++) {
						char c = bases1.charAt(i);
						switch (c) {
							case 'A':
								aCount1[i]++;
								break;
							case 'C':
								cCount1[i]++;
								break;
							case 'G':
								gCount1[i]++;
								break;
							case 'T':
								tCount1[i]++;
								break;
							default:
								otherCount1[i]++;
								break;
						}
					}
				}
				if ((bases1 != null) && (quality1 != null) && (bases1.length() != quality1.length())) {
					shortQuality1 = true;
				}
				if ((sep1 != null) && (!"+".equals(sep1))) {
					formatViolations1.add(1, "Separator line is not \"+\"");
				}
				if (header2 != null) {
					if (!header1.substring(0, header1.length() - 2).equals(header2.substring(0, header2.length() - 2))) {
						formatViolations1.add(1, "Header for R1 (" + header1 + ") does not equal header for R2 (" + header2 + ")");
					}
				}
			}
			if (header2 != null) {
				if (shortQuality2) {
					if (!doneShortQuality2) {
						formatViolations2.add(1, "Quality string is not the same length as the base string " + (readCount2 * 4 + 3));
					}
					doneShortQuality2 = true;
					shortQuality2 = false;
				}
				readCount2++;
				if ((bases2 == null) || (sep2 == null) || (quality2 == null)) {
					formatViolations2.add(2, "File appears to be truncated half-way through a read record");
				}
				if (bases2 != null) {
					for (int i = 0; i < bases2.length(); i++) {
						char c = bases2.charAt(i);
						switch (c) {
							case 'A':
								aCount2[i]++;
								break;
							case 'C':
								cCount2[i]++;
								break;
							case 'G':
								gCount2[i]++;
								break;
							case 'T':
								tCount2[i]++;
								break;
							default:
								otherCount2[i]++;
								break;
						}
					}
				}
				if ((bases2 != null) && (quality2 != null) && (bases2.length() != quality2.length())) {
					shortQuality2 = true;
				}
				if ((sep2 != null) && (!"+".equals(sep2))) {
					formatViolations2.add(1, "Separator line is not \"+\"");
				}
			}
			header1 = readLine(in1, formatViolations1);
			bases1 = readLine(in1, formatViolations1);
			sep1 = readLine(in1, formatViolations1);
			quality1 = readLine(in1, formatViolations1);
			header2 = readLine(in2, formatViolations2);
			bases2 = readLine(in2, formatViolations2);
			sep2 = readLine(in2, formatViolations2);
			quality2 = readLine(in2, formatViolations2);
		}
		in1.close();
		in2.close();
		if (shortQuality1) {
			formatViolations1.add(2, "File appears to be truncated half-way through a read record");
		}
		if (shortQuality2) {
			formatViolations2.add(2, "File appears to be truncated half-way through a read record");
		}
		StringBuilder retval = new StringBuilder();
		retval.append((readCount1 + readCount2) + "\n");
		if (readCount1 != readCount2) {
			if (readCount1 < readCount2) {
				formatViolations1.add(1, "File has fewer reads than its pair (" + readCount1 + " versus " + readCount2 + ")");
			} else {
				formatViolations2.add(1, "File has fewer reads than its pair (" + readCount2 + " versus " + readCount1 + ")");
			}
		}
		for (int i = 0; i < 600; i++) {
			long total = aCount1[i] + cCount1[i] + gCount1[i] + tCount1[i] + otherCount1[i];
			double aFrac = (aCount1[i] * 1.0) / total;
			double cFrac = (cCount1[i] * 1.0) / total;
			double gFrac = (gCount1[i] * 1.0) / total;
			double tFrac = (tCount1[i] * 1.0) / total;
			double otherFrac = (otherCount1[i] * 1.0) / total;
			if (aFrac > 0.45) {
				formatViolations1.add("Read " + (i + 1) + " A fraction is " + aFrac);
			}
			if (cFrac > 0.45) {
				formatViolations1.add("Read " + (i + 1) + " C fraction is " + cFrac);
			}
			if (gFrac > 0.45) {
				formatViolations1.add("Read " + (i + 1) + " G fraction is " + gFrac);
			}
			if (tFrac > 0.45) {
				formatViolations1.add("Read " + (i + 1) + " T fraction is " + tFrac);
			}
			if (otherFrac > 0.2) {
				formatViolations1.add("Read " + (i + 1) + " unknown fraction is " + otherFrac);
			}
		}
		for (int i = 0; i < 600; i++) {
			long total = aCount2[i] + cCount2[i] + gCount2[i] + tCount2[i] + otherCount2[i];
			double aFrac = (aCount2[i] * 1.0) / total;
			double cFrac = (cCount2[i] * 1.0) / total;
			double gFrac = (gCount2[i] * 1.0) / total;
			double tFrac = (tCount2[i] * 1.0) / total;
			double otherFrac = (otherCount1[i] * 1.0) / total;
			if (aFrac > 0.45) {
				formatViolations2.add("Read " + (i + 1) + " A fraction is " + aFrac);
			}
			if (cFrac > 0.45) {
				formatViolations2.add("Read " + (i + 1) + " C fraction is " + cFrac);
			}
			if (gFrac > 0.45) {
				formatViolations2.add("Read " + (i + 1) + " G fraction is " + gFrac);
			}
			if (tFrac > 0.45) {
				formatViolations2.add("Read " + (i + 1) + " T fraction is " + tFrac);
			}
			if (otherFrac > 0.2) {
				formatViolations2.add("Read " + (i + 1) + " unknown fraction is " + otherFrac);
			}
		}
		for (String violation : formatViolations1.getMessages()) {
			retval.append(fileName1 + "\t" + violation + "\n");
		}
		for (String violation : formatViolations2.getMessages()) {
			retval.append(fileName2 + "\t" + violation + "\n");
		}
		return retval.toString();
	}

	public static String readLine(BufferedReader in, Violation formatViolations) {
		try {
			return in.readLine();
		} catch (IOException e) {
			boolean isGzip = false;
			String message = e.getMessage();
			Throwable t = e;
			while (t != null) {
				if (t instanceof ZipException) {
					isGzip = true;
				}
				t = t.getCause();
			}
			if (isGzip) {
				formatViolations.add(3, "Failure decoding gzip stream: \"" + message + "\"");
			} else {
				formatViolations.add(3, "Read failure reading fastq file: \"" + message + "\"");
			}
			return null;
		}
	}

	public static class Worker implements Runnable
	{
		public String result;
		public String fileName1, fileName2;

		public Worker(String fileName1, String fileName2) {
			this.fileName1 = fileName1;
			this.fileName2 = fileName2;
		}

		public void run() {
			String res = null;
			try {
				res = processFastq(fileName1, fileName2);
			} catch (IOException e) {
				res = "0\n" + fileName1 + ", " + fileName2 + "\tFailed to read fastq file\n";
			} catch (Throwable e) {
				e.printStackTrace(System.err);
				res = "0\n" + fileName1 + ", " + fileName2 + "\tError processing file\n";
			}
			synchronized(this) {
				result = res;
				notifyAll();
			}
		}

		public synchronized String getResult() {
			while (result == null) {
				try {
					wait();
				} catch (InterruptedException e) {
				}
			}
			return result;
		}
	}

	public static class Violation
	{
		private int level = 0;
		private TreeSet<String> messages = new TreeSet<String>();

		public Violation() {
		}

		public void add(String message) {
			add(0, message);
		}

		public void add(int level, String message) {
			if (level > this.level) {
				messages.clear();
				this.level = level;
			}
			if (level == this.level) {
				messages.add(message);
			}
		}

		public TreeSet<String> getMessages() {
			return messages;
		}
	}
}
