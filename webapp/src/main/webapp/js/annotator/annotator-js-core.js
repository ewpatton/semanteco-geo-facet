// Brendan Edit: Organize all the code

// Keeps track of the ID's of the bundles, hopefully for use
//    in the future to show off all bundles at once.
var bID = 0;

// This array is for keeping track of the column indices of all selected columns.
//   It is empty at creation. The callbacks in the column selector function should
//   handle adding and removing indices from this array.
// Ideally, context menu functions that require knowledge of all selected columns
//   will also reference this array.
// * Note that lists of columns designated for links_via or cell-based conversion
//   are kept separately!
var currentlySelected = [];

// This array is for keeping track of column indices of those columns designated
//   for conversion using links_via. 
// It should be empty at creation, and the callback for "toggle-links_via" should
//   be the only function that can modify this array.
var links_via = [];

// Like the above, the indices of columns designated for cell-based conversion 
//    are kept in an array, as well, which is empty at creation. The callback for
//    "toggle-cell-based" adds to and removes from this array;
var cellBased = [];

// This arrayis for keeping track of bundles. Unlike the above, this array will 
//    contain OBJECTS, with each one describing one bundle. The bundle objects are
//    defined in the bundle function above; details about each part of the bundle
//    are elaborated upon there.
var bundles = [];

// Generate the subtables for each column header in the CSV file.
//    The handleFileSelect function below that generates the table calls this repeatedly.
// Every subtable will be the same, (except for the ID's of the cells)
//    with 3 rows in 1 column. These are the DEFAULT subtables; ones for
//    IMPLICIT or EXPLICIT BUNDLES will be different (and in other functions) 
// Input parameters are:
// - text: presumably the header for the column from the original CSV file,
//	   to be placed in the first row of the column.
// - colIndex: the position of that cell in the table. Subrows are identified
//	   as "nameRow,"+colIndex, "propertyRow,"+colIndex, and "classRow,"+colIndex
//	   these last two rows are classed as droppable targets.
function createSubtable(text, colIndex) {
    var theader = '<table class="marginOverride">\n';
    var tbody = '';
    tbody += '<tr><td id=nameRow,' + colIndex + '><p class="ellipses marginOverride">' + text + '</p></td></tr>\n';
    tbody += '<tr><td style="color:red" class="droppable-prop" id=propertyRow,' + colIndex + '><p class="ellipses marginOverride">[property]</p></td></tr>\n';
    tbody += '<tr><td style="color:red" class="droppable-class" id=classRow,' + colIndex + '><p class="ellipses marginOverride">[class]</p></td></tr>\n';
    var tfooter = '</table>';
    var subtable = theader + tbody + tfooter;
    return subtable;
}

// This creates a subtable for describing a bundle
//  - One row is a dropdown menu that allows the user to select which column
//    is the resource representing the bundle, with the default option for "implicit"
//  - Ideally, selecting a column for the resource should copy the column header of the
//    original column. 
// Takes two arguments: 
//    - startIndex, the index of the first column in the bundle
//    - bundleSpan, the number of columns the bundle spans
// * NOTE that this method currently assumes bundled columns are consecutive and adjacent!
// TO DO: generate the dropdown menu! Probably in another function!
function createBundleSubtable(theBundle) {
    var id = theBundle.bundleID;
    var theader = '<table id=bundle,' + id + '>\n';
    var tbody = '';
    // Brendan Edit: generate options based off of available headers
    // I realize this is a nightmare jquery statement, but I had to traverse the DOM somehow...
    var validHeadersToBundle = $("th.not-bundled").not(".hide-while-empty").not(".ui-selected").filter(function () {
        return !($($(this).children()[0].children[0].children[0].children[0].children[0]).hasClass("cellBased-on"));
    });
    // remove selected from possible dropdown
    var generatedOptions = "";
    validHeadersToBundle.each(function (index) {
        // Keeping this for archiving, but it should be deprecated now that we have a ui-selected class
        //if ($.inArray($(this).attr('id').split(",")[1], theBundle.bundleCols) == -1) {
        generatedOptions += "<option>Column " + $(this).attr('id').split(",")[1] + " (" + "TODO" + ")</option>"
    });
    tbody += '<tr><td id=bundleResource,' + id + '><form style="background:white" action=""><select style="width:100%" name="uri"><option value="">implicit</option>' + generatedOptions + '</select></form></td></tr>\n';
    tbody += '<tr><td id=bundleName,' + id + '>[name template]</td></tr>\n';
    tbody += '<tr><td style="color:red" class="droppable-prop" id=bundlePropRow,' + id + '>[property]</td></tr>\n';
    tbody += '<tr><td style="color:red" class="droppable-class" id=bundleClassRow,' + id + '>[class]</td></tr>\n';
    var tfooter = '</table>';
    var subtable = theader + tbody + tfooter;
    return subtable;
}

