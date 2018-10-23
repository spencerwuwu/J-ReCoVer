// https://searchcode.com/api/result/138245948/

package sc.bruse.engine.propagation.lazy;

/***********************************
 * Copyright 2008 Scott Langevin
 * 
 * All Rights Reserved.
 *
 * This file is part of BRUSE.
 *
 * BRUSE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BRUSE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with BRUSE.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * @author Scott Langevin (scott.langevin@gmail.com)
 *
 */

import sc.bruse.engine.*;
import sc.bruse.engine.bigclique.*;
import sc.bruse.engine.propagation.IPropagationEngine;
import sc.bruse.engine.propagation.hugin.HuginPropagationEngine;
import sc.bruse.network.BruseAPIException;
import sc.bruse.network.BruseEvidence;
import sc.bruse.network.BruseNode;
import sc.bruse.network.BruseTable;

import java.util.*;

public class LazyPropagationEngine extends HuginPropagationEngine {
	
	private Hashtable<String, ArrayList<BruseNode>> m_dconnected;
	
	public LazyPropagationEngine() {
		super();
		
		m_dconnected = new Hashtable<String, ArrayList<BruseNode>>();
	}
	
	public void init() {
		long StartTime = System.currentTimeMillis();
		
		// gen cliques using the big clique 
		m_cliques = BigCliqueFactory.createCliques(m_network, m_sEvidence);
		
		long EndTime = System.currentTimeMillis();
		BookKeepingMgr.TimeCreateCliques = (EndTime - StartTime);
		
		StartTime = System.currentTimeMillis();
		
		// create junction tree
		m_junctionTree = new JunctionTree(m_cliques);
		
		EndTime = System.currentTimeMillis();		
		BookKeepingMgr.TimeCreateJunctionTree = (EndTime - StartTime);
		
		// set the root to the big clique in the junction tree
		// the BigCliqueFactory sets the first element in the list
		// to the Big Clique, if none is found whatever is the first element
		// is chosen
		m_junctionTree.setRoot(0); //1);
		
		//TEST
		//JunctionTree.dumpJunctionTree(m_junctionTree);
		
		// propagation is needed since we have rebuilt the junction tree
		m_isDirty = true;
	}
	
	public void propagate() {
		// pass collections of potentials
		// exploit barren nodes
		//		Axiom: xP(X|pa(X) = 1
		//		rule:  If A is not in dom(C) and the only potential with A in dom is P(A|pa(A)) then A is discarded
		// exploit d-separation
		// domain reduction due to evidence
		
		// Don't propagate unless the dirty flag is set - evidence has changed or a propagate has not been done yet
		if (m_isDirty == false) return;
		
		long StartTime = System.currentTimeMillis();
		
		// dconnection must be recalculated because evidence has changed
		m_dconnected.clear();
		
		// need to rebuild the clique potential lists to initial state
		for (int i=0; i < m_cliques.size(); i++) {
			m_cliques.get(i).resetPotentials();
		}
		
		// Reset the separators - remove previous propagation values from separators
		m_junctionTree.resetSeparators();
		
		// first apply hard evidence
		applyHardEvidence();
		
		// next collect evidence to root
		collectEvidence(m_junctionTree.getRoot(), null);
	
//		m_junctionTree.getRoot().getClique().combinePotentials();
//		m_junctionTree.getRoot().getClique().getTable().normalize();
//		BruseTable.dumpTable(m_junctionTree.getRoot().getClique().getTable(), false);
		
		//JunctionTree.dumpJunctionTree(m_junctionTree);
		
		// apply the soft evidence to the big clique
		applySoftEvidence();
		
//		m_junctionTree.getRoot().getClique().getTable().normalize();
//		BruseTable.dumpTable(m_junctionTree.getRoot().getClique().getTable(), false);
		
		// TEST
//		BruseTable t = m_junctionTree.getRoot().getClique().getTable().getMarginal("BP");
//		t.normalize();
//		BruseTable.dumpTable(t, false);
//		t = m_junctionTree.getRoot().getClique().getTable().getMarginal("VentAlv");
//		t.normalize();
//		BruseTable.dumpTable(t, false);
//		t = m_junctionTree.getRoot().getClique().getTable().getMarginal("Shunt");
//		t.normalize();
//		BruseTable.dumpTable(t, false);
		
		// next distribute evidence from root
		distributeEvidence(m_junctionTree.getRoot(), null);
		
//		for (Clique c: m_cliques) {
//			if (c != m_junctionTree.getRoot().getClique()) {
//				c.combinePotentials();
//			}
//			
//			c.getTable().normalize();
//		}
//		System.out.println("After distribute evidence:");
//		JunctionTree.dumpJunctionTree(m_junctionTree);
		
		//JunctionTree.dumpJunctionTree(m_junctionTree);
		
		// calculate the posterior marginals for each variable in the BN
		calculateMarginals();
		
		long EndTime = System.currentTimeMillis();
		BookKeepingMgr.TimePropagation = (EndTime - StartTime);
		
		// no longer dirty
		m_isDirty = false;
	}
	
