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
	public double getHomozogousRefAlleleFreq() {
		List<Double> refAlleleFreq = new ArrayList<>();
		for ( Variant var : allVariants ) {
			refAlleleFreq.add(var.getHomozogousRefAlleleFreq());
		}
		return calculateAverage(refAlleleFreq);
	}

	@Override
	public double getHomozygousAltAlleleFreq() {
		List<Double> altAlleleFreq = new ArrayList<>();
		for ( Variant var : allVariants ) {
			altAlleleFreq.add(var.getHomozygousAltAlleleFreq());
		}
		return calculateAverage(altAlleleFreq);
	}

	@Override
	public double getHeterozygousFreq() {
		List<Double> heterozygousFreq = new ArrayList<>();
		for ( Variant var : allVariants ) {
			heterozygousFreq.add(var.getHeterozygousFreq());
		}
		return calculateAverage(heterozygousFreq);
	}

	public static void main(String[] args) {

	}

}
