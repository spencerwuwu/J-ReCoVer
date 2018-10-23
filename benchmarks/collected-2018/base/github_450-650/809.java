// https://searchcode.com/api/result/114366278/

/*  
 *  Copyright 2007-2010 Lawrence Beadle & Tom Castle
 *  Licensed under GNU General Public License
 * 
 *  This file is part of Epoch X - (The Genetic Programming Analysis Software)
 *
 *  Epoch X is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Epoch X is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with Epoch X.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.epochx.semantics;

import java.util.ArrayList;
import java.util.List;

import org.epochx.core.Model;
import org.epochx.epox.*;
import org.epochx.epox.ant.*;
import org.epochx.gp.model.GPModel;
import org.epochx.gp.representation.GPCandidateProgram;
import org.epochx.representation.CandidateProgram;
import org.epochx.tools.ant.*;


/**
 * The ant semantic module controls the translation of code
 * to the ant behaviour and back again
 */
public class AntSemanticModule implements SemanticModule {
	
	//private List<TerminalNode<Action>> terminals;
	private Model model;
	private ArrayList<String> antModel;
	private String orientation;
	private Ant ant;
	private String environment;
	@SuppressWarnings("unused")
	private AntLandscape landscape;
	
	/**
	 * Constructor for Ant Semantic Module
	 * @param list List of terminal nodes
	 * @param model The GPModel object
	 * @param ant The Ant object
	 * @param antLandscape The AntLanscape object
	 */
	public AntSemanticModule(List<VoidNode> list, Model model, Ant ant, AntLandscape landscape, String environment) {
		//this.terminals = list;
		this.model = model;
		this.ant = ant;
		this.landscape = landscape;
		this.environment = environment;
	}
	
	/* (non-Javadoc)
	 * @see com.epochx.semantics.SemanticModule#behaviourToCode(com.epochx.semantics.Representation)
	 */
	@Override
	public CandidateProgram behaviourToCode(Representation representation) {
		Node rootNode = this.repToCode1(representation, "E");
		
		CandidateProgram result = null;
		if(environment.equalsIgnoreCase("GP")) {
			result = new GPCandidateProgram(rootNode, (GPModel) model);
		} else if(environment.equalsIgnoreCase("GE")) {
			// TODO GE Constructor
		} else if(environment.equalsIgnoreCase("GR")) {
			// TODO GR Constructor
		}
		
		return result;
	}

	/* (non-Javadoc)
	 * @see com.epochx.semantics.SemanticModule#codeToBehaviour(com.epochx.core.representation.CandidateProgram)
	 */
	@Override
	public Representation codeToBehaviour(CandidateProgram program) {
		// develop ant monitoring model
        antModel = new ArrayList<String>();

        // initialise a new ant
        orientation = "E";
        SemanticCandidateProgram prog = new SemanticCandidateProgram(program, ant);
        Node rootNode = prog.getRootNode();
        this.runAnt(rootNode);
        
        // work out depth of if statements
        int depth = 0;
        int maxDepth = 0;
        for(String s: antModel) {
            if(s.equalsIgnoreCase("{")) {
                depth++;
            }
            if(s.equalsIgnoreCase("}")) {
                depth--;
            }
            if(depth>maxDepth) {
                maxDepth = depth;
            }
        }
        
        for (int i = 0; i < (maxDepth + 2); i++) {
            if (antModel.size() > 0) {
                antModel = this.condenseAntRep(antModel);
            }
        }

        return new AntRepresentation(antModel);
	}
	
