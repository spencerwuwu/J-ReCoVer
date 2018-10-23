// https://searchcode.com/api/result/71485484/

import java.awt.Rectangle;
import java.awt.geom.Line2D;


public class TextArea {

	private String inputMarker = "Type a command, press <Enter> when finished:";
	private String parseInfo = "";
	private String userInput = "";
	
	private Boolean gettingInput = true;
	
	private int workAreaH = Const.wheight - Const.textArea;
	private int workAreaW = Const.wwidth;
	
	private Game session;
	
	
	//Vocabulary
	//Nouns
	private String[] nounList = { "tree", "bush","plant", "oak", "brush", "green", "greens", "fountain", "water","sprinkler", "sprayer"};//note this is a manual update
	private String[] treeList= { "tree", "bush","plant", "oak", "brush", "green", "greens"};
	private String[] fountainList= { "fountain", "water","sprinkler", "sprayer"};
	private String[] catList= { "cat", "statue","furball", "animal", "kitty"};
	private String[] lightList= { "light", "lamp","torch", "pole", "bulb"};
	private String[] buildingList= { "building", "structure","outhouse", "bathroom", "facilities", "wc", "house"};
	
	//other
	private String[] stopList= { "stop", "halt","freeze", "until"};
	private String[] verbList= { "go", "walk","move","run","travel", "turn", "touch", "find" };
	private String[] moveCondList= { "dont", "around","avoid","away" }; //iffy on travel and turn
	private String[] posCondList= { "past", "in", "above", "below", "after", "beyond", "before", "top", "bottom" };
	private String[] fluffList= { "the", "a", "to", "toward", "from", "of", "ahead", "and", "side", "by" };
	private String[] nounDirList= { "left", "infront", "front", "right", "forward", "forwards", "back", "backwards", "backward", "behind" };
	private String[] mapDirList= { "up", "down", "north", "south", "east", "west", "northeast", "northwest", "southeast", "southwest" };
	

	private String[][] vocabList = {verbList, moveCondList, posCondList, fluffList, nounDirList, mapDirList};
	
	
	//constructor get a reference to session.
	public TextArea(Game environment){
		session = environment;
		
		
		
	}
	
	//add a character to the current input string
	public void addToInput(char nextc){
		if(gettingInput){
			userInput = userInput + nextc;
		}
	}
	
	//delete the last char of the input string
	public void delChar(){
		if(userInput.length() > 0) {userInput = userInput.substring(0, userInput.length() - 1);}
	}
	
	//enter button was pressed, either parse string or clear the parse info from pane
	public void enterButton(){
		if(gettingInput){
			if(userInput.contentEquals("reset")){
				resetInput();
			}
			else if(userInput.contentEquals("barrelroll") || userInput.contentEquals("barelroll")|| userInput.contentEquals("barellroll")|| userInput.contentEquals("barrellroll")){
				barrelRoll();
			}
			else if(userInput.split(" ").length <= 2){
				parseHalt();
			}
			else{
				parseString();
			}
			gettingInput = false;
			userInput = "";
		}
		else{
			gettingInput = true;
		}
	}
	

