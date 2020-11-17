import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

/**
 * Produces a summary of any problems observed in a FASTQ file.
 * This program particularly looks for read positions that have a much greater proportion of one particular base than would be expected.
 *
 * @author Matthew Wakeling
 */
public class CheckFastq
{
	public static void main(String[] args) throws IOException {
		// All arguments are fastq files (gzipped) to check.
		ArrayList<Worker> workers = new ArrayList<Worker>();
		for (String file : args) {
			Worker worker = new Worker(file);
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

	public static String processFastq(String fileName) throws IOException {
		long[] aCount = new long[600];
		long[] cCount = new long[600];
		long[] gCount = new long[600];
		long[] tCount = new long[600];
		long[] otherCount = new long[600];
		int readCount = 0;
		BufferedReader in = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(fileName))));
		String header = in.readLine();
		String bases = in.readLine();
		String sep = in.readLine();
		String quality = in.readLine();
		while (bases != null) {
			readCount++;
			for (int i = 0; i < bases.length(); i++) {
				char c = bases.charAt(i);
				switch (c) {
					case 'A':
						aCount[i]++;
						break;
					case 'C':
						cCount[i]++;
						break;
					case 'G':
						gCount[i]++;
						break;
					case 'T':
						tCount[i]++;
						break;
					default:
						otherCount[i]++;
						break;
				}
			}
			header = in.readLine();
			bases = in.readLine();
			sep = in.readLine();
			quality = in.readLine();
		}
		in.close();
		StringBuilder retval = new StringBuilder();
		retval.append(readCount + "\n");
		for (int i = 0; i < 600; i++) {
			long total = aCount[i] + cCount[i] + gCount[i] + tCount[i] + otherCount[i];
			double aFrac = (aCount[i] * 1.0) / total;
			double cFrac = (cCount[i] * 1.0) / total;
			double gFrac = (gCount[i] * 1.0) / total;
			double tFrac = (tCount[i] * 1.0) / total;
			double otherFrac = (otherCount[i] * 1.0) / total;
			if (aFrac > 0.45) {
				retval.append(fileName + "\tRead " + (i + 1) + " A fraction is " + aFrac + "\n");
			}
			if (cFrac > 0.45) {
				retval.append(fileName + "\tRead " + (i + 1) + " C fraction is " + cFrac + "\n");
			}
			if (gFrac > 0.45) {
				retval.append(fileName + "\tRead " + (i + 1) + " G fraction is " + gFrac + "\n");
			}
			if (tFrac > 0.45) {
				retval.append(fileName + "\tRead " + (i + 1) + " T fraction is " + tFrac + "\n");
			}
			if (otherFrac > 0.2) {
				retval.append(fileName + "\tRead " + (i + 1) + " unknown fraction is " + otherFrac + "\n");
			}
		}
		return retval.toString();
	}

	public static class Worker implements Runnable
	{
		public String result;
		public String fileName;

		public Worker(String fileName) {
			this.fileName = fileName;
		}

		public void run() {
			String res = null;
			try {
				res = processFastq(fileName);
			} catch (IOException e) {
				res = "0\n" + fileName + "\tFailed to read fastq file\n";
			} catch (Throwable e) {
				e.printStackTrace(System.err);
				res = "0\n" + fileName + "\tError processing file\n";
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
}
