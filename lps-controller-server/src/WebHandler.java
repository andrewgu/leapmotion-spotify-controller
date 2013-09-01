import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

class WebHandler implements Runnable {
	// Maximum delay 5 seconds for new commands to come in before responding to request.
	public static final long MAX_POLL_DELAY = 5000;
	public static final long INNER_POLL_PERIOD = 100;
	
	private Queue<Command> commandQueue;
	private boolean stopServer;
	
	public WebHandler() {
		this.commandQueue = new LinkedList<Command>();
		this.stopServer = false;
	}
	
	@Override
	public void run() {
		// Start the server
		HttpServer server;
		try {
			server = HttpServer.create(new InetSocketAddress(15678), 0);
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		
        server.createContext("/getcommand", new MyHandler(this.commandQueue));
        server.setExecutor(null); // creates a default executor
        server.start();
        
        // Sleep loop for exit signal. Really could be more than 10 seconds because we use a call to .interrupt() to signal, but "just in case".
        while (!this.stopServer) {
        	try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// Do nothing, this is intentional.
			}
        }
        
        // Kill the server because we got a kill signal from the main thread.
        server.stop(0);
	}
	
	static class MyHandler implements HttpHandler {
		private Queue<Command> sharedCommandQueue;
		
		public MyHandler(Queue<Command> commandQueue) {
			this.sharedCommandQueue = commandQueue;
		}
		
        public void handle(HttpExchange t) throws IOException {
        	System.out.println("Received request.");
        	
            Command nextCommand = new Command(SwipeListener.CMD_NONE, 0.0f);        	
        	
            long requestReceived = System.currentTimeMillis();
            long now = requestReceived;
            
            while (now - requestReceived < MAX_POLL_DELAY){
            	if (!this.sharedCommandQueue.isEmpty()) {
            		synchronized(this.sharedCommandQueue) {
            			nextCommand = this.sharedCommandQueue.poll();
            			//System.out.println("Pulled: " + nextCommand.cmd);
            		}
	            	break;
            	}
            	else {
            		try {
						Thread.sleep(INNER_POLL_PERIOD);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
            		now = System.currentTimeMillis();
            	}
	        }
        	
            String response;
            switch (nextCommand.cmd) {
            case SwipeListener.CMD_PAUSE:
            	response = "{\"command\":\"pause\"}";
            	break;
            case SwipeListener.CMD_PLAY:
            	response = "{\"command\":\"play\"}";
            	break;
            case SwipeListener.CMD_VOLUME:
            	response = "{\"command\":\"volume\",\"volume\":" + Float.toString(nextCommand.data) + "}";
            	break;
            default:
            	response = "{\"command\":\"none\"}";
            	break;
            }
            
            System.out.println("Response: " + response);
            
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
	
	public void stopServer() {
		this.stopServer = true;
	}

	public void commandUpdate(Command command) {
		synchronized (commandQueue) {
			if (command.cmd == SwipeListener.CMD_VOLUME) {
				// If it's a volume setting, either promote the foremost volume setting in the queue, or append.
				// That way the volume setting is always up to date.
				Iterator<Command> i = commandQueue.iterator();
				boolean matchFound = false;
				while (i.hasNext()) {
					Command c = i.next();
					if (c.cmd == SwipeListener.CMD_VOLUME) {
						c.data = command.data;
						matchFound = true;
					}
				}
				
				if (!matchFound) {
					this.commandQueue.add(command);
				}
			}
			else {
				this.commandQueue.add(command);
			}
		}
	}
}