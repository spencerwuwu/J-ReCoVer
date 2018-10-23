// https://searchcode.com/api/result/70625066/

package ch.fhnw.emoba.madstorm;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import ch.fhnw.emoba.madstorm.controller.Controller;
import ch.fhnw.emoba.madstorm.controller.Controller.Position;
import ch.fhnw.emoba.madstorm.controller.ControllerListener;
import ch.fhnw.emoba.madstorm.controller.SensorController;
import ch.fhnw.emoba.madstorm.controller.TouchController;
import ch.fhnw.emoba.madstorm.robothandler.LogRobotHandler;
import ch.fhnw.emoba.madstorm.robothandler.NXTRobotHandler;
import ch.fhnw.emoba.madstorm.robothandler.RobotHandler;

public class ControlActivity extends Activity {

	public static final String ACTIVITY_NAME = ControlActivity.class.getSimpleName();
	
	private String address;
	private Controller controller;
	private ControlThread controlThread;
	private List<ControllerListener> controllerListeners = new ArrayList<ControllerListener>(2);
	private RobotHandler robot;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(getLayoutInflater().inflate(R.layout.control_view, null));

		address = getIntent().getExtras().getString("MAC");
		if(address == null) {
			Log.e(ACTIVITY_NAME, "No MAC address provided.");
			throw new RuntimeException("Intent has to provice MAC address as MAC in extras!");
		}
		
		Log.v(ACTIVITY_NAME, "Connecting to: " + address);
		robot = getRobotHandler();
		
		registerListeners();
		setupController();
		setupControllerListeners();
	}

	private RobotHandler getRobotHandler() {
		if(MainActivity.IS_EMULATED) {
			return new LogRobotHandler();
		} else {
			return new NXTRobotHandler(getApplicationContext(), address);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		
		controlThread = new ControlThread(controller, controllerListeners);
		controlThread.start();
	}
	
	@Override
	protected void onStop() {
		robot.close();
		controller.close();
		controlThread.shutdown();
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.control, menu);
		return true;
	}

	public void startShoot() {
		robot.startShoot();
	}

	public void stopShoot() {
		robot.stopShoot();
	}
	
	private void registerListeners() {
		((Button) findViewById(R.id.shootButton)).setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch ( event.getAction() ) {
			    case MotionEvent.ACTION_DOWN: startShoot();break;
			    case MotionEvent.ACTION_UP: stopShoot(); break;
			    }
				return true;
			}
		});
	}

	private void setupController() {
		if(MainActivity.IS_EMULATED) {
			controller = new TouchController(((SurfaceView) findViewById(R.id.controlSurface)));
		} else {
			controller = new SensorController((SensorManager) getSystemService(Context.SENSOR_SERVICE));
		}
	}
	
	private void setupControllerListeners() {
		controllerListeners.add(new SurfaceDrawer(((SurfaceView) findViewById(R.id.controlSurface)).getHolder()));
		controllerListeners.add(new RobotHandlerUpdater(robot));
	}
	
	private static final class RobotHandlerUpdater implements ControllerListener {

		private final RobotHandler handler;
		
		public RobotHandlerUpdater(RobotHandler handler) {
			this.handler = handler;
		}
		
		@Override
		public void update(Position position) {
			handler.setVelocity(position.x, position.y);
		}
		
	}
	
	private static final class SurfaceDrawer implements ControllerListener {
		private final SurfaceHolder holder;
		private final Paint green, black;
		
		public SurfaceDrawer(SurfaceHolder holder) {
			this.holder = holder;
			this.green = new Paint();
			green.setAntiAlias(false);
			green.setARGB(255, 0, 255, 0);
			this.black = new Paint();
			black.setAntiAlias(false);
			black.setARGB(255, 0, 0, 0);
		}
		
		@Override
		public void update(Position position) {
			Canvas c = null;
			try {
				c = holder.lockCanvas();
				if(c != null) draw(c, position);
			} finally {
				if (c != null) {
					holder.unlockCanvasAndPost(c);
				}
			}
		}
		
		private void draw(Canvas c, Position pos) {
			clearCanvas(c);
			drawCenter(c);
			drawPosition(c, pos);
		}

		private void clearCanvas(Canvas c) {
			c.drawARGB(255, 0, 0, 0);
		}
		
		private void drawCenter(Canvas c) {
			c.drawCircle(c.getWidth()/2, c.getHeight()/2, 25, green);
			c.drawCircle(c.getWidth()/2, c.getHeight()/2, 20, black);
		}
		
		private void drawPosition(Canvas c, Position pos) {
			c.drawCircle((int) -(pos.x*c.getWidth()/2) + c.getWidth()/2, -(int)(pos.y*c.getHeight()/2) + c.getHeight()/2, 10, green);
		}
	}
	
	private static final class ControlThread extends Thread {

		private static final int CONTROL_WAITTIME = 10;
		private static final String LOG_NAME = "Control thread";
		
		private final Controller controller;
		private final List<ControllerListener> listeners;

		private volatile boolean running = true;
		
		public ControlThread(Controller controller, List<ControllerListener> listeners) {
			this.controller = controller;
			this.listeners = listeners;
		}

		@Override
		public void run() {
			Log.v(LOG_NAME, "Control thread started");
			while (running) {
				try {
					Position pos = controller.getPosition();
					
					for(ControllerListener lis: listeners) {
						try {
							lis.update(pos);
						} catch (Exception ex) {
							Log.e(LOG_NAME, ex.toString());
						}
					}
					
					Thread.sleep(CONTROL_WAITTIME); // reduce CPU pressure
				} catch (InterruptedException ex) {
					shutdown();
				}
			}
			Log.v(LOG_NAME, "Control thread  stopped");
		}

		public void shutdown() {
			running = false;
		}
		
	}

}