	//resets the board.
	public void resetInput(){
		//need to fill this out later.
		session.randomize();
	}
	
	
	//Attempts a barrel roll
	public void barrelRoll(){
		parseInfo = "Do a barrel roll!";
		
		int x = session.entities.get(0).cx;
		int y = session.entities.get(0).cy;
		int d = 4;
		
		((User)session.entities.get(0)).dCount = 0;//remove checkpoints.
		
		//go up
		y -= d;
		((User)session.entities.get(0)).addDestination(x, y);
		//go up left
		y -= d;
		x -= d;
		((User)session.entities.get(0)).addDestination(x, y);
		//go left
		x -= d;
		((User)session.entities.get(0)).addDestination(x, y);
		//go down left
		y += d;
		x -= d;
		((User)session.entities.get(0)).addDestination(x, y);
		//go down 
		y += d;
		((User)session.entities.get(0)).addDestination(x, y);
		//go down right
		y += d;
		x += d;
		((User)session.entities.get(0)).addDestination(x, y);
		//go  right
		x += d;
		((User)session.entities.get(0)).addDestination(x, y);
		//go up right
		y -= d;
		x += d;
		((User)session.entities.get(0)).addDestination(x, y);
		//go up
		y -= d;
		((User)session.entities.get(0)).addDestination(x, y);
		//go up left
		y -= d;
		x -= d;
		((User)session.entities.get(0)).addDestination(x, y);
		//go left
		x -= d;
		((User)session.entities.get(0)).addDestination(x, y);
		//go down left
		y += d;
		x -= d;
		((User)session.entities.get(0)).addDestination(x, y);
		//go down 
		y += d;
		((User)session.entities.get(0)).addDestination(x, y);
		//go down right
		y += d;
		x += d;
		((User)session.entities.get(0)).addDestination(x, y);
		//go  right
		x += d;
		((User)session.entities.get(0)).addDestination(x, y);
		//go up right
		y -= d;
		x += d;
		((User)session.entities.get(0)).addDestination(x, y);
		//go up
		y -= d;
		((User)session.entities.get(0)).addDestination(x, y);
		
		
	}//end barrelRoll
	
	
	//parses a halt command
	//on halt a user will stop at current location and empty out all checkpoints.
	public void parseHalt(){

		if(userInput.contains("halt")){
			parseInfo = "Keyword: [halt]";
			((User) session.entities.get(0)).dCount = 0;
		}
		else if(userInput.contains("stop")){
			parseInfo = "Keyword: [halt]";
			((User) session.entities.get(0)).dCount = 0;
		}
		else if(userInput.contains("freeze")){
			parseInfo = "Keyword: [halt]";
			((User) session.entities.get(0)).dCount = 0;
		}
		else{
			String[] words = userInput.split(" ");
			boolean hasVerb = false;
			boolean hasDirection = false;
			if(words.length ==2){
					if(isType(verbList, words[0])){
						parseInfo = "[verb][direction]";
						if(isType(nounDirList, words[1])){
							//((User)session.entities.get(0)).addDestination(x, y)
							//need method to express go in direction until hit wall.
							//IMPLEMENT: see if the move in dir until method works.
							//private String[] nounDirList= { "left", "right", "forward","forwards", "back", "backwards", "backward" };
							int[] xy = new int[2];
							if(words[1].contentEquals("forward") || words[1].contentEquals("forwards") ){
								((User)session.entities.get(0)).getFaceMod(xy, 0);
							}
							else if(words[1].contentEquals("left")){
								((User)session.entities.get(0)).getFaceMod(xy, 3);//3 is left
							}
							else if(words[1].contentEquals("right") ){
								((User)session.entities.get(0)).getFaceMod(xy, 1); //1 is left
							}
							else{
								((User)session.entities.get(0)).getFaceMod(xy, 2); //2 is backwards(reverse)
							}
							this.setMoveInDirUntil(xy[0], xy[1], 9999, 9999); //keep moving in one direciton , screen will time out move.
						}
						else if(isType(mapDirList, words[1])){
							//private String[] mapDirList= { "up", "down", "north", "south", "east", "west" };
							int xmod, ymod;
							xmod = ymod = 0;
							if(words[1].contains("south") || words[1].contains("down") ){
								ymod = 1;
							}
							else if(words[1].contains("north") || words[1].contains("up") ){
								ymod = -1;
							}
							
							if(words[1].contains("east") ){
								xmod = 1;
							}
							else if(words[1].contains("west") ){
								xmod = -1;
							}
							this.setMoveInDirUntil(xmod, ymod, 9999, 9999);
						}//end else map dir word
						else{
							parseInfo = "Invalid structure. Missing Noun";
						}
					}//end first word is a verb
					else{
						parseInfo = "Invalid structure. Missing Verb";
					}
			}//end if length == 2
			else{
				parseInfo = "Unknown Keywords: " + userInput + ". Possible incomplete sentence.";
			}
		}
		
	}//end parse halt
	
