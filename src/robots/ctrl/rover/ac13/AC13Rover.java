package robots.ctrl.rover.ac13;

// Uses the AC13Communication Library created by Uceta
// http://sourceforge.net/projects/ac13javalibrary/

import robots.RobotType;
import robots.ctrl.rover.RoverBase;

public class AC13Rover extends RoverBase {
	
	private static final String TAG = "AC13Rover";
	
	public AC13Rover() {
		super(AC13RoverTypes.AXLE_WIDTH, AC13RoverTypes.MIN_SPEED, AC13RoverTypes.MAX_SPEED, AC13RoverTypes.MIN_RADIUS, AC13RoverTypes.MAX_RADIUS);
		
//		m_oController = new AC13Comunicator();
		m_oController = new AC13Controller();
		m_oController.setLogListener(m_oLogListener);
	}

	// Default Robot Device Functions =========================================

	@Override
	public RobotType getType() {
		// TODO Auto-generated method stub
		return RobotType.RBT_AC13ROVER;
	}

	@Override
	public String getAddress() {
		// TODO Auto-generated method stub
		return AC13RoverTypes.ADDRESS;
	}

	// Custom AC 13 Rover Functions ====================================================
	
	private AC13Controller getController() {
		return (AC13Controller) m_oController;
	}
	
	public void toggleInfrared() {
		getController().switchInfrared();
	}
	
	public boolean isInfraredEnabled() {
		return getController().isInfraredEnabled();
	}
	
	public boolean startVideo() {
		return getController().startStreaming();
	}
	
	public void stopVideo() {
		getController().stopStreaming();
	}
	
	public boolean isStreaming() {
		return getController().isStreaming();
	}

}