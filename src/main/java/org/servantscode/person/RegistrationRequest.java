package org.servantscode.person;

import java.time.ZonedDateTime;

public class RegistrationRequest {
    public enum ApprovalStatus {REQUESTED, NEEDS_REVIEW, APPROVED, REJECTED, MERGED, APPLIED};

    private int id;

    private ZonedDateTime requestTime;
    private String familyName;
    private Family familyData;

    private int approverId;
    private String approverName;
    private ZonedDateTime approvalTime;
    private ApprovalStatus approvalStatus;

    // ----- Accessors -----
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public ZonedDateTime getRequestTime() { return requestTime; }
    public void setRequestTime(ZonedDateTime requestTime) { this.requestTime = requestTime; }

    public void setFamilyName(String familyName) { this.familyName = familyName; }
    public String getFamilyName() { return familyName; }

    public Family getFamilyData() { return familyData; }
    public void setFamilyData(Family familyData) { this.familyData = familyData; }

    public int getApproverId() { return approverId; }
    public void setApproverId(int approverId) { this.approverId = approverId; }

    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }

    public ZonedDateTime getApprovalTime() { return approvalTime; }
    public void setApprovalTime(ZonedDateTime approvalTime) { this.approvalTime = approvalTime; }

    public ApprovalStatus getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(ApprovalStatus approvalStatus) { this.approvalStatus = approvalStatus; }
}
