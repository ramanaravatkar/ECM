define([
	"dojo/_base/declare",
	"dojo/aspect"
], function(declare, aspect) {

	return declare("property_ValidationDojo.SearchCriteriaValidator", null, {

		message: "Please enter at least one search criterion",


		failClosed: false,
		_installed: false,

		install: function() {

			if (this._installed) {
				return;
			}

			var self = this;

			require(["ecm/widget/search/SearchForm"], function(SearchForm) {

				if (!SearchForm ||
					!SearchForm.prototype ||
					typeof SearchForm.prototype._search !== "function") {

					console.error(" search not found.");
					return;
				}

				aspect.around(SearchForm.prototype, "_search", function(original) {

					console.log("search hooked.");

					return function() {




						if (self._hasNoCriteria(this)) {

							console.log(" Blank search detected.");

							self._alert();

							return;
						}


						return original.apply(this, arguments);
					};
				});

				self._installed = true;



			});

		},

		_hasNoCriteria: function(form) {

			try {

				var template = null;

				if (typeof form.getSearchTemplate === "function") {
					template = form.getSearchTemplate();
				}


				if (!template) {

					return this.failClosed;
				}


				var criteria =
					template.searchCriteria ||
					template.criterias ||
					template.criteria ||
					(template.attributes && template.attributes.searchCriteria) ||
					[];



				if (!criteria || criteria.length === 0) {



					return true;
				}

				for (var i = 0; i < criteria.length; i++) {



					if (this._isFilled(criteria[i])) {

						console.log("Filled criterion found.");

						return false;
					}
				}

				console.log("Blank Search.");

				return true;

			} catch (e) {

				console.error(e);

				return this.failClosed;
			}

		},

		_isFilled: function(c) {

			if (!c) {
				return false;
			}

			if (c.readOnly === true) {



				return false;

			}
			

			var values = [];

			if (c.values !== undefined)
				values.push(c.values);

			if (c.value !== undefined)
				values.push(c.value);

			if (typeof c.getValue === "function") {

				try {
					values.push(c.getValue());
				} catch (e) {
				}

			}

			if (c.value2 !== undefined)
				values.push(c.value2);

			for (var i = 0; i < values.length; i++) {



				if (this._nonEmpty(values[i])) {

					return true;
				}
			}

			return false;
		},

		_nonEmpty: function(v) {

			if (v == null)
				return false;

			if (v instanceof Array) {

				for (var i = 0; i < v.length; i++) {

					if (this._nonEmpty(v[i])) {
						return true;
					}
				}

				return false;
			}

			if (typeof v === "object") {

				if (v.value !== undefined)
					return this._nonEmpty(v.value);

				return true;
			}

			return String(v).trim().length > 0;
		},

		_alert: function() {

			var self = this;



			require([
				"ecm/widget/dialog/MessageDialog"
			], function(MessageDialog) {

				try {



					var dialog = new MessageDialog({
						text: self.message
					});

					dialog.show();

					setTimeout(function() {


						dialog.domNode.style.width = "350px";


						var content = dialog.domNode.querySelector(".dijitDialogPaneContent");
						if (content) {
							content.style.padding = "10px 15px";
							content.style.marginBottom = "5px";
							content.style.minHeight = "0";
							content.style.height = "35px";
						}


						var actionBar = dialog.domNode.querySelector(".dijitDialogPaneActionBar");
						if (actionBar) {
							actionBar.style.background = "#fff";
							actionBar.style.borderTop = "1px solid #e5e5e5";
							actionBar.style.padding = "5px 15px";
							actionBar.style.margin = "0";
							actionBar.style.textAlign = "right";
						}


						var button = actionBar ? actionBar.querySelector(".dijitButton") : null;
						if (button) {
							button.style.margin = "0";
						}

						dialog.resize({
							w: 350,
							h: 200
						});

					}, 50);

				} catch (e) {

					console.error(e);

					alert(self.message);
				}

			}, function() {

				alert(self.message);

			});

		}

	});

});
