package io.github.nc.minikvstore;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

import java.util.Arrays;

import spark.route.RouteOverview;

/**
 * Starting point of the application
 * 
 * @author nilanjanc
 */
public class App {
	
	public static void main(String[] args) {
		
		if(args == null || args.length == 0) {
			throw new IllegalArgumentException("Usage: App <MiniKVStore_cluster_port(:default=0)> <REST port>");
		}
		CacheController controller = CacheController.getInstance(Arrays.copyOf(args, args.length));
		final String sparkPort = args.length > 1 ? args[1] : args[0];  		 
		port(Integer.parseInt(sparkPort));
		post("/set/:key", controller::set);
		get("/get/:key", controller::get);
		
        RouteOverview.enableRouteOverview("/route/overview");
	}
}