	private void runAnt(Node rootNode) {		
		
		if(rootNode instanceof VoidNode) {
			// terminals
			Object value = ((VoidNode) rootNode).evaluate();
			if(value instanceof AntMoveAction) {
				antModel.add("M");
			} else if(value instanceof AntTurnLeftAction) {
				if(orientation.equalsIgnoreCase("E")) {
					antModel.add("N");
					orientation = "N";
				} else if(orientation.equalsIgnoreCase("N")) {
					antModel.add("W");
					orientation = "W";
				} else if(orientation.equalsIgnoreCase("W")) {
					antModel.add("S");
					orientation = "S";
				} else if(orientation.equalsIgnoreCase("S")) {
					antModel.add("E");
					orientation = "E";
				}
			} else if(value instanceof AntTurnRightAction) {
				if(orientation.equalsIgnoreCase("E")) {
					antModel.add("S");
					orientation = "S";
				} else if(orientation.equalsIgnoreCase("S")) {
					antModel.add("W");
					orientation = "W";
				} else if(orientation.equalsIgnoreCase("W")) {
					antModel.add("N");
					orientation = "N";
				} else if(orientation.equalsIgnoreCase("N")) {
					antModel.add("E");
					orientation = "E";
				}
			}			
		} else if(rootNode instanceof Seq2Function) {
			// PROGN2
			int arity = rootNode.getArity();
			for(int i = 0; i<arity; i++) {
				this.runAnt(rootNode.getChild(i));
			}			
		} else if(rootNode instanceof Seq3Function) {
			// PROGN3
			int arity = rootNode.getArity();
			for(int i = 0; i<arity; i++) {
				this.runAnt(rootNode.getChild(i));
			}
		} else if(rootNode instanceof IfFoodAheadFunction) {
			// IF-FOOD-AHEAD
			int arity = rootNode.getArity();
			String oBeforeIf = orientation;
			for(int i = 0; i<arity; i++) {
				orientation = oBeforeIf;
				antModel.add("{");
				this.runAnt(rootNode.getChild(i));
				antModel.add("}");
			}
			// reset orientation for after if statement
			orientation = oBeforeIf;
		} 
    }
	
	/**
	 * The condense ant representation applies reduction rules to representation of 
	 * behaviour in order to return a canonical representation of behaviour
	 * @param result the representation of behaviour to condense
	 * @return the canonical behavioural representation
	 */
	public ArrayList<String> condenseAntRep(ArrayList<String> result) {
        
        // cycle through removing duplicate subsets
        // work out total depth                   
        int maxDepth = 0;
        int depth = 0;      
        
        // ---------------------------------------------------------------------
        for (String s : result) {
            if (s.equals("{")) {
                depth++;
            }
            if (s.equals("}")) {
                depth--;
            }
            if (depth > maxDepth) {
                maxDepth = depth;
            }
        }
        //condense brackets only if there are brackets i.e. maxDepth>0
        if (maxDepth > 0) {
            boolean reduce = true;
            while (reduce) {
                reduce = false;
                // cycle through and condense
                int masterDepth = 0;
                depth = 0;
                int[] tracker = new int[maxDepth + 1];
                // set all to zero
                for (int i = 0; i < tracker.length; i++) {
                    tracker[i] = 0;
                }
                ArrayList<String> subset1, subset2;
                for (int i = 0; i < result.size() - 1; i++) {
                    if (result.get(i).equalsIgnoreCase("{")) {
                        tracker[masterDepth]++;
                    }
                    if (tracker[masterDepth] % 2 == 1 && result.get(i).equalsIgnoreCase("{")) {
                        subset1 = new ArrayList<String>();
                        subset2 = new ArrayList<String>();
                        depth = 0;
                        int endPoint1 = 0;
                        int endPoint2 = 0;
                        for (int y = i; y < result.size(); y++) {
                            if (result.get(y).equalsIgnoreCase("{")) {
                                depth++;
                            }
                            if (result.get(y).equalsIgnoreCase("}")) {
                                depth--;
                            }
                            subset1.add(result.get(y));
                            if (depth == 0) {
                                endPoint1 = y;
                                break;
                            }
                        }
                        for (int y = endPoint1 + 1; y < result.size(); y++) {
                            if (result.get(y).equalsIgnoreCase("{")) {
                                depth++;
                            }
                            if (result.get(y).equalsIgnoreCase("}")) {
                                depth--;
                            }
                            subset2.add(result.get(y));
                            if (depth == 0) {
                                endPoint2 = y;
                                break;
                            }
                        }
                        // check if subsets equivalent
                        if (subset1.equals(subset2)) {                                                
                            
                            // pull up pre if code
                            ArrayList<String> preif = new ArrayList<String>();
                            // work out expected orientation before IF
                            String expectedO = "E";
                            if (i > 0) {
                                for (int k = 0; k < i; k++) {
                                    preif.add(result.get(k));
                                    if (result.get(k).equalsIgnoreCase("N") || result.get(k).equalsIgnoreCase("S") || result.get(k).equalsIgnoreCase("E") || result.get(k).equalsIgnoreCase("W")) {
                                        expectedO = result.get(k);
                                    }
                                }
                            }
                            
                            // add subset1 to pre if
                            subset1.remove(0);
                            subset1.remove(subset1.size()-1);
                            for(String s: subset1) {
                                preif.add(s);
                            }
                            
                            // get post if code if necessary
                            if(result.size()>endPoint2) {
                                // get post if code
                                ArrayList<String> postif = new ArrayList<String>();
                                for(int k = (endPoint2+1); k<result.size(); k++) {
                                    postif.add(result.get(k));
                                }
                                // add post if code to preif+subset1 - care with orientation
                                result = joinPaths(preif, postif, expectedO);                                
                            } else {
                                result = preif;
                            }
                            
                            reduce = true;
                            break;
                        }
                    }
                    // fix depth afterwards
                    if (result.get(i).equalsIgnoreCase("{")) {
                        masterDepth++;
                    }
                    if (result.get(i).equalsIgnoreCase("}")) {
                        masterDepth--;
                    }
                }
            }
        }
        // ---------------------------------------------------------------------
        
        // pull out orientation letters in sequence
        for (int i = 0; i < result.size() - 1; i++) {
            if (result.get(i).equalsIgnoreCase("N") || result.get(i).equalsIgnoreCase("W") || result.get(i).equalsIgnoreCase("S") || result.get(i).equalsIgnoreCase("E")) {
                if (result.get(i + 1).equalsIgnoreCase("N") || result.get(i + 1).equalsIgnoreCase("W") || result.get(i + 1).equalsIgnoreCase("S") || result.get(i + 1).equalsIgnoreCase("E")) {
                    result.remove(i);
                    i--;
                }
            }
        }
        
        ArrayList<String> controlStack = new ArrayList<String>();
        controlStack.add("E");
        depth = 0;
        for(int i = 0; i<result.size(); i++) {
            if(result.get(i).equalsIgnoreCase("{")) {
                // amend depth
                depth++;
                // move up previous depths orientation
                controlStack.add(controlStack.get(depth-1));
            } else if(result.get(i).equalsIgnoreCase("}")){
                // remove orientation from top of control stack
                controlStack.remove(depth);
                // amend depth
                depth--;
            } else if(result.get(i).equalsIgnoreCase("M")){
                // do nothing
            } else {
                if(result.get(i).equalsIgnoreCase(controlStack.get(depth))) {
                    // remove duplicate orientation
                    result.remove(i);
                    i--;
                } else {
                    controlStack.set(depth, result.get(i));
                }
            }            
        }
        
        return result;
    }
	
