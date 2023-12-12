package independentProject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class Test { // extends TestCase

	// single threaded testing vcf interface this method is used in the gui
	public static void parseVCFFile(BufferedReader reader) throws IOException {
				
		List<String> titles = new ArrayList<>();
		
		List<Variant> allVariantsInFile = new ArrayList<>(); 
		
		// I plan on later changing this to be multithreaded
		while ( reader.ready() ) {
						
			List<Object> values = new ArrayList<>();
			Map<String,Object> variantLineInfo= new HashMap<>();
			
			String line = reader.readLine();
			
			// handle title line
			if (line.startsWith("#CHROM")) {
				String[] titleLine = line.split("\t");
				titles = Arrays.asList(titleLine);
			}
			// all other lines that aren't headers
			if ( !line.startsWith("##") ) {
				Object[] variantLine = line.split("\t");
				values = Arrays.asList(variantLine);
			}
			
			// create hashmap for each category for variants
			for (int x = 0; x < values.size(); x++ ) {
				String key = titles.get(x);
				Object val = values.get(x);
				variantLineInfo.put(key, val);	
			}
			
			// now create Variant instance for each line
			if (variantLineInfo.get("#CHROM") != null && !variantLineInfo.get("#CHROM").equals("#CHROM")) {
				Variant instance = new Variant(variantLineInfo);
				allVariantsInFile.add(instance);
			}
		}
		
//		System.out.println(allVariantsInFile);
		
		// Add all instances of Variant to the CombinedVariant class for avg calculations
		// use overall.*methodName*() for avg calcs
		CombinedVariants overall = new CombinedVariants(allVariantsInFile);
//		System.out.println(overall.getHeterozygosity() + " " + overall.getAltAlleleFreq() + " " + overall.getDepth());
		
	}
	   
    
    
    public static class FileParserWorker implements Runnable {
    	
    	private final BlockingQueue<String> queue;
    	private final List<String >titles;
		private final CountDownLatch latch;
		private List<Variant >allVariantsInFile;
    	
    	public FileParserWorker(BlockingQueue<String> queue, List<String> titles, CountDownLatch latch,List<Variant> allVariantsInFile) {
    		this.queue = queue;
    		this.titles = titles;
    		this.latch = latch;
    		this.allVariantsInFile = allVariantsInFile;
    	}
    	
		@Override
		public void run() {
			
			try {
				
				latch.await();
				
				while(true) {
				
					String line = queue.take();
					
					   if (line.equals("EOF")) {
					        break;
					   }
					   
					List<Object> values = new ArrayList<>();
					Map<String,Object> variantLineInfo= new HashMap<>();
	
					
					// all other lines that aren't headers
					if ( !line.startsWith("##") ) {
						Object[] variantLine = line.split("\t");
						values = Arrays.asList(variantLine);
					}
					
					// create hashmap for each category for variants
					for (int x = 0; x < values.size(); x++ ) {
						String key = titles.get(x);
						Object val = values.get(x);
						variantLineInfo.put(key, val);	
					}
					
					// now create Variant instance for each line
					if (variantLineInfo.get("#CHROM") != null && !variantLineInfo.get("#CHROM").equals("#CHROM")) {
						Variant instance = new Variant(variantLineInfo);
						allVariantsInFile.add(instance);

					}
				}	   
				
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
    }
	
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		long startSingle = System.currentTimeMillis();
		
		String filepath = "/Users/whitneybrannen/CooperLab/WGS_subset.vcf";
		
		/*
		 * launch single threaded and report time
		 */
		BufferedReader reader1 = new BufferedReader(new FileReader(filepath));
		
		// parses each line into Variant class 
		parseVCFFile(reader1);
		long endSingle = System.currentTimeMillis();
		reader1.close();
		float single = (endSingle-startSingle)/1000f;
		System.out.println("single threaded time elapsed: " + single + " seconds");
		
		
		/*
		 * launch multithreaded and report time
		 */
		long startMulti = System.currentTimeMillis();
		int maxNumThreads = Runtime.getRuntime().availableProcessors()+1;
		
		
		BlockingQueue<String> queue = new LinkedBlockingQueue<>();
		List<String> titles = new CopyOnWriteArrayList<>(); 
		CountDownLatch latch = new CountDownLatch(1);
		List<Variant> allVariantsInFile = new CopyOnWriteArrayList<>();

		BufferedReader reader = new BufferedReader(new FileReader(filepath));
		
		while ( reader.ready() ) {
			String line = reader.readLine();
			if (line.startsWith("#CHROM")) {
				String[] titleLine = line.split("\t");
		        titles = new ArrayList<>(Arrays.asList(titleLine));
				latch.countDown(); // block until titles is established from the file or parsing will fail
			}
		    queue.put(line);
//			System.out.println("Putting " + line);
		}
		
		List<Thread> workerThreads = new ArrayList<>();
		
		for (int i =0; i < maxNumThreads; i++) {
		    Thread workerThread = new Thread(new FileParserWorker(queue, titles, latch, allVariantsInFile));
		    workerThreads.add(workerThread);
		    workerThread.start();
		    queue.put("EOF");
		}
		
		for (Thread workerThread : workerThreads) {
		    try {
		        workerThread.join();
		    } catch (InterruptedException e) {
		        e.printStackTrace();
		    }
		}
		
		System.out.println(allVariantsInFile.size());
//		System.out.println(allVariantsInFile);
		
		long endMulti = System.currentTimeMillis();
		reader.close();
		float multi = (endMulti-startMulti)/1000f;
		System.out.println("multithreaded threaded time elapsed: " + multi + " seconds");
		
		/*
		 * report speedup (around 2 fold)
		 */
		
		System.out.println(single/multi + " fold speedup");
	}
	
}
