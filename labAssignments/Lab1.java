package labs;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;

public class Lab1 {

	public static void main(String[] args) {
		
		/*
		 * Parts 1 & 2
		 * 
		 * The observed AAA count seem to be much higher than the expected AAA count count when randomly sampling integers in java.
		 * Almost all runs show an observed count nearly double the expected count based on my calculations.
		 */
		
		Random rand = new Random(); 
				
		int counter = 0; // counting AAA
		
		for ( int i=0; i<1000; i++)
		{
			for ( int o=0; o<3; o++)
			{
				int x = rand.nextInt(4);
				int y = rand.nextInt(4);
				int z = rand.nextInt(4);
				String code = (""+ x + y + z + "\n"); // randomly generated codons as numbers 0,1,2,3; had to make string to not add int values
				
				String codon = "";
				
				for (int pos=0; pos <= 3; pos++) // translate random numbers to nucleotides
				{	
					if ( code.substring(pos,pos+1).equals("0") )
					{
						codon = codon + "A";
					}
					else if ( code.substring(pos,pos+1).equals("1") )
					{
						codon = codon + "T";
					}
					else if ( code.substring(pos,pos+1).equals("2") )
					{
						codon = codon + "G";
					}
					else if ( code.substring(pos,pos+1).equals("3") )
					{
						codon = codon + "C";
					}
				}
				// comment below line out to see just observed and expected counts
				System.out.println(codon);
				
				if ( codon.equals("AAA"))
				{
					counter++;
				}
			}
		}
		
		System.out.println("Observed AAA count:" + counter);
		System.out.println("Expected AAA count:" + ( (.25*.25*.25) * 1000));
		
		/*
		 * Part 3: changing frequencies
		 * 
		 * The observed and expected AAA count with the weighted frequencies is very close on all runs.  This is unlike the results of the 
		 * un-weighted probabilities.  I expect this has to do with creating a list of nucleotides as their frequencies, and randomly sampling from a larger
		 * range (out of 100) instead of only sampling out of 4 like above. 
		 */
		
		List<String> weighted = new ArrayList<String>(); //creating mutable arrayList of each letter by their probability to randomly sample from
		
		// add each letter at its specified frequency out of 100
		for (int a=0; a<12; a++ )
		{
			weighted.add("A");
		}
		for (int c=0; c<38; c++ )
		{
			weighted.add("C");
		}
		for (int g=0; g<39; g++ )
		{
			weighted.add("G");
		}
		for (int t=0; t<11; t++ )
		{
			weighted.add("T");
		}
		
		
		int weightedCounter = 0;
		
		for ( int i=0; i<1000; i++)
		{
			String weightedCodon = "";
			for ( int o=0; o<3; o++)
			{
				weightedCodon = weightedCodon + weighted.get(rand.nextInt(weighted.size())); //randomly sample int for index then retrieve value and add to string
			}
			// comment below line out to see just observed and expected counts
			System.out.println(weightedCodon);
			
			if ( weightedCodon.equals("AAA") )
			{
				weightedCounter++;
			}
		}

		System.out.println("Observed weighted AAA count:" + weightedCounter);
		System.out.println("Expected weighted AAA count:" + ( (.12*.12*.12) * 1000));
		
	}

}
