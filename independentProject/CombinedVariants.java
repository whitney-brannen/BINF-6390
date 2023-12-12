package independentProject;

import java.util.ArrayList;
import java.util.List;

public class CombinedVariants implements VCF {
	
	/*
	 * This class calculates the averages of all variants for an entire vcf file
	 */

	private final List<Variant> allVariants;
	
	private double variantMissingness;
	private double homozygousRefGenotypeFreq;
	private double homozygousAltGenotypeFreq;
	private double heteroGenotypeFreq;
	private double refAlleleFreq;
	private double altAlleleFreq; 

	public CombinedVariants(List<Variant> allVariants) {
		this.allVariants = allVariants;
		calcVariantMissingness();
		calcHomozogousRefGenotypeFreq();
		calcHomozygousAltGenotypeFreq();
		calcHeterozygosity();
		calcRefAlleleFreq();
		calcAltAlleleFreq();
	}
	
	public List<Variant> getVars() {
		return allVariants;
	}
	
	public int numVariants() {
		return allVariants.size();
	}
	
	private double calculateAverage(List<Double> calculationList) {
		double sum = 0;
		double total = 0;
		for ( double num : calculationList) {
			sum = sum + num;
			total++;
		}
		return sum/total;
	}
	
	@Override
	public double getDepth() {
		List<Double> depths = new ArrayList<>();
		for ( Variant var : allVariants ) {
			depths.add(var.getDepth());
		}
		return calculateAverage(depths);
	}

	@Override
	public double getQualityScore() {
		List<Double> qualityScores = new ArrayList<>();
		for ( Variant var : allVariants ) {
			qualityScores.add(var.getQualityScore());
		}
		return calculateAverage(qualityScores);
	}

	@Override
	public void calcVariantMissingness() {
		List<Double> missingness = new ArrayList<>();
		for ( Variant var : allVariants ) {
			missingness.add(var.getVariantMissingness());
		}
		variantMissingness = calculateAverage(missingness);
	}

	@Override
	public void calcHomozogousRefGenotypeFreq() {
		List<Double> refGenotypeFreq = new ArrayList<>();
		for ( Variant var : allVariants ) {
			refGenotypeFreq.add(var.getHomozygousRefGenotypeFreq());
		}
		homozygousRefGenotypeFreq = calculateAverage(refGenotypeFreq);
	}

	@Override
	public void calcHomozygousAltGenotypeFreq() {
		List<Double> altGenotypeFreq = new ArrayList<>();
		for ( Variant var : allVariants ) {
			altGenotypeFreq.add(var.getHomozygousAltGenotypeFreq());
		}
		homozygousAltGenotypeFreq = calculateAverage(altGenotypeFreq);
	}

	@Override
	public void calcHeterozygosity() {
		List<Double> heterozygousFreq = new ArrayList<>();
		for ( Variant var : allVariants ) {
			heterozygousFreq.add(var.getHeterozygosity());
		}
		heteroGenotypeFreq = calculateAverage(heterozygousFreq);
	}
	
	@Override
	public void calcRefAlleleFreq() {
		List<Double> refAlleleFreqs = new ArrayList<>();
		for ( Variant var : allVariants ) {
			refAlleleFreqs.add(var.getRefAlleleFreq());
		}
		refAlleleFreq = calculateAverage(refAlleleFreqs);
	}

	@Override
	public void calcAltAlleleFreq() {
		List<Double> altAlleleFreqs = new ArrayList<>();
		for ( Variant var : allVariants ) {
			altAlleleFreqs.add(var.getAltAlleleFreq());
		}
		altAlleleFreq = calculateAverage(altAlleleFreqs);
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

}
