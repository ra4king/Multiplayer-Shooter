package com.ra4king.multiplayershooter;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JOptionPane;

import com.ra4king.gameutils.Entity;
import com.ra4king.gameutils.Game;
import com.ra4king.gameutils.Input;
import com.ra4king.gameutils.gameworld.GameWorld;
import com.ra4king.gameutils.networking.DatagramPacketIO;
import com.ra4king.gameutils.networking.Packet;
import com.ra4king.gameutils.networking.SocketPacketIO;
import com.ra4king.gameutils.util.FastMath;

public class Arena extends GameWorld {
	private volatile SocketPacketIO sio;
	private volatile DatagramPacketIO dio;
	private HashMap<Integer,Player> players;
	private ArrayList<Bullet> bulletsToSend;
	private volatile Player player;
	
	private volatile boolean gameStarted, gameOver, isKilled;
	
	private long lastShot;
	
	private ArrayList<String> messages;
	private long messageTime = -1;
	
	private long lastPacket;
	private long pingCount, lastPing, lastPingSent;
	
	private int packetCount, lastPacketReceived;
	
	private final int version = 18;
	private boolean showedMessage;
	
	private final String server = "10.100.130.75";
	private final int port = 5053;
	
	public void init(Game game) {
		super.init(game);
		
		Graphics2D g = (Graphics2D)game.getGraphics();
		g.setColor(Color.black);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		game.setVersion(version/100.0);
		
		players = new HashMap<>();
		
		bulletsToSend = new ArrayList<>();
		
		messages = new ArrayList<>();
		
		messages.add("Arrows to move, X or CTRL to shoot! Good luck and may the force be with you! :)");
		
		setBackground(Color.black);
		
		connect();
		
		new Thread(new Runnable() {
			private int received;
			private long lastTime = System.nanoTime();
			
			public void run() {
				while(true) {
					try {
						Thread.sleep(2);
					}
					catch(Exception exc) {}
					
					if(!gameStarted || dio == null)
						continue;
					
					try {
						Packet packet;
						while((packet = dio.read()) != null) {
							received++;
							
							int packetNum = packet.readInt();
							if(packetNum < lastPacketReceived)
								continue;
							
							lastPacketReceived = packetNum;
							
							while(packet.hasMore()) {
								int type = packet.readInt();
								
								if(type == 0) {
									int id = packet.readInt();
									
									if(players.get(id) == null)
										new Player(packet,id,false,null);
									else
										players.get(id).read(packet);
								}
								else if(type == 1) {
									if(packet.readLong() == pingCount-1) {
										player.setPing((int)Math.round((System.nanoTime()-lastPing)/1e6));
										lastPing = 0;
									}
								}
							}
						}
						
						while((packet = sio.read()) != null) {
							received++;
							
							while(packet.hasMore()) {
								int type = packet.readInt();
								
								if(type == 0) {
									int id = packet.readInt();
									Player p = new Player(packet,id,false,packet.readString());
									players.put(id,(Player)add(p));
									System.out.println("New player: " + id + ", name: " + p.getName());
								}
								else if(type == 1) {
									int id = packet.readInt();
									System.out.println("Removing player with ID: " + id + ", name: " + players.get(id).getName());
									remove(players.remove(id));
								}
								else if(type == 2)
									player.setScore(player.getScore()+1);
								else if(type == 3) {
									int id = packet.readInt();
									if(players.get(id) == null)
										new Bullet(packet,null);
									else
										add(1,new Bullet(packet,players.get(id)));
								}
								else if(type == 5)
									messages.add(packet.readString());
								else if(type == -1) {
									gameOver = true;
									gameStarted = false;
								}
							}
						}
						
						if(System.nanoTime() - lastTime >= 1e9) {
							System.out.println(received + " packets received");
							received = 0;
							lastTime += 1e9;
						}
					}
					catch(Exception exc) {
						exc.printStackTrace();
						
						try {
							Packet p = new Packet();
							p.writeInt(-1);
							sio.write(p);
						}
						catch(Exception exc2) {}
						
						gameOver = true;
						gameStarted = false;
					}
				}
			}
		}).start();
	}
	
