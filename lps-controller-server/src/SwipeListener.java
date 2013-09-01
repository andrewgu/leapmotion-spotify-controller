import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Frame;
import com.leapmotion.leap.Gesture;
import com.leapmotion.leap.GestureList;
import com.leapmotion.leap.Listener;
import com.leapmotion.leap.SwipeGesture;

public class SwipeListener extends Listener {
	public static final int CMD_NONE = 0;
	public static final int CMD_PAUSE = 1;
	public static final int CMD_PLAY = 2;
	public static final int CMD_VOLUME = 3;
	
	public static final int CMD_LOCKOUT_TIME = 100;
	// From experimentation, this is a luxurious but quick swipe.
	public static final int SWIPE_SPEED_MINIMUM = 700;
	// Swipe has to be within 22.5 degrees of the "ideal" direction.
	public static final double SWIPE_DIRECTEDNESS_THRESHOLD = Math.cos(Math.PI/8.0); 
	
	public static final long VOLUME_SAMPLE_CONSECUTIVE_MIN = 30;
	public static final float VOLUME_SAMPLE_Y_MIN = 100;
	public static final float VOLUME_SAMPLE_Y_MAX = 500;
	
	private long lastCommand;
	private SwipeServer commandHandler;
	private long volumeSampleCounter = 0;
	
	
    public SwipeListener(SwipeServer swipeServer) {
		this.commandHandler = swipeServer;
		this.lastCommand = System.currentTimeMillis();
		this.volumeSampleCounter = 0;
	}

	public void onInit(Controller controller) {
        System.out.println("Initialized");
    }

    public void onConnect(Controller controller) {
        System.out.println("Connected");
        controller.enableGesture(Gesture.Type.TYPE_SWIPE);
        //controller.enableGesture(Gesture.Type.TYPE_CIRCLE);
        //controller.enableGesture(Gesture.Type.TYPE_SCREEN_TAP);
        //controller.enableGesture(Gesture.Type.TYPE_KEY_TAP);
    }

    public void onDisconnect(Controller controller) {
        //Note: not dispatched when running in a debugger.
        System.out.println("Disconnected");
    }

    public void onExit(Controller controller) {
        System.out.println("Exited");
    }

    public void onFrame(Controller controller) {
        // Get the most recent frame and report some basic information
        Frame frame = controller.frame();
        /*System.out.println("Frame id: " + frame.id()
                         + ", timestamp: " + frame.timestamp()
                         + ", hands: " + frame.hands().count()
                         + ", fingers: " + frame.fingers().count()
                         + ", tools: " + frame.tools().count()
                         + ", gestures " + frame.gestures().count());
        //*/
        
        if (frame.hands().count() == 1 && frame.fingers().count() == 0) {
        	// One hand, closed fist = set volume.
        	//System.out.println(frame.hands().get(0).palmPosition().getY());
        	
        	this.pushVolumeSample(frame.hands().get(0).palmPosition().getY());
        }

        GestureList gestures = frame.gestures();
        for (int i = 0; i < gestures.count(); i++) {
            Gesture gesture = gestures.get(i);

            switch (gesture.type()) {
                case TYPE_SWIPE:
                    SwipeGesture swipe = new SwipeGesture(gesture);
                    //System.out.println("Swipe id: " + swipe.id()
                    //           + ", " + swipe.state()
                    //           + ", position: " + swipe.position()
                    //           + ", direction: " + swipe.direction()
                    //           + ", speed: " + swipe.speed());
                    if (swipe.state().toString().equals("STATE_STOP")) {
                    	// Only listen to the end of the swipe, as a quick and dirty proxy for a swipe.
                    	if (swipe.speed() > SWIPE_SPEED_MINIMUM) {
                    		if (swipe.direction().getX() > SWIPE_DIRECTEDNESS_THRESHOLD) {
                    			// Swipe to the right is to pause.
                    			this.updateCommand(CMD_PAUSE, 0.0f);
                    		}
                    		else if (swipe.direction().getX() < -SWIPE_DIRECTEDNESS_THRESHOLD) {
                    			// Swipe to the left is to play.
                    			this.updateCommand(CMD_PLAY, 0.0f);
                    		}
                    	}
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void updateCommand(int command, float data) {
    	long now = System.currentTimeMillis();
    	
    	// Since the leapmotion has a tendency to detect one swipe as multiple, 
    	// do not allow duplicate swipes to be interpreted as multiple commands. 
    	if (now - this.lastCommand > CMD_LOCKOUT_TIME) {
    		this.commandHandler.commandUpdate(command, data);
    		this.lastCommand = now;
    		this.volumeSampleCounter = 0;
    	}
    }
    
	private void pushVolumeSample(float y) {
		this.volumeSampleCounter++;
		if (this.volumeSampleCounter > VOLUME_SAMPLE_CONSECUTIVE_MIN) {
			// Threshold.
			y = Math.min(Math.max(y, VOLUME_SAMPLE_Y_MIN), VOLUME_SAMPLE_Y_MAX);
			float volume = (y - VOLUME_SAMPLE_Y_MIN) / (VOLUME_SAMPLE_Y_MAX-VOLUME_SAMPLE_Y_MIN);
			this.updateCommand(CMD_VOLUME, volume);
		}
	}
}