// ******************************************************************************
//                   context menu and its callback functions!
// ******************************************************************************

// Creates and enables the context menu on the column headers!
// Each function is placed under "items", and should have a NAME and a CALLBACK.
// If a callback is not specified, then it will utilize the default callback.
$(function () {
    $.contextMenu({
        selector: '.the-context-menu',
        // This is the default callback, which will be used for any functions
        //    that do not have their own callbacks specified. It echoes the 
        //    key of the selection and the column on which the menu was invoked
        //    to the console.
        callback: function (key, options) {
            var index = $("th").index(this);
            var m = "clicked: " + key + ", invoked on col: " + index;
            console.log(m);
        },
        // Each of these is one item in the context menu list.
        // Documentation at http://medialize.github.io/jQuery-contextMenu/docs.html
        items: {
            "toggle-cell-based": {
                // Modifies the cellBased array to add or remove column indices.
                // If a selected column is in the array, then it will be removed (toggled OFF)
                // If the column is not in the array, then it will be added (toggled ON)
                // * NOTE that if this is called when only one column is selected,
                //   currently it will toggle the column on which the menu was invoked,
                //   NOT the selected column.
                name: "Toggle 'cell-based'",
                callback: function () {
                    // if one or fewer columns are selected, add/remove the column on which the menu was invoked
                    if (currentlySelected.length <= 1) {
                        var index = $("th").index(this);
                        var toggle = document.getElementById("nameRow," + index);
                        // if the column is already there, remove it
                        if (existsA(cellBased, index)) {
                            $(toggle).removeClass("cellBased-on");
                            removeA(cellBased, index);
                            console.log("removing col " + index);
                        }
                        // if the column is not there, add it
                        else {
                            $(toggle).addClass("cellBased-on");
                            //$(toggle).attr("class","cellBased-on");
                            cellBased.push(index);
                            console.log("adding col " + index);
                        }
                    } // /if
                    // if more than one column is selected, perform the toggle on ALL selected columns
                    else {
                        for (i in currentlySelected) {
                            var toggle = document.getElementById("nameRow," + currentlySelected[i]);
                            // if the column is already there, remove it
                            if (existsA(cellBased, currentlySelected[i])) {
                                $(toggle).removeClass("cellBased-on");
                                removeA(cellBased, currentlySelected[i]);
                                console.log("removing col " + currentlySelected[i]);
                            }
                            // if the column is not there, add it
                            else {
                                $(toggle).addClass("cellBased-on");
                                //$(toggle).attr("class","cellBased-on");
                                cellBased.push(currentlySelected[i]);
                                console.log("adding col " + currentlySelected[i]);
                            }
                        } // /for	
                    }
                    console.log("currently specified for cell-based: " + cellBased);
                } // /cell-based callback
            }, // /cell-based

            "toggle-links_via": {
                // Modifies the links_via array to add or remove column indices.
                // If a selected column is in the array, then it will be removed (toggled OFF)
                // If the column is not in the array, then it will be added (toggled ON)
                // * NOTE that if this is called when only one column is selected,
                //   currently it will toggle the column on which the menu was invoked,
                //   NOT the selected column.
                name: "Toggle 'links_via'",
                callback: function () {
                    // if one or fewer columns are selected, add/remove the column on which the menu was invoked
                    if (currentlySelected.length <= 1) {
                        var index = $("th").index(this);
                        var toggle = document.getElementById("nameRow," + index);
                        // if the column is already there, remove it
                        if (existsA(links_via, index)) {
                            $(toggle).removeClass("links_via-on");
                            removeA(links_via, index);
                            console.log("removing col " + index);
                        }
                        // if the column is not there, add it
                        else {
                            $(toggle).addClass("links_via-on");
                            //$(toggle).attr("class","links_via-on");
                            links_via.push(index);
                            console.log("adding col " + index);
                        }
                    } // /if
                    // if more than one column is selected, perform the toggle on ALL selected columns
                    else {
                        for (i in currentlySelected) {
                            var toggle = document.getElementById("nameRow," + currentlySelected[i]);
                            // if the column is already there, remove it
                            if (existsA(links_via, currentlySelected[i])) {
                                $(toggle).removeClass("links_via-on");
                                removeA(links_via, currentlySelected[i]);
                                console.log("removing col " + currentlySelected[i]);
                            }
                            // if the column is not there, add it
                            else {
                                $(toggle).addClass("links_via-on");
                                //$(toggle).attr("class","links_via-on");
                                links_via.push(currentlySelected[i]);
                                console.log("adding col " + currentlySelected[i]);
                            }
                        } // /for	
                    } // /else
                    console.log("currently specified for links_via: " + links_via);
                } // /links_via callback
            }, // /links_via

            "bundle": {
                name: "Create Bundle",
                callback: function () {
                	console.log(currentlySelected);

                    // Maintain if the selection has a gap or not
                    var gap = false;

                    // First, let's get a reference to all DOM items that were selected
                    var selectedHeaders = [];
                    $.each(currentlySelected, function(index, value) {
                        // Detect selection gaps
                        if( index != 0 && Math.abs(value - currentlySelected[index - 1]) != 1 ) {
                            gap = true;
                        }
                        selectedHeaders.push($("th#0\\," + value));
                    });

                    console.log("GAP:", gap);

                    // Second, let's determine which columns have children below them that need to be pushed down before the bundle is created and push them down
                    $.each(selectedHeaders, function(index, value) { 
                        var selectedID = value.attr("id").split(",")[1];
                        if ($("#bundledRow\\," + selectedID).children().length > 0) {
                            // Brendan edit: expose the extended-bundles row (yuck)
                            if (!$("#bundles-extended").is(":visible")) {
                                $("#bundles-extended").removeClass("hide-while-empty");
                            }
                            // Move the item down
                            $("#bundledRow\\," + selectedID).children(":first").appendTo("td#bundledRow-extended\\," + selectedID);
                        }
                    });

                    // Next, let's make the bundle, and push it to the local array of bundles (kaite code)
                    var newBundle = new bundle(bID, currentlySelected);
                    bID++;
                    bundles.push(newBundle);
                    console.log("created bundle #" + newBundle.bundleID + ", which contains columns " + newBundle.bundleCols);
                    
                    // Now we can push down the items being bundled, and insert a new header table where the old one was
                    $.each(selectedHeaders, function(index, value) { 
                        var selectedID = value.attr("id").split(",")[1];
                        //Expose the bundles row
                        if (!$("#bundles").is(":visible")) {
                            $("#bundles").removeClass("hide-while-empty");
                        }
                        // Move the item down
                        value.children(":first").appendTo("td#bundledRow\\," + selectedID);
                        // Insert new header table
                        value.append("<div>" + createBundleSubtable(newBundle) + "</div>");
                        // TODO: handle gaps and colspan here
                        // TODO: Disable the forms here .attr("disabled", "disabled");
                    });
                }
            },

            "comment": {
                // Adds a comment to the Annotation Row
                // * when finished, this should pop up a lightbox to solicit user input
                //   including the type of comment (radio selector?) as well as the
                //   comment text itself.
                // If there is already a comment/the row is already showing, then just add it.
                // Otherwise, show the row, then add the comment to the column on which the context
                //    menu was called.
                // * NOTE that if multiple columns are selected, it will only add the comment to the 
                //   single column on which the menu is invoked!
                name: "Add Comment",
                callback: function () {
                	 $("#commentModal").dialog({
                        modal: true,
                        width: 800,
                        buttons: {
                            Ok: function () {
                                $(this).dialog("close");
                            }
                        }
                    });
                    /*
                    var cRow = document.getElementById("annotations");
                    var index = $("th").index(this);
                    var cType = "rdfs:comment"; // in the end, these two fields 
                    var cText = "testComment"; //  shouldn't be hard-coded....
                    // if the row is hidden (ie, this is the first comment added), show the row
                    if (cRow.classList.contains("hide-while-empty")) {
                        $(cRow).removeClass("hide-while-empty");
                    }
                    var workingCol = document.getElementById("annotationRow," + index);
                    var workingTable = workingCol.getElementsByTagName('TABLE')[0];
                    var addedRow = workingTable.insertRow(-1);
                    var typeCell = addedRow.insertCell(0);
                    var textCell = addedRow.insertCell(1);
                    typeCell.innerHTML = cType;
                    textCell.innerHTML = cText;
                    */
                } // /callback function
            }, // /addComment

            "edit Domain Template": {
                // Adds a comment to the Annotation Row
                // * when finished, this should pop up a lightbox to solicit user input
                //   including the type of comment (radio selector?) as well as the
                //   comment text itself.
                // If there is already a comment/the row is already showing, then just add it.
                // Otherwise, show the row, then add the comment to the column on which the context
                //    menu was called.
                // * NOTE that if multiple columns are selected, it will only add the comment to the 
                //   single column on which the menu is invoked!
                name: "Edit Domain Template",
                callback: function () {
                    // Show modal
                    $("#domainTemplateModal").dialog({
                        modal: true,
                        width: 800,
                        buttons: {
                            Ok: function () {
                                $(this).dialog("close");
                            }
                        }
                    });
                }
            },

            "add-canonical-value": {
                name: "Add Canonical Value",
                // Like addComment, this allows a user to add a canonical value
                //    using our conversion:eg. As the above, "egText" should eventually
                //    be user-specified.
                // Canonical Values hang out in the annotation row along with comments and
                //    other annotations.
                // * NOTE that, like the above, thsi will only add an eg to the column on which
                //   the context menu was invoked, even if multiple columns are selected!
                callback: function () {
                	 $("#canonicalModal").dialog({
                        modal: true,
                        width: 800,
                        buttons: {
                            Ok: function () {
                                $(this).dialog("close");
                            }
                        }
                    });
                    /*
                    var egRow = document.getElementById("annotations");
                    var index = $("th").index(this);
                    var egType = "conversion:eg"; // in the end, these two fields 
                    var egText = "test_eg"; //  shouldn't be hard-coded....
                    // if the row is hidden (ie, this is the first comment added), show the row
                    if (egRow.classList.contains("hide-while-empty")) {
                        $(egRow).removeClass("hide-while-empty");
                    }
                    var workingCol = document.getElementById("annotationRow," + index);
                    var workingTable = workingCol.getElementsByTagName('TABLE')[0];
                    var addedRow = workingTable.insertRow(-1);
                    var typeCell = addedRow.insertCell(0);
                    var textCell = addedRow.insertCell(1);
                    typeCell.innerHTML = egType;
                    textCell.innerHTML = egText;
                    */
                } // /callback function

            }, // /eg

            "add-subject-annotation": {
                name: "Add Subject Annotation",
                // Subject Annotation addes new triples. Forced triples is what we like to call it.
                callback: function () {
                    $("#subjectAnnotationModal").dialog({
                        modal: true,
                        width: 800,
                        buttons: {
                            Ok: function () {
                                $(this).dialog("close");
                            }
                        }
                    });
                }
            },
        } // /items
    }); // /context menu
}); // /context menu function


