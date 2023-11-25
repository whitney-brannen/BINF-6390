package independentProject;

public interface VCF {
	public double getVariantMissingness();
	public double getHomozogousRefAlleleFreq();
	public double getHomozygousAltAlleleFreq();
	public double getHeterozygousFreq();
	public int getDepth();
}
