package dto;

import java.time.LocalDate;

public class ResponseCoupon {
    private Long id;
    private String couponCode;
    private String type;
    private Integer value;
    private Integer minPrice;
    private Integer userCount;
    private String startDate;
    private String endDate;

    public ResponseCoupon(Long id, String couponCode, String type, Integer value, Integer minPrice, Integer userCount, LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.couponCode = couponCode;
        this.type = type;
        this.value = value;
        this.minPrice = minPrice;
        this.userCount = userCount;
        this.startDate = startDate != null ? startDate.toString() : null;
        this.endDate = endDate != null ? endDate.toString() : null;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getValue() { return value; }
    public void setValue(Integer value) { this.value = value; }
    public Integer getMinPrice() { return minPrice; }
    public void setMinPrice(Integer minPrice) { this.minPrice = minPrice; }
    public Integer getUserCount() { return userCount; }
    public void setUserCount(Integer userCount) { this.userCount = userCount; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
}