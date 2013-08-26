/*******************************************************************************
 * Copyright (c) 2012  Egon Willighagen <egonw@users.sf.net>
 *               2008  Nikolay Kochev <nick@uni-plovdiv.bg>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contact: http://www.bioclipse.net/
 ******************************************************************************/
package net.bioclipse.ambit.business;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.bioclipse.cdk.business.CDKManager;
import net.bioclipse.cdk.domain.ICDKMolecule;
import net.bioclipse.core.business.BioclipseException;
import net.bioclipse.core.domain.IMolecule;
import net.bioclipse.managers.business.IBioclipseManager;
import net.idea.opentox.cli.InvalidInputException;
import net.idea.opentox.cli.OTClient;
import net.idea.opentox.cli.Resources;
import net.idea.opentox.cli.dataset.Dataset;
import net.idea.opentox.cli.dataset.DatasetClient;
import net.idea.opentox.cli.dataset.InputData;
import net.idea.opentox.cli.dataset.Rights;
import net.idea.opentox.cli.dataset.Rights._type;
import net.idea.opentox.cli.structure.Compound;
import net.idea.opentox.cli.structure.CompoundClient;
import net.idea.opentox.cli.structure.CompoundClient.QueryType;
import net.idea.opentox.cli.task.RemoteTask;

import org.apache.log4j.Logger;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.isomorphism.UniversalIsomorphismTester;
import org.openscience.cdk.isomorphism.matchers.IQueryAtom;
import org.openscience.cdk.isomorphism.matchers.QueryAtomContainer;
import org.openscience.cdk.isomorphism.mcss.RMap;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.result.IntegerArrayResult;
import org.opentox.rest.RestException;

import ambit2.descriptors.FunctionalGroupDescriptor;
import ambit2.descriptors.PKASmartsDescriptor;
import ambit2.descriptors.VerboseDescriptorResult;
import ambit2.smarts.SmartsParser;

public class AmbitManager<POLICY_RULE> implements IBioclipseManager {

    private static final Logger logger = Logger.getLogger(AmbitManager.class);
    private static final SmartsParser smartsParser = new SmartsParser();
    public final static OTClient otclient = new OTClient();
    private final PKASmartsDescriptor pkaDescriptor = new PKASmartsDescriptor();
    private final FunctionalGroupDescriptor oecdDescriptor = new FunctionalGroupDescriptor();
    private final String db_location = "http://localhost:8080/ambit2-www-2.4.11";

    //FIXME get manager from Spring instead
    private CDKManager cdk = new CDKManager();
    private List<URL> uri;

    /**
     * Gives a short one word name of the manager used as variable name when
     * scripting.
     */
    public String getManagerName() {
        return "ambit";
    }
    
    /**
     * Find structure in DB from exact inChi String. Exact structure search relies on inChi representation.
     * @param inchi  String representing chemical structure which is looked up.
     * @return  URI with identifier where component was found. Null if exception was thrown.
     * @throws IOException 
     * @throws RestException 
     * @throws MalformedURLException 
     */
    public String findExactStructure(String inchi) throws MalformedURLException, RestException, IOException {
    	CompoundClient<POLICY_RULE> cli = otclient.getCompoundClient();
    	uri = cli.searchExactStructuresURI(new URL(db_location), inchi, QueryType.inchikey, false);
    	return uri.toString();
    }
    
    /**
     * Finds structures similar to the given one in the database. Enter a threshold that represents the degree
     * of similarity.
     * @param smiles  String representing the chemical where a similar one is supposed to be found.
     * @param threshold  double representing the similarity of the two compounds.
     * @return  List of URIs with identifiers for found structures.
     * @throws IOException 
     * @throws RestException 
     * @throws MalformedURLException 
     */
    public String findSimilarStructure(String smiles, double threshold) throws MalformedURLException, RestException, IOException {
    	CompoundClient<POLICY_RULE> cli = otclient.getCompoundClient();
    	uri = cli.searchSimilarStructuresURI(new URL(db_location), smiles, QueryType.smiles, false, threshold);
    	return uri.toString();
    }