	protected void applyHardEvidence() {
		BruseEvidence finding = null;
		Clique clique = null;
		
		// associate hard evidence with every clique that contains variable
		for (int i=0; i < m_cliques.size(); i++) {
			clique = m_cliques.get(i);
			
			for (int j=0; j < m_hEvidence.size(); j++) {
				finding = m_hEvidence.get(j);
				
				if (clique.containsNode(finding.getNodeName())) {
					//TODO this should reduce the domain of each potential by the evidence
					for (int k=0; k < clique.getPotentials().size(); k++) {
						BruseTable pot = clique.getPotentials().get(k);
						if (pot.containsVariable(finding.getNodeName())) {
							
							pot.absorb(finding.getTable(m_network));
							
							// TEST
							ArrayList<String> names = pot.getVariableNames();
							names.remove(finding.getNodeName());
							clique.getPotentials().set(k, pot.getMarginal(names));
						}
					}
					
					//clique.addPotential(finding.getTable(m_network));
					//clique.reducePotentialDomain(finding.getTable(m_network));
				}
			}
		}
	}
	
	protected void applySoftEvidence() {
		// Use IPFP to absorb soft evidence
		
		// if there is no soft evidence then skip this
		if (m_sEvidence.size() == 0) return;
		
		// first combine the potentials in the big clique so we can perform IPFP 
		m_junctionTree.getRoot().getClique().combinePotentials();
//		m_junctionTree.getRoot().getClique().getTable().normalize();
//		BruseTable.dumpTable(m_junctionTree.getRoot().getClique().getTable(), false);
		
		// apply the soft evidence (same as Hugin)
		super.applySoftEvidence();
		
		// clear the set of potentials in the big clique since we now have an updated combined potential
		m_junctionTree.getRoot().getClique().clearPotentials();
		BruseTable table = m_junctionTree.getRoot().getClique().getTable();
		
		// add the updated potential to the list of potentials for the big clique
		m_junctionTree.getRoot().getClique().addPotential(table);
	}
	
	protected void collectEvidence(JunctionNode node, JunctionNode caller) {
		JunctionSeparator separator;
		ArrayList<BruseTable> msg = null, pot = node.getClique().getPotentials();
		
		for (int i=0; i < node.getNeighbors().size(); i++) {
			JunctionNode neighbor = (JunctionNode)node.getNeighbors().get(i);
			
			// call collectEvidence on all neighbors except caller
			if (neighbor != caller) {
				// collect evidence from neighbor
				collectEvidence(neighbor, node);
			
				// absorb innerMsg of separator with neighbor
				msg = node.getSeparator(neighbor).getInnerMsgList();
				node.getClique().addPotentials(msg);
			}
		}
		
		// after all neighbors have returned evidence send message to separator

		if (caller != null) {
			
			//System.out.println("Clique " + node.getName() + " calculating message to Clique " + caller.getName() + "...");
			
			separator = node.getSeparator(caller);
			// Set the message to the list of relevant potentials
			msg = findRelPots(pot, separator.getVariables());
		
			/*System.out.println("Clique " + node.getName() + " sending message to Clique " + caller.getName() + ": ");
			for (BruseTable m: msg) {
				System.out.println(m + ", ");
			}*/
			// set separator inner message
			separator.setInnerMsg(msg);
		}
	}
	
