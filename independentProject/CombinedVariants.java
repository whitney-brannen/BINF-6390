package independentProject;

import java.util.ArrayList;
import java.util.List;

public class CombinedVariants implements VCF {
	
	/*
	 * This class calculates the averages of all variants for an entire vcf file
	 */

	private final List<Variant> allVariants;

	public CombinedVariants(List<Variant> allVariants) {
		this.allVariants = allVariants;
	}
	
	public double calculateAverage(List<Double> calculationList) {
		double sum = 0;
		int total = 0;
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
	public double getVariantMissingness() {
		List<Double> missingness = new ArrayList<>();
		for ( Variant var : allVariants ) {
			missingness.add(var.getVariantMissingness());
		}
		return calculateAverage(missingness);
	}

	@Override
	public double getHomozogousRefGenotypeFreq() {
		List<Double> refGenotypeFreq = new ArrayList<>();
		for ( Variant var : allVariants ) {
			refGenotypeFreq.add(var.getHomozogousRefGenotypeFreq());
		}
		return calculateAverage(refGenotypeFreq);
	}

	@Override
	public double getHomozygousAltGenotypeFreq() {
		List<Double> altGenotypeFreq = new ArrayList<>();
		for ( Variant var : allVariants ) {
			altGenotypeFreq.add(var.getHomozygousAltGenotypeFreq());
		}
		return calculateAverage(altGenotypeFreq);
	}

	@Override
	public double getHeterozygosity() {
		List<Double> heterozygousFreq = new ArrayList<>();
		for ( Variant var : allVariants ) {
			heterozygousFreq.add(var.getHeterozygosity());
		}
		return calculateAverage(heterozygousFreq);
	}
	
	@Override
	public double getRefAlleleFreq() {
		List<Double> refAlleleFreq = new ArrayList<>();
		for ( Variant var : allVariants ) {
			refAlleleFreq.add(var.getRefAlleleFreq());
		}
		return calculateAverage(refAlleleFreq);
	}

	@Override
	public double getAltAlleleFreq() {
		List<Double> altAlleleFreq = new ArrayList<>();
		for ( Variant var : allVariants ) {
			altAlleleFreq.add(var.getAltAlleleFreq());
		}
		return calculateAverage(altAlleleFreq);
	}

	public static void main(String[] args) {

	}

}
