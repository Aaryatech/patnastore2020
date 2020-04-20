package com.ats.tril.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import com.ats.tril.common.Constants;
import com.ats.tril.common.DateConvertor;
import com.ats.tril.model.Category;
import com.ats.tril.model.Company;
import com.ats.tril.model.ConsumptionReportWithCatId;
import com.ats.tril.model.Dept;
import com.ats.tril.model.ExportToExcel;
import com.ats.tril.model.GetCurrentStock;
import com.ats.tril.model.GetItem;
import com.ats.tril.model.GetItemGroup;
import com.ats.tril.model.GetSubDept;
import com.ats.tril.model.IndentStatusReport;
import com.ats.tril.model.IssueAndMrnGroupWise;
import com.ats.tril.model.IssueAndMrnItemWise;
import com.ats.tril.model.IssueDeptWise;
import com.ats.tril.model.IssueMonthWiseList;
import com.ats.tril.model.ItemExpectedReport;
import com.ats.tril.model.ItemValuationList;
import com.ats.tril.model.ItemWiseStockValuationReport;
import com.ats.tril.model.MonthCategoryWiseMrnReport;
import com.ats.tril.model.MonthItemWiseMrnReport;
import com.ats.tril.model.MonthSubDeptWiseIssueReport;
import com.ats.tril.model.MonthWiseIssueReport;
import com.ats.tril.model.MrnMonthWiseList;
import com.ats.tril.model.ShortItemReport;
import com.ats.tril.model.StockValuationCategoryWise;
import com.ats.tril.model.Type;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@Controller
@Scope("session")
public class ValuationReport {

	RestTemplate rest = new RestTemplate();
	String fromDate;
	String toDate;
	int typeId;
	int isDev;
	int deptId;
	int subDeptId;
	int catId;
	int year;

	List<IssueMonthWiseList> listGlobal;
	List<Dept> deparmentList;

	@RequestMapping(value = "/stockBetweenDateWithCatId", method = RequestMethod.GET)
	public ModelAndView itemValueationReport(HttpServletRequest request, HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/stockBetweenDateWithCatId");
		try {

			Category[] category = rest.getForObject(Constants.url + "/getAllCategoryByIsUsed", Category[].class);
			List<Category> categoryList = new ArrayList<Category>(Arrays.asList(category));

			model.addObject("categoryList", categoryList);
			List<GetCurrentStock> getStockBetweenDate = new ArrayList<>();

			if (request.getParameter("fromDate") == null || request.getParameter("toDate") == null
					|| request.getParameter("catId") == null) {

			} else {

				fromDate = request.getParameter("fromDate");
				toDate = request.getParameter("toDate");
				catId = Integer.parseInt(request.getParameter("catId"));

				SimpleDateFormat yy = new SimpleDateFormat("yyyy-MM-dd");
				SimpleDateFormat dd = new SimpleDateFormat("dd-MM-yyyy");

				Date date = dd.parse(fromDate);
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);

				String firstDate = "01" + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.YEAR);

				System.out.println(DateConvertor.convertToYMD(firstDate) + DateConvertor.convertToYMD(fromDate));

				if (DateConvertor.convertToYMD(firstDate).compareTo(DateConvertor.convertToYMD(fromDate)) < 0) {
					calendar.add(Calendar.DATE, -1);
					String previousDate = yy.format(new Date(calendar.getTimeInMillis()));
					MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
					map.add("fromDate", DateConvertor.convertToYMD(firstDate));
					map.add("toDate", previousDate);
					map.add("catId", catId);
					System.out.println(map);
					GetCurrentStock[] getCurrentStock = rest.postForObject(
							Constants.url + "/getStockBetweenDateWithCatId", map, GetCurrentStock[].class);
					List<GetCurrentStock> diffDateStock = new ArrayList<>(Arrays.asList(getCurrentStock));

					calendar.add(Calendar.DATE, 1);
					String addDay = yy.format(new Date(calendar.getTimeInMillis()));
					map = new LinkedMultiValueMap<>();
					map.add("fromDate", addDay);
					map.add("toDate", DateConvertor.convertToYMD(toDate));
					map.add("catId", catId);
					System.out.println(map);
					GetCurrentStock[] getCurrentStock1 = rest.postForObject(
							Constants.url + "/getStockBetweenDateWithCatId", map, GetCurrentStock[].class);
					getStockBetweenDate = new ArrayList<>(Arrays.asList(getCurrentStock1));

					for (int i = 0; i < getStockBetweenDate.size(); i++) {
						for (int j = 0; j < diffDateStock.size(); j++) {
							if (getStockBetweenDate.get(i).getItemId() == diffDateStock.get(j).getItemId()) {
								getStockBetweenDate.get(i).setOpeningStock(diffDateStock.get(j).getOpeningStock()
										+ diffDateStock.get(j).getApproveQty() - diffDateStock.get(j).getIssueQty()
										+ diffDateStock.get(j).getReturnIssueQty() - diffDateStock.get(j).getDamageQty()
										- diffDateStock.get(j).getGatepassQty()
										+ diffDateStock.get(j).getGatepassReturnQty());
								getStockBetweenDate.get(i)
										.setOpStockValue(diffDateStock.get(j).getOpStockValue()
												+ diffDateStock.get(j).getApprovedQtyValue()
												- diffDateStock.get(j).getIssueQtyValue()
												- diffDateStock.get(j).getDamagValue());

								break;
							}
						}
					}
				} else {
					MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
					map.add("fromDate", DateConvertor.convertToYMD(fromDate));
					map.add("toDate", DateConvertor.convertToYMD(toDate));
					map.add("catId", catId);
					System.out.println(map);
					GetCurrentStock[] getCurrentStock = rest.postForObject(
							Constants.url + "/getStockBetweenDateWithCatId", map, GetCurrentStock[].class);
					getStockBetweenDate = new ArrayList<>(Arrays.asList(getCurrentStock));
				}
				model.addObject("list", getStockBetweenDate);
				model.addObject("fromDate", fromDate);
				model.addObject("toDate", toDate);
				model.addObject("catId", catId);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "/getStockBetweenDateWithCatId", method = RequestMethod.GET)
	@ResponseBody
	public List<GetCurrentStock> getStockBetweenDateWithCatId(HttpServletRequest request,
			HttpServletResponse response) {

		List<GetCurrentStock> getStockBetweenDate = new ArrayList<>();

		try {

			fromDate = request.getParameter("fromDate");
			toDate = request.getParameter("toDate");
			catId = Integer.parseInt(request.getParameter("catId"));

			SimpleDateFormat yy = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat dd = new SimpleDateFormat("dd-MM-yyyy");

			Date date = dd.parse(fromDate);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);

			String firstDate = "01" + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.YEAR);

			System.out.println(DateConvertor.convertToYMD(firstDate) + DateConvertor.convertToYMD(fromDate));

			if (DateConvertor.convertToYMD(firstDate).compareTo(DateConvertor.convertToYMD(fromDate)) < 0) {
				calendar.add(Calendar.DATE, -1);
				String previousDate = yy.format(new Date(calendar.getTimeInMillis()));
				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				map.add("fromDate", DateConvertor.convertToYMD(firstDate));
				map.add("toDate", previousDate);
				map.add("catId", catId);
				System.out.println(map);
				GetCurrentStock[] getCurrentStock = rest.postForObject(Constants.url + "/getStockBetweenDateWithCatId",
						map, GetCurrentStock[].class);
				List<GetCurrentStock> diffDateStock = new ArrayList<>(Arrays.asList(getCurrentStock));

				calendar.add(Calendar.DATE, 1);
				String addDay = yy.format(new Date(calendar.getTimeInMillis()));
				map = new LinkedMultiValueMap<>();
				map.add("fromDate", addDay);
				map.add("toDate", DateConvertor.convertToYMD(toDate));
				map.add("catId", catId);
				System.out.println(map);
				GetCurrentStock[] getCurrentStock1 = rest.postForObject(Constants.url + "/getStockBetweenDateWithCatId",
						map, GetCurrentStock[].class);
				getStockBetweenDate = new ArrayList<>(Arrays.asList(getCurrentStock1));

				for (int i = 0; i < getStockBetweenDate.size(); i++) {
					for (int j = 0; j < diffDateStock.size(); j++) {
						if (getStockBetweenDate.get(i).getItemId() == diffDateStock.get(j).getItemId()) {
							getStockBetweenDate.get(i).setOpeningStock(diffDateStock.get(j).getOpeningStock()
									+ diffDateStock.get(j).getApproveQty() - diffDateStock.get(j).getIssueQty()
									+ diffDateStock.get(j).getReturnIssueQty() - diffDateStock.get(j).getDamageQty()
									- diffDateStock.get(j).getGatepassQty()
									+ diffDateStock.get(j).getGatepassReturnQty());
							getStockBetweenDate.get(i).setOpStockValue(diffDateStock.get(j).getOpStockValue()
									+ diffDateStock.get(j).getApprovedQtyValue()
									- diffDateStock.get(j).getIssueQtyValue() - diffDateStock.get(j).getDamagValue());

							break;
						}
					}
				}
			} else {
				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				map.add("fromDate", DateConvertor.convertToYMD(fromDate));
				map.add("toDate", DateConvertor.convertToYMD(toDate));
				map.add("catId", catId);
				System.out.println(map);
				GetCurrentStock[] getCurrentStock = rest.postForObject(Constants.url + "/getStockBetweenDateWithCatId",
						map, GetCurrentStock[].class);
				getStockBetweenDate = new ArrayList<>(Arrays.asList(getCurrentStock));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return getStockBetweenDate;
	}

	@RequestMapping(value = "/valueationReportDetail/{itemId}/{openingStock}", method = RequestMethod.GET)
	public ModelAndView valueationReportDetail(@PathVariable int itemId, @PathVariable int openingStock,
			HttpServletRequest request, HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/valueationReportDetail");
		try {
			MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
			map.add("itemId", itemId);
			GetItem item = rest.postForObject(Constants.url + "/getItemByItemId", map, GetItem.class);
			model.addObject("item", item);

			map.add("fromDate", fromDate);
			map.add("toDate", toDate);
			// map.add("catId", catId);
			ItemValuationList[] itemValuation = rest.postForObject(Constants.url + "/valueationReportDetail", map,
					ItemValuationList[].class);
			List<ItemValuationList> itemValuationList = new ArrayList<ItemValuationList>(Arrays.asList(itemValuation));

			model.addObject("itemValuationList", itemValuationList);
			model.addObject("openingStock", openingStock);
			model.addObject("fromDate", fromDate);
			model.addObject("toDate", toDate);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	List<StockValuationCategoryWise> stockCategoryWiseListForPdf = new ArrayList<StockValuationCategoryWise>();

	@RequestMapping(value = "/stockValueationReportCategoryWise", method = RequestMethod.GET)
	public ModelAndView stockValueationReportCategoryWise(HttpServletRequest request, HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/stockValueationReportCategoryWise");
		try {
			List<StockValuationCategoryWise> categoryWiseReport = new ArrayList<StockValuationCategoryWise>();
			Type[] type = rest.getForObject(Constants.url + "/getAlltype", Type[].class);
			List<Type> typeList = new ArrayList<Type>(Arrays.asList(type));
			model.addObject("typeList", typeList);

			if (request.getParameter("fromDate") == null || request.getParameter("toDate") == null
					|| request.getParameter("typeId") == null) {

				SimpleDateFormat yy = new SimpleDateFormat("yyyy-MM-dd");
				SimpleDateFormat dd = new SimpleDateFormat("dd-MM-yyyy");
				Date date = new Date();
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);

				fromDate = "01" + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.YEAR);
				toDate = dd.format(date);
				typeId = 0;
				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				map.add("fromDate", DateConvertor.convertToYMD(fromDate));
				map.add("toDate", yy.format(date));
				map.add("typeId", typeId);
				StockValuationCategoryWise[] stockValuationCategoryWise = rest.postForObject(
						Constants.url + "/stockValueationReport", map, StockValuationCategoryWise[].class);
				categoryWiseReport = new ArrayList<StockValuationCategoryWise>(
						Arrays.asList(stockValuationCategoryWise));

				model.addObject("categoryWiseReport", categoryWiseReport);
				model.addObject("fromDate", fromDate);
				model.addObject("toDate", dd.format(date));
				stockCategoryWiseListForPdf = categoryWiseReport;
			} else {
				fromDate = request.getParameter("fromDate");
				toDate = request.getParameter("toDate");
				typeId = Integer.parseInt(request.getParameter("typeId"));

				SimpleDateFormat yy = new SimpleDateFormat("yyyy-MM-dd");
				SimpleDateFormat dd = new SimpleDateFormat("dd-MM-yyyy");

				Date date = dd.parse(fromDate);
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);

				String firstDate = "01" + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.YEAR);

				System.out.println(DateConvertor.convertToYMD(firstDate) + DateConvertor.convertToYMD(fromDate));

				if (DateConvertor.convertToYMD(firstDate).compareTo(DateConvertor.convertToYMD(fromDate)) < 0) {
					calendar.add(Calendar.DATE, -1);
					String previousDate = yy.format(new Date(calendar.getTimeInMillis()));
					MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
					map.add("fromDate", DateConvertor.convertToYMD(firstDate));
					map.add("toDate", previousDate);
					map.add("typeId", typeId);
					StockValuationCategoryWise[] stockValuationCategoryWise = rest.postForObject(
							Constants.url + "/stockValueationReport", map, StockValuationCategoryWise[].class);
					List<StockValuationCategoryWise> diffDateStock = new ArrayList<StockValuationCategoryWise>(
							Arrays.asList(stockValuationCategoryWise));

					calendar.add(Calendar.DATE, 1);
					String addDay = yy.format(new Date(calendar.getTimeInMillis()));
					map = new LinkedMultiValueMap<>();
					map.add("fromDate", addDay);
					map.add("toDate", DateConvertor.convertToYMD(toDate));
					map.add("typeId", typeId);
					System.out.println(map);
					StockValuationCategoryWise[] stockValuationCategoryWise1 = rest.postForObject(
							Constants.url + "/stockValueationReport", map, StockValuationCategoryWise[].class);
					categoryWiseReport = new ArrayList<StockValuationCategoryWise>(
							Arrays.asList(stockValuationCategoryWise1));

					for (int i = 0; i < categoryWiseReport.size(); i++) {
						for (int j = 0; j < diffDateStock.size(); j++) {
							if (categoryWiseReport.get(i).getCatId() == diffDateStock.get(j).getCatId()) {
								categoryWiseReport.get(i).setOpeningStock(diffDateStock.get(j).getOpeningStock()
										+ diffDateStock.get(j).getApproveQty() - diffDateStock.get(j).getIssueQty()
										- diffDateStock.get(j).getDamageQty());
								categoryWiseReport.get(i)
										.setOpStockValue(diffDateStock.get(j).getOpStockValue()
												+ diffDateStock.get(j).getApprovedQtyValue()
												- diffDateStock.get(j).getIssueQtyValue()
												- diffDateStock.get(j).getDamageValue());

								break;
							}
						}
					}
				} else {
					MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
					map.add("fromDate", DateConvertor.convertToYMD(fromDate));
					map.add("toDate", DateConvertor.convertToYMD(toDate));
					map.add("typeId", typeId);
					System.out.println(map);
					StockValuationCategoryWise[] stockValuationCategoryWise1 = rest.postForObject(
							Constants.url + "/stockValueationReport", map, StockValuationCategoryWise[].class);
					categoryWiseReport = new ArrayList<StockValuationCategoryWise>(
							Arrays.asList(stockValuationCategoryWise1));
				}

				model.addObject("categoryWiseReport", categoryWiseReport);
				model.addObject("fromDate", fromDate);
				model.addObject("toDate", toDate);
				model.addObject("typeId", typeId);
				stockCategoryWiseListForPdf = categoryWiseReport;

			}

			// ----------------exel-------------------------
			DecimalFormat df = new DecimalFormat("####0.00");

			List<ExportToExcel> exportToExcelList = new ArrayList<ExportToExcel>();

			ExportToExcel expoExcel = new ExportToExcel();
			List<String> rowData = new ArrayList<String>();

			rowData.add("SR. No");
			rowData.add("CATEGORY");
			rowData.add("OP QTY");
			rowData.add("OP VALUE");
			rowData.add("OP LANDING VALUE");
			rowData.add("RECEIVED QTY");
			rowData.add("RECEIVED Value");
			rowData.add("RECEIVED LANDING VALUE");
			rowData.add("ISSUE QTY");
			rowData.add("ISSUE VALUE");
			rowData.add("ISSUE LANDING VALUE");
			rowData.add("DAMAGE QTY");
			rowData.add("DAMAGE VALUE");
			rowData.add("DAMAGE LANDING VALUE");
			rowData.add("C\\L QTY");
			rowData.add("C\\L VALUE");
			rowData.add("C\\L LANDING VALUE");

			float totalOpStock = 0;
			float totalOpValue = 0;
			float totalOpLandValue = 0;

			float totalAprvQty = 0;
			float totalAprvValue = 0;
			float totalAprvLandValue = 0;

			float totalIssueQty = 0;
			float totalIssueValue = 0;
			float totalIssueLandValue = 0;

			float totalDamageQty = 0;
			float totalDamageValue = 0;
			float totalDamageLandValue = 0;

			float totalClsQty = 0;
			float totalClsValue = 0;
			float totalClsLandValue = 0;

			expoExcel.setRowData(rowData);
			exportToExcelList.add(expoExcel);

			for (int i = 0; i < categoryWiseReport.size(); i++) {
				expoExcel = new ExportToExcel();
				rowData = new ArrayList<String>();

				rowData.add((i + 1) + "");
				rowData.add(categoryWiseReport.get(i).getCatDesc());

				rowData.add("" + df.format(categoryWiseReport.get(i).getOpeningStock()));
				totalOpStock = totalOpStock + categoryWiseReport.get(i).getOpeningStock();

				rowData.add("" + df.format(categoryWiseReport.get(i).getOpStockValue()));
				totalOpValue = totalOpValue + categoryWiseReport.get(i).getOpStockValue();

				rowData.add("" + df.format(categoryWiseReport.get(i).getOpLandingValue()));
				totalOpLandValue = totalOpLandValue + categoryWiseReport.get(i).getOpLandingValue();

				rowData.add("" + df.format(categoryWiseReport.get(i).getApproveQty()));
				totalAprvQty = totalAprvQty + categoryWiseReport.get(i).getApproveQty();

				rowData.add("" + df.format(categoryWiseReport.get(i).getApprovedQtyValue()));
				totalAprvValue = totalAprvValue + categoryWiseReport.get(i).getApprovedQtyValue();

				rowData.add("" + df.format(categoryWiseReport.get(i).getApprovedLandingValue()));
				totalAprvLandValue = totalAprvLandValue + categoryWiseReport.get(i).getApprovedLandingValue();

				rowData.add("" + df.format(categoryWiseReport.get(i).getIssueQty()));
				totalIssueQty = totalIssueQty + categoryWiseReport.get(i).getIssueQty();

				rowData.add("" + df.format(categoryWiseReport.get(i).getIssueQtyValue()));
				totalIssueValue = totalIssueValue + categoryWiseReport.get(i).getIssueQtyValue();

				rowData.add("" + df.format(categoryWiseReport.get(i).getIssueLandingValue()));
				totalIssueLandValue = totalIssueLandValue + categoryWiseReport.get(i).getIssueLandingValue();

				rowData.add("" + df.format(categoryWiseReport.get(i).getDamageQty()));
				totalDamageQty = totalDamageQty + categoryWiseReport.get(i).getDamageQty();

				rowData.add("" + df.format(categoryWiseReport.get(i).getDamageValue()));
				totalDamageValue = totalDamageValue + categoryWiseReport.get(i).getDamageValue();

				rowData.add("" + df.format(categoryWiseReport.get(i).getDamageLandingValue()));
				totalDamageLandValue = totalDamageLandValue + categoryWiseReport.get(i).getDamageLandingValue();

				float closingQty = categoryWiseReport.get(i).getOpeningStock()
						+ categoryWiseReport.get(i).getApproveQty() - categoryWiseReport.get(i).getIssueQty()
						- categoryWiseReport.get(i).getDamageQty();

				float closingValue = categoryWiseReport.get(i).getOpStockValue()
						+ categoryWiseReport.get(i).getApprovedQtyValue() - categoryWiseReport.get(i).getIssueQtyValue()
						- categoryWiseReport.get(i).getDamageValue();

				float closingLandingValue = categoryWiseReport.get(i).getOpLandingValue()
						+ categoryWiseReport.get(i).getApprovedLandingValue()
						- categoryWiseReport.get(i).getIssueLandingValue()
						- categoryWiseReport.get(i).getDamageLandingValue();

				rowData.add("" + df.format(closingQty));
				rowData.add("" + df.format(closingValue));
				rowData.add("" + df.format(closingLandingValue));

				totalClsLandValue = totalClsLandValue + closingLandingValue;
				totalClsValue = totalClsValue + closingValue;
				totalClsQty = totalClsQty + closingQty;

				expoExcel.setRowData(rowData);
				exportToExcelList.add(expoExcel);

			}

			expoExcel = new ExportToExcel();
			rowData = new ArrayList<String>();

			rowData.add("-");
			rowData.add("Total");

			rowData.add("" + df.format(totalOpStock));
			rowData.add("" + df.format(totalOpValue));
			rowData.add("" + df.format(totalOpLandValue));
			rowData.add("" + df.format(totalAprvQty));
			rowData.add("" + df.format(totalAprvValue));
			rowData.add("" + df.format(totalAprvLandValue));
			rowData.add("" + df.format(totalIssueQty));
			rowData.add("" + df.format(totalIssueValue));
			rowData.add("" + df.format(totalIssueLandValue));
			rowData.add("" + df.format(totalDamageQty));
			rowData.add("" + df.format(totalDamageValue));
			rowData.add("" + df.format(totalDamageLandValue));

			rowData.add("" + df.format(totalClsQty));
			rowData.add("" + df.format(totalClsValue));
			rowData.add("" + df.format(totalClsLandValue));

			expoExcel.setRowData(rowData);
			exportToExcelList.add(expoExcel);