	private ArrayList<BruseNode> getDConnected2(ArrayList<String> query) {
		DConnected dcon = new DConnected(m_network);
		
		// create list of all evidence
		ArrayList<String> evidence = new ArrayList<String>();
		evidence.addAll(getEvidenceVarNames(m_hEvidence));
		evidence.addAll(getEvidenceVarNames(m_sEvidence));
		
		dcon.setEvidence(evidence);
		
		return dcon.getConnected((query));
	}
	
	private ArrayList<BruseNode> getDConnected(ArrayList<String> query) {
		ArrayList<String> reqQuery = new ArrayList<String>();
		reqQuery.addAll(query);
		ArrayList<BruseNode> result = new ArrayList<BruseNode>();
		ArrayList<BruseNode> dsep = null;
		
		// if dconnection was already determined for a variable do not recalculate
		for (int i=reqQuery.size() - 1; i >= 0; i--) {
			dsep = m_dconnected.get(reqQuery.get(i));
			if (dsep != null) {
				for (int j=0; j < dsep.size(); j++) {		
					if (result.contains(dsep.get(j)) == false) {
						result.add(dsep.get(j));
					}
				}
				reqQuery.remove(i);
			}
		}
		
		if (reqQuery.size() > 0) {
			// Create a dseparation analyzer - it can calculate dconnection
			//DSeparationAnalyzer dsepAnalyzer = new DSeparationAnalyzer(m_network);
			DConnected dcon = new DConnected(m_network);
		
			// create list of all evidence
			ArrayList<String> evidence = new ArrayList<String>();
			evidence.addAll(getEvidenceVarNames(m_hEvidence));
			evidence.addAll(getEvidenceVarNames(m_sEvidence));
			
			dcon.setEvidence(evidence);
			
			String qName = null;
			ArrayList<String> q = null;
			ArrayList<BruseNode> nodes = null;
			
			// calculate dconnection for rest of query that could not be looked up
			for (int i=0; i < reqQuery.size(); i++) {
				qName = reqQuery.get(i);
				q = new ArrayList<String>();
				q.add(qName);
				
				//nodes = dsepAnalyzer.getDseparation(q, evidence);
				//System.out.println(getVarNames(nodes));
				
				//nodes = dsepAnalyzer.getDconnection(q, evidence);
				nodes = dcon.getConnected(q);
				
				//System.out.println(getVarNames(nodes));
				
				
				m_dconnected.put(qName, nodes); // index nodes for future use
				
				// add unique
				for (int j=0; j < nodes.size(); j++) {		
					if (result.contains(nodes.get(j)) == false) {
						result.add(nodes.get(j));
					}
				}
			}
		}
		
		return result;
	}
	
	private ArrayList<BruseTable> removeReceivedPots(ArrayList<BruseTable> potentials, ArrayList<BruseTable> received) {
		BruseTable pot = null;
		ArrayList<BruseTable> relPots = new ArrayList<BruseTable>();
		
		// remove any potential in received list
		for (int i=0; i < potentials.size(); i++) {
			pot = potentials.get(i);
			if (!received.contains(pot)) {
				relPots.add(pot);
			}
		}
		
		return relPots;
	}
	
	private ArrayList<BruseTable> findRelPots(ArrayList<BruseTable> potentials, BruseNode node) {
		ArrayList<BruseNode> nodes = new ArrayList<BruseNode>(1);
		nodes.add(node);
		//return VariableElimination.query(potentials, nodes); 
		return findRelPots(potentials, nodes);
	}
	