	public void connect() {
		try{
			String name;
			while((name = JOptionPane.showInputDialog(getGame(),"Type your name:")) != null && name.equals(""));
			
			if(name == null) {
				if(getGame().isApplet())
					return;
				else
					System.exit(0);
			}
			
			sio = new SocketPacketIO(server,port);
			
			System.out.println("Connection successful!");
			
			Packet packet = new Packet();
			packet.writeInt(version);
			packet.writeString(name);
			sio.write(packet);
			
			int type = sio.read().readInt();
			
			if(type == 1)
				readWorld();
			else if(type == -2) {
				if(!showedMessage) {
					JOptionPane.showMessageDialog(getGame(), "You are using an older version. Please " + (getGame().isApplet() ? "refresh the page." : "redownload at www.ra4king.com/games/Multiplayer/Shooter.jar"));
					if(!getGame().isApplet() && Desktop.isDesktopSupported()) {
						Desktop.getDesktop().browse(new URI("http://www.ra4king.com/games/Multiplayer/Shooter.jar"));
						System.exit(0);
					}
					
					showedMessage = true;
				}
			}
			else if(type == -3) {
				JOptionPane.showMessageDialog(getGame(), "Name already taken. Please use a different name.");
				connect();
				return;
			}
			else
				System.out.println("No games found....");
			
			sio.setBlocking(false);
		}
		catch(Exception exc) {
			exc.printStackTrace();
			
			try {
				Packet p = new Packet();
				p.writeInt(-1);
				sio.write(p);
			}
			catch(Exception exc2) {}
			
			JOptionPane.showMessageDialog(getGame(), "Connection to server failed! Retrying...", "Failed!", JOptionPane.OK_OPTION);
		}
	}
	
	public void readWorld() throws Exception {
		System.out.println("Building world...");
		
		Packet packet = sio.read();
		
		System.out.println(packet.getRemaining());
		
		int pid = packet.readInt();
		System.out.println("PlayerID: " + pid);
		player = (Player)add(2,new Player(packet,pid,true,packet.readString()));
		
		while(packet.hasMore()) {
			System.out.println(packet.getRemaining());
			int id = packet.readInt();
			players.put(id,(Player)add(new Player(packet,id,false,packet.readString())));
		}
		
		System.out.println();
		
		gameStarted = true;
		
		System.out.println("Game started!");
		
		dio = new DatagramPacketIO(new InetSocketAddress(server,5053),false);
		
		lastTime = System.nanoTime();
	}
	
	private int sentCount;
	private long lastTime;
	
