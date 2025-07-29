package entity;

import jakarta.persistence.*;

@Entity
@Table(name = "restaurants")
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "logo_base64")
    private String logoBase64;

    @Column(name = "tax_fee", nullable = false)
    private Integer taxFee;

    @Column(name = "additional_fee", nullable = false)
    private Integer additionalFee;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User vendor; // کاربر با نقش seller

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ResStatus status;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getLogoBase64() { return logoBase64; }
    public void setLogoBase64(String logoBase64) { this.logoBase64 = logoBase64; }
    public Integer getTaxFee() { return taxFee; }
    public void setTaxFee(Integer taxFee) { this.taxFee = taxFee; }
    public Integer getAdditionalFee() { return additionalFee; }
    public void setAdditionalFee(Integer additionalFee) { this.additionalFee = additionalFee; }
    public User getVendor() { return vendor; }
    public void setVendor(User vendor) { this.vendor = vendor; }
    public ResStatus getStatus() { return status; }
    public void setStatus(ResStatus status) { this.status = status; }
}