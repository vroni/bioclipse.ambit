package test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;

import org.opentox.rest.RestException;

import net.bioclipse.ambit.business.AmbitManager;

/**
 * This class creates statistics for how AMBIT performs.
 * @author vscholz
 *
 */
public class AmbitManagerTest {

	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
		AmbitManager manager = new AmbitManager();
		String inchi = "InChI=1S/C22H28N4O2/c1-3-20(27)23-16-9-7-13-25(14-16)22(28)21-17-10-6-12-19(17)26(24-21)"
				+ "18-11-5-4-8-15(18)2/h4-5,8,11,16H,3,6-7,9-10,12-14H2,1-2H3,(H,23,27)/t16-/m1/s1";
		String smiles = "Nc1ccc(cc1)C(F)(F)F)C3CN(C(=O)CCOc2ccccc2";
		String term = "cccc";
		String filename = "/Users/vscholz/research training/zinc/6_p0.2.sdf.zip";
		
		//Start the writer for writing to stats file
		PrintWriter writer = new PrintWriter("ambit-stats.txt", "UTF-8");
		writer.println("Statistics for AMBIT2 database\n");
		writer.println(Calendar.getInstance().getTime()+"\n");
		
		//Test exact search
		writer.print("Exact structure search: ");
		long startTime = System.nanoTime();
		try {
			manager.findExactStructure(inchi);
		} catch (RestException | IOException e) {
			e.printStackTrace();
		}
		long endTime = System.nanoTime();
		long duration = endTime - startTime;
		writer.println(duration);
		
		//Test similarity search
		writer.print("Similar structure search: ");
		startTime = System.nanoTime();
		try {
			manager.findSimilarStructure(smiles, 0.7);
		} catch (RestException | IOException e) {
			e.printStackTrace();
		}
		endTime = System.nanoTime();
		duration = endTime - startTime;
		writer.println(duration);
		
		// Test substructure search
		writer.print("Substructure search: ");
		startTime = System.nanoTime();
		try {
			manager.substructureSearch(term);
		} catch (RestException | IOException e) {
			e.printStackTrace();
		}
		endTime = System.nanoTime();
		duration = endTime - startTime;
		writer.println(duration);
		
		// Test loading data
//		writer.print("Loading .zip file of size 93 MB (uncompressed 564.8 MB): ");
//		try {
//			manager.loadData(filename, "6_p0.2");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		endTime = System.nanoTime();
//		duration = endTime - startTime;
//		writer.println(duration);
		writer.close();
	}
}
