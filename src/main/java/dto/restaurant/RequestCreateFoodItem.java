package dto.restaurant;

import java.util.List;

public class RequestCreateFoodItem {
    private String name;
    private String description;
    private Integer price;
    private Integer supply;
    private String imageBase64;
    private List<String> keywords;
    private Long vendor_id; // Added to match OpenAPI spec

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }
    public Integer getSupply() { return supply; }
    public void setSupply(Integer supply) { this.supply = supply; }
    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    public Long getVendorId() { return vendor_id; }
    public void setVendorId(Long vendorId) { this.vendor_id = vendorId; }
}