package uk.ac.ed.inf.powergrab;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;

// Lack of modifier indicates that the following class is package-private

class Stateful extends Drone {
	
	Feature closestStation;
	ArrayList<Feature> visitedFeatures;
	ArrayList<Feature> featuresToVisit;
	// TODO: Update visited features and features to visit 
	ArrayList<Feature> featuresAvoid;
	ArrayList<Move> nextMoves;
	
	Stateful(Position startPosition, String source, int seed, String fileNamePrefix) throws IOException {
		super(startPosition, source, seed, fileNamePrefix);
		
		this.visitedFeatures = new ArrayList<Feature>();
		this.nextMoves = new ArrayList<Move>();
		// Calculating the features to visit (positive)
	    this.featuresToVisit = this.features;
	    this.featuresToVisit.removeIf(f -> (f.getProperty("coins").getAsDouble() <= 0));
	    this.featuresToVisit.removeAll(visitedFeatures);
	    
	    
	    // The features to avoid when going on a direct path
	    // RIGHT NOW WE ARE AVOIDING POSITIVE FEATURES THAT ARE NOT THE TARGET
	    // TODO THINK IF THIS MAKES ANY SENSE
	    this.featuresAvoid = features;
	    this.featuresAvoid.removeAll(visitedFeatures);

	}

	Move nextMove() {
		//System.out.println(this.nextMoves);

		if (this.nextMoves.isEmpty()) {
			Feature closestStation = closestStation();
			this.nextMoves = goToStation(closestStation);
		}
		Move ret = this.nextMoves.get(0);
		this.nextMoves.remove(0);
		return ret;
					
		//return new Move(Direction.valueOf("N"), 0.0, 0.0, features.get(0), 0.0);
		
		
	}
	
	Feature closestStation() {
		Feature closestFeature = null;
		double closestDist = Double.POSITIVE_INFINITY;
		for (Feature feature : featuresToVisit) {
			double longitude = ((Point) feature.geometry()).coordinates().get(0);
			double latitude = ((Point) feature.geometry()).coordinates().get(1);
			double sqDist = Math.pow(position.longitude - longitude, 2) + Math.pow(position.latitude - latitude, 2);
			
			if (sqDist <= closestDist) {
				closestFeature = feature;
				closestDist = sqDist;
			}						
		}
		return featuresToVisit.get(3);
	}
	
	ArrayList<Move> goToStation(Feature target) {
		ArrayList<Move> path = new ArrayList<Move>();
		
		Position pos = this.position;
		
		ArrayList<Position> coordinatesAvoid = getCoordinatesAvoid();
		// Calculating the target stations' longitude and latitude
		double targetLong = ((Point) target.geometry()).coordinates().get(0);
		double targetLat = ((Point) target.geometry()).coordinates().get(1);
		
		while (inReach(pos, targetLong, targetLat) == false) { 
			Direction nextDir = getNextDirection(targetLat, targetLong, pos);                
					//getDirFromAngle(Math.atan((targetLat - position.latitude)/(targetLong-position.longitude)));
			Direction originalDir = nextDir;  // We need to keep it in case we cannot go round negative stations
			
			//try {
		    //    TimeUnit.SECONDS.sleep(1);
		    //} catch (InterruptedException e) {
		    //    e.printStackTrace();
		    //}
			int n = 0;  // This counter will keep track of all directions
			boolean clearPath = false;  // Assume, at first, that we do not have a clear path
			
			
			Position potentialPos = pos.nextPosition(nextDir);
			//System.out.println(potentialPos.longitude);
			//System.out.println(potentialPos.latitude);
			//System.out.println(Math.sqrt(Math.pow(targetLong - potentialPos.longitude, 2) + Math.pow(targetLat - potentialPos.latitude, 2)) <= 0.00025);
			//System.out.println(Math.sqrt(Math.pow(targetLong - potentialPos.longitude, 2) + Math.pow(targetLat - potentialPos.latitude, 2)));

			/*while (n < 16 && clearPath == false) {
				
				if (checkInRange(potentialPos, coordinatesAvoid) == true) {
					
			//		System.out.println("Found an obstacle!!!");
					
						//TODO: remember if the drone includes the number of moves. 
						
					if (n%2 == 1) {
						nextDir = rotate(originalDir, n/2 + 1, true);
					}
					else {
						nextDir = rotate(originalDir, (n-1)/2 + 1, false);
					}
					
					potentialPos = position.nextPosition(nextDir);
					
				}
				
				else {
					clearPath = true;
				}
			
			}
			*/
			pos = potentialPos;
			//System.out.println("Next position is");
			//System.out.print(potentialPos.latitude);
			//System.out.print(' ');
			//System.out.println(potentialPos.longitude);
			path.add(new Move(nextDir, 0.0, 1.25, null, 0.0));
			//System.out.println("Distance to target");
			//System.out.println(Math.pow(targetLong - position.longitude, 2) + Math.pow(targetLat - position.latitude, 2));
		}
				
		// Adding the feature power and coin delta to the last move, the one
		// that reaches the target feature.
		int indexLast = path.size() - 1;
		double targetCoinD = target.getProperty("coins").getAsDouble();
		double targetPowerD = 1.25 + target.getProperty("power").getAsDouble();
		Move lastMove = path.get(indexLast);
		Move newLastMove = new Move(lastMove.direction, targetCoinD, targetPowerD, target, 0.0);
		path.remove(indexLast);
		path.add(newLastMove);
		
		// Updating the visited f
		this.visitedFeatures.add(target);
		this.featuresToVisit.remove(target);
		
		return path;
		
	}
	