	//parse the input string
	public void parseString(){

		String[] commands = userInput.split(" ");
		String[] cparse = new String[commands.length];
		int verb = 0;
		
		//commands = detectCommands(); //discontinued due to way it works.
		
		
		//for each word, detect what type of word it is, then associate to a restriction.
		for(int i = 0; i < commands.length; i++){
			if(isType(stopList, commands[i])) cparse[i] = "stop";
			else if(isType(verbList, commands[i])){ cparse[i] = "verb"; verb++; }
			else if(isType(moveCondList, commands[i])) cparse[i] = "mv cnd";
			else if(isType(posCondList, commands[i])) cparse[i] = "pos cnd";
			else if(isType(fluffList, commands[i])) cparse[i] = "";
			else if(isType(nounDirList, commands[i])) cparse[i] = "n dir";
			else if(isType(mapDirList, commands[i])) cparse[i] = "m dir";
			else if(isType(treeList, commands[i])) cparse[i] = "tree n";
			else if(isType(fountainList, commands[i])) cparse[i] = "fountain n";
			else if(isType(catList, commands[i])) cparse[i] = "cat n";
			else if(isType(lightList, commands[i])) cparse[i] = "light n";
			else if(isType(buildingList, commands[i])) cparse[i] = "building n";
			else cparse[i] = "unknown";
		}//end for each word
		
		
		//get my counts
		int donts = 0;
		int stops = 0;
		int verbs = 0;
		int directionOf = 0;
		int moveInDir = 0;
		int[] nouns = new int[20];
		int nounpos = 0;
		
		
		//strip down the in line duplicates.
		//example: go[v] walk[v] to the tree[n] => [v] [n]
		for(int i = 0; i < cparse.length; i++){
			
			//singleton checks
			if(cparse[i].contentEquals("mv cnd")){donts++;}
			else if(cparse[i].contentEquals("stops")){stops++;}
			else if(cparse[i].contentEquals("verb")){verbs++;}
			else if((nounpos < 20) &&
					( cparse[i].contentEquals("tree n") 
							|| cparse[i].contentEquals("fountain n")
							|| cparse[i].contentEquals("cat n")
							|| cparse[i].contentEquals("light n")
							|| cparse[i].contentEquals("building n"))){
					nouns[nounpos] = i;
					nounpos++;
			}
			//duplicates
			if(i>1){
				if(cparse[i].contentEquals("mv cnd")){

					if(i  < cparse.length -1  ){
						if(!cparse[i+1].contentEquals("mv cnd")){
							for(int j = i + 1; j < cparse.length;j++){
								if(cparse[j].contentEquals("tree n") 
										|| cparse[j].contentEquals("fountain n")
										|| cparse[j].contentEquals("cat n")
										|| cparse[j].contentEquals("light n")
										|| cparse[j].contentEquals("building n") ){
									cparse[j] = "mv cnd loc";
								}
							}
						}
					}
				}//end if there was a mv cmd
				
				
				if(cparse[i].contentEquals("verb") && cparse[i -1].contentEquals("verb") ) {
					cparse[i-1] = "";
					verbs--;
				}
				else if(cparse[i].contentEquals("stop") && cparse[i -1].contentEquals("stop") ) {
					cparse[i-1] = "";
					stops--;
				}
				else if(cparse[i].contentEquals("mv cnd") && cparse[i -1].contentEquals("mv cnd") ) {
					cparse[i - 1] = "";
					donts--;
				}
				else if(cparse[i].contentEquals("verb") && cparse[i -1].contentEquals("mv cnd") ) {
					cparse[i] = "";
					verbs--;
				}
				else if(	(cparse[i].contentEquals("of") && cparse[i -1].contentEquals("n dir"))
						|| 	(cparse[i].contentEquals("of") && cparse[i -1].contentEquals("m dir")) ) {
					directionOf++;
					moveInDir--;
				}
				else if( cparse[i].contentEquals("n dir") || cparse[i].contentEquals("n dir")){
					moveInDir++;
				}
			}
		}//end duplicate reduction
		

		parseInfo = "";
		
		//actual parse now
		if(verbs > 0){
			if(nounpos > 0){
				
				int prevnoun = 0;
				boolean match = false;
				//for each of the found nouns attempt a parse.
				for(int j = 0; j < nounpos; j++){
					match = false;
					//first noun. get position of that noun reduce by 1.
					//so long as it doesn't go before the previous noun or start of sentence, body code
					for(int i = nouns[j] -1 ; i >= prevnoun; i--){
						
						//figure out which type of noun this is.
						int nounType = 0; //0:user, 1: 
						if(isType(treeList, commands[nouns[j]])) nounType = Const.aTree;
						if(isType(fountainList, commands[nouns[j]])) nounType = Const.aFountain;
						if(isType(catList, commands[nouns[j]])) nounType = Const.aCat;
						if(isType(lightList, commands[nouns[j]])) nounType = Const.aLight;
						if(isType(buildingList, commands[nouns[j]])) nounType = Const.aBuilding;
						//more types
						//get position of that noun.
						int destNounX = session.entities.get(nounType).cx;
						int destNounY = session.entities.get(nounType).cy;
						
						if(cparse[i].contentEquals("verb")){
							
							
							//if there are donts. look left then right, stay within previous noun(or beginning of sentence) and before next verb
							if(donts > 0){

								this.findMvCnd(cparse, commands, nouns[j], prevnoun, session.entities.get(0).cx, session.entities.get(0).cy, destNounX, destNounY);
								
								donts--;
							}
							//move to the noun without restrictions
							((User)session.entities.get(0)).addDestination(destNounX, destNounY);
							
							match = true;
							i = prevnoun -1;//out of current noun conditions detection
							
							if(Const.debug){
								System.out.println("Verb only parse");
								System.out.println("User x,y: " + ((User)session.entities.get(0)).cx + "," + ((User)session.entities.get(0)).cy);
								System.out.println("Tree x,y: " + session.entities.get(1).cx + "," + session.entities.get(1).cy);
								
							}
							
						}//found a verb meaning: [verb] [noun]
						else if(cparse[i].contentEquals("pos cnd") 
								|| (cparse[i-1].contentEquals("n dir") && (commands[i].contentEquals("of")	|| commands[i].contentEquals("side")))
								|| (cparse[i-1].contentEquals("m dir") && (commands[i].contentEquals("of")	|| commands[i].contentEquals("side")))	){
							//found an ending position
							//modify dest based upon position type
							//private String[] posCondList= { "past", "touch","in", "above", "below", "after", "beyond", "before" };
							//private String[] nounDirList= { "left", "infront", "front", "right", "forward", "forwards", "back", "backwards", "backward" };
							//private String[] mapDirList= { "up", "down", "north", "south", "east", "west", "northeast", "northwest", "southeast", "southwest" };
							
							//in will have no effect since that is the normal destination
							//first up is map position, as it is easiest. Note north, south, east, west, marked by CONTAINS because it might be northeast or similar
							//north
							if(	commands[i-1].contentEquals("up")	||commands[i-1].contains("north")	||commands[i].contentEquals("above") ||commands[i-1].contentEquals("top")){destNounY -= (Const.objectH/2 + Const.userSize/2);}
							//south
							else if(	commands[i-1].contentEquals("down")	||commands[i-1].contains("south")	||commands[i-1].contentEquals("below") ||commands[i-1].contentEquals("bottom")	){destNounY += (Const.objectH/2 + Const.userSize/2);}
							//east
							if(	commands[i-1].contains("east")	){destNounX += (Const.objectW/2 + Const.userSize/2);}
							//west
							else if(	commands[i-1].contains("west")		){destNounX -= (Const.objectW/2 + Const.userSize/2);}
							
							//relational position based off of the user.
							//get it:
							int quad = this.getQuad(((User)session.entities.get(0)).cx, ((User)session.entities.get(0)).cy, destNounX, destNounY, true);
							
							
							if(commands[i-1].contentEquals("front")	||commands[i-1].contains("infront")	||commands[i].contains("before") ||commands[i-1].contains("forward")	||commands[i-1].contains("forwards"))
							{
								//if in row column linup
								if(quad == 1){destNounX += Const.objectW/2 + Const.userSize/2; destNounY -= Const.objectH/2 + Const.userSize/2;}
								else if(quad == 2){destNounX -= Const.objectW/2 + Const.userSize/2; destNounY -= Const.objectH/2 + Const.userSize/2;}
								else if(quad == 3){destNounX -= Const.objectW/2 + Const.userSize/2; destNounY += Const.objectH/2 + Const.userSize/2;}
								else if(quad == 4){destNounX += Const.objectW/2 + Const.userSize/2; destNounY += Const.objectH/2 + Const.userSize/2;}
								else if(quad == 5){destNounX += Const.objectW/2 + Const.userSize/2;}
								else if(quad == 6){destNounY -= Const.objectH/2 + Const.userSize/2;}
								else if(quad == 7){destNounX -= Const.objectW/2 + Const.userSize/2;}
								else if(quad == 8){destNounY += Const.objectH/2 + Const.userSize/2;}
							}
							else if(commands[i-1].contentEquals("back")	||commands[i-1].contains("backward")	||commands[i-1].contains("backwards") ||commands[i].contains("past")	||commands[i].contains("after")	||commands[i].contains("beyond") ||commands[i].contains("behind") )
							{
								//if in row column linup
								if(quad == 1){destNounX -= Const.objectW/2 + Const.userSize/2; destNounY += Const.objectH/2 + Const.userSize/2;}
								else if(quad == 2){destNounX += Const.objectW/2 + Const.userSize/2; destNounY += Const.objectH/2 + Const.userSize/2;}
								else if(quad == 3){destNounX += Const.objectW/2 + Const.userSize/2; destNounY -= Const.objectH/2 + Const.userSize/2;}
								else if(quad == 4){destNounX -= Const.objectW/2 + Const.userSize/2; destNounY -= Const.objectH/2 + Const.userSize/2;}
								else if(quad == 5){destNounX -= Const.objectW/2 + Const.userSize/2;}
								else if(quad == 6){destNounY += Const.objectH/2 + Const.userSize/2;}
								else if(quad == 7){destNounX += Const.objectW/2 + Const.userSize/2;}
								else if(quad == 8){destNounY -= Const.objectH/2 + Const.userSize/2;}
							}
							else if(commands[i-1].contentEquals("left"))
							{
								//if in row column linup
								if(quad == 1){destNounX += Const.objectW/2 + Const.userSize/2; destNounY += Const.objectH/2 + Const.userSize/2;}
								else if(quad == 2){destNounX += Const.objectW/2 + Const.userSize/2; destNounY -= Const.objectH/2 + Const.userSize/2;}
								else if(quad == 3){destNounX -= Const.objectW/2 + Const.userSize/2; destNounY -= Const.objectH/2 + Const.userSize/2;}
								else if(quad == 4){destNounX -= Const.objectW/2 + Const.userSize/2; destNounY += Const.objectH/2 + Const.userSize/2;}
								else if(quad == 5){destNounY += Const.objectH/2 + Const.userSize/2;}
								else if(quad == 6){destNounX += Const.objectW/2 + Const.userSize/2;}
								else if(quad == 7){destNounY -= Const.objectH/2 + Const.userSize/2;}
								else if(quad == 8){destNounX -= Const.objectW/2 + Const.userSize/2;}
							}
							else if(commands[i-1].contentEquals("right"))
							{
								//if in row column linup
								if(quad == 1){destNounX -= Const.objectW/2 + Const.userSize/2; destNounY -= Const.objectH/2 + Const.userSize/2;}
								else if(quad == 2){destNounX -= Const.objectW/2 + Const.userSize/2; destNounY += Const.objectH/2 + Const.userSize/2;}
								else if(quad == 3){destNounX += Const.objectW/2 + Const.userSize/2; destNounY += Const.objectH/2 + Const.userSize/2;}
								else if(quad == 4){destNounX += Const.objectW/2 + Const.userSize/2; destNounY -= Const.objectH/2 + Const.userSize/2;}
								else if(quad == 5){destNounY -= Const.objectH/2 + Const.userSize/2;}
								else if(quad == 6){destNounX -= Const.objectW/2 + Const.userSize/2;}
								else if(quad == 7){destNounY += Const.objectH/2 + Const.userSize/2;}
								else if(quad == 8){destNounX += Const.objectW/2 + Const.userSize/2;}
							}
							
							
							if(donts > 0){
								this.findMvCnd(cparse, commands, nouns[j], prevnoun, session.entities.get(0).cx, session.entities.get(0).cy, destNounX, destNounY);
								
								donts--;
							}
							

							((User)session.entities.get(0)).addDestination(destNounX, destNounY);
							
							directionOf--;
							match = true;
							i = prevnoun -1;//out of current noun conditions detection

							if(Const.debug){System.out.println("position parse");}
						}//found an ending position [verb] [end position] [noun]
						else if(cparse[i].contentEquals("n dir") 	|| cparse[i].contentEquals("m dir") 	){
							//found move in direction until noun
							int[] xyMod = new int[2];
							boolean nounBlocks = false;
							xyMod[0] = xyMod[1] = 0;
							
							//north
							if(	commands[i].contentEquals("up")	||commands[i].contains("north")	||commands[i].contentEquals("above")||commands[i].contentEquals("top")	){xyMod[1] = -1; }
							//south
							else if(	commands[i].contentEquals("down")	||commands[i].contains("south")	||commands[i].contentEquals("below")||commands[i].contentEquals("bottom")	){xyMod[1] = 1; }
							//east
							if(	commands[i].contains("east")	){xyMod[0] = 1;}
							//west
							else if(	commands[i].contains("west")		){xyMod[0] = -1;}
							

							if(commands[i].contentEquals("front")	||commands[i].contains("infront")	||commands[i].contains("before") ||commands[i].contains("forward")	||commands[i].contains("forwards"))
							{
								((User)session.entities.get(0)).getFaceMod(xyMod, 0);
							}
							else if(commands[i].contentEquals("back")	||commands[i].contains("backward")	||commands[i].contains("backwards") ||commands[i].contains("past")	||commands[i].contains("after")	||commands[i].contains("beyond"))
							{
								((User)session.entities.get(0)).getFaceMod(xyMod, 2);
							}
							else if(commands[i].contentEquals("left"))
							{
								((User)session.entities.get(0)).getFaceMod(xyMod, 3);
							}
							else if(commands[i].contentEquals("right"))
							{
								((User)session.entities.get(0)).getFaceMod(xyMod, 1);
							}
							
							int vertMove = 0;
							int horiMove = 0;
							
							//if user x is to left of noun x, it needs to go right
							//else it needs to go left
							if(			((User)session.entities.get(0)).cx < destNounX	){ horiMove = 1;}
							else { horiMove = -1;}
							//if user y is above noun y, it needs to go down
							//else it needs to go up.
							if(			((User)session.entities.get(0)).cy < destNounY	){ vertMove = 1;}
							else { vertMove = -1;}
							
							//if is not going to be stopped by the noun in either direction...
							//put the destination outside of the map
							((User) session.entities.get(0)).dCount = 0; //first clear out the check points because position matters.
							if ((horiMove != xyMod[0]) && (vertMove != xyMod[1]))
							{
								destNounX = session.entities.get(0).cx + (xyMod[0] * 2000);
								destNounY = session.entities.get(0).cy + (xyMod[1] * 2000);
							}
							
							
							if(donts > 0){
								
								this.findMvCnd(cparse, commands, nouns[j], prevnoun, session.entities.get(0).cx, session.entities.get(0).cy, destNounX, destNounY);
								
								donts--;
							}
							
							this.setMoveInDirUntil(xyMod[0], xyMod[1], destNounX, destNounY); //destination matters.. becauase it might not be going in that direction.
							
							match = true;
							i = prevnoun -1;//out of current noun conditions detection

							if(Const.debug){System.out.println("direction parse");}
						}//found an ending position [verb] [end position] [noun]
					}//end for i > previous noun && >=0
					
					if(!match){
						parseInfo = "Improper structure";
					}

					//immediately update prevnoun.
					prevnoun = nouns[j];
					
				
				}//end for j < nounpos
				
				
			}//there was a noun
			else{
				parseInfo = "No nouns detected";
			}
		}
		else{
			parseInfo = "No verbs detects. ";
		}
		
		for(int i = 0; i < cparse.length; i++){
			if(cparse[i].length() > 0) {parseInfo = parseInfo + "[" + cparse[i] + "]";}
		}
		parseInfo = parseInfo + " Press <Enter> to continue.";
	}
	
