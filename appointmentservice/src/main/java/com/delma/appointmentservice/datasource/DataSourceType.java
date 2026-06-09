package com.delma.appointmentservice.datasource;

public enum DataSourceType {
    PRIMARY, // handles all writes + reads inside write transactions
    REPLICA // handles all reads — getAppointmentsForUser, getAvailableSlots etc
}
