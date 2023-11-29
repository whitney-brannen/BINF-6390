package independentProject;

import java.util.ArrayList;
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
	private final double qualityScore;
	private int depth;
	
	private double variantMissingness;
	private double homozygousRefGenotypeFreq;
	private double homozygousAltGenotypeFreq;
	private double heteroGenotypeFreq;
	private double refAlleleFreq;
	private double altAlleleFreq;
	
	
	/*
	 * here need to all private variables for each of the calculations so they can be accessed quicker later
	 */

	
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
		this.qualityScore = Double.valueOf(variantLineInfo.get("QUAL").toString());
		try {
			this.depth  = Integer.valueOf(Arrays.asList(variantLineInfo.get("INFO").toString().split("DP=")).get(1));
			} 
		catch (ArrayIndexOutOfBoundsException e) {
			this.depth = 0;
		}
		
		// add samples to a hashmap that can adjust size depending on input vcf
		for (int x=9; x<variantLineInfo.size(); x++) { // can change for item in variantLineInfo if not in (headers) to be able to use a regular hashmap for performance later
			String sampleName = keyList.get(x).toString();
			String genotype = variantList.get(x).toString().split(":")[0];
			samples.put(sampleName, genotype);
		}
		// perform calculations
		calcVariantMissingness();
		calcHomozogousRefGenotypeFreq();
		calcHomozygousAltGenotypeFreq();
		calcHeterozygosity();
		calcRefAlleleFreq();
		calcAltAlleleFreq();
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
	
	private double calcGenotypeFreq(String genotype) {
		double genotypeCount = 0;
		for ( String allele : samples.values() ) {
			if ( allele.equals(genotype) ) {
				genotypeCount++;
			}
		}
		double genotypeFreq = genotypeCount / samples.values().size();
		return genotypeFreq;
	}
	
	@Override
	public void calcVariantMissingness() {
		String missingGenotype = "./.";
		variantMissingness = calcGenotypeFreq(missingGenotype);	
	}

	@Override
	public void calcHomozogousRefGenotypeFreq() {
		String homoRefGenotype = "0/0";
		homozygousRefGenotypeFreq = calcGenotypeFreq(homoRefGenotype);	
	}

	@Override
	public void calcHomozygousAltGenotypeFreq() {
		String homoAltGenotype = "1/1";
		homozygousAltGenotypeFreq = calcGenotypeFreq(homoAltGenotype);	
	}

	@Override
	public void calcHeterozygosity() {
		double heteroCount = 0;
		for ( String allele : samples.values() ) {
			if ( allele.equals("0/1") || allele.equals("1/0") ) {
				heteroCount++;
			}
		}
		heteroGenotypeFreq = heteroCount / samples.values().size();
	}
	
	private double calcAlleleFreq(int targetAllele) {
		
		List<Integer> alleleList= new ArrayList<>();
		
		for ( String genotype : samples.values() ) {
			String[] splitGenotype = genotype.split("/");
			for ( String allele : splitGenotype) {
				if ( allele.equals(".") ) {
					alleleList.add(9);
				}
				else { 
					alleleList.add(Integer.valueOf(allele));
				}
			}
		}
		double alleleCount = 0;
		
		for ( int allele : alleleList ) {
			if ( allele == targetAllele ) {
				alleleCount++;
			}
		}
		double alleleFreq = alleleCount / alleleList.size();
		return alleleFreq;
	}
	
	@Override
	public void calcRefAlleleFreq() {
		int targetAllele = 0;
		refAlleleFreq = calcAlleleFreq(targetAllele);
	}

	@Override
	public void calcAltAlleleFreq() {
		int targetAllele = 1;
		altAlleleFreq = calcAlleleFreq(targetAllele);
	}
	
	public double getVariantMissingness() {
		return variantMissingness;
	}

	public double getHomozygousRefGenotypeFreq() {
		return homozygousRefGenotypeFreq;
	}

	public double getHomozygousAltGenotypeFreq() {
		return homozygousAltGenotypeFreq;
	}

	public double getHeterozygosity() {
		return heteroGenotypeFreq;
	}

	public double getRefAlleleFreq() {
		return refAlleleFreq;
	}

	public double getAltAlleleFreq() {
		return altAlleleFreq;
	}

	public static void main(String[] args) {
		
	}

}


