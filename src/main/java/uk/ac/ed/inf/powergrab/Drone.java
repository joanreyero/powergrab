package uk.ac.ed.inf.powergrab;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

// Lack of modifier indicates that the following class is package-private

abstract class Drone {

   protected Position position;
   protected String mapSource;
   protected Random rand;
   protected double coins;
   protected double power;
   protected int numMoves = 0;
   protected ArrayList<Point> visitedPoints;
   protected String flightProtocol;
   protected BufferedWriter flightBWriter;
   protected String fileNamePrefix;
   protected ArrayList<Feature> features;
   protected ArrayList<Feature> orginalFeatures;

	Drone(Position startPosition, String source, int seed, String fileNamePrefix) throws IOException {
		// Source of the map.
		this.mapSource = source;
		// Defining the start position.
		this.position = startPosition;
		this.coins = 0;              
		this.power = 250;
		// Initialising seed for random moves.
		this.rand = new Random(seed);
		// An array with all the features in the map.
		this.features = (ArrayList<Feature>) FeatureCollection.fromJson(this.mapSource).features();
		this.orginalFeatures = (ArrayList<Feature>) FeatureCollection.fromJson(this.mapSource).features();
		// The visited points.
	    this.visitedPoints = new ArrayList<Point>();
	    // Initialising the current point (start point) and adding it to visited points
	    Point currentPoint = Point.fromLngLat(startPosition.longitude, startPosition.latitude);
	    visitedPoints.add(currentPoint);
	    
	    // For writing the file.
	    this.fileNamePrefix = fileNamePrefix;
	    FileWriter fr = new FileWriter(fileNamePrefix+"txt", false);
	    this.flightBWriter = new BufferedWriter(fr);
	}
	
	// This will be implemented in the stateless/stateful classes
	abstract Move nextMove();
	
	
	void move() throws IOException {
		//System.out.print("moving");
		
		Move move = this.nextMove();
		String writeString = this.position.latitude + " " + this.position.longitude + " " + move.direction;
		
		// Moving in the direction given my nextMove()
		this.position = this.position.nextPosition(move.direction);
		writeString += " " + this.position.latitude + " " + this.position.longitude;
		visitedPoints.add(Point.fromLngLat(this.position.longitude, this.position.latitude));
		
		// New power is the current power + the amount of power from station (if any) minus what it
		// took to execute the move.
		this.power = this.power + move.powerD - 1.25;
		// The new coins are the coins we had plus what we took from the station (if any).
		this.coins = this.coins + move.coinD;
		
		writeString += String.format(" %f %f",this.coins,this.power);
		numMoves++;
		
		if (move.feature != null) {  // In case we visited a feature in our move
			
			//System.out.println(move.feature);
			
			// We need to store the feature's power and coins
			// as we will have to delete the properties to update them.
			double featureCoins = move.feature.getProperty("coins").getAsDouble();
			double featurePower = move.feature.getProperty("power").getAsDouble();
			
			// We need to remove the coin and power properties so we can  
			// replace them with the new ones.
			move.feature.removeProperty("coins"); 
			move.feature.removeProperty("power");
			
			// The new coins/power will be the old ones - what we took
			move.feature.addStringProperty("coins", Double.toString(featureCoins - move.coinD));
			move.feature.addStringProperty("power", Double.toString(featurePower - move.powerD));
			
			
		}
		
		this.flightBWriter.write(writeString);
		this.flightBWriter.newLine();

		System.out.println(move.direction);
		//System.out.println(this.coins);
		//System.out.println(this.power);
		//System.out.println(this.numMoves);

	}

	 void writeFlightPath() throws IOException {

		LineString dronePath = LineString.fromLngLats(this.visitedPoints);
		Feature dronePathFeature = Feature.fromGeometry(dronePath);
		this.orginalFeatures.add(dronePathFeature);
		FeatureCollection featureCollection = FeatureCollection.fromFeatures(this.orginalFeatures);
		String newMap = featureCollection.toJson().toString();
		System.out.println(newMap);
		try (FileWriter file = new FileWriter(this.fileNamePrefix+"geojson")) {
			file.write(newMap);
			System.out.println("Successfully wrote flight path to file.");

		}

	}
	
	// The Utility function is:
	 // (station power) / (current power) + (station coins) / (current coins)
	double getUtility (double SCoins, double SPower) {
		return (SPower / this.power + SCoins / this.coins);
	}
	
	Move stationInReach(Position position, Direction direction) {

		// A position with no features has a utility of 0.
		double utility = 0.0;
		
		double nearestDistance = Double.POSITIVE_INFINITY;
		
		// Initialising the move
		Move move = new Move(direction, 0.0, 0.0, null, utility);

		// For the nearest station we add up its negative or positive utility to the
		// overall utility.

		for (Integer i = 0; i < this.features.size(); i++) {
			Feature feature = this.features.get(i);
			double longitude = ((Point) feature.geometry()).coordinates().get(0);
			double latitude = ((Point) feature.geometry()).coordinates().get(1);
			
			// Getting the distance of the drone to the station, to see if it is in range
			double distSquared = Math.pow(longitude - position.longitude, 2) + Math.pow(latitude - position.latitude, 2);

			
			if (distSquared <= Math.pow(0.00025, 2) && distSquared < nearestDistance) {  // If in range
				
				nearestDistance = Math.sqrt(distSquared);  // Update nearest distance

				// Get the coin and power change the station will give us 
				double coindD = feature.getProperty("coins").getAsDouble();
				double powerGain = feature.getProperty("power").getAsDouble();
				
				// If the station stores debt, as we cannot store it in the drone, we can 
				// only give the station as much as we have. 
				if (coindD < 0) {

					coindD = Math.max(coindD, (-1) * this.coins);
				}
				
				if (powerGain < 0) {

					powerGain = Math.max(powerGain, (-1) * this.power);
				}

				// We get the utility that we would have if we moved to the station
				utility = this.getUtility(coindD, powerGain);
				// And return the potential move to the station
				move = new Move(direction, coindD, powerGain, feature, utility);

			}

		}
		
		// Return the move, which will have a utility of 0 if no features are in reach.
		return move;

	}

}