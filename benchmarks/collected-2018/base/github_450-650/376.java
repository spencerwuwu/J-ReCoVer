// https://searchcode.com/api/result/95822455/

package com.example.numbertest3;

import java.util.Locale;
import java.util.Random;
import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


/**
 * File name: 	MainActivity.java
 * Written by:	Christopher Dong
 * Date:		June 17, 2014
 * Purpose: 	People with auditory processing disorder have difficulty listening to the information 
 * 				and analyzing the sound. This application is designed to help people improve their
 * 				auditory processing skills. People can practice listening to information such as series
 * 				of number, phone number, amount of money or a calculation.
*/

public class MainActivity extends Activity {

	private Button btnReadNumber; 	// read number
	private Button btnNewNumber; 	// generate a number
	private Button btnCheckNumber; 	// check if input is correct
	
	private TextToSpeech tts; 		// create a text to speech object to read text
	
	private TextView txtNumber; 	// show if input is correct or not
	
	private EditText etNumber; 		// generate a number with this amount of digits 
	private EditText etEnterNumber;	// user inputs the answer
	
	private CheckBox speechRate;	// reduce the speech rate if check box is checked
	
	private Spinner spinner1;		// select a type of number. i.e. telephone, currency, time
	
	private Random randomNumber = new Random(); // use to generate a random number
	private Random randomBoolean = new Random(); // random true or false
	
	private int number = 10;			// the max number of digit is ten
	private int choiceIndex;			// store the spinner index number
	
	private String numberText = "";		// store the answer
	private String readCalculation;		// store the addition or subtraction 
	
