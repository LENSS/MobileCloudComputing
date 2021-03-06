/*******************************************************************************
 * SAT4J: a SATisfiability library for Java Copyright (C) 2004, 2012 Artois University and CNRS
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU Lesser General Public License Version 2.1 or later (the
 * "LGPL"), in which case the provisions of the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of the LGPL, and not to allow others to use your version of
 * this file under the terms of the EPL, indicate your decision by deleting
 * the provisions above and replace them with the notice and other provisions
 * required by the LGPL. If you do not delete the provisions above, a recipient
 * may use your version of this file under the terms of the EPL or the LGPL.
 *
 * Based on the original MiniSat specification from:
 *
 * An extensible SAT solver. Niklas Een and Niklas Sorensson. Proceedings of the
 * Sixth International Conference on Theory and Applications of Satisfiability
 * Testing, LNCS 2919, pp 502-518, 2003.
 *
 * See www.minisat.se for the original solver in C++.
 *
 * Contributors:
 *   CRIL - initial API and implementation
 *******************************************************************************/
package org.sat4j.pb.reader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Locale;

import org.sat4j.pb.IPBSolver;
import org.sat4j.reader.InstanceReader;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.reader.Reader;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;

/**
 * An reader having the responsibility to choose the right reader according to
 * the input.
 * 
 * @author leberre
 */
public class PBInstanceReader extends InstanceReader {

	private OPBReader2007 opb;

	private Reader reader = null;

	private final IPBSolver solver;

	public PBInstanceReader(IPBSolver solver) {
		super(solver);
		this.solver = solver;
	}

	private Reader getDefaultOPBReader() {
		if (opb == null) {
			opb = new OPBReader2007(solver);
		}
		return opb;
	}

	public boolean hasObjectiveFunction() {
		return opb.hasObjFunc;
	}

	@Override
	public IProblem parseInstance(String filename)
			throws FileNotFoundException, ParseFormatException, IOException,
			ContradictionException {
		String fname;
		boolean isHttp = false;
		String tempFileName = "";
		String prefix = "";

		if (filename.startsWith("http://")) {
			isHttp = true;
			tempFileName = filename;
			filename = filename.substring(filename.lastIndexOf('/'),
					filename.length() - 1);
		}

		if (filename.indexOf(':') != -1) {

			String[] parts = filename.split(":");
			filename = parts[1];
			prefix = parts[0].toUpperCase(Locale.getDefault());

		}

		if (filename.endsWith(".gz")) {
			fname = filename.substring(0, filename.lastIndexOf('.'));
		} else {
			fname = filename;
		}
		if (fname.endsWith(".opb") || "PB".equals(prefix)) {
			reader = getDefaultOPBReader();
		} else {
			return super.parseInstance(filename);
		}

		if (isHttp) {
			return reader.parseInstance((new URL(tempFileName)).openStream());
		}
		return reader.parseInstance(filename);
	}

	@Override
	@Deprecated
	public String decode(int[] model) {
		return reader.decode(model);
	}

	@Override
	public void decode(int[] model, PrintWriter out) {
		reader.decode(model, out);
	}
}
