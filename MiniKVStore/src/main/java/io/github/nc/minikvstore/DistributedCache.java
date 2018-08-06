package io.github.nc.minikvstore;

import static akka.cluster.ddata.Replicator.readLocal;
import static akka.cluster.ddata.Replicator.writeLocal;

import java.util.Optional;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ddata.DistributedData;
import akka.cluster.ddata.Key;
import akka.cluster.ddata.LWWMap;
import akka.cluster.ddata.LWWMapKey;
import akka.cluster.ddata.Replicator.Get;
import akka.cluster.ddata.Replicator.GetSuccess;
import akka.cluster.ddata.Replicator.NotFound;
import akka.cluster.ddata.Replicator.Update;
import akka.cluster.ddata.Replicator.UpdateResponse;
import lombok.AllArgsConstructor;
import scala.Option;

/**
 * This class holds all the logic for distributed cache.
 * A CRDT and akka cluster based implementation of distributed key value store(In memory) ,
 * which in turn uses Gossip protocol internally to disseminate data among the nodes.
 *
 * @author nilanjanc
 */
@SuppressWarnings("unchecked")
public class DistributedCache extends AbstractActor {

  @AllArgsConstructor
  static class Request {
    public final String key;
    public final ActorRef replyTo;
  }

  @AllArgsConstructor
  public static class PutIn {
    public final String key;
    public final Object value;
  }

  @AllArgsConstructor
  public static class GetFrom {
    public final String key;
  }

  public static class Cached {
    public final String key;
    public final Optional<Object> value;

    public Cached(String key, Optional<Object> value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((key == null) ? 0 : key.hashCode());
      result = prime * result + ((value == null) ? 0 : value.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Cached other = (Cached) obj;
      if (key == null) {
        if (other.key != null)
          return false;
      } else if (!key.equals(other.key))
        return false;
      if (value == null) {
        if (other.value != null)
          return false;
      } else if (!value.equals(other.value))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "Cached [key=" + key + ", value=" + value + "]";
    }

  }

  @AllArgsConstructor
  public static class Evict {
    public final String key;
  }

  public static Props props() {
    return Props.create(DistributedCache.class);
  }

  private final ActorRef replicator = DistributedData.get(context().system()).replicator();
  private final Cluster node = Cluster.get(context().system());

  @Override
  public Receive createReceive() {
    return receiveBuilder()
      .match(PutIn.class, cmd -> receivePutIn(cmd.key, cmd.value))
      .match(Evict.class, cmd -> receiveEvict(cmd.key))
      .match(GetFrom.class, cmd -> receiveGetFrom(cmd.key))
      .match(GetSuccess.class, g -> receiveSuccess((GetSuccess<LWWMap<Object>>) g))
      .match(NotFound.class, n -> receiveNotFound((NotFound<LWWMap<Object>>) n))
      .match(UpdateResponse.class, u -> {})
      .build();
  }

  private void receivePutIn(String key, Object value) {
    Update<LWWMap<Object>> update = new Update<>(dataKey(key), LWWMap.create(), writeLocal(),
        curr -> curr.put(node, key, value));
    replicator.tell(update, self());
  }

  private void receiveEvict(String key) {
    Update<LWWMap<Object>> update = new Update<>(dataKey(key), LWWMap.create(), writeLocal(),
        curr -> curr.remove(node, key));
    replicator.tell(update, self());
  }

  private void receiveGetFrom(String key) {
    Optional<Object> ctx = Optional.of(new Request(key, sender()));
    Get<LWWMap<Object>> get = new Get<>(dataKey(key), readLocal(), ctx);
    replicator.tell(get, self());
  }

  private void receiveSuccess(GetSuccess<LWWMap<Object>> g) {
    Request req = (Request) g.getRequest().get();
    Option<Object> valueOption = g.dataValue().get(req.key);
    Optional<Object> valueOptional = Optional.ofNullable(valueOption.isDefined() ? valueOption.get() : null);
    req.replyTo.tell(new Cached(req.key, valueOptional), self());
  }

  private void receiveNotFound(NotFound<LWWMap<Object>> n) {
    Request req = (Request) n.getRequest().get();
    req.replyTo.tell(new Cached(req.key, Optional.empty()), self());
  }

  private Key<LWWMap<Object>> dataKey(String entryKey) {
    return LWWMapKey.create("cache-" + Math.abs(entryKey.hashCode()) % 100);
  }


}