	/**
	 * Finds the position of the "mv cnd"
	 * 
	 * 
	 * @return
	 */
	public int findMvCnd(String[] parse, String[] orig, int start, int end, int ux, int uy, int dx, int dy){
		int retval = -1;
		int nval = -1;
		boolean foundMatch = false;
		int nounX = 0;
		int nounY = 0;
		
		//first search to the left
		for(int i = start; i > end; i--){
			if(parse[i].contentEquals("mv cnd")){retval = i; i = end;}
		}
		//if wasn't found
		if(retval == -1){
			//go right
			for(int i = start + 1; i < parse.length; i++){
				if(parse[i].contentEquals("mv cnd")){retval = i; i = parse.length;}
				else if(parse[i].contentEquals("verb") || parse[i].contains(" n")){retval = i; i = parse.length;}
			}
		}
		if(Const.debug){System.out.println("Inside mv cnd find\nRetval: " + retval);}
		//if mv cnd was found
		if(retval != -1){
			//need to find the associated verb
			for(int i = retval + 1; i < parse.length; i++){
				if(parse[i].contentEquals("mv cnd loc")){nval = i;}
			}

			if(Const.debug){System.out.println("nval: " + nval);}
			if(nval != -1){
				foundMatch = true;
				if(this.isType(treeList, orig[nval])){nounX = session.entities.get(Const.aTree).cx; nounY = session.entities.get(Const.aTree).cy;}
				else if(this.isType(fountainList, orig[nval])){nounX = session.entities.get(Const.aFountain).cx; nounY = session.entities.get(Const.aFountain).cy;}
				else if(this.isType(catList, orig[nval])){nounX = session.entities.get(Const.aCat).cx; nounY = session.entities.get(Const.aCat).cy;}
				else if(this.isType(lightList, orig[nval])){nounX = session.entities.get(Const.aLight).cx; nounY = session.entities.get(Const.aLight).cy;}
				else if(this.isType(buildingList, orig[nval])){nounX = session.entities.get(Const.aBuilding).cx; nounY = session.entities.get(Const.aBuilding).cy;}
				//if(this.isType(treeList, orig[nval])){nounX = session.entities.get(Const.aTree).cx; nounX = session.entities.get(Const.aTree).cy;}
				else{
					foundMatch = false;
				}
				
			}
			
			//if there was a match
			if(foundMatch){
				//create rectangles and lines representing path and obstacle
				Rectangle obstacle = new Rectangle(nounX - Const.objectW/2, nounY - Const.objectH/2, Const.objectW/2, Const.objectH/2);
				Line2D.Double leftLine = new Line2D.Double(ux - Const.userSize/2, uy, dx - Const.userSize/2, dy );
				Line2D.Double midLine = new Line2D.Double(ux, uy, dx, dy );
				Line2D.Double rightLine = new Line2D.Double(ux + Const.userSize/2, uy, dx + Const.userSize/2, dy );
				
				boolean lhit, mhit, rhit;
				//see if they intersect(meaning collision)
				if(leftLine.intersects(obstacle)){lhit = true;}
				else{lhit = false;}
				if(midLine.intersects(obstacle)){mhit = true;}
				else{mhit = false;}
				if(rightLine.intersects(obstacle)){rhit = true;}
				else{rhit = false;}
				
				//if there was a collision
				if(lhit || mhit || rhit){
					//first figure out where the user is in relation to the obstacle
					int quad = this.getQuad(ux, uy, dx, dy, false);
					
					//if quad == 1 
					if(quad == 1){
						//both hit. Go around either way
						if(!lhit && rhit)
						{ 
							((User)session.entities.get(0)).addDestination(nounX - Const.objectW/2 - Const.userSize/2, nounY - Const.objectH/2 - Const.userSize/2);
							((User)session.entities.get(0)).addDestination(nounX - Const.objectW/2 - Const.userSize/2, nounY + Const.objectH/2 + Const.userSize/2);
						}
						else
						{ 
							((User)session.entities.get(0)).addDestination(nounX + Const.objectW/2 + Const.userSize/2, nounY + Const.objectH/2 + Const.userSize/2);
							((User)session.entities.get(0)).addDestination(nounX - Const.objectW/2 - Const.userSize/2, nounY + Const.objectH/2 + Const.userSize/2);
						}
					}
					if(quad == 2){
						//both hit. Go around either way
						if(!lhit && rhit)
						{ 
							((User)session.entities.get(0)).addDestination(nounX + Const.objectW/2 + Const.userSize/2, nounY - Const.objectH/2 - Const.userSize/2);
							((User)session.entities.get(0)).addDestination(nounX + Const.objectW/2 + Const.userSize/2, nounY + Const.objectH/2 + Const.userSize/2);
						}
						else
						{ 
							((User)session.entities.get(0)).addDestination(nounX - Const.objectW/2 - Const.userSize/2, nounY + Const.objectH/2 + Const.userSize/2);
							((User)session.entities.get(0)).addDestination(nounX + Const.objectW/2 + Const.userSize/2, nounY + Const.objectH/2 + Const.userSize/2);
						}
					}
					if(quad == 3){
						//both hit. Go around either way
						if(!lhit && rhit)
						{ 
							((User)session.entities.get(0)).addDestination(nounX - Const.objectW/2 - Const.userSize/2, nounY - Const.objectH/2 - Const.userSize/2);
							((User)session.entities.get(0)).addDestination(nounX + Const.objectW/2 + Const.userSize/2, nounY - Const.objectH/2 - Const.userSize/2);
						}
						else
						{ 
							((User)session.entities.get(0)).addDestination(nounX + Const.objectW/2 + Const.userSize/2, nounY + Const.objectH/2 + Const.userSize/2);
							((User)session.entities.get(0)).addDestination(nounX + Const.objectW/2 + Const.userSize/2, nounY - Const.objectH/2 - Const.userSize/2);
						}
					}
					if(quad == 4){
						//both hit. Go around either way
						if(!lhit && rhit)
						{ 
							((User)session.entities.get(0)).addDestination(nounX - Const.objectW/2 - Const.userSize/2, nounY + Const.objectH/2 + Const.userSize/2);
							((User)session.entities.get(0)).addDestination(nounX - Const.objectW/2 - Const.userSize/2, nounY - Const.objectH/2 - Const.userSize/2);
						}
						else
						{ 
							((User)session.entities.get(0)).addDestination(nounX + Const.objectW/2 + Const.userSize/2, nounY - Const.objectH/2 - Const.userSize/2);
							((User)session.entities.get(0)).addDestination(nounX - Const.objectW/2 - Const.userSize/2, nounY - Const.objectH/2 - Const.userSize/2);
						}
					}
					
					
				}//end if there was a collision
				
			}//end if found match
			
		}//end if mv cnd was found


		if(!foundMatch && retval!= -1){parseInfo += "*Unpaired [mv cnd]*";}
		
		
		
		return retval;
	}//end findMvCnd
	
	
	/**
	 * 
	 *Four parameters for two sets of x,y.
	 *based upon positioning return quad.
	 *edit: added 4 additional quads to represent column/row linup.
	 *
	 * @param x1 reference object
	 * @param y1
	 * @param x2 origin object
	 * @param y2 
	 * @return
	 */

