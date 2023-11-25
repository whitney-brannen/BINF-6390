package labs;

import java.util.Random;
import java.util.Scanner;

public class Lab2 {
	
	public static String[] SHORT_NAMES = 
		{ "A","R", "N", "D", "C", "Q", "E", 
		"G",  "H", "I", "L", "K", "M", "F", 
		"P", "S", "T", "W", "Y", "V" };
	
	public static String[] FULL_NAMES = 
		{"alanine","arginine", "asparagine", 
		"aspartic acid", "cysteine",
		"glutamine",  "glutamic acid",
		"glycine" ,"histidine","isoleucine",
		"leucine",  "lysine", "methionine", 
		"phenylalanine", "proline", 
		"serine","threonine","tryptophan", 
		"tyrosine", "valine"};

	public static void main(String[] args) {

		System.out.println("Welcome to the amino acid quiz!\nHow many seconds would you like to play?");
		
		Scanner userTimeLimit = new Scanner(System.in);
		
		int time = userTimeLimit.nextInt(); 
		
		while (true) 
		{
			String score = quiz(time);
			
			System.out.println(score);
			
			System.out.println("Play again? (y/n)");
			
			Scanner ans = new Scanner(System.in);
			
			String playagain = ans.nextLine().toUpperCase();
			
			if ( playagain.equals("Y") )
			{
				continue;
			}
			else if ( playagain.equals("N") )
			{
				System.out.println("Thanks for playing!\nExiting...");
				break;
			}
		}
	}
	
	public static String quiz(int time) {
		
		Random rand = new Random(); 
		
		System.out.println("Game begins now: " + time + " second time limit");
		/*
		 * Initializing as float so score percentage can be calculated correctly
		 */
		float correct = 0;  
		float total = 0;
		
		long start = System.currentTimeMillis();
		long end = start + time * 1000;
		
		while (System.currentTimeMillis() < end) 
		{
			int num = rand.nextInt(FULL_NAMES.length);
			
			System.out.println("Amino Acid: " + FULL_NAMES[num]);
			
			Scanner scanner = new Scanner(System.in);
			
			String userAnswer = scanner.nextLine().toUpperCase();
			
			if ( userAnswer.equals(SHORT_NAMES[num]) ) 
			{
				System.out.println("Correct!");
				correct ++;
				total++;
			}
			else 
			{
				System.out.println("WRONG...\nCorrect answer: " + SHORT_NAMES[num]); 
				total++;
				break;
			}
		}
		float score = (correct/total)*100 ;
		/*
		 * casting correct and total to int because they will always be whole numbers and for visual appeal
		 */
		String results = ((int) correct + " out of " + (int) total + " correct.\nFinal score= " + score + "%" );	
		return results;
		
	}
}