	private Node repToCode1(Representation thisRep, String lastO) {
        ArrayList<String> representation = ((AntRepresentation) thisRep).getAntRepresentation();
        ArrayList<VoidNode> sequence = new ArrayList<VoidNode>();

        // create a linear move list
        String oBeforeIf = "E";
        String lastOrientation = lastO;
        String instruction;
        for (int i = 0; i < representation.size(); i++) {
            instruction = representation.get(i);
            // SCENARIOS
            // interpret instruction
            if (instruction.equals("M")) {
                sequence.add(new AntMoveAction(ant));
            } else if (instruction.equals("E")) {
                if (lastOrientation.equalsIgnoreCase("N")) {
                    sequence.add(new AntTurnRightAction(ant));
                }
                if (lastOrientation.equalsIgnoreCase("W")) {
                    if (Math.random() < 0.5) {
                        sequence.add(new AntTurnRightAction(ant));
                        sequence.add(new AntTurnRightAction(ant));
                    } else {
                        sequence.add(new AntTurnLeftAction(ant));
                        sequence.add(new AntTurnLeftAction(ant));
                    }
                }
                if (lastOrientation.equalsIgnoreCase("S")) {
                    sequence.add(new AntTurnLeftAction(ant));
                }
                lastOrientation = new String(instruction);
            } else if (instruction.equals("S")) {
                if (lastOrientation.equalsIgnoreCase("E")) {
                    sequence.add(new AntTurnRightAction(ant));
                }
                if (lastOrientation.equalsIgnoreCase("N")) {
                    if (Math.random() < 0.5) {
                        sequence.add(new AntTurnRightAction(ant));
                        sequence.add(new AntTurnRightAction(ant));
                    } else {
                        sequence.add(new AntTurnLeftAction(ant));
                        sequence.add(new AntTurnLeftAction(ant));
                    }
                }
                if (lastOrientation.equalsIgnoreCase("W")) {
                    sequence.add(new AntTurnLeftAction(ant));
                }
                lastOrientation = new String(instruction);
            } else if (instruction.equals("W")) {
                if (lastOrientation.equalsIgnoreCase("S")) {
                    sequence.add(new AntTurnRightAction(ant));
                }
                if (lastOrientation.equalsIgnoreCase("E")) {
                    if (Math.random() < 0.5) {
                        sequence.add(new AntTurnRightAction(ant));
                        sequence.add(new AntTurnRightAction(ant));
                    } else {
                        sequence.add(new AntTurnLeftAction(ant));
                        sequence.add(new AntTurnLeftAction(ant));
                    }
                }
                if (lastOrientation.equalsIgnoreCase("N")) {
                    sequence.add(new AntTurnLeftAction(ant));
                }
                lastOrientation = new String(instruction);
            } else if (instruction.equals("N")) {
                if (lastOrientation.equalsIgnoreCase("W")) {
                    sequence.add(new AntTurnRightAction(ant));
                }
                if (lastOrientation.equalsIgnoreCase("S")) {
                    if (Math.random() < 0.5) {
                        sequence.add(new AntTurnRightAction(ant));
                        sequence.add(new AntTurnRightAction(ant));
                    } else {
                        sequence.add(new AntTurnLeftAction(ant));
                        sequence.add(new AntTurnLeftAction(ant));
                    }
                }
                if (lastOrientation.equalsIgnoreCase("E")) {
                    sequence.add(new AntTurnLeftAction(ant));
                }
                lastOrientation = new String(instruction);
            } else if (instruction.equalsIgnoreCase("{")) {
                // save entry position
                oBeforeIf = lastOrientation;
                // IF-FOOD-AHEAD recursive call
                int depth = 1;
                ArrayList<String> part = new ArrayList<String>();
                // pull out first section of if and submit to recursive call
                while (i < representation.size()) {
                    i++;
                    if (representation.get(i).equalsIgnoreCase("{")) {
                        depth++;
                    }
                    if (representation.get(i).equalsIgnoreCase("}")) {
                        depth--;
                    }
                    if (depth == 0) {
                        break;
                    }
                    part.add(representation.get(i));
                }
                // pull part back from recursive call
                VoidNode child1 = (VoidNode) this.repToCode1(new AntRepresentation(part), oBeforeIf);
                // add part to sequence if no part then add skip
                if (child1 == null) {
                    child1 = new AntSkipAction(ant);
                }
                // reset lastX and last Y to before if branch
                lastOrientation = oBeforeIf;
                // do second part of if
                i = i + 2;
                depth = 1;
                part = new ArrayList<String>();
                while (i < representation.size()) {
                    if (representation.get(i).equalsIgnoreCase("{")) {
                        depth++;
                    }
                    if (representation.get(i).equalsIgnoreCase("}")) {
                        depth--;
                    }
                    if (depth == 0) {
                        break;
                    }
                    part.add(representation.get(i));
                    i++;
                }
                // pull part back from recursive call
                VoidNode child2 = (VoidNode) this.repToCode1(new AntRepresentation(part), oBeforeIf);
                // add part to sequence if no part then add skip
                if (child2 == null) {
                    child2 = new AntSkipAction(ant);
                }
                // end close bracket
                VoidNode iFANode = new IfFoodAheadFunction(ant, child1, child2);
                sequence.add(iFANode);
                // move i along one to get out of final if bracket
                lastOrientation = oBeforeIf;
            } else if (instruction.equalsIgnoreCase("}")) {
                // do nothing                
            } else {
                System.out.println("REP TO CODE ERROR - GPEQUIVALENCE AA");
            }
        }
        
        // run reduce sequence once to add skip node if needed
        sequence = this.reduceSequence(sequence);
        // then run repeatedly if need be
        while(sequence.size()>1) {
        	sequence = this.reduceSequence(sequence);
        }
		// System.out.println(sequence);
		return sequence.get(0);
    }
	