	public int getQuad(int x1, int y1, int x2, int y2, boolean zone8){
		int quad = 0;
		char overlap;
		if(zone8){overlap = isColRowOverlap(x1, y1, x2, y2);}
		else{overlap = ' ';}
		
		//if user x is greater than object x. user is right of object
		if(x1 > x2){
			if(overlap == 'x'){quad = 5;} //if right of and overlap
			else if(y1 > y2){
				if(overlap == 'y'){quad = 8;} //if right of and below
				else{quad = 4;}
			} //if right of and below
			else {
				if(overlap == 'y'){quad = 6;} //if right of and below
				else{quad = 1;}
			} //if right of and below
		}
		//else if x1 is to left of x2
		else {
			if(overlap == 'x'){quad = 7;} //if right of and overlap
			else if(y1 > y2){
				if(overlap == 'y'){quad = 8;} //if right of and below
				else{quad = 3;}
			} //if right of and below
			else {
				if(overlap == 'y'){quad = 6;} //if right of and below
				else{quad = 2;}
			} //if right of and below
		}
		
		
		return quad;
	}//end get quad
	
	/**
	 *Four parameters for two sets of x,y.
	 *will check if column or row position overlaps
	 *set 1 assumed to be user, set 2 assumed to be object.
	 *
	 *@ return 'x' 'y' and 'b'(both)
	 *
	*/
	public char isColRowOverlap(int x1, int y1, int x2, int y2){
		char overlap = ' ';

		int dx = Math.abs(x1 - x2);
		int dy = Math.abs(y1 - y2);
		
		if(dx < Const.userSize/2 + Const.objectW/2){ overlap = 'x'; }
		if(dy < Const.userSize/2 + Const.objectH/2){
			if(overlap == 'x'){overlap = 'b';}
			else{overlap = 'y';}
		}
		
		return overlap;
	}//end get quad
	
