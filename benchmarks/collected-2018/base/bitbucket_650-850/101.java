// https://searchcode.com/api/result/92142802/

package com.infoagro.chat.cliente;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.infoagro.chat.conexion.BorrarUsuario;
import com.infoagro.chat.conexion.ConsultarInformacion;
import com.infoagro.chat.conexion.EnviarEstado;
import com.infoagro.chat.conexion.EnviarInformacion;
import com.infoagro.chat.listadoUsuarios.ImageLoader;
import com.infoagro.chat.modelo.Usuario;
import com.infoagro.chat.utils.HttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class ConfiguracionActivity extends ActionBarActivity implements AsyncResponse{

	public static final String PROPERTY_INFO = "infoPersonal";

	private EditText textNombre, etOcupacion, etAficiones, etEstado;
	private Button btnCancelar, btnActualizar;
	private String nombre, mac, imagen, urlImagenPropia, ocupacion, aficiones, estado;
	private Date fechaNacimiento;
	private ImageView imgUsuario;
	private RadioButton rb1, rb2, rb3, rb4, rb5;
	private CheckBox cbBorrarCuenta;

	private int sexo, id_usuario, orientacion;
	private JSONObject info;

	private Bitmap imagenActual;
	private String file_name;

	private RadioGroup rg1, rg2;
	private DatePicker datePicker;

	private ConsultarInformacion task;

	private SharedPreferences prefs;

	private ProgressBar progressBar;

	private Activity activity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.configuracion);

		SharedPreferences settings = this.getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
		settings.edit().remove("CONVERSACION_ACTUAL").commit();

		progressBar = (ProgressBar)findViewById(R.id.progressBar);

		textNombre = (EditText) findViewById(R.id.etUsername);

		btnCancelar = (Button) findViewById(R.id.bCancel);
		btnCancelar.setText(getResources().getString(R.string.cancel).toUpperCase());
		btnCancelar.setOnClickListener(configuracionOnClickListener);

		btnActualizar = (Button) findViewById(R.id.bUpdate);
		btnActualizar.setOnClickListener(configuracionOnClickListener);

		imgUsuario = (ImageView) findViewById(R.id.ivUserImage);
		imgUsuario.setOnClickListener(configuracionOnClickListener);

		rg1 = (RadioGroup) findViewById(R.id.gruporb);
		rg2 = (RadioGroup) findViewById(R.id.gruporbOrientacion);

		cbBorrarCuenta = (CheckBox) findViewById(R.id.cbBorrarCuenta);
		cbBorrarCuenta.setOnCheckedChangeListener(cbBorrar);

		datePicker = (DatePicker) findViewById(R.id.datePicker1);
		Date minDate = new Date();
		minDate.setYear(minDate.getYear()-14);
		datePicker.setMaxDate(minDate.getTime());
		minDate.setYear(minDate.getYear()-18);
		datePicker.init(minDate.getYear()+1900, 0, 1, null);

		TextView tx1 = (TextView)findViewById(R.id.tvUsername);
		EditText etx1 = (EditText)findViewById(R.id.etUsername);
		TextView tx3 = (TextView)findViewById(R.id.tvSexo);
		rb1 = (RadioButton)findViewById(R.id.radio1);
		rb2 = (RadioButton)findViewById(R.id.radio2);
		rb3 = (RadioButton)findViewById(R.id.radioOrientacion1);
		rb4 = (RadioButton)findViewById(R.id.radioOrientacion2);
		rb5 = (RadioButton)findViewById(R.id.radioOrientacion3);
		TextView tx4 = (TextView)findViewById(R.id.tvFecha);
		TextView tx5 = (TextView)findViewById(R.id.tvOrientacion);
		TextView tx6 = (TextView)findViewById(R.id.tvOcupacion);
		etOcupacion = (EditText)findViewById(R.id.etOcupacion);
		TextView tx7 = (TextView)findViewById(R.id.tvAficiones);
		etAficiones = (EditText)findViewById(R.id.etAficiones);
		TextView tx8 = (TextView)findViewById(R.id.tvEstado);
		etEstado = (EditText)findViewById(R.id.etEstado);


		Typeface custom_font = Typeface.createFromAsset(getAssets(), "fonts/Handgley Regular.otf");
		tx1.setTypeface(custom_font);
		etx1.setTypeface(custom_font);

		tx3.setTypeface(custom_font);
		rb1.setTypeface(custom_font);
		rb2.setTypeface(custom_font);

		tx5.setTypeface(custom_font);
		rb3.setTypeface(custom_font);
		rb4.setTypeface(custom_font);
		rb5.setTypeface(custom_font);

		tx4.setTypeface(custom_font);

		tx6.setTypeface(custom_font);
		etOcupacion.setTypeface(custom_font);

		tx7.setTypeface(custom_font);
		etAficiones.setTypeface(custom_font);

		tx8.setTypeface(custom_font);
		etEstado.setTypeface(custom_font);

		cbBorrarCuenta.setTypeface(custom_font);

		btnCancelar.setTypeface(custom_font);
		btnActualizar.setTypeface(custom_font);

		String tituloConfiguracion = (getResources().getString(R.string.action_settings)).toUpperCase();
		SpannableString s = new SpannableString(tituloConfiguracion);
		s.setSpan(new com.infoagro.chat.utils.TypefaceSpan(this, "Handgley Regular.otf"), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		setTitle(s);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		activity = this; 

		prefs = this.getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
		String infoPersonal = prefs.getString(PROPERTY_INFO, "");
		if (infoPersonal.length() != 0) {
			try {
				info = new JSONObject(infoPersonal);
				id_usuario = info.getInt("id_usuario");
				mac = info.getString("mac");
				nombre = info.getString("nombre");

			} catch (JSONException e) {
				consultarInformacionPropia();	
			}
		}
		consultarInformacionPropia();	
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
			intent = new Intent(getBaseContext(),MainActivity.class);
			startActivity(intent);
			return true;
		case R.id.lastChats:
			intent = new Intent(getBaseContext(),UltimosChatsActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_settings:
			return true;
		case R.id.help: 
			intent = new Intent(getBaseContext(),AyudaActivity.class);
			startActivity(intent);
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
			if(arg0.getId()==R.id.bCancel){
				finish();
			}

			else if(arg0.getId()==R.id.bUpdate){
				if(isOnline()){
					if(actualizarInformacion()){
						if(imagen != "" && imagen != null) {					
							SendHttpRequestTask t = new SendHttpRequestTask();

							String[] params = new String[]{MainActivity.DIRECCION + "Imagen", file_name};
							t.execute(params);

							SendHttpRequestTaskB t2 = new SendHttpRequestTaskB();
							t2.execute(params);
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

				if (file_extn.toLowerCase().equals("img") || file_extn.toLowerCase().equals("jpg") || file_extn.toLowerCase().equals("jpeg") || 
						file_extn.toLowerCase().equals("gif") || file_extn.toLowerCase().equals("png")) {
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

	private OnCheckedChangeListener cbBorrar = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(final CompoundButton arg0, boolean arg1) {
			if(arg1){
				new AlertDialog.Builder(activity)
				.setTitle(R.string.deleteAccount)
				.setMessage(R.string.areUsure)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) { 
						BorrarUsuario task = new BorrarUsuario();
						task.setActivity(activity);
						task.setMac(mac);
						task.execute(new String[] { MainActivity.DIRECCION + "Borrado" });		
						dialog.cancel();
					}
				})
				.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) { 
						arg0.setChecked(false);
						dialog.cancel();
					}
				})
				.setIcon(android.R.drawable.ic_dialog_alert)
				.show();
			}
		}
	};

	private boolean actualizarInformacion(){
		nombre = textNombre.getText().toString();

		if(nombre.compareTo("")!=0){

			if(rg1.getCheckedRadioButtonId()!=-1){
				int id= rg1.getCheckedRadioButtonId();            
				View radioButton = rg1.findViewById(id);
				int radioId = rg1.indexOfChild(radioButton);
				sexo = radioId+1;
			}

			if(rg2.getCheckedRadioButtonId()!=-1){
				int id= rg2.getCheckedRadioButtonId();            
				View radioButton2 = rg2.findViewById(id);
				int radio2Id = rg2.indexOfChild(radioButton2);
				orientacion = radio2Id;            
			}

			ocupacion = etOcupacion.getText().toString();
			aficiones = etAficiones.getText().toString();
			estado = etEstado.getText().toString();

			fechaNacimiento = new Date(datePicker.getYear() - 1900, datePicker.getMonth(), datePicker.getDayOfMonth());
			String nombreEncode = "", ocupacionEncode = "", aficionesEncode = "", estadoEncode = "";
			try {
				nombreEncode = URLEncoder.encode(nombre, "UTF-8");
				ocupacionEncode = URLEncoder.encode(ocupacion, "UTF-8");
				aficionesEncode = URLEncoder.encode(aficiones, "UTF-8");
				estadoEncode = URLEncoder.encode(estado, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			EnviarInformacion task = new EnviarInformacion();
			task.setActivity(this);
			task.setId_usuario(id_usuario);
			task.setSexo(sexo);
			if(nombreEncode != "") task.setNombre(nombreEncode);
			else task.setNombre(nombre);
			task.setImagen(imagen);
			task.setOrientacion(orientacion);
			task.setEstado(estadoEncode);
			task.setOcupacion(ocupacionEncode);
			task.setAficiones(aficionesEncode);
			task.setFechaNacimiento(fechaNacimiento);

			task.execute(new String[] { MainActivity.DIRECCION + "ConsultaInformacion" });
			return true;
		}
		else{
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.nameNeeded), Toast.LENGTH_SHORT).show();
			return false;
		}
	}


	@SuppressWarnings("unused")
	private void mostrarAvisoCambiarEstado(){
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle(R.string.changeState);
		alert.setMessage(R.string.previousState);

		final EditText input = new EditText(this);

		String a = prefs.getString(PROPERTY_INFO, null);
		String antiguoEstado = "";
		JSONObject jsonResponse = null;
		try {
			jsonResponse = new JSONObject(prefs.getString(PROPERTY_INFO, null));
			if(a.contains("estado") && jsonResponse.getString("estado").compareTo("null")!=0) antiguoEstado = jsonResponse.getString("estado");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		input.setText(antiguoEstado);
		alert.setView(input);

		alert.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				Editable value = input.getText();
				enviarEstado(value.toString());
			}
		});

		alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.cancel();
			}
		});

		alert.show();
	}

	private void enviarEstado(String estado){
		EnviarEstado task = new EnviarEstado();
		task.setActivity(this);
		task.setId_usuario(MainActivity.ID_USUARIO);
		task.setEstado(estado);

		task.execute(new String[] { MainActivity.DIRECCION + "ConsultaEstado" });
	}


	private void guardarEnSharedPreferences() {

		JSONObject jsonResponse;
		String infoPersonal = prefs.getString(PROPERTY_INFO, "");
		String gcm="";
		try {
			jsonResponse = new JSONObject(prefs.getString(PROPERTY_INFO, null));
			if(infoPersonal.contains("gcm")){
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
			info.put("email", MainActivity.EMAIL);
			info.put("nombre", nombre);
			info.put("sexo", sexo);
			info.put("imagen", imgCodificada);
			info.put("gcm", gcm);
			info.put("orientacion", orientacion);
			if(ocupacion!=null && ocupacion.compareTo("")!=0){
				String ocupacionCodificada = URLEncoder.encode(ocupacion, "UTF-8");
				info.put("ocupacion", ocupacionCodificada);
			}
			else info.put("ocupacion", "null");
			if(aficiones!=null && aficiones.compareTo("")!=0){
				String aficionesCodificadas = URLEncoder.encode(aficiones, "UTF-8");
				info.put("aficiones", aficionesCodificadas);
			}
			else info.put("aficiones", "null");
			if(estado!=null && estado.compareTo("")!=0){
				String estadoCodificado = URLEncoder.encode(estado, "UTF-8");
				info.put("estado", estadoCodificado);
			}
			else info.put("estado", "null");
			info.put("fechaNacimiento", fechaNacimiento);

		} catch (JSONException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PROPERTY_INFO, info.toString());
		editor.commit();

	}

	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}

	private void consultarInformacionPropia() {		
		task = new ConsultarInformacion();
		task.setMiID(id_usuario);
		task.setId_usuario(id_usuario);
		task.setMac(mac);
		task.setImagen(urlImagenPropia);
		task.setImgUsuario(imgUsuario);
		task.setSexo(sexo);
		task.setActivity(this);
		task.execute(new String[] { MainActivity.DIRECCION + "ConsultaInformacion" });	
		task.delegate = this;
	}

	public void processFinish(String output){
		Usuario usuarioCargado = new Gson().fromJson(output, Usuario.class);
		if(usuarioCargado!=null){


			nombre = usuarioCargado.getNombre();
			sexo = usuarioCargado.getSexo();
			aficiones = usuarioCargado.getAficiones();
			ocupacion = usuarioCargado.getOcupacion();
			orientacion = usuarioCargado.getOrientacion();
			imagen = usuarioCargado.getImagen();
			fechaNacimiento = usuarioCargado.getFechaNacimiento();
			estado = usuarioCargado.getEstado();


			String nombreDeco = "";
			try {
				nombreDeco = URLDecoder.decode(nombre, "UTF-8");
				nombreDeco = URLDecoder.decode(nombreDeco, "UTF-8");
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			textNombre.setText(nombreDeco);

			int idSexo = usuarioCargado.getSexo();
			if(idSexo==1)
				((RadioButton) rg1.getChildAt(0)).setChecked(true);
			else if(idSexo==2)
				((RadioButton) rg1.getChildAt(1)).setChecked(true);

			int orientacion = usuarioCargado.getOrientacion();			
			RadioButton btn1 = (RadioButton) rg2.getChildAt(0);
			RadioButton btn2 = (RadioButton) rg2.getChildAt(1);
			RadioButton btn3 = (RadioButton) rg2.getChildAt(2);

			if(orientacion==0)
				btn1.setChecked(true);
			else if(orientacion==1)
				btn2.setChecked(true);
			else if(orientacion==2)
				btn3.setChecked(true);

			String ocupacionDeco = "", aficionesDeco = "", estadoDeco = "";
			try {
				ocupacionDeco = URLDecoder.decode(ocupacion, "UTF-8");
				ocupacionDeco = URLDecoder.decode(ocupacionDeco, "UTF-8");
				aficionesDeco = URLDecoder.decode(aficiones, "UTF-8");
				aficionesDeco = URLDecoder.decode(aficionesDeco, "UTF-8");
				estadoDeco = URLDecoder.decode(estado, "UTF-8");
				estadoDeco = URLDecoder.decode(estadoDeco, "UTF-8");
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}

			if(usuarioCargado.getOcupacion().compareTo("null")!=0) etOcupacion.setText(ocupacionDeco, TextView.BufferType.EDITABLE);
			if(usuarioCargado.getAficiones().compareTo("null")!=0) etAficiones.setText(aficionesDeco, TextView.BufferType.EDITABLE);
			if(usuarioCargado.getEstado().compareTo("null")!=0) etEstado.setText(estadoDeco, TextView.BufferType.EDITABLE);


			Date fechaTemp = usuarioCargado.getFechaNacimiento();
			datePicker.updateDate(fechaTemp.getYear()+1900, fechaTemp.getMonth(), fechaTemp.getDate());

			ImageLoader imageLoader = new ImageLoader(getApplicationContext());
			Bitmap imagenActual = null;

			String imagenDeco = "";
			try {
				imagenDeco = URLDecoder.decode(imagen,"UTF-8");
				System.out.println(imagenDeco);
				if(imagen!=null && imagen.compareTo("")!=0)
					imagenActual = imageLoader.getBitmap(imagen);
				if(imagenActual!=null){
					imgUsuario.setImageBitmap(imagenActual);
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} 
		}

		progressBar.setVisibility(View.GONE);
		guardarEnSharedPreferences();
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

	private class SendHttpRequestTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... params) {
			String url = params[0];
			Bitmap bitmap = BitmapFactory.decodeFile(imagen);

			Bitmap b = ThumbnailUtils.extractThumbnail(bitmap, 200, 200);
			if(b!=null){
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				b.compress(CompressFormat.PNG, 0, baos);

				try {
					HttpClient client = new HttpClient(url);
					client.connectForMultipart();
					client.addFilePart("file", id_usuario+".png", baos.toByteArray());
					client.finishMultipart();
					client.getResponse();
					ImageLoader imageLoader = new ImageLoader(getApplicationContext());
					imageLoader.clearCache();
				}
				catch(Throwable t) {
					t.printStackTrace();
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(String data) {			
			System.out.println("onPostExecute");
		}
	}

	private class SendHttpRequestTaskB extends AsyncTask<String, Void, String> {
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
					client.addFilePart("file", id_usuario+"b.png", baos.toByteArray());
					client.finishMultipart();
					client.getResponse();
					ImageLoader imageLoader = new ImageLoader(getApplicationContext());
					imageLoader.clearCache();
				}
				catch(Throwable t) {
					t.printStackTrace();
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(String data) {			
			System.out.println("onPostExecute");
		}
	}
}