// Arguments for Drag and Drop for class facets ( this applies to the jstree library. see: jstree.com)
var dnd_classes = {
    "drop_target": "#list th, #list th tr, #list th tr td, #list th tr td p",
    "drop_finish": function (data) {
        var targetID;
        if (data.r.hasClass("column-header") && data.r.is("th") && data.r.attr("id") != undefined) {
            targetID = data.r.attr("id").split(",")[1];
        } else {
            // get the header of this element
            var parentHeader = data.r.parents("th:eq(0)");
            if (parentHeader != undefined && parentHeader.attr("id") != undefined) {
               targetID = parentHeader.attr("id").split(",")[1];
            }
        }
        // Set the value now that we have done some validation (some...)
        $("[id='classRow," + targetID + "']").empty().append("<p class=\"ellipses marginOverride\" style=\"color:black\">" + $.trim($(data.o).text()) + "</p>");
    }
};

// Arguments for Drag and Drop for property facets ( this applies to the jstree library. see: jstree.com)
var dnd_properties = {
    "drop_target": "#list th, #list th tr, #list th tr td, #list th tr td p",
    "drop_finish": function (data) {
        var targetID;
        if (data.r.hasClass("column-header") && data.r.is("th") && data.r.attr("id") != undefined) {
            targetID = data.r.attr("id").split(",")[1];
        } else {
            // get the header of this element
            var parentHeader = data.r.parents("th:eq(0)");
            if (parentHeader != undefined && parentHeader.attr("id") != undefined) {
               targetID = parentHeader.attr("id").split(",")[1];
            }
        }
        // Set the value now that we have done some validation (some...)
        $("[id='propertyRow," + targetID + "']").empty().append("<p class=\"ellipses marginOverride\" style=\"color:black\">" + $.trim($(data.o).text()) + "</p>");
    }
};


