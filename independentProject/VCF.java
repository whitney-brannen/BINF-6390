package independentProject;

public interface VCF {
	
	// get variables (or their averages)

	public int getDepth();
	public int getQualityScore();

	// calculations
	public double getVariantMissingness();
	public double getHomozogousRefAlleleFreq();
	public double getHomozygousAltAlleleFreq();
	public double getHeterozygousFreq();
	
}
