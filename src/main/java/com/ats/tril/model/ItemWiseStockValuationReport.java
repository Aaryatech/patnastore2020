package com.ats.tril.model;

public class ItemWiseStockValuationReport {

	private int itemId;
	private int catId;
	private String catDesc;
	private int grpId;
	private String grpDesc;
	private String itemDesc;
	private float openingStock;
	private float opStockValue;
	private float approveQty;
	private float approvedQtyValue;
	private float approvedLandingValue;
	private float issueQty;
	private float issueQtyValue;
	private float issueLandingValue;
	private float returnIssueQty;
	private float damageQty;
	private float damageValue;
	private float opLandingValue;
	private float damageLandingValue;
	private float gatepassReturnQty;
	private float gatepassQty;
	public int getItemId() {
		return itemId;
	}
	public void setItemId(int itemId) {
		this.itemId = itemId;
	}
	public int getCatId() {
		return catId;
	}
	public void setCatId(int catId) {
		this.catId = catId;
	}
	public String getCatDesc() {
		return catDesc;
	}
	public void setCatDesc(String catDesc) {
		this.catDesc = catDesc;
	}
	public int getGrpId() {
		return grpId;
	}
	public void setGrpId(int grpId) {
		this.grpId = grpId;
	}
	public String getGrpDesc() {
		return grpDesc;
	}
	public void setGrpDesc(String grpDesc) {
		this.grpDesc = grpDesc;
	}
	public String getItemDesc() {
		return itemDesc;
	}
	public void setItemDesc(String itemDesc) {
		this.itemDesc = itemDesc;
	}
	public float getOpeningStock() {
		return openingStock;
	}
	public void setOpeningStock(float openingStock) {
		this.openingStock = openingStock;
	}
	public float getOpStockValue() {
		return opStockValue;
	}
	public void setOpStockValue(float opStockValue) {
		this.opStockValue = opStockValue;
	}
	public float getApproveQty() {
		return approveQty;
	}
	public void setApproveQty(float approveQty) {
		this.approveQty = approveQty;
	}
	public float getApprovedQtyValue() {
		return approvedQtyValue;
	}
	public void setApprovedQtyValue(float approvedQtyValue) {
		this.approvedQtyValue = approvedQtyValue;
	}
	public float getApprovedLandingValue() {
		return approvedLandingValue;
	}
	public void setApprovedLandingValue(float approvedLandingValue) {
		this.approvedLandingValue = approvedLandingValue;
	}
	public float getIssueQty() {
		return issueQty;
	}
	public void setIssueQty(float issueQty) {
		this.issueQty = issueQty;
	}
	public float getIssueQtyValue() {
		return issueQtyValue;
	}
	public void setIssueQtyValue(float issueQtyValue) {
		this.issueQtyValue = issueQtyValue;
	}
	public float getIssueLandingValue() {
		return issueLandingValue;
	}
	public void setIssueLandingValue(float issueLandingValue) {
		this.issueLandingValue = issueLandingValue;
	}
	public float getReturnIssueQty() {
		return returnIssueQty;
	}
	public void setReturnIssueQty(float returnIssueQty) {
		this.returnIssueQty = returnIssueQty;
	}
	public float getDamageQty() {
		return damageQty;
	}
	public void setDamageQty(float damageQty) {
		this.damageQty = damageQty;
	}
	public float getDamageValue() {
		return damageValue;
	}
	public void setDamageValue(float damageValue) {
		this.damageValue = damageValue;
	}
	public float getOpLandingValue() {
		return opLandingValue;
	}
	public void setOpLandingValue(float opLandingValue) {
		this.opLandingValue = opLandingValue;
	}
	public float getDamageLandingValue() {
		return damageLandingValue;
	}
	public void setDamageLandingValue(float damageLandingValue) {
		this.damageLandingValue = damageLandingValue;
	}
	public float getGatepassReturnQty() {
		return gatepassReturnQty;
	}
	public void setGatepassReturnQty(float gatepassReturnQty) {
		this.gatepassReturnQty = gatepassReturnQty;
	}
	public float getGatepassQty() {
		return gatepassQty;
	}
	public void setGatepassQty(float gatepassQty) {
		this.gatepassQty = gatepassQty;
	}
	@Override
	public String toString() {
		return "ItemWiseStockValuationReport [itemId=" + itemId + ", catId=" + catId + ", catDesc=" + catDesc
				+ ", grpId=" + grpId + ", grpDesc=" + grpDesc + ", itemDesc=" + itemDesc + ", openingStock="
				+ openingStock + ", opStockValue=" + opStockValue + ", approveQty=" + approveQty + ", approvedQtyValue="
				+ approvedQtyValue + ", approvedLandingValue=" + approvedLandingValue + ", issueQty=" + issueQty
				+ ", issueQtyValue=" + issueQtyValue + ", issueLandingValue=" + issueLandingValue + ", returnIssueQty="
				+ returnIssueQty + ", damageQty=" + damageQty + ", damageValue=" + damageValue + ", opLandingValue="
				+ opLandingValue + ", damageLandingValue=" + damageLandingValue + ", gatepassReturnQty="
				+ gatepassReturnQty + ", gatepassQty=" + gatepassQty + "]";
	}

	

}