	private ArrayList<VoidNode> reduceSequence(ArrayList<VoidNode> sequence) {
		int count = sequence.size();
		if (count == 0) {
			sequence.add(new AntSkipAction(ant));
		} else if (count == 1) {
			// do nothing is resolved ready to return
		} else if (count == 2) {
			// function up PROGN2
			VoidNode p1 = sequence.get(0);
			VoidNode p2 = sequence.get(1);
			sequence.set(0, new Seq2Function(p1, p2));
			sequence.remove(1);
		} else if (count == 3) {
			// function up PROGN3
			VoidNode p1 = sequence.get(0);
			VoidNode p2 = sequence.get(1);
			VoidNode p3 = sequence.get(2);
			sequence.set(0, new Seq3Function(p1, p2, p3));
			sequence.remove(2);
			sequence.remove(1);
		} else if (count > 3) {
			//crunch 1st three into progn3
			VoidNode p1 = sequence.get(0);
			VoidNode p2 = sequence.get(1);
			VoidNode p3 = sequence.get(2);
			sequence.set(0, new Seq3Function(p1, p2, p3));
			sequence.remove(2);
			sequence.remove(1);
		}
		// pass through and remove nulls
		return sequence;
	}
	
	/**
     * Helper method for Semantic Artificial Ant Initialisation
     * @param path1 The first path
     * @param path2 The second path
     * @param p2SO The initial direction of the second path if not E
     * @return The combined path with with the second path positions updated relative to the 1st path
     */
    public static ArrayList<String> joinPaths(ArrayList<String> path1, ArrayList<String> path2, String p2SO) {
        ArrayList<String> result = new ArrayList<String>();
        ArrayList<String> part1 = new ArrayList<String>();
        ArrayList<String> part2 = new ArrayList<String>();
        for (String p : path1) {
            part1.add(p);
        }
        for (String p : path2) {
            part2.add(p);
        }
        // pull off last direction
        String lastOrientation = "E";
        for(int i = (part1.size()-1); i>=0; i--) {
            if(part1.get(i).equalsIgnoreCase("N") || part1.get(i).equalsIgnoreCase("S") || part1.get(i).equalsIgnoreCase("E") || part1.get(i).equalsIgnoreCase("W")) {
                lastOrientation = part1.get(i);
            }
        }
        
        // update all orientations
        // work out turning
        if(lastOrientation.equalsIgnoreCase("N")) {
            if(p2SO.equalsIgnoreCase("S")) {
                part2 = turnL(part2);
                part2 = turnL(part2);
            } else if(p2SO.equalsIgnoreCase("E")) {
                part2 = turnL(part2);
                part2 = turnL(part2);
                part2 = turnL(part2);
            } else if(p2SO.equalsIgnoreCase("W")) {
                part2 = turnL(part2);                
            }
        } else if(lastOrientation.equalsIgnoreCase("S")) {
            if(p2SO.equalsIgnoreCase("N")) {
                part2 = turnL(part2);
                part2 = turnL(part2);
            } else if(p2SO.equalsIgnoreCase("E")) {
                part2 = turnL(part2);                
            } else if(p2SO.equalsIgnoreCase("W")) {
                part2 = turnL(part2); 
                part2 = turnL(part2);
                part2 = turnL(part2);
            }
        } else if(lastOrientation.equalsIgnoreCase("E")) {
            if(p2SO.equalsIgnoreCase("S")) {
                part2 = turnL(part2);
                part2 = turnL(part2);
                part2 = turnL(part2);
            } else if(p2SO.equalsIgnoreCase("N")) {
                part2 = turnL(part2);
            } else if(p2SO.equalsIgnoreCase("W")) {
                part2 = turnL(part2);
                part2 = turnL(part2);
            }
        } else if(lastOrientation.equalsIgnoreCase("W")) {
            if(p2SO.equalsIgnoreCase("S")) {
                part2 = turnL(part2);
            } else if(p2SO.equalsIgnoreCase("E")) {
                part2 = turnL(part2);
                part2 = turnL(part2);
            } else if(p2SO.equalsIgnoreCase("N")) {
                part2 = turnL(part2);
                part2 = turnL(part2);
                part2 = turnL(part2);
            }
        }
        
        // add all together
        for (String p : part1) {
            result.add(p);
        }
        for (String p : part2) {
            result.add(p);
        }
        part1 = null;
        part2 = null;
        return result;
    }
    
    private static ArrayList<String> turnL(ArrayList<String> toTurn) {
        for(int i = 0; i<toTurn.size(); i++) {
            if(toTurn.get(i).equalsIgnoreCase("N")) {
                toTurn.set(i, "W");
            } else if(toTurn.get(i).equalsIgnoreCase("W")) {
                toTurn.set(i, "S");
            } else if(toTurn.get(i).equalsIgnoreCase("S")) {
                toTurn.set(i, "E");
            } else if(toTurn.get(i).equalsIgnoreCase("E")) {
                toTurn.set(i, "N");
            }
        }
        return toTurn;
    }
}