	private boolean firstTime = true;	// disable some of the components on the GUI
	private boolean showNumber;			// show answer if show button is checked
	
	
	@Override 
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.number_test);
	
		btnReadNumber = (Button) findViewById(R.id.btnReadNumber);
		btnNewNumber = (Button) findViewById(R.id.btnNewNumber);
		btnCheckNumber = (Button) findViewById(R.id.btnCheckNumber);
		txtNumber = (TextView) findViewById(R.id.txtNumber);
		etNumber = (EditText)findViewById(R.id.etNumber);
		etEnterNumber = (EditText)findViewById(R.id.etEnterNumber);
		spinner1 = (Spinner) findViewById(R.id.spinner1);
		speechRate = (CheckBox) findViewById(R.id.checkBoxSpeech);
		
		// user needs to check new number button to begin
		btnReadNumber.setEnabled(false);
		btnCheckNumber.setEnabled(false);
		checkButton(false); // changes the background color and text on the buttons
		
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC); //control media sound

		tts = new TextToSpeech (this, new TextToSpeech.OnInitListener() {
		
			@Override
			public void onInit (int status) {
				if (status == TextToSpeech.SUCCESS) {
					tts.setLanguage(Locale.ENGLISH);
					
				    //Set Pitch, 1.0 is Normal    
				    //Lower values lower the tone of the synthesized voice, greater values increase it.
					tts.setPitch(1.0f);
				    
					//Set Speech Rate, 1.0 is Normal
				    //Lower values slow down the speech, greater values accelerate it 
					tts.setSpeechRate(1.0f);
					
					Intent installIntent = new Intent();
					installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				}
			}			
		});
	
		// press this button to read the hidden number
		btnReadNumber.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				readNumber();
			}
		});
		
		// select a type of number
		spinner1.setOnItemSelectedListener(new OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
					int position, long id) {
				
				int idx = spinner1.getSelectedItemPosition();
				
				// retrieve the amount of digit if number option is selected
		        if (idx == 0)
		        {
		        	etNumber.setEnabled(true);
		        }
		        else
		        {
		        	etNumber.setEnabled(false);
		        } 
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {		
			}
			
		});
		
		
		
		btnCheckNumber.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				String tempNumberText; // temporary store the number
				String checkNumber = etEnterNumber.getText().toString(); // user inputs an answer
				
				// commas and spaces are inserted to make speaking the number slower
				// remove commas and spaces before comparing answer to user input
				tempNumberText = numberText.replaceAll(",", "");
				tempNumberText = tempNumberText.replaceAll(" ", "");
				
				// user's input is correct
				if (checkNumber.equals(tempNumberText)) {
    				Toast.makeText(getApplicationContext(), checkNumber + " is correct",
 			               Toast.LENGTH_SHORT).show();
    				
    				txtNumber.setText(checkNumber + " is correct. \n Press \"New Number\" for next number");
    				etEnterNumber.setText("");
    				btnNewNumber.setText("New Number");
    				
    				btnCheckNumber.setEnabled(false);
    				checkButton(false);
    				showNumber = false;
				}
				else
				{
    				Toast.makeText(getApplicationContext(), checkNumber + " is incorrect",
 			               Toast.LENGTH_SHORT).show();
    				
    				txtNumber.setText(checkNumber + " is incorrect");
    				etEnterNumber.setText("");
				}
			}
		});
		
		
		// generate a new number
		btnNewNumber.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {

				
				if(showNumber == true)
				{
					// inserted commas cause tts engine to read the number slowly
					// remove commas when displaying the number
					txtNumber.setText(numberText.replaceAll(",", ""));
					
					// when user press show answer disable input box and check button
					// prompt user to press new number button
					etEnterNumber.setText("");
					btnCheckNumber.setEnabled(false);
					checkButton(false);
					btnNewNumber.setText("New Number");
					showNumber = false;
				}
				else
				{
					// user can enter a number or select a new number type
					showNumber = true;
					txtNumber.setText("Enter a number and press \"Check\"");
					btnNewNumber.setText("Show Answer");
					btnCheckNumber.setEnabled(true);
					checkButton(true);

					choiceIndex = spinner1.getSelectedItemPosition();
	
					switch (choiceIndex) {
			        case 0:
			        	numberText = numberOption();
			        	break;
			        case 1:
			        	numberText = moneyNumber();
			        	break;
			        case 2:
			        	numberText = phoneNumber();
			        	break;
			        case 3:
			        	numberText = timeNumber();
			        	break;
			        case 4:
			        	numberText = calculateNumber();
			        	break;
			        	
			        	// not using default
			        	default:
			        		Toast.makeText(MainActivity.this, "test", Toast.LENGTH_SHORT).show();
			        }

			        readNumber(); // read the number whenever a new number is generated
			        
			        if (firstTime == true)
			        {
			        	// enable the read number button when the first number is generated
				        btnReadNumber.setEnabled(true);

				        firstTime = false;
			        }
				}
			}
		});
	}

	// tts will speak the number using your phone tts setting
	// you can switch tts engine to different languages
	// on your phone go to Settings > Voice input and output > Text-to-Speech settings > Language
	public void textToSpeech(String numbers) {
		
		// tts engine cannot read the dash
		// commas creates a short pause when reading out the number
		numbers = numbers.replaceAll("-", ",");
 
		// cannot switch language within this application
		/*
		int idx = languageSpinner.getSelectedItemPosition();
	   
        switch (idx) {
        case 0:
        	tts.setLanguage(Locale.ENGLISH);
        	break;
        case 1:
        	tts.setLanguage(Locale.FRENCH);
        	break;
        case 2:
        	tts.setLanguage(Locale.ITALIAN);
        	break;
        case 3:
        	tts.setLanguage(Locale.GERMAN);
        	break;
        	default:
        		Toast.makeText(NumberActivity.this, "hello4", Toast.LENGTH_SHORT).show();
        }
        */
        tts.speak(numbers, TextToSpeech.QUEUE_FLUSH, null);
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	// create a set of numbers
	private String numberOption()
	{
		int newNumber = Integer.parseInt(etNumber.getText().toString());
		StringBuilder builder = new StringBuilder();
		
		// set a limit to the amount of digits
		System.out.println(newNumber);
		if (newNumber < 0 || newNumber > 10)
		{
			Toast.makeText(MainActivity.this, "Number must be between 1 and 10", Toast.LENGTH_SHORT).show();
			etNumber.setText(Integer.toString(number));
		}
		else
		{
			number = newNumber;
			
			int numberArray[] = new int[number];

			for (int index = 0; index < number; index++)
			{
				 numberArray[index] = randomNumber.nextInt(10);
				 builder.append(numberArray[index] + " ");
			}
		}
		return builder.toString();
	}
	
	// create a phone number
	private String phoneNumber()
	{
		int numberArray[] = new int[10];
		
		StringBuilder builder = new StringBuilder();
		
		int count = 1;
		for (int index = 0; index < 10; index++)
		{
			if (count == 3 || count == 6)
			{
				 numberArray[index] = randomNumber.nextInt(10);
				 builder.append(numberArray[index] + " - ");
			}
			else
			{
				 numberArray[index] = randomNumber.nextInt(10);
				 builder.append(numberArray[index] + " ");
			}
			 
			 count++;
		}
		
		return builder.toString();
	}
	
	
	// create a price from $0 to $999.99
	private String moneyNumber()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("$");
		builder.append(randomNumber.nextInt(999));
		builder.append(".");
		
		int cent = randomNumber.nextInt(99);
		
		if (cent >= 10)
			builder.append(cent);
		else
			builder.append("0" + cent);

		return builder.toString();
	}
	
	// create a time
	private String timeNumber()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append(randomNumber.nextInt(12) + 1);
		builder.append(":");
		
		int minute = randomNumber.nextInt(11);
		
		if (minute >= 10 && minute <= 59)
			builder.append(minute);
		else if (minute < 10)
			builder.append("0" + minute);
		else
			builder.append("00");
		
		if (randomNumber.nextBoolean() == true)
			builder.append(" AM");
		else
			builder.append(" PM");
		
		return builder.toString();
	}
	
	// add or subtract the two numbers
	private String calculateNumber()
	{
		StringBuilder builder = new StringBuilder();
		
		int firstNumber = randomNumber.nextInt(99) + 1;
		int secondNumber = randomNumber.nextInt(99) + 1;
		int total;
		boolean addOrSubtract = getRandomBoolean();
		
		

		if (addOrSubtract == true)
		{
			total = firstNumber + secondNumber;
			builder.append(firstNumber + ", + " + secondNumber);
			readCalculation = builder.toString();
			builder.append(" = " + total);
		}
		else
		{		
			if (firstNumber >= secondNumber)
			{
				total = firstNumber - secondNumber;
				builder.append(firstNumber + ", -  " + secondNumber);
				readCalculation = builder.toString();
				builder.append(" = " + total);
			}
			else
			{
				total = secondNumber - firstNumber;
				builder.append(secondNumber + ", - " + firstNumber);
				readCalculation = builder.toString();
				builder.append(" = " + total);
			}
		}
		return builder.toString();
	}
	
	// random boolean
	public boolean getRandomBoolean() {
		return randomBoolean.nextBoolean();
	}
	
	// read the number stored as a String variable
	private void readNumber()
	{
		String readNumberText;
		
		// the answer shown is read differently when calculation is chosen
		if (choiceIndex == 4)
		{
			readNumberText = readCalculation;
		}
		else
		{
			readNumberText = numberText;
		}
		
		if (speechRate.isChecked() == true)
		{
			readNumberText = readNumberText.replaceAll(" ", " , ");
		}
		
		if (choiceIndex == 4) // if choice index is calculation
		{
			readNumberText = readNumberText.replaceAll("-", "minus");
			textToSpeech(readNumberText);
		}
		else
		{
			textToSpeech(readNumberText);
		}
	}
	
	// when answer is shown or answer is correct, disable check answer button
	// change buttons style whenever the buttons are disable
	private void checkButton(boolean check) {

		if (check == false)
		{
			btnCheckNumber.setBackgroundResource(R.drawable.custom_btn_arsenic);
			btnCheckNumber.setTextAppearance(this, R.style.btnStyleArsenic);
			etEnterNumber.setEnabled(false);
		}
		
		else
		{
			btnCheckNumber.setBackgroundResource(R.drawable.custom_btn_breaker_bay);
			btnCheckNumber.setTextAppearance(this, R.style.btnStyleBreakerBay);
			etEnterNumber.setEnabled(true);
		}

	}
}

