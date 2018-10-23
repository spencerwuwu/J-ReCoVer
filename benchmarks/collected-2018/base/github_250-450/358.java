// https://searchcode.com/api/result/93219775/

package importnew;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Test;

/*
 * Java8
 */
public class Java8Tutorial{
	static int outerStaticNum;
    int outerNum;

	//
	@Test
	public void test(){
		Formula formula = new Formula(){
			@Override
			public double calculate(int a){
				return sqrt(a*100);
			}
		};
		System.out.println(formula.calculate(100));
		System.out.println("default"+formula.sqrt(16));
	}

	/*
	 * ,lambda,,
	 */
	@Test
	public void test1(){
		Converter<String, Integer> converter = (from) -> Integer.valueOf(from);
		Integer converted = converter.convert("123");
		System.out.println(converted);
		
		//,,Java 8 ::
		Converter<String, Integer> converter2 = Integer::valueOf;
		Integer converted2 = converter2.convert("123");
		System.out.println(converted2);
	}
	
	//
	@Test
	public void test2(){
		Something something = new Something();
		Converter<String,String> converte = something::startsWith;
		String coverted = converte.convert("Java");
		System.out.println(coverted);
	}
	
	// createPerson
	@Test
	public void test7(){
		PersonFactory<Person> personFactory = Person::new;
		Person person = personFactory.create("Peter", "Parker");
		System.out.println(person);
	}
	
	/*
	 * Lambda
	 */
	//lambdafinal
	@Test
	public void test3(){
		final int i=1;
		Converter<Integer,Integer> converter = (from) -> from+i;
		System.out.println(converter.convert(2));
	}
	
	//ifinal,ifinal,i
	@Test
	public void test4(){
		int i=1;
		Converter<Integer,Integer> converter = (from) -> from+i;
		System.out.println(converter.convert(2));
		//wrong: i=3;
	}
	
	//defaultlambda:
	@Test
	public void test5(){
		//wrong: Formula formula = (a) -> sqrt(a*100);
	}
	
	//lambda
	@Test
	public void test6(){
		Converter<Integer,Integer> converter = (from) -> {outerStaticNum = 100;return from+outerStaticNum;};
		Converter<Integer,Integer> converter2 = (from) -> {outerNum = 200;return from+outerNum;};
		System.out.println(converter.convert(10));
		System.out.println(converter2.convert(20));
		outerStaticNum=500;
		outerNum=600;
		System.out.println(outerStaticNum+","+outerNum);
	}
	
	/*
	 * Streamsjava.util.Stream,.
	 * Stream,.
	 * ,,(StringBufferappend)
	 */
	@Test
	public void test8(){
		List<String> stringCollection = new ArrayList<>();
		stringCollection.add("ddd2");
		stringCollection.add("aaa2");
		stringCollection.add("bbb1");
		stringCollection.add("aaa1");
		stringCollection.add("bbb3");
		stringCollection.add("ccc");
		stringCollection.add("bbb2");
		stringCollection.add("ddd1");
		
		//FilterSorted
		stringCollection.stream().filter((s) -> s.startsWith("a")).sorted().forEach(System.out::println);
		Consumer<String> greeter = (p) -> System.out.print(p+",");
		stringCollection.stream().map(String::toUpperCase).sorted().forEach(greeter);
		
		//Match
		boolean anyStartsWithA = stringCollection.stream().anyMatch((s) -> s.startsWith("a"));
		System.out.println("anyStartsWithA is: "+anyStartsWithA);
			 
		boolean allStartsWithA = stringCollection.stream().allMatch((s) -> s.startsWith("a"));
		System.out.println("allMatchA is: "+allStartsWithA);
			 
		boolean noneStartsWithZ = stringCollection.stream().noneMatch((s) -> s.startsWith("z"));
		System.out.println("noneStartsWithZ is: "+noneStartsWithZ);
		
		//Count
		long startWithB = stringCollection.stream().filter((s)->s.startsWith("b")).count();
		System.out.println("startWithB is: "+startWithB);
		
		//Reduce ,,.Optional.
		Optional<String> reduced =stringCollection.stream().sorted().reduce((s1, s2) -> s1 + "#" + s2);
		reduced.ifPresent(System.out::println);
	}
	
	/*
	 * Parallel Streams ,.,.
	 */
	@Test
	public void test9(){
		int max = 1000000;
		List<String> values = new ArrayList<>(max);
		for (int i = 0; i < max; i++) {
		    UUID uuid = UUID.randomUUID();
		    values.add(uuid.toString());
		}
		
		calculateTime(values,false);
		calculateTime(values,true);
	}
	
	public void calculateTime(List<String> list, boolean isParallel){
		long t0 = System.nanoTime();
		long count;
		if(isParallel){
			count = list.parallelStream().sorted().count();
		}else{
			count = list.stream().sorted().count();
		}
		System.out.println(count);
		long t1 = System.nanoTime();
		long millis = TimeUnit.NANOSECONDS.toMillis(t1 - t0);
		System.out.println(String.format("Sort took: %d ms", millis));
	}
	
