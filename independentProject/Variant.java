package independentProject;

import java.util.Arrays;
import java.util.HashMap;
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
	// for viewing as a string rn
	public String toString() {
		StringBuffer returnStr = new StringBuffer("(");
		for (Object x : getValues() ) {
			returnStr.append(x.toString()+" ");
		}
		returnStr.append(")");
		return returnStr.toString();
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
	
	
	public String getChromosome() {
		return chromosome;
	}

	public int getPosition() {
		return position;
	}
	
	@Override
	public double getDepth() {
		return depth;	
	}
	
	@Override
	public double getQualityScore() {
		return qualityScore;
	}
	
	public String getRefAllele() {
		return refAllele;
	}

	public String getAltAllele() {
		return altAllele;
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
	
	public static void main(String[] args) {
		
	}

}