    /**
     * Runs a substructure search in the db using the given term.
     * @param term  String representing substructure
     * @return  List of URIs containing components that include the substructure.
     * @throws MalformedURLException
     * @throws RestException
     * @throws IOException
     */
    public String substructureSearch(String term) throws MalformedURLException, RestException, IOException {
    	CompoundClient<POLICY_RULE> cli = otclient.getCompoundClient();
    	uri = cli.searchSubstructuresURI(new URL(db_location), term);
    	return uri.toString();
    }
    
    /**
     * Adds a new compound to the db.
     * @param cas  CAS of the component.
     * @param name String representing name of the component.
     * @param sMILES  SMILES string of the component.
     * @return Message printed after action.
     * @throws MalformedURLException
     * @throws InvalidInputException
     * @throws Exception
     */
    public String addComponent(String cas, String name, String sMILES) throws MalformedURLException, InvalidInputException, Exception {
    	otclient.setHTTPBasicCredentials("localhost", 8080,"admin", "changeit");
    	CompoundClient<POLICY_RULE> cli = otclient.getCompoundClient();
		Compound substance = new Compound();
		
		// More properties can be added to the compound.
		substance.setCas(cas);
		substance.setName(name);
		substance.setSMILES(sMILES);
    	RemoteTask task = cli.registerSubstanceAsync(new URL(db_location), substance,"TEST_ID","12345");
    	task.waitUntilCompleted(500);
		return task.getResult().toString();
    }
    
    /**
     * Load data into db from a file. File location has to be given with exact path. Rights are set by default
     * to "http://creativecommons.org/licenses/by-sa/2.0/". FIXME use of exact path is discouraged!
     * @param filename  String representing filename of file to be loaded. Needs to be exact path!!!
     * @param title  String representing title of dataset.
     * @throws Exception 
     */
    public String loadData(String filename, String title) throws Exception {
    	otclient.setHTTPBasicCredentials("localhost", 8080,"admin", "changeit");

		File fileToImport = new File(filename);
		Dataset dataset = new Dataset();
		dataset.getMetadata().setTitle(title);
		dataset.getMetadata().setRights(new Rights("CC-BY-SA","http://creativecommons.org/licenses/by-sa/2.0/",_type.license));
		dataset.setInputData(new InputData(fileToImport,DatasetClient._MATCH.InChI));
		DatasetClient<POLICY_RULE> otClient = otclient.getDatasetClient();
		RemoteTask task = otClient.postAsync(dataset,new URL(String.format("%s%s", db_location, Resources.dataset)));
		task.waitUntilCompleted(1000);
		return task.getResult().toString();
    }
    
    /**
     * Display information of compound as string.
     * @param id  int representing id of desired compound
     * @throws MalformedURLException
     * @throws Exception
     */
    public String displayComponent(int id) throws MalformedURLException, Exception {
    	CompoundClient<POLICY_RULE> otClient = otclient.getCompoundClient();
		//get the first record
		List<Compound> substances = otClient.getIdentifiersAndLinks(
				new URL(String.format("%s", db_location)),
				new URL(String.format("%s%s/1", db_location, Resources.compound))
				);	
		StringBuilder str = new StringBuilder();
		for (Compound s : substances) {
			str.append(s.getName() + ", " + s.getSMILES());
			str.append("\n");
		}
		return str.toString();
    }
    
    public double calculatePKa(IMolecule molecule) throws BioclipseException {
    	logger.debug("Calculate the pKa");
    	ICDKMolecule cdkMol = cdk.asCDKMolecule(molecule);
    	DescriptorValue pka = pkaDescriptor.calculate(cdkMol.getAtomContainer());
    	
    	return ((DoubleResult)((VerboseDescriptorResult)pka.getValue()).getResult()).doubleValue();
    }

    public List<String> listOECDGroups() throws BioclipseException {
    	List<String> groups = new ArrayList<String>();
    	String[] names = oecdDescriptor.getDescriptorNames();
    	for (int i=0;i<names.length;i++) groups.add(names[i]);
    	return groups;
    }

