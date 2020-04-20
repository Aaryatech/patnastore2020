package com.ats.tril.controller;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import com.ats.tril.common.Constants;
import com.ats.tril.common.DateConvertor;
import com.ats.tril.model.AccountHead;
import com.ats.tril.model.Category;
import com.ats.tril.model.ErrorMessage;
import com.ats.tril.model.GetPODetail;
import com.ats.tril.model.OpeningStockModel;
import com.ats.tril.model.PoDetail;
import com.ats.tril.model.SettingValue;
import com.ats.tril.model.StockHeader;
import com.ats.tril.model.Type;
import com.ats.tril.model.Vendor;
import com.ats.tril.model.doc.DocumentBean;
import com.ats.tril.model.doc.SubDocument;
import com.ats.tril.model.indent.Indent;
import com.ats.tril.model.indent.IndentTrans;
import com.ats.tril.model.indent.TempIndentDetail;
import com.ats.tril.model.mrn.MrnDetail;
import com.ats.tril.model.mrn.MrnHeader;
import com.ats.tril.model.po.PoHeader;

@Controller
@Scope("session")
public class OpeningUtilityController {
	RestTemplate rest = new RestTemplate();

	List<OpeningStockModel> itemList = new ArrayList<>();
	DecimalFormat df = new DecimalFormat("#.000");
	PoHeader PoHeader = new PoHeader();
	
