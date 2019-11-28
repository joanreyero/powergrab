package uk.ac.ed.inf.powergrab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;


class Stateless extends Drone {

	Stateless(Position startPosition, String source, int seed, String fileNamePrefix) throws IOException {
		super(startPosition, source, seed, fileNamePrefix);

	}
	
	Move nextMove() {
		
		// Moves that do not lead to a station.
		List<Move> randomMoves = new ArrayList<Move>();
		
		// The highest utility we have
		double highestUtility = Double.NEGATIVE_INFINITY;
		
		Move currentBestMove = null;

		for (Direction directionP : Direction.values()) {  // For every single potential direction
			// get the next potential position by moving into that direction.
			Position positionP = this.position.nextPosition(directionP);

			if (positionP.inPlayArea()) {  // we need to check if we are in bounds
				// see if there are features in reach, and if so get the move to them
				Move potentialMove = this.stationInReach(positionP, directionP);
				
				//If not, it is a 0 utility move
				if (potentialMove.utility == 0) {
					randomMoves.add(potentialMove);
				}
				
				
				// If that move has a higher utility than we have encountered yet
				else if (potentialMove.utility > highestUtility) {
					currentBestMove = potentialMove;

				}

			}

		}

		// Pick random zero-utility move if there exists one and if no station with positive utility was found

		if (currentBestMove == null || (currentBestMove.utility < 0 && randomMoves.size() > 0)) {
			System.out.println("Size" + randomMoves.size());

			currentBestMove = randomMoves.get(this.rand.nextInt(randomMoves.size() - 1));

			

		}

		return currentBestMove;

	}
}