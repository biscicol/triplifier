function VocabularyManager(element, uploadElement, storage, alertErrorFn) {
	
	var lastVocabulary = 0, // used to assign ids to vocabulary DOM elements (needed for labels to work)
		vocabularies = {}, // hash of vocabularies downloaded in current session
		vocabularyTemplate = element.find("div.vocabulary").remove(), // used to create vocabulary DOM elements
		onChangeFn; // function called when current vocabulary is changed (or finishes loading)

	this.onChangeFn = function(fn) {
		if (fn)
			onChangeFn = fn;
		else if(onChangeFn)
			onChangeFn();
	}
	
	this.getSelectedVocabulary = function() {
		return vocabularies[element.find("div.vocabulary input:checked").val()];		
	}

	// get vocabularies available on server, ask about ones listed in localStorage, load first one
	$.ajax({
		url: "rest/getVocabularies",
		type: "POST",
		data: JSON.stringify(localStorage.getObject(storage) || []),
		contentType: "application/json; charset=utf-8",
		dataType: "json",
		success: populateVocabularies,
		error: alertErrorFn
		});

   	// assign event handlers
   	element.find("input.showUpload").click(showUpload);
   	uploadElement.find("input.hideUpload").click(hideUpload);
   	uploadElement.find("form.uploadLocal").submit(vocabularyUploadLocal);
   	uploadElement.find("form.loadUrl").submit(vocabularyLoadUrl);
   	
	function showUpload() {
		var overlay = $("#overlay");
		if (overlay.is(":visible"))
			overlay.css("z-index", 2);
		else
			overlay.css("z-index", 1).fadeIn();
		uploadElement.fadeIn();
	}
	
	function hideUpload() {
		var overlay = $("#overlay");
		if (overlay.css("z-index") < 2)
			overlay.fadeOut();
		overlay.css("z-index", 0);
		uploadElement.fadeOut();
	}
	
	function populateVocabularies(data) {
		if (data) {
			var elements = [];
			$.each(data, function(vocabName, displayName) { 
				elements.push(getVocabularyElement(vocabName, displayName).get(0));
			});
			$(elements).insertBefore(element.find("hr")).first()
				.children("input").prop("checked", true).change(); // load first vocabulary
		} else
		    alert("No Vocabularies available.");
	}
	
	function getVocabularyElement(vocabName, displayName) {
		lastVocabulary++;
		return vocabularyTemplate.clone().children("input").val(vocabName)
			.attr("id", "v" + lastVocabulary).change(getVocabulary).end()
			.children("label").attr("for", "v" + lastVocabulary).html(displayName).end();
	}
	
	function vocabularyUploadLocal() {
		return vocabularyLoad(this.file, "Please select a vocabulary file to upload.");
	}
	
	function vocabularyLoadUrl() {
		return vocabularyLoad(this.url, "Please enter a URL to load vocabulary from.");
	}
	
	function vocabularyLoad(vocabField, reqMessage) {
		if (!vocabField.value) {
			alert(reqMessage);
			vocabField.focus();
			return false;
		}
		uploadElement.css("z-index", 1); // put upload window behind overlay
		$("#status").html("Loading vocabulary:</br>'" + vocabField.value + "'").fadeIn(); // display status
		$("#uploadTarget").one("load", afterLoad);
		return true;
	}
	
	function afterLoad() {
		$("#status").hide(); // hide status
		uploadElement.css("z-index", 3); // put upload window in front
		var data = frames.uploadTarget.document.body.textContent;
		// distinguish response OK status by JSON format
		if (isJson(data)) {
			var vocabulary = JSON.parse(data),
				vocabularyStorage = localStorage.getObject(storage) || [];
			// TODO: handle empty vocabulary
//			if (!vocabulary.classes || !vocabulary.classes.length && !vocabulary.properties || !vocabulary.properties.length)
			vocabularyStorage.push(vocabulary.name);
			localStorage.setObject(storage, vocabularyStorage);
			vocabularies[vocabulary.name] = vocabulary;
			getVocabularyElement(vocabulary.name, vocabulary.name).insertBefore(element.find("hr"))
				.children("input").prop("checked", true).end().children("label").removeClass("external");
			alert("Vocabulary '" + vocabulary.name + "' uploaded successfully.");
			hideUpload();
			if (onChangeFn)
				onChangeFn();	
		}
		else
			alert("Error" + (data ? ":\n\n"+data : "."));	
	}
	
	function getVocabulary() {
		if (onChangeFn)
			onChangeFn();	
		if (!vocabularies[this.value])
			$.ajax({
				url: "rest/getVocabulary",
				type: "POST",
				data: this.value,
				contentType: "text/plain; charset=utf-8",
				dataType: "json",
				success: afterGet,
				error: alertErrorFn
			});
	}
	
	function afterGet(vocabulary) {
		element.find("input[value='" + vocabulary.name + "']").parent().children("label").removeClass("external");
		vocabularies[vocabulary.name] = vocabulary;
		if (onChangeFn)
			onChangeFn();	
	}

}