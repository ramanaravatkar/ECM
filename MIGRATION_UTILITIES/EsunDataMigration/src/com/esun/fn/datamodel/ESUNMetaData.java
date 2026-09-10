package com.esun.fn.datamodel;

public class ESUNMetaData {

    private String customerId;
    private String customerName;
    private String accountNumber;
    private String identityNumber;
    private String w8BenDate;
    private String passportNo;
    private String businessRegistrationCertificate;
    private String mainDocumentType;
    private String subDocumentType;
    private String accountOpeningDate;
    private String businessUnit;
    private String rmId;
    private String countryOfIncorporationBirth;
    private String accountClosed;
    private String shareholderName;
    private String typeOfUpdate;

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getIdentityNumber() {
        return identityNumber;
    }

    public void setIdentityNumber(String identityNumber) {
        this.identityNumber = identityNumber;
    }

    public String getW8BenDate() {
        return w8BenDate;
    }

    public void setW8BenDate(String w8BenDate) {
        this.w8BenDate = w8BenDate;
    }

    public String getPassportNo() {
        return passportNo;
    }

    public void setPassportNo(String passportNo) {
        this.passportNo = passportNo;
    }

    public String getBusinessRegistrationCertificate() {
        return businessRegistrationCertificate;
    }

    public void setBusinessRegistrationCertificate(String businessRegistrationCertificate) {
        this.businessRegistrationCertificate = businessRegistrationCertificate;
    }

    public String getMainDocumentType() {
        return mainDocumentType;
    }

    public void setMainDocumentType(String mainDocumentType) {
        this.mainDocumentType = mainDocumentType;
    }

    public String getSubDocumentType() {
        return subDocumentType;
    }

    public void setSubDocumentType(String subDocumentType) {
        this.subDocumentType = subDocumentType;
    }

    public String getAccountOpeningDate() {
        return accountOpeningDate;
    }

    public void setAccountOpeningDate(String accountOpeningDate) {
        this.accountOpeningDate = accountOpeningDate;
    }

    public String getBusinessUnit() {
        return businessUnit;
    }

    public void setBusinessUnit(String businessUnit) {
        this.businessUnit = businessUnit;
    }

    public String getRmId() {
        return rmId;
    }

    public void setRmId(String rmId) {
        this.rmId = rmId;
    }

    public String getCountryOfIncorporationBirth() {
        return countryOfIncorporationBirth;
    }

    public void setCountryOfIncorporationBirth(String countryOfIncorporationBirth) {
        this.countryOfIncorporationBirth = countryOfIncorporationBirth;
    }

    public String getAccountClosed() {
        return accountClosed;
    }

    public void setAccountClosed(String accountClosed) {
        this.accountClosed = accountClosed;
    }

    public String getShareholderName() {
        return shareholderName;
    }

    public void setShareholderName(String shareholderName) {
        this.shareholderName = shareholderName;
    }

    public String getTypeOfUpdate() {
        return typeOfUpdate;
    }

    public void setTypeOfUpdate(String typeOfUpdate) {
        this.typeOfUpdate = typeOfUpdate;
    }

    @Override
    public String toString() {
        return "ESUNMetaData [customerId=" + customerId
                + ", customerName=" + customerName
                + ", accountNumber=" + accountNumber
                + ", identityNumber=" + identityNumber
                + ", w8BenDate=" + w8BenDate
                + ", passportNo=" + passportNo
                + ", businessRegistrationCertificate=" + businessRegistrationCertificate
                + ", mainDocumentType=" + mainDocumentType
                + ", subDocumentType=" + subDocumentType
                + ", accountOpeningDate=" + accountOpeningDate
                + ", businessUnit=" + businessUnit
                + ", rmId=" + rmId
                + ", countryOfIncorporationBirth=" + countryOfIncorporationBirth
                + ", accountClosed=" + accountClosed
                + ", shareholderName=" + shareholderName
                + ", typeOfUpdate=" + typeOfUpdate + "]";
    }
}