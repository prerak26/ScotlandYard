package bobby;

import java.net.*;
import java.io.*;
import java.util.*;

import java.util.concurrent.Semaphore;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ScotlandYard implements Runnable{

	/*
		this is a wrapper class for the game.
		It just loops, and runs game after game
	*/

	public int port;
	public int gamenumber;

	public ScotlandYard(int port){
		this.port = port;
		this.gamenumber = 0;
	}

	public void run(){
		while (true){
			Thread tau = new Thread(new ScotlandYardGame(this.port, this.gamenumber));
			tau.start();
			try{
				tau.join();
			}
			catch (InterruptedException e){
				return;
			}
			this.gamenumber++;
		}
	}

	public class ScotlandYardGame implements Runnable{
		private Board board;
		private ServerSocket server;
		public int port;
		public int gamenumber;
		private ExecutorService threadPool;

		public ScotlandYardGame(int port, int gamenumber){
			this.port = port;
			this.board = new Board();
			this.gamenumber = gamenumber;
			try{
				this.server = new ServerSocket(port);
				System.out.println(String.format("Game %d:%d on", port, gamenumber));
				server.setSoTimeout(5000);
			}
			catch (IOException i) {
				return;
			}
			this.threadPool = Executors.newFixedThreadPool(10);
		}


		public void run(){

			try{
			
				//INITIALISATION: get the game going

				

				Socket socket = null;
				boolean fugitiveIn;
				
				/*
				listen for a client to play fugitive, and spawn the moderator.
				
				here, it is actually ok to edit this.board.dead, because the game hasn't begun
				*/
				fugitiveIn = false;
				
				do{
			        try {
						socket = server.accept();
						System.out.println("hey");
					} catch (Exception e) {
						
						continue;
					}         
					
					
                    //if(this.board.embryo){
						
						fugitiveIn = true;
						this.board.dead = false;
					//}         
       
					                  
                         
               
      
				} while (!fugitiveIn);
				//Sytem.out.println("IN");
				System.out.println(this.gamenumber);

				// Spawn a thread to run the Fugitive
                this.threadPool.execute(new ServerThread(this.board, -1, socket, this.port, this.gamenumber));                            
                                 
                            
                                                                                                  
                                             

				// Spawn the moderator
				
				//System.out.println("Part2.2");
				Moderator mod = new Moderator(this.board);
				Thread mod_t = new Thread(mod);
                mod_t.start();                                  
                
				while (true){
					/*
					listen on the server, accept connections
					if there is a timeout, check that the game is still going on, and then listen again!
					*/
					//System.out.println("IN");
					try {
						socket = server.accept();
					} 
					catch (SocketTimeoutException t){
                        
						if(this.board.dead){
							break;
						}                       
                            
                        continue;                        
             
					}
					
					
					/*
					acquire thread info lock, and decide whether you can serve the connection at this moment,

					if you can't, drop connection (game full, game dead), continue, or break.

					if you can, spawn a thread, assign an ID, increment the totalThreads

					don't forget to release lock when done!
					*/
					//System.out.println("1");                                        
                     this.board.threadInfoProtector.acquire();
					 //System.out.println("2");
						int identity = this.board.getAvailableID();
						if(	this.board.dead){
							//System.out.println("21");
							socket.close();
							break;
						}
						if(identity == -1){
							//System.out.println("22");
							socket.close();
							continue;
						}
						else{
							//System.out.println("23");
							this.threadPool.execute(new ServerThread(this.board, identity, socket, this.port, this.gamenumber));
							this.board.totalThreads++;
						}

					this.board.threadInfoProtector.release();
				
				}

				/*
				reap the moderator thread, close the server, 
				
				kill threadPool (Careless Whispers BGM stops)
				*/
				mod_t.join();
				//this.board.moderatorEnabler.release();
				this.server.close();
				this.threadPool.shutdown();       
                        
                               
    
				System.out.println(String.format("Game %d:%d Over", this.port, this.gamenumber));
				return;
			}
			catch (InterruptedException ex){
				System.err.println("An InterruptedException was caught: " + ex.getMessage());
				ex.printStackTrace();
				return;
			}
			catch (IOException i){
				return;
			}
			
		}

		
	}

	public static void main(String[] args) {
		for (int i=0; i<args.length; i++){
			int port = Integer.parseInt(args[i]);
			Thread tau = new Thread(new ScotlandYard(port));
			tau.start();
		}
	}
}