// Extract a string from the jsTree (this is silly, re-write code so this is not needed)
function toString(obj, level) {
    if (typeof (level) === 'undefined') level = 0;
    var ret = "";
    if (typeof obj == "object") {
        ret += "\n";
        // for (var j = 0; j < level; ++j) ret += " ";
        // ret += "\u007B\n"; // left curly brace
        for (i in obj)
            if (!$.isEmptyObject(obj[i])) {
                for (var j = 0; j < level; ++j) ret += " ";
                ret += toString(i) + ": " + toString(obj[i], level + 2) + "\n" + "\n";
            }
    } else {
        ret += obj;
    }
    return ret;
}

//  Extracting comments, putting them in the comment box for a facet jsTree
$(window).bind("rendered_tree.semanteco", function (e, div) {
    $(div).addClass("jstree-default");
    $(div).delegate("a", "click", function (event, data) {
        event.preventDefault();

        // TODO: this needs looked at badly.
        var a = $.jstree._focused().get_selected();
        if (a.length > 0) {
            var lookup = $("#ClassTree div.jstree").data("hierarchy.lookup");
            var comments = lookup[a.attr("hierarchy_id")].rawData.comment;
            $("#ClassBox").html("<p class=\"ellipses marginOverride\">" + toString(comments, 0) + "</p>");
        }   
    });
});

