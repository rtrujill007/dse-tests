/*
 * (C) Copyright 2017 David Jennings
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     David Jennings
 */
package com.esri.rttest.send;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/*
 * Sends planes dummy data to Cassandra at a specific rate.
 */
public class Cassandra
{

    private static final Logger LOG = LogManager.getLogger(Cassandra.class);

    private String cassandraHost;
    private String keyspace;
    private String tableName;
    private CassandraBulkLoader bulkLoader;

    public Cassandra(String cassandraHosts, String keyspace, String tableName, int numOfThreads, boolean useSolr, boolean storeGeo) {
      this.cassandraHost = cassandraHosts;
      this.keyspace = keyspace;
      this.tableName = tableName;

      // add the shutdown hook
      Runtime.getRuntime().addShutdownHook(new Thread(this::closeCassandraSession));

      // init - create the table
      init(numOfThreads, useSolr, storeGeo);
    }

    private Cluster cluster = null;

    /**
     * Creates or fetches the existing Cassandra Session
     * @return Session
     */
    private Session createOrGetCassandraSession()
    {
      if (cluster == null || cluster.isClosed())
      {
        cluster = Cluster.builder().addContactPoint(cassandraHost).build();
      }
      return cluster.connect();
    }

    /**
     * Closes the cassandra session
     */
    public void closeCassandraSession() {
      if (cluster != null)
        cluster.close();
    }

