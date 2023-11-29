package independentProject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
				System.out.println(instance.getHeterozygosity() + " " + instance.getVariantMissingness());
			}
		}
		
		System.out.println(allVariantsInFile);
		
		// Add all instances of Variant to the CombinedVariant class for avg calculations
		// use overall.*methodName*() for avg calcs
		CombinedVariants overall = new CombinedVariants(allVariantsInFile);
		System.out.println(overall.getHeterozygosity() + " " + overall.getAltAlleleFreq() + " " + overall.getDepth());
		
	}
	
	
	public static void main(String[] args) throws IOException {
		
		// file input string will be taken from user input later and then checked to see if it exists before sending to parse
		BufferedReader reader = new BufferedReader(new FileReader("/Users/whitneybrannen/git/BINF-6390/src/independentProject/sampleVCF.vcf"));
		
		// parses each line into Variant class 
		parseVCFFile(reader);

	}
	
}
