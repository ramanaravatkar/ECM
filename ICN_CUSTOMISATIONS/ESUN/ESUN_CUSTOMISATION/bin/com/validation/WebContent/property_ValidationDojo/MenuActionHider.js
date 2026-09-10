define([
	"dojo/_base/declare",
	"dojo/_base/lang",
	"dojo/aspect",
	"dojo/query",
	"dojo/dom-style",
	"ecm/model/Desktop",
	"ecm/model/Request",
	"ecm/widget/layout/MainLayout",
	"ecm/widget/layout/NavigatorMainLayout",
	"ecm/widget/Toolbar"
], function(
	declare,
	lang,
	aspect,
	query,
	domStyle,
	Desktop,
	Request,
	MainLayout,
	NavigatorMainLayout,
	Toolbar
) {

	return declare("Property_ValidationDojo.MenuActionHider", null, {

		allowedFeatures: [],
		allowedOperations: [],
		managedOperations: [],

		_installed: false,
		_hookInstalled: false,
		_actionHookInstalled: false,
		_toolbarHookInstalled: false,
		_sessionHooksInstalled: false,

		_permissionsLoaded: false,
		_sessionGeneration: 0,

		_exportAllRetryTimer: null,
		_exportAllRetryCount: 0,
		_exportAllMaxRetries: 30,

		install: function() {

			if (this._installed) {
				return;
			}

			this._installed = true;


			this._installHook();
			this._installActionHook();
			this._installToolbarHook();
			this._installSessionHooks();

			this._waitForRepository(20);
		},

		_installSessionHooks: function() {

			if (this._sessionHooksInstalled) {
				return;
			}

			this._sessionHooksInstalled = true;

			var self = this;

			aspect.after(Desktop, "onLogout", function() {

				self._sessionGeneration++;
				self._permissionsLoaded = false;

				self.allowedFeatures = [];
				self.allowedOperations = [];
				self.managedOperations = [];

				self._stopExportAllRetry();

				self._publish();
				self._patchExistingNav();
				self._patchExistingExportAll();

				console.log(" User logged out.");

			}, true);

			aspect.after(Desktop, "onLogin", function() {

				self._sessionGeneration++;
				self._permissionsLoaded = false;

				self.allowedFeatures = [];
				self.allowedOperations = [];
				self.managedOperations = [];

				self._stopExportAllRetry();

				self._publish();
				self._patchExistingNav();
				self._patchExistingExportAll();

				self._waitForRepository(20);

				console.log("User logged in:", Desktop.userId);
			}, true);
		},

		_waitForRepository: function(retry) {

			var self = this;

			if (
				Desktop.connected &&
				Desktop.userId &&
				Desktop.defaultRepositoryId
			) {

				this._callService(
					Desktop.defaultRepositoryId,
					Desktop.userId,
					this._sessionGeneration
				);

				return;
			}

			if (retry > 0) {

				setTimeout(function() {
					self._waitForRepository(retry - 1);
				}, 250);

			} else {



				this._permissionsLoaded = true;

				this.allowedFeatures = [];
				this.allowedOperations = [];
				this.managedOperations = [];

				this._publish();
				this._patchExistingNav();
				this._patchExistingExportAll();
			}
		},

		_callService: function(
			repositoryId,
			requestUser,
			requestGeneration
		) {

			var self = this;

			Request.invokePluginService("Property_Validation", "GetAllowedOperationsService",
				{
					requestParams: {
						repositoryId: repositoryId
					},

					requestCompleteCallback: lang.hitch(this, function(response) {

						var currentUser = Desktop.userId;
						var currentGeneration =
							self._sessionGeneration;

						if (
							requestUser !== currentUser ||
							requestGeneration !== currentGeneration
						) {

							return;
						}

						if (typeof response === "string") {

							try {
								response = JSON.parse(response);
							} catch (e) {



								response = null;
							}
						}

						self.allowedFeatures =
							response &&
								Array.isArray(response.allowedFeatures)
								? response.allowedFeatures
								: [];

						self.allowedOperations =
							response &&
								Array.isArray(response.allowedOperations)
								? response.allowedOperations
								: [];

						self.managedOperations =
							response &&
								Array.isArray(response.managedOperations)
								? response.managedOperations
								: [];

						self._permissionsLoaded = true;

						self._publish();
						self._patchExistingNav();
						self._startExportAllRetry();
						self._refreshActionsUI();

						console.log("Permissions loaded for:", Desktop.userId);


					}
					),

					requestFailedCallback: lang.hitch(
						this,
						function(err) {

							if (
								requestUser !== Desktop.userId ||
								requestGeneration !==
								self._sessionGeneration
							) {

								return;
							}

							console.error(" Permission service failed.", err);



							self._permissionsLoaded = true;

							self.allowedFeatures = [];
							self.allowedOperations = [];
							self.managedOperations = [];

							self._publish();
							self._patchExistingNav();
							self._startExportAllRetry();
						}
					)
				}
			);
		},

		_publish: function() {

			window.icnAllowedFeatures = this.allowedFeatures;
			window.icnAllowedOperations = this.allowedOperations;
		},

		_refreshActionsUI: function() {

			try {

				if (
					Desktop &&
					typeof Desktop.refreshActionsList === "function"
				) {

					Desktop.refreshActionsList();
				}

			} catch (e) {

				console.warn("Unable to refresh actions.");
			}
		},

		_isBrowseAllowed: function() {

			return this.allowedFeatures.indexOf("browsePane") >= 0;
		},

		_isActionAllowed: function(label) {

			return this.allowedOperations.indexOf(label) >= 0;
		},

		_isExportAllAllowed: function() {

			return this.allowedOperations.indexOf("ExportAll") >= 0;
		},

		_installHook: function() {

			if (this._hookInstalled) {
				return;
			}

			this._hookInstalled = true;

			var self = this;

			var filterFn = function(features) {

				if (!self._permissionsLoaded) {
					return features;
				}

				if (!Array.isArray(features)) {
					return features;
				}

				if (!self._isBrowseAllowed()) {

					features = features.filter(function(feature) {

						return feature.id !== "browsePane";
					});
				}

				return features;
			};

			aspect.after(MainLayout.prototype, "getAvailableFeatures", filterFn, false);

			if (
				NavigatorMainLayout &&
				NavigatorMainLayout.prototype &&
				NavigatorMainLayout.prototype.hasOwnProperty("getAvailableFeatures")

			) {

				aspect.after(NavigatorMainLayout.prototype, "getAvailableFeatures", filterFn, false);
			}
		},

		_looksLikeActionMenu: function(children) {

			var universe =
				this.managedOperations &&
					this.managedOperations.length
					? this.managedOperations
					: this.allowedOperations;

			for (var i = 0; i < children.length; i++) {

				var item = children[i];

				if (!item.label) {
					continue;
				}

				var label = lang.trim(
					item.label.replace(/<[^>]*>/g, "")
				);

				if (universe.indexOf(label) >= 0) {
					return true;
				}
			}

			return false;
		},

		_installActionHook: function() {

			if (this._actionHookInstalled) {
				return;
			}

			this._actionHookInstalled = true;

			var self = this;

			require(["dijit/Menu"], function(Menu) {

				aspect.after(Menu.prototype, "onOpen", function() {

					var menu = this;

					var children =
						menu.getChildren
							? menu.getChildren()
							: [];

					if (!self._looksLikeActionMenu(children)) {
						return;
					}

					children.forEach(function(item) {

						if (!item.label) {
							return;
						}

						var label = lang.trim(
							item.label.replace(/<[^>]*>/g, "")

						);

						var allowed =
							self._isActionAllowed(label);

						domStyle.set(
							item.domNode,
							"display",
							allowed ? "" : "none"
						);
					});

				},
					true
				);
			});
		},

		_installToolbarHook: function() {

			if (this._toolbarHookInstalled) {
				return;
			}

			this._toolbarHookInstalled = true;

			var self = this;

			aspect.after(Toolbar.prototype, "onToolbarButtonsCreated", function(toolbarButtons) {

				self._patchExistingExportAll();

				return toolbarButtons;
			},
				false
			);
		},

		_startExportAllRetry: function() {

			var self = this;

			self._stopExportAllRetry();
			self._exportAllRetryCount = 0;

			if (self._patchExistingExportAll()) {
				return;
			}

			self._exportAllRetryTimer = setInterval(
				function() {

					if (self._patchExistingExportAll()) {

						self._stopExportAllRetry();
						return;
					}

					self._exportAllRetryCount++;

					if (
						self._exportAllRetryCount >=
						self._exportAllMaxRetries
					) {

						self._stopExportAllRetry();

						console.warn(" Export All button was not found.");
					}

				},
				500
			);
		},

		_stopExportAllRetry: function() {

			if (this._exportAllRetryTimer) {

				clearInterval(
					this._exportAllRetryTimer
				);

				this._exportAllRetryTimer = null;
			}

			this._exportAllRetryCount = 0;
		},

		_patchExistingExportAll: function() {

			if (!this._permissionsLoaded) {
				return false;
			}

			var exportAllAllowed =
				this._isExportAllAllowed();

			var matched = query(
				"[id^='EXPORTALL_'], " +
				"[widgetid^='EXPORTALL_'], " +
				"[aria-label='Export All'], " +
				"[title='Export All']"
			);

			if (!matched || matched.length === 0) {
				return false;
			}

			var processedButtons = [];

			matched.forEach(function(node) {

				if (!node) {
					return;
				}

				var button =
					node.closest
						? (
							node.closest(".dijitButton") ||
							node.closest("[role='button']") ||
							node.closest("[widgetid^='EXPORTALL_']") ||
							node
						)
						: node;

				if (
					processedButtons.indexOf(button) >= 0
				) {
					return;
				}

				processedButtons.push(button);

				domStyle.set(
					button,
					"display",
					exportAllAllowed ? "" : "none"
				);
			});

			return processedButtons.length > 0;
		},

		_patchExistingNav: function() {

			var browseAllowed =
				this._isBrowseAllowed();

			var matched = query(
				".dijitMenuItemLabel"
			).filter(function(node) {

				return (
					lang.trim(node.textContent || "") === "Browse"
				);
			});

			if (matched.length === 0) {

				matched = query("*").filter(
					function(node) {

						return (
							node.children.length === 0 &&
							lang.trim(
								node.textContent || ""
							) === "Browse"
						);
					}
				);
			}

			matched.forEach(function(node) {

				var row =
					node.closest
						? (
							node.closest("[role='menuitem']") ||
							node.closest(".dijitMenuItem") ||
							node.closest("tr") ||
							node
						)
						: node;

				domStyle.set(
					row,
					"display",
					browseAllowed ? "" : "none"
				);
			});
		}
	});
});