    private void init(int numOfThreads, boolean useSolr, boolean storeGeo) {
      final String insertCQL =
          "INSERT INTO " + keyspace + "." + tableName +
              " (id, ts, speed, dist, bearing, rtid, orig, dest, secstodep, lon, lat, geometry) " +
              " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      bulkLoader = new CassandraBulkLoader(numOfThreads, insertCQL, cassandraHost);

      String actualTableName = keyspace + "." + tableName;
      Session session = createOrGetCassandraSession();
      session.execute("DROP KEYSPACE IF EXISTS " + keyspace);
      session.execute("CREATE KEYSPACE IF NOT EXISTS "+ keyspace + " WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1 }");
      session.execute("DROP TABLE IF EXISTS " + actualTableName);

      // FiXME: Dynamically create the CREATE TABLE sql based on schema
      session.execute(
          " CREATE TABLE IF NOT EXISTS " + actualTableName +
              "( " +
               "   id text,"+
               "   ts timestamp," +
               "   speed double, " +
               "   dist double, " +
               "   bearing double, " +
               "   rtid int, " +
               "   orig text, " +
               "   dest text, " +
               "   secstodep int, " +
               "   lon double, " +
               "   lat double, " +
               "   geometry text, " +

               "   PRIMARY KEY (id, ts)" +
               ")"
      );

      if (useSolr) {
        //
        // NOTE: LOOK AT THE SOFT COMMIT INTERVAL IN SOLR
        //
        // enable search on all fields (except geometry)
        session.execute(
            "CREATE SEARCH INDEX ON " + actualTableName + "\n" +
             "WITH COLUMNS\n" +
                "  id,\n" +
                "  ts,\n" +
                "  speed,\n" +
                "  dist,\n" +
                "  bearing,\n" +
                "  rtid,\n" +
                "  orig,\n" +
                "  dest,\n" +
                "  secstodep,\n" +
                "  lon,\n" +
                "  lat;"
          );

        // check if we want to store the Geo
        if (storeGeo) {
          // enable search on geometry field
          session.execute(
            "ALTER SEARCH INDEX SCHEMA ON " + actualTableName + "\n" +
                "ADD types.fieldType[ @name='rpt',\n" +
                "                     @class='solr.SpatialRecursivePrefixTreeFieldType',\n" +
                "                     @geo='false',\n" +
                "                     @worldBounds='ENVELOPE(-1000, 1000, 1000, -1000)',\n" +
                "                     @maxDistErr='0.001',\n" +
                "                     @distanceUnits='degrees' ];"
          );
          session.execute(
              "ALTER SEARCH INDEX SCHEMA ON "+ actualTableName + "\n" +
                  "ADD fields.field[ @name='geometry',\n" +
                  "                  @type='rpt',\n" +
                  "                  @indexed='true',\n" +
                  "                  @stored='true' ];"
          );
          session.execute(
              "RELOAD SEARCH INDEX ON " + actualTableName
          );
        }
      }
    }
    /**
     * 
     * @param rate Rate in lines per second to send.
     * @param numToSend Number of lines to send. If more than number of lines in file will resend from start.
     * @param burstDelay Number of milliseconds to burst at; set to 0 to send one line at a time
     */
    public void sendPlanes(Integer rate, Integer numToSend, Integer burstDelay) {
        try {


            // Get the System Time
            Long st = System.currentTimeMillis();

            Integer cnt = 0;

            // Tweak used to adjust delays to try and get requested rate
            Long tweak = 0L;

            /*
                For rates < 100/s burst is better
                For rates > 100/s continous is better
            */

            List<Object[]> recordsToSend = new ArrayList<>();
              int sampleDataCounter = 0;

            // *********** SEND Constant Rate using nanosecond delay *********
            if (burstDelay == 0) {
                // Delay between each send in nano seconds            
                Double ns_delay = 1000000000.0 / (double) rate;
                
                // By adding some to the delay you can fine tune to achieve the desired output
                ns_delay = ns_delay - (tweak * 100);
                
                long ns = ns_delay.longValue();
                if (ns < 0) ns = 0;  // can't be less than 0 

                while (cnt < numToSend) {
                    
                   if (cnt % rate == 0 && cnt > 0) {

                        // flush here
                        bulkLoader.ingest(recordsToSend.listIterator());
                        recordsToSend.clear();

                        // Calculate rate and adjust as needed
                        Double curRate = (double) cnt / (System.currentTimeMillis() - st) * 1000;
                        System.out.println(cnt + "," + String.format("%.0f", curRate));
                    }

                    if (cnt % 1000 == 0 && cnt > 0) {
                        // Calculate rate and adjust as needed
                        Double curRate = (double) cnt / (System.currentTimeMillis() - st) * 1000;

                        // rate difference as percentage 
                        Double rateDiff = (rate - curRate) / rate;

                        // Add or subracts up to 100ns 
                        tweak = (long) (rateDiff * rate);

                        // By adding some to the delay you can fine tune to achieve the desired output
                        ns = ns - tweak;
                        if (ns < 0) {
                            ns = 0;  // can't be less than 0 
                        }

                    }                             
                    
                    cnt += 1;

                    // create the plane record to send
                    Plane plane = planeData.get(sampleDataCounter);
                    sampleDataCounter++;
                    if (sampleDataCounter >= planeData.size()) {
                      sampleDataCounter = 0;
                    }

                    String id = getUUID();
                    long ts = System.currentTimeMillis();
                    Object[] record = new Object[]{id, new Date(ts), plane.getSpeed(), plane.getDist(), plane.getBearing(), plane.getRtid(), plane.getOrig(), plane.getDest(), plane.getSecsToDep(), plane.getLongitude(), plane.getLatitude(), plane.getGeometry()};
                    recordsToSend.add(record);

                    final long stime = System.nanoTime();
                    long etime;
                    do {
                        // This approach uses a lot of CPU                    
                        etime = System.nanoTime();
                        // Adding the following sleep for a few microsecond reduces the load
                        // However, it also effects the through put
                        //Thread.sleep(0,100);  
                    } while (stime + ns >= etime);                

                }
            } else {
                // *********** SEND in bursts every msDelay ms  *********

                Integer msDelay = burstDelay;
                Integer numPerBurst = Math.round(rate / 1000 * msDelay); 

                if (numPerBurst < 1) numPerBurst = 1;
                
                Integer delay = burstDelay;

                while (cnt < numToSend) {
                    
                   // Adjust delay every burst
                    Double curRate = (double) cnt / (System.currentTimeMillis() - st) * 1000;
                    Double rateDiff = (rate - curRate) / rate;
                    tweak = (long) (rateDiff * rate);
                    delay = delay - Math.round(tweak / 1000.0f);
                    if (delay < 0) {
                        delay = 0;  // delay cannot be negative
                    } else {
                        Thread.sleep(delay);
                    }
                                        

                    Integer i = 0;
                    while (i < numPerBurst) {
                        if (cnt % rate == 0 && cnt > 0) {

                            // flush here
                            bulkLoader.ingest(recordsToSend.listIterator());
                            recordsToSend.clear();

                            curRate = (double) cnt / (System.currentTimeMillis() - st) * 1000;
                            System.out.println(cnt + "," + String.format("%.0f", curRate));
                        }                        
                        
                        cnt += 1;
                        
                        i += 1;

                        // create the plane record to send
                        Plane plane = planeData.get(sampleDataCounter);
                        sampleDataCounter++;
                        if (sampleDataCounter >= planeData.size()) {
                          sampleDataCounter = 0;
                        }

                        String id = getUUID();
                        long ts = System.currentTimeMillis();
                        Object[] record = new Object[]{id, ts, plane.getSpeed(), plane.getDist(), plane.getBearing(), plane.getRtid(), plane.getOrig(), plane.getDest(), plane.getSecsToDep(), plane.getLongitude(), plane.getLatitude(), plane.getGeometry()};
                        recordsToSend.add(record);

                        // Break out as soon as numToSend is reached
                        if (cnt >= numToSend) {
                            break;
                        }                           
                        
                    }

                }
            }

            // flush here
            if (!recordsToSend.isEmpty())
              bulkLoader.ingest(recordsToSend.listIterator());
            bulkLoader.close();

            Double sendRate = (double) cnt / (System.currentTimeMillis() - st) * 1000;
            System.out.println(cnt + "," + String.format("%.0f", sendRate));
                             
        } catch (Exception error) {
            // Could fail on very large files that would fill heap space
            LOG.error("ERROR", error);
        }
    }
    
