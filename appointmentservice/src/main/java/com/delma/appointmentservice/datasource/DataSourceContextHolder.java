package com.delma.appointmentservice.datasource;

// Web server uses a thread pool — threads are reused across requests
// ThreadLocal gives each thread its own isolated copy of the variable
// Thread 1 (booking request) sets PRIMARY
// Thread 2 (read request) sets REPLICA
// They don't interfere with each other
public class DataSourceContextHolder {
    private static final ThreadLocal<DataSourceType> contextHolder = new ThreadLocal<>();


    // Called by AOP aspect before method execution
    // Sets which datasource this thread should use
    public static void set(DataSourceType type){
        contextHolder.set(type);
    }


    // Called by RoutingDataSource to determine which connection to return
    // Returns null if not set — RoutingDataSource falls back to default (PRIMARY)
    public static DataSourceType get(){
        return contextHolder.get();
    }

    // called after method execution
    // If we don't clear, next request on same thread inherits stale routing
    public static void clear(){
        contextHolder.remove();
    }


}
