package labs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FastaSequence implements Comparable<FastaSequence>
{
	private String header;
	private String seq;

	public FastaSequence ( String header, String seq )
	{
		this.header = header;
		this.seq=seq;
	}
	
	@Override
	public int compareTo(FastaSequence other) {
		int thisLength = this.seq.length();
		int otherLength = other.seq.length();
		
		if ( thisLength < otherLength) {
			// less than
			return -1;
		}
		else if ( thisLength > otherLength) {
			// greater than
			return 1;
		}
		else {
			// equal
			return 0;
		}
	}
	
	
	@Override
	public String toString()
	{
		return ( "Header: >" + getHeader() + " Sequence: " + getSequence() );
	}
	

	
	public String getHeader()
	{
		return header.strip().substring(2,header.length()-1);
	}
	
	public String getSequence() 
	{
		return seq.strip();
	}

	public float getGCRatio() 
	{
		float gcCount = 0;
		float length = seq.length();
		
		String seq = this.seq.strip(); 
		
		for ( int i = 0; i < length; i++)
		{
			char base = seq.charAt(i);
			if ( base == 'G' || base == 'C')
			{
				gcCount++;
			}
		}
		return ( gcCount/length ) ;
	}
	
	public String getSeqID()
	{
		String seqID = getHeader().substring(0, getHeader().indexOf(" "));
		return seqID;
	}
	
	public int countNumBase(char base) throws Exception
	{
		int countBase = 0;
		for ( char c : seq.toCharArray() )
		{
			if ( c == base )
			{
				countBase ++;
			}
		}
		return countBase;
	}
	
	public static List<FastaSequence> readFastaFile(String filepath) throws Exception 
	{
		BufferedReader reader = new BufferedReader(new FileReader(new File(filepath)));
		
		List<FastaSequence> fs = new ArrayList<FastaSequence>();
		
		String currentHeader = null;
		StringBuilder currentSeq = new StringBuilder();

		for (String nextLine = reader.readLine(); nextLine != null; nextLine = reader.readLine()) 
		{
			if ( nextLine.strip().startsWith(">"))
			{
				if ( currentHeader != null )
				{
					fs.add(new FastaSequence(currentHeader, currentSeq.toString()));
					currentSeq.setLength(0);
				}
				currentHeader = nextLine.strip();
			}
			else
			{
				currentSeq = currentSeq.append(nextLine.strip());
			}
			
		}
		
		// add final sequence after hitting end of file
		fs.add(new FastaSequence(currentHeader, currentSeq.toString()));
		
		reader.close();
		return fs;
	}
	
	public static void writeTableSummary( List<FastaSequence> list, File outputFile) throws Exception
	{
		char [] numCountsPerBase = {'A','C','G','T'};
		
		BufferedWriter writer = new BufferedWriter( new FileWriter(outputFile));
		
		writer.write("sequenceID\tnumA\tnumC\tnumG\tnumT\tsequence\n");
		
		for ( FastaSequence f : list )
		{
			writer.write(f.getSeqID()+ "\t");
			for ( char i : numCountsPerBase )
			{
				writer.write(f.countNumBase(i) + "\t");
			}
			writer.write(f.getSequence() + "\n");	
		}
		writer.flush();
		writer.close();
	}
	

	public static void main(String[] args) throws Exception 
	{
		// return list of FastaSequence objects
		List <FastaSequence> fastaList = FastaSequence.readFastaFile("/Users/whitneybrannen/git/BINF-6390/test.fasta");
		System.out.println(fastaList);
		
		// call 3 functions of each FastaSequence object
		for ( FastaSequence f : fastaList)
		{
			System.out.println(f.getHeader());
			System.out.println(f.getSequence());
			System.out.println(f.getGCRatio());
		}
		
		FastaSequence one = fastaList.get(0);
		FastaSequence two = fastaList.get(1);
		System.out.println(one.compareTo(two));
		
		File myFile = new File("/Users/whitneybrannen/fastaTableSummary.txt");

//		writeTableSummary( fastaList,  myFile);
//		System.out.println("Fasta summary found at: " + myFile);

	}
	
}
