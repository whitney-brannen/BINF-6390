package independentProject;

public interface VCF {

	// calculations for variant or average for file
	public double getDepth();
	public double getQualityScore();
	// calcuated within instance initialization and then accessed through getters
	public void calcVariantMissingness();
	public void calcHomozogousRefGenotypeFreq();
	public void calcHomozygousAltGenotypeFreq();
	public void calcHeterozygosity();
	public void calcRefAlleleFreq();
	public void calcAltAlleleFreq();
	
}
