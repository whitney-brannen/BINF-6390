package independentProject;

public interface VCF {

	// get variables (or their averages)
	public double getDepth();
	public double getQualityScore();

	// calculations for variant or average for file
	public double getVariantMissingness();
	public double getHomozogousRefAlleleFreq();
	public double getHomozygousAltAlleleFreq();
	public double getHeterozygousFreq();
	
}
