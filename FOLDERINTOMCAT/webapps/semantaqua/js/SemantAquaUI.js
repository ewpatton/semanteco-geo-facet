var SemantAquaUI = {
	//store all reference to markers
	"markers": [],
	"markersByUri": {},

	//initializing the map the element such as infowindow and content containers in infowindow
	"configureMap": function() {
		var mapOptions = {
				center: new google.maps.LatLng(37.4419, -122.1419),
				zoom: 8,
				mapTypeId: google.maps.MapTypeId.ROADMAP,
				zoomControl: true,
				panControl: true
			};
		SemantAquaUI.map = new google.maps.Map(document.getElementById("map_canvas"), mapOptions);
		SemantAquaUI.geocoder = new google.maps.Geocoder();

 
		SemantAquaUI.infowindowcontainer = document.createElement('div');
        $(SemantAquaUI.infowindowcontainer).attr("id","infowindowcontainer");
        SemantAquaUI.infowindowcontent = document.createElement('div');
        $(SemantAquaUI.infowindowcontent).attr("id","infowindowcontent");
        $(SemantAquaUI.infowindowcontent).appendTo(SemantAquaUI.infowindowcontainer);
        SemantAquaUI.infowindowcontrol = document.createElement('div');
        $(SemantAquaUI.infowindowcontrol).attr("id","infowindowcontrol");
        $(SemantAquaUI.infowindowcontrol).appendTo(SemantAquaUI.infowindowcontainer);
		SemantAquaUI.infowindow=new google.maps.InfoWindow({
            content:SemantAquaUI.infowindowcontainer
        });

		//the function below is listening on show-marker-info event,
		//more than just an event, the trigger also passed a parameter(marker) when it trigger the event
		//this is a really good jquery function
        $(window).on("show-marker-info",function(event,marker){

        	//copy user-data from marker to infowindow itself
        	//the user-data on marker of also get from somewhere else
        	SemantAquaUI.infowindow.data=marker.data;
        	console.log(marker.data);
            SemantAquaUI.infowindow.open(SemantAquaUI.map,marker);
            //all container in infowindow is cleared and new data is being put into them everytime a marker is clicked
            $(SemantAquaUI.infowindowcontrol).html("");
            $(SemantAquaUI.infowindowcontrol).html("<a>Data</a><a>Visualize 1</a><a>Visualize 2</a>");
            $(SemantAquaUI.infowindowcontent).html(SemantAquaUI.infowindow.data.infowindowcontent);
            $($("#infowindowcontrol a").get(0)).click(function(){
            	$(SemantAquaUI.infowindowcontent).html(SemantAquaUI.infowindow.data.infowindowcontent);
            });

            $($("#infowindowcontrol a").get(1)).click(function(){
            	$(SemantAquaUI.infowindowcontent).html("");
	            var plot1 = $.jqplot('infowindowcontent', [SemantAquaUI.infowindow.data.visualize1], {
	                title:SemantAquaUI.infowindow.data.label.value,
	                axes:{
	                    xaxis:{
	                        renderer:$.jqplot.DateAxisRenderer,
	                        tickOptions:{
	                            formatString:'%b&nbsp;%#d'
	                        } 
	                    },
	                    yaxis:{
	                        tickOptions:{
	                            formatString:'%.5f'
	                        }
	                    }
	                },
	                series:[{label:"carbonMonoxide",lineWidth:4}],
	                highlighter: {
	                    show: true,
	                    sizeAdjust: 7.5
	                },
	                legend: { 
	                	show:true, 
	                	location: 'se'
	                },
	                cursor: {
	                    show: false
	                }
	            });
            });

			$($("#infowindowcontrol a").get(2)).click(function(){
            	$(SemantAquaUI.infowindowcontent).html("");
	            var plot1 = $.jqplot('infowindowcontent', [SemantAquaUI.infowindow.data.visualize1,SemantAquaUI.infowindow.data.visualize2], {
	                title:SemantAquaUI.infowindow.data.label.value,
	                axes:{
	                    xaxis:{
	                        renderer:$.jqplot.DateAxisRenderer,
	                        tickOptions:{
	                            formatString:'%b&nbsp;%#d'
	                        } 
	                    },
	                    yaxis:{
	                        tickOptions:{
	                            formatString:'%.5f'
	                        },
	                    },
	                    y2axis:{
			                autoscale:true, 
			                tickOptions:{showGridline:false}
			            }
	                },
	                legend: { 
	                	show:true, 
	                	location: 'se'
	                },
	                series:[{label:"carbonMonoxide",yaxis:'yaxis',lineWidth:4}, {label:"Aves",yaxis:'y2axis'}],
	                highlighter: {
	                    show: true,
	                    sizeAdjust: 7.5
	                },
	                cursor: {
	                    show: false
	                }
	            });
            });
            
        })
	},
	//decode zip code and center the map
	"doGeocode": function(zip) {
		console.trace();
		if(SemantAquaUI.geocoder) {
			SemantAquaUI.geocoder.geocode({"address":zip, "region":"US"},
				function(pt) {
					console.log(pt);
					if(!pt) {
						alert(zip + " not found");
					}
					else {
						SemantAquaUI.map.setCenter(pt[0].geometry.location);
						SemantAquaUI.map.setZoom(10);
					}
				});
		}
		else {
			console.log("SemantAqua.geocoder is null");
		}
	},
	"getState": function() {
		return $.extend({}, $.bbq.getState(), SemantAquaUI.getFacetParams());
	},

	//these two function below is controlling facet bar on the side by intercting with the bbq
	"getFacetParams": function() {
		var params = {};
		$("div#facets .facet").each(function() {
			var that = $(this);
			$("input", that).each(function() {
				var type = this.getAttribute("type").toLowerCase();
				var name = this.getAttribute("name");
				if(type == "checkbox") {
					if(typeof params[name] == "undefined") {
						params[name] = [];
					}
					if(this.checked) {
						params[name].push(this.value);
					}
				}
				else if(type == "radio") {
					if(this.checked) {
						params[name] = this.value;
					}
				}
				else if(type == "text") {
					params[name] = this.value;
				}
				else {
					console.warn("Facet "+that.getAttribute("id")+
							" uses input type "+type+
							" which is not supported.");
				}
			});
			$("select", that).each(function() {
				var name = this.getAttribute("name");
				params[name] = this.value;
			});
		});
		return params;
	},

	"populateFacets": function() {
		var params = $.bbq.getState();
		for(var param in params) {
			var inputs = $("*[name="+param+"]");
			if(inputs.length > 0) {
				if(inputs[0].tagName == "INPUT") {
					var type = inputs[0].type.toLowerCase();
					if(type == "checkbox") {
						for(var i=0;i<inputs.length;i++) {
							if($.inArray($(inputs[i]).val(), params[param])>=0) {
								console.debug("Setting "+$(inputs[i]).val()+" to true")
								inputs[i].checked=true;
							}
							else {
								console.debug("Setting "+$(inputs[i]).val()+" to false")
								inputs[i].checked=false;
							}
						}
					}
					else if(type == "radio") {
						
					}
					else if(type == "text") {
						console.debug("Setting "+$(inputs[i]).attr("name")+" to "+params[param]);
						inputs[0].value = params[param];
					}
				}
				else {
					console.debug("Setting "+$(inputs[0]).attr("name")+" to "+params[param]);
					inputs[0].value = params[param];
				}
			}
		}
	},

	//the function that reallly talk to google maps api and get a marker ojbect reference
	//however, the marker is not on the map yet
	"createMarker": function(uri, lat, lng, icon, visible, label) {
		var opts = {"clickable": true,
				"icon": new google.maps.MarkerImage(icon, null, null, null, new google.maps.Size(30, 34)),
				"title": label,
				"position": new google.maps.LatLng(lat, lng),
				"visible": visible
				};
		return new google.maps.Marker(opts);
	},
	//add markers to the map that is created in the configure function above
	"addMarker": function(marker) {
		var uri = marker.data["site"].value;
		SemantAquaUI.markers.push(marker);
		SemantAquaUI.markersByUri[uri] = marker;
		marker.setMap(SemantAquaUI.map);

		google.maps.event.addListener(marker, "click",function() {
			SemantAqua.action = SemantAquaUI.handleClickedMarker;
			$.bbq.pushState({"uri": uri});
			//trigger the show-marker-info event and pass the coresponding marker to tell where the infowindow should show
			$(window).trigger('show-marker-info',marker);  
		});
	},

	//simply return all referece of markers stored in the semantAqua object itself.
	"getMarkers": function() {
		return SemantAquaUI.markers;
	},
	"getMarkerForUri": function(uri) {
		return SemantAquaUI.markersByUri[uri];
	},

	//
	"handleClickedMarker": function() {
		SemantAqua.action = null;
		$(window).trigger('show-marker-info');
	},
	"showSpinner": function() {
		$("#spinner").css("display", "block");
	},
	"hideSpinner": function() {
		$("#spinner").css("display", "none");
		$.bbq.removeState("action");
	}
};