    public List<Integer> countOECDGroups(IMolecule molecule)
            throws BioclipseException {
    	logger.debug("Calculate the OECD functional group counts");
    	ICDKMolecule cdkMol = cdk.asCDKMolecule(molecule);
    	DescriptorValue pka = oecdDescriptor.calculate(cdkMol.getAtomContainer());
    	
    	IntegerArrayResult result = (IntegerArrayResult)((VerboseDescriptorResult)pka.getValue()).getResult();
    	List<Integer> counts = new ArrayList<Integer>();
    	for (int i=0;i<result.length();i++) counts.add(result.get(i));
    	return counts;
    }
    
    public boolean smartsMatches(IMolecule molecule, String smarts )
    	throws BioclipseException {
    	ICDKMolecule cdkMol = cdk.asCDKMolecule(molecule);

    	QueryAtomContainer query = smartsParser.parse(smarts);
    	String error = smartsParser.getErrorMessages();
    	if (error.length() != 0) {
    		throw new BioclipseException("SMARTS Parser error: " + error);
    	}
    	IAtomContainer atomContainer = cdkMol.getAtomContainer();
    	smartsParser.setSMARTSData(atomContainer);

    	try {
			return matches(atomContainer, query);
		} catch (CDKException exception) {
			throw new BioclipseException("SMARTS matching error: " + error);
		}
    }

	private boolean matches(IAtomContainer atomContainer, QueryAtomContainer query) throws CDKException {
		List<List<Integer>> matchingAtoms;

		// lets see if we have a single atom query
		if (query.getAtomCount() == 1) {
			// lets get the query atom
			IQueryAtom queryAtom = (IQueryAtom) query.getAtom(0);			
			matchingAtoms = new ArrayList<List<Integer>>();
			Iterator<IAtom> atoms = atomContainer.atoms().iterator();
			while (atoms.hasNext()) 
			{
				IAtom atom = atoms.next();
				if (queryAtom.matches(atom)) {
					List<Integer> tmp = new ArrayList<Integer>();
					tmp.add(atomContainer.getAtomNumber(atom));
					matchingAtoms.add(tmp);
				}
			}
		} else {
			List<List<RMap>> bondMapping = UniversalIsomorphismTester.getSubgraphMaps(atomContainer, query);
			matchingAtoms = getAtomMappings(bondMapping, atomContainer);
		}		
		return matchingAtoms.size() != 0;
	}

	private List<List<Integer>> getAtomMappings(List<List<RMap>> bondMapping, IAtomContainer atomContainer) {
		List<List<Integer>> atomMapping = new ArrayList<List<Integer>>();
		// loop over each mapping
		for (List<RMap> aBondMapping : bondMapping) {
			List<RMap> list = aBondMapping;
			
			List<Integer> tmp = new ArrayList<Integer>();
			IAtom atom1 = null;
			IAtom atom2 = null;
			// loop over this mapping
			for (Object aList : list) {
				RMap map = (RMap) aList;
				int bondID = map.getId1();
				
				// get the atoms in this bond
				IBond bond = atomContainer.getBond(bondID);
				atom1 = bond.getAtom(0);
				atom2 = bond.getAtom(1);
				
				Integer idx1 = atomContainer.getAtomNumber(atom1);
				Integer idx2 = atomContainer.getAtomNumber(atom2);
				
				if (!tmp.contains(idx1)) tmp.add(idx1);
				if (!tmp.contains(idx2)) tmp.add(idx2);
			}
			if (tmp.size() > 0) atomMapping.add(tmp);
			
			// If there is only one bond, check if it matches both ways.
			if (list.size() == 1 && atom1.getAtomicNumber() == atom2.getAtomicNumber()) {
				List<Integer> tmp2 = new ArrayList<Integer>();
				tmp2.add(tmp.get(0));
				tmp2.add(tmp.get(1));
				atomMapping.add(tmp2);
			}
		}
		return atomMapping;
	}
}
