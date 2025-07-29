package entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // کاربر با نقش buyer

    @ManyToOne
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant vendor;

    @ManyToOne
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items;

    @Column(name = "raw_price", nullable = false)
    private Integer rawPrice;

    @Column(name = "tax_fee", nullable = false)
    private Integer taxFee;

    @Column(name = "additional_fee", nullable = false)
    private Integer additionalFee;

    @Column(name = "courier_fee", nullable = false)
    private Integer courierFee;

    @Column(name = "pay_price", nullable = false)
    private Integer payPrice;

    @Column(name = "courier_id")
    private Long courierId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public User getUser() { return user; }
    public void setUser(User customer) { this.user = customer; }
    public Restaurant getVendor() { return vendor; }
    public void setVendor(Restaurant vendor) { this.vendor = vendor; }
    public Coupon getCoupon() { return coupon; }
    public void setCoupon(Coupon coupon) { this.coupon = coupon; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
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
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}