	//used to detect if a word is in a list
	//given list and word to check against
	public boolean isType(String[] list, String word){
		boolean retval = false;
		
		for(int i = 0; i < list.length; i++){
			if(word.contentEquals(list[i])){
				retval = true;
				break;
			}
		}
		return retval;
	}//end isType
	
	//detects commands.
	//current format:
	//count nouns. Will need further exclusion based off of conditional moves.
	public String[] detectCommands(){
		String[] retval;
		String[] words = userInput.split(" ");
		int[] commandPoints = new int[1];
		int verbs = 0;
		
		for(int i = 0; i < words.length; i++){
			for(int j = 0; j < verbList.length; j++){
				if(words[i].contentEquals(verbList[j])){
					verbs++;
					if(verbs > commandPoints.length){
						int[] tempA = new int[commandPoints.length * 2];
						for(int k = 0; k < commandPoints.length; k++){
							tempA[k] = commandPoints[k];
						}
						commandPoints = tempA;
					}//end if verbs
				}//end w
			}//end j
		}//end i
		
		
		if(commandPoints.length > 1){
			
		}
		//else{
		retval = new String[1];
		retval[1] = userInput;
		
		return retval;
	}//end detect commands.
	
	//counts the number of verbs in a sentence.
	public int countVerbs(String sentence){
		int retval = 0;

		return retval;
	}
	
