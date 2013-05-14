package robots.ctrl.rover;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.dobots.utilities.Utils;

import robots.IVideoListener;
import robots.ctrl.DifferentialRobot;
import robots.ctrl.IMoveRepeaterListener;
import robots.ctrl.MoveRepeater;
import robots.ctrl.DifferentialRobot.DriveVelocityLR;
import robots.ctrl.MoveRepeater.MoveCommand;
import robots.ctrl.rover.RoverBaseTypes.RoverParameters;
import robots.ctrl.rover.RoverBaseTypes.VideoResolution;
import robots.gui.MessageTypes;
import android.os.Handler;

public abstract class RoverBase extends DifferentialRobot implements IMoveRepeaterListener {
	
	private static final String TAG = "RoverBase";

	protected RoverBaseController m_oController;

	protected VideoResolution m_eResolution = VideoResolution.res_unknown;

	protected Handler m_oUiHandler;

	protected ExecutorService executorSerive = Executors.newCachedThreadPool();

	protected Timer m_oKeepAliveTimer;

	protected double m_dblBaseSpeed = 50.0;

	protected MoveRepeater m_oRepeater;
	
	public RoverBase(double i_dblAxleWidth, int i_nMinVelocity, int i_nMaxVelocity, int i_nMinRadius, int i_nMaxRadius) {
		super(i_dblAxleWidth, i_nMinVelocity, i_nMaxVelocity, i_nMinRadius, i_nMaxRadius);

		m_oKeepAliveTimer = new Timer("KeepAliveTimer");
		m_oKeepAliveTimer.schedule(m_oKeepAliveTask, 60000, 60000);

		m_oRepeater = new MoveRepeater(this, 500);
	}

	private final TimerTask m_oKeepAliveTask = new TimerTask() {
		
		@Override
		public void run() {
			keepAlive();
		}
		
	};

	public void keepAlive() {
		if (m_oController.isConnected()) {
			m_oController.keepAlive();
		}
	}

	@Override
	public boolean toggleInvertDrive() {
		return false;
	}

	public abstract void toggleInfrared();

	public void setHandler(Handler i_oUiHandler) {
		m_oUiHandler = i_oUiHandler;
	}

	public abstract boolean isStreaming();
	public abstract boolean startVideo();
	public abstract void stopVideo();

	@Override
	public void destroy() {
		m_oKeepAliveTimer.cancel();
		disconnect();
	}

	@Override
	public void connect() {
		executorSerive.submit(new Runnable() {
			
			@Override
			public void run() {
				if (m_oController.connect()) {
					Utils.sendMessage(m_oUiHandler, MessageTypes.STATE_CONNECTED, null);
				} else {
					Utils.sendMessage(m_oUiHandler, MessageTypes.STATE_CONNECTERROR, null);
				}
			}
		});
	}

	@Override
	public void disconnect() {
		if (m_oController.isConnected()) {
			m_oController.disconnect();
			Utils.sendMessage(m_oUiHandler, MessageTypes.STATE_DISCONNECTED, null);
		}
	}

	@Override
	public boolean isConnected() {
		return m_oController.isConnected();
	}

	@Override
	public void enableControl(boolean i_bEnable) {
		// nothing to do
	}

	@Override
	public void moveForward(double i_dblSpeed) {
		m_oRepeater.startMove(MoveCommand.MOVE_FWD, i_dblSpeed, true);
	}

	@Override
	public void moveForward(double i_dblSpeed, int i_nRadius) {
		m_oRepeater.startMove(MoveCommand.MOVE_FWD, i_dblSpeed, i_nRadius, true);
	}

	@Override
	public void moveForward(double i_dblSpeed, double i_dblAngle) {

		debug(TAG, String.format("moveForward(%f, %f)", i_dblSpeed, i_dblAngle));
		
		if (Math.abs(i_dblAngle) < 10) {
			moveForward(i_dblSpeed);
		} else {
			int nRadius = angleToRadius(i_dblAngle);
			moveForward(i_dblSpeed, nRadius);
		}
		
	}

	@Override
	public void moveBackward(double i_dblSpeed) {
		m_oRepeater.startMove(MoveCommand.MOVE_BWD, i_dblSpeed, true);
	}

	@Override
	public void moveBackward(double i_dblSpeed, int i_nRadius) {
		m_oRepeater.startMove(MoveCommand.MOVE_BWD, i_dblSpeed, i_nRadius, true);
	}

	@Override
	public void moveBackward(double i_dblSpeed, double i_dblAngle) {

		debug(TAG, String.format("moveBackward(%f, %f)", i_dblSpeed, i_dblAngle));
		
		if (Math.abs(i_dblAngle) < 10) {
			moveBackward(i_dblSpeed);
		} else {
			int nRadius = angleToRadius(i_dblAngle);
			moveBackward(i_dblSpeed, nRadius);
		}
		
	}