	private ArrayList<BruseTable> findRelPots(ArrayList<BruseTable> potentials, ArrayList<BruseNode> nodes) {
		// find potentials that are d-connected to nodes
		// eliminate barren head variables
		// marginalize out any variables not in nodes
		
		ArrayList<BruseTable> relPots = new ArrayList<BruseTable>();
		
		// TODO switch back to older method of mulitplication
		
		// TODO rewrite using ancestral graph method?
		relPots = findConnectedPots(potentials, nodes);
		//relPots.addAll(potentials);
//		if (potentials.size() != relPots.size()) System.err.println("Removed: " + (potentials.size() - relPots.size()));
		
		// TODO rewrite barren var to check if descendents have evidence
		relPots = removeBarrenVars2(relPots, nodes);
		
//		
//		// TEST
//		relPots.clear();
//		relPots.addAll(potentials);
		
		//long StartTime = System.currentTimeMillis();
		//elPots = getMarginal(potentials, nodes);
		relPots = VariableElimination.query(relPots, nodes);
		
//		long EndTime = System.currentTimeMillis();
//		BookKeepingMgr.TMP += (EndTime - StartTime);
		
		return relPots;
	}

	private boolean isVarInBigClique(BruseTable pot) {
		for (BruseNode node: pot.getVariables()) {
			if (m_junctionTree.getRoot().getClique().containsNode(node.getName())) return true;
		}
		
		return false;
	}
	
	private ArrayList<BruseTable> findConnectedPots2(ArrayList<BruseTable> potentials, ArrayList<BruseNode> nodes) {
		ArrayList<BruseTable> relPots = new ArrayList<BruseTable>();
		ArrayList<BruseTable> workingSet = new ArrayList<BruseTable>();
		Set<String> domain = new HashSet<String>();
		
		workingSet.addAll(potentials);
		
		// create initial domain
		for (BruseNode node: nodes) {
			domain.add(node.getName());
		}
		
		for (BruseTable pot: potentials) {
			if (pot.getVariables().length == 0) {
				relPots.add(pot);
				workingSet.remove(pot);
			}
			else {
				for (String var: domain) {
					if (pot.containsVariable(var)) {
						relPots.add(pot);
						workingSet.remove(pot);
						domain.addAll(pot.getVariableNames());
						break;
					}
				}
			}
		}
		
		int oldSize = 0;
		
		while (relPots.size() != oldSize) {
			oldSize = relPots.size();
			
			for (int i=workingSet.size()-1; i >= 0; i--) { //(BruseTable pot: workingSet) {
				BruseTable pot = workingSet.get(i);

				for (String var: domain) {
					if (pot.containsVariable(var)) {
						relPots.add(pot);
						workingSet.remove(pot);
						domain.addAll(pot.getVariableNames());
						break;
					}
				}
			}
		}
		
		/*for (BruseTable pot: workingSet) {
			System.out.println("D-sep: " + pot);
		}*/
		
		return relPots;
	}
	
	private ArrayList<BruseTable> findConnectedPots(ArrayList<BruseTable> potentials, ArrayList<BruseNode> nodes) {
		BruseTable pot = null;
		ArrayList<BruseTable> workingSet = new ArrayList<BruseTable>();
		ArrayList<BruseTable> relPots = new ArrayList<BruseTable>();
		ArrayList<String> varNames = getVarNames(nodes);
		ArrayList<BruseNode> conVarNames = getDConnected2(varNames);
		
		workingSet.addAll(potentials);
		
		// for each potential check if it contains at least one connected variable
		// if it does add to the rel potentials list, also any potential that is a normalization constant (domain of 0)
		for (int i=0; i < potentials.size(); i++) {
			pot = potentials.get(i);
			for (int j=0; j < conVarNames.size(); j++) {
				if ( pot.containsVariable( conVarNames.get(j).getName() ) || pot.getVariables().length == 0) {
				//if ( pot.containsVariable( conVarNames.get(j).getName() ) || pot.getVariables().length == 0 || isVarInBigClique(pot)) {
					relPots.add(pot);
					workingSet.remove(pot);
					break;
				}
			}
		}
		
		/*for (BruseTable p: potentials) {
			if (!relPots.contains(p)) {
				System.out.println("D-sep potential: " + p);
			}
		}*/
		
		/*
		int oldSize = 0;
		while (relPots.size() != oldSize) {
			oldSize = relPots.size();
			for (int i=0; i < relPots.size(); i++) {
				pot = relPots.get(i);
				for (int j=workingSet.size()-1; j >= 0; j--) {
					for (int k=0; k < pot.getVariables().length; k++) {
						if ( workingSet.get(j).containsVariable(pot.getVariables()[k].getName())) {
							relPots.add(workingSet.get(j));
							workingSet.remove(j);
							break;
						}
					}
				}
			}
		}*/
		
		return relPots;
	}
	