// =====================================================================
// ====================== On Document Ready Calls ======================
// =====================================================================

$(document).ready(function () {

    // Check on mouseenter if ellipses are being used, qtip if they are (works on dynamicly created qtips)
    $('body').on('mouseenter' ,'.ellipses', function(e) {
        if (this.offsetWidth < this.scrollWidth) {
            $(this).qtip({
                content: {
                    text: $(this).text()
                },
                overwrite: false, // Don't overwrite tooltips already bound
                show: {
                    event: e.type, // Use the same event type as above
                    ready: true // Show immediately - important!
                },
                position: {
                    my: 'bottom center',  // Position my top left...
                    at: 'top center', // at the bottom right of...
                    target: $(this) // my target
                }
            });
        } else {
            if (this.offsetWidth >= this.scrollWidth) {
                $(this).qtip('hide'); 
            }
        }
    });

    // Bind various click and form event listeners once the DOM is good to go
    $('#commit_enhancement').click(function () {
        $.bbq.pushState({
            "FileName": window.file_name,
            "Source": $("#source_info").val(),
            "DataSet": $("#dataset_info").val(),
            "annotationMappings": window.a
        });
        AnnotatorModule.queryForEnhancing({}, function (d) {
            console.log(d);
        });
    });

    // TODO: rewrite these in jquery syntax
    //document.getElementById('the_form').addEventListener('submit', handleFileSelect, false);
    document.getElementById('the_file').addEventListener('change', fileInfo, false);

    // Import File button shows modal for import
    $('#menu-import-file').click(function () {
        $("#fileDialogWrapper").dialog({
            modal: true,
            width: 800,
            buttons: {
                Import: function () {
                    handleFileSelect();
                    $(this).dialog("close");
                }
            }
        });
    });

    // Enable sortable facets
    $("div#facets").sortable({
        axis: "y",
        items: "table",
        handle: "th",
        placeholder: "ui-state-highlight"
    }).find("th").disableSelection();

    // getListofOntologies() call, builds the dropdown and then fills the facets
    AnnotatorModule.getListofOntologies({}, function (d) {
        d = $.parseJSON(d);
        if (d.length > 0) {
            var dropDown = $("<select></select>").attr("id", "checkboxDropDownOntologies").attr("name", "testCheckboxDropDown").attr("multiple", "multiple");
            for (var i = 0; i < d.length; i++) {
                dropDown.append("<option>" + d[i] + "</option>");
            }

            // Push our dropdown to the DOM
            $("#ontology-dropdown").prepend(dropDown);
            // Turn it into what we want!
            $("#checkboxDropDownOntologies").dropdownchecklist({
                emptyText: "Select an Ontology ...",
                width: 230,
                onComplete: function (selector) {
                    var values = "";
                    for (i = 0; i < selector.options.length; i++) {
                        if (selector.options[i].selected && (selector.options[i].value != "")) {
                            if (values != "") values += ";";
                            values += selector.options[i].value;
                        }
                    }
                    $.bbq.pushState({
                        "listOfOntologies": values.split(";")
                    });
                    // Re-query facets
                    $("#ClassTree").empty();
                    $("#PropertyTree").empty();
                    $("#dataPropertiesTree").empty();
                    $("#annotationPropertiesTree").empty();
                    $("#DataTypeTree").empty();

                    // Call patrice's new silly init call thingy ( :D )
                    AnnotatorModule.initOWLModel({}, function (d) {
                        SemantEcoUI.HierarchicalFacet.create("#ClassTree", AnnotatorModule, "queryClassHM", "classes", {
                            "dnd": dnd_classes,
                            "plugins": ["dnd"]
                        });
                        SemantEcoUI.HierarchicalFacet.create("#PropertyTree", AnnotatorModule, "queryObjPropertyHM", "objProperties", {
                            "dnd": dnd_properties,
                            "plugins": ["dnd"]
                        });
                        SemantEcoUI.HierarchicalFacet.create("#dataPropertiesTree", AnnotatorModule, "queryDataPropertyHM", "dataProperties", {
                            "dnd": dnd_properties,
                            "plugins": ["dnd"]
                        });
                        SemantEcoUI.HierarchicalFacet.create("#annotationPropertiesTree", AnnotatorModule, "queryAnnoPropertyHM", "annoProperties", {
                            "dnd": dnd_properties,
                            "plugins": ["dnd"]
                        });
                        SemantEcoUI.HierarchicalFacet.create("#DataTypeTree", AnnotatorModule, "queryDataTypesHM", "dataTypes", {
                            "dnd": dnd_classes,
                            "plugins": ["dnd"]
                        });
                    });
                }
            });
        }
    });
});