	@RequestMapping(value = "/AddOPeningstockutility", method = RequestMethod.GET)
	public ModelAndView AddOPeningstockutility(HttpServletRequest request, HttpServletResponse response) {

		ModelAndView model = new ModelAndView("stock/openingStockUtility");
		try {

			Category[] category = rest.getForObject(Constants.url + "/getAllCategoryByIsUsed", Category[].class);
			List<Category> categoryList = new ArrayList<Category>(Arrays.asList(category));
			model.addObject("categoryList", categoryList);

			AccountHead[] accountHead = rest.getForObject(Constants.url + "/getAllAccountHeadByIsUsed",
					AccountHead[].class);
			List<AccountHead> accountHeadList = new ArrayList<AccountHead>(Arrays.asList(accountHead));
			model.addObject("accountHeadList", accountHeadList);

			Vendor[] vendorRes = rest.getForObject(Constants.url + "/getAllVendorByIsUsed", Vendor[].class);
			List<Vendor> vendorList = new ArrayList<Vendor>(Arrays.asList(vendorRes));
			model.addObject("vendorList", vendorList);

			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
			Date date = new Date();
			model.addObject("date", dateFormat.format(date));

			Type[] type = rest.getForObject(Constants.url + "/getAlltype", Type[].class);
			List<Type> typeList = new ArrayList<Type>(Arrays.asList(type));
			model.addObject("typeList", typeList);
			PoHeader = new PoHeader();
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "getItemcategorywise", method = RequestMethod.GET)
	public @ResponseBody List<OpeningStockModel> GetAllitemOpeningStock(HttpServletRequest request,
			HttpServletResponse response) {

		itemList = new ArrayList<>();

		try {

			int catId = Integer.parseInt(request.getParameter("catId"));
			int vendId = Integer.parseInt(request.getParameter("vendId"));

			MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
			map.add("cat_id", catId);
			map.add("vendId", vendId);
			OpeningStockModel[] itemRes = rest.postForObject(Constants.url + "/getAllitemOpeningStock", map,
					OpeningStockModel[].class);
			itemList = new ArrayList<OpeningStockModel>(Arrays.asList(itemRes));
			PoHeader = new PoHeader();
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		return itemList;
	}

	@RequestMapping(value = "deleteItemFromOplist", method = RequestMethod.GET)
	public @ResponseBody List<OpeningStockModel> deleteItemFromOplist(HttpServletRequest request,
			HttpServletResponse response) {

		try {
			int index = Integer.parseInt(request.getParameter("key"));
			itemList.remove(index);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return itemList;
	}
	
	@RequestMapping(value = "setValueToItemList", method = RequestMethod.GET)
	public @ResponseBody List<OpeningStockModel> setValueToItemList(HttpServletRequest request,
			HttpServletResponse response) {

		try {
			int itemId = Integer.parseInt(request.getParameter("itemId"));
			float Qty = Float.parseFloat(request.getParameter("Qty"));
			float Rate = Float.parseFloat(request.getParameter("Rate"));
			float disc = Float.parseFloat(request.getParameter("disc")); 
			float discamt = Float.parseFloat(request.getParameter("discamt")); 
			 
			float poBasicValue = 0;
			float discValue = 0;
			float taxValue = 0;
			
			 for(int i=0; i<itemList.size() ; i++) {
				 
				 if(itemId==itemList.get(i).getItemId()) {
					 
					 itemList.get(i).setItemOpQty(Qty);
					 itemList.get(i).setItemOpRate(Rate);
					 itemList.get(i).setDiscPer(disc);
					 itemList.get(i).setDiscamt(discamt);
					 itemList.get(i).setBasicValue(Qty*Rate); 
					 
				 }
				 poBasicValue = poBasicValue + itemList.get(i).getBasicValue();
			 }
			 
			 PoHeader.setPoBasicValue(Float.parseFloat(df.format(poBasicValue)));
			 
			 for(int i=0; i<itemList.size() ; i++) {
				  
					  
					 float divFactor = (itemList.get(i).getBasicValue() / PoHeader.getPoBasicValue()) * 100; 
					 itemList.get(i).setPackValue(Float.parseFloat(df.format(divFactor * PoHeader.getPoPackVal() / 100))); 
					 itemList.get(i).setInsu(Float.parseFloat(df.format(divFactor * PoHeader.getPoInsuVal() / 100))); 
					 itemList.get(i).setFreightValue(Float.parseFloat(df.format(divFactor * PoHeader.getPoFrtVal() / 100))); 
					 itemList.get(i).setTaxValue(Float.parseFloat(df.format(((itemList.get(i).getCgstPer()+itemList.get(i).getSgstPer())/ 100)*(itemList.get(i).getBasicValue()-
					 itemList.get(i).getDiscamt()+itemList.get(i).getPackValue()+itemList.get(i).getInsu()+
					 itemList.get(i).getFreightValue())))); 
					 itemList.get(i) .setOtherChargesAfter(Float.parseFloat(df.format(divFactor * PoHeader.getOtherChargeAfter() / 100)));
					 itemList.get(i).setLandingCost(Float.parseFloat(df.format(itemList.get(i).getBasicValue()
								- itemList.get(i).getDiscamt()
								+ itemList.get(i).getPackValue() + itemList.get(i).getInsu()
								+ itemList.get(i).getFreightValue()
								+ itemList.get(i).getTaxValue()
								+ itemList.get(i).getOtherChargesAfter())));  
				 
					discValue = discValue + itemList.get(i).getDiscamt();
					taxValue=taxValue+itemList.get(i).getTaxValue();
			 }
			  
				PoHeader.setDiscValue(Float.parseFloat(df.format(discValue))); 
				PoHeader.setPoTaxValue(Float.parseFloat(df.format(taxValue)));

		} catch (Exception e) {
			e.printStackTrace();
		}

		return itemList;
	}
	
	
 	@RequestMapping(value = "/calculatePurchaseHeaderValuesInDirectMRN", method = RequestMethod.GET)
	@ResponseBody
	public PoHeader calculatePurchaseHeaderValues(HttpServletRequest request, HttpServletResponse response) {

		try {
			float total = 0;
			float taxValue = 0;
			float poBasicValue = 0;
			float discValue = 0; 
			
			float packPer = Float.parseFloat(request.getParameter("packPer"));
			float packValue = Float.parseFloat(request.getParameter("packValue"));
			float insuPer = Float.parseFloat(request.getParameter("insuPer"));
			float insuValue = Float.parseFloat(request.getParameter("insuValue"));
			float freightPer = Float.parseFloat(request.getParameter("freightPer"));
			float freightValue = Float.parseFloat(request.getParameter("freightValue"));
			float otherPer = Float.parseFloat(request.getParameter("otherPer"));
			float otherValue = Float.parseFloat(request.getParameter("otherValue"));
			//float taxPer = Float.parseFloat(request.getParameter("taxPer"));
			//int taxId = Integer.parseInt(request.getParameter("taxId"));

			if (packPer != 0) {
				PoHeader.setPoPackPer(packPer);
				PoHeader.setPoPackVal(Float.parseFloat(df.format((packPer / 100) * PoHeader.getPoBasicValue())));
			} else {
				PoHeader.setPoPackPer(0);
				PoHeader.setPoPackVal(packValue);
			}

			if (insuPer != 0) {
				PoHeader.setPoInsuPer(insuPer);
				PoHeader.setPoInsuVal(Float.parseFloat(df.format((insuPer / 100) * PoHeader.getPoBasicValue())));
			} else {
				PoHeader.setPoInsuPer(0);
				PoHeader.setPoInsuVal(insuValue);
			}

			if (freightPer != 0) {
				PoHeader.setPoFrtPer(freightPer);
				PoHeader.setPoFrtVal(Float.parseFloat(df.format((freightPer / 100) * PoHeader.getPoBasicValue())));
			} else {
				PoHeader.setPoFrtPer(0);
				PoHeader.setPoFrtVal(freightValue);
			}

			total = PoHeader.getPoBasicValue() + PoHeader.getPoPackVal() + PoHeader.getPoInsuVal()
					+ PoHeader.getPoFrtVal() - PoHeader.getDiscValue();
			/*PoHeader.setPoTaxId(taxId);
			PoHeader.setPoTaxPer(taxPer);
			PoHeader.setPoTaxValue((taxPer / 100) * total);*/

			if (otherPer != 0) {
				total = PoHeader.getPoBasicValue() + PoHeader.getPoPackVal() + PoHeader.getPoInsuVal()
						+ PoHeader.getPoFrtVal() - PoHeader.getDiscValue() + PoHeader.getPoTaxValue();
				PoHeader.setOtherChargeAfter(Float.parseFloat(df.format((otherPer / 100) * total)));
			} else if (otherValue != 0) {
				PoHeader.setOtherChargeAfter(otherValue);
			} else {
				PoHeader.setOtherChargeAfter(0);
			}

			for(int i=0; i<itemList.size() ; i++) {
				   
					poBasicValue = poBasicValue + itemList.get(i).getBasicValue(); 
			}
			
			PoHeader.setPoBasicValue(Float.parseFloat(df.format(poBasicValue))); 
			
			for(int i=0; i<itemList.size() ; i++) {
				  
					 float divFactor = (itemList.get(i).getBasicValue() / PoHeader.getPoBasicValue()) * 100; 
					 itemList.get(i).setPackValue(Float.parseFloat(df.format(divFactor * PoHeader.getPoPackVal() / 100))); 
					 itemList.get(i).setInsu(Float.parseFloat(df.format(divFactor * PoHeader.getPoInsuVal() / 100))); 
					 itemList.get(i).setFreightValue(Float.parseFloat(df.format(divFactor * PoHeader.getPoFrtVal() / 100))); 
					 itemList.get(i).setTaxValue(Float.parseFloat(df.format(((itemList.get(i).getCgstPer()+itemList.get(i).getSgstPer())/ 100)*(itemList.get(i).getBasicValue()-
					 itemList.get(i).getDiscamt()+itemList.get(i).getPackValue()+itemList.get(i).getInsu()+
					 itemList.get(i).getFreightValue())))); 
					 itemList.get(i) .setOtherChargesAfter(Float.parseFloat(df.format(divFactor * PoHeader.getOtherChargeAfter() / 100)));
					 itemList.get(i).setLandingCost(Float.parseFloat(df.format(itemList.get(i).getBasicValue()
								- itemList.get(i).getDiscamt()
								+ itemList.get(i).getPackValue() + itemList.get(i).getInsu()
								+ itemList.get(i).getFreightValue()
								+ itemList.get(i).getTaxValue()
								+ itemList.get(i).getOtherChargesAfter())));
						taxValue=taxValue+itemList.get(i).getTaxValue();
						poBasicValue = poBasicValue + itemList.get(i).getBasicValue();
						discValue = discValue + itemList.get(i).getDiscamt();
			 }

			PoHeader.setDiscValue(Float.parseFloat(df.format(discValue))); 
			PoHeader.setPoTaxValue(Float.parseFloat(df.format(taxValue)));
			//System.out.println(PoHeader);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return PoHeader;
	} 

	@RequestMapping(value = "/insertIndentPoMrn", method = RequestMethod.POST)
	public String insertIndentPoMrn(HttpServletRequest request, HttpServletResponse response) {

		try {

			int catId = Integer.parseInt(request.getParameter("catId"));

			String indNo = request.getParameter("indent_no");

			int indType = Integer.parseInt(request.getParameter("indent_type"));

			// String indHeaderRemark = request.getParameter("indHeaderRemark");

			String indDate = request.getParameter("indent_date");
			System.err.println("indeDate " + indDate);

			int accHead = Integer.parseInt(request.getParameter("acc_head"));

			int isMachineSpe = Integer.parseInt(request.getParameter("machine_specific"));

			int dept = 0;
			int subDept = 0;

			if (isMachineSpe == 1) {
				System.err.println("It is Machine Specific");
				dept = Integer.parseInt(request.getParameter("dept"));
				subDept = Integer.parseInt(request.getParameter("sub_dept"));

				System.err.println("dept " + dept + "sub Dept  " + subDept);

			}

			System.err.println("dept " + dept + "sub Dept  " + subDept);

			int isDev = Integer.parseInt(request.getParameter("is_dev"));
			int isMonthly = Integer.parseInt(request.getParameter("is_monthly"));

			Indent indent = new Indent();
			DocumentBean docBean = null;
			try {

				MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
				map.add("docId", 1);
				map.add("catId", catId);
				map.add("date", DateConvertor.convertToYMD(indDate));
				map.add("typeId", indType);
				RestTemplate restTemplate = new RestTemplate();

				docBean = restTemplate.postForObject(Constants.url + "getDocumentData", map, DocumentBean.class);
				String indMNo = docBean.getSubDocument().getCategoryPrefix() + "";
				int counter = docBean.getSubDocument().getCounter();
				int counterLenth = String.valueOf(counter).length();
				counterLenth = 4 - counterLenth;
				StringBuilder code = new StringBuilder(indMNo + "");

				for (int i = 0; i < counterLenth; i++) {
					String j = "0";
					code.append(j);
				}
				code.append(String.valueOf(counter));

				indent.setIndMNo("" + code);

				docBean.getSubDocument().setCounter(docBean.getSubDocument().getCounter() + 1);
			} catch (Exception e) {
				e.printStackTrace();
			}
			indent.setAchdId(accHead);
			indent.setCatId(catId);
			indent.setIndIsdev(isDev);
			indent.setIndIsmonthly(isMonthly);
			indent.setIndMDate(DateConvertor.convertToYMD(indDate));

			indent.setIndMStatus(9);
			indent.setIndMType(indType);
			indent.setIndRemark("-");

			indent.setDeptId(dept);
			indent.setSubDeptId(subDept);
			indent.setIndApr1Date(DateConvertor.convertToYMD(indDate));
			indent.setIndApr2Date(DateConvertor.convertToYMD(indDate));

			indent.setDelStatus(Constants.delStatus);
			indent.setIndRemark("");
			List<IndentTrans> indTrasList = new ArrayList<IndentTrans>();
			for (int i = 0; i < itemList.size(); i++) {

				if (Float.parseFloat(request.getParameter("Qty" + itemList.get(i).getItemId())) > 0) {

					IndentTrans transDetail = new IndentTrans();
					transDetail.setIndItemCurstk(itemList.get(i).getItemOpQty());
					transDetail.setIndItemDesc(itemList.get(i).getItemDesc());
					transDetail.setIndItemSchd(1);
					transDetail.setIndItemSchddt(DateConvertor.convertToSqlDate(indDate));
					transDetail.setIndItemUom(itemList.get(i).getItemUom());
					transDetail.setIndMDate(indent.getIndMDate());
					transDetail.setIndMNo(indent.getIndMNo());
					transDetail.setIndQty(Float.parseFloat(request.getParameter("Qty" + itemList.get(i).getItemId())));
					transDetail.setIndRemark("");
					transDetail.setItemId(itemList.get(i).getItemId());
					transDetail.setIndFyr(transDetail.getIndQty());
					transDetail.setDelStatus(Constants.delStatus);
					transDetail.setIndDStatus(2);
					transDetail.setIndApr1Date(DateConvertor.convertToYMD(indDate));
					transDetail.setIndApr2Date(DateConvertor.convertToYMD(indDate));
					transDetail.setIndDStatus(9);
					indTrasList.add(transDetail);
				}

			}
			indent.setIndentTrans(indTrasList); 

			//System.err.println("Indent = " + indent.toString());

			RestTemplate restTemp = new RestTemplate();
			if (indTrasList.size() > 0) {

				Indent indRes = restTemp.postForObject(Constants.url + "/saveIndentAndTrans", indent, Indent.class);

				//System.out.println("resposne indent" + indRes);

				indTrasList = indRes.getIndentTrans();

				if (indRes != null) {
					try {

						SubDocument subDocRes = restTemp.postForObject(Constants.url + "/saveSubDoc",
								docBean.getSubDocument(), SubDocument.class);

					} catch (Exception e) {
						e.printStackTrace();
					}

					int utility = Integer.parseInt(request.getParameter("indpomrn"));
					if (utility > 1) {

						int isState = 0;
						int vendId = Integer.parseInt(request.getParameter("vendId"));
						String packRemark = request.getParameter("packRemark");
						String insuRemark = request.getParameter("insuRemark");
						String freghtRemark = request.getParameter("freghtRemark");
						String otherRemark = request.getParameter("otherRemark"); 
						
						float poBasicValue = 0;
						float discValue = 0;
						float taxValue = 0;

						MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
						map = new LinkedMultiValueMap<String, Object>();
						map.add("name", "sameState");
						System.out.println("map " + map);
						SettingValue settingValue = rest.postForObject(Constants.url + "/getSettingValue", map,
								SettingValue.class);
						
						map.add("vendId", vendId); 
						ErrorMessage stateCodeRes = rest.postForObject(Constants.url + "/getStateCodeByVendId", map,
								ErrorMessage.class);

						
						  if(stateCodeRes.getMessage().equals(settingValue.getValue())) {
						  
						  isState=1; }
						 

						  
						// ----------------------------Inv No---------------------------------
						docBean = new DocumentBean();
						try {

							map = new LinkedMultiValueMap<String, Object>();
							map.add("docId", 2);
							map.add("catId", 1);
							map.add("date", DateConvertor.convertToYMD(indDate));
							map.add("typeId", indType);
							RestTemplate restTemplate = new RestTemplate();

							docBean = restTemplate.postForObject(Constants.url + "getDocumentData", map,
									DocumentBean.class);
							String indMNo = docBean.getSubDocument().getCategoryPrefix() + "";
							int counter = docBean.getSubDocument().getCounter();
							int counterLenth = String.valueOf(counter).length();
							counterLenth = 4 - counterLenth;
							StringBuilder code = new StringBuilder(indMNo + "");

							for (int i = 0; i < counterLenth; i++) {
								String j = "0";
								code.append(j);
							}
							code.append(String.valueOf(counter));

							PoHeader.setPoNo("" + code);

							docBean.getSubDocument().setCounter(docBean.getSubDocument().getCounter() + 1);
						} catch (Exception e) {
							e.printStackTrace();
						}
						// ----------------------------Inv No---------------------------------
						PoHeader.setVendId(vendId);
						PoHeader.setVendQuation("-");
						PoHeader.setPoType(indType);
						PoHeader.setPaymentTermId(1);
						PoHeader.setDeliveryId(1);
						PoHeader.setDispatchId(1);
						PoHeader.setVendQuationDate(DateConvertor.convertToYMD(indDate));
						PoHeader.setPoDate(DateConvertor.convertToYMD(indDate));

						PoHeader.setOtherChargeAfterRemark(otherRemark);
						PoHeader.setPoFrtRemark(freghtRemark);
						PoHeader.setPoInsuRemark(insuRemark);
						PoHeader.setPoPackRemark(packRemark);
						// PoHeader.setIndId(PoHeader.getPoDetailList().get(0).getIndId());
						PoHeader.setDelStatus(1);
						PoHeader.setPoRemark("");
						PoHeader.setPoStatus(9);
						PoHeader.setApprovStatus(1);
						List<PoDetail> poDetailList = new ArrayList<>();

						Calendar c = Calendar.getInstance();
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
						
						
						for (int i = 0; i < indTrasList.size(); i++) {

							for (int j = 0; j < itemList.size(); j++) {
								
								if(indTrasList.get(i).getItemId()==itemList.get(j).getItemId()) {
									 
									PoDetail poDetail = new PoDetail();
									poDetail.setIndId(indTrasList.get(i).getIndDId());
									poDetail.setSchDays(indTrasList.get(i).getIndItemSchd());
									poDetail.setItemId(indTrasList.get(i).getItemId());
									poDetail.setIndedQty(indTrasList.get(i).getIndQty());
									poDetail.setItemUom(indTrasList.get(i).getIndItemUom());
									poDetail.setStatus(9);
									poDetail.setItemQty(indTrasList.get(i).getIndQty());
									poDetail.setPendingQty(poDetail.getItemQty());
									poDetail.setDiscPer(itemList.get(j).getDiscPer());
									poDetail.setItemRate(itemList.get(j).getItemOpRate());
									poDetail.setSchRemark(""); 
									poDetail.setBalanceQty(poDetail.getPendingQty());
									poDetail.setSchDate(String.valueOf(indTrasList.get(i).getIndItemSchddt()));
									c.setTime(sdf.parse(poDetail.getSchDate()));
									c.add(Calendar.DAY_OF_MONTH, poDetail.getSchDays());
									poDetail.setSchDate(sdf.format(c.getTime()));
									poDetail.setIndId(indTrasList.get(i).getIndDId());
									poDetail.setIndMNo(indTrasList.get(i).getIndMNo());
									poDetail.setBasicValue(itemList.get(j).getBasicValue());
									poDetail.setDiscValue(itemList.get(j).getDiscamt());
									poDetail.setFreightValue(itemList.get(j).getFreightValue());
									poDetail.setInsu(itemList.get(j).getInsu());
									poDetail.setPackValue(itemList.get(j).getPackValue());
									poDetail.setOtherChargesAfter(itemList.get(j).getOtherChargesAfter());
									
									float taxPer = Float
											.parseFloat(request.getParameter("taxper" + indTrasList.get(i).getItemId()));

									if (isState == 0) {
										poDetail.setIgst(taxPer);

									} else {
										poDetail.setCgst(taxPer / 2);
										poDetail.setSgst(taxPer / 2);
									}
									poDetail.setTaxValue(itemList.get(j).getTaxValue());
									poDetail.setLandingCost(Float.parseFloat(df.format(itemList.get(j).getBasicValue()
											- itemList.get(j).getDiscamt()
											+ itemList.get(j).getPackValue() + itemList.get(j).getInsu()
											+ itemList.get(j).getFreightValue()
											+ itemList.get(j).getTaxValue()
											+ itemList.get(j).getOtherChargesAfter())));
									
									poBasicValue = poBasicValue + poDetail.getBasicValue();
									discValue = discValue + poDetail.getDiscValue();
									taxValue = taxValue + poDetail.getTaxValue();
									poDetailList.add(poDetail);
									indTrasList.get(i).setIndFyr(indTrasList.get(i).getIndFyr() - poDetail.getItemQty());
									PoHeader.setIndNo(indTrasList.get(i).getIndMNo());
									
									break;
								}
								
							}
							 
						}
						//System.out.println(poDetailList);

						PoHeader.setIndId(indRes.getIndMId()); 
						PoHeader.setPoDetailList(poDetailList); 
						
						//System.out.println(PoHeader);
						PoHeader save = rest.postForObject(Constants.url + "/savePoHeaderAndDetail", PoHeader,
								PoHeader.class);
						poDetailList=save.getPoDetailList();
						
						System.out.println(save);
						if (save != null) {
							try {

								SubDocument subDocRes = rest.postForObject(Constants.url + "/saveSubDoc",
										docBean.getSubDocument(), SubDocument.class);

							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						if (save != null && indTrasList.size() > 0) {
							for (int i = 0; i < indTrasList.size(); i++) {
								indTrasList.get(i)
										.setIndMDate(DateConvertor.convertToYMD(indTrasList.get(i).getIndMDate()));
								if (indTrasList.get(i).getIndFyr() == 0)
									indTrasList.get(i).setIndDStatus(2);
								else if (indTrasList.get(i).getIndFyr() > 0
										&& indTrasList.get(i).getIndFyr() < indTrasList.get(i).getIndQty())
									indTrasList.get(i).setIndDStatus(1);
								else
									indTrasList.get(i).setIndDStatus(0);
							}
							ErrorMessage errorMessage = rest.postForObject(Constants.url + "/updateIndendPendingQty",
									indTrasList, ErrorMessage.class);
							System.out.println(errorMessage);

							if (utility == 3) {

								System.err.println("inside /insertMrnProcess");
 
								String billNo = request.getParameter("bill_no");
								String billDate = request.getParameter("bill_date");
								
								MrnHeader mrnHeader = new MrnHeader();
								// ----------------------------Inv No---------------------------------
								// DocumentBean docBean=null;

								try {
 

									map = new LinkedMultiValueMap<String, Object>();
									map.add("docType", 1);
									map.add("date", DateConvertor.convertToYMD(indDate));

									RestTemplate restTemplate = new RestTemplate();

									errorMessage = restTemplate.postForObject(
											Constants.url + "generateIssueNoAndMrnNo", map, ErrorMessage.class);

									mrnHeader.setMrnNo("" + errorMessage.getMessage());

									// docBean.getSubDocument().setCounter(docBean.getSubDocument().getCounter()+1);
								} catch (Exception e) {
									e.printStackTrace();
								}
								// ----------------------------Inv No---------------------------------
								List<MrnDetail> mrnDetailList = new ArrayList<MrnDetail>();

								mrnHeader.setBillDate(DateConvertor.convertToYMD(billDate));
								mrnHeader.setBillNo(billNo);
								mrnHeader.setDelStatus(Constants.delStatus);
								mrnHeader.setDocDate(DateConvertor.convertToYMD(indDate));
								mrnHeader.setDocNo("");
								mrnHeader.setGateEntryDate(DateConvertor.convertToYMD(indDate));
								mrnHeader.setGateEntryNo("");
								mrnHeader.setLrDate(DateConvertor.convertToYMD(indDate));
								mrnHeader.setLrNo("");
								mrnHeader.setMrnDate(DateConvertor.convertToYMD(indDate));

								mrnHeader.setMrnStatus(4);
								mrnHeader.setMrnType(indType);
								mrnHeader.setRemark1("");
								mrnHeader.setRemark2("def");
								mrnHeader.setTransport("");
								mrnHeader.setUserId(1);
								mrnHeader.setVendorId(vendId);

								for (PoDetail detail : poDetailList) {
 
										MrnDetail mrnDetail = new MrnDetail(); 
										mrnDetail.setIndentQty(detail.getIndedQty()); 
										mrnDetail.setPoQty(detail.getItemQty()); 
										mrnDetail.setMrnQty(detail.getItemQty()); 
										mrnDetail.setItemId(detail.getItemId()); 
										mrnDetail.setPoId(detail.getPoId()); 
										mrnDetail.setPoNo(save.getPoNo()); 
										mrnDetail.setMrnDetailStatus(4); 
										mrnDetail.setBatchNo("Default Batch KKKK-00456");
										mrnDetail.setDelStatus(Constants.delStatus); 
										mrnDetail.setPoDetailId(detail.getPoDetailId()); 
										mrnDetail.setMrnQtyBeforeEdit(-1);
										mrnDetail.setRemainingQty(mrnDetail.getMrnQty());
										mrnDetail.setApproveQty(mrnDetail.getMrnQty());
										mrnDetailList.add(mrnDetail); 
								}

								mrnHeader.setMrnDetailList(mrnDetailList);

								System.err.println("Mrn Header   " + mrnHeader.toString());

							 

								MrnHeader mrnHeaderRes = rest.postForObject(Constants.url + "/saveMrnHeadAndDetail",
										mrnHeader, MrnHeader.class);
								if (mrnHeaderRes != null) {
									try {

										// SubDocument subDocRes = restTemp.postForObject(Constants.url + "/saveSubDoc",
										// docBean.getSubDocument(), SubDocument.class);

									} catch (Exception e) {
										e.printStackTrace();
									}
								}
								System.err.println("mrnHeaderRes " + mrnHeaderRes.toString());

							}
						}

					}
				} 
			}

		} catch (Exception e) {

			System.err.println("Exception in @saveIndent  Indent" + e.getMessage());
			e.getCause();
			e.printStackTrace();
		}
		  
		return "redirect:/AddOPeningstockutility";
	}
}
