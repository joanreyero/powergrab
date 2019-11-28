package uk.ac.ed.inf.powergrab;

import com.mapbox.geojson.Feature;

public class Move {
	final Direction direction;
	final Double coinD;
	final Double powerD;
	final Feature feature;
	final Double utility;
	
	Move(Direction direction, Double coinD, Double powerD, Feature feature, Double utility) {
		this.direction = direction;
		this.coinD = coinD;
		this.powerD = powerD;
		this.feature = feature;
		this.utility = utility;
	}

}