// =====================================================================
// ====================== ACCESSORY / MISC. FUNCTIONS ==================
// =====================================================================

// Accessory function for removing things from arrays
// Takes two arguments:
//  - the array
//  - the item to be removed
// And returns the array minus that object.
function removeA(arr) {
    var what, a = arguments,
        L = a.length,
        ax;
    while (L > 1 && arr.length) {
        what = a[--L];
        while ((ax = arr.indexOf(what)) !== -1) {
            arr.splice(ax, 1);
        }
    }
    return arr;
}

// Accessory function for checking to see if a thing is
//    in an array.
// Takes two arguments:
//  - the array
//  - the item to look for
// And returns TRUE if the object is there or FALSE if not
function existsA(theArray, theThing) {
    if (theArray.indexOf(theThing) === -1) {
        return false;
    } else
        return true;
}

// Accessory function for creating a bundle 
// Takes three arguments:
//  - id: a unique static ID for the bundle, different from
//    its index in the bundles array.
//  - columns: the array of column indices of the columns to
//    be placed in the bundle.
//  - resource: the index of the column that describes the
//    bundle, IF it is EXPLICIT. A value of -1 indicates the
//    bundle is IMPLICIT; this is the default set here at 
//    creation.
function bundle(id, columns) {
    this.bundleID = id;
    this.bundleCols = columns;
    this.bundleResource = -1;
}

