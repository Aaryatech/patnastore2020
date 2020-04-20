package com.ats.tril.model;




public class OpeningStockModel {
	
	

	private int itemId;

	private String itemCode;

	private String itemDesc;

	private String itemDesc2;

	private String itemDesc3;

	private String itemUom;

	private String itemUom2;
	
	private Float itemOpRate;

	private Float itemOpQty;
	
	private float cgstPer;
	
	private float sgstPer;
	private float igstPer;
	
	
	float discPer;
	float discamt;
	
	
	private float basicValue; 
	private float packValue; 
	private float insu; 
	private float otherChargesBefor; 
	private float taxValue; 
	private float freightValue; 
	private float otherChargesAfter; 
	private float landingCost; 
	  
	
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
	public String getItemDesc2() {
		return itemDesc2;
	}
	public void setItemDesc2(String itemDesc2) {
		this.itemDesc2 = itemDesc2;
	}
	public String getItemDesc3() {
		return itemDesc3;
	}
	public void setItemDesc3(String itemDesc3) {
		this.itemDesc3 = itemDesc3;
	}
	public String getItemUom() {
		return itemUom;
	}
	public void setItemUom(String itemUom) {
		this.itemUom = itemUom;
	}
	public String getItemUom2() {
		return itemUom2;
	}
	public void setItemUom2(String itemUom2) {
		this.itemUom2 = itemUom2;
	}
	public Float getItemOpRate() {
		return itemOpRate;
	}
	public void setItemOpRate(Float itemOpRate) {
		this.itemOpRate = itemOpRate;
	}
	public Float getItemOpQty() {
		return itemOpQty;
	}
	public void setItemOpQty(Float itemOpQty) {
		this.itemOpQty = itemOpQty;
	}
	public float getCgstPer() {
		return cgstPer;
	}
	public void setCgstPer(float cgstPer) {
		this.cgstPer = cgstPer;
	}
	public float getSgstPer() {
		return sgstPer;
	}
	public void setSgstPer(float sgstPer) {
		this.sgstPer = sgstPer;
	}
	public float getIgstPer() {
		return igstPer;
	}
	public void setIgstPer(float igstPer) {
		this.igstPer = igstPer;
	}
	
	
	public float getDiscPer() {
		return discPer;
	}
	public void setDiscPer(float discPer) {
		this.discPer = discPer;
	}
	public float getDiscamt() {
		return discamt;
	}
	public void setDiscamt(float discamt) {
		this.discamt = discamt;
	}
	public float getBasicValue() {
		return basicValue;
	}
	public void setBasicValue(float basicValue) {
		this.basicValue = basicValue;
	}
	public float getPackValue() {
		return packValue;
	}
	public void setPackValue(float packValue) {
		this.packValue = packValue;
	}
	public float getInsu() {
		return insu;
	}
	public void setInsu(float insu) {
		this.insu = insu;
	}
	public float getOtherChargesBefor() {
		return otherChargesBefor;
	}
	public void setOtherChargesBefor(float otherChargesBefor) {
		this.otherChargesBefor = otherChargesBefor;
	}
	public float getTaxValue() {
		return taxValue;
	}
	public void setTaxValue(float taxValue) {
		this.taxValue = taxValue;
	}
	public float getFreightValue() {
		return freightValue;
	}
	public void setFreightValue(float freightValue) {
		this.freightValue = freightValue;
	}
	public float getOtherChargesAfter() {
		return otherChargesAfter;
	}
	public void setOtherChargesAfter(float otherChargesAfter) {
		this.otherChargesAfter = otherChargesAfter;
	}
	public float getLandingCost() {
		return landingCost;
	}
	public void setLandingCost(float landingCost) {
		this.landingCost = landingCost;
	}
	@Override
	public String toString() {
		return "OpeningStockModel [itemId=" + itemId + ", itemCode=" + itemCode + ", itemDesc=" + itemDesc
				+ ", itemDesc2=" + itemDesc2 + ", itemDesc3=" + itemDesc3 + ", itemUom=" + itemUom + ", itemUom2="
				+ itemUom2 + ", itemOpRate=" + itemOpRate + ", itemOpQty=" + itemOpQty + ", cgstPer=" + cgstPer
				+ ", sgstPer=" + sgstPer + ", igstPer=" + igstPer + ", discPer=" + discPer + ", discamt=" + discamt
				+ ", basicValue=" + basicValue + ", packValue=" + packValue + ", insu=" + insu + ", otherChargesBefor="
				+ otherChargesBefor + ", taxValue=" + taxValue + ", freightValue=" + freightValue
				+ ", otherChargesAfter=" + otherChargesAfter + ", landingCost=" + landingCost + "]";
	}
	

}
