# dse-spark
Sample Application using Spark/Spark Streaming to write to DSE

## Instructions

### Setting up DSE
1. Download DSE https://academy.datastax.com/quick-downloads
2. Install in a directory, follow instructions here: https://docs.datastax.com/en/install/doc/
3. Download the JTS jar to your **$DSE_HOME/resources/solr/lib directory** ex: `curl -O 'http://central.maven.org/maven2/com/vividsolutions/jts/1.13/jts-1.13.jar'`
4. Start DSE with Search: `./dse cassandra -s`
5. DSE is ready

### Running the sample simulator in IntelliJ
1. Setup this project in IntelliJ 
2. Run the `Simulator` class located in the `test` directory
3. Run the `KafkaToDSETester` tester located in the `test` directory.
4. Done

### Solr
Accessing your solr instance: http://localhost:8983/solr/


## Building and Running for DC/OS

1. Run `mvn clean install` to create the docker image: `rtrujill007/dse-spark` (if you want to change the name edit the **pom.xml**)
2. Push the docker image to docker hub: `docker push rtrujill007/dse-spark:latest`
3. Copy the [marathon.json](marathon.json)  file, configure the application to use your cluster.
4. Deploy the **dse-spark** app as a marathon app (via cmd line or UI), use your modified [marathon.json](marathon.json)
5. Done


#### References
https://docs.datastax.com/en/install/doc/
https://docs.datastax.com/en/install/doc/install/installTARdse.html
https://docs.datastax.com/en/dse/6.0/dse-dev/datastax_enterprise/spark/sparkJavaApi.html
https://docs.datastax.com/en/dse/6.0/cql/cql/cql_using/search_index/queriesGeoSpatial.html
https://github.com/payamp/dse-search-polygon-tutorial
