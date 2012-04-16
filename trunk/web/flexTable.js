function FlexTable(element, authorFn, addButtonFn, storage, backFn, nextFn, onModifyFn, level2, author2Fn, add2ButtonFn) {

	this.activate = activate;
	
	this.removeMatching = function(matchFn, match2Fn) {
		var removed = false,
			items2;
		for (var i = 0; i < items.length; i++)
			if (matchFn(items[i])) {
				items.splice(i, 1);
				element.children("table").children("tbody").children().slice(i * (1 + !!level2), (i + 1) * (1 + !!level2)).remove();
				i--;
				removed = true;
			} else if (level2 && match2Fn) {
				items2 = items[i][level2];
				for (var j = 0; j < items2.length; j++)
					if (match2Fn(items2[j])) {
						items2.splice(j, 1);
						element.children("table").children("tbody").children().eq(i*2+1)
							.children().children("table").children("tbody").children().eq(j).remove();
						j--;
						removed = true;
					}
				}
		if (removed)
			refresh();
	}
	
	this.update = function(newItems) {
		items = newItems;
		var table = element.children("table").show();
		table.children("tbody").children().remove();
		$.each(items, function(i, item) {
			display(trTemplates.display, item).appendTo(table);
		});
		refresh();
	}
	
	var items, // array of objects representing flexTable content
		trTemplates = {}, // tr DOM element templates
		idx = -1, // index of checked item in items
		idx2 = -1, // index of checked level2 item in items[idx][level2]
		checkedTr, // tr DOM element containing checked radio 
		isEdit, // true if existing item is edited, false if new item is created
		saveNewFn;
		
	element.addClass("flexTable");
	element.children("table").hide();
	templates();
	buttonClicks();
	activate(true, true); // deactivate, no animation

	function templates() {
		trTemplates.edit = removeTr("tr.edit");
		trTemplates.level2 = removeTr("tr.level2");
		trTemplates.display = removeTr(":last");
		trTemplates.edit2 = removeTr("tr.edit", true);
		trTemplates.display2 = removeTr(":last", true);
	}

	function removeTr(trSelector, l2) {
		return (l2 ? trTemplates.level2.children() : element).children("table").children("tbody").children(trSelector).remove();
	}

	function buttonClicks() {
		element.children("input.add").click(add);
		element.children("input.add2").click(add2);
		element.children("input.edit").click(edit);
		element.children("input.delete").click(remove);
		element.children("input.back").click(back).prop("disabled", false); // disabled: firefox bug fix
		element.children("input.next").click(next).prop("disabled", false); // disabled: firefox bug fix
	}

	// store, check first, set buttons
	function refresh() {
		store();
		if (items.length) {
			checkedTr = element.find("input:radio").first().prop("checked", true).parent().parent();
			idx = 0;
			idx2 = -1;
		}
		else
			element.children("table").hide();
		setButtons();
	}
	
	function activate(deactivate, notAnimate) {
		element.toggleClass("active", !deactivate);
		var inputs = element.find("input");
		if (notAnimate)
			inputs.toggle(!deactivate);
		else if (deactivate)
			inputs.slideUp();
		else
			inputs.slideDown();
	}

	function add() { 
		isEdit = false; 
		styleEdit(author({}).appendTo(element.children("table").show()));
	}

	function add2() {
		isEdit = false;
		styleEdit(author2({}).appendTo((idx2 >= 0 ? 
			checkedTr.parent().parent() : checkedTr.next().children().children("table")).show()));
	}

	function edit() {
		isEdit = true;
		var tr = idx2 >= 0 ?
			author2(items[idx][level2][idx2]) : 
			author(items[idx]);
		tr.insertAfter(checkedTr);
		styleEdit(tr);
	}

	function styleEdit(tr, isEnd) {
		tr.parent().siblings("thead").add(isEdit && checkedTr).toggleClass("editData", !isEnd); // table header row
		$("#overlay").toggle(!isEnd);
	}

	function author(item) {
		return authorX(trTemplates.edit, item, authorFn, save);
	}

	function author2(item2) {
		return authorX(trTemplates.edit2, item2, author2Fn, save2);
	}

    function authorX(editTrTemplate, item, authorXFn, saveFn) {
		var tr = editTrTemplate.clone();
		authorXFn(tr, item, items[idx]);
		tr.find("input.save").click(saveFn).prop("disabled", false); // disabled: firefox bug fix
		tr.find("input.cancel").click(cancel).prop("disabled", false); // disabled: firefox bug fix
		$.each(item, function(name, value) { 
			tr.find("select[name='" + name + "']").val(value).change();
		});
		return tr;
	}

	function cancel() {
		var tr = $(this).parent().parent();
		styleEdit(tr, true);
		hideRemove(tr);
	}

	function hideRemove(tr) { 
		if (!tr.siblings().size()) 
			tr.closest("table").hide();
		tr.remove();
	}

	function save() {
		saveX(this, items, idx, trTemplates.display, saveNew, onModifyFn);
	}

	function save2() {
		saveX(this, items[idx][level2], idx2, trTemplates.display2, saveNew2);
	}

	function saveX(button, itemsX, idxX, displayTrTemplate, newFn, onModifyXFn) {
		var tr = $(button).parent().parent(),
			item = tr.formParams();
		if (isEdit) {
			if (onModifyXFn)
				onModifyXFn(itemsX[idxX], item);
			$.each(item, function(name, value) { // copy values from form to mapping/attribute
				itemsX[idxX][name] = value;
			});
			tr.prev().remove(); // remove old row
		}
		else {
			itemsX.push(item);
			newFn(item, itemsX);
			setButtons();
		}
		store();
		checkedTr = display(displayTrTemplate, item).replaceAll(tr).first();
		styleEdit(checkedTr, true);
	}

	function saveNew(item) {
		if (level2)
			item[level2] = [];
		idx = items.length - 1;
		idx2 = -1;
	}

	function saveNew2(item2, items2) {
		idx2 = items2.length - 1;
	}

	function remove() { 
		if (confirm("This will delete currently checked row. Are you sure?")) {
			if (idx2 >= 0) {
				items[idx][level2].splice(idx2, 1); // remove attribute
				var temp = checkedTr.parent()
					.closest("tr")
					.prev()
					.find("input:radio")
					.prop("checked", true) // check mapping
					.end(); // back to result of 'prev()' 
				hideRemove(checkedTr); // remove checked row
				checkedTr = temp;
				idx2 = -1;
				element.children("input.add2").prop("disabled", false);
				store();
			}
			else {
				if (onModifyFn)
					onModifyFn(items[idx]);
				items.splice(idx, 1); // remove mapping
				if (level2)
					checkedTr.next().remove(); // remove level2 row
				// hideRemove(checkedTr); // remove checked row
				checkedTr.remove();
				refresh();
			}
		}
	}

	function display(trTemplate, item) { 
		var result = trTemplate.clone(),
			td = result.children().first(); // first td
		td.children("input").change(radioChange).prop("checked", true);
		$.each(item, function(name, value) { 
			if (name != level2)
				td = td.next().html(value); // write value to next sibling (td)
			else {
				result = result.add(trTemplates.level2.clone());
				var table2 = result.last().children().children("table");
				if (!value.length)
					table2.hide();
				$.each(value, function(i, item2) { 
					display(trTemplates.display2, item2).appendTo(table2);
				});
			}
		});
		return result;
	}

	function radioChange() { 
		checkedTr = $(this).parent().parent();
		var parentTr = checkedTr.parent().closest("tr").prev();
		idx = (parentTr.length ? parentTr : checkedTr).index() / (1 + !!level2);
		// idx = indexOf(items, "table", (parentTr.length ? parentTr : checkedTr).children().eq(1).html());
		idx2 = parentTr.length ? checkedTr.index() : -1;
			// indexOf(items[idx][level2], "column", checkedTr.children().eq(1).html()) :
		setButtons();
	}

	// function checkFirst() { 
		// checkedTr = element.find("input:radio").first().prop("checked", true).parent().parent();
		// idx = 0;
		// idx2 = -1;
	// }

	function setButtons() { 
		element.children("input.edit, input.delete").prop("disabled", !items.length);
		element.children("input.add").prop("disabled", addButtonFn && addButtonFn());
		element.children("input.add2").prop("disabled", add2ButtonFn && add2ButtonFn(items[idx]));
	}

	function store() { 
		localStorage.setObject(storage, items);
	}

	function back() { 
		backFn && backFn() && activate(true);
	}

	function next() { 
		nextFn && nextFn() && activate(true);
	}

}