// ==========================================
// = HERE LIES THE CODE ARCHIVE / GRAVEYARD =
// =           TRESSPASSERS BEWARE          =
// ==========================================


// Working a new way to generate the column headers....
/*
    function createSubtable(text, colIndex) { 
        var theTable = document.createElement('table');
        var nameRow = document.createElement('tr');
        var theName = document.createElement('td');
        
        var propRow = document.createElement('tr');
        var theProp = document.createElement('td');
        $(theProp).attr("class", "droppableProp");
        $(theProp).attr("id", "propertyRow,"+colIndex);
        
        var classRow = document.createElement('tr');
        var theClass = document.createElement('td');
        $(theClass).attr("class", "droppableClass");
        $(theClass).attr("id", "classRow,"+colIndex);
    }// /createSubtable */



// Tooltip function
// This just enables the mouseover tooltips.
//$(function () {
//    $(document).tooltip();
//});

// This is the jsTree for DATA TYPES
// To help clarify plugins:
//   "dnd" means "drag-and-drop"
//   "crrm" means "create, rename, remove and move" [nodes]
// They all have documentation on the jsTree main page

/*
    $(function() {
    $("#DataTypeTree").jstree({
        
        "themes" : {
                 "theme" : "apple",
                 "dots"  : true,
                 "icons" : true,
                 "url": "../../js/jstree/themes/apple/style.css"
            },// /themes
        
        "json_data": {
            "data": [
                {
                    "attr": { "id": "" },
                    "data": "[ Data Types ]",
                    "state": "open",
                    "children": [
                        {
                            "data": "xsd:string",
                            "attr": { "id": "1_datatype" }
                        },
                        {
                            "data": "xsd:boolean",
                            "attr": { "id": "2_datatype" }
                        },
                        {
                            "data": "xsd:decimal",
                            "attr": { "id": "3_datatype" }
                        },
                        {
                            "data": "xsd:float",
                            "attr": { "id": "4_datatype" }
                        },
                        {
                            "data": "xsd:double",
                            "attr": { "id": "5_datatype" }
                        },
                        {
                            "data": "xsd:integer",
                            "attr": { "id": "6_datatype" }
                        },
                        {
                            "data": "xsd:anyURI",
                            "attr": { "id": "7_datatype" }
                        }
                    ]
                }
            ]
        }, // /json_data
        
        // drag and drop for DATA TYPES 
        // From the documentation:
        //   data.o - the object being dragged
        //   data.r - the drop target
        // * The drop target will be the node in which the mouse button
        //   is released, NOT the node in which the object text will appear!
        "dnd" : {
            "drop_target" : "#list td",
            "drop_finish" : function (data) {
                $(data.r).css("color","black");
            //add the node name to the header, and get the parent node "id"
                // p = node id of the datathing from the JS tree
                var p = data.o.attr("id");
                // q = text in the datathing from the JS tree,
                //     essentially, the data type name itself
                var q = $(data.o).text();
                
                // i = id of the destination cell where the user drops the thing dragged from the JS tree
                var i = data.r.attr("id");
                // r = the text of that cell
                var r = $(data.r).text();
                
                // cn = column number
                var cn = i.split(',')[1];
                
                $("[id='classRow,"+cn+"']").text(q.replace(/^\s+|\s+$/g,''));
                
            // window.annotationMappings = {"RangeClass":[{"classname":"url"}], "Property":[{"propertyname": "url"}]};
                
            // var args = {};
            // args[r] = p;
           
                function keySearch(dict,key) {
                    for (var i in dict) if (key in dict[i]) 
                        return dict[i][key];
                    return null;                                                
                }
            // These are for the row-adding function, if there is no propertyRow or classRow
            // * This is currently not in use (???)
                var am = window.a
                var kg = keySearch(am,r);
                var args = {};
                args[r]  = {"DataType":p};
          
                if (kg!=null){                          
                    kg["DataType"] = p;                     
                }            
                else{
                    am.push(args);
                };
            }// /dropfinish function
        }, // /dnd
        
        "crrm" : {
            "move" : {
                "check_move" : function (data) {
                    if(data.r.attr("id") == "") {
                        
                    }
                }
            }
        },
        "plugins": ["themes", "json_data", "ui", "dnd", "crrm"]
        });// /jsTree
    });// /jsTree function
    */
