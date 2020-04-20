<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<jsp:include page="/WEB-INF/views/include/header.jsp"></jsp:include>


<body onload="search()">
	<c:url var="getItemRateAvgMonth" value="/getItemRateAvgMonth"></c:url>

	<div class="container" id="main-container">

		<!-- BEGIN Sidebar -->
		<div id="sidebar" class="navbar-collapse collapse">

			<jsp:include page="/WEB-INF/views/include/navigation.jsp"></jsp:include>

			<div id="sidebar-collapse" class="visible-lg">
				<i class="fa fa-angle-double-left"></i>
			</div>
			<!-- END Sidebar Collapse Button -->
		</div>
		<!-- END Sidebar -->

		<!-- BEGIN Content -->
		<div id="main-content">
			<!-- BEGIN Page Title -->
			<div class="page-title">
				<!-- <div>
					<h1>
						<i class="fa fa-file-o"></i>Indent Header
					</h1>

				</div> -->
			</div>
			<br>
			<!-- END Page Title -->



			<!-- BEGIN Main Content -->
			<div class="row">
				<div class="col-md-12">
					<div class="box">
						<div class="box-title">
							<h3>
								<i class="fa fa-bars"></i>Item wise Rate Avg For Month
							</h3>
							<div class="box-tool">
								<div class="box-tool">
									<%-- 	<a href="${pageContext.request.contextPath}/showIndent">Add Indent</a> <a data-action="collapse" href="#"><i
									class="fa fa-chevron-up"></i></a>  --%>
								</div>
							</div>
							<!-- <div class="box-tool">
								<a data-action="collapse" href="#"><i
									class="fa fa-chevron-up"></i></a> <a data-action="close" href="#"><i
									class="fa fa-times"></i></a>
							</div> -->
						</div>

						<c:set value="0" var="isEdit"></c:set>
						<c:set value="0" var="isDelete"></c:set>
						<c:forEach items="${sessionScope.newModuleList}"
							var="allModuleList">
							<c:choose>
								<c:when
									test="${allModuleList.moduleId==sessionScope.sessionModuleId}">
									<c:forEach items="${allModuleList.subModuleJsonList}"
										var="subModuleJsonList">
										<c:choose>
											<c:when
												test="${subModuleJsonList.subModuleId==sessionScope.sessionSubModuleId}">
												<c:choose>

													<c:when test="${subModuleJsonList.editReject eq 'visible'}">
														<c:set value="1" var="isEdit"></c:set>
													</c:when>
												</c:choose>
												<c:choose>
													<c:when
														test="${subModuleJsonList.deleteRejectApprove eq 'visible'}">
														<c:set value="1" var="isDelete"></c:set>
													</c:when>
												</c:choose>
											</c:when>
										</c:choose>
									</c:forEach>
								</c:when>
							</c:choose>

						</c:forEach>


						<div class="box-content">
							<form
								action="${pageContext.request.contextPath}/showMonthwiseAvgItemRate"
								class="form-horizontal" id="validation-form" method="get">



								<input type="hidden" name="mode_add" id="mode_add"
									value="add_att">

								<div class="form-group">
									<label class="col-sm-3 col-lg-2 control-label">From
										Month</label>
									<div class="col-sm-5 col-lg-3 controls">
										<input class="form-control date-picker" id="from_date"
											size="16" type="text" name="from_date" value="${fromDate}"
											required />
									</div>
									<!-- </div>


								<div class="form-group"> -->
									<%-- <label class="col-sm-3 col-lg-2 control-label">To Date</label>
									<div class="col-sm-5 col-lg-3 controls">
										<input class="form-control date-picker" id="to_date" size="16"
											type="text" name="to_date" required value="${toDate}" />
									</div> --%>

									<div
										class="col-sm-25 col-sm-offset-3 col-lg-30 col-lg-offset-0">
										<input type="submit" value="Submit" class="btn btn-primary">
										
										<input type="button" value="PDF" class="btn btn-primary"
													onclick="genPdf()" />&nbsp;
													 <input type="button" id="expExcel" class="btn btn-primary" value="EXPORT TO Excel" onclick="exportToExcel();">
									</div>

								</div>

								<div class="form-group">
									<label class="col-sm-3 col-lg-2 control-label">Select
										View</label>
									<div class="col-sm-5 col-lg-3 controls">
										<select class="form-control chosen" name="view_items" id="view_items" onchange="search()"
											required>
											<option value="0">All</option>
											<option value="1">High</option>
											<option value="2">Low</option>

										</select>
									</div>


								</div>


								<div class="clearfix"></div>
								<div id="table-scroll" class="table-scroll">

									<div id="faux-table" class="faux-table" aria="hidden">
										<table id="table2" class="main-table">
											<thead>
												<tr class="bgpink">
													<!-- <th width="180" style="width: 90px">Indent No</th>
													<th width="200" align="left">Date</th>
													<th width="358" align="left">Category</th>
													<th width="194" align="left">Type</th>
													<th width="102" align="left">Development</th>
													<th width="102" align="left">Monthly</th>

													<th width="88" align="left">Action</th> -->
												</tr>
											</thead>
										</table>

									</div>
									<div class="col-md-8"></div>
									<!-- 			<label for="search" class="col-md-2" id="search">
    <i class="fa fa-search" style="font-size:15px"></i>
									<input type="text" value="" id="myInput" style="text-align: left; width: 240px;" class="form-control" onkeyup="myFunction()" placeholder="Search Mrn by Name or Vendor" title="Type in a name">
										</label>  -->
									<div class="input-group">
										<!-- <input type="text"  id="myInput"  style="text-align: left; color: green;" class="form-control" onkeyup="myFunction()" placeholder="Search Indent By No Or Type"/>
    <span class="input-group-addon">
        <i class="fa fa-search"></i>
    </span> -->
									</div>
									<br />
									<div class="table-wrap">

										<table id="table1" class="table table-advance">
											<thead>
												<tr class="bgpink">

													<th style="width: 5%;">Sr.</th>
													<th class="col-md-1">Item Desc</th>
													<th class="col-md-1">Code</th>
													<th class="col-md-1">UOM</th>
													<th class="col-md-1">Prev Month Avg Rate</th>
													<th class="col-md-1">This Month Avg Rate</th>
													<th class="col-md-1">Difference</th>
												</tr>
											</thead>
											<!-- 	<div class="table-responsive" style="border: 0">
									<table width="100%" class="table table-advance" id="table1">
										<thead>
											<tr>
												<th width="180" style="width: 90px">Prod ID</th>
												<th width="200" align="left">Production Date</th>
												<th width="358" align="left">Category</th>
												<th width="194" align="left">Status</th>
												<th width="102" align="left">IsPlanned</th>
												<th width="88" align="left">Action</th>
											</tr>
										</thead> -->
											<tbody>
												<c:forEach items="${itemList}" var="item" varStatus="count">
													<tr>

														<c:set var="fmAvg"
															value="${item.approvedQtyValueCm / item.approvedQtyCm}"></c:set>

														<c:set var="lmAvg"
															value="${item.approvedQtyValuePm / item.approvedQtyPm}"></c:set>
														<c:set var="diff" value="${fmAvg-lmAvg}"></c:set>

														<td><c:out value="${count.index+1}" /></td>
														<td><c:out value="${item.itemDesc}" /></td>
														<td><c:out value="${item.itemCode}" /></td>
														<td><c:out value="${item.itemUom}" /></td>

														<td><fmt:formatNumber type="number"
																maxFractionDigits="2" value="${lmAvg}" /></td>
														<td><fmt:formatNumber type="number"
																maxFractionDigits="2" value="${fmAvg}" /></td>

														<td><fmt:formatNumber type="number"
																maxFractionDigits="2" value="${diff}" /></td>

													</tr>
												</c:forEach>

											</tbody>
										</table>


										<br> <br>
										<button
											style="background-color: #008CBA; border: none; color: white; text-align: center; text-decoration: none; display: block; font-size: 12px; cursor: pointer; width: 50px; height: 30px; margin: auto;"
											onclick="commonPdf()">PDF</button>


									</div>

								</div>
							</form>
						</div>
					</div>

				</div>

			</div>
			<!-- END Main Content -->
			<footer>
			<p>2019 © MONGINIS</p>
			</footer>

			<a id="btn-scrollup" class="btn btn-circle btn-lg" href="#"><i
				class="fa fa-chevron-up"></i></a>
		</div>
		<!-- END Content -->
	</div>
	<!-- END Container -->

	<!--basic scripts-->


	<script type="text/javascript">
		function genPdf(id) {

			window.open('pdfForReport?url=/pdf/indentPdfDoc/' + id);

		}
		
		
		
		function commonPdf() {

			var list = [];

			$("input:checkbox[name=name1]:checked").each(function() {
				list.push($(this).val());
			});

			window.open('pdfForReport?url=/pdf/indentPdfDoc/' + list);

		}
	</script>


	<script>
