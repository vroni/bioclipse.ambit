/*******************************************************************************
 * Copyright (c) 2012  Egon Willighagen <egonw@users.sf.net>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contact: http://www.bioclipse.net/
 ******************************************************************************/
package net.bioclipse.ambit.business;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import org.opentox.rest.RestException;

import net.bioclipse.core.PublishedClass;
import net.bioclipse.core.PublishedMethod;
import net.bioclipse.core.Recorded;
import net.bioclipse.core.business.BioclipseException;
import net.bioclipse.core.domain.IMolecule;
import net.bioclipse.managers.business.IBioclipseManager;
import net.idea.opentox.cli.InvalidInputException;

@PublishedClass(
    value="Manager exposing AMBIT functonality.",
    doi="10.1186/1758-2946-3-18"
)
public interface IAmbitManager<POLICY_RULE> extends IBioclipseManager {

	@Recorded
	@PublishedMethod(
            methodSummary = "Finds a structure in the db defined by inChi string.",
            params="String inchi"
        )
    public String findExactStructure(String inchi);
	
	
	@Recorded
	@PublishedMethod(
			methodSummary = "Finds structures similar to compound given as smiles string.",
			params="String smiles, double threshold"
			)
	public String findSimilarStructure(String smiles, double threshold);
	
	@Recorded
    @PublishedMethod(
    		methodSummary = "Trying to display some information of compound using compound id. Would be great to get smiles or something like that.",
    		params="int id")
    public String displayComponent(int id) throws MalformedURLException, Exception;
	
	@Recorded
	@PublishedMethod(
			methodSummary = "Adds a new component to the db.",
			params="String cas, String name, String sMILES")
	public String addComponent(String cas, String name, String sMILES) throws MalformedURLException, InvalidInputException, Exception;
	
	@Recorded
	@PublishedMethod(
			methodSummary = "Loads data into db. Location of file needs to be given as exact path. Supported formats are "
					+ "SDF, MOL, SMI, CSV, TXT, XLS, ToxML (.xml) and ZIP. ",
			params="String filename, String title")
	public String loadData(String filename, String title) throws Exception;
	
	@Recorded
	@PublishedMethod(
			methodSummary = "Queries the db for components containing the substructure.",
			params="String term")
	public String substructureSearch(String term) throws MalformedURLException, RestException, IOException;
	
    @Recorded
    @PublishedMethod(
        methodSummary = "Calculates the pKa value for the molecule.",
        params="IMolecule molecule"
    )
    public double calculatePKa(IMolecule molecule)
        throws BioclipseException;

    @PublishedMethod ( 
        params = "IMolecule molecule, String smarts",
        methodSummary = "Returns true if the given SMARTS matches the given " +
    		"molecule" )
    @Recorded
    public boolean smartsMatches(IMolecule molecule, String smarts )
    	throws BioclipseException;

    @Recorded
    @PublishedMethod(
        methodSummary = "Counts the OECD functional group counts.",
        params="IMolecule molecule"
    )
    public List<Integer> countOECDGroups(IMolecule molecule)
        throws BioclipseException;

    @Recorded
    @PublishedMethod(
        methodSummary = "Lists the OECD functional group counts."
    )
    public List<String> listOECDGroups() throws BioclipseException;
}
