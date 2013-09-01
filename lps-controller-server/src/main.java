/******************************************************************************\
* Copyright (C) 2012-2013 Leap Motion, Inc. All rights reserved.               *
* Leap Motion proprietary and confidential. Not for distribution.              *
* Use subject to the terms of the Leap Motion SDK Agreement available at       *
* https://developer.leapmotion.com/sdk_agreement, or another agreement         *
* between Leap Motion and you, your company or other organization.             *
\******************************************************************************/

import java.io.IOException;
import java.lang.Math;
import com.leapmotion.leap.*;
//import com.leapmotion.leap.Gesture.State;
class Command {
	public int cmd;
	public float data;
	
	public Command(int c, float d) {
		this.cmd = c;
		this.data = d;
	}
}

class SwipeServer {
	
	private WebHandler webHandler;

	public SwipeServer() {
		this.webHandler = new WebHandler();
	}
	
	public void commandUpdate(int cmd, float data) {
		this.webHandler.commandUpdate(new Command(cmd, data));
		
		// Debug
		switch(cmd) {
		case SwipeListener.CMD_PAUSE:
			System.out.println("PAUSE");
			break;
		case SwipeListener.CMD_PLAY:
			System.out.println("PLAY");
			break;
		case SwipeListener.CMD_VOLUME:
			System.out.println("VOLUME:" + Float.toString(data));
			break;
		}
	}
	
	public void run() {
		// Create listener and controller
        SwipeListener listener = new SwipeListener(this);
        Controller controller = new Controller();
        // Have listener receive events from the controller, even when in background.
        controller.setPolicyFlags(Controller.PolicyFlag.POLICY_BACKGROUND_FRAMES);
        controller.addListener(listener);
        
        Thread webHandlerThread = new Thread(this.webHandler);
        webHandlerThread.start();

        // Keep this process running until Enter is pressed
        System.out.println("Press Enter to quit...");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // Remove the sample listener when done
        controller.removeListener(listener);
        
        // Give kill order to the web server.
        this.webHandler.stopServer();
        // Wait for web handler to die before exiting.
        try {
        	webHandlerThread.interrupt();
        	webHandlerThread.join();
        }
        catch (InterruptedException e) {
        	e.printStackTrace();
        }
	}
}

class Main {
    public static void main(String[] args) {
        new SwipeServer().run();
    }
}