function myFunction() {
  var input, filter, table, tr, td,td1, i;
  input = document.getElementById("myInput");
  filter = input.value.toUpperCase();
  table = document.getElementById("table1");
  tr = table.getElementsByTagName("tr");
  for (i = 0; i < tr.length; i++) {
    td = tr[i].getElementsByTagName("td")[5];
    td1 = tr[i].getElementsByTagName("td")[1];
    if (td || td1) {
      if (td.innerHTML.toUpperCase().indexOf(filter) > -1) {
        tr[i].style.display = "";
      }else if (td1.innerHTML.toUpperCase().indexOf(filter) > -1) {
        tr[i].style.display = "";
      }  else {
        tr[i].style.display = "none";
      }
    }       
  }//end of for
  
 
  
}


function search() {
	  
	
	
	var viewItems = $("#view_items").val();
	

	$
			.getJSON(
					'${getItemRateAvgMonth}',

					{
						viewItems : viewItems,
					
						ajax : 'true',

					},
					function(data) {

						$('#table1 td').remove();

						if (data == "") {
							alert("No records found !!");

						}
					 var sr=0;

					  $.each( data,
									function(key, itemList) {
						
						  var fmAvg=parseFloat(itemList.approvedQtyValueCm)/parseFloat(itemList.approvedQtyCm);
						  
						  var lmAvg=parseFloat(itemList.approvedQtyValuePm)/parseFloat(itemList.approvedQtyPm);
						  var diff=parseFloat(fmAvg-lmAvg);
						  fmAvg=fmAvg.toFixed(2);
						  lmAvg=lmAvg.toFixed(2);
						  diff=diff.toFixed(2);
						  
						  if(viewItems==0){
							 // alert("viewItems ==0");
														  
										var tr = $('<tr></tr>'); 
									  	tr.append($('<td></td>').html(key+1));
									 	tr.append($('<td></td>').html(itemList.itemDesc));
									  	tr.append($('<td></td>').html(itemList.itemCode));
									  	tr.append($('<td></td>').html(itemList.itemUom));  
									  	
									  	tr.append($('<td></td>').html(lmAvg));
									  	tr.append($('<td></td>').html(fmAvg));
									  	tr.append($('<td></td>').html(diff));  
						  }else if(viewItems==1){
							  if(diff>0){
								  sr=sr+1;
								 // alert("diff >0");
								  var tr = $('<tr></tr>'); 
								  	tr.append($('<td></td>').html(sr));
								 	tr.append($('<td></td>').html(itemList.itemDesc));
								  	tr.append($('<td></td>').html(itemList.itemCode));
								  	tr.append($('<td></td>').html(itemList.itemUom));  
								  	
								  	tr.append($('<td></td>').html(lmAvg));
								  	tr.append($('<td></td>').html(fmAvg));
								  	tr.append($('<td></td>').html(diff));  
								  
							  }
							  
						  }
						  else if(viewItems==2){
							  
							  if(diff<0){
								  sr=sr+1;
								 // alert("diff >0");
								  var tr = $('<tr></tr>'); 
								  	tr.append($('<td></td>').html(sr));
								 	tr.append($('<td></td>').html(itemList.itemDesc));
								  	tr.append($('<td></td>').html(itemList.itemCode));
								  	tr.append($('<td></td>').html(itemList.itemUom));  
								  	
								  	tr.append($('<td></td>').html(lmAvg));
								  	tr.append($('<td></td>').html(fmAvg));
								  	tr.append($('<td></td>').html(diff));  
								  
							  }
						  }
									  	
									    $('#table1 tbody').append(tr); 
									})  
									
						 
					}); 
}