	public void update(long deltaTime) {
		if(gameStarted) {
			Input i = getGame().getInput();
			
			if(player.getHealth() > 0) {
				double speed = 350 * deltaTime/1e9;
				double rotSpeed = FastMath.PI * 5/6 * deltaTime/1e9;
				
				if(i.isKeyDown(KeyEvent.VK_LEFT))
					player.setRotation(player.getRotation()-rotSpeed);
				else if(i.isKeyDown(KeyEvent.VK_RIGHT))
					player.setRotation(player.getRotation()+rotSpeed);
				
				if(i.isKeyDown(KeyEvent.VK_UP))
					player.move(speed);
				else if(i.isKeyDown(KeyEvent.VK_DOWN))
					player.move(-speed);
				
				if((i.isKeyDown(KeyEvent.VK_X) || i.isKeyDown(KeyEvent.VK_CONTROL)) && System.nanoTime()-lastShot >= 8.5e7) {
					bulletsToSend.add((Bullet)add(1,new Bullet(player.getCenterX() + 20 * FastMath.cos(player.getRotation() + FastMath.PI/4),player.getCenterY() + 20 * FastMath.sin(player.getRotation() + FastMath.PI/4),500,player.getRotation(),player)));
					lastShot = System.nanoTime();
				}
			}
			else if(i.isKeyDown(KeyEvent.VK_SPACE)) {
				player.setLocation(0, 0);
				player.setHealth(50);
				player.setRotation(0);
				isKilled = false;
			}
			
			super.update(deltaTime);
			
			if(lastPacket == 0)
				lastPacket = System.nanoTime();
			
			try {
				long packetDelay;
				
				if(player.getPing() > 1000)		packetDelay = (long)1e9 * 5;
				else if(player.getPing() > 500)	packetDelay = (long)1e9;
				else if(player.getPing() > 200) packetDelay = (long)(1e9/5);
				else if(player.getPing() > 100) packetDelay = (long)(1e9/20);
				else 							packetDelay = (long)(1e9/30);
				
				if(player.getHealth() <= 0 && !isKilled) {
					Packet packet = new Packet();
					packet.writeInt(1);
					packet.writeInt(player.getKillingBullet().getPlayer().getID());
					sio.write(packet);
					isKilled = true;
				}
				else if(System.nanoTime() - lastPacket >= packetDelay) {
					Packet packet = new Packet();
					packet.writeInt(player.getID());
					packet.writeInt(packetCount++);
					packet.writeInt(0);
					player.write(packet);
					
					if(( player.getPing() == 0 && lastPing == 0) || (player.getPing() > 0 && System.nanoTime()-lastPingSent >= 1e9 + player.getPing())) {
						packet.writeInt(1);
						packet.writeLong(pingCount++);
						lastPing = System.nanoTime();
						lastPingSent = System.nanoTime();
					}
					
					dio.write(packet);
					sentCount++;
					
					if(bulletsToSend.size() > 0) {
						packet = new Packet();
						for(Bullet b : bulletsToSend) {
							packet.writeInt(2);
							b.write(packet);
						}
						bulletsToSend.clear();
						sio.write(packet);
						
						sentCount++;
					}
					
					lastPacket += packetDelay;
					
					if(System.nanoTime() - lastTime >= 1e9) {
						System.out.println(sentCount + " packets sent");
						sentCount = 0;
						lastTime += 1e9;
					}
				}
			}
			catch(Exception exc) {
				exc.printStackTrace();
				gameOver = true;
				gameStarted = false;
			}
		}
		else if(gameOver) {
			if(getGame().getInput().isKeyDown(KeyEvent.VK_ENTER))
				gameOver = false;
			
			clear();
		}
		else {
			try{
				Packet p = sio.read();
				
				if(p != null && p.readInt() == 1)
					readWorld();
			}
			catch(Exception exc) {
				connect();
			}
		}
	}
	
	public void draw(Graphics2D g) {
		if(gameStarted) {
			super.draw(g);
			
			for(Entity e : getEntitiesAt(0)) {
				if(e instanceof Player)
					((Player)e).drawHUD(g);
			}
			
			player.drawHUD(g);
			
			if(player.getHealth() <= 0) {
				g.setColor(Player.Colors.get(player.getID()));
				String msg = "YOU ARE DEAD! Hit SPACE to Respawn";
				g.drawString(msg,getWidth()/2 - g.getFontMetrics().stringWidth(msg)/2,getHeight()/2+5);
			}
			else if(messages.size() > 0) {
				if(messageTime == 0)
					messageTime = System.nanoTime();
				else if(messageTime == -1)
					messageTime = System.nanoTime() + (long)7e9;
				
				if(System.nanoTime() - messageTime < 3e9) {
					String msg = messages.get(0);
					g.drawString(msg,getWidth()/2 - g.getFontMetrics().stringWidth(msg)/2,getHeight()/2+5);
				}
				else {
					messageTime = 0;
					messages.remove(0);
				}
			}
		}
		else {
			String message = gameOver ? "Game Over! Press ENTER for new game" : "Waiting for match...";
			g.drawString(message, getWidth()/2 - g.getFontMetrics().stringWidth(message),getHeight()/2-10);
		}
	}
}