    public static void main(String args[]) throws Exception {
        if (args.length != 8 && args.length != 9) {
            System.err.print("Usage: Cassandra <host-names> <keyspace> <tablename> <useSolr> <storeGeo> <rate> <numrecords> (<burst-delay-ms>)\n");
        } else {
            String hostNames = args[0];
            String keyspace = args[1];
            String tableName = args[2];
            int numOfThreads = Integer.parseInt(args[3]);
            boolean useSolr = Boolean.parseBoolean(args[4]);
            boolean storeGeo = Boolean.parseBoolean(args[5]);
            int rate = Integer.parseInt(args[6]);
            int numOfRecords = Integer.parseInt(args[7]);
            int burstDelay = 0;

            Cassandra cassandra = new Cassandra(hostNames, keyspace, tableName, numOfThreads, useSolr, storeGeo);
            if (args.length == 9)
            {
              burstDelay = Integer.parseInt(args[8]);
            }
            cassandra.sendPlanes(rate, numOfRecords, burstDelay);
            cassandra.closeCassandraSession();
        }
    }

    /**
     * Plane Sample Data
     */
    private static List<Plane> planeData = new ArrayList<>();
    static {
      planeData.add(new Plane(0,1506957079575L,240.25,5024.32,-70.72,1,"Mielec Airport","Frank Pais International Airport",-1,-31.88592,49.21297));
      planeData.add(new Plane(1,1506957079575L,292.55,1126.25,-128.79,2,"Covilhã Airport","Tangará da Serra Airport",-1,-51.19537,-6.60298));
      planeData.add(new Plane(2,1506957079575L,225.7,323.55,-77.1,3,"Tari Airport","Bougouni Airport",-1,-4.57528,11.85789));
      planeData.add(new Plane(3,1506957079575L,221.87,4744.88,16.74,4,"Salalah Airport","Mountain Village Airport",-1,86.93716,64.99481));
      planeData.add(new Plane(4,1506957079575L,209.99,9467.71,-6.54,5,"Vanua Balavu Airport","Nancy-Essey Airport",-1,172.69416,45.42308));
      planeData.add(new Plane(5,1506957079575L,165.11,8887.07,-98.09,6,"Yariguíes Airport","Misima Island Airport",-1,-126.97885,-2.33721));
      planeData.add(new Plane(6,1506957079575L,152.33,5461.39,75.08,7,"Værøy Heliport","Maumere(Wai Oti) Airport",-1,101.76599,36.55492));
      planeData.add(new Plane(7,1506957079575L,296.85,7216.26,-58.07,8,"Moulay Ali Cherif Airport","Lawton Fort Sill Regional Airport",-1,-15.62401,37.04034));
      planeData.add(new Plane(8,1506957079575L,367.49,4300.45,-3.9,9,"Biysk Airport","Dane County Regional Truax Field",-1,-75.40851,81.52389));
      planeData.add(new Plane(9,1506957079575L,245.77,7330.32,-11.12,10,"Malamala Airport","Notodden Airport",-1,27.66888,-4.71715));
      planeData.add(new Plane(10,1506957079575L,347.63,1010.71,-27.37,11,"Tatoi Airport","Cut Bank International Airport",-1,-103.50368,55.91659));
      planeData.add(new Plane(11,1506957079575L,307.74,4277.81,99.48,12,"Tela Airport","Benguela Airport",-1,-23.85246,-1.71326));
      planeData.add(new Plane(12,1506957079575L,195.9,1007.97,28.14,13,"El Jagüel / Punta del Este Airport","Örnsköldsvik Airport",-1,4.13208,57.94306));
      planeData.add(new Plane(13,1506957079575L,206.51,6773.04,93.04,14,"Soalala Airport","Kaimana Airport",-1,72.91514,-15.72307));
      planeData.add(new Plane(14,1506957079575L,132.3,1271.83,44.13,15,"Wemindji Airport","Muş Airport",-1,32.48871,48.06576));
      planeData.add(new Plane(15,1506957079575L,168.94,5575.18,32.01,16,"Higuerote Airport","Oban Airport",-1,-57.39709,23.19267));
      planeData.add(new Plane(16,1506957079575L,178.22,5410.15,-28.27,17,"Bowerman Airport","Taplejung Airport",-1,133.51189,67.61094));
      planeData.add(new Plane(17,1506957079575L,280.06,5085.02,-19.23,18,"Chanute Martin Johnson Airport","Khok Kathiam Airport",-1,121.87841,57.7363));
      planeData.add(new Plane(18,1506957079575L,244.78,7146.83,66.36,19,"Brisbane West Wellcamp Airport","Pecos Municipal Airport",-1,-162.49917,-0.63096));
      planeData.add(new Plane(19,1506957079575L,209.05,6188.91,71.88,20,"Sydney Kingsford Smith International Airport","Person County Airport",-1,-134.41428,11.25938));
      planeData.add(new Plane(20,1506957079575L,217.2,5605.05,124.43,21,"Yuendumu Airport","Del Caribe Santiago Mariño International Airport",-1,-103.82463,-20.81007));
      planeData.add(new Plane(21,1506957079575L,235.21,5517.52,-14.23,22,"Woomera Airfield","Qinhuangdao Beidaihe Airport",-1,131.2204,-8.6567));
      planeData.add(new Plane(22,1506957079575L,205.18,1942.5,-40.94,23,"Leczyca Military Air Base","Kangiqsujuaq (Wakeham Bay) Airport",-1,-32.88826,66.17023));
      planeData.add(new Plane(23,1506957079575L,263.62,7711.53,25.37,24,"W H 'Bud' Barron Airport","Salem Airport",-1,13.96448,67.43835));
      planeData.add(new Plane(24,1506957079575L,294.86,5870.31,6.55,25,"Subic Bay International Airport","Northern Aroostook Regional Airport",-1,149.44668,77.78985));
      planeData.add(new Plane(25,1506957079575L,242.7,7460.3,32.43,26,"Auckland International Airport","Dawson Creek Airport",-1,-164.67954,-0.84773));
      planeData.add(new Plane(26,1506957079575L,248.43,11796.91,120.91,27,"Hokitika Airfield","Eduardo Gomes International Airport",-1,-177.59073,-46.83888));
      planeData.add(new Plane(27,1506957079575L,255.57,2345.81,91.56,28,"Hunter Army Air Field","Ngot Nzoungou Airport",-1,-5.28939,6.93283));
      planeData.add(new Plane(28,1506957079575L,255.07,1493.5,69.56,29,"Montluçon-Domérat Airport","Volgograd International Airport",-1,23.77391,49.54464));
      planeData.add(new Plane(29,1506957079575L,315.97,2077.94,79.96,30,"Moron Airport","Sir Bani Yas Airport",-1,35.45796,14.80259));
      planeData.add(new Plane(30,1506957079575L,286.81,509.25,-38.86,31,"Guangzhou MR Air Base","Grefrath-Niershorst Airport",-1,12.90891,53.58614));
      planeData.add(new Plane(31,1506957079575L,206.93,814.82,29.14,32,"Vila Bela da Santíssima Trindade Airport","Petrozavodsk Airport",-1,19.13779,60.5755));
      planeData.add(new Plane(32,1506957079575L,299.35,8167.4,73.13,33,"Stóra Dímun Heliport","Pinang Kampai Airport",-1,42.35699,59.51048));
      planeData.add(new Plane(33,1506957079575L,277.38,3965.3,-57.55,34,"Daman Airport","Del Caribe Santiago Mariño International Airport",-1,-31.3046,29.55053));
      planeData.add(new Plane(34,1506957079575L,0,5985.15,-38.88,35,"Naha Airport","Shijiazhuang Daguocun International Airport",492,127.646,26.1958));
      planeData.add(new Plane(35,1506957079575L,241.59,6799.57,21.16,36,"Thumamah Airport","Markovo Airport",-1,55.87737,42.71259));
      planeData.add(new Plane(36,1506957079575L,167.01,930,90.53,37,"Vöslau Airport","Maimana Airport",-1,55.68704,40.3009));
      planeData.add(new Plane(37,1506957079575L,151.94,1158.21,87.55,38,"Campbell River Airport","Cape May County Airport",-1,-86.96415,44.26531));
      planeData.add(new Plane(38,1506957079575L,292.29,11579.08,107.16,39,"Lokichoggio Airport","Walaha Airport",-1,61.71239,-4.40654));
      planeData.add(new Plane(39,1506957079575L,234.42,3938.25,72.35,40,"Memanbetsu Airport","Santa Teresita Airport",-1,-87.44238,-14.10468));
      planeData.add(new Plane(40,1506957079575L,250.03,3969.36,-44.33,41,"Tarama Airport","Cagliari Elmas Airport",-1,57.94278,50.56698));
      planeData.add(new Plane(41,1506957079575L,267.18,9441.45,110.12,42,"Süleyman Demirel International Airport","Kadina Airport",-1,67.59346,18.68907));
      planeData.add(new Plane(42,1506957079575L,223.48,3046.13,41.34,43,"Beauregard Regional Airport","Nis Airport",-1,-16.833,54.96503));
      planeData.add(new Plane(43,1506957079575L,174.65,3232.9,5.39,44,"Fernando Air Base","Yakutsk Airport",-1,123.27024,33.31705));
      planeData.add(new Plane(44,1506957079575L,272.04,6992.59,-128.67,45,"Mc Clellan Airfield","Pago Pago International Airport",-1,-128.2876,33.88318));
      planeData.add(new Plane(45,1506957079575L,231.47,2940.37,159.3,46,"Burlington Alamance Regional Airport","Paso De Los Libres Airport",-1,-65.60097,-4.48224));
      planeData.add(new Plane(46,1506957079575L,331.21,3017.66,40.56,47,"Comox Airport","Valencia Airport",-1,-29.74583,59.44034));
      planeData.add(new Plane(47,1506957079575L,187.68,1043.14,50.18,48,"Billy Bishop Toronto City Centre Airport","Hinterstoisser Air Base",-1,2.24248,51.9622));
      planeData.add(new Plane(48,1506957079575L,221.11,12270.56,74.18,49,"Capitan D Daniel Vazquez Airport","Erebuni Airport",-1,-39.63047,-39.35153));
      planeData.add(new Plane(49,1506957079575L,0,14504.76,-87.69,50,"Memanbetsu Airport","Joshua Mqabuko Nkomo International Airport",806,144.164,43.8806));
      planeData.add(new Plane(50,1506957079575L,315.59,3564.9,14,51,"Susuman Airport","Itaperuna Airport",-1,-45.55394,10.63638));
      planeData.add(new Plane(51,1506957079575L,207.71,8325.05,-31.32,52,"Cortez Municipal Airport","Chongqing Jiangbei International Airport",-1,-140.46033,60.0689));
      planeData.add(new Plane(52,1506957079575L,227.28,4061.56,21.76,53,"Sparrevohn LRRS Airport","Getafe Air Base",-1,-34.07988,73.93589));
      planeData.add(new Plane(53,1506957079575L,295.02,2315.86,31.04,54,"Northway Airport","Grise Fiord Airport",-1,-139.34132,64.77117));
      planeData.add(new Plane(54,1506957079575L,185.19,9050.25,96.78,55,"Moenjodaro Airport","Koro Island Airport",-1,105.04432,18.4167));
      planeData.add(new Plane(55,1506957079575L,254.34,9177.19,-79.55,56,"Oakland County International Airport","Hayman Island Heliport",-1,-145.07252,33.35753));
      planeData.add(new Plane(56,1506957079575L,253.11,5195.67,-51.21,57,"Drachten Airport","Watertown Regional Airport",-1,-18.84443,60.5841));
      planeData.add(new Plane(57,1506957079575L,312.36,1813.58,-55.59,58,"Principe Airport","Darlington County Jetport Airport",-1,-60.36333,32.83968));
      planeData.add(new Plane(58,1506957079575L,256.69,2502.82,41.8,59,"Mendi Airport","Crystal Airport",-1,-126.08639,48.37532));
      planeData.add(new Plane(59,1506957079575L,332.75,3304.84,-18.95,60,"Delta Municipal Airport","Yerbogachen Airport",-1,-172.18108,74.7427));
      planeData.add(new Plane(60,1506957079575L,278.97,3853.36,-73.1,61,"New Tempe Airport","Curtis Field",-1,-63.32856,18.9054));
      planeData.add(new Plane(61,1506957079575L,253.78,653.35,-45.23,62,"Bhavnagar Airport","Dzhermuk Airport",-1,51.9618,36.63726));
      planeData.add(new Plane(62,1506957079575L,200.78,5901.82,-69.89,63,"Walaha Airport","Rajkot Airport",-1,122.45083,4.32601));
      planeData.add(new Plane(63,1506957079575L,330.79,265.43,-25.81,64,"Americana Airport","Itaituba Airport",-1,-55.03392,-6.42676));
      planeData.add(new Plane(64,1506957079575L,210.17,10093.71,-98.8,65,"First Flight Airport","Forbes Airport",-1,-130.86699,14.45528));
      planeData.add(new Plane(65,1506957079575L,299.86,15856.63,-143.1,66,"El Embrujo Airport","Rottnest Island Airport",-1,-89.22642,2.7388));
      planeData.add(new Plane(66,1506957079575L,171.58,2097.98,-87.57,67,"Prospect Creek Airport","Okushiri Airport",-1,157.71661,56.89069));
      planeData.add(new Plane(67,1506957079575L,167.86,4249.08,39.12,68,"Licenciado Gustavo Díaz Ordaz International Airport","Melun-Villaroche Air Base",-1,-57.98846,50.69677));
      planeData.add(new Plane(68,1506957079575L,200.01,1762.24,46.74,69,"Islas Malvinas Airport","Saratov Central Airport",-1,23.16756,46.29794));
      planeData.add(new Plane(69,1506957079575L,107.75,7206.8,-72.63,70,"Hierro Airport","Licenciado Gustavo Díaz Ordaz International Airport",-1,-32.94147,31.01886));
      planeData.add(new Plane(70,1506957079575L,360.45,182.42,156.37,71,"Jiujiang Lushan Airport","Launceston Airport",-1,146.21437,-40.08956));
      planeData.add(new Plane(71,1506957079575L,274.06,8323.47,-13.61,72,"Talhar Airport","Lakeway Airpark",-1,37.9799,69.89059));
      planeData.add(new Plane(72,1506957079575L,259.97,12263.38,12.29,73,"Yichang Sanxia Airport","Pompano Beach Airpark",-1,114.85154,42.08067));
      planeData.add(new Plane(73,1506957079575L,286.92,3620.96,-6.2,74,"Rundu Airport","Hohn Airport",-1,15.40546,22.05094));
      planeData.add(new Plane(74,1506957079575L,203.45,6014.84,-58.68,75,"Vichy-Charmeil Airport","Waynesville-St. Robert Regional Forney field",-1,-14.68078,51.6296));
      planeData.add(new Plane(75,1506957079575L,289.59,3604.13,-116.79,76,"Siena-Ampugnano Airport","Centro de Lançamento de Alcântara Airport",-1,-22.32866,21.8213));
      planeData.add(new Plane(76,1506957079575L,185.96,3502.74,24.09,77,"John Wayne Airport-Orange County Airport","Cengiz Topel Airport",-1,-4.38201,65.54705));
      planeData.add(new Plane(77,1506957079575L,249.17,10120.35,55.14,78,"Queen Beatrix International Airport","Al Ain International Airport",-1,-47.97003,25.32638));
      planeData.add(new Plane(78,1506957079575L,202.7,13137.7,91.09,79,"Pampulha - Carlos Drummond de Andrade Airport","Nakhon Sawan Airport",-1,-14.94881,-18.03191));
      planeData.add(new Plane(79,1506957079575L,256.32,3302.59,45.75,80,"Juan Pablo Pérez Alfonso Airport","Ramsar Airport",-1,12.41558,44.87248));
      planeData.add(new Plane(80,1506957079575L,186.76,9728.89,-112.45,81,"Palmyra Airport","Zona da Mata Regional Airport",-1,29.7351,31.22395));
      planeData.add(new Plane(81,1506957079575L,219.45,1977.26,-114.33,82,"Maré Airport","Rottnest Island Airport",-1,136.30832,-30.55089));
      planeData.add(new Plane(82,1506957079575L,236.44,2206.48,161.38,83,"General Pedro Jose Mendez International Airport","General Villamil Airport",-1,-96.94477,18.00444));
      planeData.add(new Plane(83,1506957079575L,221.87,3073.25,-33.05,84,"Fair Isle Airport","Northwest Regional Airport Terrace-Kitimat",-1,-78.20567,73.34511));
      planeData.add(new Plane(84,1506957079575L,191.74,3457.78,-26.86,85,"Basrah International Airport","Vängsö Airport",-1,45.43578,34.17005));
      planeData.add(new Plane(85,1506957079575L,194.84,5968.58,-47.85,86,"Soroako Airport","Rota Naval Station Airport",-1,62.32602,37.0232));
      planeData.add(new Plane(86,1506957079575L,258.88,6023.11,75.5,87,"Itzehoe/Hungriger Wolf Airport","Xieng Khouang Airport",-1,47.69448,53.55626));
      planeData.add(new Plane(87,1506957079575L,217.22,4130.01,-55.44,88,"Fresno Yosemite International Airport","Tokunoshima Airport",-1,169.93511,46.70544));
      planeData.add(new Plane(88,1506957079575L,248.28,6091.3,9.06,89,"Palmer Municipal Airport","Trenčín Airport",-1,-140.14939,75.52996));
      planeData.add(new Plane(89,1506957079575L,256.52,13069.77,-18.01,90,"Natuashish Airport","Stenkol Airport",-1,-66.1598,62.82822));
      planeData.add(new Plane(90,1506957079575L,227.05,8384.26,-36.57,91,"Kooddoo Airport","DCAE Cosford Air Base",-1,70.19906,5.07656));
      planeData.add(new Plane(91,1506957079575L,136.58,6107.22,32.45,92,"Whitehorse / Erik Nielsen International Airport","Agen-La Garenne Airport",-1,-111.00001,71.20583));
      planeData.add(new Plane(92,1506957079575L,279.22,1907.52,-150,93,"Geilenkirchen Air Base","Kawass Airport",-1,-8.4539,26.81994));
      planeData.add(new Plane(93,1506957079575L,208.43,6135.05,-118.19,94,"Robertson Airport","Olavarria Airport",-1,10.7961,-37.35901));
      planeData.add(new Plane(94,1506957079575L,261.4,4091.64,57.51,95,"Deer Lake Airport","Berlin-Tempelhof International Airport",-1,-48.83074,52.24259));
      planeData.add(new Plane(95,1506957079575L,251.93,387.47,61.41,96,"Montluçon-Guéret Airport","Yibin Caiba Airport",-1,101.71895,31.28241));
      planeData.add(new Plane(96,1506957079575L,180.73,6073.07,-63.96,97,"Milano-Bresso Airport","El Tajín National Airport",-1,-38.4828,50.23677));
      planeData.add(new Plane(97,1506957079575L,308.99,759.41,-90.53,98,"Konduz Airport","Mashhad International Airport",-1,68.1176,36.65653));
      planeData.add(new Plane(98,1506957079575L,214.27,5549.49,11.13,99,"Sparrevohn LRRS Airport","Siegerland Airport",-1,-139.54896,77.85065));
      planeData.add(new Plane(99,1506957079575L,262.16,10684.52,8.33,100,"Tlaxcala Airport","Bellary Airport",-1,-81.76749,67.27225));
      planeData.add(new Plane(100,1506957079575L,157.7,7760.79,-105.75,101,"Hunt Field","Longana Airport",-1,-138.18734,31.70385));
    }

    private static Random random  = new Random();

    private static String getUUID() {
      UUID uuid = new UUID(random.nextLong(), random.nextLong());
      return uuid.toString();
    }
}
