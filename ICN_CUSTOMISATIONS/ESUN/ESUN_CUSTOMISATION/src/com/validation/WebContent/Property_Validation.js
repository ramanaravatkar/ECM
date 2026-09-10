require([
	"property_ValidationDojo/MenuActionHider",
	"property_ValidationDojo/SearchCriteriaValidator"
	
], function(
	MenuActionHider,
	SearchCriteriaValidator
	
) {
	
	var menuActionHider =new MenuActionHider();
	menuActionHider.install();
	var searchValidator =new SearchCriteriaValidator();
	searchValidator.install();
	
	
}
);