	Direction getNextDirection(double targetLat, double targetLong, Position pos) {
		double minDist = Double.POSITIVE_INFINITY;
		
		Direction minDir = null;
		for (Direction direction: Direction.values()) {
			Position newPos = pos.nextPosition(direction);
			double dist = Math.pow(targetLong - newPos.longitude, 2) + Math.pow(targetLat - newPos.latitude, 2);
			//try {
		    //    TimeUnit.SECONDS.sleep(1);
		    //} catch (InterruptedException e) {
		    //    e.printStackTrace();
		    //}
			//System.out.println(dist);
			//System.out.print(": ");
			
			if (dist < minDist) {
				minDist = dist;
				minDir = direction;
			}
			
		}
		//System.out.print("DIRECTION DIST: ");
		//System.out.println(Math.sqrt(minDist));
		return minDir;
	}
	
	boolean checkInRange(Position potentialMove, ArrayList<Position> coordinatesAvoid) {
		for (Position pos : coordinatesAvoid) {
			if (inReach(potentialMove, pos.longitude, pos.latitude)) {
				return true;
			}
		}
		
		return false;
	}
	
	ArrayList<Position> getCoordinatesAvoid() {
		ArrayList<Position> coordinates = new ArrayList<Position>();
		for (Feature featureAvoid : featuresAvoid) {
			coordinates.add(new Position(((Point) featureAvoid.geometry()).coordinates().get(0), ((Point) featureAvoid.geometry()).coordinates().get(1)));
		}
		return coordinates;
	}
	
	Direction rotate(Direction dir, int n, boolean clockwise) {
		if (clockwise == true) {
			return Direction.values()[dir.ordinal() + n];
		}
		else {
			return Direction.values()[dir.ordinal() - n];
		}
	}
	
	Direction getDirFromAngle(double angle) {
		angle = Math.toDegrees(angle) - 90;
		//System.out.println(angle);
		// We do not want negative angles, and we need to rotate it by 90 degrees
		angle = (angle < 0 ? 360 + angle: angle);
		Direction dir = Direction.values()[(int) Math.round(angle / 22.5) == 16 ? 0 : (int) Math.round(angle / 22.5) ];
		//System.out.println(dir.name());
		return dir;
	}
	
	boolean inReach(Position pos, double longitude, double latitude) {
		//System.out.println(Math.pow(longitude - pos.longitude, 2) + Math.pow(latitude - pos.latitude, 2));
		return (Math.pow(longitude - pos.longitude, 2) + Math.pow(latitude - pos.latitude, 2) <= Math.pow(0.00025, 2));
		
		
	}
}
							
