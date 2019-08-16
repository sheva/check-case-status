package com.essheva;

public class ReceiptRecord implements Cloneable {

    private String owner;
    private String receiptNumber;
    private String mainStatus;
    private String description;

    ReceiptRecord(String owner, String receiptNumber) {
        this.owner = owner;
        this.receiptNumber = receiptNumber;
    }

    String getOwner() {
        return owner;
    }

    void setOwner(String owner) {
        this.owner = owner;
    }

    String getReceiptNumber() {
        return receiptNumber;
    }

    void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    String getMainStatus() {
        return mainStatus;
    }

    void setMainStatus(String mainStatus) {
        this.mainStatus = mainStatus;
    }

    String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReceiptRecord receiptRecord = (ReceiptRecord) o;

        if (!owner.equals(receiptRecord.owner)) return false;
        if (!receiptNumber.equals(receiptRecord.receiptNumber)) return false;
        if (mainStatus != null ? !mainStatus.equals(receiptRecord.mainStatus) : receiptRecord.mainStatus != null) return false;
        return description != null ? description.equals(receiptRecord.description) : receiptRecord.description == null;
    }

    @Override
    public int hashCode() {
        int result = owner.hashCode();
        result = 31 * result + receiptNumber.hashCode();
        result = 31 * result + (mainStatus != null ? mainStatus.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ReceiptRecord {" +
                "owner='" + owner + '\'' +
                ", receiptNumber='" + receiptNumber + '\'' +
                ", mainStatus='" + mainStatus + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