			HttpSession session = request.getSession();
			session.setAttribute("exportExcelList", exportToExcelList);
			session.setAttribute("excelName", "CategoryWiseStockValue");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "/stockValuetionReportCategoryWisePDF", method = RequestMethod.GET)
	public void stockValuetionReportCategoryWisePDF(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException {
		BufferedOutputStream outStream = null;
		try {
			Document document = new Document(PageSize.A4.rotate(), 10f, 10f, 10f, 0f);
			DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
			String reportDate = DF.format(new Date());
			document.addHeader("Date: ", reportDate);
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			System.out.println("time in Gen Bill PDF ==" + dateFormat.format(cal.getTime()));
			String timeStamp = dateFormat.format(cal.getTime());
			String FILE_PATH = Constants.REPORT_SAVE;
			File file = new File(FILE_PATH);

			PdfWriter writer = null;
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			FileOutputStream out = new FileOutputStream(FILE_PATH);
			try {
				writer = PdfWriter.getInstance(document, out);
			} catch (DocumentException e) {

				e.printStackTrace();
			}

			PdfPTable table = new PdfPTable(17);
			try {
				System.out.println("Inside PDF Table try");
				table.setWidthPercentage(100);
				table.setWidths(new float[] { 0.7f, 1.7f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
						1.0f, 1.0f, 1.0f, 1.0f, 1.0f });
				Font headFont = new Font(FontFamily.TIMES_ROMAN, 9, Font.NORMAL, BaseColor.BLACK);
				Font headFont1 = new Font(FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
				Font f = new Font(FontFamily.TIMES_ROMAN, 11.0f, Font.UNDERLINE, BaseColor.BLUE);
				Font f1 = new Font(FontFamily.TIMES_ROMAN, 9.0f, Font.BOLD, BaseColor.DARK_GRAY);

				PdfPCell hcell = new PdfPCell();

				hcell.setPadding(4);
				hcell = new PdfPCell(new Phrase("SR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("CATEGORY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("OP QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("OP VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("OP LAND VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("RECEIVED QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("RECEIVED VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("RECEIVED LAND VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE LAND VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("DAMAGE QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("DAMAGE VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("DAMAGE LAND VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("C/L QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("C/L VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("C/L LAND VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				float totalOpStock = 0;
				float totalOpValue = 0;
				float totalOpLandValue = 0;

				float totalAprvQty = 0;
				float totalAprvValue = 0;
				float totalAprvLandValue = 0;

				float totalIssueQty = 0;
				float totalIssueValue = 0;
				float totalIssueLandValue = 0;

				float totalDamageQty = 0;
				float totalDamageValue = 0;
				float totalDamageLandValue = 0;

				float totalClsQty = 0;
				float totalClsValue = 0;
				float totalClsLandValue = 0;

				int index = 0;
				if (!stockCategoryWiseListForPdf.isEmpty()) {
					for (int k = 0; k < stockCategoryWiseListForPdf.size(); k++) {

						if (stockCategoryWiseListForPdf.get(k).getOpeningStock() > 0
								|| stockCategoryWiseListForPdf.get(k).getOpStockValue() > 0
								|| stockCategoryWiseListForPdf.get(k).getApproveQty() > 0
								|| stockCategoryWiseListForPdf.get(k).getApprovedQtyValue() > 0
								|| stockCategoryWiseListForPdf.get(k).getIssueQty() > 0
								|| stockCategoryWiseListForPdf.get(k).getIssueQtyValue() > 0
								|| stockCategoryWiseListForPdf.get(k).getDamageQty() > 0
								|| stockCategoryWiseListForPdf.get(k).getDamageValue() > 0) {

							index++;

							PdfPCell cell;

							cell = new PdfPCell(new Phrase("" + index, headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase(stockCategoryWiseListForPdf.get(k).getCatDesc(), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase(
									"" + df.format(stockCategoryWiseListForPdf.get(k).getOpeningStock()), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalOpStock = totalOpStock + stockCategoryWiseListForPdf.get(k).getOpeningStock();

							cell = new PdfPCell(new Phrase(
									"" + df.format(stockCategoryWiseListForPdf.get(k).getOpStockValue()), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalOpValue = totalOpValue + stockCategoryWiseListForPdf.get(k).getOpStockValue();

							cell = new PdfPCell(new Phrase(
									"" + df.format(stockCategoryWiseListForPdf.get(k).getOpLandingValue()), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalOpLandValue = totalOpLandValue
									+ stockCategoryWiseListForPdf.get(k).getOpLandingValue();

							cell = new PdfPCell(new Phrase(
									"" + df.format(stockCategoryWiseListForPdf.get(k).getApproveQty()), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalAprvQty = totalAprvQty + stockCategoryWiseListForPdf.get(k).getApproveQty();

							cell = new PdfPCell(
									new Phrase("" + df.format(stockCategoryWiseListForPdf.get(k).getApprovedQtyValue()),
											headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalAprvValue = totalAprvValue + stockCategoryWiseListForPdf.get(k).getApprovedQtyValue();

							cell = new PdfPCell(new Phrase(
									"" + df.format(stockCategoryWiseListForPdf.get(k).getApprovedLandingValue()),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalAprvLandValue = totalAprvLandValue
									+ stockCategoryWiseListForPdf.get(k).getApprovedLandingValue();

							cell = new PdfPCell(new Phrase(
									"" + df.format(stockCategoryWiseListForPdf.get(k).getIssueQty()), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalIssueQty = totalIssueQty + stockCategoryWiseListForPdf.get(k).getIssueQty();

							cell = new PdfPCell(new Phrase(
									"" + df.format(stockCategoryWiseListForPdf.get(k).getIssueQtyValue()), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalIssueValue = totalIssueValue + stockCategoryWiseListForPdf.get(k).getIssueQtyValue();

							cell = new PdfPCell(new Phrase(
									"" + df.format(stockCategoryWiseListForPdf.get(k).getIssueLandingValue()),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalIssueLandValue = totalIssueLandValue
									+ stockCategoryWiseListForPdf.get(k).getIssueLandingValue();

							cell = new PdfPCell(new Phrase(
									"" + df.format(stockCategoryWiseListForPdf.get(k).getDamageQty()), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalDamageQty = totalDamageQty + stockCategoryWiseListForPdf.get(k).getDamageQty();

							cell = new PdfPCell(new Phrase(
									"" + df.format(stockCategoryWiseListForPdf.get(k).getDamageValue()), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalDamageValue = totalDamageValue + stockCategoryWiseListForPdf.get(k).getDamageValue();

							cell = new PdfPCell(new Phrase(
									"" + df.format(stockCategoryWiseListForPdf.get(k).getDamageLandingValue()),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalDamageLandValue = totalDamageLandValue
									+ stockCategoryWiseListForPdf.get(k).getDamageLandingValue();

							float closingQty = stockCategoryWiseListForPdf.get(k).getOpeningStock()
									+ stockCategoryWiseListForPdf.get(k).getApproveQty()
									- stockCategoryWiseListForPdf.get(k).getIssueQty()
									- stockCategoryWiseListForPdf.get(k).getDamageQty();

							float closingValue = stockCategoryWiseListForPdf.get(k).getOpStockValue()
									+ stockCategoryWiseListForPdf.get(k).getApprovedQtyValue()
									- stockCategoryWiseListForPdf.get(k).getIssueQtyValue()
									- stockCategoryWiseListForPdf.get(k).getDamageValue();

							float closingLandingValue = stockCategoryWiseListForPdf.get(k).getOpLandingValue()
									+ stockCategoryWiseListForPdf.get(k).getApprovedLandingValue()
									- stockCategoryWiseListForPdf.get(k).getIssueLandingValue()
									- stockCategoryWiseListForPdf.get(k).getDamageLandingValue();

							cell = new PdfPCell(new Phrase("" + df.format(closingQty), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalClsQty = totalClsQty + closingQty;

							cell = new PdfPCell(new Phrase("" + df.format(closingValue), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalClsValue = totalClsValue + closingValue;

							cell = new PdfPCell(new Phrase("" + df.format(closingLandingValue), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalClsLandValue = totalClsLandValue + closingLandingValue;

						}
					}
				}

				PdfPCell cell;

				cell = new PdfPCell(new Phrase("Total", headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_LEFT);
				cell.setPadding(3);
				cell.setColspan(2);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalOpStock), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalOpValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalOpLandValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalAprvQty), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalAprvValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalAprvLandValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalIssueQty), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalIssueValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalIssueLandValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalDamageQty), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalDamageValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalDamageLandValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalClsQty), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalClsValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalClsLandValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				document.open();
				Paragraph company = new Paragraph(comp.getCompanyName() + "\n", f);
				company.setAlignment(Element.ALIGN_CENTER);
				document.add(company);

				Paragraph heading1 = new Paragraph(comp.getOfficeAdd() + "\n", f1);
				heading1.setAlignment(Element.ALIGN_CENTER);
				document.add(heading1);

				Paragraph headingDate = new Paragraph(
						"Category Wise Stock Report , From Date: " + fromDate + "  To Date: " + toDate + "", f1);
				headingDate.setAlignment(Element.ALIGN_CENTER);
				document.add(headingDate);

				Paragraph ex3 = new Paragraph("\n");
				document.add(ex3);
				table.setHeaderRows(1);
				document.add(table);

				int totalPages = writer.getPageNumber();

				System.out.println("Page no " + totalPages);

				document.close();

				if (file != null) {

					String mimeType = URLConnection.guessContentTypeFromName(file.getName());

					if (mimeType == null) {

						mimeType = "application/pdf";

					}

					response.setContentType(mimeType);

					response.addHeader("content-disposition", String.format("inline; filename=\"%s\"", file.getName()));

					response.setContentLength((int) file.length());

					InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

					try {
						FileCopyUtils.copy(inputStream, response.getOutputStream());
					} catch (IOException e) {
						System.out.println("Excep in Opening a Pdf File");
						e.printStackTrace();
					}
				}

			} catch (DocumentException ex) {

				System.out.println("Pdf Generation Error" + ex.getMessage());

				ex.printStackTrace();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@RequestMapping(value = "/listForStockValuetioinGraph", method = RequestMethod.GET)
	public @ResponseBody List<StockValuationCategoryWise> listForStockValuetioinGraph(HttpServletRequest request,
			HttpServletResponse response) {

		return stockCategoryWiseListForPdf;
	}

	List<GetCurrentStock> itemWiseStockValuetionListForPDF = new ArrayList<>();

	@RequestMapping(value = "/stockSummaryWithCatId/{catId}", method = RequestMethod.GET)
	public ModelAndView stockSummaryWithCatId(@PathVariable int catId, HttpServletRequest request,
			HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/stockSummaryWithCatId");
		List<GetCurrentStock> getStockBetweenDate = new ArrayList<>();

		try {

			SimpleDateFormat yy = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat dd = new SimpleDateFormat("dd-MM-yyyy");

			Date date = dd.parse(fromDate);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);

			String firstDate = "01" + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.YEAR);

			System.out.println(DateConvertor.convertToYMD(firstDate) + DateConvertor.convertToYMD(fromDate));

			if (DateConvertor.convertToYMD(firstDate).compareTo(DateConvertor.convertToYMD(fromDate)) < 0) {
				calendar.add(Calendar.DATE, -1);
				String previousDate = yy.format(new Date(calendar.getTimeInMillis()));
				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				map.add("fromDate", DateConvertor.convertToYMD(firstDate));
				map.add("toDate", previousDate);
				map.add("catId", catId);
				map.add("typeId", typeId);
				System.out.println(map);
				GetCurrentStock[] getCurrentStock = rest.postForObject(
						Constants.url + "/getStockBetweenDateWithCatIdAndTypeId", map, GetCurrentStock[].class);
				List<GetCurrentStock> diffDateStock = new ArrayList<>(Arrays.asList(getCurrentStock));

				calendar.add(Calendar.DATE, 1);
				String addDay = yy.format(new Date(calendar.getTimeInMillis()));
				map = new LinkedMultiValueMap<>();
				map.add("fromDate", addDay);
				map.add("toDate", DateConvertor.convertToYMD(toDate));
				map.add("catId", catId);
				map.add("typeId", typeId);
				System.out.println(map);
				GetCurrentStock[] getCurrentStock1 = rest.postForObject(
						Constants.url + "/getStockBetweenDateWithCatIdAndTypeId", map, GetCurrentStock[].class);
				getStockBetweenDate = new ArrayList<>(Arrays.asList(getCurrentStock1));

				for (int i = 0; i < getStockBetweenDate.size(); i++) {
					for (int j = 0; j < diffDateStock.size(); j++) {
						if (getStockBetweenDate.get(i).getItemId() == diffDateStock.get(j).getItemId()) {
							getStockBetweenDate.get(i).setOpeningStock(diffDateStock.get(j).getOpeningStock()
									+ diffDateStock.get(j).getApproveQty() - diffDateStock.get(j).getIssueQty()
									+ diffDateStock.get(j).getReturnIssueQty() - diffDateStock.get(j).getDamageQty()
									- diffDateStock.get(j).getGatepassQty()
									+ diffDateStock.get(j).getGatepassReturnQty());
							getStockBetweenDate.get(i).setOpStockValue(diffDateStock.get(j).getOpStockValue()
									+ diffDateStock.get(j).getApprovedQtyValue()
									- diffDateStock.get(j).getIssueQtyValue() - diffDateStock.get(j).getDamagValue());

							break;
						}
					}
				}
			} else {
				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				map.add("fromDate", DateConvertor.convertToYMD(fromDate));
				map.add("toDate", DateConvertor.convertToYMD(toDate));
				map.add("catId", catId);
				map.add("typeId", typeId);
				System.out.println(map);
				GetCurrentStock[] getCurrentStock = rest.postForObject(
						Constants.url + "/getStockBetweenDateWithCatIdAndTypeId", map, GetCurrentStock[].class);
				getStockBetweenDate = new ArrayList<>(Arrays.asList(getCurrentStock));
			}
			itemWiseStockValuetionListForPDF = getStockBetweenDate;
			model.addObject("list", getStockBetweenDate);

			// ----------------exel-------------------------

			DecimalFormat df = new DecimalFormat("####0.00");

			List<ExportToExcel> exportToExcelList = new ArrayList<ExportToExcel>();

			ExportToExcel expoExcel = new ExportToExcel();
			List<String> rowData = new ArrayList<String>();

			rowData.add("SR. No");
			rowData.add("ITEM NAME");
			rowData.add("OP QTY");
			rowData.add("OP VALUE");
			rowData.add("OP LANDING VALUE");
			rowData.add("RECEIVED QTY");
			rowData.add("RECEIVED VALUE");
			rowData.add("RECEIVED LANDING VALUE");
			rowData.add("ISSUE QTY");
			rowData.add("ISSUE VALUE");
			rowData.add("ISSUE LANDING VALUE");
			rowData.add("DAMAGE QTY");
			rowData.add("DAMAGE VALUE");
			rowData.add("DAMAGE LANDING VALUE");
			rowData.add("C\\L QTY");
			rowData.add("C\\L VALUE");
			rowData.add("DAMAGE LANDING VALUE");

			expoExcel.setRowData(rowData);
			exportToExcelList.add(expoExcel);
			int k = 0;
			for (int i = 0; i < getStockBetweenDate.size(); i++) {
				if (getStockBetweenDate.get(i).getOpeningStock() > 0 || getStockBetweenDate.get(i).getOpStockValue() > 0
						|| getStockBetweenDate.get(i).getApproveQty() > 0
						|| getStockBetweenDate.get(i).getApprovedQtyValue() > 0
						|| getStockBetweenDate.get(i).getIssueQty() > 0
						|| getStockBetweenDate.get(i).getApprovedQtyValue() > 0
						|| getStockBetweenDate.get(i).getIssueQty() > 0
						|| getStockBetweenDate.get(i).getIssueQtyValue() > 0
						|| getStockBetweenDate.get(i).getDamageQty() > 0
						|| getStockBetweenDate.get(i).getDamagValue() > 0) {
					expoExcel = new ExportToExcel();
					rowData = new ArrayList<String>();
					k++;
					rowData.add((k) + "");
					rowData.add(getStockBetweenDate.get(i).getItemCode());
					rowData.add("" + df.format(getStockBetweenDate.get(i).getOpeningStock()));
					rowData.add("" + df.format(getStockBetweenDate.get(i).getOpStockValue()));
					rowData.add("" + df.format(getStockBetweenDate.get(i).getOpLandingValue()));
					rowData.add("" + df.format(getStockBetweenDate.get(i).getApproveQty()));
					rowData.add("" + df.format(getStockBetweenDate.get(i).getApprovedQtyValue()));
					rowData.add("" + df.format(getStockBetweenDate.get(i).getApprovedLandingValue()));
					rowData.add("" + df.format(getStockBetweenDate.get(i).getIssueQty()));
					rowData.add("" + df.format(getStockBetweenDate.get(i).getIssueQtyValue()));
					rowData.add("" + df.format(getStockBetweenDate.get(i).getIssueLandingValue()));
					rowData.add("" + df.format(getStockBetweenDate.get(i).getDamageQty()));
					rowData.add("" + df.format(getStockBetweenDate.get(i).getDamagValue()));
					rowData.add("" + df.format(getStockBetweenDate.get(i).getDamageLandingValue()));

					float closingQty = getStockBetweenDate.get(i).getOpeningStock()
							+ getStockBetweenDate.get(i).getApproveQty() - getStockBetweenDate.get(i).getIssueQty()
							- getStockBetweenDate.get(i).getDamageQty();

					float closingValue = getStockBetweenDate.get(i).getOpStockValue()
							+ getStockBetweenDate.get(i).getApprovedQtyValue()
							- getStockBetweenDate.get(i).getIssueQtyValue()
							- getStockBetweenDate.get(i).getDamagValue();

					float closingLandingValue = getStockBetweenDate.get(i).getOpLandingValue()
							+ getStockBetweenDate.get(i).getApprovedLandingValue()
							- getStockBetweenDate.get(i).getIssueLandingValue()
							- getStockBetweenDate.get(i).getDamageLandingValue();

					rowData.add("" + df.format(closingQty));
					rowData.add("" + df.format(closingValue));
					rowData.add("" + df.format(closingLandingValue));
					expoExcel.setRowData(rowData);
					exportToExcelList.add(expoExcel);
				}

			}

			HttpSession session = request.getSession();
			session.setAttribute("exportExcelList", exportToExcelList);
			session.setAttribute("excelName", "ItemWiseStockValue");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "/stockValuetionReportItemWisePDF", method = RequestMethod.GET)
	public void stockValuetionReportItemWisePDF(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException {
		BufferedOutputStream outStream = null;
		try {
			Document document = new Document(PageSize.A4.rotate(), 10f, 10f, 10f, 0f);
			DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
			String reportDate = DF.format(new Date());
			document.addHeader("Date: ", reportDate);
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			System.out.println("time in Gen Bill PDF ==" + dateFormat.format(cal.getTime()));
			String timeStamp = dateFormat.format(cal.getTime());
			String FILE_PATH = Constants.REPORT_SAVE;
			File file = new File(FILE_PATH);

			PdfWriter writer = null;
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			FileOutputStream out = new FileOutputStream(FILE_PATH);
			try {
				writer = PdfWriter.getInstance(document, out);
			} catch (DocumentException e) {

				e.printStackTrace();
			}

			PdfPTable table = new PdfPTable(17);
			try {
				System.out.println("Inside PDF Table try");
				table.setWidthPercentage(100);
				table.setWidths(new float[] { 0.7f, 2.7f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
						1.0f, 1.0f, 1.0f, 1.0f, 1.0f });
				Font headFont = new Font(FontFamily.TIMES_ROMAN, 9, Font.NORMAL, BaseColor.BLACK);
				Font headFont1 = new Font(FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
				Font f = new Font(FontFamily.TIMES_ROMAN, 11.0f, Font.UNDERLINE, BaseColor.BLUE);
				Font f1 = new Font(FontFamily.TIMES_ROMAN, 9.0f, Font.BOLD, BaseColor.DARK_GRAY);

				PdfPCell hcell = new PdfPCell();

				hcell.setPadding(4);
				hcell = new PdfPCell(new Phrase("SR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ITEM NAME", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("OP QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("OP VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("OP LAND VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("RECEIVED QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("RECEIVED VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("RECEIVED LAND VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE LAND VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("DAMAGE QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("DAMAGE VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("DAMAGE LAND VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("C/L QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("C/L VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("C/L LAND VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				int index = 0;
				if (!itemWiseStockValuetionListForPDF.isEmpty()) {
					for (int k = 0; k < itemWiseStockValuetionListForPDF.size(); k++) {

						if (itemWiseStockValuetionListForPDF.get(k).getOpeningStock() > 0
								|| itemWiseStockValuetionListForPDF.get(k).getOpStockValue() > 0
								|| itemWiseStockValuetionListForPDF.get(k).getApproveQty() > 0
								|| itemWiseStockValuetionListForPDF.get(k).getApprovedQtyValue() > 0
								|| itemWiseStockValuetionListForPDF.get(k).getIssueQty() > 0
								|| itemWiseStockValuetionListForPDF.get(k).getIssueQtyValue() > 0
								|| itemWiseStockValuetionListForPDF.get(k).getDamageQty() > 0
								|| itemWiseStockValuetionListForPDF.get(k).getDamagValue() > 0) {

							index++;

							PdfPCell cell;

							cell = new PdfPCell(new Phrase("" + index, headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(
									new Phrase(itemWiseStockValuetionListForPDF.get(k).getItemCode(), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase(
									"" + df.format(itemWiseStockValuetionListForPDF.get(k).getOpeningStock()),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase(
									"" + df.format(itemWiseStockValuetionListForPDF.get(k).getOpStockValue()),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase(
									"" + df.format(itemWiseStockValuetionListForPDF.get(k).getOpLandingValue()),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase(
									"" + df.format(itemWiseStockValuetionListForPDF.get(k).getApproveQty()), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase(
									"" + df.format(itemWiseStockValuetionListForPDF.get(k).getApprovedQtyValue()),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase(
									"" + df.format(itemWiseStockValuetionListForPDF.get(k).getApprovedLandingValue()),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase(
									"" + df.format(itemWiseStockValuetionListForPDF.get(k).getIssueQty()), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase(
									"" + df.format(itemWiseStockValuetionListForPDF.get(k).getIssueQtyValue()),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase(
									"" + df.format(itemWiseStockValuetionListForPDF.get(k).getIssueLandingValue()),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase(
									"" + df.format(itemWiseStockValuetionListForPDF.get(k).getDamageQty()), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase(
									"" + df.format(itemWiseStockValuetionListForPDF.get(k).getDamagValue()), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase(
									"" + df.format(itemWiseStockValuetionListForPDF.get(k).getDamageLandingValue()),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							float closingQty = itemWiseStockValuetionListForPDF.get(k).getOpeningStock()
									+ itemWiseStockValuetionListForPDF.get(k).getApproveQty()
									- itemWiseStockValuetionListForPDF.get(k).getIssueQty()
									- itemWiseStockValuetionListForPDF.get(k).getDamageQty();

							float closingValue = itemWiseStockValuetionListForPDF.get(k).getOpStockValue()
									+ itemWiseStockValuetionListForPDF.get(k).getApprovedQtyValue()
									- itemWiseStockValuetionListForPDF.get(k).getIssueQtyValue()
									- itemWiseStockValuetionListForPDF.get(k).getDamagValue();

							float closingLandingValue = itemWiseStockValuetionListForPDF.get(k).getOpLandingValue()
									+ itemWiseStockValuetionListForPDF.get(k).getApprovedLandingValue()
									- itemWiseStockValuetionListForPDF.get(k).getIssueLandingValue()
									- itemWiseStockValuetionListForPDF.get(k).getDamageLandingValue();

							cell = new PdfPCell(new Phrase("" + df.format(closingQty), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase("" + df.format(closingValue), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase("" + df.format(closingLandingValue), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
						}
					}
				}

				document.open();
				Paragraph company = new Paragraph("\n" + comp.getCompanyName() + "\n", f);
				company.setAlignment(Element.ALIGN_CENTER);
				document.add(company);

				Paragraph heading1 = new Paragraph(comp.getOfficeAdd(), f1);
				heading1.setAlignment(Element.ALIGN_CENTER);
				document.add(heading1);

				Paragraph headingDate = new Paragraph(
						"Item Wise Stock Report , From Date: " + fromDate + "  To Date: " + toDate + "", f1);
				headingDate.setAlignment(Element.ALIGN_CENTER);
				document.add(headingDate);

				Paragraph ex3 = new Paragraph("\n");
				document.add(ex3);
				table.setHeaderRows(1);
				document.add(table);

				int totalPages = writer.getPageNumber();

				System.out.println("Page no " + totalPages);

				document.close();
				// Atul Sir code to open a Pdf File
				if (file != null) {

					String mimeType = URLConnection.guessContentTypeFromName(file.getName());

					if (mimeType == null) {

						mimeType = "application/pdf";

					}

					response.setContentType(mimeType);

					response.addHeader("content-disposition", String.format("inline; filename=\"%s\"", file.getName()));

					response.setContentLength((int) file.length());

					InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

					try {
						FileCopyUtils.copy(inputStream, response.getOutputStream());
					} catch (IOException e) {
						System.out.println("Excep in Opening a Pdf File");
						e.printStackTrace();
					}
				}

			} catch (DocumentException ex) {

				System.out.println("Pdf Generation Error" + ex.getMessage());

				ex.printStackTrace();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	List<StockValuationCategoryWise> categoryWiseIssueAndMrnForPdf = new ArrayList<StockValuationCategoryWise>();

	@RequestMapping(value = "/issueAndMrnReportCategoryWise", method = RequestMethod.GET)
	public ModelAndView issueAndMrnReportCategoryWise(HttpServletRequest request, HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/issueAndMrnReportCategoryWise");
		try {
			List<StockValuationCategoryWise> categoryWiseReport = new ArrayList<StockValuationCategoryWise>();
			Type[] type = rest.getForObject(Constants.url + "/getAlltype", Type[].class);
			List<Type> typeList = new ArrayList<Type>(Arrays.asList(type));
			model.addObject("typeList", typeList);

			if (request.getParameter("fromDate") == null || request.getParameter("toDate") == null
					|| request.getParameter("typeId") == null || request.getParameter("isDev") == null) {

				SimpleDateFormat yy = new SimpleDateFormat("yyyy-MM-dd");
				SimpleDateFormat dd = new SimpleDateFormat("dd-MM-yyyy");
				Date date = new Date();
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);

				fromDate = "01" + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.YEAR);
				toDate = dd.format(date);
				typeId = 0;
				isDev = -1;

				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				map.add("fromDate", DateConvertor.convertToYMD(fromDate));
				map.add("toDate", yy.format(date));
				map.add("typeId", typeId);
				map.add("isDev", isDev);
				System.out.println(map);
				StockValuationCategoryWise[] stockValuationCategoryWise1 = rest.postForObject(
						Constants.url + "/issueAndMrnCatWiseReport", map, StockValuationCategoryWise[].class);
				categoryWiseReport = new ArrayList<StockValuationCategoryWise>(
						Arrays.asList(stockValuationCategoryWise1));

				model.addObject("categoryWiseReport", categoryWiseReport);
				model.addObject("fromDate", fromDate);
				model.addObject("toDate", dd.format(date));
			} else {
				fromDate = request.getParameter("fromDate");
				toDate = request.getParameter("toDate");
				typeId = Integer.parseInt(request.getParameter("typeId"));
				isDev = Integer.parseInt(request.getParameter("isDev"));

				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				map.add("fromDate", DateConvertor.convertToYMD(fromDate));
				map.add("toDate", DateConvertor.convertToYMD(toDate));
				map.add("typeId", typeId);
				map.add("isDev", isDev);
				System.out.println(map);
				StockValuationCategoryWise[] stockValuationCategoryWise1 = rest.postForObject(
						Constants.url + "/issueAndMrnCatWiseReport", map, StockValuationCategoryWise[].class);
				categoryWiseReport = new ArrayList<StockValuationCategoryWise>(
						Arrays.asList(stockValuationCategoryWise1));

				model.addObject("categoryWiseReport", categoryWiseReport);
				model.addObject("fromDate", fromDate);
				model.addObject("toDate", toDate);
				model.addObject("typeId", typeId);
				model.addObject("isDevelompent", isDev);

			}

			// ----------------exel-------------------------
			DecimalFormat df = new DecimalFormat("####0.00");

			categoryWiseIssueAndMrnForPdf = categoryWiseReport;

			List<ExportToExcel> exportToExcelList = new ArrayList<ExportToExcel>();

			ExportToExcel expoExcel = new ExportToExcel();
			List<String> rowData = new ArrayList<String>();

			rowData.add("SR. No");
			rowData.add("CATEGORY NAME");
			rowData.add("MRN QTY");
			rowData.add("MRN VALUE");
			rowData.add("ISSUE QTY");
			rowData.add("ISSUE VALUE");

			expoExcel.setRowData(rowData);
			exportToExcelList.add(expoExcel);
			int k = 0;
			for (int i = 0; i < categoryWiseReport.size(); i++) {
				if (categoryWiseReport.get(i).getApproveQty() > 0 || categoryWiseReport.get(i).getApprovedQtyValue() > 0
						|| categoryWiseReport.get(i).getIssueQty() > 0
						|| categoryWiseReport.get(i).getApprovedQtyValue() > 0) {
					expoExcel = new ExportToExcel();
					rowData = new ArrayList<String>();
					k++;
					rowData.add((k) + "");
					rowData.add(categoryWiseReport.get(i).getCatDesc());
					rowData.add("" + df.format(categoryWiseReport.get(i).getApproveQty()));
					rowData.add("" + df.format(categoryWiseReport.get(i).getApprovedQtyValue()));
					rowData.add("" + df.format(categoryWiseReport.get(i).getIssueQty()));
					rowData.add("" + df.format(categoryWiseReport.get(i).getIssueQtyValue()));

					expoExcel.setRowData(rowData);
					exportToExcelList.add(expoExcel);
				}

			}

			HttpSession session = request.getSession();
			session.setAttribute("exportExcelList", exportToExcelList);
			session.setAttribute("excelName", "CategoryWiseMrnAndIssue");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "/listForIssueAndMrnGraphCategoryWise", method = RequestMethod.GET)
	public @ResponseBody List<StockValuationCategoryWise> listForIssueAndMrnGraph(HttpServletRequest request,
			HttpServletResponse response) {

		return categoryWiseIssueAndMrnForPdf;
	}

	@RequestMapping(value = "/issueAndMrnCategoryWisePDF", method = RequestMethod.GET)
	public void issueAndMrnCategoryWisePDF(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException {
		BufferedOutputStream outStream = null;
		try {
			Document document = new Document(PageSize.A4);
			DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
			String reportDate = DF.format(new Date());
			document.addHeader("Date: ", reportDate);
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			System.out.println("time in Gen Bill PDF ==" + dateFormat.format(cal.getTime()));
			String timeStamp = dateFormat.format(cal.getTime());
			String FILE_PATH = Constants.REPORT_SAVE;
			File file = new File(FILE_PATH);

			PdfWriter writer = null;
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			FileOutputStream out = new FileOutputStream(FILE_PATH);
			try {
				writer = PdfWriter.getInstance(document, out);
			} catch (DocumentException e) {

				e.printStackTrace();
			}

			float totalMrnQty = 0;
			float totalMrnValue = 0;
			float totalMrnLandingValue = 0;

			float totalIssueQty = 0;
			float totalIssueValue = 0;
			float totalIssueLandingValue = 0;

			PdfPTable table = new PdfPTable(8);
			try {
				System.out.println("Inside PDF Table try");
				table.setWidthPercentage(100);
				table.setWidths(new float[] { 0.4f, 3.0f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f });
				Font headFont = new Font(FontFamily.TIMES_ROMAN, 9, Font.NORMAL, BaseColor.BLACK);
				Font headFont1 = new Font(FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
				Font f = new Font(FontFamily.TIMES_ROMAN, 11.0f, Font.UNDERLINE, BaseColor.BLUE);
				Font f1 = new Font(FontFamily.TIMES_ROMAN, 9.0f, Font.BOLD, BaseColor.DARK_GRAY);

				PdfPCell hcell = new PdfPCell();

				hcell.setPadding(4);
				hcell = new PdfPCell(new Phrase("SR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("CATEGORY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MRN QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MRN VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MRN LANDING VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE LANDING VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				int index = 0;
				if (!categoryWiseIssueAndMrnForPdf.isEmpty()) {
					for (int k = 0; k < categoryWiseIssueAndMrnForPdf.size(); k++) {

						if (categoryWiseIssueAndMrnForPdf.get(k).getOpeningStock() > 0
								|| categoryWiseIssueAndMrnForPdf.get(k).getOpStockValue() > 0
								|| categoryWiseIssueAndMrnForPdf.get(k).getApproveQty() > 0
								|| categoryWiseIssueAndMrnForPdf.get(k).getApprovedQtyValue() > 0
								|| categoryWiseIssueAndMrnForPdf.get(k).getIssueQty() > 0
								|| categoryWiseIssueAndMrnForPdf.get(k).getIssueQtyValue() > 0
								|| categoryWiseIssueAndMrnForPdf.get(k).getDamageQty() > 0
								|| categoryWiseIssueAndMrnForPdf.get(k).getDamageValue() > 0) {

							index++;

							PdfPCell cell;

							cell = new PdfPCell(new Phrase("" + index, headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(
									new Phrase(categoryWiseIssueAndMrnForPdf.get(k).getCatDesc(), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase(
									"" + df.format(categoryWiseIssueAndMrnForPdf.get(k).getApproveQty()), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalMrnQty = totalMrnQty + categoryWiseIssueAndMrnForPdf.get(k).getApproveQty();

							cell = new PdfPCell(new Phrase(
									"" + df.format(categoryWiseIssueAndMrnForPdf.get(k).getApprovedQtyValue()),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalMrnValue = totalMrnValue + categoryWiseIssueAndMrnForPdf.get(k).getApprovedQtyValue();

							cell = new PdfPCell(new Phrase(
									"" + df.format(categoryWiseIssueAndMrnForPdf.get(k).getApprovedLandingValue()),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalMrnLandingValue = totalMrnLandingValue
									+ categoryWiseIssueAndMrnForPdf.get(k).getApprovedLandingValue();

							cell = new PdfPCell(new Phrase(
									"" + df.format(categoryWiseIssueAndMrnForPdf.get(k).getIssueQty()), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalIssueQty = totalIssueQty + categoryWiseIssueAndMrnForPdf.get(k).getIssueQty();

							cell = new PdfPCell(new Phrase(
									"" + df.format(categoryWiseIssueAndMrnForPdf.get(k).getIssueQtyValue()), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalIssueValue = totalIssueValue + categoryWiseIssueAndMrnForPdf.get(k).getIssueQtyValue();

							cell = new PdfPCell(new Phrase(
									"" + df.format(categoryWiseIssueAndMrnForPdf.get(k).getIssueLandingValue()),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalIssueLandingValue = totalIssueLandingValue
									+ categoryWiseIssueAndMrnForPdf.get(k).getIssueLandingValue();
						}
					}
				}

				PdfPCell cell;

				cell = new PdfPCell(new Phrase("Total ", headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_LEFT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				cell.setColspan(2);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalMrnQty), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalMrnValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalMrnLandingValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalIssueQty), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalIssueValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalIssueLandingValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				document.open();
				Paragraph company = new Paragraph("\n" + comp.getCompanyName() + "\n", f);
				company.setAlignment(Element.ALIGN_CENTER);
				document.add(company);

				Paragraph heading1 = new Paragraph(comp.getOfficeAdd(), f1);
				heading1.setAlignment(Element.ALIGN_CENTER);
				document.add(heading1);

				Paragraph headingDate = new Paragraph(
						"Category Wise Issue And Mrn Report , From Date: " + fromDate + "  To Date: " + toDate + "",
						f1);
				headingDate.setAlignment(Element.ALIGN_CENTER);
				document.add(headingDate);

				Paragraph ex3 = new Paragraph("\n");
				document.add(ex3);
				table.setHeaderRows(1);
				document.add(table);

				int totalPages = writer.getPageNumber();

				System.out.println("Page no " + totalPages);

				document.close();
				// Atul Sir code to open a Pdf File
				if (file != null) {

					String mimeType = URLConnection.guessContentTypeFromName(file.getName());

					if (mimeType == null) {

						mimeType = "application/pdf";

					}

					response.setContentType(mimeType);

					response.addHeader("content-disposition", String.format("inline; filename=\"%s\"", file.getName()));

					response.setContentLength((int) file.length());

					InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

					try {
						FileCopyUtils.copy(inputStream, response.getOutputStream());
					} catch (IOException e) {
						System.out.println("Excep in Opening a Pdf File");
						e.printStackTrace();
					}
				}

			} catch (DocumentException ex) {

				System.out.println("Pdf Generation Error" + ex.getMessage());

				ex.printStackTrace();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	List<IssueAndMrnGroupWise> issueAndMrnGroupWiseListForPdf = new ArrayList<IssueAndMrnGroupWise>();

	@RequestMapping(value = "/issueAndMrnReportGroupWise/{catId}", method = RequestMethod.GET)
	public ModelAndView issueAndMrnReportGroupWise(@PathVariable int catId, HttpServletRequest request,
			HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/issueAndMrnReportGroupWise");
		List<IssueAndMrnGroupWise> groupWiseList = new ArrayList<IssueAndMrnGroupWise>();

		try {

			MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
			map.add("fromDate", DateConvertor.convertToYMD(fromDate));
			map.add("toDate", DateConvertor.convertToYMD(toDate));
			map.add("catId", catId);
			map.add("typeId", typeId);
			map.add("isDev", isDev);
			System.out.println(map);
			IssueAndMrnGroupWise[] issueAndMrnGroupWise = rest.postForObject(
					Constants.url + "/issueAndMrnGroupWisReportByCatId", map, IssueAndMrnGroupWise[].class);
			groupWiseList = new ArrayList<IssueAndMrnGroupWise>(Arrays.asList(issueAndMrnGroupWise));

			List<ExportToExcel> exportToExcelList = new ArrayList<ExportToExcel>();

			ExportToExcel expoExcel = new ExportToExcel();
			List<String> rowData = new ArrayList<String>();

			rowData.add("SR. No");
			rowData.add("GROUP NAME");
			rowData.add("MRN QTY");
			rowData.add("MRN VALUE");
			rowData.add("ISSUE QTY");
			rowData.add("ISSUE VALUE");

			expoExcel.setRowData(rowData);
			exportToExcelList.add(expoExcel);
			int k = 0;
			DecimalFormat df = new DecimalFormat("####0.00");

			for (int i = 0; i < groupWiseList.size(); i++) {
				if (groupWiseList.get(i).getApproveQty() > 0 || groupWiseList.get(i).getApprovedQtyValue() > 0
						|| groupWiseList.get(i).getIssueQty() > 0 || groupWiseList.get(i).getApprovedQtyValue() > 0) {
					expoExcel = new ExportToExcel();
					rowData = new ArrayList<String>();
					k++;
					rowData.add((k) + "");
					rowData.add(groupWiseList.get(i).getGrpCode());
					rowData.add("" + df.format(groupWiseList.get(i).getApproveQty()));
					rowData.add("" + df.format(groupWiseList.get(i).getApprovedQtyValue()));
					rowData.add("" + df.format(groupWiseList.get(i).getIssueQty()));
					rowData.add("" + df.format(groupWiseList.get(i).getIssueQtyValue()));

					expoExcel.setRowData(rowData);
					exportToExcelList.add(expoExcel);
				}

			}

			HttpSession session = request.getSession();
			session.setAttribute("exportExcelList", exportToExcelList);
			session.setAttribute("excelName", "GroupWiseMrnAndIssue");

			issueAndMrnGroupWiseListForPdf = groupWiseList;

			model.addObject("list", groupWiseList);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "/listForIssueAndMrnGraphGroupWise", method = RequestMethod.GET)
	public @ResponseBody List<IssueAndMrnGroupWise> listForIssueAndMrnGraphGroupWise(HttpServletRequest request,
			HttpServletResponse response) {

		return issueAndMrnGroupWiseListForPdf;
	}

	@RequestMapping(value = "/issueAndMrnGroupWisePDF", method = RequestMethod.GET)
	public void issueAndMrnGroupWisePDF(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException {
		BufferedOutputStream outStream = null;
		try {
			Document document = new Document(PageSize.A4);
			DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
			String reportDate = DF.format(new Date());
			document.addHeader("Date: ", reportDate);
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			System.out.println("time in Gen Bill PDF ==" + dateFormat.format(cal.getTime()));
			String timeStamp = dateFormat.format(cal.getTime());
			String FILE_PATH = Constants.REPORT_SAVE;
			File file = new File(FILE_PATH);

			PdfWriter writer = null;
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			FileOutputStream out = new FileOutputStream(FILE_PATH);
			try {
				writer = PdfWriter.getInstance(document, out);
			} catch (DocumentException e) {

				e.printStackTrace();
			}

			PdfPTable table = new PdfPTable(8);
			try {
				System.out.println("Inside PDF Table try");
				table.setWidthPercentage(100);
				table.setWidths(new float[] { 0.4f, 3.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f });
				Font headFont = new Font(FontFamily.TIMES_ROMAN, 9, Font.NORMAL, BaseColor.BLACK);
				Font headFont1 = new Font(FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
				Font f = new Font(FontFamily.TIMES_ROMAN, 11.0f, Font.UNDERLINE, BaseColor.BLUE);
				Font f1 = new Font(FontFamily.TIMES_ROMAN, 9.0f, Font.BOLD, BaseColor.DARK_GRAY);

				PdfPCell hcell = new PdfPCell();

				hcell.setPadding(4);
				hcell = new PdfPCell(new Phrase("SR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("GROUP", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MRN QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MRN VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MRN LAND VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE LAND VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				float totalMrnQty = 0;
				float totalMrnValue = 0;
				float totalMrnLandingValue = 0;

				float totalIssueQty = 0;
				float totalIssueValue = 0;
				float totalIssueLandingValue = 0;

				int index = 0;
				if (!issueAndMrnGroupWiseListForPdf.isEmpty()) {
					for (int k = 0; k < issueAndMrnGroupWiseListForPdf.size(); k++) {

						if (issueAndMrnGroupWiseListForPdf.get(k).getApproveQty() > 0
								|| issueAndMrnGroupWiseListForPdf.get(k).getApprovedQtyValue() > 0
								|| issueAndMrnGroupWiseListForPdf.get(k).getIssueQty() > 0
								|| issueAndMrnGroupWiseListForPdf.get(k).getIssueQtyValue() > 0) {

							index++;

							PdfPCell cell;

							cell = new PdfPCell(new Phrase("" + index, headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(
									new Phrase(issueAndMrnGroupWiseListForPdf.get(k).getGrpCode(), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase(
									"" + df.format(issueAndMrnGroupWiseListForPdf.get(k).getApproveQty()), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalMrnQty = totalMrnQty + issueAndMrnGroupWiseListForPdf.get(k).getApproveQty();

							cell = new PdfPCell(new Phrase(
									"" + df.format(issueAndMrnGroupWiseListForPdf.get(k).getApprovedQtyValue()),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalMrnValue = totalMrnValue + issueAndMrnGroupWiseListForPdf.get(k).getApprovedQtyValue();

							cell = new PdfPCell(new Phrase(
									"" + df.format(issueAndMrnGroupWiseListForPdf.get(k).getApprovedLandingValue()),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalMrnLandingValue = totalMrnLandingValue
									+ issueAndMrnGroupWiseListForPdf.get(k).getApprovedLandingValue();

							cell = new PdfPCell(new Phrase(
									"" + df.format(issueAndMrnGroupWiseListForPdf.get(k).getIssueQty()), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalIssueQty = totalIssueQty + issueAndMrnGroupWiseListForPdf.get(k).getIssueQty();

							cell = new PdfPCell(
									new Phrase("" + df.format(issueAndMrnGroupWiseListForPdf.get(k).getIssueQtyValue()),
											headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalIssueValue = totalIssueValue
									+ issueAndMrnGroupWiseListForPdf.get(k).getIssueQtyValue();

							cell = new PdfPCell(new Phrase(
									"" + df.format(issueAndMrnGroupWiseListForPdf.get(k).getIssueLandingValue()),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalIssueLandingValue = totalIssueLandingValue
									+ issueAndMrnGroupWiseListForPdf.get(k).getIssueLandingValue();

						}
					}
				}

				PdfPCell cell;

				cell = new PdfPCell(new Phrase("Total ", headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_LEFT);
				cell.setPadding(3);
				cell.setColspan(2);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalMrnQty), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalMrnValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalMrnLandingValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalIssueQty), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalIssueValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalIssueLandingValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				document.open();
				Paragraph company = new Paragraph("\n" + comp.getCompanyName() + "\n", f);
				company.setAlignment(Element.ALIGN_CENTER);
				document.add(company);

				Paragraph heading1 = new Paragraph(comp.getOfficeAdd(), f1);
				heading1.setAlignment(Element.ALIGN_CENTER);
				document.add(heading1);

				Paragraph headingDate = new Paragraph(
						"Group Wise Issue And Mrn Report , From Date: " + fromDate + "  To Date: " + toDate + "", f1);
				headingDate.setAlignment(Element.ALIGN_CENTER);
				document.add(headingDate);

				Paragraph ex3 = new Paragraph("\n");
				document.add(ex3);
				table.setHeaderRows(1);
				document.add(table);

				int totalPages = writer.getPageNumber();

				System.out.println("Page no " + totalPages);

				document.close();
				// Atul Sir code to open a Pdf File
				if (file != null) {

					String mimeType = URLConnection.guessContentTypeFromName(file.getName());

					if (mimeType == null) {

						mimeType = "application/pdf";

					}

					response.setContentType(mimeType);

					response.addHeader("content-disposition", String.format("inline; filename=\"%s\"", file.getName()));

					response.setContentLength((int) file.length());

					InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

					try {
						FileCopyUtils.copy(inputStream, response.getOutputStream());
					} catch (IOException e) {
						System.out.println("Excep in Opening a Pdf File");
						e.printStackTrace();
					}
				}

			} catch (DocumentException ex) {

				System.out.println("Pdf Generation Error" + ex.getMessage());

				ex.printStackTrace();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	List<IssueAndMrnItemWise> itemWiseIssueAndMrnListForPdf = new ArrayList<IssueAndMrnItemWise>();

	@RequestMapping(value = "/issueAndMrnReportItemWise/{groupId}", method = RequestMethod.GET)
	public ModelAndView issueAndMrnReportItemWise(@PathVariable int groupId, HttpServletRequest request,
			HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/issueAndMrnReportItemWise");
		List<IssueAndMrnItemWise> itemWiseList = new ArrayList<IssueAndMrnItemWise>();

		try {
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
			map.add("fromDate", DateConvertor.convertToYMD(fromDate));
			map.add("toDate", DateConvertor.convertToYMD(toDate));
			map.add("groupId", groupId);
			map.add("typeId", typeId);
			map.add("isDev", isDev);
			System.out.println(map);
			IssueAndMrnItemWise[] issueAndMrnGroupWise = rest.postForObject(
					Constants.url + "/issueAndMrnItemWiseReportByGroupId", map, IssueAndMrnItemWise[].class);
			itemWiseList = new ArrayList<IssueAndMrnItemWise>(Arrays.asList(issueAndMrnGroupWise));

			List<ExportToExcel> exportToExcelList = new ArrayList<ExportToExcel>();

			ExportToExcel expoExcel = new ExportToExcel();
			List<String> rowData = new ArrayList<String>();

			rowData.add("SR. No");
			rowData.add("ITEM NAME");
			rowData.add("MRN QTY");
			rowData.add("MRN VALUE");
			rowData.add("ISSUE QTY");
			rowData.add("ISSUE VALUE");

			expoExcel.setRowData(rowData);
			exportToExcelList.add(expoExcel);
			int k = 0;
			for (int i = 0; i < itemWiseList.size(); i++) {
				if (itemWiseList.get(i).getApproveQty() > 0 || itemWiseList.get(i).getApprovedQtyValue() > 0
						|| itemWiseList.get(i).getIssueQty() > 0 || itemWiseList.get(i).getApprovedQtyValue() > 0) {
					expoExcel = new ExportToExcel();
					rowData = new ArrayList<String>();
					k++;
					rowData.add((k) + "");
					rowData.add(itemWiseList.get(i).getItemCode());
					rowData.add("" + df.format(itemWiseList.get(i).getApproveQty()));
					rowData.add("" + df.format(itemWiseList.get(i).getApprovedQtyValue()));
					rowData.add("" + df.format(itemWiseList.get(i).getIssueQty()));
					rowData.add("" + df.format(itemWiseList.get(i).getIssueQtyValue()));

					expoExcel.setRowData(rowData);
					exportToExcelList.add(expoExcel);
				}

			}

			HttpSession session = request.getSession();
			session.setAttribute("exportExcelList", exportToExcelList);
			session.setAttribute("excelName", "ItemWiseMrnAndIssue");

			model.addObject("list", itemWiseList);
			itemWiseIssueAndMrnListForPdf = itemWiseList;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "/issueAndMrnItemWisePDF", method = RequestMethod.GET)
	public void issueAndMrnItemWisePDF(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException {
		BufferedOutputStream outStream = null;
		try {
			Document document = new Document(PageSize.A4);
			DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
			String reportDate = DF.format(new Date());
			document.addHeader("Date: ", reportDate);
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			System.out.println("time in Gen Bill PDF ==" + dateFormat.format(cal.getTime()));
			String timeStamp = dateFormat.format(cal.getTime());
			String FILE_PATH = Constants.REPORT_SAVE;
			File file = new File(FILE_PATH);

			PdfWriter writer = null;
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			FileOutputStream out = new FileOutputStream(FILE_PATH);
			try {
				writer = PdfWriter.getInstance(document, out);
			} catch (DocumentException e) {

				e.printStackTrace();
			}

			PdfPTable table = new PdfPTable(8);
			try {
				System.out.println("Inside PDF Table try");
				table.setWidthPercentage(100);
				table.setWidths(new float[] { 0.4f, 3.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f });
				Font headFont = new Font(FontFamily.TIMES_ROMAN, 9, Font.NORMAL, BaseColor.BLACK);
				Font headFont1 = new Font(FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
				Font f = new Font(FontFamily.TIMES_ROMAN, 11.0f, Font.UNDERLINE, BaseColor.BLUE);
				Font f1 = new Font(FontFamily.TIMES_ROMAN, 9.0f, Font.BOLD, BaseColor.DARK_GRAY);

				PdfPCell hcell = new PdfPCell();

				hcell.setPadding(4);
				hcell = new PdfPCell(new Phrase("SR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ITEM", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MRN QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MRN VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MRN LAND VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE LAND VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				float totalMrnQty = 0;
				float totalMrnValue = 0;
				float totalMrnLandingValue = 0;

				float totalIssueQty = 0;
				float totalIssueValue = 0;
				float totalIssueLandingValue = 0;

				int index = 0;
				if (!itemWiseIssueAndMrnListForPdf.isEmpty()) {
					for (int k = 0; k < itemWiseIssueAndMrnListForPdf.size(); k++) {

						if (itemWiseIssueAndMrnListForPdf.get(k).getApproveQty() > 0
								|| itemWiseIssueAndMrnListForPdf.get(k).getApprovedQtyValue() > 0
								|| itemWiseIssueAndMrnListForPdf.get(k).getIssueQty() > 0
								|| itemWiseIssueAndMrnListForPdf.get(k).getIssueQtyValue() > 0) {

							index++;

							PdfPCell cell;

							cell = new PdfPCell(new Phrase("" + index, headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(
									new Phrase(itemWiseIssueAndMrnListForPdf.get(k).getItemCode(), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase(
									"" + df.format(itemWiseIssueAndMrnListForPdf.get(k).getApproveQty()), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalMrnQty = totalMrnQty + itemWiseIssueAndMrnListForPdf.get(k).getApproveQty();

							cell = new PdfPCell(new Phrase(
									"" + df.format(itemWiseIssueAndMrnListForPdf.get(k).getApprovedQtyValue()),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalMrnValue = totalMrnValue + itemWiseIssueAndMrnListForPdf.get(k).getApprovedQtyValue();

							cell = new PdfPCell(new Phrase(
									"" + df.format(itemWiseIssueAndMrnListForPdf.get(k).getApprovedLandingValue()),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalMrnLandingValue = totalMrnLandingValue
									+ itemWiseIssueAndMrnListForPdf.get(k).getApprovedLandingValue();

							cell = new PdfPCell(new Phrase(
									"" + df.format(itemWiseIssueAndMrnListForPdf.get(k).getIssueQty()), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalIssueQty = totalIssueQty + itemWiseIssueAndMrnListForPdf.get(k).getIssueQty();

							cell = new PdfPCell(new Phrase(
									"" + df.format(itemWiseIssueAndMrnListForPdf.get(k).getIssueQtyValue()), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalIssueValue = totalIssueValue + itemWiseIssueAndMrnListForPdf.get(k).getIssueQtyValue();

							cell = new PdfPCell(new Phrase(
									"" + df.format(itemWiseIssueAndMrnListForPdf.get(k).getIssueLandingValue()),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalIssueLandingValue = totalIssueLandingValue
									+ itemWiseIssueAndMrnListForPdf.get(k).getIssueLandingValue();
						}
					}
				}

				PdfPCell cell;

				cell = new PdfPCell(new Phrase("Total ", headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_LEFT);
				cell.setPadding(3);
				cell.setColspan(2);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalMrnQty), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalMrnValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalMrnLandingValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalIssueQty), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalIssueValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalIssueLandingValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				document.open();
				Paragraph company = new Paragraph("\n" + comp.getCompanyName(), f);
				company.setAlignment(Element.ALIGN_CENTER);
				document.add(company);

				Paragraph heading1 = new Paragraph(comp.getOfficeAdd(), f1);
				heading1.setAlignment(Element.ALIGN_CENTER);
				document.add(heading1);

				Paragraph headingDate = new Paragraph(
						"Item Wise Issue And Mrn Report , From Date: " + fromDate + "  To Date: " + toDate + "", f1);
				headingDate.setAlignment(Element.ALIGN_CENTER);
				document.add(headingDate);

				Paragraph ex3 = new Paragraph("\n");
				document.add(ex3);
				table.setHeaderRows(1);
				document.add(table);

				int totalPages = writer.getPageNumber();

				System.out.println("Page no " + totalPages);

				document.close();
				// Atul Sir code to open a Pdf File
				if (file != null) {

					String mimeType = URLConnection.guessContentTypeFromName(file.getName());

					if (mimeType == null) {

						mimeType = "application/pdf";

					}

					response.setContentType(mimeType);

					response.addHeader("content-disposition", String.format("inline; filename=\"%s\"", file.getName()));

					response.setContentLength((int) file.length());

					InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

					try {
						FileCopyUtils.copy(inputStream, response.getOutputStream());
					} catch (IOException e) {
						System.out.println("Excep in Opening a Pdf File");
						e.printStackTrace();
					}
				}

			} catch (DocumentException ex) {

				System.out.println("Pdf Generation Error" + ex.getMessage());

				ex.printStackTrace();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	List<IssueDeptWise> deptWiselistGlobal = null;

	@RequestMapping(value = "/issueReportDeptWise", method = RequestMethod.GET)
	public ModelAndView issueReportDeptWise(HttpServletRequest request, HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/issueReportDeptWise");
		try {
			List<IssueDeptWise> deptWiselist = new ArrayList<IssueDeptWise>();
			Type[] type = rest.getForObject(Constants.url + "/getAlltype", Type[].class);
			List<Type> typeList = new ArrayList<Type>(Arrays.asList(type));

			Dept[] Dept = rest.getForObject(Constants.url + "/getAllDeptByIsUsed", Dept[].class);
			List<Dept> deparmentList = new ArrayList<Dept>(Arrays.asList(Dept));

			model.addObject("deparmentList", deparmentList);
			model.addObject("typeList", typeList);

			if (request.getParameter("fromDate") == null || request.getParameter("toDate") == null
					|| request.getParameter("typeId") == null || request.getParameter("isDev") == null
					|| request.getParameter("deptId") == null) {

				SimpleDateFormat yy = new SimpleDateFormat("yyyy-MM-dd");
				SimpleDateFormat dd = new SimpleDateFormat("dd-MM-yyyy");
				Date date = new Date();
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);

				fromDate = "01" + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.YEAR);
				toDate = dd.format(date);
				typeId = 0;
				isDev = -1;
				deptId = 0;

				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				map.add("fromDate", DateConvertor.convertToYMD(fromDate));
				map.add("toDate", yy.format(date));
				map.add("typeId", typeId);
				map.add("isDev", isDev);
				map.add("deptId", deptId);
				System.out.println(map);
				IssueDeptWise[] IssueDeptWise = rest.postForObject(Constants.url + "/issueDepartmentWiseReport", map,
						IssueDeptWise[].class);
				deptWiselist = new ArrayList<IssueDeptWise>(Arrays.asList(IssueDeptWise));

				model.addObject("deptWiselist", deptWiselist);
				model.addObject("fromDate", fromDate);
				model.addObject("toDate", toDate);
				deptWiselistGlobal = deptWiselist;
			} else {
				fromDate = request.getParameter("fromDate");
				toDate = request.getParameter("toDate");
				typeId = Integer.parseInt(request.getParameter("typeId"));
				isDev = Integer.parseInt(request.getParameter("isDev"));
				deptId = Integer.parseInt(request.getParameter("deptId"));
				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				map.add("fromDate", DateConvertor.convertToYMD(fromDate));
				map.add("toDate", DateConvertor.convertToYMD(toDate));
				map.add("typeId", typeId);
				map.add("isDev", isDev);
				map.add("deptId", deptId);
				System.out.println(map);
				IssueDeptWise[] IssueDeptWise = rest.postForObject(Constants.url + "/issueDepartmentWiseReport", map,
						IssueDeptWise[].class);
				deptWiselist = new ArrayList<IssueDeptWise>(Arrays.asList(IssueDeptWise));

				deptWiselistGlobal = deptWiselist;
				model.addObject("deptWiselist", deptWiselist);
				model.addObject("fromDate", fromDate);
				model.addObject("toDate", toDate);
				model.addObject("typeId", typeId);
				model.addObject("isDevelompent", isDev);
				model.addObject("deptId", deptId);

			}
			// ------------------------ Export To
			// Excel--------------------------------------
			DecimalFormat df = new DecimalFormat("####0.00");
			List<ExportToExcel> exportToExcelList = new ArrayList<ExportToExcel>();

			ExportToExcel expoExcel = new ExportToExcel();
			List<String> rowData = new ArrayList<String>();

			rowData.add("SR. No");
			rowData.add("DEPARMENT NAME");
			rowData.add("ISSUE QTY");
			rowData.add("ISSUE VALUE");

			expoExcel.setRowData(rowData);
			exportToExcelList.add(expoExcel);
			for (int i = 0; i < deptWiselist.size(); i++) {
				expoExcel = new ExportToExcel();
				rowData = new ArrayList<String>();

				rowData.add((i + 1) + "");
				rowData.add(deptWiselist.get(i).getDeptCode());
				rowData.add("" + df.format(deptWiselist.get(i).getIssueQty()));
				rowData.add("" + df.format(deptWiselist.get(i).getIssueQtyValue()));

				expoExcel.setRowData(rowData);
				exportToExcelList.add(expoExcel);

			}

			HttpSession session = request.getSession();
			session.setAttribute("exportExcelList", exportToExcelList);
			session.setAttribute("excelName", "DeptWiseConsumption(Issues)");
			// ------------------------------------END------------------------------------------
		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "/getIssueReportDeptWise", method = RequestMethod.GET)
	public @ResponseBody List<IssueDeptWise> getIssueReportDeptWise(HttpServletRequest request,
			HttpServletResponse response) {

		List<IssueDeptWise> deptWiselist = new ArrayList<IssueDeptWise>();

		try {

			if (request.getParameter("fromDate") == null || request.getParameter("toDate") == null
					|| request.getParameter("typeId") == null || request.getParameter("isDev") == null
					|| request.getParameter("deptId") == null) {

			} else {
				fromDate = request.getParameter("fromDate");
				toDate = request.getParameter("toDate");
				typeId = Integer.parseInt(request.getParameter("typeId"));
				isDev = Integer.parseInt(request.getParameter("isDev"));
				deptId = Integer.parseInt(request.getParameter("deptId"));
				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				map.add("fromDate", DateConvertor.convertToYMD(fromDate));
				map.add("toDate", DateConvertor.convertToYMD(toDate));
				map.add("typeId", typeId);
				map.add("isDev", isDev);
				map.add("deptId", deptId);
				System.out.println(map);
				IssueDeptWise[] IssueDeptWise = rest.postForObject(Constants.url + "/issueDepartmentWiseReport", map,
						IssueDeptWise[].class);
				deptWiselist = new ArrayList<IssueDeptWise>(Arrays.asList(IssueDeptWise));

				deptWiselistGlobal = deptWiselist;

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return deptWiselist;
	}

	@RequestMapping(value = "/issueReportSubDeptWise/{deptId}", method = RequestMethod.GET)
	public ModelAndView issueReportSubDeptWise(@PathVariable int deptId, HttpServletRequest request,
			HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/issueReportSubDeptWise");
		try {
			List<IssueDeptWise> deptWiselist = new ArrayList<IssueDeptWise>();

			MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
			map.add("fromDate", DateConvertor.convertToYMD(fromDate));
			map.add("toDate", DateConvertor.convertToYMD(toDate));
			map.add("typeId", typeId);
			map.add("isDev", isDev);
			map.add("deptId", deptId);
			System.out.println(map);
			IssueDeptWise[] IssueDeptWise = rest.postForObject(Constants.url + "/issueSubDepartmentWiseReport", map,
					IssueDeptWise[].class);
			deptWiselist = new ArrayList<IssueDeptWise>(Arrays.asList(IssueDeptWise));

			model.addObject("deptWiselist", deptWiselist);
			model.addObject("deptId", deptId);
			deptWiselistGlobal = deptWiselist;
			// ------------------------ Export To
			// Excel--------------------------------------
			DecimalFormat df = new DecimalFormat("####0.00");
			List<ExportToExcel> exportToExcelList = new ArrayList<ExportToExcel>();

			ExportToExcel expoExcel = new ExportToExcel();
			List<String> rowData = new ArrayList<String>();

			rowData.add("SR. No");
			rowData.add("SUB-DEPARMENT NAME");
			rowData.add("ISSUE QTY");
			rowData.add("ISSUE VALUE");

			expoExcel.setRowData(rowData);
			exportToExcelList.add(expoExcel);
			for (int i = 0; i < deptWiselist.size(); i++) {
				expoExcel = new ExportToExcel();
				rowData = new ArrayList<String>();

				rowData.add((i + 1) + "");
				rowData.add(deptWiselist.get(i).getDeptCode());
				rowData.add("" + df.format(deptWiselist.get(i).getIssueQty()));
				rowData.add("" + df.format(deptWiselist.get(i).getIssueQtyValue()));

				expoExcel.setRowData(rowData);
				exportToExcelList.add(expoExcel);

			}

			HttpSession session = request.getSession();
			session.setAttribute("exportExcelList", exportToExcelList);
			session.setAttribute("excelName", "SubDeptWiseConsumption(Issues)");
			// ------------------------------------END------------------------------------------

		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "/issueReportSubDeptWiseReport", method = RequestMethod.GET)
	public @ResponseBody List<IssueDeptWise> issueReportSubDeptWiseReport(HttpServletRequest request,
			HttpServletResponse response) {
		List<IssueDeptWise> deptWiselist = new ArrayList<IssueDeptWise>();

		try {
			deptId = Integer.parseInt(request.getParameter("deptId"));
			MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
			map.add("fromDate", DateConvertor.convertToYMD(fromDate));
			map.add("toDate", DateConvertor.convertToYMD(toDate));
			map.add("typeId", typeId);
			map.add("isDev", isDev);
			map.add("deptId", deptId);
			System.out.println(map);
			IssueDeptWise[] IssueDeptWise = rest.postForObject(Constants.url + "/issueSubDepartmentWiseReport", map,
					IssueDeptWise[].class);
			deptWiselist = new ArrayList<IssueDeptWise>(Arrays.asList(IssueDeptWise));
			deptWiselistGlobal = deptWiselist;
			System.err.println(deptWiselistGlobal.toString());

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return deptWiselist;
	}

	@RequestMapping(value = "/issueReportItemWise/{subDeptId}", method = RequestMethod.GET)
	public ModelAndView issueReportItemWise(@PathVariable int subDeptId, HttpServletRequest request,
			HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/issueReportItemWise");
		try {
			List<IssueDeptWise> itemWiselist = new ArrayList<IssueDeptWise>();
			DecimalFormat df = new DecimalFormat("####0.00");

			MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
			map.add("fromDate", DateConvertor.convertToYMD(fromDate));
			map.add("toDate", DateConvertor.convertToYMD(toDate));
			map.add("typeId", typeId);
			map.add("isDev", isDev);
			map.add("subDept", subDeptId);
			System.out.println(map);
			IssueDeptWise[] IssueDeptWise = rest.postForObject(Constants.url + "/issueItemWiseReportBySubDept", map,
					IssueDeptWise[].class);
			itemWiselist = new ArrayList<IssueDeptWise>(Arrays.asList(IssueDeptWise));
			deptWiselistGlobal = itemWiselist;
			model.addObject("itemWiselist", itemWiselist);
			// ------------------------ Export To
			// Excel--------------------------------------
			List<ExportToExcel> exportToExcelList = new ArrayList<ExportToExcel>();

			ExportToExcel expoExcel = new ExportToExcel();
			List<String> rowData = new ArrayList<String>();

			rowData.add("SR. No");
			rowData.add("ITEM NAME");
			rowData.add("ISSUE QTY");
			rowData.add("ISSUE VALUE");

			expoExcel.setRowData(rowData);
			exportToExcelList.add(expoExcel);
			for (int i = 0; i < itemWiselist.size(); i++) {
				expoExcel = new ExportToExcel();
				rowData = new ArrayList<String>();

				rowData.add((i + 1) + "");
				rowData.add(itemWiselist.get(i).getDeptCode());
				rowData.add("" + df.format(itemWiselist.get(i).getIssueQty()));
				rowData.add("" + df.format(itemWiselist.get(i).getIssueQtyValue()));

				expoExcel.setRowData(rowData);
				exportToExcelList.add(expoExcel);

			}

			HttpSession session = request.getSession();
			session.setAttribute("exportExcelList", exportToExcelList);
			session.setAttribute("excelName", "ItemWiseConsumption(Issues)");
			// ------------------------------------END------------------------------------------

		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "/issueReportDeptWisePDF", method = RequestMethod.GET)
	public void showProdByOrderPdf(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException {
		BufferedOutputStream outStream = null;
		try {
			Document document = new Document(PageSize.A4);
			DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
			String reportDate = DF.format(new Date());
			document.addHeader("Date: ", reportDate);
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			System.out.println("time in Gen Bill PDF ==" + dateFormat.format(cal.getTime()));
			String timeStamp = dateFormat.format(cal.getTime());
			String FILE_PATH = Constants.REPORT_SAVE;
			File file = new File(FILE_PATH);

			PdfWriter writer = null;
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			FileOutputStream out = new FileOutputStream(FILE_PATH);
			try {
				writer = PdfWriter.getInstance(document, out);
			} catch (DocumentException e) {

				e.printStackTrace();
			}

			PdfPTable table = new PdfPTable(5);
			try {
				System.out.println("Inside PDF Table try");
				table.setWidthPercentage(100);
				table.setWidths(new float[] { 0.4f, 3.0f, 1.5f, 1.5f, 1.5f });
				Font headFont = new Font(FontFamily.TIMES_ROMAN, 9, Font.NORMAL, BaseColor.BLACK);
				Font headFont1 = new Font(FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
				Font f = new Font(FontFamily.TIMES_ROMAN, 11.0f, Font.UNDERLINE, BaseColor.BLUE);
				Font f1 = new Font(FontFamily.TIMES_ROMAN, 9.0f, Font.BOLD, BaseColor.DARK_GRAY);

				PdfPCell hcell = new PdfPCell();

				hcell.setPadding(4);
				hcell = new PdfPCell(new Phrase("SR.NO.", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("DEPARTMENT NAME", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE LAND VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				float totalIssueQty = 0;
				float totalIssueValue = 0;
				float totalIssueLandingValue = 0;

				int index = 0;
				if (!deptWiselistGlobal.isEmpty()) {
					for (int k = 0; k < deptWiselistGlobal.size(); k++) {

						index++;

						PdfPCell cell;

						cell = new PdfPCell(new Phrase("" + index, headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(new Phrase(deptWiselistGlobal.get(k).getDeptCode(), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(
								new Phrase("" + df.format(deptWiselistGlobal.get(k).getIssueQty()), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);
						totalIssueQty = totalIssueQty + deptWiselistGlobal.get(k).getIssueQty();

						cell = new PdfPCell(
								new Phrase("" + df.format(deptWiselistGlobal.get(k).getIssueQtyValue()), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);
						totalIssueValue = totalIssueValue + deptWiselistGlobal.get(k).getIssueQtyValue();

						cell = new PdfPCell(
								new Phrase("" + df.format(deptWiselistGlobal.get(k).getIssueLandingValue()), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);
						totalIssueLandingValue = totalIssueLandingValue
								+ deptWiselistGlobal.get(k).getIssueLandingValue();
					}
				}

				PdfPCell cell;

				cell = new PdfPCell(new Phrase("Total ", headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_LEFT);
				cell.setPadding(3);
				cell.setColspan(2);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalIssueQty), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalIssueValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalIssueLandingValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				document.open();
				Paragraph company = new Paragraph("\n" + comp.getCompanyName() + "\n", f);
				company.setAlignment(Element.ALIGN_CENTER);
				document.add(company);

				Paragraph heading1 = new Paragraph(comp.getOfficeAdd() + "\n", f1);
				heading1.setAlignment(Element.ALIGN_CENTER);
				document.add(heading1);

				Paragraph headingDate = new Paragraph(
						"Department Wise Consumption(Issues), From Date: " + fromDate + "  To Date: " + toDate + "",
						f1);
				headingDate.setAlignment(Element.ALIGN_CENTER);
				document.add(headingDate);

				Paragraph ex3 = new Paragraph("\n");
				document.add(ex3);
				table.setHeaderRows(1);
				document.add(table);

				int totalPages = writer.getPageNumber();

				System.out.println("Page no " + totalPages);

				document.close();
				// Atul Sir code to open a Pdf File
				if (file != null) {

					String mimeType = URLConnection.guessContentTypeFromName(file.getName());

					if (mimeType == null) {

						mimeType = "application/pdf";

					}

					response.setContentType(mimeType);

					response.addHeader("content-disposition", String.format("inline; filename=\"%s\"", file.getName()));

					response.setContentLength((int) file.length());

					InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

					try {
						FileCopyUtils.copy(inputStream, response.getOutputStream());
					} catch (IOException e) {
						System.out.println("Excep in Opening a Pdf File");
						e.printStackTrace();
					}
				}

			} catch (DocumentException ex) {

				System.out.println("Pdf Generation Error" + ex.getMessage());

				ex.printStackTrace();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@RequestMapping(value = "/issueReportSubDeptWisePDF", method = RequestMethod.GET)
	public void issueReportSubDeptWisePDF(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException {
		BufferedOutputStream outStream = null;
		try {
			Document document = new Document(PageSize.A4);
			DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
			String reportDate = DF.format(new Date());
			document.addHeader("Date: ", reportDate);
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			System.out.println("time in Gen Bill PDF ==" + dateFormat.format(cal.getTime()));
			String timeStamp = dateFormat.format(cal.getTime());
			String FILE_PATH = Constants.REPORT_SAVE;
			File file = new File(FILE_PATH);

			PdfWriter writer = null;
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			FileOutputStream out = new FileOutputStream(FILE_PATH);
			try {
				writer = PdfWriter.getInstance(document, out);
			} catch (DocumentException e) {

				e.printStackTrace();
			}

			PdfPTable table = new PdfPTable(5);
			try {
				System.out.println("Inside PDF Table try");
				table.setWidthPercentage(100);
				table.setWidths(new float[] { 0.4f, 3.0f, 1.5f, 1.5f, 1.5f });
				Font headFont = new Font(FontFamily.TIMES_ROMAN, 9, Font.NORMAL, BaseColor.BLACK);
				Font headFont1 = new Font(FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
				Font f = new Font(FontFamily.TIMES_ROMAN, 11.0f, Font.UNDERLINE, BaseColor.BLUE);
				Font f1 = new Font(FontFamily.TIMES_ROMAN, 9.0f, Font.BOLD, BaseColor.DARK_GRAY);

				PdfPCell hcell = new PdfPCell();

				hcell.setPadding(4);
				hcell = new PdfPCell(new Phrase("SR.NO.", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("DEPARTMENT NAME", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE LANDING VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				float totalIssueQty = 0;
				float totalIssueValue = 0;
				float totalIssueLandingValue = 0;

				int index = 0;
				if (!deptWiselistGlobal.isEmpty()) {
					for (int k = 0; k < deptWiselistGlobal.size(); k++) {

						index++;

						PdfPCell cell;

						cell = new PdfPCell(new Phrase("" + index, headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(new Phrase(deptWiselistGlobal.get(k).getDeptCode(), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(
								new Phrase("" + df.format(deptWiselistGlobal.get(k).getIssueQty()), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);
						totalIssueQty = totalIssueQty + deptWiselistGlobal.get(k).getIssueQty();

						cell = new PdfPCell(
								new Phrase("" + df.format(deptWiselistGlobal.get(k).getIssueQtyValue()), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);
						totalIssueValue = totalIssueValue + deptWiselistGlobal.get(k).getIssueQtyValue();

						cell = new PdfPCell(
								new Phrase("" + df.format(deptWiselistGlobal.get(k).getIssueLandingValue()), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);
						totalIssueLandingValue = totalIssueLandingValue
								+ deptWiselistGlobal.get(k).getIssueLandingValue();
					}
				}

				PdfPCell cell;

				cell = new PdfPCell(new Phrase("Total ", headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_LEFT);
				cell.setPadding(3);
				cell.setColspan(2);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalIssueQty), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalIssueValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalIssueLandingValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				document.open();
				Paragraph company = new Paragraph("\n" + comp.getCompanyName() + "\n", f);
				company.setAlignment(Element.ALIGN_CENTER);
				document.add(company);

				Paragraph heading1 = new Paragraph(comp.getOfficeAdd() + "\n", f1);
				heading1.setAlignment(Element.ALIGN_CENTER);
				document.add(heading1);
				Paragraph ex2 = new Paragraph("\n");
				document.add(ex2);

				Paragraph headingDate = new Paragraph(
						"Sub-Department Wise Consumption(Issues), From Date: " + fromDate + "  To Date: " + toDate + "",
						f1);
				headingDate.setAlignment(Element.ALIGN_CENTER);

				document.add(headingDate);

				Paragraph ex3 = new Paragraph("\n");
				document.add(ex3);
				table.setHeaderRows(1);
				document.add(table);

				int totalPages = writer.getPageNumber();

				System.out.println("Page no " + totalPages);

				document.close();
				// Atul Sir code to open a Pdf File
				if (file != null) {

					String mimeType = URLConnection.guessContentTypeFromName(file.getName());

					if (mimeType == null) {

						mimeType = "application/pdf";

					}

					response.setContentType(mimeType);

					response.addHeader("content-disposition", String.format("inline; filename=\"%s\"", file.getName()));

					response.setContentLength((int) file.length());

					InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

					try {
						FileCopyUtils.copy(inputStream, response.getOutputStream());
					} catch (IOException e) {
						System.out.println("Excep in Opening a Pdf File");
						e.printStackTrace();
					}
				}

			} catch (DocumentException ex) {

				System.out.println("Pdf Generation Error" + ex.getMessage());

				ex.printStackTrace();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@RequestMapping(value = "/issueReportItemWisePDF", method = RequestMethod.GET)
	public void issueReportItemWisePDF(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException {
		BufferedOutputStream outStream = null;
		try {
			Document document = new Document(PageSize.A4);
			DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
			String reportDate = DF.format(new Date());
			document.addHeader("Date: ", reportDate);
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			System.out.println("time in Gen Bill PDF ==" + dateFormat.format(cal.getTime()));
			String timeStamp = dateFormat.format(cal.getTime());
			String FILE_PATH = Constants.REPORT_SAVE;
			File file = new File(FILE_PATH);

			PdfWriter writer = null;
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			FileOutputStream out = new FileOutputStream(FILE_PATH);
			try {
				writer = PdfWriter.getInstance(document, out);
			} catch (DocumentException e) {

				e.printStackTrace();
			}

			PdfPTable table = new PdfPTable(5);
			try {
				System.out.println("Inside PDF Table try");
				table.setWidthPercentage(100);
				table.setWidths(new float[] { 0.4f, 4.9f, 1.3f, 1.3f, 1.3f });
				Font headFont = new Font(FontFamily.TIMES_ROMAN, 9, Font.NORMAL, BaseColor.BLACK);
				Font headFont1 = new Font(FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
				Font f = new Font(FontFamily.TIMES_ROMAN, 11.0f, Font.UNDERLINE, BaseColor.BLUE);
				Font f1 = new Font(FontFamily.TIMES_ROMAN, 9.0f, Font.BOLD, BaseColor.GRAY);

				PdfPCell hcell = new PdfPCell();

				hcell.setPadding(4);
				hcell = new PdfPCell(new Phrase("SR.", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ITEM NAME", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE LANDING VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				float totalIssueQty = 0;
				float totalIssueValue = 0;
				float totalIssueLandingValue = 0;

				int index = 0;
				if (!deptWiselistGlobal.isEmpty()) {
					for (int k = 0; k < deptWiselistGlobal.size(); k++) {

						if (deptWiselistGlobal.get(k).getIssueQty() > 0
								|| deptWiselistGlobal.get(k).getIssueQtyValue() > 0) {
							index++;

							PdfPCell cell;

							cell = new PdfPCell(new Phrase("" + index, headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase(deptWiselistGlobal.get(k).getDeptCode(), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(
									new Phrase("" + df.format(deptWiselistGlobal.get(k).getIssueQty()), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalIssueQty = totalIssueQty + deptWiselistGlobal.get(k).getIssueQty();

							cell = new PdfPCell(
									new Phrase("" + df.format(deptWiselistGlobal.get(k).getIssueQtyValue()), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalIssueValue = totalIssueValue + deptWiselistGlobal.get(k).getIssueQtyValue();

							cell = new PdfPCell(new Phrase(
									"" + df.format(deptWiselistGlobal.get(k).getIssueLandingValue()), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							totalIssueLandingValue = totalIssueLandingValue
									+ deptWiselistGlobal.get(k).getIssueLandingValue();
						}

					}
				}

				PdfPCell cell;

				cell = new PdfPCell(new Phrase("Total ", headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_LEFT);
				cell.setPadding(3);
				cell.setColspan(2);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalIssueQty), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalIssueValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalIssueLandingValue), headFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				document.open();
				Paragraph company = new Paragraph("\n" + comp.getCompanyName() + "\n", f);
				company.setAlignment(Element.ALIGN_CENTER);
				document.add(company);

				Paragraph heading1 = new Paragraph(comp.getOfficeAdd() + "\n", f1);
				heading1.setAlignment(Element.ALIGN_CENTER);
				document.add(heading1);

				Paragraph headingDate = new Paragraph(
						"Item Wise Consumption(Issues), From Date: " + fromDate + "  To Date: " + toDate + "", f1);
				headingDate.setAlignment(Element.ALIGN_CENTER);

				document.add(headingDate);

				Paragraph ex3 = new Paragraph("\n");
				document.add(ex3);
				table.setHeaderRows(1);

				document.add(table);

				int totalPages = writer.getPageNumber();

				System.out.println("Page no " + totalPages);

				document.close();
				// Atul Sir code to open a Pdf File
				if (file != null) {

					String mimeType = URLConnection.guessContentTypeFromName(file.getName());

					if (mimeType == null) {

						mimeType = "application/pdf";

					}

					response.setContentType(mimeType);

					response.addHeader("content-disposition", String.format("inline; filename=\"%s\"", file.getName()));

					response.setContentLength((int) file.length());

					InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

					try {
						FileCopyUtils.copy(inputStream, response.getOutputStream());
					} catch (IOException e) {
						System.out.println("Excep in Opening a Pdf File");
						e.printStackTrace();
					}
				}

			} catch (DocumentException ex) {

				System.out.println("Pdf Generation Error" + ex.getMessage());

				ex.printStackTrace();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@RequestMapping(value = "/issueMonthWieReport", method = RequestMethod.GET)
	public ModelAndView issueMonthWieReport(HttpServletRequest request, HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/issueMonthWieReport");
		try {
			List<IssueMonthWiseList> list = new ArrayList<IssueMonthWiseList>();
			Type[] type = rest.getForObject(Constants.url + "/getAlltype", Type[].class);
			List<Type> typeList = new ArrayList<Type>(Arrays.asList(type));
			model.addObject("typeList", typeList);

			if (request.getParameter("typeId") == null || request.getParameter("isDev") == null) {

				typeId = 0;
				isDev = -1;

				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				/*
				 * map.add("fromDate",DateConvertor.convertToYMD(fromDate));
				 * map.add("toDate",DateConvertor.convertToYMD(toDate));
				 */

				Date date = new Date();
				LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
				year = localDate.getYear();

				System.out.println("year" + year);

				map.add("typeId", typeId);
				map.add("isDev", isDev);
				map.add("isDev", isDev);
				map.add("year", year);
				System.out.println(map);
				IssueMonthWiseList[] issueMonthWiseList = rest
						.postForObject(Constants.url + "/issueMonthWiseReportByDept", map, IssueMonthWiseList[].class);
				list = new ArrayList<IssueMonthWiseList>(Arrays.asList(issueMonthWiseList));

				System.out.println("list " + list);

				for (int i = 0; i < list.size(); i++) {

					model.addObject("month" + i, list.get(i));
				}
				listGlobal = list;
				model.addObject("list", list);
				model.addObject("fromDate", fromDate);
				model.addObject("toDate", toDate);
				model.addObject("typeId", typeId);
				model.addObject("isDevelompent", isDev);

				Dept[] Dept = rest.getForObject(Constants.url + "/getAllDeptByIsUsed", Dept[].class);
				deparmentList = new ArrayList<Dept>(Arrays.asList(Dept));
				model.addObject("deparmentList", deparmentList);
			} else {
				/*
				 * fromDate = request.getParameter("fromDate"); toDate =
				 * request.getParameter("toDate");
				 */
				typeId = Integer.parseInt(request.getParameter("typeId"));
				isDev = Integer.parseInt(request.getParameter("isDev"));
				year = Integer.parseInt(request.getParameter("Year"));

				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				/*
				 * map.add("fromDate",DateConvertor.convertToYMD(fromDate));
				 * map.add("toDate",DateConvertor.convertToYMD(toDate));
				 */
				map.add("typeId", typeId);
				map.add("isDev", isDev);
				map.add("year", year);
				System.out.println(map);
				IssueMonthWiseList[] issueMonthWiseList = rest
						.postForObject(Constants.url + "/issueMonthWiseReportByDept", map, IssueMonthWiseList[].class);
				list = new ArrayList<IssueMonthWiseList>(Arrays.asList(issueMonthWiseList));

				System.out.println("list " + list);

				for (int i = 0; i < list.size(); i++) {

					model.addObject("month" + i, list.get(i));
				}
				listGlobal = list;
				model.addObject("list", list);
				model.addObject("fromDate", fromDate);
				model.addObject("toDate", toDate);
				model.addObject("typeId", typeId);
				model.addObject("isDevelompent", isDev);

				Dept[] Dept = rest.getForObject(Constants.url + "/getAllDeptByIsUsed", Dept[].class);
				deparmentList = new ArrayList<Dept>(Arrays.asList(Dept));
				model.addObject("deparmentList", deparmentList);

			}

			companyInfo = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			// ------------------------ Export To
			// Excel--------------------------------------
			List<ExportToExcel> exportToExcelList = new ArrayList<ExportToExcel>();

			ExportToExcel expoExcel = new ExportToExcel();
			List<String> rowData = new ArrayList<String>();

			/*
			 * rowData.add("SR. No"); rowData.add("DEPARMENT NAME");
			 * rowData.add("APR ISSUE QTY"); rowData.add("APR ISSUE VALUE");
			 * rowData.add("MAY ISSUE QTY"); rowData.add("MAY ISSUE VALUE");
			 * rowData.add("JUNE ISSUE QTY"); rowData.add("JUNE ISSUE VALUE");
			 * rowData.add("JULY ISSUE QTY"); rowData.add("JULY ISSUE VALUE");
			 * rowData.add("AUGUST ISSUE QTY"); rowData.add("AUGUST ISSUE VALUE");
			 * rowData.add("SEPTEMBR ISSUE QTY"); rowData.add("SEPTEMBR ISSUE VALUE");
			 * rowData.add("OCTOMBER ISSUE QTY"); rowData.add("OCTOMBER ISSUE VALUE");
			 * rowData.add("NOVEMBER ISSUE QTY"); rowData.add("NOVEMBER ISSUE VALUE");
			 * rowData.add("DECEMBER ISSUE QTY"); rowData.add("DECEMBER ISSUE VALUE");
			 * rowData.add("JANUARY ISSUE QTY"); rowData.add("JANUARY ISSUE VALUE");
			 * rowData.add("FEBRUARY ISSUE QTY"); rowData.add("FEBRUARY ISSUE VALUE");
			 * rowData.add("MARCH ISSUE QTY"); rowData.add("MARCH ISSUE VALUE");
			 */

			rowData.add("SR. No");
			rowData.add("DEPARMENT NAME");
			rowData.add("APR ISSUE VALUE");
			rowData.add("MAY ISSUE VALUE");
			rowData.add("JUNE ISSUE VALUE");
			rowData.add("JULY ISSUE VALUE");
			rowData.add("AUGUST ISSUE VALUE");
			rowData.add("SEPTEMBR ISSUE VALUE");
			rowData.add("OCTOMBER ISSUE VALUE");
			rowData.add("NOVEMBER ISSUE VALUE");
			rowData.add("DECEMBER ISSUE VALUE");
			rowData.add("JANUARY ISSUE VALUE");
			rowData.add("FEBRUARY ISSUE VALUE");
			rowData.add("MARCH ISSUE VALUE");

			expoExcel.setRowData(rowData);

			exportToExcelList.add(expoExcel);
			for (int i = 0; i < deparmentList.size(); i++) {
				expoExcel = new ExportToExcel();
				rowData = new ArrayList<String>();

				rowData.add((i + 1) + "");
				rowData.add(deparmentList.get(i).getDeptCode() + " " + deparmentList.get(i).getDeptDesc());
				for (int k = 0; k < list.size(); k++) {
					List<MonthWiseIssueReport> monthList = list.get(k).getMonthList();

					for (int j = 0; j < monthList.size(); j++) {
						if (monthList.get(j).getDeptId() == deparmentList.get(i).getDeptId()) {
							// rowData.add(""+monthList.get(j).getIssueQty());
							rowData.add("" + monthList.get(j).getIssueQtyValue());
						}
					}

				}

				expoExcel.setRowData(rowData);
				exportToExcelList.add(expoExcel);

			}

			HttpSession session = request.getSession();
			session.setAttribute("exportExcelList", exportToExcelList);
			session.setAttribute("excelName", "MonthWiseConsumption(Issues)");
			// ------------------------------------END------------------------------------------

		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "/issueMonthQtyWieReport", method = RequestMethod.GET)
	public ModelAndView issueMonthQtyWieReport(HttpServletRequest request, HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/issueMonthQtyWieReport");
		try {
			List<IssueMonthWiseList> list = new ArrayList<IssueMonthWiseList>();
			Type[] type = rest.getForObject(Constants.url + "/getAlltype", Type[].class);
			List<Type> typeList = new ArrayList<Type>(Arrays.asList(type));
			model.addObject("typeList", typeList);

			if (request.getParameter("typeId") == null || request.getParameter("isDev") == null) {

				typeId = 0;
				isDev = -1;

				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				/*
				 * map.add("fromDate",DateConvertor.convertToYMD(fromDate));
				 * map.add("toDate",DateConvertor.convertToYMD(toDate));
				 */
				map.add("typeId", typeId);
				map.add("isDev", isDev);
				System.out.println(map);
				IssueMonthWiseList[] issueMonthWiseList = rest
						.postForObject(Constants.url + "/issueMonthWiseReportByDept", map, IssueMonthWiseList[].class);
				list = new ArrayList<IssueMonthWiseList>(Arrays.asList(issueMonthWiseList));

				System.out.println("list " + list);

				for (int i = 0; i < list.size(); i++) {

					model.addObject("month" + i, list.get(i));
				}
				listGlobal = list;
				model.addObject("list", list);
				model.addObject("fromDate", fromDate);
				model.addObject("toDate", toDate);
				model.addObject("typeId", typeId);
				model.addObject("isDevelompent", isDev);

				Dept[] Dept = rest.getForObject(Constants.url + "/getAllDeptByIsUsed", Dept[].class);
				deparmentList = new ArrayList<Dept>(Arrays.asList(Dept));
				model.addObject("deparmentList", deparmentList);
			} else {
				/*
				 * fromDate = request.getParameter("fromDate"); toDate =
				 * request.getParameter("toDate");
				 */
				typeId = Integer.parseInt(request.getParameter("typeId"));
				isDev = Integer.parseInt(request.getParameter("isDev"));

				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				/*
				 * map.add("fromDate",DateConvertor.convertToYMD(fromDate));
				 * map.add("toDate",DateConvertor.convertToYMD(toDate));
				 */
				map.add("typeId", typeId);
				map.add("isDev", isDev);
				System.out.println(map);
				IssueMonthWiseList[] issueMonthWiseList = rest
						.postForObject(Constants.url + "/issueMonthWiseReportByDept", map, IssueMonthWiseList[].class);
				list = new ArrayList<IssueMonthWiseList>(Arrays.asList(issueMonthWiseList));

				System.out.println("list " + list);

				for (int i = 0; i < list.size(); i++) {

					model.addObject("month" + i, list.get(i));
				}
				listGlobal = list;
				model.addObject("list", list);
				model.addObject("fromDate", fromDate);
				model.addObject("toDate", toDate);
				model.addObject("typeId", typeId);
				model.addObject("isDevelompent", isDev);

				Dept[] Dept = rest.getForObject(Constants.url + "/getAllDeptByIsUsed", Dept[].class);
				deparmentList = new ArrayList<Dept>(Arrays.asList(Dept));
				model.addObject("deparmentList", deparmentList);

			}

			// ------------------------ Export To
			// Excel--------------------------------------
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			List<ExportToExcel> exportToExcelList = new ArrayList<ExportToExcel>();

			ExportToExcel expoExcel = new ExportToExcel();
			List<String> rowData = new ArrayList<String>();

			rowData.add("SR. No");
			rowData.add("DEPARMENT NAME");
			rowData.add("APR ISSUE QTY");
			rowData.add("MAY ISSUE QTY");
			rowData.add("JUNE ISSUE QTY");
			rowData.add("JULY ISSUE QTY");
			rowData.add("AUGUST ISSUE QTY");
			rowData.add("SEPTEMBR ISSUE QTY");
			rowData.add("OCTOMBER ISSUE QTY");
			rowData.add("NOVEMBER ISSUE QTY");
			rowData.add("DECEMBER ISSUE QTY");
			rowData.add("JANUARY ISSUE QTY");
			rowData.add("FEBRUARY ISSUE QTY");
			rowData.add("MARCH ISSUE QTY");

			expoExcel.setRowData(rowData);

			exportToExcelList.add(expoExcel);
			for (int i = 0; i < deparmentList.size(); i++) {
				expoExcel = new ExportToExcel();
				rowData = new ArrayList<String>();

				rowData.add((i + 1) + "");
				rowData.add(deparmentList.get(i).getDeptCode() + " " + deparmentList.get(i).getDeptDesc());
				for (int k = 0; k < list.size(); k++) {
					List<MonthWiseIssueReport> monthList = list.get(k).getMonthList();

					for (int j = 0; j < monthList.size(); j++) {
						if (monthList.get(j).getDeptId() == deparmentList.get(i).getDeptId()) {
							// rowData.add(""+monthList.get(j).getIssueQty());
							rowData.add("" + df.format(monthList.get(j).getIssueQty()));
						}
					}

				}

				expoExcel.setRowData(rowData);
				exportToExcelList.add(expoExcel);

			}

			HttpSession session = request.getSession();
			session.setAttribute("exportExcelList", exportToExcelList);
			session.setAttribute("excelName", "MonthQtyWiseConsumption(Issues)");
			// ------------------------------------END------------------------------------------

		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "/listForIssueGraphDeptWise", method = RequestMethod.GET)
	public @ResponseBody List<IssueMonthWiseList> listForIssueGraphDeptWise(HttpServletRequest request,
			HttpServletResponse response) {

		return listGlobal;
	}

	@RequestMapping(value = "/getDeptListForGraph", method = RequestMethod.GET)
	public @ResponseBody List<Dept> getDeptListForGraph(HttpServletRequest request, HttpServletResponse response) {

		return deparmentList;
	}

	@RequestMapping(value = "/issueMonthWieReportPdf", method = RequestMethod.GET)
	public void issueMonthWieReportPdf(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException {
		BufferedOutputStream outStream = null;
		try {
			Document document = new Document(PageSize.A4.rotate(), 10f, 10f, 10f, 0f);
			DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
			String reportDate = DF.format(new Date());
			document.addHeader("Date: ", reportDate);
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			System.out.println("time in Gen Bill PDF ==" + dateFormat.format(cal.getTime()));
			String timeStamp = dateFormat.format(cal.getTime());
			String FILE_PATH = Constants.REPORT_SAVE;
			File file = new File(FILE_PATH);

			PdfWriter writer = null;
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			FileOutputStream out = new FileOutputStream(FILE_PATH);
			try {
				writer = PdfWriter.getInstance(document, out);
			} catch (DocumentException e) {

				e.printStackTrace();
			}

			PdfPTable table = new PdfPTable(14);
			try {

				System.out.println("Inside PDF Table try");
				table.setWidthPercentage(100);
				table.setWidths(new float[] { 0.4f, 1.7f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
						1.0f, 1.0f });
				Font headFont = new Font(FontFamily.TIMES_ROMAN, 8, Font.NORMAL, BaseColor.BLACK);
				Font headFont1 = new Font(FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
				Font f = new Font(FontFamily.TIMES_ROMAN, 11.0f, Font.UNDERLINE, BaseColor.BLUE);
				Font f1 = new Font(FontFamily.TIMES_ROMAN, 9.0f, Font.BOLD, BaseColor.DARK_GRAY);
				PdfPCell hcell = new PdfPCell();

				hcell.setPadding(4);
				hcell = new PdfPCell(new Phrase("SR.", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("Department Name", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("APR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);
				hcell = new PdfPCell(new Phrase("MAY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);
				hcell = new PdfPCell(new Phrase("JUN", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);
				hcell = new PdfPCell(new Phrase("JUL", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);
				hcell = new PdfPCell(new Phrase("AUG", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("SEP", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("OCT", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("NOV", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("DEC", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JAN", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("FEB", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MAR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				int index = 0;
				if (!deparmentList.isEmpty()) {
					for (int k = 0; k < deparmentList.size(); k++) {

						index++;

						PdfPCell cell;

						cell = new PdfPCell(new Phrase("" + index, headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(new Phrase(
								deparmentList.get(k).getDeptCode() + " " + deparmentList.get(k).getDeptDesc(),
								headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						for (int j = 0; j < listGlobal.size(); j++) {

							List<MonthWiseIssueReport> monthList = listGlobal.get(j).getMonthList();

							for (int l = 0; l < monthList.size(); l++) {
								if (monthList.get(l).getDeptId() == deparmentList.get(k).getDeptId()) {
									/*
									 * cell = new PdfPCell(new Phrase(""+monthList.get(l).getIssueQty(), headFont));
									 * cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
									 * cell.setHorizontalAlignment(Element.ALIGN_RIGHT); cell.setPaddingRight(2);
									 * cell.setPadding(3); table.addCell(cell);
									 */

									cell = new PdfPCell(
											new Phrase("" + df.format(monthList.get(l).getIssueQtyValue()), headFont));
									cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
									cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
									cell.setPaddingRight(2);
									cell.setPadding(3);
									table.addCell(cell);
								}
							}
						}

					}
				}

				document.open();
				Paragraph company = new Paragraph("\n" + comp.getCompanyName(), f);
				company.setAlignment(Element.ALIGN_CENTER);
				document.add(company);

				Paragraph heading1 = new Paragraph(comp.getOfficeAdd(), f1);
				heading1.setAlignment(Element.ALIGN_CENTER);
				document.add(heading1);
				Paragraph ex2 = new Paragraph("\n");
				document.add(ex2);

				Paragraph headingDate = new Paragraph("Month Wise Consumption(Issues)", f1);
				headingDate.setAlignment(Element.ALIGN_CENTER);
				document.add(headingDate);

				Paragraph ex3 = new Paragraph("\n");
				document.add(ex3);
				table.setHeaderRows(1);
				document.add(table);

				int totalPages = writer.getPageNumber();

				System.out.println("Page no " + totalPages);

				document.close();
				// Atul Sir code to open a Pdf File
				if (file != null) {

					String mimeType = URLConnection.guessContentTypeFromName(file.getName());

					if (mimeType == null) {

						mimeType = "application/pdf";

					}

					response.setContentType(mimeType);

					response.addHeader("content-disposition", String.format("inline; filename=\"%s\"", file.getName()));

					response.setContentLength((int) file.length());

					InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

					try {
						FileCopyUtils.copy(inputStream, response.getOutputStream());
					} catch (IOException e) {
						System.out.println("Excep in Opening a Pdf File");
						e.printStackTrace();
					}
				}

			} catch (DocumentException ex) {

				System.out.println("Pdf Generation Error" + ex.getMessage());

				ex.printStackTrace();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@RequestMapping(value = "/issueMonthWieQtyReportPdf", method = RequestMethod.GET)
	public void issueMonthWieQtyReportPdf(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException {
		BufferedOutputStream outStream = null;
		try {
			Document document = new Document(PageSize.A4.rotate(), 10f, 10f, 10f, 0f);
			DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
			String reportDate = DF.format(new Date());
			document.addHeader("Date: ", reportDate);
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			System.out.println("time in Gen Bill PDF ==" + dateFormat.format(cal.getTime()));
			String timeStamp = dateFormat.format(cal.getTime());
			String FILE_PATH = Constants.REPORT_SAVE;
			File file = new File(FILE_PATH);

			PdfWriter writer = null;
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			FileOutputStream out = new FileOutputStream(FILE_PATH);
			try {
				writer = PdfWriter.getInstance(document, out);
			} catch (DocumentException e) {

				e.printStackTrace();
			}

			PdfPTable table = new PdfPTable(14);
			try {

				System.out.println("Inside PDF Table try");
				table.setWidthPercentage(100);
				table.setWidths(new float[] { 0.4f, 1.7f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
						1.0f, 1.0f });
				Font headFont = new Font(FontFamily.TIMES_ROMAN, 8, Font.NORMAL, BaseColor.BLACK);
				Font headFont1 = new Font(FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
				Font f = new Font(FontFamily.TIMES_ROMAN, 11.0f, Font.UNDERLINE, BaseColor.BLUE);
				Font f1 = new Font(FontFamily.TIMES_ROMAN, 9.0f, Font.BOLD, BaseColor.DARK_GRAY);
				PdfPCell hcell = new PdfPCell();

				hcell.setPadding(4);
				hcell = new PdfPCell(new Phrase("SR.", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("Department Name", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("APR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);
				hcell = new PdfPCell(new Phrase("MAY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);
				hcell = new PdfPCell(new Phrase("JUN", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);
				hcell = new PdfPCell(new Phrase("JUL", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);
				hcell = new PdfPCell(new Phrase("AUG", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("SEP", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("OCT", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("NOV", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("DEC", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JAN", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("FEB", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MAR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				int index = 0;
				if (!deparmentList.isEmpty()) {
					for (int k = 0; k < deparmentList.size(); k++) {

						index++;

						PdfPCell cell;

						cell = new PdfPCell(new Phrase("" + index, headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(new Phrase(
								deparmentList.get(k).getDeptCode() + " " + deparmentList.get(k).getDeptDesc(),
								headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						for (int j = 0; j < listGlobal.size(); j++) {

							List<MonthWiseIssueReport> monthList = listGlobal.get(j).getMonthList();

							for (int l = 0; l < monthList.size(); l++) {
								if (monthList.get(l).getDeptId() == deparmentList.get(k).getDeptId()) {
									/*
									 * cell = new PdfPCell(new Phrase(""+monthList.get(l).getIssueQty(), headFont));
									 * cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
									 * cell.setHorizontalAlignment(Element.ALIGN_RIGHT); cell.setPaddingRight(2);
									 * cell.setPadding(3); table.addCell(cell);
									 */

									cell = new PdfPCell(new Phrase("" + monthList.get(l).getIssueQty(), headFont));
									cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
									cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
									cell.setPaddingRight(2);
									cell.setPadding(3);
									table.addCell(cell);
								}
							}
						}

					}
				}

				document.open();
				Paragraph company = new Paragraph("\n" + comp.getCompanyName(), f);
				company.setAlignment(Element.ALIGN_CENTER);
				document.add(company);

				Paragraph heading1 = new Paragraph(comp.getOfficeAdd() + "\n", f1);
				heading1.setAlignment(Element.ALIGN_CENTER);
				document.add(heading1);
				Paragraph ex2 = new Paragraph("\n");
				document.add(ex2);

				Paragraph headingDate = new Paragraph("Month Wise Consumption(Issues)", f1);
				headingDate.setAlignment(Element.ALIGN_CENTER);
				document.add(headingDate);

				Paragraph ex3 = new Paragraph("\n");
				document.add(ex3);
				table.setHeaderRows(1);
				document.add(table);

				int totalPages = writer.getPageNumber();

				System.out.println("Page no " + totalPages);

				document.close();
				// Atul Sir code to open a Pdf File
				if (file != null) {

					String mimeType = URLConnection.guessContentTypeFromName(file.getName());

					if (mimeType == null) {

						mimeType = "application/pdf";

					}

					response.setContentType(mimeType);

					response.addHeader("content-disposition", String.format("inline; filename=\"%s\"", file.getName()));

					response.setContentLength((int) file.length());

					InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

					try {
						FileCopyUtils.copy(inputStream, response.getOutputStream());
					} catch (IOException e) {
						System.out.println("Excep in Opening a Pdf File");
						e.printStackTrace();
					}
				}

			} catch (DocumentException ex) {

				System.out.println("Pdf Generation Error" + ex.getMessage());

				ex.printStackTrace();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	List<IssueMonthWiseList> subDeptWiselistForPdf = new ArrayList<IssueMonthWiseList>();
	List<GetSubDept> subDeptList = new ArrayList<>();
	int deptIdForPdf = 0;

	@RequestMapping(value = "/issueMonthSubDeptWieReportByDeptId/{deptId}", method = RequestMethod.GET)
	public ModelAndView issueMonthSubDeptWieReportByDeptId(@PathVariable int deptId, HttpServletRequest request,
			HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/issueReportMonthSubDeptWise");
		try {
			List<IssueMonthWiseList> subDeptWiselist = new ArrayList<IssueMonthWiseList>();

			MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
			map.add("typeId", typeId);
			map.add("isDev", isDev);
			map.add("deptId", deptId);
			System.out.println(map);
			IssueMonthWiseList[] issueMonthWiseList = rest.postForObject(
					Constants.url + "/issueMonthSubDeptWiseReportByDeptId", map, IssueMonthWiseList[].class);
			subDeptWiselist = new ArrayList<IssueMonthWiseList>(Arrays.asList(issueMonthWiseList));
			subDeptWiselistForPdf = subDeptWiselist;
			model.addObject("list", subDeptWiselist);

			GetSubDept[] getSubDept = rest.getForObject(Constants.url + "/getAllSubDept", GetSubDept[].class);
			subDeptList = new ArrayList<GetSubDept>(Arrays.asList(getSubDept));

			model.addObject("subDeptList", subDeptList);

			model.addObject("deptId", deptId);
			deptIdForPdf = deptId;
			// ------------------------ Export To
			// Excel--------------------------------------
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			List<ExportToExcel> exportToExcelList = new ArrayList<ExportToExcel>();

			ExportToExcel expoExcel = new ExportToExcel();
			List<String> rowData = new ArrayList<String>();
			;

			/*
			 * rowData.add("SR. No"); rowData.add("SUB DEPARMENT NAME");
			 * rowData.add("APR ISSUE QTY"); rowData.add("APR ISSUE VALUE");
			 * rowData.add("MAY ISSUE QTY"); rowData.add("MAY ISSUE VALUE");
			 * rowData.add("JUNE ISSUE QTY"); rowData.add("JUNE ISSUE VALUE");
			 * rowData.add("JULY ISSUE QTY"); rowData.add("JULY ISSUE VALUE");
			 * rowData.add("AUGUST ISSUE QTY"); rowData.add("AUGUST ISSUE VALUE");
			 * rowData.add("SEPTEMBR ISSUE QTY"); rowData.add("SEPTEMBR ISSUE VALUE");
			 * rowData.add("OCTOMBER ISSUE QTY"); rowData.add("OCTOMBER ISSUE VALUE");
			 * rowData.add("NOVEMBER ISSUE QTY"); rowData.add("NOVEMBER ISSUE VALUE");
			 * rowData.add("DECEMBER ISSUE QTY"); rowData.add("DECEMBER ISSUE VALUE");
			 * rowData.add("JANUARY ISSUE QTY"); rowData.add("JANUARY ISSUE VALUE");
			 * rowData.add("FEBRUARY ISSUE QTY"); rowData.add("FEBRUARY ISSUE VALUE");
			 * rowData.add("MARCH ISSUE QTY"); rowData.add("MARCH ISSUE VALUE");
			 */

			rowData.add("SR. No");
			rowData.add("SUB DEPARMENT NAME");
			rowData.add("APR ISSUE VALUE");
			rowData.add("MAY ISSUE VALUE");
			rowData.add("JUNE ISSUE VALUE");
			rowData.add("JULY ISSUE VALUE");
			rowData.add("AUGUST ISSUE VALUE");
			rowData.add("SEPTEMBR ISSUE VALUE");
			rowData.add("OCTOMBER ISSUE VALUE");
			rowData.add("NOVEMBER ISSUE VALUE");
			rowData.add("DECEMBER ISSUE VALUE");
			rowData.add("JANUARY ISSUE VALUE");
			rowData.add("FEBRUARY ISSUE VALUE");
			rowData.add("MARCH ISSUE VALUE");

			expoExcel.setRowData(rowData);

			exportToExcelList.add(expoExcel);
			int index = 0;
			for (int i = 0; i < subDeptList.size(); i++) {

				if (deptIdForPdf == subDeptList.get(i).getDeptId()) {
					expoExcel = new ExportToExcel();
					rowData = new ArrayList<String>();
					index++;
					rowData.add((index) + "");
					rowData.add(subDeptList.get(i).getSubDeptCode() + " " + subDeptList.get(i).getSubDeptDesc());
					for (int k = 0; k < subDeptWiselist.size(); k++) {
						List<MonthSubDeptWiseIssueReport> monthList = subDeptWiselist.get(k).getMonthSubDeptList();

						for (int j = 0; j < monthList.size(); j++) {
							if (monthList.get(j).getSubDeptId() == subDeptList.get(i).getSubDeptId()) {
								// rowData.add(""+monthList.get(j).getIssueQty());
								rowData.add("" + df.format(monthList.get(j).getIssueQtyValue()));
							}
						}

					}

					expoExcel.setRowData(rowData);
					exportToExcelList.add(expoExcel);
				}
			}

			HttpSession session = request.getSession();
			session.setAttribute("exportExcelList", exportToExcelList);
			session.setAttribute("excelName", "SubDeptMonthWiseConsumption(Issues)");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "/issueMonthSubDeptQtyWieReportByDeptId/{deptId}", method = RequestMethod.GET)
	public ModelAndView issueMonthSubDeptQtyWieReportByDeptId(@PathVariable int deptId, HttpServletRequest request,
			HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/issueQtyReportMonthSubDeptWise");
		try {
			List<IssueMonthWiseList> subDeptWiselist = new ArrayList<IssueMonthWiseList>();

			MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
			map.add("typeId", typeId);
			map.add("isDev", isDev);
			map.add("deptId", deptId);
			System.out.println(map);
			IssueMonthWiseList[] issueMonthWiseList = rest.postForObject(
					Constants.url + "/issueMonthSubDeptWiseReportByDeptId", map, IssueMonthWiseList[].class);
			subDeptWiselist = new ArrayList<IssueMonthWiseList>(Arrays.asList(issueMonthWiseList));
			subDeptWiselistForPdf = subDeptWiselist;
			model.addObject("list", subDeptWiselist);

			GetSubDept[] getSubDept = rest.getForObject(Constants.url + "/getAllSubDept", GetSubDept[].class);
			subDeptList = new ArrayList<GetSubDept>(Arrays.asList(getSubDept));

			model.addObject("subDeptList", subDeptList);

			model.addObject("deptId", deptId);
			deptIdForPdf = deptId;
			// ------------------------ Export To
			// Excel--------------------------------------
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			List<ExportToExcel> exportToExcelList = new ArrayList<ExportToExcel>();

			ExportToExcel expoExcel = new ExportToExcel();
			List<String> rowData = new ArrayList<String>();
			;

			rowData.add("SR. No");
			rowData.add("SUB DEPARMENT NAME");
			rowData.add("APR ISSUE QTY");
			rowData.add("MAY ISSUE QTY");
			rowData.add("JUNE ISSUE QTY");
			rowData.add("JULY ISSUE QTY");
			rowData.add("AUGUST ISSUE QTY");
			rowData.add("SEPTEMBR ISSUE QTY");
			rowData.add("OCTOMBER ISSUE QTY");
			rowData.add("NOVEMBER ISSUE QTY");
			rowData.add("DECEMBER ISSUE QTY");
			rowData.add("JANUARY ISSUE QTY");
			rowData.add("FEBRUARY ISSUE QTY");
			rowData.add("MARCH ISSUE QTY");

			expoExcel.setRowData(rowData);

			exportToExcelList.add(expoExcel);
			int index = 0;
			for (int i = 0; i < subDeptList.size(); i++) {

				if (deptIdForPdf == subDeptList.get(i).getDeptId()) {
					expoExcel = new ExportToExcel();
					rowData = new ArrayList<String>();
					index++;
					rowData.add((index) + "");
					rowData.add(subDeptList.get(i).getSubDeptCode() + " " + subDeptList.get(i).getSubDeptDesc());
					for (int k = 0; k < subDeptWiselist.size(); k++) {
						List<MonthSubDeptWiseIssueReport> monthList = subDeptWiselist.get(k).getMonthSubDeptList();

						for (int j = 0; j < monthList.size(); j++) {
							if (monthList.get(j).getSubDeptId() == subDeptList.get(i).getSubDeptId()) {
								// rowData.add(""+monthList.get(j).getIssueQty());
								rowData.add("" + df.format(monthList.get(j).getIssueQty()));
							}
						}

					}

					expoExcel.setRowData(rowData);
					exportToExcelList.add(expoExcel);
				}
			}

			HttpSession session = request.getSession();
			session.setAttribute("exportExcelList", exportToExcelList);
			session.setAttribute("excelName", "SubDeptMonthQtyWiseConsumption(Issues)");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "/listForIssueMonthGraphSubDeptWise", method = RequestMethod.GET)
	public @ResponseBody List<IssueMonthWiseList> listForIssueMonthGraphSubDeptWise(HttpServletRequest request,
			HttpServletResponse response) {

		return subDeptWiselistForPdf;
	}

	@RequestMapping(value = "/getSubDeptListForGraph", method = RequestMethod.GET)
	public @ResponseBody List<GetSubDept> getSubDeptListForGraph(HttpServletRequest request,
			HttpServletResponse response) {

		return subDeptList;
	}

	@RequestMapping(value = "/issueMonthSubDeptWiseReportPdf", method = RequestMethod.GET)
	public void issueMonthSubDeptWiseReportPdf(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException {
		BufferedOutputStream outStream = null;
		try {
			Document document = new Document(PageSize.A4.rotate(), 10f, 10f, 10f, 0f);
			DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
			String reportDate = DF.format(new Date());
			document.addHeader("Date: ", reportDate);
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			System.out.println("time in Gen Bill PDF ==" + dateFormat.format(cal.getTime()));
			String timeStamp = dateFormat.format(cal.getTime());
			String FILE_PATH = Constants.REPORT_SAVE;
			File file = new File(FILE_PATH);

			PdfWriter writer = null;
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			FileOutputStream out = new FileOutputStream(FILE_PATH);
			try {
				writer = PdfWriter.getInstance(document, out);
			} catch (DocumentException e) {

				e.printStackTrace();
			}

			PdfPTable table = new PdfPTable(14);
			try {
				System.out.println("Inside PDF Table try");
				table.setWidthPercentage(100);
				table.setWidths(new float[] { 0.4f, 1.7f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
						1.0f, 1.0f });
				Font headFont = new Font(FontFamily.TIMES_ROMAN, 8, Font.NORMAL, BaseColor.BLACK);
				Font headFont1 = new Font(FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
				Font f = new Font(FontFamily.TIMES_ROMAN, 11.0f, Font.UNDERLINE, BaseColor.BLUE);
				Font f1 = new Font(FontFamily.TIMES_ROMAN, 9.0f, Font.BOLD, BaseColor.DARK_GRAY);
				PdfPCell hcell = new PdfPCell();

				hcell.setPadding(4);
				hcell = new PdfPCell(new Phrase("SR.", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("Sub Department Name", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("APR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MAY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JUN", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JUL", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("AUG", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("SEP", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("OCT", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("NOV", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("DEC", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JAN", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("FEB", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MAR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				int index = 0;
				if (!subDeptWiselistForPdf.isEmpty()) {
					for (int k = 0; k < subDeptList.size(); k++) {
						if (deptIdForPdf == subDeptList.get(k).getDeptId()) {
							index++;

							PdfPCell cell;

							cell = new PdfPCell(new Phrase("" + index, headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase(
									subDeptList.get(k).getSubDeptCode() + " " + subDeptList.get(k).getSubDeptDesc(),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							for (int j = 0; j < subDeptWiselistForPdf.size(); j++) {

								List<MonthSubDeptWiseIssueReport> monthList = subDeptWiselistForPdf.get(j)
										.getMonthSubDeptList();

								for (int l = 0; l < monthList.size(); l++) {
									if (monthList.get(l).getSubDeptId() == subDeptList.get(k).getSubDeptId()) {
										/*
										 * cell = new PdfPCell(new Phrase(""+monthList.get(l).getIssueQty(), headFont));
										 * cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
										 * cell.setHorizontalAlignment(Element.ALIGN_RIGHT); cell.setPaddingRight(2);
										 * cell.setPadding(3); table.addCell(cell);
										 */
										cell = new PdfPCell(new Phrase(
												"" + df.format(monthList.get(l).getIssueQtyValue()), headFont));
										cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
										cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
										cell.setPaddingRight(2);
										cell.setPadding(3);
										table.addCell(cell);
									}
								}
							}

						}
					}
				}

				document.open();
				Paragraph company = new Paragraph("\n" + comp.getCompanyName(), f);
				company.setAlignment(Element.ALIGN_CENTER);
				document.add(company);

				Paragraph heading1 = new Paragraph(comp.getOfficeAdd(), f1);
				heading1.setAlignment(Element.ALIGN_CENTER);
				document.add(heading1);
				Paragraph ex2 = new Paragraph("\n");
				document.add(ex2);

				Paragraph headingDate = new Paragraph("Sub Dept Month Wise Consumption(Issues)", f1);
				headingDate.setAlignment(Element.ALIGN_CENTER);
				document.add(headingDate);

				Paragraph ex3 = new Paragraph("\n");
				document.add(ex3);
				table.setHeaderRows(1);
				document.add(table);

				int totalPages = writer.getPageNumber();

				System.out.println("Page no " + totalPages);

				document.close();
				// Atul Sir code to open a Pdf File
				if (file != null) {

					String mimeType = URLConnection.guessContentTypeFromName(file.getName());

					if (mimeType == null) {

						mimeType = "application/pdf";

					}

					response.setContentType(mimeType);

					response.addHeader("content-disposition", String.format("inline; filename=\"%s\"", file.getName()));

					response.setContentLength((int) file.length());

					InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

					try {
						FileCopyUtils.copy(inputStream, response.getOutputStream());
					} catch (IOException e) {
						System.out.println("Excep in Opening a Pdf File");
						e.printStackTrace();
					}
				}

			} catch (DocumentException ex) {

				System.out.println("Pdf Generation Error" + ex.getMessage());

				ex.printStackTrace();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@RequestMapping(value = "/issueMonthSubDeptQtyWiseReportPdf", method = RequestMethod.GET)
	public void issueMonthSubDeptQtyWiseReportPdf(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException {
		BufferedOutputStream outStream = null;
		try {
			Document document = new Document(PageSize.A4.rotate(), 10f, 10f, 10f, 0f);
			DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
			String reportDate = DF.format(new Date());
			document.addHeader("Date: ", reportDate);
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			System.out.println("time in Gen Bill PDF ==" + dateFormat.format(cal.getTime()));
			String timeStamp = dateFormat.format(cal.getTime());
			String FILE_PATH = Constants.REPORT_SAVE;
			File file = new File(FILE_PATH);

			PdfWriter writer = null;
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			FileOutputStream out = new FileOutputStream(FILE_PATH);
			try {
				writer = PdfWriter.getInstance(document, out);
			} catch (DocumentException e) {

				e.printStackTrace();
			}

			PdfPTable table = new PdfPTable(14);
			try {
				System.out.println("Inside PDF Table try");
				table.setWidthPercentage(100);
				table.setWidths(new float[] { 0.4f, 1.7f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
						1.0f, 1.0f });
				Font headFont = new Font(FontFamily.TIMES_ROMAN, 8, Font.NORMAL, BaseColor.BLACK);
				Font headFont1 = new Font(FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
				Font f = new Font(FontFamily.TIMES_ROMAN, 11.0f, Font.UNDERLINE, BaseColor.BLUE);
				Font f1 = new Font(FontFamily.TIMES_ROMAN, 9.0f, Font.BOLD, BaseColor.DARK_GRAY);
				PdfPCell hcell = new PdfPCell();

				hcell.setPadding(4);
				hcell = new PdfPCell(new Phrase("SR.", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("Sub Department Name", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("APR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MAY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JUN", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JUL", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("AUG", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("SEP", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("OCT", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("NOV", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("DEC", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JAN", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("FEB", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MAR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				int index = 0;
				if (!subDeptWiselistForPdf.isEmpty()) {
					for (int k = 0; k < subDeptList.size(); k++) {
						if (deptIdForPdf == subDeptList.get(k).getDeptId()) {
							index++;

							PdfPCell cell;

							cell = new PdfPCell(new Phrase("" + index, headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase(
									subDeptList.get(k).getSubDeptCode() + " " + subDeptList.get(k).getSubDeptDesc(),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							for (int j = 0; j < subDeptWiselistForPdf.size(); j++) {

								List<MonthSubDeptWiseIssueReport> monthList = subDeptWiselistForPdf.get(j)
										.getMonthSubDeptList();

								for (int l = 0; l < monthList.size(); l++) {
									if (monthList.get(l).getSubDeptId() == subDeptList.get(k).getSubDeptId()) {
										/*
										 * cell = new PdfPCell(new Phrase(""+monthList.get(l).getIssueQty(), headFont));
										 * cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
										 * cell.setHorizontalAlignment(Element.ALIGN_RIGHT); cell.setPaddingRight(2);
										 * cell.setPadding(3); table.addCell(cell);
										 */
										cell = new PdfPCell(
												new Phrase("" + df.format(monthList.get(l).getIssueQty()), headFont));
										cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
										cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
										cell.setPaddingRight(2);
										cell.setPadding(3);
										table.addCell(cell);
									}
								}
							}

						}
					}
				}

				document.open();
				Paragraph company = new Paragraph(comp.getCompanyName() + "\n", f);
				company.setAlignment(Element.ALIGN_CENTER);
				document.add(company);

				Paragraph heading1 = new Paragraph(comp.getOfficeAdd(), f1);
				heading1.setAlignment(Element.ALIGN_CENTER);
				document.add(heading1);
				Paragraph ex2 = new Paragraph("\n");
				document.add(ex2);

				Paragraph headingDate = new Paragraph("Sub Dept Month Wise Consumption(Issues)", f1);
				headingDate.setAlignment(Element.ALIGN_CENTER);
				document.add(headingDate);

				Paragraph ex3 = new Paragraph("\n");
				document.add(ex3);
				table.setHeaderRows(1);
				document.add(table);

				int totalPages = writer.getPageNumber();

				System.out.println("Page no " + totalPages);

				document.close();
				// Atul Sir code to open a Pdf File
				if (file != null) {

					String mimeType = URLConnection.guessContentTypeFromName(file.getName());

					if (mimeType == null) {

						mimeType = "application/pdf";

					}

					response.setContentType(mimeType);

					response.addHeader("content-disposition", String.format("inline; filename=\"%s\"", file.getName()));

					response.setContentLength((int) file.length());

					InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

					try {
						FileCopyUtils.copy(inputStream, response.getOutputStream());
					} catch (IOException e) {
						System.out.println("Excep in Opening a Pdf File");
						e.printStackTrace();
					}
				}

			} catch (DocumentException ex) {

				System.out.println("Pdf Generation Error" + ex.getMessage());

				ex.printStackTrace();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	List<IssueMonthWiseList> issueItemWiselistForPdf = new ArrayList<IssueMonthWiseList>();
	List<GetItem> itemListforPdf = new ArrayList<>();

	@RequestMapping(value = "/issueMonthItemWieReportBySubDeptId/{subDeptId}", method = RequestMethod.GET)
	public ModelAndView issueMonthItemWieReportBySubDeptId(@PathVariable int subDeptId, HttpServletRequest request,
			HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/issueReportMonthItemWise");
		try {
			List<IssueMonthWiseList> subDeptWiselist = new ArrayList<IssueMonthWiseList>();

			MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
			map.add("typeId", typeId);
			map.add("isDev", isDev);
			map.add("subDeptId", subDeptId);
			System.out.println(map);
			IssueMonthWiseList[] issueMonthWiseList = rest.postForObject(
					Constants.url + "/issueMonthItemWiseReportBySubDeptId", map, IssueMonthWiseList[].class);
			subDeptWiselist = new ArrayList<IssueMonthWiseList>(Arrays.asList(issueMonthWiseList));
			issueItemWiselistForPdf = subDeptWiselist;
			model.addObject("list", subDeptWiselist);

			GetItem[] item = rest.getForObject(Constants.url + "/getAllItems", GetItem[].class);
			List<GetItem> itemList = new ArrayList<GetItem>(Arrays.asList(item));
			model.addObject("itemList", itemList);
			itemListforPdf = itemList;
			// ------------------------ Export To
			// Excel--------------------------------------
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			List<ExportToExcel> exportToExcelList = new ArrayList<ExportToExcel>();

			ExportToExcel expoExcel = new ExportToExcel();
			List<String> rowData = new ArrayList<String>();

			/*
			 * rowData.add("SR. No"); rowData.add("ITEM NAME");
			 * rowData.add("APR ISSUE QTY"); rowData.add("APR ISSUE VALUE");
			 * rowData.add("MAY ISSUE QTY"); rowData.add("MAY ISSUE VALUE");
			 * rowData.add("JUNE ISSUE QTY"); rowData.add("JUNE ISSUE VALUE");
			 * rowData.add("JULY ISSUE QTY"); rowData.add("JULY ISSUE VALUE");
			 * rowData.add("AUGUST ISSUE QTY"); rowData.add("AUGUST ISSUE VALUE");
			 * rowData.add("SEPTEMBR ISSUE QTY"); rowData.add("SEPTEMBR ISSUE VALUE");
			 * rowData.add("OCTOMBER ISSUE QTY"); rowData.add("OCTOMBER ISSUE VALUE");
			 * rowData.add("NOVEMBER ISSUE QTY"); rowData.add("NOVEMBER ISSUE VALUE");
			 * rowData.add("DECEMBER ISSUE QTY"); rowData.add("DECEMBER ISSUE VALUE");
			 * rowData.add("JANUARY ISSUE QTY"); rowData.add("JANUARY ISSUE VALUE");
			 * rowData.add("FEBRUARY ISSUE QTY"); rowData.add("FEBRUARY ISSUE VALUE");
			 * rowData.add("MARCH ISSUE QTY"); rowData.add("MARCH ISSUE VALUE");
			 */

			rowData.add("SR. No");
			rowData.add("ITEM NAME");
			rowData.add("APR ISSUE VALUE");
			rowData.add("MAY ISSUE VALUE");
			rowData.add("JUNE ISSUE VALUE");
			rowData.add("JULY ISSUE VALUE");
			rowData.add("AUGUST ISSUE VALUE");
			rowData.add("SEPTEMBR ISSUE VALUE");
			rowData.add("OCTOMBER ISSUE VALUE");
			rowData.add("NOVEMBER ISSUE VALUE");
			rowData.add("DECEMBER ISSUE VALUE");
			rowData.add("JANUARY ISSUE VALUE");
			rowData.add("FEBRUARY ISSUE VALUE");
			rowData.add("MARCH ISSUE VALUE");

			expoExcel.setRowData(rowData);

			exportToExcelList.add(expoExcel);
			int index = 0;
			for (int i = 0; i < itemList.size(); i++) {

				expoExcel = new ExportToExcel();
				rowData = new ArrayList<String>();
				index++;
				rowData.add((index) + "");
				rowData.add(itemList.get(i).getItemCode() + " " + itemList.get(i).getItemDesc());
				for (int k = 0; k < subDeptWiselist.size(); k++) {
					List<MonthSubDeptWiseIssueReport> monthList = subDeptWiselist.get(k).getMonthSubDeptList();

					for (int j = 0; j < monthList.size(); j++) {
						if (monthList.get(j).getSubDeptId() == itemList.get(i).getItemId()) {
							// rowData.add(""+monthList.get(j).getIssueQty());
							rowData.add("" + df.format(monthList.get(j).getIssueQtyValue()));
						}
					}

				}

				expoExcel.setRowData(rowData);
				exportToExcelList.add(expoExcel);

			}

			HttpSession session = request.getSession();
			session.setAttribute("exportExcelList", exportToExcelList);
			session.setAttribute("excelName", "ItemIssueMonthWiseConsumption(Issues)");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "/issueMonthItemQtyWieReportBySubDeptId/{subDeptId}", method = RequestMethod.GET)
	public ModelAndView issueMonthItemQtyWieReportBySubDeptId(@PathVariable int subDeptId, HttpServletRequest request,
			HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/issueReportMonthItemQtyWise");
		try {
			List<IssueMonthWiseList> subDeptWiselist = new ArrayList<IssueMonthWiseList>();

			MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
			map.add("typeId", typeId);
			map.add("isDev", isDev);
			map.add("subDeptId", subDeptId);
			System.out.println(map);
			IssueMonthWiseList[] issueMonthWiseList = rest.postForObject(
					Constants.url + "/issueMonthItemWiseReportBySubDeptId", map, IssueMonthWiseList[].class);
			subDeptWiselist = new ArrayList<IssueMonthWiseList>(Arrays.asList(issueMonthWiseList));
			issueItemWiselistForPdf = subDeptWiselist;
			model.addObject("list", subDeptWiselist);

			GetItem[] item = rest.getForObject(Constants.url + "/getAllItems", GetItem[].class);
			List<GetItem> itemList = new ArrayList<GetItem>(Arrays.asList(item));
			model.addObject("itemList", itemList);
			itemListforPdf = itemList;
			// ------------------------ Export To
			// Excel--------------------------------------
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			List<ExportToExcel> exportToExcelList = new ArrayList<ExportToExcel>();

			ExportToExcel expoExcel = new ExportToExcel();
			List<String> rowData = new ArrayList<String>();

			rowData.add("SR. No");
			rowData.add("ITEM NAME");
			rowData.add("APR ISSUE QTY");
			rowData.add("MAY ISSUE QTY");
			rowData.add("JUNE ISSUE QTY");
			rowData.add("JULY ISSUE QTY");
			rowData.add("AUGUST ISSUE QTY");
			rowData.add("SEPTEMBR ISSUE QTY");
			rowData.add("OCTOMBER ISSUE QTY");
			rowData.add("NOVEMBER ISSUE QTY");
			rowData.add("DECEMBER ISSUE QTY");
			rowData.add("JANUARY ISSUE QTY");
			rowData.add("FEBRUARY ISSUE QTY");
			rowData.add("MARCH ISSUE QTY");

			expoExcel.setRowData(rowData);

			exportToExcelList.add(expoExcel);
			int index = 0;
			for (int i = 0; i < itemList.size(); i++) {

				expoExcel = new ExportToExcel();
				rowData = new ArrayList<String>();
				index++;
				rowData.add((index) + "");
				rowData.add(itemList.get(i).getItemCode() + " " + itemList.get(i).getItemDesc());
				for (int k = 0; k < subDeptWiselist.size(); k++) {
					List<MonthSubDeptWiseIssueReport> monthList = subDeptWiselist.get(k).getMonthSubDeptList();

					for (int j = 0; j < monthList.size(); j++) {
						if (monthList.get(j).getSubDeptId() == itemList.get(i).getItemId()) {
							// rowData.add(""+monthList.get(j).getIssueQty());
							rowData.add("" + df.format(monthList.get(j).getIssueQty()));
						}
					}

				}

				expoExcel.setRowData(rowData);
				exportToExcelList.add(expoExcel);

			}

			HttpSession session = request.getSession();
			session.setAttribute("exportExcelList", exportToExcelList);
			session.setAttribute("excelName", "ItemIssueMonthQtyWiseConsumption(Issues)");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "/issueMonthItemWiseReportPdf", method = RequestMethod.GET)
	public void issueMonthItemWiseReportPdf(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException {
		BufferedOutputStream outStream = null;
		try {
			Document document = new Document(PageSize.A4.rotate(), 10f, 10f, 10f, 0f);
			DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
			String reportDate = DF.format(new Date());
			document.addHeader("Date: ", reportDate);
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			System.out.println("time in Gen Bill PDF ==" + dateFormat.format(cal.getTime()));
			String timeStamp = dateFormat.format(cal.getTime());
			String FILE_PATH = Constants.REPORT_SAVE;
			File file = new File(FILE_PATH);

			PdfWriter writer = null;
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			FileOutputStream out = new FileOutputStream(FILE_PATH);
			try {
				writer = PdfWriter.getInstance(document, out);
			} catch (DocumentException e) {

				e.printStackTrace();
			}

			PdfPTable table = new PdfPTable(14);
			try {
				System.out.println("Inside PDF Table try");
				table.setWidthPercentage(100);
				table.setWidths(new float[] { 0.4f, 3.0f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f,
						0.6f, 0.6f });
				Font headFont = new Font(FontFamily.TIMES_ROMAN, 9, Font.NORMAL, BaseColor.BLACK);
				Font headFont1 = new Font(FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
				Font f = new Font(FontFamily.TIMES_ROMAN, 11.0f, Font.UNDERLINE, BaseColor.BLUE);
				Font f1 = new Font(FontFamily.TIMES_ROMAN, 9.0f, Font.BOLD, BaseColor.DARK_GRAY);
				PdfPCell hcell = new PdfPCell();

				hcell.setPadding(4);
				hcell = new PdfPCell(new Phrase("SR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("Item Name", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("APR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MAY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JUN", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JUL", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("AUG", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("SEP", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("OCT", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("NOV", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("DEC", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JAN", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("FEB", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MAR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				int index = 0;
				if (!issueItemWiselistForPdf.isEmpty()) {
					for (int k = 0; k < itemListforPdf.size(); k++) {

						index++;

						PdfPCell cell;

						cell = new PdfPCell(new Phrase("" + index, headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(new Phrase(
								itemListforPdf.get(k).getItemCode() + " " + itemListforPdf.get(k).getItemDesc(),
								headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						for (int j = 0; j < issueItemWiselistForPdf.size(); j++) {

							List<MonthSubDeptWiseIssueReport> monthList = issueItemWiselistForPdf.get(j)
									.getMonthSubDeptList();

							for (int l = 0; l < monthList.size(); l++) {
								if (monthList.get(l).getSubDeptId() == itemListforPdf.get(k).getItemId()) {
									/*
									 * cell = new PdfPCell(new Phrase(""+monthList.get(l).getIssueQty(), headFont));
									 * cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
									 * cell.setHorizontalAlignment(Element.ALIGN_RIGHT); cell.setPaddingRight(2);
									 * cell.setPadding(3); table.addCell(cell);
									 */
									cell = new PdfPCell(
											new Phrase("" + df.format(monthList.get(l).getIssueQtyValue()), headFont));
									cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
									cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
									cell.setPaddingRight(2);
									cell.setPadding(3);
									table.addCell(cell);
								}
							}
						}

					}

				}

				document.open();
				Paragraph company = new Paragraph("\n" + comp.getCompanyName(), f);
				company.setAlignment(Element.ALIGN_CENTER);
				document.add(company);

				Paragraph heading1 = new Paragraph(comp.getOfficeAdd(), f1);
				heading1.setAlignment(Element.ALIGN_CENTER);
				document.add(heading1);
				Paragraph ex2 = new Paragraph("\n");
				document.add(ex2);

				Paragraph headingDate = new Paragraph("Item Month Wise Consumption(Issues)", f1);
				headingDate.setAlignment(Element.ALIGN_CENTER);
				document.add(headingDate);

				Paragraph ex3 = new Paragraph("\n");
				document.add(ex3);
				table.setHeaderRows(1);
				document.add(table);

				int totalPages = writer.getPageNumber();

				System.out.println("Page no " + totalPages);

				document.close();
				// Atul Sir code to open a Pdf File
				if (file != null) {

					String mimeType = URLConnection.guessContentTypeFromName(file.getName());

					if (mimeType == null) {

						mimeType = "application/pdf";

					}

					response.setContentType(mimeType);

					response.addHeader("content-disposition", String.format("inline; filename=\"%s\"", file.getName()));

					response.setContentLength((int) file.length());

					InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

					try {
						FileCopyUtils.copy(inputStream, response.getOutputStream());
					} catch (IOException e) {
						System.out.println("Excep in Opening a Pdf File");
						e.printStackTrace();
					}
				}

			} catch (DocumentException ex) {

				System.out.println("Pdf Generation Error" + ex.getMessage());

				ex.printStackTrace();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@RequestMapping(value = "/issueMonthItemQtyWiseReportPdf", method = RequestMethod.GET)
	public void issueMonthItemQtyWiseReportPdf(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException {
		BufferedOutputStream outStream = null;
		try {
			Document document = new Document(PageSize.A4.rotate(), 10f, 10f, 10f, 0f);
			DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
			String reportDate = DF.format(new Date());
			document.addHeader("Date: ", reportDate);
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			System.out.println("time in Gen Bill PDF ==" + dateFormat.format(cal.getTime()));
			String timeStamp = dateFormat.format(cal.getTime());
			String FILE_PATH = Constants.REPORT_SAVE;
			File file = new File(FILE_PATH);

			PdfWriter writer = null;
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			FileOutputStream out = new FileOutputStream(FILE_PATH);
			try {
				writer = PdfWriter.getInstance(document, out);
			} catch (DocumentException e) {

				e.printStackTrace();
			}

			PdfPTable table = new PdfPTable(14);
			try {
				System.out.println("Inside PDF Table try");
				table.setWidthPercentage(100);
				table.setWidths(new float[] { 0.4f, 3.0f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f,
						0.6f, 0.6f });
				Font headFont = new Font(FontFamily.TIMES_ROMAN, 9, Font.NORMAL, BaseColor.BLACK);
				Font headFont1 = new Font(FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
				Font f = new Font(FontFamily.TIMES_ROMAN, 11.0f, Font.UNDERLINE, BaseColor.BLUE);
				Font f1 = new Font(FontFamily.TIMES_ROMAN, 9.0f, Font.BOLD, BaseColor.DARK_GRAY);
				PdfPCell hcell = new PdfPCell();

				hcell.setPadding(4);
				hcell = new PdfPCell(new Phrase("SR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("Item Name", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("APR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MAY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JUN", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JUL", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("AUG", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("SEP", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("OCT", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("NOV", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("DEC", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JAN", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("FEB", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MAR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				int index = 0;
				if (!issueItemWiselistForPdf.isEmpty()) {
					for (int k = 0; k < itemListforPdf.size(); k++) {

						index++;

						PdfPCell cell;

						cell = new PdfPCell(new Phrase("" + index, headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(new Phrase(
								itemListforPdf.get(k).getItemCode() + " " + itemListforPdf.get(k).getItemDesc(),
								headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						for (int j = 0; j < issueItemWiselistForPdf.size(); j++) {

							List<MonthSubDeptWiseIssueReport> monthList = issueItemWiselistForPdf.get(j)
									.getMonthSubDeptList();

							for (int l = 0; l < monthList.size(); l++) {
								if (monthList.get(l).getSubDeptId() == itemListforPdf.get(k).getItemId()) {
									/*
									 * cell = new PdfPCell(new Phrase(""+monthList.get(l).getIssueQty(), headFont));
									 * cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
									 * cell.setHorizontalAlignment(Element.ALIGN_RIGHT); cell.setPaddingRight(2);
									 * cell.setPadding(3); table.addCell(cell);
									 */
									cell = new PdfPCell(
											new Phrase("" + df.format(monthList.get(l).getIssueQty()), headFont));
									cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
									cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
									cell.setPaddingRight(2);
									cell.setPadding(3);
									table.addCell(cell);
								}
							}
						}

					}

				}

				document.open();
				Paragraph company = new Paragraph("\n" + comp.getCompanyName(), f);
				company.setAlignment(Element.ALIGN_CENTER);
				document.add(company);

				Paragraph heading1 = new Paragraph(comp.getOfficeAdd(), f1);
				heading1.setAlignment(Element.ALIGN_CENTER);
				document.add(heading1);
				Paragraph ex2 = new Paragraph("\n");
				document.add(ex2);

				Paragraph headingDate = new Paragraph("Item Month Wise Consumption(Issues)", f1);
				headingDate.setAlignment(Element.ALIGN_CENTER);
				document.add(headingDate);

				Paragraph ex3 = new Paragraph("\n");
				document.add(ex3);
				table.setHeaderRows(1);
				document.add(table);

				int totalPages = writer.getPageNumber();

				System.out.println("Page no " + totalPages);

				document.close();
				// Atul Sir code to open a Pdf File
				if (file != null) {

					String mimeType = URLConnection.guessContentTypeFromName(file.getName());

					if (mimeType == null) {

						mimeType = "application/pdf";

					}

					response.setContentType(mimeType);

					response.addHeader("content-disposition", String.format("inline; filename=\"%s\"", file.getName()));

					response.setContentLength((int) file.length());

					InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

					try {
						FileCopyUtils.copy(inputStream, response.getOutputStream());
					} catch (IOException e) {
						System.out.println("Excep in Opening a Pdf File");
						e.printStackTrace();
					}
				}

			} catch (DocumentException ex) {

				System.out.println("Pdf Generation Error" + ex.getMessage());

				ex.printStackTrace();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	List<MrnMonthWiseList> mrnCategoryMonthWiseListForPdf = new ArrayList<MrnMonthWiseList>();
	List<Category> categoryList = new ArrayList<Category>();

	@RequestMapping(value = "/mrnMonthCategoryWieReport", method = RequestMethod.GET)
	public ModelAndView mrnMonthCategoryWieReport(HttpServletRequest request, HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/mrnMonthCategoryWieReport");
		try {
			List<MrnMonthWiseList> list = new ArrayList<MrnMonthWiseList>();
			Type[] type = rest.getForObject(Constants.url + "/getAlltype", Type[].class);
			List<Type> typeList = new ArrayList<Type>(Arrays.asList(type));
			model.addObject("typeList", typeList);

			Dept[] Dept = rest.getForObject(Constants.url + "/getAllDeptByIsUsed", Dept[].class);
			List<Dept> deparmentList = new ArrayList<Dept>(Arrays.asList(Dept));
			model.addObject("deparmentList", deparmentList);

			if (request.getParameter("typeId") == null || request.getParameter("isDev") == null) {

				typeId = 0;
				isDev = -1;
				deptId = 0;
				subDeptId = 0;

				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				/*
				 * map.add("fromDate",DateConvertor.convertToYMD(fromDate));
				 * map.add("toDate",DateConvertor.convertToYMD(toDate));
				 */
				Date date = new Date();
				LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
				year = localDate.getYear();
				int month = localDate.getMonthValue();
				System.out.println("year" + year);
				System.out.println("month" + month);
				if (month < 4) {
					year = year - 1;

					System.out.println("year" + year);
				}

				map.add("typeId", typeId);
				map.add("year", year);
				map.add("deptId", deptId);
				map.add("subDeptId", subDeptId);
				if (isDev == -1) {
					map.add("isDev", "0,1");
				} else {
					map.add("isDev", isDev);
				}
				System.out.println(map);
				MrnMonthWiseList[] mrnMonthWiseList = rest.postForObject(Constants.url + "/mrnMonthCategoryWiseReport",
						map, MrnMonthWiseList[].class);
				list = new ArrayList<MrnMonthWiseList>(Arrays.asList(mrnMonthWiseList));

				model.addObject("list", list);
				model.addObject("typeId", typeId);
				model.addObject("isDevelompent", isDev);
				model.addObject("deptId", deptId);
				model.addObject("subDeptId", subDeptId);

				/*
				 * Category[] category = rest.getForObject(Constants.url +
				 * "/getAllCategoryByIsUsed", Category[].class); List<Category> categoryList =
				 * new ArrayList<Category>(Arrays.asList(category));
				 * model.addObject("categoryList", categoryList);
				 */
			} else {
				/*
				 * fromDate = request.getParameter("fromDate"); toDate =
				 * request.getParameter("toDate");
				 */
				typeId = Integer.parseInt(request.getParameter("typeId"));
				isDev = Integer.parseInt(request.getParameter("isDev"));
				deptId = Integer.parseInt(request.getParameter("deptId"));
				subDeptId = Integer.parseInt(request.getParameter("subDeptId"));

				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				/*
				 * map.add("fromDate",DateConvertor.convertToYMD(fromDate));
				 * map.add("toDate",DateConvertor.convertToYMD(toDate));
				 */
				map.add("typeId", typeId);

				map.add("deptId", deptId);
				map.add("subDeptId", subDeptId);
				map.add("year", year);
				if (isDev == -1) {
					map.add("isDev", "0,1");
				} else {
					map.add("isDev", isDev);
				}
				System.out.println(map);
				MrnMonthWiseList[] mrnMonthWiseList = rest.postForObject(Constants.url + "/mrnMonthCategoryWiseReport",
						map, MrnMonthWiseList[].class);
				list = new ArrayList<MrnMonthWiseList>(Arrays.asList(mrnMonthWiseList));

				model.addObject("list", list);
				model.addObject("typeId", typeId);
				model.addObject("isDevelompent", isDev);
				model.addObject("deptId", deptId);
				model.addObject("subDeptId", subDeptId);

			}

			Category[] category = rest.getForObject(Constants.url + "/getAllCategoryByIsUsed", Category[].class);
			categoryList = new ArrayList<Category>(Arrays.asList(category));
			model.addObject("categoryList", categoryList);

			mrnCategoryMonthWiseListForPdf = list;
			// ------------------------ Export To
			// Excel--------------------------------------
			List<ExportToExcel> exportToExcelList = new ArrayList<ExportToExcel>();

			ExportToExcel expoExcel = new ExportToExcel();
			List<String> rowData = new ArrayList<String>();
			;

			/*
			 * rowData.add("SR. No"); rowData.add("CAT NAME"); rowData.add("APR ISSUE QTY");
			 * rowData.add("APR ISSUE VALUE"); rowData.add("MAY ISSUE QTY");
			 * rowData.add("MAY ISSUE VALUE"); rowData.add("JUNE ISSUE QTY");
			 * rowData.add("JUNE ISSUE VALUE"); rowData.add("JULY ISSUE QTY");
			 * rowData.add("JULY ISSUE VALUE"); rowData.add("AUGUST ISSUE QTY");
			 * rowData.add("AUGUST ISSUE VALUE"); rowData.add("SEPTEMBR ISSUE QTY");
			 * rowData.add("SEPTEMBR ISSUE VALUE"); rowData.add("OCTOMBER ISSUE QTY");
			 * rowData.add("OCTOMBER ISSUE VALUE"); rowData.add("NOVEMBER ISSUE QTY");
			 * rowData.add("NOVEMBER ISSUE VALUE"); rowData.add("DECEMBER ISSUE QTY");
			 * rowData.add("DECEMBER ISSUE VALUE"); rowData.add("JANUARY ISSUE QTY");
			 * rowData.add("JANUARY ISSUE VALUE"); rowData.add("FEBRUARY ISSUE QTY");
			 * rowData.add("FEBRUARY ISSUE VALUE"); rowData.add("MARCH ISSUE QTY");
			 * rowData.add("MARCH ISSUE VALUE");
			 */

			rowData.add("SR. No");
			rowData.add("CAT NAME");
			rowData.add("APR ISSUE VALUE");
			rowData.add("MAY ISSUE VALUE");
			rowData.add("JUNE ISSUE VALUE");
			rowData.add("JULY ISSUE VALUE");
			rowData.add("AUGUST ISSUE VALUE");
			rowData.add("SEPTEMBR ISSUE VALUE");
			rowData.add("OCTOMBER ISSUE VALUE");
			rowData.add("NOVEMBER ISSUE VALUE");
			rowData.add("DECEMBER ISSUE VALUE");
			rowData.add("JANUARY ISSUE VALUE");
			rowData.add("FEBRUARY ISSUE VALUE");
			rowData.add("MARCH ISSUE VALUE");

			expoExcel.setRowData(rowData);

			exportToExcelList.add(expoExcel);
			int index = 0;
			for (int i = 0; i < categoryList.size(); i++) {

				expoExcel = new ExportToExcel();
				rowData = new ArrayList<String>();
				index++;
				rowData.add((index) + "");
				rowData.add(categoryList.get(i).getCatDesc());
				for (int k = 0; k < list.size(); k++) {
					List<MonthCategoryWiseMrnReport> monthList = list.get(k).getMonthList();

					for (int j = 0; j < monthList.size(); j++) {
						if (monthList.get(j).getCatId() == categoryList.get(i).getCatId()) {
							// rowData.add(""+monthList.get(j).getApproveQty());
							rowData.add("" + monthList.get(j).getApprovedQtyValue());
						}
					}

				}

				expoExcel.setRowData(rowData);
				exportToExcelList.add(expoExcel);

			}

			HttpSession session = request.getSession();
			session.setAttribute("exportExcelList", exportToExcelList);
			session.setAttribute("excelName", "MrnCategoryMonthWiseList");
			companyInfo = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "/mrnMonthCategoryWieReportQty", method = RequestMethod.GET)
	public ModelAndView mrnMonthCategoryWieReportQty(HttpServletRequest request, HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/mrnMonthCategoryWiseReportQty");
		try {
			List<MrnMonthWiseList> list = new ArrayList<MrnMonthWiseList>();
			Type[] type = rest.getForObject(Constants.url + "/getAlltype", Type[].class);
			List<Type> typeList = new ArrayList<Type>(Arrays.asList(type));
			model.addObject("typeList", typeList);

			Dept[] Dept = rest.getForObject(Constants.url + "/getAllDeptByIsUsed", Dept[].class);
			List<Dept> deparmentList = new ArrayList<Dept>(Arrays.asList(Dept));
			model.addObject("deparmentList", deparmentList);

			if (request.getParameter("typeId") == null || request.getParameter("isDev") == null) {

				typeId = 0;
				isDev = -1;
				deptId = 0;
				subDeptId = 0;

				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				/*
				 * map.add("fromDate",DateConvertor.convertToYMD(fromDate));
				 * map.add("toDate",DateConvertor.convertToYMD(toDate));
				 */
				map.add("typeId", typeId);

				map.add("deptId", deptId);
				map.add("subDeptId", subDeptId);
				if (isDev == -1) {
					map.add("isDev", "0,1");
				} else {
					map.add("isDev", isDev);
				}
				System.out.println(map);
				MrnMonthWiseList[] mrnMonthWiseList = rest.postForObject(Constants.url + "/mrnMonthCategoryWiseReport",
						map, MrnMonthWiseList[].class);
				list = new ArrayList<MrnMonthWiseList>(Arrays.asList(mrnMonthWiseList));

				model.addObject("list", list);
				model.addObject("typeId", typeId);
				model.addObject("isDevelompent", isDev);
				model.addObject("deptId", deptId);
				model.addObject("subDeptId", subDeptId);

				/*
				 * Category[] category = rest.getForObject(Constants.url +
				 * "/getAllCategoryByIsUsed", Category[].class); List<Category> categoryList =
				 * new ArrayList<Category>(Arrays.asList(category));
				 * model.addObject("categoryList", categoryList);
				 */
			} else {
				/*
				 * fromDate = request.getParameter("fromDate"); toDate =
				 * request.getParameter("toDate");
				 */
				typeId = Integer.parseInt(request.getParameter("typeId"));
				isDev = Integer.parseInt(request.getParameter("isDev"));
				deptId = Integer.parseInt(request.getParameter("deptId"));
				subDeptId = Integer.parseInt(request.getParameter("subDeptId"));

				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				/*
				 * map.add("fromDate",DateConvertor.convertToYMD(fromDate));
				 * map.add("toDate",DateConvertor.convertToYMD(toDate));
				 */
				map.add("typeId", typeId);

				map.add("deptId", deptId);
				map.add("subDeptId", subDeptId);
				if (isDev == -1) {
					map.add("isDev", "0,1");
				} else {
					map.add("isDev", isDev);
				}
				System.out.println(map);
				MrnMonthWiseList[] mrnMonthWiseList = rest.postForObject(Constants.url + "/mrnMonthCategoryWiseReport",
						map, MrnMonthWiseList[].class);
				list = new ArrayList<MrnMonthWiseList>(Arrays.asList(mrnMonthWiseList));

				model.addObject("list", list);
				model.addObject("typeId", typeId);
				model.addObject("isDevelompent", isDev);
				model.addObject("deptId", deptId);
				model.addObject("subDeptId", subDeptId);

			}

			Category[] category = rest.getForObject(Constants.url + "/getAllCategoryByIsUsed", Category[].class);
			categoryList = new ArrayList<Category>(Arrays.asList(category));
			model.addObject("categoryList", categoryList);

			mrnCategoryMonthWiseListForPdf = list;
			// ------------------------ Export To
			// Excel--------------------------------------

			DecimalFormat df = new DecimalFormat("####0.00");
			List<ExportToExcel> exportToExcelList = new ArrayList<ExportToExcel>();

			ExportToExcel expoExcel = new ExportToExcel();
			List<String> rowData = new ArrayList<String>();
			;

			/*
			 * rowData.add("SR. No"); rowData.add("CAT NAME"); rowData.add("APR ISSUE QTY");
			 * rowData.add("APR ISSUE VALUE"); rowData.add("MAY ISSUE QTY");
			 * rowData.add("MAY ISSUE VALUE"); rowData.add("JUNE ISSUE QTY");
			 * rowData.add("JUNE ISSUE VALUE"); rowData.add("JULY ISSUE QTY");
			 * rowData.add("JULY ISSUE VALUE"); rowData.add("AUGUST ISSUE QTY");
			 * rowData.add("AUGUST ISSUE VALUE"); rowData.add("SEPTEMBR ISSUE QTY");
			 * rowData.add("SEPTEMBR ISSUE VALUE"); rowData.add("OCTOMBER ISSUE QTY");
			 * rowData.add("OCTOMBER ISSUE VALUE"); rowData.add("NOVEMBER ISSUE QTY");
			 * rowData.add("NOVEMBER ISSUE VALUE"); rowData.add("DECEMBER ISSUE QTY");
			 * rowData.add("DECEMBER ISSUE VALUE"); rowData.add("JANUARY ISSUE QTY");
			 * rowData.add("JANUARY ISSUE VALUE"); rowData.add("FEBRUARY ISSUE QTY");
			 * rowData.add("FEBRUARY ISSUE VALUE"); rowData.add("MARCH ISSUE QTY");
			 * rowData.add("MARCH ISSUE VALUE");
			 */

			rowData.add("SR. No");
			rowData.add("CAT NAME");
			rowData.add("APR ISSUE QTY");
			rowData.add("MAY ISSUE QTY");
			rowData.add("JUNE ISSUE QTY");
			rowData.add("JULY ISSUE QTY");
			rowData.add("AUGUST ISSUE QTY");
			rowData.add("SEPTEMBR ISSUE QTY");
			rowData.add("OCTOMBER ISSUE QTY");
			rowData.add("NOVEMBER ISSUE QTY");
			rowData.add("DECEMBER ISSUE QTY");
			rowData.add("JANUARY ISSUE QTY");
			rowData.add("FEBRUARY ISSUE QTY");
			rowData.add("MARCH ISSUE QTY");

			expoExcel.setRowData(rowData);

			exportToExcelList.add(expoExcel);
			int index = 0;
			for (int i = 0; i < categoryList.size(); i++) {

				expoExcel = new ExportToExcel();
				rowData = new ArrayList<String>();
				index++;
				rowData.add((index) + "");
				rowData.add(categoryList.get(i).getCatDesc());
				for (int k = 0; k < list.size(); k++) {
					List<MonthCategoryWiseMrnReport> monthList = list.get(k).getMonthList();

					for (int j = 0; j < monthList.size(); j++) {
						if (monthList.get(j).getCatId() == categoryList.get(i).getCatId()) {
							// rowData.add(""+monthList.get(j).getApproveQty());
							rowData.add("" + df.format(monthList.get(j).getApproveQty()));
						}
					}

				}

				expoExcel.setRowData(rowData);
				exportToExcelList.add(expoExcel);

			}

			HttpSession session = request.getSession();
			session.setAttribute("exportExcelList", exportToExcelList);
			session.setAttribute("excelName", "MrnCategoryMonthWiseQtyList");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "/listForMrnGraphCategoryMonthWise", method = RequestMethod.GET)
	public @ResponseBody List<MrnMonthWiseList> listForMrnGraphCategoryMonthWise(HttpServletRequest request,
			HttpServletResponse response) {

		return mrnCategoryMonthWiseListForPdf;
	}

	@RequestMapping(value = "/getCatListForGraph", method = RequestMethod.GET)
	public @ResponseBody List<Category> getCatListForGraph(HttpServletRequest request, HttpServletResponse response) {

		return categoryList;
	}

	@RequestMapping(value = "/mrnCategoryMonthWiseReportPdf", method = RequestMethod.GET)
	public void mrnCategoryMonthWiseReportPdf(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException {
		BufferedOutputStream outStream = null;
		try {
			Document document = new Document(PageSize.A4.rotate(), 10f, 10f, 10f, 0f);
			DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
			String reportDate = DF.format(new Date());
			document.addHeader("Date: ", reportDate);
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			System.out.println("time in Gen Bill PDF ==" + dateFormat.format(cal.getTime()));
			String timeStamp = dateFormat.format(cal.getTime());
			String FILE_PATH = Constants.REPORT_SAVE;
			File file = new File(FILE_PATH);

			PdfWriter writer = null;
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			FileOutputStream out = new FileOutputStream(FILE_PATH);
			try {
				writer = PdfWriter.getInstance(document, out);
			} catch (DocumentException e) {

				e.printStackTrace();
			}

			PdfPTable table = new PdfPTable(14);
			try {
				System.out.println("Inside PDF Table try");
				table.setWidthPercentage(100);
				table.setWidths(new float[] { 0.4f, 1.7f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
						1.0f, 1.0f });
				Font headFont = new Font(FontFamily.TIMES_ROMAN, 9, Font.NORMAL, BaseColor.BLACK);
				Font headFont1 = new Font(FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
				Font f = new Font(FontFamily.TIMES_ROMAN, 11.0f, Font.UNDERLINE, BaseColor.BLUE);
				Font f1 = new Font(FontFamily.TIMES_ROMAN, 9.0f, Font.BOLD, BaseColor.DARK_GRAY);
				PdfPCell hcell = new PdfPCell();

				hcell.setPadding(4);
				hcell = new PdfPCell(new Phrase("SR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("Cat Name", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("APR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MAY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JUN", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JUL", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("AUG", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("SEP", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("OCT", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("NOV", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("DEC", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JAN", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("FEB", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MAR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				/*
				 * hcell.setPadding(4); hcell = new PdfPCell(new Phrase("SR.", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("Cat Name", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("APR", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); hcell.setColspan(2);
				 * table.addCell(hcell); hcell = new PdfPCell(new Phrase("MAY", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); hcell.setColspan(2);
				 * table.addCell(hcell); hcell = new PdfPCell(new Phrase("JUN", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); hcell.setColspan(2);
				 * table.addCell(hcell); hcell = new PdfPCell(new Phrase("JUL", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); hcell.setColspan(2);
				 * table.addCell(hcell); hcell = new PdfPCell(new Phrase("AUG", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); hcell.setColspan(2);
				 * table.addCell(hcell); hcell = new PdfPCell(new Phrase("SEP", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); hcell.setColspan(2);
				 * table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("OCT", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); hcell.setColspan(2);
				 * table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("NOV", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); hcell.setColspan(2);
				 * table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("DEC", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); hcell.setColspan(2);
				 * table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("JAN", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); hcell.setColspan(2);
				 * table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("FEB", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); hcell.setColspan(2);
				 * table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("MAR", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); hcell.setColspan(2);
				 * table.addCell(hcell);
				 * 
				 * 
				 * 
				 * hcell = new PdfPCell(); hcell.setPadding(4); hcell = new PdfPCell(new
				 * Phrase(" ", headFont1)); hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase(" ", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("QTY", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("VAL", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("QTY", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("VAL", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("QTY", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("VAL", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("QTY", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("VAL", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("QTY", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("VAL", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("QTY", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("VAL", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("QTY", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("VAL", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("QTY", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("VAL", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("QTY", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("VAL", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("QTY", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("VAL", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell); hcell = new
				 * PdfPCell(new Phrase("QTY", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("VAL", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("QTY", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 * 
				 * hcell = new PdfPCell(new Phrase("VAL", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 */

				int index = 0;
				if (!mrnCategoryMonthWiseListForPdf.isEmpty()) {
					for (int k = 0; k < categoryList.size(); k++) {

						index++;

						PdfPCell cell;

						cell = new PdfPCell(new Phrase("" + index, headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(new Phrase(categoryList.get(k).getCatDesc(), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						for (int j = 0; j < mrnCategoryMonthWiseListForPdf.size(); j++) {

							List<MonthCategoryWiseMrnReport> monthList = mrnCategoryMonthWiseListForPdf.get(j)
									.getMonthList();

							for (int l = 0; l < monthList.size(); l++) {
								if (monthList.get(l).getCatId() == categoryList.get(k).getCatId()) {
									/*
									 * cell = new PdfPCell(new Phrase(""+monthList.get(l).getApproveQty(),
									 * headFont)); cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
									 * cell.setHorizontalAlignment(Element.ALIGN_RIGHT); cell.setPaddingRight(2);
									 * cell.setPadding(3); table.addCell(cell);
									 */
									cell = new PdfPCell(new Phrase(
											"" + df.format(monthList.get(l).getApprovedQtyValue()), headFont));
									cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
									cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
									cell.setPaddingRight(2);
									cell.setPadding(3);
									table.addCell(cell);
								}
							}
						}

					}

				}

				document.open();
				Paragraph company = new Paragraph("\n" + comp.getCompanyName(), f);
				company.setAlignment(Element.ALIGN_CENTER);
				document.add(company);

				Paragraph heading1 = new Paragraph(comp.getFactoryAdd(), f1);
				heading1.setAlignment(Element.ALIGN_CENTER);
				document.add(heading1);
				Paragraph ex2 = new Paragraph("\n");
				document.add(ex2);

				Paragraph headingDate = new Paragraph("Mrn Category Month Wise Report ", f1);
				headingDate.setAlignment(Element.ALIGN_CENTER);
				document.add(headingDate);

				Paragraph ex3 = new Paragraph("\n");
				document.add(ex3);
				table.setHeaderRows(1);
				document.add(table);

				int totalPages = writer.getPageNumber();

				System.out.println("Page no " + totalPages);

				document.close();
				// Atul Sir code to open a Pdf File
				if (file != null) {

					String mimeType = URLConnection.guessContentTypeFromName(file.getName());

					if (mimeType == null) {

						mimeType = "application/pdf";

					}

					response.setContentType(mimeType);

					response.addHeader("content-disposition", String.format("inline; filename=\"%s\"", file.getName()));

					response.setContentLength((int) file.length());

					InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

					try {
						FileCopyUtils.copy(inputStream, response.getOutputStream());
					} catch (IOException e) {
						System.out.println("Excep in Opening a Pdf File");
						e.printStackTrace();
					}
				}

			} catch (DocumentException ex) {

				System.out.println("Pdf Generation Error" + ex.getMessage());

				ex.printStackTrace();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@RequestMapping(value = "/mrnCategoryMonthQtyWiseReportPdf", method = RequestMethod.GET)
	public void mrnCategoryMonthQtyWiseReportPdf(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException {
		BufferedOutputStream outStream = null;
		try {
			Document document = new Document(PageSize.A4.rotate(), 10f, 10f, 10f, 0f);
			DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
			String reportDate = DF.format(new Date());
			document.addHeader("Date: ", reportDate);
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			System.out.println("time in Gen Bill PDF ==" + dateFormat.format(cal.getTime()));
			String timeStamp = dateFormat.format(cal.getTime());
			String FILE_PATH = Constants.REPORT_SAVE;
			File file = new File(FILE_PATH);

			PdfWriter writer = null;
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			FileOutputStream out = new FileOutputStream(FILE_PATH);
			try {
				writer = PdfWriter.getInstance(document, out);
			} catch (DocumentException e) {

				e.printStackTrace();
			}

			PdfPTable table = new PdfPTable(14);
			try {
				System.out.println("Inside PDF Table try");
				table.setWidthPercentage(100);
				table.setWidths(new float[] { 0.4f, 1.7f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
						1.0f, 1.0f });
				Font headFont = new Font(FontFamily.TIMES_ROMAN, 9, Font.NORMAL, BaseColor.BLACK);
				Font headFont1 = new Font(FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
				Font f = new Font(FontFamily.TIMES_ROMAN, 11.0f, Font.UNDERLINE, BaseColor.BLUE);
				Font f1 = new Font(FontFamily.TIMES_ROMAN, 9.0f, Font.BOLD, BaseColor.DARK_GRAY);
				PdfPCell hcell = new PdfPCell();

				hcell.setPadding(4);
				hcell = new PdfPCell(new Phrase("SR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("Cat Name", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("APR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MAY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JUN", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JUL", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("AUG", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("SEP", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("OCT", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("NOV", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("DEC", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JAN", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("FEB", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MAR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				int index = 0;
				if (!mrnCategoryMonthWiseListForPdf.isEmpty()) {
					for (int k = 0; k < categoryList.size(); k++) {

						index++;

						PdfPCell cell;

						cell = new PdfPCell(new Phrase("" + index, headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(new Phrase(categoryList.get(k).getCatDesc(), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						for (int j = 0; j < mrnCategoryMonthWiseListForPdf.size(); j++) {

							List<MonthCategoryWiseMrnReport> monthList = mrnCategoryMonthWiseListForPdf.get(j)
									.getMonthList();

							for (int l = 0; l < monthList.size(); l++) {
								if (monthList.get(l).getCatId() == categoryList.get(k).getCatId()) {
									/*
									 * cell = new PdfPCell(new Phrase(""+monthList.get(l).getApproveQty(),
									 * headFont)); cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
									 * cell.setHorizontalAlignment(Element.ALIGN_RIGHT); cell.setPaddingRight(2);
									 * cell.setPadding(3); table.addCell(cell);
									 */
									cell = new PdfPCell(
											new Phrase("" + df.format(monthList.get(l).getApproveQty()), headFont));
									cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
									cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
									cell.setPaddingRight(2);
									cell.setPadding(3);
									table.addCell(cell);
								}
							}
						}

					}

				}

				document.open();
				Paragraph company = new Paragraph("\n" + comp.getCompanyName(), f);
				company.setAlignment(Element.ALIGN_CENTER);
				document.add(company);

				Paragraph heading1 = new Paragraph(comp.getOfficeAdd(), f1);
				heading1.setAlignment(Element.ALIGN_CENTER);
				document.add(heading1);
				Paragraph ex2 = new Paragraph("\n");
				document.add(ex2);

				Paragraph headingDate = new Paragraph("Mrn Category Month Wise Report ", f1);
				headingDate.setAlignment(Element.ALIGN_CENTER);
				document.add(headingDate);

				Paragraph ex3 = new Paragraph("\n");
				document.add(ex3);
				table.setHeaderRows(1);
				document.add(table);

				int totalPages = writer.getPageNumber();

				System.out.println("Page no " + totalPages);

				document.close();
				// Atul Sir code to open a Pdf File
				if (file != null) {

					String mimeType = URLConnection.guessContentTypeFromName(file.getName());

					if (mimeType == null) {

						mimeType = "application/pdf";

					}

					response.setContentType(mimeType);

					response.addHeader("content-disposition", String.format("inline; filename=\"%s\"", file.getName()));

					response.setContentLength((int) file.length());

					InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

					try {
						FileCopyUtils.copy(inputStream, response.getOutputStream());
					} catch (IOException e) {
						System.out.println("Excep in Opening a Pdf File");
						e.printStackTrace();
					}
				}

			} catch (DocumentException ex) {

				System.out.println("Pdf Generation Error" + ex.getMessage());

				ex.printStackTrace();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	List<MrnMonthWiseList> mrnItemMonthWiseListForPdf = new ArrayList<MrnMonthWiseList>();

	@RequestMapping(value = "/mrnMonthItemWiseReportBycatId/{catId}", method = RequestMethod.GET)
	public ModelAndView mrnMonthItemWiseReportBycatId(@PathVariable int catId, HttpServletRequest request,
			HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/mrnMonthItemWiseReport");
		try {
			List<MrnMonthWiseList> list = new ArrayList<MrnMonthWiseList>();

			MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
			map.add("typeId", typeId);
			map.add("deptId", deptId);
			map.add("subDeptId", subDeptId);
			map.add("catId", catId);
			if (isDev == -1) {
				map.add("isDev", "0,1");
			} else {
				map.add("isDev", isDev);
			}
			System.out.println(map);
			MrnMonthWiseList[] mrnMonthWiseList = rest.postForObject(Constants.url + "/mrnMonthItemWiseReport", map,
					MrnMonthWiseList[].class);
			list = new ArrayList<MrnMonthWiseList>(Arrays.asList(mrnMonthWiseList));

			mrnItemMonthWiseListForPdf = list;

			model.addObject("list", list);
			model.addObject("catId", catId);
			GetItem[] item = rest.getForObject(Constants.url + "/getAllItems", GetItem[].class);
			List<GetItem> itemList = new ArrayList<GetItem>(Arrays.asList(item));
			model.addObject("itemList", itemList);

			itemListforPdf = itemList;
			deptIdForPdf = catId;
			// ------------------------ Export To
			// Excel--------------------------------------

			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			List<ExportToExcel> exportToExcelList = new ArrayList<ExportToExcel>();

			ExportToExcel expoExcel = new ExportToExcel();
			List<String> rowData = new ArrayList<String>();

			/*
			 * rowData.add("SR. No"); rowData.add("Item NAME");
			 * rowData.add("APR ISSUE QTY"); rowData.add("APR ISSUE VALUE");
			 * rowData.add("MAY ISSUE QTY"); rowData.add("MAY ISSUE VALUE");
			 * rowData.add("JUNE ISSUE QTY"); rowData.add("JUNE ISSUE VALUE");
			 * rowData.add("JULY ISSUE QTY"); rowData.add("JULY ISSUE VALUE");
			 * rowData.add("AUGUST ISSUE QTY"); rowData.add("AUGUST ISSUE VALUE");
			 * rowData.add("SEPTEMBR ISSUE QTY"); rowData.add("SEPTEMBR ISSUE VALUE");
			 * rowData.add("OCTOMBER ISSUE QTY"); rowData.add("OCTOMBER ISSUE VALUE");
			 * rowData.add("NOVEMBER ISSUE QTY"); rowData.add("NOVEMBER ISSUE VALUE");
			 * rowData.add("DECEMBER ISSUE QTY"); rowData.add("DECEMBER ISSUE VALUE");
			 * rowData.add("JANUARY ISSUE QTY"); rowData.add("JANUARY ISSUE VALUE");
			 * rowData.add("FEBRUARY ISSUE QTY"); rowData.add("FEBRUARY ISSUE VALUE");
			 * rowData.add("MARCH ISSUE QTY"); rowData.add("MARCH ISSUE VALUE");
			 */

			rowData.add("SR. No");
			rowData.add("Item NAME");
			rowData.add("APR ISSUE VALUE");
			rowData.add("MAY ISSUE VALUE");
			rowData.add("JUNE ISSUE VALUE");
			rowData.add("JULY ISSUE VALUE");
			rowData.add("AUGUST ISSUE VALUE");
			rowData.add("SEPTEMBR ISSUE VALUE");
			rowData.add("OCTOMBER ISSUE VALUE");
			rowData.add("NOVEMBER ISSUE VALUE");
			rowData.add("DECEMBER ISSUE VALUE");
			rowData.add("JANUARY ISSUE VALUE");
			rowData.add("FEBRUARY ISSUE VALUE");
			rowData.add("MARCH ISSUE VALUE");

			expoExcel.setRowData(rowData);

			exportToExcelList.add(expoExcel);
			int index = 0;
			for (int i = 0; i < itemList.size(); i++) {

				if (itemList.get(i).getCatId() == catId) {
					expoExcel = new ExportToExcel();
					rowData = new ArrayList<String>();
					index++;
					rowData.add((index) + "");
					rowData.add(itemList.get(i).getItemCode() + " " + itemList.get(i).getItemDesc());
					for (int k = 0; k < list.size(); k++) {
						List<MonthItemWiseMrnReport> monthList = list.get(k).getItemWiseMonthList();

						for (int j = 0; j < monthList.size(); j++) {
							if (monthList.get(j).getItemId() == itemList.get(i).getItemId()) {
								// rowData.add(""+monthList.get(j).getApproveQty());
								rowData.add("" + df.format(monthList.get(j).getApprovedQtyValue()));
							}
						}

					}

					expoExcel.setRowData(rowData);
					exportToExcelList.add(expoExcel);
				}
			}

			HttpSession session = request.getSession();
			session.setAttribute("exportExcelList", exportToExcelList);
			session.setAttribute("excelName", "MrnItemMonthWiseList");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "/mrnMonthItemQtyWiseReportBycatId/{catId}", method = RequestMethod.GET)
	public ModelAndView mrnMonthItemQtyWiseReportBycatId(@PathVariable int catId, HttpServletRequest request,
			HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/mrnMonthItemQtyWiseReport");
		try {
			List<MrnMonthWiseList> list = new ArrayList<MrnMonthWiseList>();

			MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
			map.add("typeId", typeId);
			map.add("deptId", deptId);
			map.add("subDeptId", subDeptId);
			map.add("catId", catId);
			if (isDev == -1) {
				map.add("isDev", "0,1");
			} else {
				map.add("isDev", isDev);
			}
			System.out.println(map);
			MrnMonthWiseList[] mrnMonthWiseList = rest.postForObject(Constants.url + "/mrnMonthItemWiseReport", map,
					MrnMonthWiseList[].class);
			list = new ArrayList<MrnMonthWiseList>(Arrays.asList(mrnMonthWiseList));

			mrnItemMonthWiseListForPdf = list;

			model.addObject("list", list);
			model.addObject("catId", catId);
			GetItem[] item = rest.getForObject(Constants.url + "/getAllItems", GetItem[].class);
			List<GetItem> itemList = new ArrayList<GetItem>(Arrays.asList(item));
			model.addObject("itemList", itemList);

			itemListforPdf = itemList;
			deptIdForPdf = catId;
			// ------------------------ Export To
			// Excel--------------------------------------
			DecimalFormat df = new DecimalFormat("####0.00");
			List<ExportToExcel> exportToExcelList = new ArrayList<ExportToExcel>();

			ExportToExcel expoExcel = new ExportToExcel();
			List<String> rowData = new ArrayList<String>();

			rowData.add("SR. No");
			rowData.add("Item NAME");
			rowData.add("APR ISSUE QTY");
			rowData.add("MAY ISSUE QTY");
			rowData.add("JUNE ISSUE QTY");
			rowData.add("JULY ISSUE QTY");
			rowData.add("AUGUST ISSUE QTY");
			rowData.add("SEPTEMBR ISSUE QTY");
			rowData.add("OCTOMBER ISSUE QTY");
			rowData.add("NOVEMBER ISSUE QTY");
			rowData.add("DECEMBER ISSUE QTY");
			rowData.add("JANUARY ISSUE QTY");
			rowData.add("FEBRUARY ISSUE QTY");
			rowData.add("MARCH ISSUE QTY");

			expoExcel.setRowData(rowData);

			exportToExcelList.add(expoExcel);
			int index = 0;
			for (int i = 0; i < itemList.size(); i++) {

				if (itemList.get(i).getCatId() == catId) {
					expoExcel = new ExportToExcel();
					rowData = new ArrayList<String>();
					index++;
					rowData.add((index) + "");
					rowData.add(itemList.get(i).getItemCode() + " " + itemList.get(i).getItemDesc());
					for (int k = 0; k < list.size(); k++) {
						List<MonthItemWiseMrnReport> monthList = list.get(k).getItemWiseMonthList();

						for (int j = 0; j < monthList.size(); j++) {
							if (monthList.get(j).getItemId() == itemList.get(i).getItemId()) {
								// rowData.add(""+monthList.get(j).getApproveQty());
								rowData.add("" + df.format(monthList.get(j).getApproveQty()));
							}
						}

					}

					expoExcel.setRowData(rowData);
					exportToExcelList.add(expoExcel);
				}
			}

			HttpSession session = request.getSession();
			session.setAttribute("exportExcelList", exportToExcelList);
			session.setAttribute("excelName", "MrnItemMonthQtyWiseList");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "/mrnItemMonthWiseReportPdf", method = RequestMethod.GET)
	public void mrnItemMonthWiseReportPdf(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException {
		BufferedOutputStream outStream = null;
		try {
			Document document = new Document(PageSize.A4.rotate(), 10f, 10f, 10f, 0f);
			DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
			String reportDate = DF.format(new Date());
			document.addHeader("Date: ", reportDate);
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			System.out.println("time in Gen Bill PDF ==" + dateFormat.format(cal.getTime()));
			String timeStamp = dateFormat.format(cal.getTime());
			String FILE_PATH = Constants.REPORT_SAVE;
			File file = new File(FILE_PATH);

			PdfWriter writer = null;
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			FileOutputStream out = new FileOutputStream(FILE_PATH);
			try {
				writer = PdfWriter.getInstance(document, out);
			} catch (DocumentException e) {

				e.printStackTrace();
			}

			PdfPTable table = new PdfPTable(14);
			try {
				System.out.println("Inside PDF Table try");
				table.setWidthPercentage(100);
				table.setWidths(new float[] { 0.4f, 3.0f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f,
						0.8f, 0.8f });
				Font headFont = new Font(FontFamily.TIMES_ROMAN, 8, Font.NORMAL, BaseColor.BLACK);
				Font headFont1 = new Font(FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
				Font f = new Font(FontFamily.TIMES_ROMAN, 11.0f, Font.UNDERLINE, BaseColor.BLUE);
				Font f1 = new Font(FontFamily.TIMES_ROMAN, 9.0f, Font.BOLD, BaseColor.DARK_GRAY);
				PdfPCell hcell = new PdfPCell();

				hcell.setPadding(4);
				hcell = new PdfPCell(new Phrase("SR.", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("Item Name", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("APR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MAY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JUN", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JUL", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("AUG", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("SEP", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("OCT", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("NOV", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("DEC", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JAN", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("FEB", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MAR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				int index = 0;
				if (!mrnItemMonthWiseListForPdf.isEmpty()) {
					for (int k = 0; k < itemListforPdf.size(); k++) {
						if (deptIdForPdf == itemListforPdf.get(k).getCatId()) {
							index++;

							PdfPCell cell;

							cell = new PdfPCell(new Phrase("" + index, headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase(
									itemListforPdf.get(k).getItemCode() + " " + itemListforPdf.get(k).getItemDesc(),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							for (int j = 0; j < mrnItemMonthWiseListForPdf.size(); j++) {

								List<MonthItemWiseMrnReport> monthList = mrnItemMonthWiseListForPdf.get(j)
										.getItemWiseMonthList();

								for (int l = 0; l < monthList.size(); l++) {
									if (monthList.get(l).getItemId() == itemListforPdf.get(k).getItemId()) {
										/*
										 * cell = new PdfPCell(new Phrase(""+monthList.get(l).getApproveQty(),
										 * headFont)); cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
										 * cell.setHorizontalAlignment(Element.ALIGN_RIGHT); cell.setPaddingRight(2);
										 * cell.setPadding(3); table.addCell(cell);
										 */
										cell = new PdfPCell(new Phrase(
												"" + df.format(monthList.get(l).getApprovedQtyValue()), headFont));
										cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
										cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
										cell.setPaddingRight(2);
										cell.setPadding(3);
										table.addCell(cell);
									}
								}
							}

						}
					}
				}

				document.open();
				Paragraph company = new Paragraph("\n" + comp.getCompanyName(), f);
				company.setAlignment(Element.ALIGN_CENTER);
				document.add(company);

				Paragraph heading1 = new Paragraph(comp.getOfficeAdd(), f1);
				heading1.setAlignment(Element.ALIGN_CENTER);
				document.add(heading1);
				Paragraph ex2 = new Paragraph("\n");
				document.add(ex2);

				Paragraph headingDate = new Paragraph("Mrn Item Month Wise Report ", f1);
				headingDate.setAlignment(Element.ALIGN_CENTER);
				document.add(headingDate);

				Paragraph ex3 = new Paragraph("\n");
				document.add(ex3);
				table.setHeaderRows(1);
				document.add(table);

				int totalPages = writer.getPageNumber();

				System.out.println("Page no " + totalPages);

				document.close();
				// Atul Sir code to open a Pdf File
				if (file != null) {

					String mimeType = URLConnection.guessContentTypeFromName(file.getName());

					if (mimeType == null) {

						mimeType = "application/pdf";

					}

					response.setContentType(mimeType);

					response.addHeader("content-disposition", String.format("inline; filename=\"%s\"", file.getName()));

					response.setContentLength((int) file.length());

					InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

					try {
						FileCopyUtils.copy(inputStream, response.getOutputStream());
					} catch (IOException e) {
						System.out.println("Excep in Opening a Pdf File");
						e.printStackTrace();
					}
				}

			} catch (DocumentException ex) {

				System.out.println("Pdf Generation Error" + ex.getMessage());

				ex.printStackTrace();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@RequestMapping(value = "/mrnItemMonthWiseQtyReportPdf", method = RequestMethod.GET)
	public void mrnItemMonthWiseQtyReportPdf(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException {
		BufferedOutputStream outStream = null;
		try {
			Document document = new Document(PageSize.A4.rotate(), 10f, 10f, 10f, 0f);
			DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
			String reportDate = DF.format(new Date());
			document.addHeader("Date: ", reportDate);
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			System.out.println("time in Gen Bill PDF ==" + dateFormat.format(cal.getTime()));
			String timeStamp = dateFormat.format(cal.getTime());
			String FILE_PATH = Constants.REPORT_SAVE;
			File file = new File(FILE_PATH);

			PdfWriter writer = null;
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			FileOutputStream out = new FileOutputStream(FILE_PATH);
			try {
				writer = PdfWriter.getInstance(document, out);
			} catch (DocumentException e) {

				e.printStackTrace();
			}

			PdfPTable table = new PdfPTable(14);
			try {
				System.out.println("Inside PDF Table try");
				table.setWidthPercentage(100);
				table.setWidths(new float[] { 0.4f, 3.0f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f,
						0.8f, 0.8f });
				Font headFont = new Font(FontFamily.TIMES_ROMAN, 8, Font.NORMAL, BaseColor.BLACK);
				Font headFont1 = new Font(FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
				Font f = new Font(FontFamily.TIMES_ROMAN, 11.0f, Font.UNDERLINE, BaseColor.BLUE);
				Font f1 = new Font(FontFamily.TIMES_ROMAN, 9.0f, Font.BOLD, BaseColor.DARK_GRAY);
				PdfPCell hcell = new PdfPCell();

				hcell.setPadding(4);
				hcell = new PdfPCell(new Phrase("SR.", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("Item Name", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("APR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MAY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JUN", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JUL", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("AUG", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("SEP", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("OCT", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("NOV", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("DEC", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("JAN", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("FEB", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("MAR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				int index = 0;
				if (!mrnItemMonthWiseListForPdf.isEmpty()) {
					for (int k = 0; k < itemListforPdf.size(); k++) {
						if (deptIdForPdf == itemListforPdf.get(k).getCatId()) {
							index++;

							PdfPCell cell;

							cell = new PdfPCell(new Phrase("" + index, headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPadding(3);
							table.addCell(cell);

							cell = new PdfPCell(new Phrase(
									itemListforPdf.get(k).getItemCode() + " " + itemListforPdf.get(k).getItemDesc(),
									headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);

							for (int j = 0; j < mrnItemMonthWiseListForPdf.size(); j++) {

								List<MonthItemWiseMrnReport> monthList = mrnItemMonthWiseListForPdf.get(j)
										.getItemWiseMonthList();

								for (int l = 0; l < monthList.size(); l++) {
									if (monthList.get(l).getItemId() == itemListforPdf.get(k).getItemId()) {
										/*
										 * cell = new PdfPCell(new Phrase(""+monthList.get(l).getApproveQty(),
										 * headFont)); cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
										 * cell.setHorizontalAlignment(Element.ALIGN_RIGHT); cell.setPaddingRight(2);
										 * cell.setPadding(3); table.addCell(cell);
										 */
										cell = new PdfPCell(
												new Phrase("" + df.format(monthList.get(l).getApproveQty()), headFont));
										cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
										cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
										cell.setPaddingRight(2);
										cell.setPadding(3);
										table.addCell(cell);
									}
								}
							}

						}
					}
				}

				document.open();
				Paragraph company = new Paragraph("\n" + comp.getCompanyName(), f);
				company.setAlignment(Element.ALIGN_CENTER);
				document.add(company);

				Paragraph heading1 = new Paragraph(comp.getOfficeAdd(), f1);
				heading1.setAlignment(Element.ALIGN_CENTER);
				document.add(heading1);
				Paragraph ex2 = new Paragraph("\n");
				document.add(ex2);

				Paragraph headingDate = new Paragraph("Mrn Item Month Wise Report ", f1);
				headingDate.setAlignment(Element.ALIGN_CENTER);
				document.add(headingDate);

				Paragraph ex3 = new Paragraph("\n");
				document.add(ex3);
				table.setHeaderRows(1);
				document.add(table);

				int totalPages = writer.getPageNumber();

				System.out.println("Page no " + totalPages);

				document.close();
				// Atul Sir code to open a Pdf File
				if (file != null) {

					String mimeType = URLConnection.guessContentTypeFromName(file.getName());

					if (mimeType == null) {

						mimeType = "application/pdf";

					}

					response.setContentType(mimeType);

					response.addHeader("content-disposition", String.format("inline; filename=\"%s\"", file.getName()));

					response.setContentLength((int) file.length());

					InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

					try {
						FileCopyUtils.copy(inputStream, response.getOutputStream());
					} catch (IOException e) {
						System.out.println("Excep in Opening a Pdf File");
						e.printStackTrace();
					}
				}

			} catch (DocumentException ex) {

				System.out.println("Pdf Generation Error" + ex.getMessage());

				ex.printStackTrace();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	List<IndentStatusReport> indentStatusReportListForPdf = new ArrayList<IndentStatusReport>();

	@RequestMapping(value = "/indentStatusReport", method = RequestMethod.GET)
	public ModelAndView indentStatusReport(HttpServletRequest request, HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/indentStatusReport");
		try {

			if (request.getParameter("fromDate") == null || request.getParameter("toDate") == null) {

				SimpleDateFormat yy = new SimpleDateFormat("yyyy-MM-dd");
				SimpleDateFormat dd = new SimpleDateFormat("dd-MM-yyyy");
				Date date = new Date();
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);

				fromDate = "01" + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.YEAR);
				toDate = dd.format(date);

				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				map.add("fromDate", DateConvertor.convertToYMD(fromDate));
				map.add("toDate", yy.format(date));
				map.add("flag", 0);// for compare schDate or Indent Date
				IndentStatusReport[] indentStatusReport = rest.postForObject(Constants.url + "/indentStatusReport", map,
						IndentStatusReport[].class);
				List<IndentStatusReport> list = new ArrayList<IndentStatusReport>(Arrays.asList(indentStatusReport));

				model.addObject("indentStatusReport", list);
				model.addObject("fromDate", fromDate);
				model.addObject("toDate", dd.format(date));
				model.addObject("flag", 0);
				indentStatusReportListForPdf = list;
			} else {

				fromDate = request.getParameter("fromDate");
				toDate = request.getParameter("toDate");
				int flag = Integer.parseInt(request.getParameter("flag"));

				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				map.add("fromDate", DateConvertor.convertToYMD(fromDate));
				map.add("toDate", DateConvertor.convertToYMD(toDate));
				map.add("flag", flag);

				IndentStatusReport[] indentStatusReport = rest.postForObject(Constants.url + "/indentStatusReport", map,
						IndentStatusReport[].class);
				List<IndentStatusReport> list = new ArrayList<IndentStatusReport>(Arrays.asList(indentStatusReport));

				model.addObject("indentStatusReport", list);
				model.addObject("fromDate", fromDate);
				model.addObject("toDate", toDate);
				model.addObject("flag", flag);
				indentStatusReportListForPdf = list;
			}
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			List<ExportToExcel> exportToExcelList = new ArrayList<ExportToExcel>();

			ExportToExcel expoExcel = new ExportToExcel();
			List<String> rowData = new ArrayList<String>();

			rowData.add("FROM DATE-");
			rowData.add(fromDate);
			rowData.add("TO DATE");
			rowData.add(toDate);

			expoExcel.setRowData(rowData);
			exportToExcelList.add(expoExcel);

			expoExcel = new ExportToExcel();
			rowData = new ArrayList<String>();

			rowData.add("SR. No");
			rowData.add("INDENT NO");
			rowData.add("INDENT DATE");
			rowData.add("ITEM DESC");
			rowData.add("INDENT QTY");
			rowData.add("SCH DATE");
			rowData.add("EXPRESS DAYS");
			rowData.add("REMARK");

			expoExcel.setRowData(rowData);
			exportToExcelList.add(expoExcel);

			int k = 0;
			for (int i = 0; i < indentStatusReportListForPdf.size(); i++) {

				expoExcel = new ExportToExcel();
				rowData = new ArrayList<String>();
				k++;
				rowData.add((k) + "");
				rowData.add(indentStatusReportListForPdf.get(i).getIndMNo());
				rowData.add("" + indentStatusReportListForPdf.get(i).getIndMDate());
				rowData.add("" + indentStatusReportListForPdf.get(i).getItemCode());
				rowData.add("" + indentStatusReportListForPdf.get(i).getIndQty());
				rowData.add("" + indentStatusReportListForPdf.get(i).getIndItemSchddt());
				rowData.add("" + indentStatusReportListForPdf.get(i).getExcessDays());
				rowData.add("" + indentStatusReportListForPdf.get(i).getRemark());

				expoExcel.setRowData(rowData);
				exportToExcelList.add(expoExcel);

			}

			HttpSession session = request.getSession();
			session.setAttribute("exportExcelList", exportToExcelList);
			session.setAttribute("excelName", "indentSttatusList");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "/indentStatusReportPDF", method = RequestMethod.GET)
	public void indentStatusReportPDF(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException {
		BufferedOutputStream outStream = null;
		try {
			Document document = new Document(PageSize.A4);
			DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
			String reportDate = DF.format(new Date());
			document.addHeader("Date: ", reportDate);
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			System.out.println("time in Gen Bill PDF ==" + dateFormat.format(cal.getTime()));
			String timeStamp = dateFormat.format(cal.getTime());
			String FILE_PATH = Constants.REPORT_SAVE;
			File file = new File(FILE_PATH);

			PdfWriter writer = null;
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			FileOutputStream out = new FileOutputStream(FILE_PATH);
			try {
				writer = PdfWriter.getInstance(document, out);
			} catch (DocumentException e) {

				e.printStackTrace();
			}

			PdfPTable table = new PdfPTable(7);
			try {
				System.out.println("Inside PDF Table try");
				table.setWidthPercentage(100);
				table.setWidths(new float[] { 1.0f, 1.7f, 1.7f, 5.0f, 1.7f, 1.7f, 3.7f });
				Font headFont = new Font(FontFamily.TIMES_ROMAN, 8, Font.NORMAL, BaseColor.BLACK);
				Font headFont1 = new Font(FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
				Font f = new Font(FontFamily.TIMES_ROMAN, 11.0f, Font.UNDERLINE, BaseColor.BLUE);
				Font f1 = new Font(FontFamily.TIMES_ROMAN, 9.0f, Font.BOLD, BaseColor.GRAY);

				PdfPCell hcell = new PdfPCell();

				hcell.setPadding(4);
				hcell = new PdfPCell(new Phrase("SR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("INDENT NO", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("INDENT DATE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ITEM DESC", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("INDENT QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("SCH DATE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				/*
				 * hcell = new PdfPCell(new Phrase("EXPRESS DAYS", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 */

				hcell = new PdfPCell(new Phrase("REMARK", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				int index = 0;
				if (!indentStatusReportListForPdf.isEmpty()) {
					for (int k = 0; k < indentStatusReportListForPdf.size(); k++) {

						index++;

						PdfPCell cell;

						cell = new PdfPCell(new Phrase("" + index, headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(new Phrase(indentStatusReportListForPdf.get(k).getIndMNo(), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(
								new Phrase("" + indentStatusReportListForPdf.get(k).getIndMDate(), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(
								new Phrase("" + indentStatusReportListForPdf.get(k).getItemCode(), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(new Phrase("" + indentStatusReportListForPdf.get(k).getIndQty(), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(
								new Phrase("" + indentStatusReportListForPdf.get(k).getIndItemSchddt(), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						/*
						 * cell = new PdfPCell(new
						 * Phrase(""+indentStatusReportListForPdf.get(k).getExcessDays(), headFont));
						 * cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						 * cell.setHorizontalAlignment(Element.ALIGN_RIGHT); cell.setPaddingRight(2);
						 * cell.setPadding(3); table.addCell(cell);
						 */

						cell = new PdfPCell(new Phrase("" + indentStatusReportListForPdf.get(k).getRemark(), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

					}
				}

				document.open();
				Paragraph company = new Paragraph("\n" + comp.getCompanyName(), f);
				company.setAlignment(Element.ALIGN_CENTER);
				document.add(company);

				Paragraph heading1 = new Paragraph(comp.getOfficeAdd(), f1);
				heading1.setAlignment(Element.ALIGN_CENTER);
				document.add(heading1);

				Paragraph headingDate = new Paragraph(
						"Indent Status Report, From Date: " + fromDate + "  To Date: " + toDate + "", f1);
				headingDate.setAlignment(Element.ALIGN_CENTER);

				document.add(headingDate);

				Paragraph ex3 = new Paragraph("\n");
				document.add(ex3);
				table.setHeaderRows(1);
				document.add(table);

				int totalPages = writer.getPageNumber();

				System.out.println("Page no " + totalPages);

				document.close();
				// Atul Sir code to open a Pdf File
				if (file != null) {

					String mimeType = URLConnection.guessContentTypeFromName(file.getName());

					if (mimeType == null) {

						mimeType = "application/pdf";

					}

					response.setContentType(mimeType);

					response.addHeader("content-disposition", String.format("inline; filename=\"%s\"", file.getName()));

					response.setContentLength((int) file.length());

					InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

					try {
						FileCopyUtils.copy(inputStream, response.getOutputStream());
					} catch (IOException e) {
						System.out.println("Excep in Opening a Pdf File");
						e.printStackTrace();
					}
				}

			} catch (DocumentException ex) {

				System.out.println("Pdf Generation Error" + ex.getMessage());

				ex.printStackTrace();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	List<ItemExpectedReport> itemExpectedReportForPdf = new ArrayList<>();
	Company companyInfo = new Company();

	@RequestMapping(value = "/poItemExpectedReport", method = RequestMethod.GET)
	public ModelAndView poItemExpectedReport(HttpServletRequest request, HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/poItemExpectedReport");
		try {

			Category[] category = rest.getForObject(Constants.url + "/getAllCategoryByIsUsed", Category[].class);
			List<Category> categoryList = new ArrayList<Category>(Arrays.asList(category));
			model.addObject("categoryList", categoryList);

			if (request.getParameter("fromDate") == null || request.getParameter("toDate") == null) {

				SimpleDateFormat yy = new SimpleDateFormat("yyyy-MM-dd");
				SimpleDateFormat dd = new SimpleDateFormat("dd-MM-yyyy");
				Date date = new Date();
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);

				fromDate = "01" + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.YEAR);
				toDate = dd.format(date);

				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				map.add("fromDate", DateConvertor.convertToYMD(fromDate));
				map.add("toDate", yy.format(date));
				map.add("status", "0,1,2");
				map.add("catId", "0");
				ItemExpectedReport[] itemExpectedReport = rest.postForObject(
						Constants.url + "/getItemExpectedReportBetweenDate", map, ItemExpectedReport[].class);
				List<ItemExpectedReport> list = new ArrayList<ItemExpectedReport>(Arrays.asList(itemExpectedReport));
				itemExpectedReportForPdf = list;
				model.addObject("itemExpectedReport", list);
				model.addObject("fromDate", fromDate);
				model.addObject("toDate", dd.format(date));

			} else {

				fromDate = request.getParameter("fromDate");
				toDate = request.getParameter("toDate");
				int catId = Integer.parseInt(request.getParameter("catId"));
				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				map.add("fromDate", DateConvertor.convertToYMD(fromDate));
				map.add("toDate", DateConvertor.convertToYMD(toDate));
				map.add("status", "0,1,2");
				map.add("catId", catId);

				ItemExpectedReport[] itemExpectedReport = rest.postForObject(
						Constants.url + "/getItemExpectedReportBetweenDate", map, ItemExpectedReport[].class);
				List<ItemExpectedReport> list = new ArrayList<ItemExpectedReport>(Arrays.asList(itemExpectedReport));

				model.addObject("itemExpectedReport", list);
				model.addObject("fromDate", fromDate);
				model.addObject("toDate", toDate);
				model.addObject("catId", catId);
				itemExpectedReportForPdf = list;
			}
			DecimalFormat df = new DecimalFormat("####0.00");

			List<ExportToExcel> exportToExcelList = new ArrayList<ExportToExcel>();

			ExportToExcel expoExcel = new ExportToExcel();
			List<String> rowData = new ArrayList<String>();

			rowData.add("FROM DATE-");
			rowData.add(fromDate);
			rowData.add("TO DATE");
			rowData.add(toDate);

			expoExcel.setRowData(rowData);
			exportToExcelList.add(expoExcel);

			expoExcel = new ExportToExcel();
			rowData = new ArrayList<String>();

			rowData.add("SR. No");
			rowData.add("PO No");
			rowData.add("PO DATE");
			rowData.add("Vendor Name");
			rowData.add("Item Name");
			rowData.add("UOM");
			rowData.add("QTY");
			rowData.add("SCH DATE");

			expoExcel.setRowData(rowData);
			exportToExcelList.add(expoExcel);

			int k = 0;
			for (int i = 0; i < itemExpectedReportForPdf.size(); i++) {

				expoExcel = new ExportToExcel();
				rowData = new ArrayList<String>();
				k++;
				rowData.add((k) + "");
				rowData.add(itemExpectedReportForPdf.get(i).getPoNo());
				rowData.add("" + itemExpectedReportForPdf.get(i).getPoDate());
				rowData.add("" + itemExpectedReportForPdf.get(i).getVendorCode() + " - "
						+ itemExpectedReportForPdf.get(i).getVendorName());
				rowData.add("" + itemExpectedReportForPdf.get(i).getItemCode() + " - "
						+ itemExpectedReportForPdf.get(i).getItemDesc());
				rowData.add("" + itemExpectedReportForPdf.get(i).getUom());
				rowData.add("" + itemExpectedReportForPdf.get(i).getItemQty());
				rowData.add("" + itemExpectedReportForPdf.get(i).getSchDate());

				expoExcel.setRowData(rowData);
				exportToExcelList.add(expoExcel);

			}

			HttpSession session = request.getSession();
			session.setAttribute("exportExcelList", exportToExcelList);
			session.setAttribute("excelName", "poItemExpectedList");
			companyInfo = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "/poItemExpectedReportPdf", method = RequestMethod.GET)
	public void poItemExpectedReportPdf(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException {
		BufferedOutputStream outStream = null;
		try {
			Document document = new Document(PageSize.A4);
			DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
			String reportDate = DF.format(new Date());
			document.addHeader("Date: ", reportDate);
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			System.out.println("time in Gen Bill PDF ==" + dateFormat.format(cal.getTime()));
			String timeStamp = dateFormat.format(cal.getTime());
			String FILE_PATH = Constants.REPORT_SAVE;
			File file = new File(FILE_PATH);

			PdfWriter writer = null;
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			FileOutputStream out = new FileOutputStream(FILE_PATH);
			try {
				writer = PdfWriter.getInstance(document, out);
			} catch (DocumentException e) {

				e.printStackTrace();
			}

			PdfPTable table = new PdfPTable(8);
			try {
				System.out.println("Inside PDF Table try");
				table.setWidthPercentage(100);
				table.setWidths(new float[] { 0.6f, 1.5f, 1.5f, 3.0f, 3.0f, 1.0f, 1.0f, 1.5f });
				Font headFont = new Font(FontFamily.TIMES_ROMAN, 9, Font.NORMAL, BaseColor.BLACK);
				Font headFont1 = new Font(FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
				Font f = new Font(FontFamily.TIMES_ROMAN, 11.0f, Font.UNDERLINE, BaseColor.BLUE);
				Font f1 = new Font(FontFamily.TIMES_ROMAN, 9.0f, Font.BOLD, BaseColor.DARK_GRAY);

				PdfPCell hcell = new PdfPCell();

				hcell.setPadding(4);
				hcell = new PdfPCell(new Phrase("SR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("PO No", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("PO Date", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("Vendor Name", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("Item Name", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("UOM", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("Delivery Date", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				int index = 0;
				if (!itemExpectedReportForPdf.isEmpty()) {
					for (int k = 0; k < itemExpectedReportForPdf.size(); k++) {

						index++;
						PdfPCell cell;

						cell = new PdfPCell(new Phrase("" + index, headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(new Phrase(itemExpectedReportForPdf.get(k).getPoNo(), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(new Phrase("" + itemExpectedReportForPdf.get(k).getPoDate(), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(new Phrase("" + itemExpectedReportForPdf.get(k).getVendorCode() + " - "
								+ itemExpectedReportForPdf.get(k).getVendorName(), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(new Phrase("" + itemExpectedReportForPdf.get(k).getItemCode() + " - "
								+ itemExpectedReportForPdf.get(k).getItemDesc(), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(new Phrase("" + itemExpectedReportForPdf.get(k).getUom(), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(
								new Phrase("" + df.format(itemExpectedReportForPdf.get(k).getItemQty()), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(new Phrase("" + itemExpectedReportForPdf.get(k).getSchDate(), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

					}
				}

				document.open();
				Paragraph company = new Paragraph(companyInfo.getCompanyName() + "\n", f);
				company.setAlignment(Element.ALIGN_CENTER);
				document.add(company);

				Paragraph heading1 = new Paragraph(companyInfo.getOfficeAdd(), f1);
				heading1.setAlignment(Element.ALIGN_CENTER);
				document.add(heading1);
				Paragraph ex2 = new Paragraph("\n");
				document.add(ex2);

				Paragraph headingDate = new Paragraph(
						"Purchase Order Item Expected Report, From Date: " + fromDate + "  To Date: " + toDate + "",
						f1);
				headingDate.setAlignment(Element.ALIGN_CENTER);
				document.add(headingDate);

				Paragraph ex3 = new Paragraph("\n");
				document.add(ex3);
				table.setHeaderRows(1);
				document.add(table);

				int totalPages = writer.getPageNumber();

				System.out.println("Page no " + totalPages);

				document.close();
				// Atul Sir code to open a Pdf File
				if (file != null) {

					String mimeType = URLConnection.guessContentTypeFromName(file.getName());

					if (mimeType == null) {

						mimeType = "application/pdf";

					}

					response.setContentType(mimeType);

					response.addHeader("content-disposition", String.format("inline; filename=\"%s\"", file.getName()));

					response.setContentLength((int) file.length());

					InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

					try {
						FileCopyUtils.copy(inputStream, response.getOutputStream());
					} catch (IOException e) {
						System.out.println("Excep in Opening a Pdf File");
						e.printStackTrace();
					}
				}

			} catch (DocumentException ex) {

				System.out.println("Pdf Generation Error" + ex.getMessage());

				ex.printStackTrace();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	List<ShortItemReport> shortItemReportForPdf = new ArrayList<ShortItemReport>();

	@RequestMapping(value = "/materialShortRecievedReport", method = RequestMethod.GET)
	public ModelAndView materialShortRecievedReport(HttpServletRequest request, HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/materialShortRecievedReport");
		try {

			Category[] category = rest.getForObject(Constants.url + "/getAllCategoryByIsUsed", Category[].class);
			List<Category> categoryList = new ArrayList<Category>(Arrays.asList(category));
			model.addObject("categoryList", categoryList);

			if (request.getParameter("fromDate") == null || request.getParameter("toDate") == null) {

				SimpleDateFormat yy = new SimpleDateFormat("yyyy-MM-dd");
				SimpleDateFormat dd = new SimpleDateFormat("dd-MM-yyyy");
				Date date = new Date();
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);

				fromDate = "01" + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.YEAR);
				toDate = dd.format(date);

				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				map.add("fromDate", DateConvertor.convertToYMD(fromDate));
				map.add("toDate", yy.format(date));
				map.add("catId", "0");
				ShortItemReport[] shortItemReport = rest.postForObject(Constants.url + "/getShortItemReportBetweenDate",
						map, ShortItemReport[].class);
				List<ShortItemReport> list = new ArrayList<ShortItemReport>(Arrays.asList(shortItemReport));
				shortItemReportForPdf = list;
				model.addObject("shortItemReportForPdf", list);
				model.addObject("fromDate", fromDate);
				model.addObject("toDate", dd.format(date));

			} else {

				fromDate = request.getParameter("fromDate");
				toDate = request.getParameter("toDate");
				int catId = Integer.parseInt(request.getParameter("catId"));

				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				map.add("fromDate", DateConvertor.convertToYMD(fromDate));
				map.add("toDate", DateConvertor.convertToYMD(toDate));
				map.add("catId", catId);
				ShortItemReport[] shortItemReport = rest.postForObject(Constants.url + "/getShortItemReportBetweenDate",
						map, ShortItemReport[].class);
				List<ShortItemReport> list = new ArrayList<ShortItemReport>(Arrays.asList(shortItemReport));
				shortItemReportForPdf = list;
				model.addObject("shortItemReportForPdf", list);
				model.addObject("fromDate", fromDate);
				model.addObject("toDate", toDate);
				model.addObject("catId", catId);

				shortItemReportForPdf = list;
			}
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			List<ExportToExcel> exportToExcelList = new ArrayList<ExportToExcel>();

			ExportToExcel expoExcel = new ExportToExcel();
			List<String> rowData = new ArrayList<String>();

			rowData.add("FROM DATE-");
			rowData.add(fromDate);
			rowData.add("TO DATE");
			rowData.add(toDate);

			expoExcel.setRowData(rowData);
			exportToExcelList.add(expoExcel);

			expoExcel = new ExportToExcel();
			rowData = new ArrayList<String>();

			rowData.add("SR. No");
			rowData.add("PO No");
			rowData.add("PO DATE");
			rowData.add("Item Name");
			rowData.add("PO Qty");
			rowData.add("GRN Qty");
			rowData.add("Short");
			rowData.add("SCH DATE");

			expoExcel.setRowData(rowData);
			exportToExcelList.add(expoExcel);

			int k = 0;
			for (int i = 0; i < shortItemReportForPdf.size(); i++) {

				expoExcel = new ExportToExcel();
				rowData = new ArrayList<String>();
				k++;
				rowData.add((k) + "");
				rowData.add(shortItemReportForPdf.get(i).getPoNo());
				rowData.add("" + shortItemReportForPdf.get(i).getPoDate());
				rowData.add("" + shortItemReportForPdf.get(i).getItemCode() + " - "
						+ shortItemReportForPdf.get(i).getItemDesc());
				rowData.add("" + df.format(shortItemReportForPdf.get(i).getItemQty()));
				rowData.add("" + df.format(shortItemReportForPdf.get(i).getMrnQty()));
				rowData.add("" + df.format(shortItemReportForPdf.get(i).getPendingQty()));
				rowData.add("" + shortItemReportForPdf.get(i).getSchDate());

				expoExcel.setRowData(rowData);
				exportToExcelList.add(expoExcel);

			}

			HttpSession session = request.getSession();
			session.setAttribute("exportExcelList", exportToExcelList);
			session.setAttribute("excelName", "shortItemList");
			companyInfo = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "/materialShortRecievedReportPdf", method = RequestMethod.GET)
	public void materialShortRecievedReportPdf(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException {
		BufferedOutputStream outStream = null;
		try {
			Document document = new Document(PageSize.A4);
			DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
			String reportDate = DF.format(new Date());
			document.addHeader("Date: ", reportDate);
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			System.out.println("time in Gen Bill PDF ==" + dateFormat.format(cal.getTime()));
			String timeStamp = dateFormat.format(cal.getTime());
			String FILE_PATH = Constants.REPORT_SAVE;
			File file = new File(FILE_PATH);

			PdfWriter writer = null;
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			FileOutputStream out = new FileOutputStream(FILE_PATH);
			try {
				writer = PdfWriter.getInstance(document, out);
			} catch (DocumentException e) {

				e.printStackTrace();
			}

			PdfPTable table = new PdfPTable(8);
			try {
				System.out.println("Inside PDF Table try");
				table.setWidthPercentage(100);
				table.setWidths(new float[] { 0.6f, 1.5f, 1.5f, 3.0f, 1.5f, 1.5f, 1.5f, 1.5f });
				Font headFont = new Font(FontFamily.TIMES_ROMAN, 9, Font.NORMAL, BaseColor.BLACK);
				Font headFont1 = new Font(FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
				Font f = new Font(FontFamily.TIMES_ROMAN, 11.0f, Font.UNDERLINE, BaseColor.BLUE);
				Font f1 = new Font(FontFamily.TIMES_ROMAN, 9.0f, Font.BOLD, BaseColor.DARK_GRAY);

				PdfPCell hcell = new PdfPCell();

				hcell.setPadding(4);
				hcell = new PdfPCell(new Phrase("SR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("PO No", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("PO Date", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("Item Name", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("PO QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("GRN QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("SHORT", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("Delivery Date", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				int index = 0;
				if (!shortItemReportForPdf.isEmpty()) {
					for (int k = 0; k < shortItemReportForPdf.size(); k++) {

						index++;
						PdfPCell cell;

						cell = new PdfPCell(new Phrase("" + index, headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(new Phrase(shortItemReportForPdf.get(k).getPoNo(), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(new Phrase("" + shortItemReportForPdf.get(k).getPoDate(), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(new Phrase("" + shortItemReportForPdf.get(k).getItemCode() + " - "
								+ shortItemReportForPdf.get(k).getItemDesc(), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(
								new Phrase("" + df.format(shortItemReportForPdf.get(k).getItemQty()), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(new Phrase("" + shortItemReportForPdf.get(k).getMrnQty(), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(new Phrase("" + shortItemReportForPdf.get(k).getPendingQty(), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

						cell = new PdfPCell(new Phrase("" + shortItemReportForPdf.get(k).getSchDate(), headFont));
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setHorizontalAlignment(Element.ALIGN_LEFT);
						cell.setPaddingRight(2);
						cell.setPadding(3);
						table.addCell(cell);

					}
				}

				document.open();
				Paragraph company = new Paragraph(companyInfo.getCompanyName() + "\n", f);
				company.setAlignment(Element.ALIGN_CENTER);
				document.add(company);

				Paragraph heading1 = new Paragraph(companyInfo.getOfficeAdd(), f1);
				heading1.setAlignment(Element.ALIGN_CENTER);
				document.add(heading1);
				Paragraph ex2 = new Paragraph("\n");
				document.add(ex2);

				Paragraph headingDate = new Paragraph(
						"Material Short Report, From Date: " + fromDate + "  To Date: " + toDate + "", f1);
				headingDate.setAlignment(Element.ALIGN_CENTER);
				document.add(headingDate);

				Paragraph ex3 = new Paragraph("\n");
				document.add(ex3);
				table.setHeaderRows(1);
				document.add(table);

				int totalPages = writer.getPageNumber();

				System.out.println("Page no " + totalPages);

				document.close();
				// Atul Sir code to open a Pdf File
				if (file != null) {

					String mimeType = URLConnection.guessContentTypeFromName(file.getName());

					if (mimeType == null) {

						mimeType = "application/pdf";

					}

					response.setContentType(mimeType);

					response.addHeader("content-disposition", String.format("inline; filename=\"%s\"", file.getName()));

					response.setContentLength((int) file.length());

					InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

					try {
						FileCopyUtils.copy(inputStream, response.getOutputStream());
					} catch (IOException e) {
						System.out.println("Excep in Opening a Pdf File");
						e.printStackTrace();
					}

				}

			} catch (DocumentException ex) {

				System.out.println("Pdf Generation Error" + ex.getMessage());

				ex.printStackTrace();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	List<ItemWiseStockValuationReport> stockItemWiseListForPdf = new ArrayList<ItemWiseStockValuationReport>();
	ArrayList<Integer> uniqueCatIdList = new ArrayList<>();
	List<GetItemGroup> itemGroupList = new ArrayList<>();

	// Anmol------>4/12/2019---------->Item wise stock
	@RequestMapping(value = "/itemwiseStockValueationReport", method = RequestMethod.GET)
	public ModelAndView itemwiseStockValueationReport(HttpServletRequest request, HttpServletResponse response) {

		ModelAndView model = new ModelAndView("valuationReport/itemwiseStock");

		System.err.println("IN ------------- >---------  itemwiseStockValueationReport");

		try {
			List<ItemWiseStockValuationReport> itemWiseReport = new ArrayList<ItemWiseStockValuationReport>();

			/*
			 * if (request.getParameter("fromDate") == null ||
			 * request.getParameter("toDate") == null || request.getParameter("typeId") ==
			 * null) {
			 * 
			 * SimpleDateFormat yy = new SimpleDateFormat("yyyy-MM-dd"); SimpleDateFormat dd
			 * = new SimpleDateFormat("dd-MM-yyyy"); Date date = new Date(); Calendar
			 * calendar = Calendar.getInstance(); calendar.setTime(date);
			 * 
			 * fromDate = "01" + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" +
			 * calendar.get(Calendar.YEAR); toDate = dd.format(date); typeId = 0;
			 * MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
			 * map.add("fromDate", DateConvertor.convertToYMD(fromDate)); map.add("toDate",
			 * yy.format(date)); ItemWiseStockValuationReport[] stockValuationCategoryWise =
			 * rest.postForObject( Constants.url + "/itemwiseStockValueationReport", map,
			 * ItemWiseStockValuationReport[].class); itemWiseReport = new
			 * ArrayList<ItemWiseStockValuationReport>(
			 * Arrays.asList(stockValuationCategoryWise));
			 * 
			 * System.err.println("RESULT ------------- 1 >--------- "+itemWiseReport);
			 * 
			 * model.addObject("categoryWiseReport", itemWiseReport);
			 * //model.addObject("fromDate", fromDate); //model.addObject("toDate",
			 * dd.format(date)); stockItemWiseListForPdf = itemWiseReport; } else {
			 */

			fromDate = request.getParameter("fromDate");
			toDate = request.getParameter("toDate");

			SimpleDateFormat yy = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat dd = new SimpleDateFormat("dd-MM-yyyy");

			Date date = dd.parse(fromDate);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);

			String firstDate = "01" + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.YEAR);

			System.out.println(DateConvertor.convertToYMD(firstDate) + DateConvertor.convertToYMD(fromDate));

			if (DateConvertor.convertToYMD(firstDate).compareTo(DateConvertor.convertToYMD(fromDate)) < 0) {
				calendar.add(Calendar.DATE, -1);
				String previousDate = yy.format(new Date(calendar.getTimeInMillis()));
				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				map.add("fromDate", DateConvertor.convertToYMD(firstDate));
				map.add("toDate", previousDate);
				ItemWiseStockValuationReport[] stockValuationCategoryWise = rest.postForObject(
						Constants.url + "/itemwiseStockValueationReport", map, ItemWiseStockValuationReport[].class);
				List<ItemWiseStockValuationReport> diffDateStock = new ArrayList<ItemWiseStockValuationReport>(
						Arrays.asList(stockValuationCategoryWise));

				System.err.println("RESULT ------------- 2 >--------- " + diffDateStock);

				calendar.add(Calendar.DATE, 1);
				String addDay = yy.format(new Date(calendar.getTimeInMillis()));
				map = new LinkedMultiValueMap<>();
				map.add("fromDate", addDay);
				map.add("toDate", DateConvertor.convertToYMD(toDate));
				System.out.println(map);
				ItemWiseStockValuationReport[] stockValuationCategoryWise1 = rest.postForObject(
						Constants.url + "/itemwiseStockValueationReport", map, ItemWiseStockValuationReport[].class);
				itemWiseReport = new ArrayList<ItemWiseStockValuationReport>(
						Arrays.asList(stockValuationCategoryWise1));

				for (int i = 0; i < itemWiseReport.size(); i++) {
					for (int j = 0; j < diffDateStock.size(); j++) {
						if (itemWiseReport.get(i).getCatId() == diffDateStock.get(j).getCatId()) {
							itemWiseReport.get(i)
									.setOpeningStock(diffDateStock.get(j).getOpeningStock()
											+ diffDateStock.get(j).getApproveQty() - diffDateStock.get(j).getIssueQty()
											- diffDateStock.get(j).getDamageQty());
							itemWiseReport.get(i).setOpStockValue(diffDateStock.get(j).getOpStockValue()
									+ diffDateStock.get(j).getApprovedQtyValue()
									- diffDateStock.get(j).getIssueQtyValue() - diffDateStock.get(j).getDamageValue());

							break;
						}
					}
				}
			} else {
				MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				map.add("fromDate", DateConvertor.convertToYMD(fromDate));
				map.add("toDate", DateConvertor.convertToYMD(toDate));
				System.out.println(map);
				ItemWiseStockValuationReport[] stockValuationCategoryWise1 = rest.postForObject(
						Constants.url + "/itemwiseStockValueationReport", map, ItemWiseStockValuationReport[].class);
				itemWiseReport = new ArrayList<ItemWiseStockValuationReport>(
						Arrays.asList(stockValuationCategoryWise1));

				System.err.println("RESULT ------------- 3 >--------- " + itemWiseReport);
			}

			Set<Integer> setCatId = new HashSet<Integer>();
			Set<Integer> setSubCatId = new HashSet<Integer>();

			if (itemWiseReport != null) {
				for (int i = 0; i < itemWiseReport.size(); i++) {
					setCatId.add(itemWiseReport.get(i).getCatId());
					setSubCatId.add(itemWiseReport.get(i).getGrpId());
				}
			}

			uniqueCatIdList.addAll(setCatId);
			ArrayList<Integer> uniqueSubCatIdList = new ArrayList<>();
			uniqueSubCatIdList.addAll(setSubCatId);

			/*
			 * List<GetItemGroup> itemGroupList = rest.getForObject(Constants.url +
			 * "/getAllItemGroupByIsUsed", List.class);
			 */

			GetItemGroup[] itemGroupList1 = rest.getForObject(Constants.url + "/getAllItemGroupByIsUsed",
					GetItemGroup[].class);
			itemGroupList = new ArrayList<GetItemGroup>(Arrays.asList(itemGroupList1));

			model.addObject("itemGroupList", itemGroupList);

			System.err.println("ITEM GROUP LIST ---------------------- " + itemGroupList);

			model.addObject("categoryWiseReport", itemWiseReport);
			model.addObject("catIds", uniqueCatIdList);
			model.addObject("subCatIds", uniqueSubCatIdList);
			model.addObject("fromDate", fromDate);
			model.addObject("toDate", toDate);
			stockItemWiseListForPdf = itemWiseReport;

			// }

			// ----------------exel-------------------------
			DecimalFormat df = new DecimalFormat("####0.00");

			List<ExportToExcel> exportToExcelList = new ArrayList<ExportToExcel>();

			ExportToExcel expoExcel = new ExportToExcel();
			List<String> rowData = new ArrayList<String>();

			rowData.add("SR. No");
			rowData.add("ITEM");
			rowData.add("OPENING QTY");
			rowData.add("OPENING VALUE");
			rowData.add("OPENING LANDING VALUE");
			rowData.add("PURCHASE QTY");
			rowData.add("PURCHASE Value");
			rowData.add("PURCHASE LANDING VALUE");
			rowData.add("ISSUE QTY");
			rowData.add("ISSUE VALUE");
			rowData.add("ISSUE LANDING VALUE");
			rowData.add("DAMAGE QTY");
			rowData.add("DAMAGE VALUE");
			rowData.add("DAMAGE LANDING VALUE");
			rowData.add("BALANCE QTY");
			rowData.add("CLOSING VALUE");
			rowData.add("CLOSING LANDING VALUE");
			rowData.add(" ");//Issue percent

			float totalOpStock = 0;
			float totalOpValue = 0;
			float totalOpLandValue = 0;

			float totalAprvQty = 0;
			float totalAprvValue = 0;
			float totalAprvLandValue = 0;

			float totalIssueQty = 0;
			float totalIssueValue = 0;
			float totalIssueLandValue = 0;

			float totalDamageQty = 0;
			float totalDamageValue = 0;
			float totalDamageLandValue = 0;

			float totalClsQty = 0;
			float totalClsValue = 0;
			float totalClsLandValue = 0;

			expoExcel.setRowData(rowData);
			exportToExcelList.add(expoExcel);

			for (int c = 0; c < uniqueCatIdList.size(); c++) {

				float catOpStock = 0;
				float catOpValue = 0;
				float catOpLandValue = 0;

				float catAprvQty = 0;
				float catAprvValue = 0;
				float catAprvLandValue = 0;

				float catIssueQty = 0;
				float catIssueValue = 0;
				float catIssueLandValue = 0;

				float catDamageQty = 0;
				float catDamageValue = 0;
				float catDamageLandValue = 0;

				float catClsQty = 0;
				float catClsValue = 0;
				float catClsLandValue = 0;

				for (int g = 0; g < itemGroupList.size(); g++) {

					if (uniqueCatIdList.get(c) == itemGroupList.get(g).getCatId()) {

						float sumOpStock = 0;
						float sumOpValue = 0;
						float sumOpLandValue = 0;

						float sumAprvQty = 0;
						float sumAprvValue = 0;
						float sumAprvLandValue = 0;

						float sumIssueQty = 0;
						float sumIssueValue = 0;
						float sumIssueLandValue = 0;

						float sumDamageQty = 0;
						float sumDamageValue = 0;
						float sumDamageLandValue = 0;

						float sumClsQty = 0;
						float sumClsValue = 0;
						float sumClsLandValue = 0;

						expoExcel = new ExportToExcel();
						rowData = new ArrayList<String>();

						rowData.add("");
						rowData.add("" + itemGroupList.get(g).getGrpDesc());
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
					

						expoExcel.setRowData(rowData);
						exportToExcelList.add(expoExcel);

						for (int i = 0; i < itemWiseReport.size(); i++) {

							if (itemGroupList.get(g).getGrpId() == itemWiseReport.get(i).getGrpId()) {

								expoExcel = new ExportToExcel();
								rowData = new ArrayList<String>();

								rowData.add((i + 1) + "");
								rowData.add(itemWiseReport.get(i).getItemDesc());

								rowData.add("" + df.format(itemWiseReport.get(i).getOpeningStock()));
								
								totalOpStock = totalOpStock + itemWiseReport.get(i).getOpeningStock();
								sumOpStock = sumOpStock + itemWiseReport.get(i).getOpeningStock();

								rowData.add("" + df.format(itemWiseReport.get(i).getOpStockValue()));
								
								totalOpValue = totalOpValue + itemWiseReport.get(i).getOpStockValue();
								sumOpValue = sumOpValue + itemWiseReport.get(i).getOpStockValue();

								rowData.add("" + df.format(itemWiseReport.get(i).getOpLandingValue()));
								
								totalOpLandValue = totalOpLandValue + itemWiseReport.get(i).getOpLandingValue();
								sumOpLandValue = sumOpLandValue + itemWiseReport.get(i).getOpLandingValue();

								rowData.add("" + df.format(itemWiseReport.get(i).getApproveQty()));
								
								totalAprvQty = totalAprvQty + itemWiseReport.get(i).getApproveQty();
								sumAprvQty = sumAprvQty + itemWiseReport.get(i).getApproveQty();

								rowData.add("" + df.format(itemWiseReport.get(i).getApprovedQtyValue()));
								
								totalAprvValue = totalAprvValue + itemWiseReport.get(i).getApprovedQtyValue();
								sumAprvValue = sumAprvValue + itemWiseReport.get(i).getApprovedQtyValue();

								rowData.add("" + df.format(itemWiseReport.get(i).getApprovedLandingValue()));
								
								totalAprvLandValue = totalAprvLandValue
										+ itemWiseReport.get(i).getApprovedLandingValue();
								sumAprvLandValue = sumAprvLandValue
										+ itemWiseReport.get(i).getApprovedLandingValue();

								rowData.add("" + df.format(itemWiseReport.get(i).getIssueQty()));
								
								totalIssueQty = totalIssueQty + itemWiseReport.get(i).getIssueQty();
								sumIssueQty = sumIssueQty + itemWiseReport.get(i).getIssueQty();

								rowData.add("" + df.format(itemWiseReport.get(i).getIssueQtyValue()));
								
								totalIssueValue = totalIssueValue + itemWiseReport.get(i).getIssueQtyValue();
								sumIssueValue = sumIssueValue + itemWiseReport.get(i).getIssueQtyValue();

								rowData.add("" + df.format(itemWiseReport.get(i).getIssueLandingValue()));
								totalIssueLandValue = totalIssueLandValue
										+ itemWiseReport.get(i).getIssueLandingValue();
								sumIssueLandValue = sumIssueLandValue
										+ itemWiseReport.get(i).getIssueLandingValue();

								rowData.add("" + df.format(itemWiseReport.get(i).getDamageQty()));
								
								totalDamageQty = totalDamageQty + itemWiseReport.get(i).getDamageQty();
								sumDamageQty = sumDamageQty + itemWiseReport.get(i).getDamageQty();

								rowData.add("" + df.format(itemWiseReport.get(i).getDamageValue()));
								
								totalDamageValue = totalDamageValue + itemWiseReport.get(i).getDamageValue();
								sumDamageValue = sumDamageValue + itemWiseReport.get(i).getDamageValue();
							

								rowData.add("" + df.format(itemWiseReport.get(i).getDamageLandingValue()));
								
								totalDamageLandValue = totalDamageLandValue
										+ itemWiseReport.get(i).getDamageLandingValue();
								sumDamageLandValue = sumDamageLandValue
										+ itemWiseReport.get(i).getDamageLandingValue();

								float closingQty = itemWiseReport.get(i).getOpeningStock()
										+ itemWiseReport.get(i).getApproveQty() - itemWiseReport.get(i).getIssueQty()
										- itemWiseReport.get(i).getDamageQty();

								float closingValue = itemWiseReport.get(i).getOpStockValue()
										+ itemWiseReport.get(i).getApprovedQtyValue()
										- itemWiseReport.get(i).getIssueQtyValue()
										- itemWiseReport.get(i).getDamageValue();

								float closingLandingValue = itemWiseReport.get(i).getOpLandingValue()
										+ itemWiseReport.get(i).getApprovedLandingValue()
										- itemWiseReport.get(i).getIssueLandingValue()
										- itemWiseReport.get(i).getDamageLandingValue();

								rowData.add("" + df.format(closingQty));
								rowData.add("" + df.format(closingValue));
								rowData.add("" + df.format(closingLandingValue));

								totalClsLandValue = totalClsLandValue + closingLandingValue;
								totalClsValue = totalClsValue + closingValue;
								totalClsQty = totalClsQty + closingQty;

								sumClsQty = sumClsQty + closingQty;
								sumClsValue = sumClsValue + closingValue;
								sumClsLandValue = sumClsLandValue + closingLandingValue;

								expoExcel.setRowData(rowData);
								exportToExcelList.add(expoExcel);
							}
						}

						expoExcel = new ExportToExcel();
						rowData = new ArrayList<String>();

						rowData.add("");
						rowData.add("TOTAL");
						rowData.add(" " );
						rowData.add("" + sumOpValue);
						rowData.add("" + sumOpLandValue);
						rowData.add(" " );
						rowData.add("" + sumAprvValue);
						rowData.add("" + sumAprvLandValue);
						rowData.add(" " );
						rowData.add("" + sumIssueValue);
						rowData.add("" + sumIssueLandValue);
						rowData.add(" " );
						rowData.add("" + sumDamageValue);
						rowData.add("" + sumDamageLandValue);
						rowData.add(" " );
						rowData.add("" + sumClsValue);
						rowData.add("" + sumClsLandValue);

						float totOpe = 0, totPur = 0, totIssue = 0, totDamage = 0, totClosing = 0;
						for (int j = 0; j < itemWiseReport.size(); j++) {

							if (uniqueCatIdList.get(c) == itemWiseReport.get(j).getCatId()) {

								totOpe = totOpe + itemWiseReport.get(j).getOpLandingValue();
								totPur = totPur + itemWiseReport.get(j).getApprovedLandingValue();
								totIssue = totIssue + itemWiseReport.get(j).getIssueLandingValue();
								totDamage = totDamage + itemWiseReport.get(j).getDamageLandingValue();

								float closing = itemWiseReport.get(j).getOpLandingValue()
										+ itemWiseReport.get(j).getApprovedLandingValue()
										- itemWiseReport.get(j).getIssueLandingValue()
										- itemWiseReport.get(j).getDamageLandingValue();

								totClosing = totClosing + closing;

							}

						}

						float opePer=0,purPer=0,issuePer=0,damagePer=0,closePer=0;
						if(totOpe>0) {
							opePer = (sumOpLandValue * 100) / totOpe;
						}
						
						if(totPur>0) {
							purPer = (sumAprvLandValue * 100) / totPur;
						}
						
						if(totIssue>0) {
							issuePer = (sumIssueLandValue * 100) / totIssue;
						}
						
						if(totDamage>0) {
							damagePer = (sumDamageLandValue * 100) / totDamage;
						}
						
						if(totClosing>0) {
							closePer = (sumClsLandValue * 100) / totClosing;
						}
						 
//						float purPer = (sumAprvValue * 100) / totPur;
//						float issuePer = (sumIssueValue * 100) / totIssue;
//						float damagePer = (sumDamageValue * 100) / totDamage;
//						float closePer = (sumClsValue * 100) / totClosing;

						if (uniqueCatIdList.get(c) ==3) {
							rowData.add(" ");							
						}else {
							rowData.add("" + issuePer);
						}

						

						expoExcel.setRowData(rowData);
						exportToExcelList.add(expoExcel);

						catOpStock = catOpStock + sumOpStock;
						catOpValue = catOpValue + sumOpValue;
						catOpLandValue = catOpLandValue + sumOpLandValue;
						catAprvQty = catAprvQty + sumAprvQty;
						catAprvValue = catAprvValue + sumAprvValue;
						catAprvLandValue = catAprvLandValue + sumAprvLandValue;
						catIssueQty = catIssueQty + sumIssueQty;
						catIssueValue = catIssueValue + sumIssueValue;
						catIssueLandValue = catIssueLandValue + sumIssueLandValue;
						catDamageQty = catDamageQty + sumDamageQty;
						catDamageValue = catDamageValue + sumDamageValue;
						catDamageLandValue = catDamageLandValue + sumDamageLandValue;
						catClsQty = catClsQty + sumClsQty;
						catClsValue = catClsValue + sumClsValue;
						catClsLandValue = catClsLandValue + sumClsLandValue;

						//totalOpStock = totalOpStock + catOpStock;

						expoExcel = new ExportToExcel();
						rowData = new ArrayList<String>();

						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						

						expoExcel.setRowData(rowData);
						exportToExcelList.add(expoExcel);

					}

				}

				// ---------------------------------------
				expoExcel = new ExportToExcel();
				rowData = new ArrayList<String>();

				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
			

				expoExcel.setRowData(rowData);
				exportToExcelList.add(expoExcel);
				// ---------------------------------------

				// ---------------------------------------
				expoExcel = new ExportToExcel();
				rowData = new ArrayList<String>();

				rowData.add("");

				for (int k = 0; k < itemGroupList.size(); k++) {

					if (uniqueCatIdList.get(c) == itemGroupList.get(k).getCatId()) {
						rowData.add("" + itemGroupList.get(k).getCatDesc() + " Total");
						break;
					}
				}
				rowData.add(" " );
				rowData.add("" + catOpValue);
				rowData.add("" + catOpLandValue);
				rowData.add(" " );
				rowData.add("" + catAprvValue);
				rowData.add("" + catAprvLandValue);
				rowData.add(" " );
				rowData.add("" + catIssueValue);
				rowData.add("" + catIssueLandValue);
				rowData.add(" " );
				rowData.add("" + catDamageValue);
				rowData.add("" + catDamageLandValue);
				rowData.add(" " );
				rowData.add("" + catClsValue);
				rowData.add("" + catClsLandValue);

				rowData.add(" ");
				

				expoExcel.setRowData(rowData);
				exportToExcelList.add(expoExcel);
				// ---------------------------------------

				// ---------------------------------------
				expoExcel = new ExportToExcel();
				rowData = new ArrayList<String>();

				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				rowData.add("");
				

				expoExcel.setRowData(rowData);
				exportToExcelList.add(expoExcel);
				// ---------------------------------------

			}

			expoExcel = new ExportToExcel();
			rowData = new ArrayList<String>();

			rowData.add("-");
			rowData.add("Total");

			rowData.add(" ");
			rowData.add("" + df.format(totalOpValue));
			rowData.add("" + df.format(totalOpLandValue));
			rowData.add(" ");
			rowData.add("" + df.format(totalAprvValue));
			rowData.add("" + df.format(totalAprvLandValue));
			rowData.add(" ");
			rowData.add("" + df.format(totalIssueValue));
			rowData.add("" + df.format(totalIssueLandValue));
			rowData.add(" ");
			rowData.add("" + df.format(totalDamageValue));
			rowData.add("" + df.format(totalDamageLandValue));

			rowData.add(" ");
			rowData.add("" + df.format(totalClsValue));
			rowData.add("" + df.format(totalClsLandValue));

			rowData.add("");
			

			expoExcel.setRowData(rowData);
			exportToExcelList.add(expoExcel);

			HttpSession session = request.getSession();
			session.setAttribute("exportExcelList", exportToExcelList);
			session.setAttribute("excelName", "ItemWiseStockValue");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	// @RequestMapping(value = "/getStockBetweenDateItemWise", method =
	// RequestMethod.GET)
	// @ResponseBody
	// public List<GetCurrentStock> getStockBetweenDateItemWise(HttpServletRequest
	// request, HttpServletResponse response) {
	//
	// List<GetCurrentStock> getStockBetweenDate = new ArrayList<>();
	//
	// try {
	//
	// fromDate = request.getParameter("fromDate");
	// toDate = request.getParameter("toDate");
	// catId = Integer.parseInt(request.getParameter("catId"));
	//
	// SimpleDateFormat yy = new SimpleDateFormat("yyyy-MM-dd");
	// SimpleDateFormat dd = new SimpleDateFormat("dd-MM-yyyy");
	//
	// Date date = dd.parse(fromDate);
	// Calendar calendar = Calendar.getInstance();
	// calendar.setTime(date);
	//
	// String firstDate = "01" + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" +
	// calendar.get(Calendar.YEAR);
	//
	// System.out.println(DateConvertor.convertToYMD(firstDate) +
	// DateConvertor.convertToYMD(fromDate));
	//
	// if
	// (DateConvertor.convertToYMD(firstDate).compareTo(DateConvertor.convertToYMD(fromDate))
	// < 0) {
	// calendar.add(Calendar.DATE, -1);
	// String previousDate = yy.format(new Date(calendar.getTimeInMillis()));
	// MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
	// map.add("fromDate", DateConvertor.convertToYMD(firstDate));
	// map.add("toDate", previousDate);
	// map.add("catId", catId);
	// System.out.println(map);
	// GetCurrentStock[] getCurrentStock = rest.postForObject(Constants.url +
	// "/getStockBetweenDateWithCatId",
	// map, GetCurrentStock[].class);
	// List<GetCurrentStock> diffDateStock = new
	// ArrayList<>(Arrays.asList(getCurrentStock));
	//
	// calendar.add(Calendar.DATE, 1);
	// String addDay = yy.format(new Date(calendar.getTimeInMillis()));
	// map = new LinkedMultiValueMap<>();
	// map.add("fromDate", addDay);
	// map.add("toDate", DateConvertor.convertToYMD(toDate));
	// map.add("catId", catId);
	// System.out.println(map);
	// GetCurrentStock[] getCurrentStock1 = rest.postForObject(Constants.url +
	// "/getStockBetweenDateWithCatId",
	// map, GetCurrentStock[].class);
	// getStockBetweenDate = new ArrayList<>(Arrays.asList(getCurrentStock1));
	//
	// for (int i = 0; i < getStockBetweenDate.size(); i++) {
	// for (int j = 0; j < diffDateStock.size(); j++) {
	// if (getStockBetweenDate.get(i).getItemId() ==
	// diffDateStock.get(j).getItemId()) {
	// getStockBetweenDate.get(i).setOpeningStock(diffDateStock.get(j).getOpeningStock()
	// + diffDateStock.get(j).getApproveQty() - diffDateStock.get(j).getIssueQty()
	// + diffDateStock.get(j).getReturnIssueQty() -
	// diffDateStock.get(j).getDamageQty()
	// - diffDateStock.get(j).getGatepassQty()
	// + diffDateStock.get(j).getGatepassReturnQty());
	// getStockBetweenDate.get(i).setOpStockValue(diffDateStock.get(j).getOpStockValue()
	// + diffDateStock.get(j).getApprovedQtyValue()
	// - diffDateStock.get(j).getIssueQtyValue() -
	// diffDateStock.get(j).getDamagValue());
	//
	// break;
	// }
	// }
	// }
	// } else {
	// MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
	// map.add("fromDate", DateConvertor.convertToYMD(fromDate));
	// map.add("toDate", DateConvertor.convertToYMD(toDate));
	// map.add("catId", catId);
	// System.out.println(map);
	// GetCurrentStock[] getCurrentStock = rest.postForObject(Constants.url +
	// "/getStockBetweenDateWithCatId",
	// map, GetCurrentStock[].class);
	// getStockBetweenDate = new ArrayList<>(Arrays.asList(getCurrentStock));
	// }
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// return getStockBetweenDate;
	// }

	@RequestMapping(value = "/itemwiseStockValuetionReportPDF", method = RequestMethod.GET)
	public void itemwiseStockValuetionReportPDF(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException {
		BufferedOutputStream outStream = null;
		
		
		System.err.println("ITEM ------------ "+stockItemWiseListForPdf);
		
		try {
			Document document = new Document(PageSize.A4.rotate(), 10f, 10f, 10f, 0f);
			DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
			String reportDate = DF.format(new Date());
			document.addHeader("Date: ", reportDate);
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			System.out.println("time in Gen Bill PDF ==" + dateFormat.format(cal.getTime()));
			String timeStamp = dateFormat.format(cal.getTime());
			String FILE_PATH = Constants.REPORT_SAVE;
			File file = new File(FILE_PATH);

			PdfWriter writer = null;
			DecimalFormat df = new DecimalFormat("####0.00");
			Company comp = rest.getForObject(Constants.url + "getCompanyDetails", Company.class);

			FileOutputStream out = new FileOutputStream(FILE_PATH);
			try {
				writer = PdfWriter.getInstance(document, out);
			} catch (DocumentException e) {

				e.printStackTrace();
			}

			PdfPTable table = new PdfPTable(13);
			try {
				System.out.println("Inside PDF Table try");
				table.setWidthPercentage(100);
				table.setWidths(new float[] { 0.7f, 1.7f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
						1.0f });
				Font headFont = new Font(FontFamily.TIMES_ROMAN, 9, Font.NORMAL, BaseColor.BLACK);
				Font headFont1 = new Font(FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
				Font f = new Font(FontFamily.TIMES_ROMAN, 11.0f, Font.UNDERLINE, BaseColor.BLUE);
				Font f1 = new Font(FontFamily.TIMES_ROMAN, 9.0f, Font.BOLD, BaseColor.DARK_GRAY);

				Font totalFont = new Font(FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.BLACK);

				PdfPCell hcell = new PdfPCell();

				hcell.setPadding(4);
				hcell = new PdfPCell(new Phrase("SR", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ITEM", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("OPE QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				/*
				 * hcell = new PdfPCell(new Phrase("OPE VALUE", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 */

				hcell = new PdfPCell(new Phrase("OP LAND VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("PUR QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				/*
				 * hcell = new PdfPCell(new Phrase("PUR VALUE", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 */

				hcell = new PdfPCell(new Phrase("PUR LAND VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("ISSUE QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				/*
				 * hcell = new PdfPCell(new Phrase("ISSUE VALUE", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 */

				hcell = new PdfPCell(new Phrase("ISSUE LAND VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("DAMAGE QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				/*
				 * hcell = new PdfPCell(new Phrase("DAMAGE VALUE", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 */

				hcell = new PdfPCell(new Phrase("DAMAGE LAND VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				hcell = new PdfPCell(new Phrase("BALANCE QTY", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				/*
				 * hcell = new PdfPCell(new Phrase("CLOSING VALUE", headFont1));
				 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				 * hcell.setBackgroundColor(BaseColor.PINK); table.addCell(hcell);
				 */

				hcell = new PdfPCell(new Phrase("CLOSING LAND VALUE", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);


				hcell = new PdfPCell(new Phrase(" ", headFont1));
				hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
				hcell.setBackgroundColor(BaseColor.PINK);
				table.addCell(hcell);

				float totalOpStock = 0;
				float totalOpValue = 0;
				float totalOpLandValue = 0;

				float totalAprvQty = 0;
				float totalAprvValue = 0;
				float totalAprvLandValue = 0;

				float totalIssueQty = 0;
				float totalIssueValue = 0;
				float totalIssueLandValue = 0;

				float totalDamageQty = 0;
				float totalDamageValue = 0;
				float totalDamageLandValue = 0;

				float totalClsQty = 0;
				float totalClsValue = 0;
				float totalClsLandValue = 0;

				int index = 0;

				
				System.err.println(" CAT IDS -************----------- "+uniqueCatIdList);
				
				
				if (uniqueCatIdList != null) {

					for (int c = 0; c < uniqueCatIdList.size(); c++) {

						float catOpStock = 0;
						float catOpValue = 0;
						float catOpLandValue = 0;

						float catAprvQty = 0;
						float catAprvValue = 0;
						float catAprvLandValue = 0;

						float catIssueQty = 0;
						float catIssueValue = 0;
						float catIssueLandValue = 0;

						float catDamageQty = 0;
						float catDamageValue = 0;
						float catDamageLandValue = 0;

						float catClsQty = 0;
						float catClsValue = 0;
						float catClsLandValue = 0;

						if (itemGroupList != null) {

							for (int g = 0; g < itemGroupList.size(); g++) {

								if (uniqueCatIdList.get(c) == itemGroupList.get(g).getCatId()) {

									float sumOpStock = 0;
									float sumOpValue = 0;
									float sumOpLandValue = 0;

									float sumAprvQty = 0;
									float sumAprvValue = 0;
									float sumAprvLandValue = 0;

									float sumIssueQty = 0;
									float sumIssueValue = 0;
									float sumIssueLandValue = 0;

									float sumDamageQty = 0;
									float sumDamageValue = 0;
									float sumDamageLandValue = 0;

									float sumClsQty = 0;
									float sumClsValue = 0;
									float sumClsLandValue = 0;

									hcell.setPadding(4);
									hcell = new PdfPCell(new Phrase(" ", headFont1));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									hcell.setBackgroundColor(BaseColor.PINK);
									table.addCell(hcell);

									hcell = new PdfPCell(new Phrase("" + itemGroupList.get(g).getGrpDesc(), headFont1));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									hcell.setBackgroundColor(BaseColor.PINK);
									table.addCell(hcell);

									hcell = new PdfPCell(new Phrase(" ", headFont1));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									hcell.setBackgroundColor(BaseColor.PINK);
									table.addCell(hcell);

									hcell = new PdfPCell(new Phrase(" ", headFont1));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									hcell.setBackgroundColor(BaseColor.PINK);
									table.addCell(hcell);

									hcell = new PdfPCell(new Phrase(" ", headFont1));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									hcell.setBackgroundColor(BaseColor.PINK);
									table.addCell(hcell);

									hcell = new PdfPCell(new Phrase(" ", headFont1));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									hcell.setBackgroundColor(BaseColor.PINK);
									table.addCell(hcell);

									hcell = new PdfPCell(new Phrase(" ", headFont1));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									hcell.setBackgroundColor(BaseColor.PINK);
									table.addCell(hcell);

									hcell = new PdfPCell(new Phrase(" ", headFont1));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									hcell.setBackgroundColor(BaseColor.PINK);
									table.addCell(hcell);

									hcell = new PdfPCell(new Phrase(" ", headFont1));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									hcell.setBackgroundColor(BaseColor.PINK);
									table.addCell(hcell);

									hcell = new PdfPCell(new Phrase(" ", headFont1));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									hcell.setBackgroundColor(BaseColor.PINK);
									table.addCell(hcell);

									hcell = new PdfPCell(new Phrase(" ", headFont1));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									hcell.setBackgroundColor(BaseColor.PINK);
									table.addCell(hcell);

									hcell = new PdfPCell(new Phrase(" ", headFont1));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									hcell.setBackgroundColor(BaseColor.PINK);
									table.addCell(hcell);

									hcell = new PdfPCell(new Phrase(" ", headFont1));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									hcell.setBackgroundColor(BaseColor.PINK);
									table.addCell(hcell);

									

									if (!stockItemWiseListForPdf.isEmpty()) {
										for (int k = 0; k < stockItemWiseListForPdf.size(); k++) {

											if (itemGroupList.get(g).getGrpId() == stockItemWiseListForPdf.get(k)
													.getGrpId()) {

												index++;

												PdfPCell cell;

												cell = new PdfPCell(new Phrase("" + index, headFont));
												cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
												cell.setHorizontalAlignment(Element.ALIGN_LEFT);
												cell.setPadding(3);
												table.addCell(cell);

												cell = new PdfPCell(new Phrase(
														stockItemWiseListForPdf.get(k).getItemDesc(), headFont));
												cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
												cell.setHorizontalAlignment(Element.ALIGN_LEFT);
												cell.setPaddingRight(2);
												cell.setPadding(3);
												table.addCell(cell);

												cell = new PdfPCell(new Phrase(
														"" + 
																stockItemWiseListForPdf.get(k).getOpeningStock(),
														headFont));
												cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
												cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
												cell.setPaddingRight(2);
												cell.setPadding(3);
												table.addCell(cell);

												totalOpStock = totalOpStock
														+ stockItemWiseListForPdf.get(k).getOpeningStock();

												sumOpStock = sumOpStock
														+ stockItemWiseListForPdf.get(k).getOpeningStock();

												/*
												 * cell = new PdfPCell(new Phrase( "" + df.format(
												 * stockItemWiseListForPdf.get(k).getOpStockValue()), headFont));
												 * cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
												 * cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
												 * cell.setPaddingRight(2); cell.setPadding(3); table.addCell(cell);
												 */

												totalOpValue = totalOpValue
														+ stockItemWiseListForPdf.get(k).getOpStockValue();

												sumOpValue = sumOpValue
														+ stockItemWiseListForPdf.get(k).getOpStockValue();

												cell = new PdfPCell(new Phrase(
														"" + df.format(
																stockItemWiseListForPdf.get(k).getOpLandingValue()),
														headFont));
												cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
												cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
												cell.setPaddingRight(2);
												cell.setPadding(3);
												table.addCell(cell);

												totalOpLandValue = totalOpLandValue
														+ stockItemWiseListForPdf.get(k).getOpLandingValue();

												sumOpLandValue = sumOpLandValue
														+ stockItemWiseListForPdf.get(k).getOpLandingValue();

												cell = new PdfPCell(new Phrase(
														"" + stockItemWiseListForPdf.get(k).getApproveQty(),
														headFont));
												cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
												cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
												cell.setPaddingRight(2);
												cell.setPadding(3);
												table.addCell(cell);

												totalAprvQty = totalAprvQty
														+ stockItemWiseListForPdf.get(k).getApproveQty();

												sumAprvQty = sumAprvQty
														+ stockItemWiseListForPdf.get(k).getApproveQty();

												/*
												 * cell = new PdfPCell(new Phrase( "" + df.format(
												 * stockItemWiseListForPdf.get(k).getApprovedQtyValue()), headFont));
												 * cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
												 * cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
												 * cell.setPaddingRight(2); cell.setPadding(3); table.addCell(cell);
												 */

												totalAprvValue = totalAprvValue
														+ stockItemWiseListForPdf.get(k).getApprovedQtyValue();

												sumAprvValue = sumAprvValue
														+ stockItemWiseListForPdf.get(k).getApprovedQtyValue();

												cell = new PdfPCell(new Phrase("" + df.format(
														stockItemWiseListForPdf.get(k).getApprovedLandingValue()),
														headFont));
												cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
												cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
												cell.setPaddingRight(2);
												cell.setPadding(3);
												table.addCell(cell);

												totalAprvLandValue = totalAprvLandValue
														+ stockItemWiseListForPdf.get(k).getApprovedLandingValue();

												sumAprvLandValue = sumAprvLandValue
														+ stockItemWiseListForPdf.get(k).getApprovedLandingValue();
												
												
												

												cell = new PdfPCell(new Phrase(
														"" + stockItemWiseListForPdf.get(k).getIssueQty(),
														headFont));
												cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
												cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
												cell.setPaddingRight(2);
												cell.setPadding(3);
												table.addCell(cell);

												totalIssueQty = totalIssueQty
														+ stockItemWiseListForPdf.get(k).getIssueQty();
												sumIssueQty = sumIssueQty
														+ stockItemWiseListForPdf.get(k).getIssueQty();

												/*
												 * cell = new PdfPCell(new Phrase( "" + df.format(
												 * stockItemWiseListForPdf.get(k).getIssueQtyValue()), headFont));
												 * cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
												 * cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
												 * cell.setPaddingRight(2); cell.setPadding(3); table.addCell(cell);
												 */
												totalIssueValue = totalIssueValue
														+ stockItemWiseListForPdf.get(k).getIssueQtyValue();

												sumIssueValue = sumIssueValue
														+ stockItemWiseListForPdf.get(k).getIssueQtyValue();

												cell = new PdfPCell(new Phrase(
														"" + df.format(
																stockItemWiseListForPdf.get(k).getIssueLandingValue()),
														headFont));
												cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
												cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
												cell.setPaddingRight(2);
												cell.setPadding(3);
												table.addCell(cell);

												totalIssueLandValue = totalIssueLandValue
														+ stockItemWiseListForPdf.get(k).getIssueLandingValue();

												sumIssueLandValue = sumIssueLandValue
														+ stockItemWiseListForPdf.get(k).getIssueLandingValue();

												cell = new PdfPCell(new Phrase(
														"" + stockItemWiseListForPdf.get(k).getDamageQty(),
														headFont));
												cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
												cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
												cell.setPaddingRight(2);
												cell.setPadding(3);
												table.addCell(cell);

												totalDamageQty = totalDamageQty
														+ stockItemWiseListForPdf.get(k).getDamageQty();

												sumDamageQty = sumDamageQty
														+ stockItemWiseListForPdf.get(k).getDamageQty();

												/*
												 * cell = new PdfPCell(new Phrase( "" +
												 * df.format(stockItemWiseListForPdf.get(k).getDamageValue()),
												 * headFont)); cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
												 * cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
												 * cell.setPaddingRight(2); cell.setPadding(3); table.addCell(cell);
												 */

												totalDamageValue = totalDamageValue
														+ stockItemWiseListForPdf.get(k).getDamageValue();

												sumDamageValue = sumDamageValue
														+ stockItemWiseListForPdf.get(k).getDamageValue();

												cell = new PdfPCell(new Phrase(
														"" + df.format(
																stockItemWiseListForPdf.get(k).getDamageLandingValue()),
														headFont));
												cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
												cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
												cell.setPaddingRight(2);
												cell.setPadding(3);
												table.addCell(cell);

												totalDamageLandValue = totalDamageLandValue
														+ stockItemWiseListForPdf.get(k).getDamageLandingValue();

												sumDamageLandValue = sumDamageLandValue
														+ stockItemWiseListForPdf.get(k).getDamageLandingValue();

												float closingQty = stockItemWiseListForPdf.get(k).getOpeningStock()
														+ stockItemWiseListForPdf.get(k).getApproveQty()
														- stockItemWiseListForPdf.get(k).getIssueQty()
														- stockItemWiseListForPdf.get(k).getDamageQty();

												float closingValue = stockItemWiseListForPdf.get(k).getOpStockValue()
														+ stockItemWiseListForPdf.get(k).getApprovedQtyValue()
														- stockItemWiseListForPdf.get(k).getIssueQtyValue()
														- stockItemWiseListForPdf.get(k).getDamageValue();

												float closingLandingValue = stockItemWiseListForPdf.get(k)
														.getOpLandingValue()
														+ stockItemWiseListForPdf.get(k).getApprovedLandingValue()
														- stockItemWiseListForPdf.get(k).getIssueLandingValue()
														- stockItemWiseListForPdf.get(k).getDamageLandingValue();

												cell = new PdfPCell(new Phrase("" +closingQty, headFont));
												cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
												cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
												cell.setPaddingRight(2);
												cell.setPadding(3);
												table.addCell(cell);
												totalClsQty = totalClsQty + closingQty;

												sumClsQty = sumClsQty + closingQty;

												/*
												 * cell = new PdfPCell(new Phrase("" + df.format(closingValue),
												 * headFont)); cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
												 * cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
												 * cell.setPaddingRight(2); cell.setPadding(3); table.addCell(cell);
												 */

												totalClsValue = totalClsValue + closingValue;

												sumClsValue = sumClsValue + closingValue;

												cell = new PdfPCell(
														new Phrase("" + df.format(closingLandingValue), headFont));
												cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
												cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
												cell.setPaddingRight(2);
												cell.setPadding(3);
												table.addCell(cell);

												totalClsLandValue = totalClsLandValue + closingLandingValue;
												sumClsLandValue = sumClsLandValue + closingLandingValue;

												cell = new PdfPCell(new Phrase(" ", headFont));
												cell.setPaddingRight(2);
												cell.setPadding(3);
												table.addCell(cell);

												

											}
										}

									}

									hcell = new PdfPCell(new Phrase("TOTAL", totalFont));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									hcell.setColspan(2);
									table.addCell(hcell);

									//hcell = new PdfPCell(new Phrase(" " + sumOpStock, totalFont));
									hcell = new PdfPCell(new Phrase(" ", totalFont));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									table.addCell(hcell);

									/*
									 * hcell = new PdfPCell(new Phrase("" + sumOpValue, totalFont));
									 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER); table.addCell(hcell);
									 */

									hcell = new PdfPCell(new Phrase("" + sumOpLandValue, totalFont));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									table.addCell(hcell);

									//hcell = new PdfPCell(new Phrase("" + sumAprvQty, totalFont));
									hcell = new PdfPCell(new Phrase(" ", totalFont));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									table.addCell(hcell);

									/*
									 * hcell = new PdfPCell(new Phrase("" + sumAprvValue, totalFont));
									 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER); table.addCell(hcell);
									 */

									hcell = new PdfPCell(new Phrase("" + sumAprvLandValue, totalFont));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									table.addCell(hcell);

									//hcell = new PdfPCell(new Phrase("" + sumIssueQty, totalFont));
									hcell = new PdfPCell(new Phrase(" ", totalFont));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									table.addCell(hcell);

									/*
									 * hcell = new PdfPCell(new Phrase("" + sumIssueValue, totalFont));
									 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER); table.addCell(hcell);
									 */

									hcell = new PdfPCell(new Phrase("" + sumIssueLandValue, totalFont));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									table.addCell(hcell);

									//hcell = new PdfPCell(new Phrase("" + sumDamageQty, totalFont));
									hcell = new PdfPCell(new Phrase(" ", totalFont));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									table.addCell(hcell);

									/*
									 * hcell = new PdfPCell(new Phrase("" + sumDamageValue, totalFont));
									 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER); table.addCell(hcell);
									 */

									hcell = new PdfPCell(new Phrase("" + sumDamageLandValue, totalFont));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									table.addCell(hcell);

									//hcell = new PdfPCell(new Phrase("" + sumClsQty, totalFont));
									hcell = new PdfPCell(new Phrase(" ", totalFont));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									table.addCell(hcell);

									/*
									 * hcell = new PdfPCell(new Phrase("" + sumClsValue, totalFont));
									 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER); table.addCell(hcell);
									 */

									hcell = new PdfPCell(new Phrase("" + sumClsLandValue, totalFont));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									table.addCell(hcell);

									float totOpe = 0, totPur = 0, totIssue = 0, totDamage = 0, totClosing = 0;
									for (int j = 0; j < stockItemWiseListForPdf.size(); j++) {

										if (uniqueCatIdList.get(c) == stockItemWiseListForPdf.get(j).getCatId()) {

											totOpe = totOpe + stockItemWiseListForPdf.get(j).getOpLandingValue();
											totPur = totPur + stockItemWiseListForPdf.get(j).getApprovedLandingValue();
											totIssue = totIssue + stockItemWiseListForPdf.get(j).getIssueLandingValue();
											totDamage = totDamage
													+ stockItemWiseListForPdf.get(j).getDamageLandingValue();

											float closing = stockItemWiseListForPdf.get(j).getOpLandingValue()
													+ stockItemWiseListForPdf.get(j).getApprovedLandingValue()
													- stockItemWiseListForPdf.get(j).getIssueLandingValue()
													- stockItemWiseListForPdf.get(j).getDamageLandingValue();

											totClosing = totClosing + closing;

										}

									}

//									float opePer = (sumOpLandValue * 100) / totOpe;
//									float purPer = (sumAprvLandValue * 100) / totPur;
//									float issuePer = (sumIssueLandValue * 100) / totIssue;
//									float damagePer = (sumDamageLandValue * 100) / totDamage;
//									float closePer = (sumClsLandValue * 100) / totClosing;
									
									
									float opePer=0,purPer=0,issuePer=0,damagePer=0,closePer=0;
									if(totOpe>0) {
										opePer = (sumOpLandValue * 100) / totOpe;
									}
									
									if(totPur>0) {
										purPer = (sumAprvLandValue * 100) / totPur;
									}
									
									if(totIssue>0) {
										issuePer = (sumIssueLandValue * 100) / totIssue;
									}
									
									if(totDamage>0) {
										damagePer = (sumDamageLandValue * 100) / totDamage;
									}
									
									if(totClosing>0) {
										closePer = (sumClsLandValue * 100) / totClosing;
									}
									

									if(uniqueCatIdList.get(c)==3) {

										hcell = new PdfPCell(new Phrase(" ", totalFont));
										hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
										table.addCell(hcell);
										
									}else {

										hcell = new PdfPCell(new Phrase("" + String.format("%.2f", issuePer), totalFont));
										hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
										table.addCell(hcell);
										
									}
									

									

									catOpStock = catOpStock + sumOpStock;
									catOpValue = catOpValue + sumOpValue;
									catOpLandValue = catOpLandValue + sumOpLandValue;
									catAprvQty = catAprvQty + sumAprvQty;
									catAprvValue = catAprvValue + sumAprvValue;
									catAprvLandValue = catAprvLandValue + sumAprvLandValue;
									catIssueQty = catIssueQty + sumIssueQty;
									catIssueValue = catIssueValue + sumIssueValue;
									catIssueLandValue = catIssueLandValue + sumIssueLandValue;
									catDamageQty = catDamageQty + sumDamageQty;
									catDamageValue = catDamageValue + sumDamageValue;
									catDamageLandValue = catDamageLandValue + sumDamageLandValue;
									catClsQty = catClsQty + sumClsQty;
									catClsValue = catClsValue + sumClsValue;
									catClsLandValue = catClsLandValue + sumClsLandValue;

									// ---BLANK---------
									hcell = new PdfPCell(new Phrase(" ", totalFont));
									hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
									hcell.setColspan(13);
									table.addCell(hcell);

								}

							}

						}

						for (int i = 0; i < itemGroupList.size(); i++) {
							if (uniqueCatIdList.get(c) == itemGroupList.get(i).getCatId()) {

								hcell = new PdfPCell(
										new Phrase("" + itemGroupList.get(i).getCatDesc() + " Total", totalFont));
								hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
								hcell.setColspan(2);
								table.addCell(hcell);

								break;
							}
						}

						hcell = new PdfPCell(new Phrase(" ", totalFont));
						hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
						table.addCell(hcell);

						/*
						 * hcell = new PdfPCell(new Phrase("" + catOpValue, totalFont));
						 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER); table.addCell(hcell);
						 */

						hcell = new PdfPCell(new Phrase("" + catOpLandValue, totalFont));
						hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
						table.addCell(hcell);

						hcell = new PdfPCell(new Phrase(" ", totalFont));
						hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
						table.addCell(hcell);

						/*
						 * hcell = new PdfPCell(new Phrase("" + catAprvValue, totalFont));
						 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER); table.addCell(hcell);
						 */

						hcell = new PdfPCell(new Phrase("" + catAprvLandValue, totalFont));
						hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
						table.addCell(hcell);

						hcell = new PdfPCell(new Phrase(" ", totalFont));
						hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
						table.addCell(hcell);

						/*
						 * hcell = new PdfPCell(new Phrase("" + catIssueValue, totalFont));
						 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER); table.addCell(hcell);
						 */

						hcell = new PdfPCell(new Phrase("" + catIssueLandValue, totalFont));
						hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
						table.addCell(hcell);

						hcell = new PdfPCell(new Phrase(" ", totalFont));
						hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
						table.addCell(hcell);

						/*
						 * hcell = new PdfPCell(new Phrase("" + catDamageValue, totalFont));
						 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER); table.addCell(hcell);
						 */

						hcell = new PdfPCell(new Phrase("" + catDamageLandValue, totalFont));
						hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
						table.addCell(hcell);

						hcell = new PdfPCell(new Phrase(" ", totalFont));
						hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
						table.addCell(hcell);

						/*
						 * hcell = new PdfPCell(new Phrase("" + catClsValue, totalFont));
						 * hcell.setHorizontalAlignment(Element.ALIGN_CENTER); table.addCell(hcell);
						 */

						hcell = new PdfPCell(new Phrase("" + catClsLandValue, totalFont));
						hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
						table.addCell(hcell);

						hcell = new PdfPCell(new Phrase(" ", totalFont));
						hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
						table.addCell(hcell);

						// ---BLANK---------
						hcell = new PdfPCell(new Phrase(" ", totalFont));
						hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
						hcell.setColspan(13);
						table.addCell(hcell);

					}

				}

				PdfPCell cell;

				cell = new PdfPCell(new Phrase("FINAL TOTAL", totalFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_CENTER);
				cell.setPadding(3);
				cell.setColspan(2);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase(" ", totalFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalOpLandValue), totalFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase(" ", totalFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalAprvLandValue), totalFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase(" ", totalFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalIssueLandValue), totalFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase(" ", totalFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalDamageLandValue), totalFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase(" ", totalFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase("" + df.format(totalClsLandValue), totalFont));
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				cell = new PdfPCell(new Phrase(" ", totalFont));
				cell.setPaddingRight(2);
				cell.setPadding(3);
				table.addCell(cell);

				document.open();
				Paragraph company = new Paragraph(comp.getCompanyName() + "\n", f);
				company.setAlignment(Element.ALIGN_CENTER);
				document.add(company);

				Paragraph heading1 = new Paragraph(comp.getOfficeAdd() + "\n", f1);
				heading1.setAlignment(Element.ALIGN_CENTER);
				document.add(heading1);

				Paragraph headingDate = new Paragraph(
						"Sub Category Wise Stock Report , From Date: " + fromDate + "  To Date: " + toDate + "", f1);
				headingDate.setAlignment(Element.ALIGN_CENTER);
				document.add(headingDate);

				Paragraph ex3 = new Paragraph("\n");
				document.add(ex3);
				table.setHeaderRows(1);
				document.add(table);

				int totalPages = writer.getPageNumber();

				System.out.println("Page no " + totalPages);

				document.close();

				if (file != null) {

					String mimeType = URLConnection.guessContentTypeFromName(file.getName());

					if (mimeType == null) {

						mimeType = "application/pdf";

					}

					response.setContentType(mimeType);

					response.addHeader("content-disposition", String.format("inline; filename=\"%s\"", file.getName()));

					response.setContentLength((int) file.length());

					InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

					try {
						FileCopyUtils.copy(inputStream, response.getOutputStream());
					} catch (IOException e) {
						System.out.println("Excep in Opening a Pdf File");
						e.printStackTrace();
					}
				}

			} catch (DocumentException ex) {

				System.out.println("Pdf Generation Error" + ex.getMessage());

				ex.printStackTrace();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
