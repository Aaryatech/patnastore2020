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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
import com.ats.tril.model.Company;
import com.ats.tril.model.ExportToExcel;
import com.ats.tril.model.MonthwiseAvgItemRate;
import com.ats.tril.model.StockHeader;
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
public class ItemReportController {

	RestTemplate rest = new RestTemplate();
	List<MonthwiseAvgItemRate> itemList = new ArrayList<MonthwiseAvgItemRate>();

	public String incrementDate(String date, int day) {

		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
		Calendar c = Calendar.getInstance();
		try {
			c.setTime(sdf.parse(date));

		} catch (ParseException e) {
			System.out.println("Exception while incrementing date " + e.getMessage());
			e.printStackTrace();
		}
		c.add(Calendar.DATE, day); // number of days to add
		date = sdf.format(c.getTime());

		return date;

	}

	public static String getLastDayOfTheMonth(String date) {
		String lastDayOfTheMonth = "";

		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
		try {
			java.util.Date dt = formatter.parse(date);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(dt);

			calendar.add(Calendar.MONTH, 1);
			calendar.set(Calendar.DAY_OF_MONTH, 1);
			calendar.add(Calendar.DATE, -1);

			java.util.Date lastDay = calendar.getTime();

			lastDayOfTheMonth = formatter.format(lastDay);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return lastDayOfTheMonth;
	}

	@RequestMapping(value = "/showMonthwiseAvgItemRate", method = RequestMethod.GET)
	public ModelAndView showMonthwiseAvgItemRate(HttpServletRequest request, HttpServletResponse response) {
		itemList = new ArrayList<MonthwiseAvgItemRate>();

		ModelAndView model = new ModelAndView("report/monthly_avg_itemrate");
		try {

			DateFormat dateFormat = new SimpleDateFormat("dd-MM-YYYY");

			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.DAY_OF_MONTH, 1);
			Date firstDate = cal.getTime();
			String fromDate = dateFormat.format(firstDate);

			int incDay = 1;
			StockHeader stockHeader = rest.getForObject(Constants.url + "/getCurrentRunningMonthAndYear",
					StockHeader.class);
			System.err.println("stockHeader.getDate() " + stockHeader.getDate());
			if (request.getParameter("from_date") == null || request.getParameter("from_date") == "") {
				System.err.println("in if ");
				fromDate = DateConvertor.convertToDMY(stockHeader.getDate());// dateFormat.format(stockHeader.getDate());
				incDay = 1;
			} else {
				System.err.println("in else ");
				fromDate = request.getParameter("from_date");
				incDay = 2;
			}
			System.err.println("from date setted " + fromDate);
			// DateFormat dateFormat2 = new SimpleDateFormat("YYYY-MM-dd");
			// cal.setTime(dateFormat2.parse(fromDate));

			String x = getLastDayOfTheMonth(fromDate);
			System.err.println("x " + x);

			java.util.Date dt = dateFormat.parse(fromDate);
			System.err.println("DT = " + dt);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(dt);

			cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
			Date lastDate = cal.getTime();
			String toDate = dateFormat.format(lastDate);

			System.err.println("from Date " + fromDate + "toDate " + x);

			cal = Calendar.getInstance();
			cal.set(Calendar.MONTH, firstDate.getMonth() - 1);
			cal.set(Calendar.DAY_OF_MONTH, 1);

			Date prevLastDate = cal.getTime();
			String prevToDate = incrementDate(fromDate, -1);// dateFormat.format(prevLastDate);
			cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));

			Date prevFirstDate = cal.getTime();
			String prevfromDate = incrementDate(prevToDate, (-cal.getActualMaximum(Calendar.DAY_OF_MONTH) + incDay));// dateFormat.format(prevFirstDate);
			System.err.println("prevfromDate Date " + prevfromDate + "prevToDate " + prevToDate);

			MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();

			map.add("fromDate", fromDate);
			map.add("toDate", x);
			map.add("prevFromDate", prevfromDate);
			map.add("prevToDate", prevToDate);

