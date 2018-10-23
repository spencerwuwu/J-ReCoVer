// https://searchcode.com/api/result/11450466/

package tests;

import tester.*;

@Example
class LightExample{
	LightExample(){}
	
	// test the method setTime in the class Light
	@TestMethod
	void testSetTime(Tester t){
		Light r = new Light("Red");
		r.setTime(5);
		t.checkExpect(r, new Light("Red", 5));
	}
	
	// test the method tick in the class Light
	@TestMethod
	void testTick(Tester t){
		Light r5 = new Light("Red", 5);
		
		/* New Style */
		t.checkEffect(r5, "tick", new Arguments(), new EffectField("time", 4));
		
		/* Old style */
		//r5.tick();
		t.checkExpect(r5, new Light("Red", 4));
		
	}
	
	// test the method off in the class Light
	@TestMethod
	void testOff(Tester t){
		Light r5 = new Light("Red", 5);
		Light r0 = new Light("Red", 0);
		t.checkExpect(r5.off(), false);
		t.checkExpect(r0.off(), true);
	}
	
	// test the method newCurrent in the class Traffic
	@TestMethod
	void testNewCurrent(Tester t){
		Traffic tr = new Traffic(5, 2, 7);
		 
		/*
		 *  We can't perform this test yet b/c this involves
		 */
		/* New Style!*/
		Light[] lightArray = {	new Light("Red", 5), 
								new Light("Yellow", 0), 
								new Light("Green", 7)};
		t.checkEffect(tr, "newCurrent", new Arguments(2), 
				new EffectField("current", lightArray[2]),
				new EffectField("currIndex", 2),
				new EffectField("lights", lightArray));
		
		/* Old style!*/
		tr.newCurrent(2);
		t.checkExpect(tr.currIndex, 2);
		t.checkExpect(tr.current, new Light("Green", 7));
	}
	
	// test the method tick in the class Traffic
	@TestMethod
	void testTickTraffic(Tester t){
		Traffic tr = new Traffic(5, 2, 7);
		
		/*New style, more exact*/
		t.checkEffect(tr, "tick", new Arguments(), 
				new EffectField("currIndex", 0),
				new EffectField("current", new Light("Red", 4)));
		
		/*Old style, very inaccurate*/
		t.checkExpect(tr.currIndex, 0);
		t.checkExpect(tr.current, new Light("Red", 4));
		
		tr.tick();
		tr.tick();
		tr.tick();
		
		/*New style*/
		/*
		 * The new style needs additional language features 
		 * to be able to handle complex cases such as this
		 */
		Light[] lightArray = {new Light("Red", 0), new Light("Yellow", 2), new Light("Green", 0)};
		t.checkEffect(tr, "tick", new Arguments(),
						new EffectField("current", new Light("Yellow", 2)),
						new EffectField("currIndex", 1),
						new EffectField("lights", lightArray));
		
		/*Old style*/
		tr.tick();
		t.checkExpect(tr.currIndex, 1);
		t.checkExpect(tr.current, new Light("Yellow", 1));
		
		tr.newCurrent(2);
		tr.tick();
		tr.tick();
		tr.tick();
		tr.tick();
		tr.tick();
		tr.tick();
		tr.tick();
		t.checkExpect(tr.currIndex, 0);
		t.checkExpect(tr.current, new Light("Red", 5));	
	}
	
	public static void main(String[] argv){
		LightExample e = new LightExample();
		Tester.runReport(e, true, true);
	}
}

class Light extends AbstractLight{

	Light(String color) {
		super(color);
	}
	
	Light(String color, int time){
		super(color, time);
	}
	 
}

class AbstractLight{
	String color;
	int time = 0;
	
	AbstractLight(String color){
		this.color = color;
	}
	
	AbstractLight(String color, int time){
		this.color = color;
		this.time = time;
	}
	
	
	void setTime(int time){
		this.time = time;
	}
	
	// reduce the time available by one
	void tick(){
		if (this.time > 0)
		 this.time = this.time - 1;
	}
	
	boolean off(){
		return this.time == 0;
	}
}

class Traffic{
	Light current;
	int currIndex;
	int[] times;
	Light[] lights = 
		new Light[]{new Light("Red"),
		            new Light("Yellow"),
		            new Light("Green")};
	
	Traffic(){
		current = null;
		currIndex = 0;
		times = null;
		lights = null;
	}
	
	Traffic(int red, int yellow, int green){
		this.times = new int[]{red, yellow, green};
		this.currIndex = 0;
		this.current = this.lights[this.currIndex]; 
		this.current.setTime(this.times[this.currIndex]);
	}
	
	void newCurrent(int index){
		this.currIndex = index;
		this.current = this.lights[this.currIndex]; 
		this.current.setTime(this.times[this.currIndex]);
	}
	
	void tick(){
		this.current.tick();
		if (this.current.off()){
			this.newCurrent((this.currIndex + 1) % 3);
		}
	}
}

