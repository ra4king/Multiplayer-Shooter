package com.ra4king.multiplayershooter;

import java.awt.Graphics2D;

import com.ra4king.gameutils.gameworld.GameComponent;
import com.ra4king.gameutils.networking.Packet;

public class Bullet extends GameComponent {
	private Player player;
	private double speed, rotation;
	
	public Bullet(double x, double y, double speed, double rotation, Player player) {
		super(x,y,6,6);
		this.speed = speed;
		this.rotation = rotation;
		this.player = player;
	}
	
	public Bullet(Packet p, Player player) {
		read(p);
		setSize(5,5);
		this.player = player;
	}
	
	public void read(Packet p) {
		setX(p.readDouble());
		setY(p.readDouble());
		speed = p.readDouble();
		rotation = p.readDouble();
	}
	
	public void write(Packet p) {
		p.writeDouble(getX(),getY(),speed,rotation);
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public double getSpeed() {
		return speed;
	}
	
	public double getRotation() {
		return rotation;
	}
	
	public void update(long deltaTime) {
		if(getX()+getWidth() < 0 || getX() >= getParent().getWidth() || getY()+getHeight() < 0 || getY() >= getParent().getHeight())
			getParent().remove(this);
		
		double s = speed * deltaTime / 1e9;
		
		setX(getX() + s * Math.cos(rotation));
		setY(getY() + s * Math.sin(rotation));
	}
	
	public void draw(Graphics2D g) {
		g.setColor(Player.Colors.get(player.getID()));
		g.rotate(rotation,getCenterX(),getCenterY());
		g.fillOval(getIntX(), getIntY(), getIntWidth(), getIntHeight());
	}
}
