package com.ra4king.multiplayershooter;

import java.awt.Color;
import java.awt.Graphics2D;

import com.ra4king.gameutils.Entity;
import com.ra4king.gameutils.gameworld.GameComponent;
import com.ra4king.gameutils.networking.Packet;
import com.ra4king.gameutils.util.FastMath;

public class Player extends GameComponent {
	private Bullet killingBullet;
	
	private String name;
	
	private int health, ping, score;
	
	private final int maxHealth = 50, id;
	
	private double rotation;
	private boolean highlight;
	
	public enum Colors {
		$1(Color.yellow), $2(Color.red), $3(Color.blue), $4(Color.orange), $5(Color.green),
		$6(Color.magenta), $7(Color.GRAY), $8(Color.white), $9(Color.darkGray), $10(Color.pink);
		
		private Color c;
		
		Colors(Color c) {
			this.c = c;
		}
		
		public Color getColor() {
			return c;
		}
		
		public static Color get(int id) {
			return values()[id].c;
		}
	}
	
	public Player(Packet p, int id, boolean highLight, String name) {
		read(p);
		setSize(38,33);
		this.id = id;
		this.highlight = highLight;
		this.name = name;
	}
	
	public void read(Packet p) {
		setX(p.readDouble());
		setY(p.readDouble());
		rotation = p.readDouble();
		health = p.readInt();
		score = p.readInt();
		ping = p.readInt();
	}
	
	public void write(Packet p) {
		p.writeDouble(getX(),getY(),rotation);
		p.writeInt(health,ping);
	}
	
	public int getID() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public int getPing() {
		return ping;
	}
	
	public void setPing(int ping) {
		this.ping = ping;
	}
	
	public void move(double speed) {
		setX(Math.min(getParent().getWidth()-getWidth(),Math.max(0,getX() + speed * FastMath.cos(rotation))));
		setY(Math.min(getParent().getHeight()-getHeight(),Math.max(0,getY() + speed * FastMath.sin(rotation))));
	}
	
	public int getHealth() {
		return health;
	}
	
	public void setHealth(int health) {
		this.health = health;
	}
	
	public double getRotation() {
		return rotation;
	}
	
	public void setRotation(double rot) {
		this.rotation = rot;
	}
	
	public int getScore() {
		return score;
	}
	
	public void setScore(int score) {
		this.score = score;
	}
	
	public Bullet getKillingBullet() {
		Bullet b = killingBullet;
		killingBullet = null;
		return b;
	}
	
	public void update(long deltaTime) {
		if(health <= 0)
			return;
		
		if(highlight) {
			for(Entity c : getParent().getEntities()) {
				if(c instanceof Bullet && ((Bullet)c).getPlayer() != this && c.getBounds().intersects(getBounds())) {
					health -= 3;
					
					getParent().remove(c);
					
					if(health <= 0) {
						killingBullet = (Bullet)c;
						health = 0;
						break;
					}
				}
			}
		}
	}
	
	public void draw(Graphics2D g) {
		if(health <= 0)
			return;
		
		g.rotate(rotation, getCenterX(), getCenterY());
		
//		Polygon poly = new Polygon(new int[] { getIntX(), getIntX() + getIntWidth(), getIntX() },
//				   new int[] { getIntY(), getIntY() + getIntHeight()/2, getIntY() + getIntHeight() },
//				   3);
		
//		g.setColor(Colors.get(id));
//		g.fill(poly);
		
		g.drawImage(getParent().getGame().getArt().get("player"), getIntX(), getIntY(), null);//Colors.get(id), null);
		
		g.setColor(Color.gray);
		g.fillRect(getIntX(), getIntY() + getIntHeight() + 10, getIntWidth(), 5);
		g.setColor(Colors.get(id));
		g.fillRect(getIntX(), getIntY() + getIntHeight() + 10, (int)Math.round((double)health / maxHealth * getWidth()), 5);
		
//		if(highlight) {
//			g.setStroke(new BasicStroke(2));
//			g.setColor(Color.yellow);
//			g.draw(poly);
//		}
	}
	
	public void drawHUD(Graphics2D g) {
		g.setColor(Colors.get(id));
		g.drawString(name, getParent().getWidth()/15 + (id%5) * getParent().getWidth() / 5, 50 + (id/5) * (getParent().getHeight()-150));
		g.drawString("Ping: " + ping, getParent().getWidth()/15 + (id%5) * getParent().getWidth() / 5, 65 + (id/5) * (getParent().getHeight()-150));
		g.drawString("Health: " + health, getParent().getWidth()/15 + (id%5) * getParent().getWidth() / 5, 80 + (id/5) * (getParent().getHeight()-150));
		g.drawString("Score: " + score, getParent().getWidth()/15 + (id%5) * getParent().getWidth() / 5, 95 + (id/5) * (getParent().getHeight()-150));
		g.drawString("X: " + getIntX() + ", Y: " + getIntY(), getParent().getWidth()/15 + (id%5) * getParent().getWidth() / 5, 110 + (id/5) * (getParent().getHeight()-150));
	}
}
