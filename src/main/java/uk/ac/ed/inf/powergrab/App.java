
package uk.ac.ed.inf.powergrab;

import java.net.URL;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class App {
	private String mapSource; // url object of map
	private Position startPosition;
	private Drone drone;
	private int seed;

	// construct a new game with given parameters
	public App(URL mapUrl, Position startPosition, int seed, String droneType, String date) throws IOException {
		
		this.startPosition = startPosition;
		this.seed = seed;
		
		this.mapSource = getMap(mapUrl);
	
		String fileNamePrefix = String.format("%s-%s.",droneType, date);

		//System.out.println(this.mapSource);

		switch (droneType) {
		case "stateless":
			drone = new Stateless(this.startPosition, this.mapSource, this.seed, fileNamePrefix);
			break;
		case "stateful":
			drone = new Stateful(this.startPosition, this.mapSource, this.seed, fileNamePrefix);
			break;
		default:
			System.out.println("Drone type does not exist.");
		}
	}

	private String getMap(URL mapUrl) throws IOException {
		InputStream is = mapUrl.openConnection().getInputStream();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(is));
			return in.lines().collect(Collectors.joining(System.lineSeparator()));

		} finally {
			is.close();
		}
	}

	public void play() throws IOException {
		while(this.drone.power > 0 && this.drone.numMoves <= 250) {
			this.drone.move();
		}
		this.drone.writeFlightPath();
		//this.drone.writeFlightPath();
	}

	public static void main(String[] args) throws IOException {

		// Construct string of URL to download the map from
		String mapString = String.format("http://homepages.inf.ed.ac.uk/stg/powergrab/%s/%s/%s/powergrabmap.geojson",
				args[2], args[1], args[0]);
		String date = String.format("%s-%s-%s",args[0],args[1],args[2]);

		// Construct an URL object from mapString
		URL mapUrl = new URL(mapString);

		Position startPosition = new Position(Double.parseDouble(args[3]), Double.parseDouble(args[4]));

		App game = new App(mapUrl, startPosition, Integer.parseInt(args[5]), args[6], date);
		game.play();
	}

}