function genPdf(){
	var viewItems = $("#view_items").val();

	window.open('${pageContext.request.contextPath}/getItemRateAvgMonthPdf/'+viewItems);
}
function exportToExcel()
{
	window.open("${pageContext.request.contextPath}/exportToExcel");
	//document.getElementById("expExcel").disabled=true;
}
</script>


	<script
		src="//ajax.googleapis.com/ajax/libs/jquery/2.0.3/jquery.min.js"></script>
	<script>
		window.jQuery
				|| document
						.write('<script src="${pageContext.request.contextPath}/resources/assets/jquery/jquery-2.0.3.min.js"><\/script>')
	</script>
	<script
		src="${pageContext.request.contextPath}/resources/assets/bootstrap/js/bootstrap.min.js"></script>
	<script
		src="${pageContext.request.contextPath}/resources/assets/jquery-slimscroll/jquery.slimscroll.min.js"></script>
	<script
		src="${pageContext.request.contextPath}/resources/assets/jquery-cookie/jquery.cookie.js"></script>

	<!--page specific plugin scripts-->
	<script
		src="${pageContext.request.contextPath}/resources/assets/flot/jquery.flot.js"></script>
	<script
		src="${pageContext.request.contextPath}/resources/assets/flot/jquery.flot.resize.js"></script>
	<script
		src="${pageContext.request.contextPath}/resources/assets/flot/jquery.flot.pie.js"></script>
	<script
		src="${pageContext.request.contextPath}/resources/assets/flot/jquery.flot.stack.js"></script>
	<script
		src="${pageContext.request.contextPath}/resources/assets/flot/jquery.flot.crosshair.js"></script>
	<script
		src="${pageContext.request.contextPath}/resources/assets/flot/jquery.flot.tooltip.min.js"></script>
	<script
		src="${pageContext.request.contextPath}/resources/assets/sparkline/jquery.sparkline.min.js"></script>


	<!--page specific plugin scripts-->
	<script type="text/javascript"
		src="${pageContext.request.contextPath}/resources/assets/jquery-validation/dist/jquery.validate.min.js"></script>
	<script type="text/javascript"
		src="${pageContext.request.contextPath}/resources/assets/jquery-validation/dist/additional-methods.min.js"></script>





	<!--flaty scripts-->
	<script src="${pageContext.request.contextPath}/resources/js/flaty.js"></script>
	<script
		src="${pageContext.request.contextPath}/resources/js/flaty-demo-codes.js"></script>
	<!--page specific plugin scripts-->
	<script type="text/javascript"
		src="${pageContext.request.contextPath}/resources/assets/bootstrap-fileupload/bootstrap-fileupload.min.js"></script>
	<script type="text/javascript"
		src="${pageContext.request.contextPath}/resources/assets/chosen-bootstrap/chosen.jquery.min.js"></script>
	<script type="text/javascript"
		src="${pageContext.request.contextPath}/resources/assets/clockface/js/clockface.js"></script>
	<script type="text/javascript"
		src="${pageContext.request.contextPath}/resources/assets/bootstrap-timepicker/js/bootstrap-timepicker.js"></script>
	<script type="text/javascript"
		src="${pageContext.request.contextPath}/resources/assets/bootstrap-colorpicker/js/bootstrap-colorpicker.js"></script>
	<script type="text/javascript"
		src="${pageContext.request.contextPath}/resources/assets/bootstrap-datepicker/js/bootstrap-datepicker.js"></script>
	<script type="text/javascript"
		src="${pageContext.request.contextPath}/resources/assets/bootstrap-daterangepicker/date.js"></script>
	<script type="text/javascript"
		src="${pageContext.request.contextPath}/resources/assets/bootstrap-daterangepicker/daterangepicker.js"></script>
</body>
</html>