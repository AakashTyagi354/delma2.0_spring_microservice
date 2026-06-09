package com.delma.appointmentservice.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

// Spring's AbstractRoutingDataSource holds a Map of datasources:
// {PRIMARY → primaryDataSource, REPLICA → replicaDataSource}
// When Spring needs a connection, it calls determineCurrentLookupKey()
// The return value is used as the Map key to find the right datasource
//
// We extend it and override ONE method — determineCurrentLookupKey()
// Everything else (connection pooling, error handling) is handled by Spring
public class RoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected  Object determineCurrentLookupKey(){
        DataSourceType type=  DataSourceContextHolder.get();
        if(type == null){
            return DataSourceType.PRIMARY;
        }
        return type;
    }
}
