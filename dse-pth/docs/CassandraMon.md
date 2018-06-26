### com.esri.rttest.mon.CassandraMon

- Monitors a Cassandra Table count and measures and reports rate of change in count.  
- When the tool starts it gets the current count and starts sampling count every sampleRateSec seconds (defaults to 5 seconds).
- When count changes the tool starts collecting sample points. 
- After collecting three points the output will use linear regression to estimate the rate of change.
- After count stops changing the final line will give the count received and the best fit linear approximation of the rate.  The last sample is excluded from the final rate calculation.
- After reporting the final count and rate the tool will continue monitoring for count changes.  Use **Ctrl-C** to stop.

<pre>
java -cp target/pth.jar com.esri.rttest.mon.CassandraMon
Usage CassandraMon [CassandraHost] [Keyspace] [TableName] (sampleRateSec=5) ((username="") (password=""))
</pre>

Example:

<pre>
java -cp target/pth.jar com.esri.rttest.mon.CassandraMon 172.17.2.5 realtime planes 60

- Cassandra running on 172.17.2.5 on default port 9042
- The keyspace name is realtime
- The table name is planes
- Sample every 60 seconds
</pre>

Example Output:
<pre>
1,1518057198604,4252762
2,1518057258692,5190695
3,1518057318586,5366873,9289
4,1518057378587,7404803,16059
5,1518057438602,7581889,14790
6,1518057498623,9640049,17215
7,1518057558585,10017645,16912
Removing: 1518057558585,10017645
7462604 , 17215.24, 2.1803
</pre>

- Sample Lines: Sample Number,System Time in Milliseconds,Count,(Rate /s)
- Final Line: Total Count Change, Rate, Rate Std Deviation 

### DC/OS

For DC/OS if you deployed DSE name "datastax-dse" then you can access via an endpoint like: `dse-0-node.datastax-dse.autoip.dcos.thisdcos.directory`

Example Command:
<pre>
java -cp target/pth.jar com.esri.rttest.mon.CassandraMon dse-0-node.datastax-dse.autoip.dcos.thisdcos.directory realtime planes 60
</pre>

**Note:** You may need to use larger sampleRateSec; to prevent false endings for slow loading data. This often happens if the index refresh is disabled for bulk loading.

You will get false readings if the count goes down during loading.  For example the index is deleted before it is loaded. 

