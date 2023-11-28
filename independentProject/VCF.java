package independentProject;

public interface VCF {

	// get variables (or their averages)
	public double getDepth();
	public double getQualityScore();

	// calculations for variant or average for file
	public double getVariantMissingness();
	public double getHomozogousRefGenotypeFreq();
	public double getHomozygousAltGenotypeFreq();
	public double getHeterozygosity();
	public double getRefAlleleFreq();
	public double getAltAlleleFreq();
	
}