			MonthwiseAvgItemRate[] itemArray = rest.postForObject(Constants.url + "/getMonthwiseAvgItemRate", map,
					MonthwiseAvgItemRate[].class);

			itemList = new ArrayList<MonthwiseAvgItemRate>(Arrays.asList(itemArray));
			System.err.println("detail list " + itemList.toString());
			model.addObject("itemList", itemList);

		} catch (Exception e) {

			System.err.println("Exce in showMonthwiseAvgItemRate   " + e.getMessage());
			e.printStackTrace();
		}

		return model;
	}

	@RequestMapping(value = "/getItemRateAvgMonth", method = RequestMethod.GET)
	public @ResponseBody List<MonthwiseAvgItemRate> getItemRateAvgMonth(HttpServletRequest request,
			HttpServletResponse response) {

		int viewItems = Integer.parseInt(request.getParameter("viewItems"));

		List<ExportToExcel> exportToExcelList = new ArrayList<ExportToExcel>();

		ExportToExcel expoExcel = new ExportToExcel();
		List<String> rowData = new ArrayList<String>();

		rowData.add("SR. No");
		rowData.add("Item Desc");
		rowData.add("Code");
		rowData.add("UOM");
		rowData.add("Prev Month Avg Rate");
		rowData.add("This Month Avg Rate");
		rowData.add("Difference");

		expoExcel.setRowData(rowData);
		exportToExcelList.add(expoExcel);
		int sr = 0;
		for (int i = 0; i < itemList.size(); i++) {

			float fmAvg = (itemList.get(i).getApprovedQtyValueCm() / itemList.get(i).getApprovedQtyCm());
			float lmAvg = (itemList.get(i).getApprovedQtyValuePm() / itemList.get(i).getApprovedQtyPm());
			float diff = (fmAvg - lmAvg);

			if (viewItems==0) {
				expoExcel = new ExportToExcel();
				rowData = new ArrayList<String>();
				sr = sr + 1;
				rowData.add((sr) + "");
				rowData.add(itemList.get(i).getItemCode());
				rowData.add((itemList.get(i).getItemDesc()));
				rowData.add(itemList.get(i).getItemUom());

				rowData.add((""+lmAvg));
				rowData.add(""+fmAvg);
				rowData.add((""+diff));

				expoExcel.setRowData(rowData);
				exportToExcelList.add(expoExcel);
			} else if (viewItems == 1) {
				if (diff>0) {
					sr = sr + 1;

					expoExcel = new ExportToExcel();
					rowData = new ArrayList<String>();

					rowData.add((sr) + "");
					rowData.add(itemList.get(i).getItemCode());
					rowData.add((itemList.get(i).getItemDesc()));
					rowData.add(itemList.get(i).getItemUom());

					rowData.add((""+lmAvg));
					rowData.add(""+fmAvg);
					rowData.add((""+diff));

					expoExcel.setRowData(rowData);
					exportToExcelList.add(expoExcel);
				}
			} else if (viewItems == 2) {

				if (diff<0) {
					sr = sr + 1;

					expoExcel = new ExportToExcel();
					rowData = new ArrayList<String>();

					rowData.add(""+sr);
					rowData.add(itemList.get(i).getItemCode());
					rowData.add((itemList.get(i).getItemDesc()));
					rowData.add(itemList.get(i).getItemUom());

					rowData.add((""+lmAvg));
					rowData.add(""+fmAvg);
					rowData.add((""+diff));

					expoExcel.setRowData(rowData);
					exportToExcelList.add(expoExcel);

				}
			}

		}

		HttpSession session = request.getSession();
		session.setAttribute("exportExcelList", exportToExcelList);
		session.setAttribute("excelName", "AvgItemRateReport");

		return itemList;

	}
	
	
	@RequestMapping(value = "/getItemRateAvgMonthPdf/{viewItems}", method = RequestMethod.GET)
	public void stockValuetionReportCategoryWisePDF(HttpServletRequest request, HttpServletResponse response,
			@PathVariable int viewItems)
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
			table.setWidths(new float[] {0.4f, 1.2f,0.4f, 0.4f, 1.0f, 1.0f, 1.0f});
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

			hcell = new PdfPCell(new Phrase("Item Desc", headFont1));
			hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
			hcell.setBackgroundColor(BaseColor.PINK);
			table.addCell(hcell);
			
			hcell = new PdfPCell(new Phrase("CODE", headFont1));
			hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
			hcell.setBackgroundColor(BaseColor.PINK);
			table.addCell(hcell);
			
			hcell = new PdfPCell(new Phrase("UOM", headFont1));
			hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
			hcell.setBackgroundColor(BaseColor.PINK);
			table.addCell(hcell);
			
			hcell = new PdfPCell(new Phrase("Prev Month Avg Rate", headFont1));
			hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
			hcell.setBackgroundColor(BaseColor.PINK);
			table.addCell(hcell);
			
			hcell = new PdfPCell(new Phrase("This Month Avg Rate", headFont1));
			hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
			hcell.setBackgroundColor(BaseColor.PINK);
			table.addCell(hcell);
			
			hcell = new PdfPCell(new Phrase("Difference", headFont1));
			hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
			hcell.setBackgroundColor(BaseColor.PINK);
			table.addCell(hcell);
			
			//int viewItems = Integer.parseInt(request.getParameter("viewItems"));

			int index = 0;
		
					for (int k = 0; k < itemList.size(); k++) {
                            
						
						float fmAvg = (itemList.get(k).getApprovedQtyValueCm() / itemList.get(k).getApprovedQtyCm());
						float lmAvg = (itemList.get(k).getApprovedQtyValuePm() / itemList.get(k).getApprovedQtyPm());
						float diff = (fmAvg - lmAvg);

						if (viewItems==0) {
						
							index++;
						
							PdfPCell cell;
							
							cell = new PdfPCell(new Phrase(""+index, headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPadding(3);
							table.addCell(cell);

						
							cell = new PdfPCell(new Phrase(itemList.get(k).getItemDesc(), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
						
							cell = new PdfPCell(new Phrase(""+itemList.get(k).getItemCode(), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							
							cell = new PdfPCell(new Phrase(""+itemList.get(k).getItemUom(), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							
							cell = new PdfPCell(new Phrase(""+df.format(lmAvg), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							
							cell = new PdfPCell(new Phrase(""+df.format(fmAvg), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							
							cell = new PdfPCell(new Phrase(""+df.format(diff), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
					}else if(viewItems==1) {
						
						if(diff>0) {
							index++;
							
							PdfPCell cell;
							
							cell = new PdfPCell(new Phrase(""+index, headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPadding(3);
							table.addCell(cell);

						
							cell = new PdfPCell(new Phrase(itemList.get(k).getItemDesc(), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
						
							cell = new PdfPCell(new Phrase(""+itemList.get(k).getItemCode(), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							
							cell = new PdfPCell(new Phrase(""+itemList.get(k).getItemUom(), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							
							cell = new PdfPCell(new Phrase(""+df.format(lmAvg), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							
							cell = new PdfPCell(new Phrase(""+df.format(fmAvg), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							
							cell = new PdfPCell(new Phrase(""+df.format(diff), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							
						}
						
					}else if(viewItems==2) {
						
						if(diff<0) {
							
							
							index++;
							
							PdfPCell cell;
							
							cell = new PdfPCell(new Phrase(""+index, headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPadding(3);
							table.addCell(cell);

						
							cell = new PdfPCell(new Phrase(itemList.get(k).getItemDesc(), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
						
							cell = new PdfPCell(new Phrase(""+itemList.get(k).getItemCode(), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							
							cell = new PdfPCell(new Phrase(""+itemList.get(k).getItemUom(), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_LEFT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							
							cell = new PdfPCell(new Phrase(""+df.format(lmAvg), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							
							cell = new PdfPCell(new Phrase(""+df.format(fmAvg), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							
							cell = new PdfPCell(new Phrase(""+df.format(diff), headFont));
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
							cell.setPaddingRight(2);
							cell.setPadding(3);
							table.addCell(cell);
							
							
							
						}
					}
						
			}
			
			document.open();
		
			
			Paragraph ex3=new Paragraph("\n");
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
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	

}
