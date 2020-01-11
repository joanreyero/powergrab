
# Software architecture


The code is divided in eight classes that can be classified in three
groups:

App

:   The class `App` acts as the game controller. It parses the command
    line arguments, it chooses between stateful and stateless, it gets
    the map for the appropriate date, and it triggers the file writing.

Drone

:   The classes `Drone`, `Stateless` and `Stateful` are in charge of
    playing the game. They choose the best moves, execute them and
    update the relevant fields.

Game helpers

:   The classes `Direction`, `Move` and `Position` are helper classes.
    They are used by the drone classes in order to store directions,
    moves and positions respectively.

The Drone and Game helpers types will be explained in more detail in the
following two sections.

## Drone classes


The `Drone` class is the super-class of the other two classes,
`Stateful` and `Stateless`, which are one for each drone type. `Drone`
contains several methods used by both the `Stateless` and `Stateful`
classes:

-   A function to perform the each move of the drone, updating the
    coins, power and the stations.

-   The functions to find the best move according to a stateless
    utility, or a random move. These will be needed by the stateless
    drone to perform all movements, and by the stateful drone if it has
    already collected all possible coins.

-   The functions to write the `GeoJSON` with the path.

The `Drone` class has the functions to perform a move, but what move to
perform is determined in the `Stateless` and `Stateful` classes. They
return the best move, and the super-class `Drone` performs it.

## Game helpers

The game helper classes are used to store attributes of the game.

Direction

:   Simply stores all the possible directions so that they can be
    accessed. It is used in several occasions by the `Stateful` and
    `Stateless` classes in order to find potential next moves. It is
    also used by the `Position` class to move to a next position in a
    specified direction.

Position

:   Represents a position, with its respective coordinates. It is used
    by the `Stateful` and `Stateless` classes in order to track where
    they are and to move to a new position.

Move

:   Stores the relevant details of a move: the direction, the change in
    coins and power and whether a feature is in reach after a particular
    move. A type `Move` is what the `Stateless` and `Stateful` classes
    send back to `Drone` once they have found the best possible move, so
    that `Drone` can perform the move.

# Drone strategy


## Stateless drone

This section outlines the strategy to pick the best possible move in a
stateless manner, which will be used by the stateless drone for all
movements, and by the stateful one once all the coins have been
collected. Each possible move is guided by a utility function.

The strategy to pick the best stateless move is first to look at all
possible directions. For all the directions the utility is obtained; it
is positive if there is a positive station in reach in that direction,
negative if a negative station is in reach, and zero if no stations are
in reach. If there are no directions with a positive utility, a random
direction from the ones with zero utility will be picked. If all the
directions have a negative utility the least harmful will be picked.


## Stateful drone

The strategy for flying the drone in a stateful manner will differ from
the stateless: now the drone can keep track of which points need to be
visited and plan in advance. Thus, the strategy is divided into three
sections: choosing a station to visit, finding the best path to that
station, and returning and executing the plan.

The last stage is simple: the drone plans a sequence of moves that will
take it to the next station. It moves on that sequence until it is
empty, then it chooses a different station and repeats the process until
all stations have been reached. Once all the stations have been visited,
it moves in a stateless manner using the strategy of the stateless
drone.

### Choosing which station to visit

The optimal way to visit all the stations is to find the shortest path
that goes through all features. This is an NP-hard problem, which cannot
be solved in a straight forward manner. Therefore, an approximation must
be found.

Two different methods were considered: a greedy approach, in which the
drone always moves to the closest possible station, and building a
Monte-Carlo tree search. The greedy approach was chosen as it
already gave optimal results.

Choosing which station to visit turned into a simple problem: iterating
through the stations and picking the closest not-visited positive
station.

### Finding the best path

Finding the best path from the current station, A, to the closest one,
B, is not trivial: negative stations must be avoided to not loose
coins.

The drone loops through all possible directions and picks the one that
would get it closest to B without getting in range of any negative
stations, let dir. The distance, d, is saved to a set D and the
drone plans a move in direction dir. The procedure is repeated until
either

-   B is in range, which means that the drone has reached the target
    station,

-   or d \in D, which means that the drone has been at that exact
    point before, and therefore it entered a loop. In that case the
    station is discarded and the next closest station is picked.
