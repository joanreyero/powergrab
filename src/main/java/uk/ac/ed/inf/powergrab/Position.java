package uk.ac.ed.inf.powergrab;

public class Position {
	public double latitude;
	public double longitude;
			
	public Position(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public Position nextPosition(Direction direction) {
		double r = 0.0003;
		double degree = direction.ordinal() * 22.5;
		
		double newLong = longitude + r * Math.cos(Math.toRadians(90 - degree));
		double newLat = latitude + r * Math.sin(Math.toRadians(90 - degree));
		return new Position(newLat, newLong);
	}
	
	public boolean inPlayArea() {
		// If the latitude is within limits and
		if (this.latitude < 55.946233 && this.latitude > 55.942617 &&
		// the longitude is as well
				this.longitude > -3.192473 && this.longitude < -3.184319) {
			return true;
		}
		else {
			return false;
		}
	}
	
	
    public static void main( String[] args )
    {
        Position pos = new Position(3, 4);
        Position pos2 = pos.nextPosition(Direction.N);
        System.out.println(pos2.latitude);
    }
}

