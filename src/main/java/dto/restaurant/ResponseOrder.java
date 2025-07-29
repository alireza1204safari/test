package dto.restaurant;

import entity.OrderItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ResponseOrder {
    private Long id;
    private String deliveryAddress;
    private Long customerId;
    private Long vendorId;
    private Long couponId;
    private List<Long> itemIds;
    private Integer rawPrice;
    private Integer taxFee;
    private Integer additionalFee;
    private Integer courierFee;
    private Integer payPrice;
    private Long courierId;
    private String status;
    private String createdAt;
    private String updatedAt;

    public ResponseOrder(Long id, String deliveryAddress, Long customerId, Long vendorId, Long couponId,
                         List<OrderItem> items, Integer rawPrice, Integer taxFee, Integer additionalFee,
                         Integer courierFee, Integer payPrice, Long courierId, String status,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.deliveryAddress = deliveryAddress;
        this.customerId = customerId;
        this.vendorId = vendorId;
        this.couponId = couponId;
        this.itemIds = items != null ? items.stream().map(item -> item.getFoodItem().getId()).collect(Collectors.toList()) : null;
        this.rawPrice = rawPrice;
        this.taxFee = taxFee;
        this.additionalFee = additionalFee;
        this.courierFee = courierFee;
        this.payPrice = payPrice;
        this.courierId = courierId;
        this.status = status;
        this.createdAt = createdAt != null ? createdAt.toString() : null;
        this.updatedAt = updatedAt != null ? updatedAt.toString() : null;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }
    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }
    public List<Long> getItemIds() { return itemIds; }
    public void setItemIds(List<Long> itemIds) { this.itemIds = itemIds; }
    public Integer getRawPrice() { return rawPrice; }
    public void setRawPrice(Integer rawPrice) { this.rawPrice = rawPrice; }
    public Integer getTaxFee() { return taxFee; }
    public void setTaxFee(Integer taxFee) { this.taxFee = taxFee; }
    public Integer getAdditionalFee() { return additionalFee; }
    public void setAdditionalFee(Integer additionalFee) { this.additionalFee = additionalFee; }
    public Integer getCourierFee() { return courierFee; }
    public void setCourierFee(Integer courierFee) { this.courierFee = courierFee; }
    public Integer getPayPrice() { return payPrice; }
    public void setPayPrice(Integer payPrice) { this.payPrice = payPrice; }
    public Long getCourierId() { return courierId; }
    public void setCourierId(Long courierId) { this.courierId = courierId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}