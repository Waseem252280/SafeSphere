package com.example.safesphere.dto;
public class FamilyCircleDto {
    private String userId;
    private String familyMemberUserId;
    private String adminId;
    private String relation;

    public FamilyCircleDto() {}

    public FamilyCircleDto(String userId, String familyMemberUserId, String relation, String adminId) {
        this.userId = userId;
        this.familyMemberUserId = familyMemberUserId;
        this.relation = relation;
        this.adminId = adminId;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFamilyMemberUserId() { return familyMemberUserId; }
    public void setFamilyMemberUserId(String familyMemberUserId) { this.familyMemberUserId = familyMemberUserId; }

    public String getRelation() { return relation; }
    public void setRelation(String relation) { this.relation = relation; }
}
