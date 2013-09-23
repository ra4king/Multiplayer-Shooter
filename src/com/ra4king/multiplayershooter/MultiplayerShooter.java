package com.ra4king.multiplayershooter;

import java.awt.Color;
import java.awt.Graphics2D;

import com.ra4king.gameutils.Game;

public class MultiplayerShooter extends Game {
	private static final long serialVersionUID = 1L;
	
	public static void main(String[] args) {
		MultiplayerShooter game = new MultiplayerShooter();
		game.setupFrame("Multiplayer Shooter", true);
		game.start();
	}
	
	public MultiplayerShooter() {
		super(800,600);
	}
	
	public void initGame() {
		setMaximumUpdatesBeforeRender(1);
		
		try {
			getArt().add("player.png");
		}
		catch(Exception exc) {
			exc.printStackTrace();
		}
		
		setScreen("Arena",new Arena());
	}
	
	public void paint(Graphics2D g) {
		super.paint(g);
		g.setColor(Color.red);
	}
}