	@Test
	public void test10(){
		Map<Integer, String> map = new HashMap<>();
		for (int i = 0; i < 10; i++){
		    map.putIfAbsent(i, "val" + i);
		}
		map.forEach((id, val) -> System.out.println(val));
		
		//
		map.computeIfPresent(3, (num, val) -> val + num);
		System.out.println(map.get(3));
		
		map.computeIfPresent(9, (num, val) -> null);
		System.out.println(map.containsKey(9));
		
		//
		map.computeIfAbsent(23, num -> "val" + num);
		System.out.println(map);
		
		//3,
		map.computeIfAbsent(3, num -> "bam");
		System.out.println(map);
	}
	
	/*
	 * Clock
	 * Clock,System.currentTimeMillis()
	 * Instance,Instancejava.util.Date
	 */
	@Test
	public void test11(){
		Clock clock = Clock.systemDefaultZone();
		long millis = clock.millis();
		System.out.println("millis is: "+millis);
		Instant instant = clock.instant();
		Date legacyDate = Date.from(instant);
		System.out.println("legacyDate is: "+legacyDate);
	}
	
	@Test
	public void test12(){
		System.out.println(ZoneId.getAvailableZoneIds());
		// prints all available timezone ids
		ZoneId zone1 = ZoneId.of("Europe/Berlin");
		ZoneId zone2 = ZoneId.of("Brazil/East");
		System.out.println(zone1.getRules());
		System.out.println(zone2.getRules());
	}
	
	/*
	 *,:2014-03-11.,LocalTime
	 *,,
	 *
	 */
	@Test
	public void test13(){
		LocalDate today = LocalDate.now();
		LocalDate tomorrow = today.plusDays(1);
		LocalDate yesterday = tomorrow.minusDays(2);
		System.out.println("tomorrow is: "+tomorrow);
		System.out.println("yesterday is: "+yesterday);
		LocalDate independenceDay = LocalDate.of(2014, Month.JULY, 4);
		DayOfWeek dayOfWeek = independenceDay.getDayOfWeek();
		System.out.println(dayOfWeek);
	}
	
	/*
	 * LocalDateTime-,,
	 * LocalDateTime,LocalTimeLocalDate.
	 * 
	 */
	@Test
	public void test14(){
		LocalDateTime localDateTime = LocalDateTime.of(2014, Month.OCTOBER, 24, 23, 59, 59);
		DayOfWeek day = localDateTime.getDayOfWeek();
		System.out.println("Day of week: "+day);
		
		Month month = localDateTime.getMonth();
		System.out.println("Month: "+month);
		
		long minute = localDateTime.getLong(ChronoField.MINUTE_OF_DAY);
		System.out.println("Minute of Day: "+minute);
		
		//,LocalDateTimeInstance,Instancejava.util.Date
		Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
		Date legacyDate = Date.from(instant);
		System.out.println("Legacy Date: "+legacyDate);
		
		//Format
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy - HH:mm");
		LocalDateTime parsed = LocalDateTime.parse("Nov 03, 2014 - 07:13", formatter);
		String string = formatter.format(parsed);
		System.out.println("Format Date is: "+string);
	}
	
	
	//TODO something wrong with this
	@Test
	public void test15(){
		Hint hint0 = School.class.getAnnotation(Hint.class);
		System.out.println(hint0);
		
		Hints hints1 = School2.class.getAnnotation(Hints.class);
		System.out.println(hints1.value()[0].value());
		 
		Hint[] hints2 = School.class.getAnnotationsByType(Hint.class);
		System.out.println(hints2.length); 
		
		for(Hint hint:hints2){
			System.out.println(hint.value());
		}
		
	}
}

interface Formula {
    double calculate(int a);
    default double sqrt(int a) {
        return Math.sqrt(a);
    }
}

//
@FunctionalInterface
interface Converter<F, T> {
    T convert(F from);
}

class Something {
	String startsWith(String s){
		return String.valueOf(s.charAt(0));
	}
}

class Person {
    String firstName;
    String lastName;
    Person() {}
    Person(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }
    
    @Override
    public String toString(){
    	return "firstName: "+firstName+", "+"lastName: "+lastName;
    }
}

interface PersonFactory<P extends Person> {
    P create(String firstName, String lastName);
}

@Target( ElementType.TYPE )
@Retention( RetentionPolicy.RUNTIME )
@interface Hints {
    Hint[] value();
}

@Target( ElementType.TYPE )
@Retention( RetentionPolicy.RUNTIME )
@Repeatable(Hints.class)
@interface Hint {
    String value();
}

@Hints({@Hint("hint1"),@Hint("hint2")})
class School{}
//()
@Hint("hint1")
@Hint("hint2")
class School2{}