	private boolean isBarren(BruseTable test, Collection<BruseTable> potentials, ArrayList<BruseNode> domain) {
		// Only conditional potentials can be barren
		if (test.getType() != BruseTable.PotentialType.Conditional) return false;
		
		for (BruseNode node: domain) {
			// If the head variable is a member of the domain then not considered barren
			if (test.getHeadVar().getName().equalsIgnoreCase(node.getName())) return false;
		}
		
		for (BruseTable pot: potentials) {
			// Not considered barren if another potentials has head var in domain
			if (pot != test && pot.containsVariable(test.getHeadVar().getName())) return false;
		}
		
		// No other potentials has head var in domain so test is considered barren
		return true;
	}
	
	private ArrayList<BruseTable> removeBarrenVars2(ArrayList<BruseTable> potentials, ArrayList<BruseNode> nodes) {
		//ArrayList<BruseTable> relPots = new ArrayList<BruseTable>();
		HashSet<BruseTable> relPots = new HashSet<BruseTable>();

		// initially all potentials are considered relevant
		for (BruseTable pot: potentials) {
			relPots.add(pot);
		}
		
		int oldSize = 0;
		
		// repeat until the relPots collection has not changed
		while (oldSize != relPots.size()) {
			oldSize = relPots.size();
			BruseTable barren = null;
			
			for (BruseTable pot: relPots) {
				if (isBarren(pot, relPots, nodes)) {
					barren = pot;
					break;
				}
			}
			
			/*if (barren != null) {
				System.out.println("Barren pot: " + barren);
				relPots.remove(barren);
			}*/
		}
		
		ArrayList<BruseTable> result = new ArrayList<BruseTable>(relPots);
		
		return result;
	}
	
	private ArrayList<BruseTable> removeBarrenVars(ArrayList<BruseTable> potentials, ArrayList<BruseNode> nodes) {
		BruseTable pot = null;
		String head = null;
		ArrayList<String> varNames = getVarNames(nodes);
		ArrayList<BruseTable> relPots = new ArrayList<BruseTable>();
		
		for (int i=0; i < potentials.size(); i++) {
			pot = potentials.get(i);
			if (pot.getType() == BruseTable.PotentialType.Conditional && pot.getVariables().length > 0) {
				head = pot.getVariables()[pot.getVariables().length-1].getName();
				if (!varNames.contains(head)) {
					for (int j=0; j < potentials.size(); j++) {
						if ( (i != j) && potentials.get(j).containsVariable(head) ) {
							relPots.add(pot);
							break;
						}
					}
				}
				else {
					relPots.add(pot);
				}
			}
			else {
				relPots.add(pot);
			}
		}
		
		/*for (BruseTable p: potentials) {
			if (!relPots.contains(p)) {
				System.out.println("Barren variable: " + p);
			}
		}*/
		
		return relPots;
	}
	
	// No longer needed
	/*private ArrayList<BruseTable> getMarginal(ArrayList<BruseTable> potentials, ArrayList<BruseNode> nodes) {
		BruseTable pot = null, marg = null;
		ArrayList<String> varNames = getVarNames(nodes), domain = null;
		ArrayList<BruseTable> relPots = new ArrayList<BruseTable>();
		
		// for each potential that contains variables not in nodes combine those potentials
		for (int i=0; i < potentials.size(); i++) {
			pot = potentials.get(i);
			domain = pot.getVariableNames();
			
			if (!varNames.containsAll(domain) && pot.getVariables().length > 0) {
				if (marg == null) {
					marg = pot;
				}
				else {
					marg = marg.multiplyBy(pot);
				}
			}
			else {
				relPots.add(pot);
			}
		}
		
		// if we have combined potentials then marginalize
		if (marg != null) relPots.add(marg.getMarginal(varNames));
		
		return relPots;
	}*/
		
