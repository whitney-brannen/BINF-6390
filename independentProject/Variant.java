package independentProject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Variant implements VCF {
	
	/*
	 * this class is used to read in all the different fields per site in a vcf file and initialize them
	 * to variables to perform calculations
	 */

	private final Map<String,Object> variantLineInfo;
	private final List<Object> variantList;
	private final List<Object> keyList;
	
	private final Map<String,String> samples;

	// split inputs to make each field have its own variable
	private final String chromosome;
	private final int position;
	private final String refAllele;
	private final String altAllele;
	private final Integer qualityScore;
	private final int depth;

	
	public Variant(Map<String,Object> variantLineInfo) {
		
		this.variantLineInfo = variantLineInfo;
		
		// set to lists for indexing using linkedhashmap, may change this later
		this.keyList = Arrays.asList(variantLineInfo.keySet().toArray());
		this.variantList = Arrays.asList(variantLineInfo.values().toArray());
		this.samples = new HashMap<>();
		
		// important fields for calculations
		this.chromosome = (variantLineInfo.get("#CHROM")).toString();
		this.position = Integer.valueOf(variantLineInfo.get("POS").toString());
		this.refAllele = variantLineInfo.get("REF").toString();
		this.altAllele = variantLineInfo.get("ALT").toString();
		this.qualityScore = Integer.valueOf(variantLineInfo.get("QUAL").toString());
		this.depth  = Integer.valueOf(Arrays.asList(variantLineInfo.get("INFO").toString().split("DP=")).get(1));
		
		// add samples to a hashmap that can adjust size depending on input vcf
		for (int x=9; x<variantLineInfo.size(); x++) { // can change for item in variantLineInfo if not in (headers) to be able to use a regular hashmap for performance later
			String sampleName = keyList.get(x).toString();
			String genotype = variantList.get(x).toString();
			samples.put(sampleName, genotype);
		}
	}
	
	public List<Object> getValues() {
		return Arrays.asList(variantLineInfo.values().toArray());
	}
	
	@Override
	public boolean equals(Object o) {
		
		Variant other = (Variant) o;
		
		if ( this == o ) return true;
		
		if (o == null || getClass() != o.getClass()) return false;
		
		if ( position != other.position ) return true;
		
		return chromosome.equals(other.chromosome) ;
	}
	
	@Override
	public int hashCode() {
	    int result = 17;
	    result = 31 * result + chromosome.hashCode();
	    result = 31 * result + position;
	    return result;
	}
	
	@Override
	// for viewing as a string rn
	public String toString() {
		StringBuffer returnStr = new StringBuffer("(");
		for (Object x : getValues() ) {
			returnStr.append(x.toString()+" ");
		}
		returnStr.append(")");
		return returnStr.toString();
	}
	
	public double calcAlleleFreq(String genotype) {
		double alleleCount = 0;
		for ( String allele : samples.values() ) {
			if ( allele.equals(genotype) ) {
				alleleCount++;
			}
		}
		double alleleFreq = alleleCount / samples.values().size();
		return alleleFreq;
	}
	
	@Override
	public double getVariantMissingness() {
		String missingGenotype = "./.";
		return calcAlleleFreq(missingGenotype);
		
	}


	@Override
	public double getHomozogousRefAlleleFreq() {
		String homoRefGenotype = "0/0";
		return calcAlleleFreq(homoRefGenotype);
		
	}

	@Override
	public double getHomozygousAltAlleleFreq() {
		String homoAltGenotype = "1/1";
		return calcAlleleFreq(homoAltGenotype);
		
	}

	@Override
	public double getHeterozygousFreq() {
		double heteroAlleleCount = 0;
		for ( String allele : samples.values() ) {
			if ( allele.equals("0/1") || allele.equals("1/0") ) {
				heteroAlleleCount++;
			}
		}
		double heteroAlleleFreq = heteroAlleleCount / samples.values().size();
		return heteroAlleleFreq;
		
	}
	
	@Override
	public int getDepth() {
		return depth;
		
	}


	
	// method for parsing file
	public static void parseVCFFile(BufferedReader reader) throws IOException {
		
		List<String> allVariantsInFile = new ArrayList<>(); // currently this is just an arraylist for easy display but i will likely turn 
															// this into a class to calculate the average of all the functions called for 
															// each site to return averageDepth, averageMissingness, etc...
		
		List<String> titles = new ArrayList<>();
		
		// I plan on later changing this to be multithreaded
		while ( reader.ready() ) {
						
			List<Object> values = new ArrayList<>();
			Map<String,Object> variantLineInfo= new LinkedHashMap<>();
			
			String line = reader.readLine();
			if (line.startsWith("#CHROM")) {
				String[] titleLine = line.split("\t");
				titles = Arrays.asList(titleLine);
			}

			if ( !line.startsWith("##") ) {
				Object[] variantLine = line.split("\t");
				values = Arrays.asList(variantLine);
			}
			
			for (int x = 0; x < values.size(); x++ ) {
				String key = titles.get(x);
				Object val = values.get(x);
				variantLineInfo.put(key, val);	
			}
			
			if (variantLineInfo.get("#CHROM") != null && !variantLineInfo.get("#CHROM").equals("#CHROM")) {
	
				Variant instance = new Variant(variantLineInfo);
				String instanceStr = instance.toString();
				allVariantsInFile.add(instanceStr);
//				System.out.println(instance.getHeterozygousFreq());
			}
			
		}
		
		System.out.println(allVariantsInFile);
		
		
	}
	
	
	public static void main(String[] args) throws IOException {
		
		// file input string will be taken from user input later and then checked to see if it exists before sending to parse
		
		BufferedReader reader = new BufferedReader(new FileReader("/Users/whitneybrannen/git/BINF-6390/src/independentProject/sampleVCF.vcf"));
		
		// parses each line into Variant class 
		parseVCFFile(reader);
		
		
	}

}


