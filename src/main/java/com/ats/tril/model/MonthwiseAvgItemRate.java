package com.ats.tril.model;

public class MonthwiseAvgItemRate {// 5FEB 2019

	private int itemId;

	private String itemCode;

	private String itemDesc;

	private String itemUom;

	private float approvedQtyCm;
	private float approvedQtyValueCm;
	private float approvedLandingValueCm;

	private float approvedQtyPm;
	private float approvedQtyValuePm;
	private float approvedLandingValuePm;

	public int getItemId() {
		return itemId;
	}

	public void setItemId(int itemId) {
		this.itemId = itemId;
	}

	public String getItemCode() {
		return itemCode;
	}

	public void setItemCode(String itemCode) {
		this.itemCode = itemCode;
	}

	public String getItemDesc() {
		return itemDesc;
	}

	public void setItemDesc(String itemDesc) {
		this.itemDesc = itemDesc;
	}

	public String getItemUom() {
		return itemUom;
	}

	public void setItemUom(String itemUom) {
		this.itemUom = itemUom;
	}

	public float getApprovedQtyCm() {
		return approvedQtyCm;
	}

	public void setApprovedQtyCm(float approvedQtyCm) {
		this.approvedQtyCm = approvedQtyCm;
	}

	public float getApprovedQtyValueCm() {
		return approvedQtyValueCm;
	}

	public void setApprovedQtyValueCm(float approvedQtyValueCm) {
		this.approvedQtyValueCm = approvedQtyValueCm;
	}

	public float getApprovedLandingValueCm() {
		return approvedLandingValueCm;
	}

	public void setApprovedLandingValueCm(float approvedLandingValueCm) {
		this.approvedLandingValueCm = approvedLandingValueCm;
	}

	public float getApprovedQtyPm() {
		return approvedQtyPm;
	}

	public void setApprovedQtyPm(float approvedQtyPm) {
		this.approvedQtyPm = approvedQtyPm;
	}

	public float getApprovedQtyValuePm() {
		return approvedQtyValuePm;
	}

	public void setApprovedQtyValuePm(float approvedQtyValuePm) {
		this.approvedQtyValuePm = approvedQtyValuePm;
	}

	public float getApprovedLandingValuePm() {
		return approvedLandingValuePm;
	}

	public void setApprovedLandingValuePm(float approvedLandingValuePm) {
		this.approvedLandingValuePm = approvedLandingValuePm;
	}

	@Override
	public String toString() {
		return "MonthwiseAvgItemRate [itemId=" + itemId + ", itemCode=" + itemCode + ", itemDesc=" + itemDesc
				+ ", itemUom=" + itemUom + ", approvedQtyCm=" + approvedQtyCm + ", approvedQtyValueCm="
				+ approvedQtyValueCm + ", approvedLandingValueCm=" + approvedLandingValueCm + ", approvedQtyPm="
				+ approvedQtyPm + ", approvedQtyValuePm=" + approvedQtyValuePm + ", approvedLandingValuePm="
				+ approvedLandingValuePm + "]";
	}

}
