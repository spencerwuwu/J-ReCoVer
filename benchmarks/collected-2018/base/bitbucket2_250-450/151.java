// https://searchcode.com/api/result/92142811/

package com.infoagro.chat.cliente;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.infoagro.chat.GCM.GcmConnection;
import com.infoagro.chat.utils.HttpClient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.Bitmap.CompressFormat;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class ConfiguracionPrevia extends ActionBarActivity {

	public static final String PROPERTY_INFO = "infoPersonal";

	private EditText textNombre;
	private Button btnGuardar;
	private String nombre, imagen;
	private Date fechaNacimiento;
	private ImageView imgUsuario;
	private RadioButton rb1, rb2;

	private int sexo, id_usuario, orientacion;
	
	private Bitmap imagenActual;
	private String file_name;

	private RadioGroup rg1;
	private DatePicker datePicker;

	private SharedPreferences prefs;

	public static Activity activity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.configuracion_previa);

		SharedPreferences settings = this.getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
		settings.edit().remove("CONVERSACION_ACTUAL").commit();

		textNombre = (EditText) findViewById(R.id.etUsername);

		btnGuardar = (Button) findViewById(R.id.bSave);
		btnGuardar.setOnClickListener(configuracionOnClickListener);

		imgUsuario = (ImageView) findViewById(R.id.ivUserImage);
		imgUsuario.setOnClickListener(configuracionOnClickListener);

		rg1 = (RadioGroup) findViewById(R.id.gruporb);

		datePicker = (DatePicker) findViewById(R.id.datePicker1);

		Calendar calendar = Calendar.getInstance();
		int target = calendar.get(Calendar.YEAR);
		calendar.set(target-14, 11, 31);
		
		//datePicker.setCalendarViewShown(false);
		//datePicker.setMaxDate(calendar.getTimeInMillis());
		datePicker.init(target-18, 0, 1, null);

		TextView tx1 = (TextView)findViewById(R.id.tvUsername);
		EditText etx1 = (EditText)findViewById(R.id.etUsername);
		TextView tx3 = (TextView)findViewById(R.id.tvSexo);
		rb1 = (RadioButton)findViewById(R.id.radio1);
		rb2 = (RadioButton)findViewById(R.id.radio2);
		TextView tx4 = (TextView)findViewById(R.id.tvFecha);


		Typeface custom_font = Typeface.createFromAsset(getAssets(), "fonts/Handgley Regular.otf");
		tx1.setTypeface(custom_font);
		etx1.setTypeface(custom_font);

		tx3.setTypeface(custom_font);
		rb1.setTypeface(custom_font);
		rb2.setTypeface(custom_font);

		tx4.setTypeface(custom_font);

		btnGuardar.setTypeface(custom_font);

		String tituloConfiguracion = (getResources().getString(R.string.action_settings)).toUpperCase();
		SpannableString s = new SpannableString(tituloConfiguracion);
		s.setSpan(new com.infoagro.chat.utils.TypefaceSpan(this, "Handgley Regular.otf"), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		setTitle(s);

		getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		getSupportActionBar().setHomeButtonEnabled(false);


		prefs = this.getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
	}

	@Override
	public void onBackPressed() {
		nombre = textNombre.getText().toString();
		if(nombre.compareTo("")!=0 && rg1.getCheckedRadioButtonId()!=-1){
			super.onBackPressed();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if(MainActivity.EMAIL!=null)
			getMenuInflater().inflate(R.menu.main, menu);
		else
			getMenuInflater().inflate(R.menu.sesion, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
		case R.id.search:
			return true;
		case R.id.lastChats:
			return true;
		case R.id.action_settings:
			return true;
		case R.id.help: 
			return true;
		case R.id.session: 
			intent = new Intent(getBaseContext(),SesionActivity.class);
			startActivity(intent);
			return true;
		case android.R.id.home:
			finish();
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private OnClickListener configuracionOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			if(arg0.getId()==R.id.bSave){
				if(isOnline()){
					if(actualizarInformacion()){
						if(imagen != "" && imagen != null) {					
							SendHttpRequestTask t = new SendHttpRequestTask();

							String[] params = new String[]{MainActivity.DIRECCION + "Imagen", file_name};
							t.execute(params);
						}
						finish();
					}
				}
				else{
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.noConexion), Toast.LENGTH_SHORT).show();
				}
			}
			else if(arg0.getId()==R.id.ivUserImage){
				Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
				photoPickerIntent.setType("image/*");
				startActivityForResult(photoPickerIntent, 1);
			}
		}
	};

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 1)
			if (resultCode == Activity.RESULT_OK) {
				Uri selectedImage = data.getData();

				String filePath = getPath(selectedImage);
				String file_extn = filePath.substring(filePath.lastIndexOf(".")+1);

				if (file_extn.equals("img") || file_extn.equals("jpg") || file_extn.equals("jpeg") || file_extn.equals("gif") || file_extn.equals("png")) {
					imagen = filePath;
//					Bitmap a = BitmapFactory.decodeFile(imagen);
//					Bitmap b = ThumbnailUtils.extractThumbnail(a, 400, 400);
//					imagenActual = b;
					
					Bitmap a = decodeFile(new File(imagen));
					Bitmap b = ThumbnailUtils.extractThumbnail(a, 400, 400);
					imagenActual = b;
					imgUsuario.setImageBitmap(imagenActual);
					file_name = filePath.substring(filePath.lastIndexOf("/")+1);
				}
				else {
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.invalidPic), Toast.LENGTH_SHORT).show();
					imagen = "";
					file_name = "";
				}
			}
	}
	
	//decodes image and scales it to reduce memory consumption
	private Bitmap decodeFile(File f){
	    try {
	        //Decode image size
	        BitmapFactory.Options o = new BitmapFactory.Options();
	        o.inJustDecodeBounds = true;
	        BitmapFactory.decodeStream(new FileInputStream(f),null,o);

	        //The new size we want to scale to
	        final int REQUIRED_SIZE=400;

	        //Find the correct scale value. It should be the power of 2.
	        int scale=1;
	        while(o.outWidth/scale/2>=REQUIRED_SIZE && o.outHeight/scale/2>=REQUIRED_SIZE)
	            scale*=2;

	        //Decode with inSampleSize
	        BitmapFactory.Options o2 = new BitmapFactory.Options();
	        o2.inSampleSize=scale;
	        return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
	    } catch (FileNotFoundException e) {}
	    return null;
	}

	public String getPath(Uri uri) {
		Cursor cursor = getContentResolver().query(uri, new String[] { android.provider.MediaStore.Images.ImageColumns.DATA }, null, null, null);
		cursor.moveToFirst();
		final String imageFilePath = cursor.getString(0);
		return imageFilePath;

	}
	
	private boolean actualizarInformacion(){
		nombre = textNombre.getText().toString();

		if(rg1.getCheckedRadioButtonId()!=-1){
			int id= rg1.getCheckedRadioButtonId();            
			View radioButton = rg1.findViewById(id);
			int radioId = rg1.indexOfChild(radioButton);
			sexo = radioId+1;
		}

		fechaNacimiento = new Date(datePicker.getYear() - 1900, datePicker.getMonth(), datePicker.getDayOfMonth());
		String nombreEncode = "";
		try {
			nombreEncode = URLEncoder.encode(nombre, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		if(nombre.compareTo("")!=0 || rg1.getCheckedRadioButtonId()!=-1){
			GcmConnection gcmConnection = new GcmConnection();
			gcmConnection.setActividadOrigen(activity);
			gcmConnection.setId_usuario(id_usuario);
			gcmConnection.setMac(MainActivity.MAC);
			gcmConnection.setSexo(sexo);
			if(nombreEncode != "") gcmConnection.setNombre(nombreEncode);
			else gcmConnection.setNombre(nombre);

			gcmConnection.setImagen(imagen);

			gcmConnection.setOrientacion(orientacion);
			gcmConnection.setFechaNacimiento(fechaNacimiento);

			if (gcmConnection.checkPlayServices()) {
				GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
				gcmConnection.setGcm(gcm);
				gcmConnection.getRegistrationId(getApplicationContext());

				gcmConnection.registerInBackground();
				guardarEnSharedPreferences();
			} else {
				Log.i("GCM Demo", "No valid Google Play Services APK found.");
			}
			return true;
		}
		else {
			Toast toast1 = Toast.makeText(getApplicationContext(), R.string.obligatory, Toast.LENGTH_SHORT);
			toast1.show();
			return false;
		}
	}

	public void guardarEnSharedPreferences() {

		JSONObject jsonResponse;
		String infoPersonal = prefs.getString(PROPERTY_INFO, "");
		String gcm="";
		try {
			if(infoPersonal.length()>0 && infoPersonal.contains("gcm"))	{
				jsonResponse = new JSONObject(prefs.getString(PROPERTY_INFO, null));
				gcm = jsonResponse.getString("gcm");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		JSONObject info = new JSONObject();
		String imgCodificada = "";
		try {
			if(imagen!="" && imagen != null) imgCodificada = URLEncoder.encode(imagen,"UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		try {
			info.put("id_usuario", id_usuario);
			info.put("mac", MainActivity.MAC);
			info.put("nombre", nombre);
			info.put("sexo", sexo);
			info.put("imagen", imgCodificada);
			info.put("gcm", gcm);
			info.put("orientacion", orientacion);
			info.put("fechaNacimiento", fechaNacimiento);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PROPERTY_INFO, info.toString());
		editor.commit();

	}

	public static int[] formatearFecha(String string) {
		int[] resultado = new int[3];
		String[] tmp = string.split(" ");

		String mes = tmp[1];
		if(mes.compareTo("Jan")==0) resultado[1] = 0;
		if(mes.compareTo("Feb")==0) resultado[1] = 1;
		if(mes.compareTo("Mar")==0) resultado[1] = 2;
		if(mes.compareTo("Apr")==0) resultado[1] = 3;
		if(mes.compareTo("May")==0) resultado[1] = 4;
		if(mes.compareTo("Jun")==0) resultado[1] = 5;
		if(mes.compareTo("Jul")==0) resultado[1] = 6;
		if(mes.compareTo("Aug")==0) resultado[1] = 7;
		if(mes.compareTo("Sep")==0) resultado[1] = 8;
		if(mes.compareTo("Oct")==0) resultado[1] = 9;
		if(mes.compareTo("Nov")==0) resultado[1] = 10;
		if(mes.compareTo("Dec")==0) resultado[1] = 11;

		resultado[0] = Integer.parseInt(tmp[2]);
		resultado[2] = Integer.parseInt(tmp[tmp.length-1]);
		return resultado;
	}


	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}

	private class SendHttpRequestTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... params) {
			String url = params[0];
			Bitmap bitmap = BitmapFactory.decodeFile(imagen);

			Bitmap b = ThumbnailUtils.extractThumbnail(bitmap, 400, 400);
			if(b!=null){
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				b.compress(CompressFormat.PNG, 0, baos);

				try {
					HttpClient client = new HttpClient(url);
					client.connectForMultipart();
					client.addFilePart("file", id_usuario+".png", baos.toByteArray());
					client.finishMultipart();
					client.getResponse();
				}
				catch(Throwable t) {
					t.printStackTrace();
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(String data) {		
		}
	}
}

