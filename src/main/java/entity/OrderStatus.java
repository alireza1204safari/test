package entity;

public enum OrderStatus {
    submitted,
    unpaidAndCancelled,
    waitingVendor,
    cancelled,
    findingCourier,
    onTheWay,
    completed
}
