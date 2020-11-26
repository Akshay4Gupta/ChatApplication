
import java.io.*; 
import java.util.*;
class Try{
	public static void main(String argv[]) {
		Scanner in = new Scanner(System.in);
		String j[] = new String[5];
		j[0] = "\n";
		j[1] = "\n";
		for(int i = 2; i<5;i++){
			j[i] = in.nextLine();
		}
		String trying = "";
		for(int i = 0; i<5; i++){
			trying+= j[i]+"\n";

		}
		System.out.println("::"+trying+"::");
		trying = trying.trim();

		System.out.println("::"+trying+"::");	
	}
}