// ** NOT CURRENTLY IN USE ** 
// ( drag-and-drop is currently handled via a jsTree plugin, NOT jQuery UI )
// Responsible for draggable and droppable functions for classes
//   and properties. They are separate from each other, to assist
//   with target highlighting.
/*
$(function () {
    // Draggable and Droppable for PROPERTIES
    $(".draggable-prop").draggable(
        {helper: function( event ) {
                    return $( "<div style=\"color:#aaa\">property!</div>");
                }// /helper
            }
    ); // /draggable-prop
    $(".droppable-prop").droppable({
        accept: ".draggable-prop",
        activeClass: "ui-state-hover",
        drop: function (event, ui) {
            $(this)
                .addClass("ui-state-highlight");
        } // /drop
    }); // /droppable-prop

    // Draggable and Droppable for CLASSES
    $(".draggable-class").draggable(
        {helper: function( event ) {
                    return $( "<div style=\"color:#aaa\">class!</div>");
                }// /helper
            }
    ); // /draggable-class
    $(".droppable-class").droppable({
        accept: ".draggable-class",
        activeClass: "ui-state-hover",
        drop: function (event, ui) {
            $(this)
                .addClass("ui-state-highlight");
        } // /drop function
    }); // /droppable-class
}); // /all draggable and droppable functions

// archive of robins old drag and drop code for js tree
var dnd = {

    "drop_target": "#list th",
    "drop_finish": function (data) {
        console.log("Drag: Finished | " + data);
        //add the node name to the header, and get the parent node "id"
        // p = node id of the datathing from the JS tree
        var p = data.o.attr("hierarchy_id");
        // q = text in the datathing from the JS tree,
        //     essentially, the data type name itself
        var q = $(data.o).text();
        // i = id of the destination cell where the user drops the thing dragged from the JS tree
        var i = data.r.attr("id");
        // r = the text of that cell
        var r = $(data.r).text();
        // cn = column number
        var cn = i.split(',')[1];

        $("[id='classRow," + cn + "']").empty().append("<p class=\"ellipses marginOverride\" style=\"color:black\">" + $.trim(q) + "</p>");
        //$("[id='classRow," + cn + "']").text(q.replace(/^\s+|\s+$/g, ''));

        //  window.annotationMappings = {"RangeClass":[{"classname":"url"}], "Property":[{"propertyname": "url"}]};

        //    var args = {};
        //    args[r] = p;

        function keySearch(dict, key) {
            for (var i in dict)
                if (key in dict[i])
                    return dict[i][key];
            return null;
        }

        var am = window.a
        var kg = keySearch(am, r);
        var args = {};
        args[r] = {
            "RangeClass": p
        };

        if (kg != null) {
            kg["RangeClass"] = p;
        } else {
            am.push(args);
        };
    } // /drop finish
};


// A context menu for selecting ontologies for the Class Hierarchy.
// Eventually, we'll want to populate this from some listing of all the ontologies we have?
// Right now, for sake of simplicity and testing, this is hard-coded and has no callbacks 
//    written....
$(function () {
    $.contextMenu({
        selector: '.ontology-selector',
        // This is the default callback, which will be used for any functions
        //    that do not have their own callbacks specified. It echoes the 
        //    key of the selection to the console.
        callback: function (key, options) {
            var m = "clicked: " + key;
            console.log(m);
        },
        items: {
            "semantEcoWater": {
                name: "SemantEco-Water",
                type: 'checkbox'
            },
            "prov": {
                name: "PROV",
                type: 'checkbox'
            },
            "wgs": {
                name: "WGS",
                type: 'checkbox'
            },
            "void": {
                name: "VOID",
                type: 'checkbox'
            },
        } // /items
    }); // /context menu

}); // /ontology-selector


*/