	protected void distributeEvidence(JunctionNode node, JunctionNode caller) {
		JunctionSeparator separator;
		ArrayList<BruseTable> pot = node.getClique().getPotentials();
		ArrayList<BruseTable> msg = new ArrayList<BruseTable>();;
		
		// if this is not the root then absorb the outer msg of separator with the caller
		if (caller != null) {
			separator = node.getSeparator(caller);
			node.getClique().addPotentials(separator.getOuterMsgList());
		}
		
		for (int i=0; i < node.getNeighbors().size(); i++) {
			JunctionNode neighbor = (JunctionNode)node.getNeighbors().get(i);
			
			if (neighbor != caller) {			
				separator = node.getSeparator(neighbor);
				
				if (m_sEvidence.size() > 0 && caller == null) {
					BruseTable table = node.getClique().getTable().getMarginal(getVarNames(separator.getVariables()));
					// This is the Big Clique must divide message by each potential in neighboring separator
					// this avoids passing information receive back to neighbor
					for (int j=0; j < separator.getInnerMsgList().size(); j++) {
						table = table.divideBy(separator.getInnerMsgList().get(j));
					}
					
					// create new message
					msg.clear();
					// add table to message
					msg.add(table);
				}
				else {
					// For each potential associated with node, find potentials that are relevant to separator
					// Relevant potentials are not d-separated or unit potentials
					msg = findRelPots( removeReceivedPots(pot, separator.getInnerMsgList()), 
										separator.getVariables() );
				}
				// set the outer message
				separator.setOuterMsg(msg);
				
				// distribute neighbors evidence
				distributeEvidence(neighbor, node);
			}
		}
	}
	
	//TODO this needs rewriting - needs to take advantage of separators
	protected BruseTable calculateMarginal(BruseNode node) {
		ArrayList<JunctionSeparator> separators = m_junctionTree.getSeparators();
		ArrayList<BruseTable> potentials = null, relPots = null;
		int minDom = Integer.MAX_VALUE;
		
		/*
		for (int i = 0; i < separators.size(); i++) {
			JunctionSeparator separator = separators.get(i);
			
			if (separator.getVariables().contains(node)) {
				if (potentials == null) {
					minDom = separator.getVariables().size();
					potentials = separator.getOuterMsgList();
					potentials.addAll(separator.getInnerMsgList());
				}
				else if (separator.getVariables().size() < minDom){
					minDom = separator.getVariables().size();
					potentials = separator.getOuterMsgList();
					potentials.addAll(separator.getInnerMsgList());
				}
			}
			
			// if the tables domain is size 1 then break since there is no smaller domain
			if (minDom == 1) break;
		}*/
		
		for (BruseEvidence ev: m_hEvidence) {
			if (node.getName().equalsIgnoreCase(ev.getNodeName())) {
				// hard evidence node do not need to be calculated
				node.updateStates(ev.getTable(m_network));
				return ev.getTable(m_network);
			}
		}
		
		if (potentials == null) {
			for (int i=0; i < m_cliques.size(); i++) {
				Clique clique = m_cliques.get(i);
				
				if (clique.getMembers().contains(node)) {				
					if (potentials == null) {
						minDom = clique.getMembers().size();
						potentials = clique.getPotentials();
						
					}
					else if (clique.getMembers().size() < minDom){
						minDom = clique.getMembers().size();
						potentials = clique.getPotentials();
					}
				}
			}
		}
		
		// find the relevant potentials
		relPots = findRelPots(potentials, node);
		
//		//TEST
//		ArrayList<BruseNode> nodes = new ArrayList<BruseNode>();
//		nodes.add(node);
//		relPots = VariableElimination.query(potentials, nodes);
		
		BruseTable result = relPots.get(0);
		
		// combine the potentials
		for (int i = 1; i < relPots.size(); i++) {
			BruseTable pot = relPots.get(i);
			result = result.multiplyBy(pot);
		}
		
		// normalize the marginal table
		result.normalize();
		
		// update the variables states
		node.updateStates(result);
		
		return result;
	}
}


