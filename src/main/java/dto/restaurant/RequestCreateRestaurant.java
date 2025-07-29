package dto.restaurant;

public class RequestCreateRestaurant {
    private String name;
    private String address;
    private String phone;
    private String logoBase64;
    private Integer tax_fee;
    private Integer additional_fee;

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLogoBase64() {
        return logoBase64;
    }

    public void setLogoBase64(String logoBase64) {
        this.logoBase64 = logoBase64;
    }

    public Integer getTaxFee() {
        return tax_fee;
    }

    public void setTaxFee(Integer taxFee) {
        this.tax_fee = taxFee;
    }

    public Integer getAdditional_fee() {
        return additional_fee;
    }

    public void setAdditional_fee(Integer additional_fee) {
        this.additional_fee = additional_fee;
    }
}