package com.esri.rttest.send;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class CassandraBulkLoader {

  private final int threads;
  private final String contactHosts;
  private final Cluster cluster;
  private final Session session;
  private final PreparedStatement statement;
  private final ExecutorService executor;

  public CassandraBulkLoader(int threads, String insertCQL, String contactHosts){
    this.threads = threads;
    this.contactHosts = contactHosts;

    this.cluster = Cluster.builder().addContactPoints(contactHosts).build();
    this.session = cluster.newSession();
    this.statement = session.prepare(insertCQL);

    this.executor = MoreExecutors.getExitingExecutorService((ThreadPoolExecutor) Executors.newFixedThreadPool(threads));
  }

  //callback class
  public static class IngestCallback implements FutureCallback<ResultSet>
  {

    @Override
    public void onSuccess(ResultSet result) {
      //System.out.println("Done writing record " + result.toString());
      //placeholder: put any logging or on success logic here.
    }

    @Override
    public void onFailure(Throwable t) {
      //go ahead and wrap in a runtime exception for this case, but you can do logging or start counting errors.
      throw new RuntimeException(t);
    }
  }

  public void ingest(Iterator<Object[]> boundItemsIterator) throws InterruptedException
  {
    // fixed thread pool that closes on app exit
    while (boundItemsIterator.hasNext())
    {
      BoundStatement boundStatement = statement.bind(boundItemsIterator.next());
      ResultSetFuture future = session.executeAsync(boundStatement);
      Futures.addCallback(future, new IngestCallback(), executor);
    }
  }

  public void close() {
    /**
    executor.shutdown();
    try
    {
      executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }
    catch (InterruptedException e)
    {
    }
     **/

    try
    {
      session.close();
      cluster.close();
    } catch (Exception error) {

    }
  }
}