	//counts the number of nouns in a sentence.
	public int countNouns(String sentence){
		int retval = 0;
		
		return retval;
	}
	
	
	//calculate direction move until parameter given or screen boundaries reached
	//natural limit at screen boundaries.
	public void setMoveInDirUntil(int xmod, int ymod, int xdest, int ydest){
		int ux, uy;
		boolean spotMatch = false;
		
		if(xmod == 0 && ymod == 0){
			if(Const.debug){
				System.out.println("Move cancelled due to mods being 0");
			}
			return; //can't move if there is no mod value.
		}
		
		ux = ((User)session.entities.get(0)).cx;
		uy = ((User)session.entities.get(0)).cy;
		
		while(!spotMatch){
			if(xdest * xmod == ux || ydest * ymod == uy){
				spotMatch = true;
			}
			else if(Const.wwidth < ux + Const.userSize/2){
				ux = Const.wwidth - 1 - Const.userSize/2;
				spotMatch =true;
			}
			else if(0 > ux - Const.userSize/2){
				ux = 1 + Const.userSize/2;
				spotMatch = true;
			}
			else if(Const.wheight < uy + Const.userSize/2){
				uy = Const.wheight - 1 - Const.userSize/2;
				spotMatch = true;
			}
			else if(0 > uy - Const.userSize/2){
				uy = 1 + Const.userSize/2;
				spotMatch = true;
			}
			else{
				ux += xmod;
				uy += ymod;
			}
		}//end while no spot match
		
		
		((User)session.entities.get(0)).addDestination(ux, uy);
		
	}//end set move in dir until
	
	//get string methods
	public String getIM(){ return inputMarker;}
	public String getPI(){ return parseInfo;}
	public String getUI(){ return userInput;}
	
	//get text start area
	public int getTextAHS(){return workAreaH;}
	//get state
	public boolean isPolling(){return gettingInput;}
	
	
}//end class