	@Override
	public void rotateClockwise(double i_dblSpeed) {
		m_oRepeater.startMove(MoveCommand.ROTATE_RIGHT, i_dblSpeed, true);
	}

	@Override
	public void rotateCounterClockwise(double i_dblSpeed) {
		m_oRepeater.startMove(MoveCommand.ROTATE_LEFT, i_dblSpeed, true);
	}

	@Override
	public void moveStop() {
		m_oRepeater.stopMove();
		m_oController.moveStop();
	}

	
	// ------------------------------------------------------------------------------------------

	@Override
	public void onDoMove(MoveCommand i_eMove, double i_dblSpeed) {
		switch(i_eMove) {
		case MOVE_BWD:
			executeMoveBackward(i_dblSpeed);
			break;
		case MOVE_FWD:
			executeMoveForward(i_dblSpeed);
			break;
		case ROTATE_LEFT:
			executeRotateCounterClockwise(i_dblSpeed);
			break;
		case ROTATE_RIGHT:
			executeRotateClockwise(i_dblSpeed);
			break;
		default:
			error(TAG, "Move not available");
			return;
		}
	}

	private void executeMoveForward(double i_dblSpeed) {
		int nVelocity = calculateVelocity(i_dblSpeed);

		m_oController.moveForward(nVelocity);
	}
	
	private void executeMoveBackward(double i_dblSpeed) {
		int nVelocity = calculateVelocity(i_dblSpeed);

		m_oController.moveBackward(nVelocity);
	}
	
	private void executeRotateCounterClockwise(double i_dblSpeed) {
		int nVelocity = calculateVelocity(i_dblSpeed);
		
		m_oController.rotateLeft(nVelocity);
	}
	
	private void executeRotateClockwise(double i_dblSpeed) {
		int nVelocity = calculateVelocity(i_dblSpeed);
		
		m_oController.rotateRight(nVelocity);
	}

	@Override
	public void onDoMove(MoveCommand i_eMove, double i_dblSpeed, int i_nRadius) {
		switch(i_eMove) {
		case MOVE_BWD:
			executeMoveBackward(i_dblSpeed, i_nRadius);
			break;
		case MOVE_FWD:
			executeMoveForward(i_dblSpeed, i_nRadius);
			break;
		default:
			error(TAG, "Move not available");
			return;
		}
	}

	private void executeMoveForward(double i_dblSpeed, int i_nRadius) {
		DriveVelocityLR oVelocity = calculateVelocity(i_dblSpeed, i_nRadius);
		
		m_oController.moveForward(oVelocity.left, oVelocity.right);
	}

	private void executeMoveBackward(double i_dblSpeed, int i_nRadius) {
		DriveVelocityLR oVelocity = calculateVelocity(i_dblSpeed, i_nRadius);
		
		m_oController.moveBackward(oVelocity.left, oVelocity.right);
	}

	
	// ------------------------------------------------------------------------------------------

	@Override
	public void executeCircle(double i_dblTime, double i_dblSpeed) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setBaseSpeed(double i_dblSpeed) {
		m_dblBaseSpeed = i_dblSpeed;
	}

	@Override
	public double getBaseSped() {
		return m_dblBaseSpeed;
	}

	@Override
	public void moveForward() {
		moveForward(m_dblBaseSpeed);
	}

	@Override
	public void moveBackward() {
		moveBackward(m_dblBaseSpeed);
	}

	@Override
	public void rotateCounterClockwise() {
		rotateCounterClockwise(m_dblBaseSpeed);
	}

	@Override
	public void rotateClockwise() {
		rotateClockwise(m_dblBaseSpeed);
	}

	@Override
	public void moveLeft() {
		// not available
	}

	@Override
	public void moveRight() {
		// not available
	}

	public void setResolution(VideoResolution i_eResolution) {
		if (m_eResolution != i_eResolution) {
			switch(i_eResolution) {
			case res_320x240:
				m_oController.setResolution320x240();
				break;
			case res_640x480:
				m_oController.setResolution640x480();
				break;
			}
			m_eResolution = i_eResolution;
		}
	}
	
	public VideoResolution getResolution() {
		if (m_eResolution == VideoResolution.res_unknown) {
			// we don't know what the resolution is so we obtain it from the robot
			RoverParameters param = m_oController.getParameters();
			switch (Integer.valueOf(param.resolution)) {
			case 8:
				m_eResolution = VideoResolution.res_320x240;
				break;
			case 32:
				m_eResolution = VideoResolution.res_640x480;
				break;
			}
		}
		return m_eResolution;
	}
	
	public void setVideoListener(IVideoListener i_oListener) {
		m_oController.setVideoListener(i_oListener);
	}
	
	public void removeVideoListener(IVideoListener i_oListener) {
		m_oController.removeVideoListener(i_oListener);
	}

}