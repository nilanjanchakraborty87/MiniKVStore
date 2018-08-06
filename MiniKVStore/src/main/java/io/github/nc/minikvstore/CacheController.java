package io.github.nc.minikvstore;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import io.github.nc.minikvstore.DistributedCache.Cached;
import io.github.nc.minikvstore.DistributedCache.GetFrom;
import io.github.nc.minikvstore.DistributedCache.PutIn;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import spark.Request;
import spark.Response;

/**
 * A singleton class which holds the Rest APIs.
 * 
 * @author nilanjanc
 */
public class CacheController {
	
	private static CacheController controller;
	private ActorSystem actorSystem;
	private ActorRef replicationActorRef;
	private static final String DEFAULT_PORT = "0";
	
	private CacheController(String[] args) {
		final String port = args.length > 1 ? args[0] : DEFAULT_PORT;
	    System.setProperty("akka.remote.netty.tcp.port", port);
		actorSystem = ActorSystem.create("MiniKVStore");
		replicationActorRef = actorSystem.actorOf(DistributedCache.props(), "replicationActorRef");
	}
	
	public static CacheController getInstance(String args[]) {
		if(controller == null) {
			synchronized (CacheController.class) {
				if(controller == null) {
					controller = new CacheController(args);
				}
			}
		}
		return controller;
	}
	
	/**
	 * puts the data into store
	 * 
	 * @param sparkRequest
	 * @param sparkResponse
	 * @return
	 */
	public String set(Request sparkRequest, Response sparkResponse) {
		String key = sparkRequest.params(":key");
		final String value = sparkRequest.body();
		replicationActorRef.tell(new PutIn(key, value), replicationActorRef);
		return "OK";
		
	}
	
	/**
	 * retrieves the data from the store
	 * 
	 * @param sparkRequest
	 * @param sparkResponse
	 * @return
	 */
	public String get(Request sparkRequest, Response sparkResponse) throws Exception {
		String key = sparkRequest.params(":key");
		Future<Object> f = Patterns.ask(replicationActorRef,new GetFrom(key),1000L);
		Cached response = (Cached)Await.result(f, Duration.create(1, "second"));
		return response.value.isPresent() ? response.value.get().toString() : "";
	}


}